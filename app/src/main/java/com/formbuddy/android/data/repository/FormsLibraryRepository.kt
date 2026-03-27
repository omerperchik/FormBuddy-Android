package com.formbuddy.android.data.repository

import com.formbuddy.android.data.remote.firebase.FirebaseManager
import com.formbuddy.android.data.model.FormTemplate
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class LibraryFormReference(
    val id: String,
    val documentName: String,
    val category: String,
    val thumbnailUrl: String?,
    val documentUrl: String,
    val pagesCount: Int,
    val fileSizeBytes: Long
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
            firebaseManager.searchFormsLibrary(query, lastDocument, pageSize).also { (forms, lastDoc) ->
                lastDocument = lastDoc
            }.first
        }

    suspend fun downloadFormDocument(url: String): ByteArray = withContext(Dispatchers.IO) {
        firebaseManager.downloadDocument(url)
    }

    fun resetPagination() {
        lastDocument = null
    }
}
