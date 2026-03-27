package com.formbuddy.android.ui.screens.filling.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.repository.FormRepository
import com.formbuddy.android.domain.analysis.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val pdfExporter: PdfExporter,
    private val context: Context
) : ViewModel() {

    private val _formTemplate = MutableStateFlow<FormTemplate?>(null)
    val formTemplate: StateFlow<FormTemplate?> = _formTemplate

    private val _pageBitmaps = MutableStateFlow<List<Bitmap?>>(emptyList())
    val pageBitmaps: StateFlow<List<Bitmap?>> = _pageBitmaps

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private var formId: String = ""
    private var documentData: ByteArray? = null

    fun loadForm(id: String) {
        formId = id
        viewModelScope.launch {
            val entity = formRepository.getFormById(id) ?: return@launch
            _formTemplate.value = formRepository.getFormTemplate(entity)
            documentData = formRepository.getDocumentData(id)
            documentData?.let { renderPages(it) }
        }
    }

    private suspend fun renderPages(data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("pdf_", ".pdf", context.cacheDir)
            tempFile.writeBytes(data)
            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            val bitmaps = (0 until renderer.pageCount).map { i ->
                val page = renderer.openPage(i)
                val scale = 2f
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            }

            renderer.close()
            fd.close()
            tempFile.delete()

            _pageBitmaps.value = bitmaps
        } catch (_: Exception) {
            _pageBitmaps.value = emptyList()
        }
    }

    fun toggleEditing() {
        _isEditing.value = !_isEditing.value
    }

    fun renameDocument(name: String) {
        val template = _formTemplate.value ?: return
        template.documentName = name
        _formTemplate.value = template.copy(documentName = name)
        saveForm()
    }

    fun updateFieldValue(fieldId: String, value: String) {
        val template = _formTemplate.value ?: return
        template.allFields.find { it.id == fieldId }?.userValue = value
        _formTemplate.value = template.copy()
    }

    fun saveForm() {
        viewModelScope.launch {
            val template = _formTemplate.value ?: return@launch
            formRepository.updateForm(formId, template)
        }
    }

    fun deleteForm() {
        viewModelScope.launch {
            formRepository.deleteForm(formId)
        }
    }

    fun exportPdf(onExported: (File) -> Unit) {
        viewModelScope.launch {
            val template = _formTemplate.value ?: return@launch
            val data = documentData ?: return@launch
            val file = pdfExporter.exportFilledPdf(data, template)
            onExported(file)
        }
    }
}
