package com.formbuddy.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.formbuddy.android.data.local.db.dao.FormDao
import com.formbuddy.android.data.local.db.entity.FormReferenceEntity
import com.formbuddy.android.data.local.encryption.EncryptionManager
import com.formbuddy.android.data.model.FormTemplate
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormRepository @Inject constructor(
    private val formDao: FormDao,
    private val encryptionManager: EncryptionManager,
    private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val MAX_DOCUMENT_SIZE = 25 * 1024 * 1024L // 25 MB
        private const val DOCUMENTS_DIR = "documents"
        private const val THUMBNAILS_DIR = "thumbnails"
        private const val THUMBNAIL_QUALITY = 70 // WEBP is ~30% smaller than PNG at same quality
        private const val THUMBNAIL_MAX_WIDTH = 400 // Enough for grid cards
    }

    // Cache directory refs — avoid repeated File object allocation
    private val docsDir: File by lazy { File(context.filesDir, DOCUMENTS_DIR).apply { mkdirs() } }
    private val thumbsDir: File by lazy { File(context.filesDir, THUMBNAILS_DIR).apply { mkdirs() } }

    fun getAllForms(): Flow<List<FormReferenceEntity>> = formDao.getAllForms()

    suspend fun getFormById(id: String): FormReferenceEntity? = formDao.getFormById(id)

    suspend fun saveForm(
        formTemplate: FormTemplate,
        documentData: ByteArray,
        existingId: String? = null
    ): String = withContext(Dispatchers.IO) {
        require(documentData.size <= MAX_DOCUMENT_SIZE) { "Document exceeds 25MB limit" }

        val id = existingId ?: UUID.randomUUID().toString()

        val docFile = File(docsDir, "$id.pdf.enc")
        encryptionManager.writeEncryptedFile(docFile, documentData)

        val thumbnailPath = generateThumbnail(documentData, id)
        val templateJson = gson.toJson(formTemplate)

        val entity = FormReferenceEntity(
            id = id,
            formTemplateId = formTemplate.id,
            documentName = formTemplate.documentName,
            pagesCount = formTemplate.pages.size,
            documentFileSizeInBytes = documentData.size.toLong(),
            formTemplateJson = templateJson,
            documentFilePath = docFile.absolutePath,
            thumbnailFilePath = thumbnailPath,
            modifiedAt = System.currentTimeMillis()
        )

        formDao.insertForm(entity)
        id
    }

    suspend fun updateForm(
        id: String,
        formTemplate: FormTemplate
    ) = withContext(Dispatchers.IO) {
        val existing = formDao.getFormById(id) ?: return@withContext
        val templateJson = gson.toJson(formTemplate)
        formDao.updateForm(
            existing.copy(
                formTemplateJson = templateJson,
                documentName = formTemplate.documentName,
                modifiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteForm(id: String) = withContext(Dispatchers.IO) {
        val entity = formDao.getFormById(id) ?: return@withContext
        File(entity.documentFilePath).delete()
        entity.thumbnailFilePath?.let { File(it).delete() }
        formDao.deleteFormById(id)
    }

    suspend fun getDocumentData(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val entity = formDao.getFormById(id) ?: return@withContext null
        encryptionManager.readEncryptedFile(File(entity.documentFilePath))
    }

    suspend fun getFormTemplate(entity: FormReferenceEntity): FormTemplate {
        return gson.fromJson(entity.formTemplateJson, FormTemplate::class.java)
    }

    private fun generateThumbnail(
        documentData: ByteArray,
        id: String
    ): String? {
        var renderer: PdfRenderer? = null
        var fd: ParcelFileDescriptor? = null
        var tempFile: File? = null

        try {
            tempFile = File.createTempFile("pdf_", ".pdf", context.cacheDir)
            tempFile.writeBytes(documentData)

            fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)

            if (renderer.pageCount == 0) return null

            val page = renderer.openPage(0)

            // Scale to fit THUMBNAIL_MAX_WIDTH while preserving aspect ratio
            val aspectRatio = page.height.toFloat() / page.width
            val width = THUMBNAIL_MAX_WIDTH
            val height = (width * aspectRatio).toInt()

            // Use RGB_565 for thumbnails — half the memory of ARGB_8888, fine for previews
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val thumbFile = File(thumbsDir, "$id.webp")
            // WEBP is ~30% smaller than PNG, supported since API 14
            FileOutputStream(thumbFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, THUMBNAIL_QUALITY, fos)
            }
            bitmap.recycle()

            return thumbFile.absolutePath
        } catch (_: Exception) {
            return null
        } finally {
            renderer?.close()
            fd?.close()
            tempFile?.delete()
        }
    }
}
