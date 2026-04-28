package com.formbuddy.android.ui.components.ios

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * iOS `UISegmentedControl`-style picker.
 *
 * Draws a single capsule track with a sliding capsule "thumb" behind the
 * selected option. Animates width-based offset for the thumb so transitions
 * look like the native iOS one.
 */
@Composable
fun FillinSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(FillinShapes.capsule)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
    ) {
        val totalWidth = maxWidth - 4.dp
        val itemWidth = if (options.isNotEmpty()) totalWidth / options.size else totalWidth
        val thumbOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = tween(durationMillis = FillinAnimationMs.default),
            label = "FillinSegmentedThumb"
        )

        // Animated thumb behind the labels.
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(FillinShapes.capsule)
                .background(MaterialTheme.colorScheme.surface)
        )

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                FillinPressContainer(
                    onClick = { onSelected(index) },
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (index == selectedIndex)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
