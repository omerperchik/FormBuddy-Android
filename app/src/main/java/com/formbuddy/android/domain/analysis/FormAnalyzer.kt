package com.formbuddy.android.domain.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.remote.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormAnalyzer @Inject constructor(
    private val fillablePdfParser: FillablePdfParser,
    private val ocrService: OcrService,
    private val fieldTypeInferencer: FieldTypeInferencer,
    private val geminiFieldRefiner: GeminiFieldRefiner,
    private val firebaseManager: FirebaseManager,
    private val context: Context
) {
    suspend fun analyzeDocument(documentData: ByteArray): FormTemplate = withContext(Dispatchers.IO) {
        val pdfBytes = if (looksLikePdf(documentData)) documentData else wrapImageInPdf(documentData)

        val isFillable = fillablePdfParser.isFillable(pdfBytes)

        val processedPdf = if (isFillable) {
            pdfBytes
        } else {
            // Send to cloud for processing — CommonForms turns the image-or-scan
            // into a fillable PDF whose widgets we can then parse.
            firebaseManager.processDocument(pdfBytes)
        }

        var template = fillablePdfParser.parse(processedPdf)
        template = ocrService.enrichFieldLabels(template, processedPdf)
        template = fieldTypeInferencer.inferFieldTypes(template)
        template = geminiFieldRefiner.refineFields(template)

        if (template.documentName.isBlank()) {
            template.documentName = "Form ${System.currentTimeMillis() / 1000}"
        }
        template
    }

    /** True if the byte stream starts with the `%PDF-` magic. */
    private fun looksLikePdf(bytes: ByteArray): Boolean {
        if (bytes.size < 5) return false
        return bytes[0] == '%'.code.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[4] == '-'.code.toByte()
    }

    /**
     * Wraps an image (JPEG/PNG/WebP/HEIC — anything `BitmapFactory` can decode)
     * into a single-page PDF so the rest of the pipeline (CommonForms / parser
     * / OCR) treats it identically to a real PDF input.
     *
     * Uses Android's built-in [PdfDocument]; no external deps.
     */
    private fun wrapImageInPdf(imageBytes: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: error("Could not decode image bytes")
        // Letter size at 72 DPI ~= 612 × 792, but we keep the image's native
        // aspect ratio so the field bounding boxes line up with the source.
        val out = ByteArrayOutputStream()
        val doc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo
                .Builder(bitmap.width, bitmap.height, 1)
                .create()
            val page = doc.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        return out.toByteArray()
    }
}
