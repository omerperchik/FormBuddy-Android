package com.formbuddy.android.domain.analysis

import com.formbuddy.android.data.model.BoundingBox
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormPage
import com.formbuddy.android.data.model.FormTemplate
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDSignatureField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FillablePdfParser @Inject constructor() {

    fun isFillable(documentData: ByteArray): Boolean {
        return try {
            val doc = PDDocument.load(ByteArrayInputStream(documentData))
            val hasForms = doc.documentCatalog.acroForm?.fields?.isNotEmpty() == true
            doc.close()
            hasForms
        } catch (_: Exception) {
            false
        }
    }

    suspend fun parse(documentData: ByteArray): FormTemplate = withContext(Dispatchers.IO) {
        val doc = PDDocument.load(ByteArrayInputStream(documentData))
        val acroForm = doc.documentCatalog.acroForm
        val template = FormTemplate()

        // Create pages
        val pageCount = doc.numberOfPages
        val pages = (0 until pageCount).map { FormPage(index = it, pdfPageIndex = it) }.toMutableList()

        if (acroForm != null) {
            parseAcroFormFields(acroForm, doc, pages)
        }

        template.pages.addAll(pages)
        doc.close()
        template
    }

    private fun parseAcroFormFields(acroForm: PDAcroForm, doc: PDDocument, pages: MutableList<FormPage>) {
        acroForm.fieldTree.forEach { field ->
            try {
                parseField(field, doc, pages)
            } catch (_: Exception) {}
        }
    }

    private fun parseField(field: PDField, doc: PDDocument, pages: MutableList<FormPage>) {
        val widgets = field.widgets
        if (widgets.isNullOrEmpty()) return

        val widget = widgets[0]
        val rect = widget.rectangle ?: return

        // Determine which page this field belongs to
        val page = widget.page ?: return
        val pageIndex = doc.pages.indexOf(page)
        if (pageIndex < 0 || pageIndex >= pages.size) return

        val pageBox = page.mediaBox
        val pageWidth = pageBox.width.toDouble()
        val pageHeight = pageBox.height.toDouble()

        // Normalize coordinates (PDF origin is bottom-left, we need top-left)
        val x = rect.lowerLeftX.toDouble() / pageWidth
        val y = 1.0 - (rect.upperRightY.toDouble() / pageHeight)
        val width = (rect.upperRightX - rect.lowerLeftX).toDouble() / pageWidth
        val height = (rect.upperRightY - rect.lowerLeftY).toDouble() / pageHeight

        val boundingBox = BoundingBox(x = x, y = y, width = width, height = height)

        val fieldType = when (field) {
            is PDCheckBox -> FieldType.CHECKBOX
            is PDSignatureField -> FieldType.SIGNATURE
            is PDTextField -> FieldType.TEXT
            else -> FieldType.TEXT
        }

        val detectedValue = when (field) {
            is PDTextField -> field.value
            is PDCheckBox -> if (field.isChecked) "true" else null
            else -> null
        }

        val formField = FormField(
            label = field.partialName ?: field.fullyQualifiedName ?: "",
            fieldType = fieldType,
            boundingBox = boundingBox,
            detectedValue = detectedValue
        )

        pages[pageIndex].fields.add(formField)
    }
}
