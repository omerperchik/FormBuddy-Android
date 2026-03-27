package com.formbuddy.android.domain.analysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.formbuddy.android.data.model.CheckboxType
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.model.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor(
    private val context: Context
) {
    suspend fun exportFilledPdf(documentData: ByteArray, template: FormTemplate): File = withContext(Dispatchers.IO) {
        val tempInputFile = File.createTempFile("export_in_", ".pdf", context.cacheDir)
        tempInputFile.writeBytes(documentData)

        val fd = ParcelFileDescriptor.open(tempInputFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val pdfDocument = PdfDocument()

        for (pageIndex in 0 until renderer.pageCount) {
            val pdfPage = renderer.openPage(pageIndex)
            val pageWidth = pdfPage.width
            val pageHeight = pdfPage.height

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Render original PDF page
            canvas.drawColor(Color.WHITE)
            pdfPage.render(canvas.toBitmap(pageWidth, pageHeight), null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            pdfPage.close()

            // Draw filled fields
            val formPage = template.pages.getOrNull(pageIndex)
            formPage?.fields?.forEach { field ->
                drawField(canvas, field, pageWidth.toFloat(), pageHeight.toFloat())
            }

            pdfDocument.finishPage(page)
        }

        renderer.close()
        fd.close()
        tempInputFile.delete()

        val outputFile = File(context.cacheDir, "FormBuddy_${template.documentName.replace(" ", "_")}.pdf")
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        outputFile
    }

    private fun drawField(canvas: Canvas, field: FormField, pageWidth: Float, pageHeight: Float) {
        val value = field.displayValue
        if (value.isBlank() && field.fieldType != FieldType.CHECKBOX) return

        val rect = field.boundingBox.scaledTo(pageWidth, pageHeight)
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        when (field.fieldType) {
            FieldType.CHECKBOX -> {
                if (value == "true" || value.equals("yes", true)) {
                    paint.textSize = rect.height() * 0.8f
                    val symbol = when (field.style.checkboxType) {
                        CheckboxType.CHECKMARK -> "\u2713"
                        CheckboxType.XMARK -> "\u2717"
                        CheckboxType.CIRCLE -> "\u25CF"
                        CheckboxType.NONE -> "\u2713"
                    }
                    canvas.drawText(symbol, rect.left + 2, rect.bottom - 2, paint)
                }
            }
            FieldType.SIGNATURE -> {
                // Draw signature path
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.strokeCap = Paint.Cap.ROUND
                // Signature path would be parsed and drawn here
            }
            else -> {
                paint.textSize = field.style.fontSize
                paint.textAlign = when (field.style.textAlignment) {
                    TextAlignment.CENTER -> Paint.Align.CENTER
                    TextAlignment.RIGHT -> Paint.Align.RIGHT
                    TextAlignment.LEFT -> Paint.Align.LEFT
                }

                val x = when (field.style.textAlignment) {
                    TextAlignment.LEFT -> rect.left + field.style.leftSpacing
                    TextAlignment.CENTER -> rect.centerX()
                    TextAlignment.RIGHT -> rect.right - field.style.rightSpacing
                }
                val y = rect.top + field.style.topSpacing + paint.textSize

                canvas.drawText(value, x, y, paint)
            }
        }
    }

    private fun Canvas.toBitmap(width: Int, height: Int): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val bitmapCanvas = Canvas(bitmap)
        bitmapCanvas.drawColor(Color.WHITE)
        return bitmap
    }
}
