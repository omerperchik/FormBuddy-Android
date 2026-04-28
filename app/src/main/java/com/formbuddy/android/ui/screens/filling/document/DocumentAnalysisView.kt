package com.formbuddy.android.ui.screens.filling.document

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.shape.RoundedCornerShape
import com.formbuddy.android.R
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormPage
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.ui.theme.AddressFieldColor
import com.formbuddy.android.ui.theme.CheckboxFieldColor
import com.formbuddy.android.ui.theme.DateFieldColor
import com.formbuddy.android.ui.theme.EmailFieldColor
import com.formbuddy.android.ui.theme.NumberFieldColor
import com.formbuddy.android.ui.theme.PhoneFieldColor
import com.formbuddy.android.ui.theme.SignatureFieldColor
import com.formbuddy.android.ui.theme.TextFieldColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Mirrors iOS `DocumentAnalysisView` — renders each PDF page as a bitmap and overlays
 * the field bounding boxes in their type-specific colors. Tapping a rectangle selects
 * the field, which the parent uses to scroll the form-list view to that field.
 *
 * The PDF is loaded once into the cache, then [PdfRenderer] streams pages on demand.
 */
@Composable
fun DocumentAnalysisView(
    template: FormTemplate,
    documentBytes: ByteArray?,
    selectedFieldId: String? = null,
    onFieldSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val pageBitmaps = remember(documentBytes) { mutableStateOf<List<Bitmap>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(documentBytes) {
        if (documentBytes == null) return@LaunchedEffect
        scope.launch {
            withContext(Dispatchers.IO) {
                val tmp = File.createTempFile("doc_view_", ".pdf", context.cacheDir).apply {
                    writeBytes(documentBytes)
                }
                val fd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val out = ArrayList<Bitmap>(renderer.pageCount)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    // Render at ~150 DPI-ish; PDF coordinates are 72 DPI, so 2x is fine for legibility.
                    val scale = 2
                    val bmp = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    ).apply { eraseColor(android.graphics.Color.WHITE) }
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    out += bmp
                }
                renderer.close()
                fd.close()
                tmp.delete()
                pageBitmaps.value = out
            }
        }
    }

    if (pageBitmaps.value.isEmpty() && documentBytes != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.filling_processing),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.document_zoom_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(template.pages, key = { it.index }) { page ->
                val bitmap = pageBitmaps.value.getOrNull(page.index)
                if (bitmap != null) {
                    PageWithOverlay(
                        bitmap = bitmap,
                        page = page,
                        selectedFieldId = selectedFieldId,
                        onFieldSelected = onFieldSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun PageWithOverlay(
    bitmap: Bitmap,
    page: FormPage,
    selectedFieldId: String?,
    onFieldSelected: (String) -> Unit
) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        // Overlay rectangles + tap targets, scaled to the box size.
        var boxSize by remember { mutableStateOf(Size.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(page.fields, boxSize) {
                    detectTapGestures(onTap = { offset ->
                        val w = boxSize.width
                        val h = boxSize.height
                        if (w == 0f || h == 0f) return@detectTapGestures
                        val hit = page.fields.firstOrNull { field ->
                            val box = field.contentBoundingBox
                            val left = box.x.toFloat() * w
                            val top = box.y.toFloat() * h
                            val right = left + box.width.toFloat() * w
                            val bottom = top + box.height.toFloat() * h
                            offset.x in left..right && offset.y in top..bottom
                        }
                        hit?.let { onFieldSelected(it.id) }
                    })
                }
                .drawWithContent {
                    drawContent()
                    boxSize = size
                    page.fields.forEach { field ->
                        val color = field.fieldType.overlayColor()
                        val box = field.contentBoundingBox
                        val left = box.x.toFloat() * size.width
                        val top = box.y.toFloat() * size.height
                        val w = box.width.toFloat() * size.width
                        val h = box.height.toFloat() * size.height
                        if (w <= 0 || h <= 0) return@forEach
                        val isSelected = field.id == selectedFieldId
                        val strokeWidth = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx()
                        drawRect(
                            color = color.copy(alpha = if (isSelected) 0.18f else 0.10f),
                            topLeft = Offset(left, top),
                            size = Size(w, h)
                        )
                        drawRect(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
        )
    }
}

private fun FieldType.overlayColor(): Color = when (this) {
    FieldType.TEXT -> TextFieldColor
    FieldType.NUMBER -> NumberFieldColor
    FieldType.EMAIL -> EmailFieldColor
    FieldType.PHONE -> PhoneFieldColor
    FieldType.CHECKBOX -> CheckboxFieldColor
    FieldType.DATE -> DateFieldColor
    FieldType.SIGNATURE -> SignatureFieldColor
    FieldType.ADDRESS -> AddressFieldColor
}
