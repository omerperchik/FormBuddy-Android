package com.formbuddy.android.data.share

import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.formbuddy.android.data.repository.FormRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Fill this for me" — the viral loop.
 *
 * The owner taps Share → we upload the form template (no values) plus a
 * one-time download key to `sharedFills/{token}` in Firestore. The link
 * `https://formbuddy.app/fill?token=...` opens on any phone; the
 * recipient app downloads the template, asks the user to fill, signs,
 * and (optionally) writes the filled result back to
 * `sharedFills/{token}/responses/{uid}` so the original sender sees it
 * in their Docs list.
 *
 * Tokens are 24-byte URL-safe base64; Firestore security rules should
 * allow reads by the token only and writes by the recipient's anonymous
 * auth UID.
 */
@Singleton
class ShareForFillService @Inject constructor(
    private val gson: Gson,
    private val formRepository: FormRepository,
    private val auditLog: PrivacyAuditLog
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Publishes [formId]'s template (with field values cleared) to Firestore
     * and returns a sharable URL.
     */
    suspend fun publish(formId: String): String? {
        val entity = formRepository.getFormById(formId) ?: return null
        val template = formRepository.getFormTemplate(entity)
        // Don't share the values — only the *shape* of the form.
        for (field in template.allFields) {
            field.userValue = null
            field.detectedValue = null
            field.userInputMethod = null
        }
        val token = randomToken()
        val payload = mapOf(
            "templateJson" to gson.toJson(template),
            "documentName" to entity.documentName,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "ownerHint" to "shared-fill"
        )
        firestore.collection("sharedFills")
            .document(token)
            .set(payload, SetOptions.merge())
            .await()
        auditLog.log(
            PrivacyAuditLog.Category.Cloud,
            destination = "firestore:sharedFills/$token",
            description = "Published form for share-fill ($formId)"
        )
        return DeepLink.shareForFillUrl(token)
    }

    /**
     * Resolves a deep-link token by reading the published template from
     * Firestore. Returns null when the token is missing or malformed.
     */
    suspend fun resolve(token: String): FormTemplate? {
        if (token.isBlank()) return null
        val snap = firestore.collection("sharedFills").document(token).get().await()
        val json = snap.getString("templateJson") ?: return null
        return runCatching { gson.fromJson(json, FormTemplate::class.java) }.getOrNull()
    }

    private fun randomToken(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(
            bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
    }
}
