package com.formbuddy.android.data.remote.firebase

import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.formbuddy.android.data.repository.LibraryFormReference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase services using the **same schema and callable contracts** as
 * the iOS app (`Documentation/FirebaseLibrary.md`):
 *
 *   - Firestore collection: `formReferences` (not `forms`).
 *   - Firestore collection for parsed templates: `formTemplates` (same id).
 *   - Storage paths: `originals/{agency}/{sha256}.{ext}`,
 *                    `thumbnails/{agency}/{sha256}.png`.
 *   - Callable: `processDocument` with payload `base64Document`, `mimeType`,
 *     optional `version`. Region `us-central1`.
 *   - Callable: `getTextToSpeechAudioData` with `text`, `voice`, `instructions`,
 *     `model`, `format`. Returns `{ audioBase64, format }`.
 *   - Search via the `searchKeywords` array using `arrayContainsAny` (Firestore
 *     allows up to 10 terms per query) and an AND re-check on the client.
 *
 * Every cross-network operation also writes a [PrivacyAuditLog] entry so the
 * Settings screen can show "what crossed the device boundary, when, and why".
 */
@Singleton
class FirebaseManager @Inject constructor(
    private val auditLog: PrivacyAuditLog
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance(REGION) }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            auditLog.log(
                PrivacyAuditLog.Category.Cloud,
                destination = "firebase-auth",
                description = "Anonymous sign-in"
            )
            auth.signInAnonymously().await()
        }
    }

    /** Calls the iOS-shared `processDocument` callable. Returns the fillable PDF bytes. */
    suspend fun processDocument(
        documentData: ByteArray,
        mimeType: String = "application/pdf",
        modelVersion: String? = null
    ): ByteArray {
        ensureAuthenticated()
        auditLog.log(
            PrivacyAuditLog.Category.Cloud,
            destination = "fn:processDocument",
            description = "Sent ${documentData.size} bytes for CommonForms widget detection"
        )
        val base64 = android.util.Base64.encodeToString(documentData, android.util.Base64.NO_WRAP)
        val payload = hashMapOf<String, Any>(
            "base64Document" to base64,
            "mimeType" to mimeType
        )
        modelVersion?.let { payload["version"] = it }

        val result = functions.getHttpsCallable("processDocument").call(payload).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val output = data["output"] as? Map<String, Any?>
        val fillableUrl = output?.get("fillable_pdf") as? String
            ?: throw IllegalStateException("processDocument: no fillable_pdf in response")
        return downloadFromUrl(fillableUrl)
    }

    /** OpenAI TTS proxy. Returns audio bytes in [format]. */
    suspend fun getTextToSpeechAudio(
        text: String,
        voice: String = "alloy",
        instructions: String? = null,
        model: String = "gpt-4o-mini-tts",
        format: String = "mp3"
    ): ByteArray {
        ensureAuthenticated()
        auditLog.log(
            PrivacyAuditLog.Category.Cloud,
            destination = "fn:getTextToSpeechAudioData",
            description = "TTS request (${text.length} chars, voice=$voice)"
        )
        val payload = hashMapOf<String, Any>(
            "text" to text,
            "voice" to voice,
            "model" to model,
            "format" to format
        )
        instructions?.let { payload["instructions"] = it }

        val result = functions.getHttpsCallable("getTextToSpeechAudioData").call(payload).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as Map<String, Any?>
        val base64 = data["audioBase64"] as? String
            ?: throw IllegalStateException("TTS callable: missing audioBase64")
        return android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
    }

    /**
     * Searches `formReferences` using the keyword index that the iOS ingestion
     * script writes. The query is split into tokens; we hand the first 10 to
     * `arrayContainsAny` and AND-validate on the client for the rest.
     */
    suspend fun searchFormsLibrary(
        query: String,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): Pair<List<LibraryFormReference>, DocumentSnapshot?> {
        ensureAuthenticated()

        val terms = query
            .lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() }
            .distinct()

        var firestoreQuery: Query = firestore.collection("formReferences")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (terms.isNotEmpty()) {
            val arrayTerms = terms.take(10)
            firestoreQuery = firestore.collection("formReferences")
                .whereArrayContainsAny("searchKeywords", arrayTerms)
                .limit(pageSize.toLong())
        }

        if (lastDocument != null) {
            firestoreQuery = firestoreQuery.startAfter(lastDocument)
        }

        val snapshot = firestoreQuery.get().await()
        val forms = snapshot.documents
            .filter { doc ->
                if (terms.isEmpty()) true
                else {
                    @Suppress("UNCHECKED_CAST")
                    val keywords = (doc.get("searchKeywords") as? List<String>).orEmpty()
                    terms.all { it in keywords }
                }
            }
            .mapNotNull { doc -> doc.toLibraryFormReference() }

        return forms to snapshot.documents.lastOrNull()
    }

    /**
     * Uploads a community-contributed form. Persists into `originals/`,
     * `thumbnails/`, and writes a `formReferences/{id}` row matching the iOS
     * schema. Library-mode editor users use this from the Pro upload flow.
     */
    suspend fun uploadCommunityForm(
        agency: String,
        title: String,
        language: String,
        documentBytes: ByteArray,
        thumbnailBytes: ByteArray,
        contentType: String,
        notes: String = ""
    ): String {
        ensureAuthenticated()
        val sha256 = sha256(documentBytes)
        val ext = if (contentType == "application/pdf") "pdf" else "bin"
        val originalRef = storage.reference.child("originals/$agency/$sha256.$ext")
        val thumbRef = storage.reference.child("thumbnails/$agency/$sha256.png")
        auditLog.log(
            PrivacyAuditLog.Category.Cloud,
            destination = "storage:originals/$agency",
            description = "Community upload of $title (${documentBytes.size} bytes)"
        )
        originalRef.putBytes(documentBytes).await()
        thumbRef.putBytes(thumbnailBytes).await()
        val downloadUrl = originalRef.downloadUrl.await().toString()
        val thumbnailUrl = thumbRef.downloadUrl.await().toString()

        val docRef = firestore.collection("formReferences").document()
        val keywords = computeSearchKeywords(title, agency, language)
        val data = mapOf(
            "documentName" to title,
            "downloadUrl" to downloadUrl,
            "thumbnailUrl" to thumbnailUrl,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "pagesCount" to 0,
            "agency" to agency,
            "title" to title,
            "originalFileName" to title,
            "language" to language,
            "yearHint" to "",
            "contentType" to contentType,
            "sha256" to sha256,
            "discoveredFrom" to "community",
            "notes" to notes,
            "bytes" to documentBytes.size,
            "isFillable" to false,
            "isVerified" to false,
            "searchKeywords" to keywords
        )
        docRef.set(data).await()
        return docRef.id
    }

    /**
     * Looks up an instant-match template by PDF SHA-256.
     *
     * The ingestion pipeline writes `formTemplates/{sha256}` with a `templateJson`
     * field whose value is the JSON-encoded [com.formbuddy.android.data.model.FormTemplate].
     * Returns null if no match.
     */
    suspend fun fetchInstantMatch(sha256: String): String? {
        ensureAuthenticated()
        auditLog.log(
            PrivacyAuditLog.Category.Cloud,
            destination = "firestore:formTemplates/$sha256",
            description = "Instant-match lookup"
        )
        val snap = firestore.collection("formTemplates").document(sha256).get().await()
        if (!snap.exists()) return null
        return snap.getString("templateJson")
    }

    /** Downloads either a Storage `gs://`/firebasestorage URL or plain HTTPS. */
    suspend fun downloadDocument(url: String): ByteArray = downloadFromUrl(url)

    private suspend fun downloadFromUrl(url: String): ByteArray {
        ensureAuthenticated()
        return if (url.startsWith("gs://") || url.contains("firebasestorage")) {
            storage.getReferenceFromUrl(url).getBytes(MAX_DOWNLOAD_BYTES).await()
        } else {
            withContext(Dispatchers.IO) {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 30_000
                conn.readTimeout = 60_000
                try {
                    conn.inputStream.use { it.readBytes() }
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    private fun computeSearchKeywords(vararg sources: String): List<String> {
        val tokens = sources.flatMap { src ->
            src.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        }
        val expanded = tokens.flatMap { tok ->
            val out = mutableListOf(tok)
            val isNumeric = tok.all { it.isDigit() }
            val maxLen = if (isNumeric) 8 else 6
            for (n in 1..minOf(tok.length, maxLen)) out += tok.substring(0, n)
            out
        }
        return expanded.distinct().take(300)
    }

    private fun sha256(bytes: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun DocumentSnapshot.toLibraryFormReference(): LibraryFormReference? = try {
        LibraryFormReference(
            id = id,
            documentName = getString("documentName") ?: "",
            agency = getString("agency") ?: "",
            language = getString("language") ?: "",
            thumbnailUrl = getString("thumbnailUrl"),
            downloadUrl = getString("downloadUrl") ?: "",
            pagesCount = getLong("pagesCount")?.toInt() ?: 0,
            bytes = getLong("bytes") ?: 0,
            isVerified = getBoolean("isVerified") ?: false
        )
    } catch (_: Exception) { null }

    companion object {
        private const val REGION = "us-central1"
        private const val MAX_DOWNLOAD_BYTES = 25L * 1024 * 1024
    }
}
