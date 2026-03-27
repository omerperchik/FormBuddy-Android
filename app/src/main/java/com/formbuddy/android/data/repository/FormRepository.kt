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
import java.io.ByteArrayOutputStream
import java.io.File
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
    }

    fun getAllForms(): Flow<List<FormReferenceEntity>> = formDao.getAllForms()

    suspend fun getFormById(id: String): FormReferenceEntity? = formDao.getFormById(id)

    suspend fun saveForm(
        formTemplate: FormTemplate,
        documentData: ByteArray,
        existingId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val id = existingId ?: UUID.randomUUID().toString()

        val docsDir = File(context.filesDir, DOCUMENTS_DIR).apply { mkdirs() }
        val thumbsDir = File(context.filesDir, THUMBNAILS_DIR).apply { mkdirs() }

        val docFile = File(docsDir, "$id.pdf.enc")
        encryptionManager.writeEncryptedFile(docFile, documentData)

        val thumbnailPath = generateThumbnail(documentData, id, thumbsDir)

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

    private suspend fun generateThumbnail(
        documentData: ByteArray,
        id: String,
        thumbsDir: File
    ): String? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("pdf_", ".pdf", context.cacheDir)
            tempFile.writeBytes(documentData)

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val scale = 2f
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val thumbFile = File(thumbsDir, "$id.png")
                ByteArrayOutputStream().use { baos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, baos)
                    thumbFile.writeBytes(baos.toByteArray())
                }
                bitmap.recycle()
                renderer.close()
                fd.close()
                tempFile.delete()

                thumbFile.absolutePath
            } else {
                renderer.close()
                fd.close()
                tempFile.delete()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
