package com.formbuddy.android.data.repository

import com.formbuddy.android.data.remote.firebase.FirebaseManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors iOS `FormReference` / `FormReferencesPage`. Field names match the
 * Firestore documents so Android & iOS read the same data without divergence.
 */
data class LibraryFormReference(
    val id: String,
    val documentName: String,
    val agency: String,
    val language: String,
    val thumbnailUrl: String?,
    val downloadUrl: String,
    val pagesCount: Int,
    val bytes: Long,
    val isVerified: Boolean
)

@Singleton
class FormsLibraryRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    private var lastDocument: DocumentSnapshot? = null
    private val pageSize = 20

    suspend fun searchForms(query: String, reset: Boolean = false): List<LibraryFormReference> =
        withContext(Dispatchers.IO) {
            if (reset) lastDocument = null
            val (forms, lastDoc) = firebaseManager.searchFormsLibrary(query, lastDocument, pageSize)
            lastDocument = lastDoc
            forms
        }

    suspend fun downloadFormDocument(url: String): ByteArray = withContext(Dispatchers.IO) {
        firebaseManager.downloadDocument(url)
    }

    /** Community upload — submits an editor's form to the shared library. */
    suspend fun uploadForm(
        agency: String,
        title: String,
        language: String,
        documentBytes: ByteArray,
        thumbnailBytes: ByteArray,
        contentType: String,
        notes: String = ""
    ): String = withContext(Dispatchers.IO) {
        firebaseManager.uploadCommunityForm(
            agency = agency,
            title = title,
            language = language,
            documentBytes = documentBytes,
            thumbnailBytes = thumbnailBytes,
            contentType = contentType,
            notes = notes
        )
    }

    fun resetPagination() {
        lastDocument = null
    }
}
