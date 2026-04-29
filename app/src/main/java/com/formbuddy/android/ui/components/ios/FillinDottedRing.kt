package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dotted/dashed circular ring that surrounds the centered hero icon on the
 * Docs empty state in the iOS app (IMG_9039). Renders the dashed border on
 * a Canvas because Compose doesn't ship a dashed circle modifier.
 */
@Composable
fun FillinDottedRing(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    color: Color = Color(0xFF3A3A3C),
    strokeWidth: Dp = 1.5.dp,
    dashOn: Dp = 2.dp,
    dashOff: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sw = strokeWidth.toPx()
            drawCircle(
                color = color,
                radius = (this.size.minDimension - sw) / 2f,
                center = Offset(this.size.width / 2f, this.size.height / 2f),
                style = Stroke(
                    width = sw,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashOn.toPx(), dashOff.toPx()),
                        0f
                    )
                )
            )
        }
        content()
    }
}

/** Background size helper to make Box use the full circle bounds. */
@Suppress("UNUSED_PARAMETER")
private fun unused(s: Size) = Unit
