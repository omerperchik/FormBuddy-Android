package com.formbuddy.android.ui.components.ios

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS-matching paywall hero (IMG_9046).
 *
 * Mirrors `PaywallView.screenshotsView` from iOS:
 *   - Four columns side-by-side, each with three device screenshots
 *     stacked vertically.
 *   - The whole stack is rotated **-30°** so it appears to fan up-left
 *     from a bottom anchor.
 *   - Each column after the first has a vertical offset (200, 200, 235 dp)
 *     so the columns interlock instead of aligning.
 *   - The composite is masked with a top-down `white→clear` linear
 *     gradient so the staggered ends fade into the violet paywall
 *     background.
 *
 * The PNGs come from
 * `app/src/main/assets/paywall/paywall_device_{1..7}.png`, copied
 * unchanged from the iOS asset catalog.
 */
@Composable
fun PaywallDeviceHero(
    modifier: Modifier = Modifier,
    columnSpacing: Dp = 24.dp,
    columnRotationDegrees: Float = -30f,
    columnTwoOffset: Dp = (-200).dp,
    columnThreeOffset: Dp = (-200).dp,
    columnFourOffset: Dp = (-235).dp
) {
    val devices = rememberPaywallDeviceBitmaps()
    if (devices.size < 7) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .drawWithContent {
                drawContent()
                // Top-to-bottom mask: keep the top vivid, fade to clear at the bottom
                // so the staggered tails dissolve into the violet background.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(columnSpacing)
        ) {
            DeviceColumn(
                bitmaps = listOf(devices[0], devices[1], devices[2]),
                rotationDeg = columnRotationDegrees
            )
            DeviceColumn(
                bitmaps = listOf(devices[0], devices[2], devices[5]),
                rotationDeg = columnRotationDegrees,
                yOffset = columnTwoOffset
            )
            DeviceColumn(
                bitmaps = listOf(devices[1], devices[4], devices[2]),
                rotationDeg = columnRotationDegrees,
                yOffset = columnThreeOffset
            )
            DeviceColumn(
                bitmaps = listOf(devices[0], devices[3], devices[0]),
                rotationDeg = columnRotationDegrees,
                yOffset = columnFourOffset
            )
        }
    }
}

@Composable
private fun DeviceColumn(
    bitmaps: List<ImageBitmap>,
    rotationDeg: Float,
    yOffset: Dp = 0.dp
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = yOffset)
                .graphicsLayer {
                    rotationZ = rotationDeg
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bitmaps.forEach { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Decodes the seven paywall device PNGs from `assets/paywall/` once and
 * caches them. Decoding is on `IO` to keep the first frame cheap.
 */
@Composable
private fun rememberPaywallDeviceBitmaps(): List<ImageBitmap> {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(context) {
        bitmaps = withContext(Dispatchers.IO) {
            (1..7).mapNotNull { idx ->
                runCatching {
                    context.assets.open("paywall/paywall_device_${idx}.png").use { input ->
                        BitmapFactory.decodeStream(input).asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    return bitmaps
}
