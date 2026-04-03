package com.formbuddy.android.data.remote.firebase

import com.formbuddy.android.data.repository.LibraryFormReference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    suspend fun processDocument(documentData: ByteArray): ByteArray {
        ensureAuthenticated()
        val base64Data = android.util.Base64.encodeToString(documentData, android.util.Base64.NO_WRAP)
        val result = functions
            .getHttpsCallable("processDocument")
            .call(hashMapOf("documentData" to base64Data))
            .await()

        val responseData = result.getData() as Map<*, *>
        val processedBase64 = responseData["processedDocument"] as String
        return android.util.Base64.decode(processedBase64, android.util.Base64.NO_WRAP)
    }

    suspend fun getTextToSpeechAudio(
        text: String,
        voice: String,
        locale: String
    ): ByteArray {
        ensureAuthenticated()
        val result = functions
            .getHttpsCallable("getTextToSpeechAudioData")
            .call(
                hashMapOf(
                    "text" to text,
                    "voice" to voice,
                    "locale" to locale
                )
            )
            .await()

        val responseData = result.getData() as Map<*, *>
        val audioBase64 = responseData["audioData"] as String
        return android.util.Base64.decode(audioBase64, android.util.Base64.NO_WRAP)
    }

    suspend fun searchFormsLibrary(
        query: String,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): Pair<List<LibraryFormReference>, DocumentSnapshot?> {
        ensureAuthenticated()

        var firestoreQuery = firestore.collection("forms")
            .limit(pageSize.toLong())

        if (query.isNotBlank()) {
            firestoreQuery = firestoreQuery
                .whereGreaterThanOrEqualTo("documentName", query)
                .whereLessThanOrEqualTo("documentName", query + "\uf8ff")
        }

        if (lastDocument != null) {
            firestoreQuery = firestoreQuery.startAfter(lastDocument)
        }

        val snapshot = firestoreQuery.get().await()
        val forms = snapshot.documents.mapNotNull { doc ->
            try {
                LibraryFormReference(
                    id = doc.id,
                    documentName = doc.getString("documentName") ?: "",
                    category = doc.getString("category") ?: "",
                    thumbnailUrl = doc.getString("thumbnailUrl"),
                    documentUrl = doc.getString("documentUrl") ?: "",
                    pagesCount = doc.getLong("pagesCount")?.toInt() ?: 0,
                    fileSizeBytes = doc.getLong("fileSizeBytes") ?: 0
                )
            } catch (_: Exception) { null }
        }

        val lastDoc = snapshot.documents.lastOrNull()
        return forms to lastDoc
    }

    suspend fun downloadDocument(url: String): ByteArray {
        ensureAuthenticated()
        val ref = storage.getReferenceFromUrl(url)
        return ref.getBytes(25 * 1024 * 1024).await()
    }
}
