package com.formbuddy.android.domain.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.RectF
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

/**
 * Renders a filled [FormTemplate] back onto the original PDF and writes it to cache.
 *
 * Mirrors iOS `FormTemplate.rendererPDFData()` + `DocumentPageViewExportingWrapper`:
 *   - Each page is rendered to a bitmap from [PdfRenderer], then drawn onto the new
 *     PdfDocument page so the original visual layer (text, lines, images) is preserved.
 *   - Filled values are overlaid using the fields' value bounding boxes.
 *   - Signatures are reconstructed from the stroke string produced by
 *     [com.formbuddy.android.ui.components.signatureStrokesToPath] and drawn as ink
 *     within the value bounding box, scaled and centered.
 */
@Singleton
class PdfExporter @Inject constructor(
    private val context: Context
) {
    suspend fun exportFilledPdf(
        documentData: ByteArray,
        template: FormTemplate
    ): File = withContext(Dispatchers.IO) {
        val tempInputFile = File.createTempFile("export_in_", ".pdf", context.cacheDir)
        tempInputFile.writeBytes(documentData)

        val fd = ParcelFileDescriptor.open(tempInputFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val pdfDocument = PdfDocument()

        try {
            for (pageIndex in 0 until renderer.pageCount) {
                val pdfPage = renderer.openPage(pageIndex)
                val pageWidth = pdfPage.width
                val pageHeight = pdfPage.height

                // 1. Render the original page into a bitmap.
                val pageBitmap = Bitmap.createBitmap(
                    pageWidth, pageHeight, Bitmap.Config.ARGB_8888
                ).apply {
                    eraseColor(Color.WHITE)
                }
                pdfPage.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                )
                pdfPage.close()

                // 2. Start a new PDF page and draw the bitmap.
                val pageInfo = PdfDocument.PageInfo
                    .Builder(pageWidth, pageHeight, pageIndex)
                    .create()
                val outPage = pdfDocument.startPage(pageInfo)
                val canvas = outPage.canvas
                canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                pageBitmap.recycle()

                // 3. Overlay filled fields.
                template.pages.getOrNull(pageIndex)?.fields?.forEach { field ->
                    drawField(canvas, field, pageWidth.toFloat(), pageHeight.toFloat())
                }

                pdfDocument.finishPage(outPage)
            }

            val safeName = template.documentName
                .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .ifBlank { "FormBuddy" }
            val outputFile = File(context.cacheDir, "${safeName}_filled.pdf")
            FileOutputStream(outputFile).use { fos -> pdfDocument.writeTo(fos) }
            outputFile
        } finally {
            pdfDocument.close()
            renderer.close()
            fd.close()
            tempInputFile.delete()
        }
    }

    private fun drawField(canvas: Canvas, field: FormField, pageWidth: Float, pageHeight: Float) {
        val value = field.displayValue
        val rect = (field.valueBoundingBox ?: field.boundingBox).scaledTo(pageWidth, pageHeight)

        when (field.fieldType) {
            FieldType.CHECKBOX -> drawCheckbox(canvas, field, rect)
            FieldType.SIGNATURE -> drawSignature(canvas, value, rect)
            else -> if (value.isNotBlank()) drawText(canvas, field, value, rect)
        }
    }

    private fun drawCheckbox(canvas: Canvas, field: FormField, rect: RectF) {
        if (field.style.checkboxType == CheckboxType.NONE) return
        val symbol = when (field.style.checkboxType) {
            CheckboxType.CHECKMARK -> "✓"
            CheckboxType.XMARK -> "✗"
            CheckboxType.CIRCLE -> "●"
            CheckboxType.NONE -> return
        }
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = rect.height() * 0.85f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val cx = rect.centerX()
        // Vertical center via font metrics so the glyph sits centered in the box.
        val metrics = paint.fontMetrics
        val cy = rect.centerY() - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(symbol, cx, cy, paint)
    }

    private fun drawText(canvas: Canvas, field: FormField, value: String, rect: RectF) {
        val style = field.style
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.DEFAULT
            textSize = if (style.fontSize > 0) style.fontSize else (rect.height() * 0.7f).coerceAtLeast(8f)
            letterSpacing = if (style.fontSize > 0) (style.kern / style.fontSize) else 0f
            textAlign = when (style.textAlignment) {
                TextAlignment.CENTER -> Paint.Align.CENTER
                TextAlignment.RIGHT -> Paint.Align.RIGHT
                TextAlignment.LEFT -> Paint.Align.LEFT
            }
        }

        val x = when (style.textAlignment) {
            TextAlignment.LEFT -> rect.left + style.leftSpacing
            TextAlignment.CENTER -> rect.centerX()
            TextAlignment.RIGHT -> rect.right - style.rightSpacing
        }
        // Baseline = top + ascent + topSpacing — keeps the visible top of the glyph aligned.
        val metrics = paint.fontMetrics
        val baseline = rect.top + style.topSpacing + (-metrics.ascent)

        // Multi-line wrap when the field type supports it.
        val supportsMultiline = field.fieldType == FieldType.TEXT || field.fieldType == FieldType.ADDRESS
        if (!supportsMultiline) {
            canvas.drawText(value, x, baseline, paint)
            return
        }

        val maxWidth = rect.width() - style.leftSpacing - style.rightSpacing
        val lines = wrap(value, paint, maxWidth)
        val lineHeight = paint.textSize * style.lineHeightFactor.coerceAtLeast(1f)
        var y = baseline
        for (line in lines) {
            if (y > rect.bottom) break
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
    }

    private fun drawSignature(canvas: Canvas, raw: String, rect: RectF) {
        if (raw.isBlank()) return
        val strokes = parseStrokes(raw)
        if (strokes.isEmpty()) return

        // Compute the source bounding box of the recorded strokes.
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (stroke in strokes) {
            for ((px, py) in stroke) {
                if (px < minX) minX = px
                if (py < minY) minY = py
                if (px > maxX) maxX = px
                if (py > maxY) maxY = py
            }
        }
        if (!minX.isFinite() || !minY.isFinite()) return

        val srcW = (maxX - minX).coerceAtLeast(1f)
        val srcH = (maxY - minY).coerceAtLeast(1f)
        val pad = 4f
        val targetW = (rect.width() - pad * 2).coerceAtLeast(1f)
        val targetH = (rect.height() - pad * 2).coerceAtLeast(1f)
        // Uniform scale so signature keeps its aspect ratio.
        val scale = minOf(targetW / srcW, targetH / srcH)
        val drawnW = srcW * scale
        val drawnH = srcH * scale
        val offsetX = rect.left + (rect.width() - drawnW) / 2f
        val offsetY = rect.top + (rect.height() - drawnH) / 2f

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (rect.height() * 0.05f).coerceIn(1.2f, 3.5f)
        }

        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = AndroidPath()
            val (sx, sy) = stroke.first()
            path.moveTo(offsetX + (sx - minX) * scale, offsetY + (sy - minY) * scale)
            for (i in 1 until stroke.size) {
                val (px, py) = stroke[i]
                path.lineTo(offsetX + (px - minX) * scale, offsetY + (py - minY) * scale)
            }
            canvas.drawPath(path, paint)
        }
    }

    /** Parses the stroke string produced by `signatureStrokesToPath` into raw points. */
    private fun parseStrokes(raw: String): List<List<Pair<Float, Float>>> =
        raw.split("|").mapNotNull { strokeStr ->
            val points = strokeStr.split(",").mapNotNull { pt ->
                val parts = pt.split(":")
                if (parts.size != 2) return@mapNotNull null
                val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
                x to y
            }
            if (points.isNotEmpty()) points else null
        }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (maxWidth <= 0f) return listOf(text)
        val out = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            val words = paragraph.split(' ')
            val current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current.clear()
                    current.append(candidate)
                } else {
                    if (current.isNotEmpty()) out += current.toString()
                    current.clear()
                    if (paint.measureText(word) > maxWidth) {
                        // Hard-break overlong words.
                        val (head, tail) = breakOversize(word, paint, maxWidth)
                        out += head
                        if (tail.isNotEmpty()) current.append(tail)
                    } else {
                        current.append(word)
                    }
                }
            }
            if (current.isNotEmpty()) out += current.toString()
        }
        return out
    }

    private fun breakOversize(word: String, paint: Paint, maxWidth: Float): Pair<String, String> {
        val bounds = Rect()
        var cut = word.length
        while (cut > 1) {
            val sub = word.substring(0, cut)
            paint.getTextBounds(sub, 0, sub.length, bounds)
            if (bounds.width() <= maxWidth) break
            cut--
        }
        return word.substring(0, cut) to word.substring(cut)
    }
}
