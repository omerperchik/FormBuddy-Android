package com.formbuddy.android.domain.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.formbuddy.android.data.model.FormTemplate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class OcrService @Inject constructor(
    private val context: Context
) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        // 2x is sufficient for ML Kit OCR accuracy — 3x wastes 2.25x more memory
        private const val OCR_SCALE = 2f
    }

    suspend fun enrichFieldLabels(template: FormTemplate, documentData: ByteArray): FormTemplate = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("ocr_", ".pdf", context.cacheDir)
        tempFile.writeBytes(documentData)

        var renderer: PdfRenderer? = null
        var fd: ParcelFileDescriptor? = null

        try {
            fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)

            template.pages.forEach { page ->
                if (page.index < renderer.pageCount) {
                    val pdfPage = renderer.openPage(page.index)
                    val bitmapWidth = (pdfPage.width * OCR_SCALE).toInt()
                    val bitmapHeight = (pdfPage.height * OCR_SCALE).toInt()

                    // RGB_565 uses half the memory of ARGB_8888 — OCR doesn't need alpha
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val ocrResult = recognizeText(bitmap)
                    bitmap.recycle()

                    // Use saved dimensions — bitmap is recycled above
                    page.fields.forEach { field ->
                        if (field.label.isBlank() || isGenericLabel(field.label)) {
                            val nearbyText = findNearbyText(field, ocrResult, bitmapWidth, bitmapHeight)
                            if (nearbyText.isNotBlank()) {
                                field.label = nearbyText
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            renderer?.close()
            fd?.close()
            tempFile.delete()
        }

        template
    }

    private suspend fun recognizeText(bitmap: Bitmap): List<OcrTextBlock> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                val blocks = result.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        OcrTextBlock(
                            text = line.text,
                            boundingBox = line.boundingBox,
                            centerX = line.boundingBox?.centerX()?.toFloat() ?: 0f,
                            centerY = line.boundingBox?.centerY()?.toFloat() ?: 0f
                        )
                    }
                }
                cont.resume(blocks)
            }
            .addOnFailureListener { cont.resume(emptyList()) }
    }

    private fun findNearbyText(
        field: com.formbuddy.android.data.model.FormField,
        ocrResults: List<OcrTextBlock>,
        pageWidth: Int,
        pageHeight: Int
    ): String {
        val fieldRect = field.boundingBox.scaledTo(pageWidth.toFloat(), pageHeight.toFloat())
        val searchMargin = 50f

        val nearby = ocrResults
            .filter { block ->
                block.boundingBox != null &&
                    block.centerY >= fieldRect.top - searchMargin &&
                    block.centerY <= fieldRect.bottom + searchMargin &&
                    (block.centerX < fieldRect.left + searchMargin)
            }
            .sortedBy {
                val dx = it.centerX - fieldRect.left
                val dy = it.centerY - fieldRect.centerY()
                dx * dx + dy * dy
            }

        return nearby.firstOrNull()?.text ?: ""
    }

    private fun isGenericLabel(label: String): Boolean {
        val generic = setOf("field", "text", "input", "textfield", "textbox", "field1", "field2")
        return label.lowercase().trim() in generic
    }

    data class OcrTextBlock(
        val text: String,
        val boundingBox: android.graphics.Rect?,
        val centerX: Float,
        val centerY: Float
    )
}
