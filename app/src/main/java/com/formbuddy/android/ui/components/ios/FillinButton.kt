package com.formbuddy.android.ui.components.ios

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

/**
 * iOS-style filled button.
 *
 * Mirrors `TapScaleButtonStyle` from CoreUI: the button scales to ~0.96 while
 * pressed (over `fastAnimationDuration` = 150ms), with no Material ripple and
 * no elevation. iOS uses SF Semibold body weight by default for action labels.
 */
@Composable
fun FillinFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    height: androidx.compose.ui.unit.Dp = 50.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = FillinSpacing.padding24)
) {
    FillinPressContainer(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(FillinShapes.default)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text)
                }
            }
        }
    }
}

/** iOS-style outlined button — same press behaviour, hairline border, accent text. */
@Composable
fun FillinOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    FillinPressContainer(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(FillinShapes.default)
            .border(FillinSeparatorWidth.thick, contentColor, FillinShapes.default)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text)
                }
            }
        }
    }
}

/** iOS plain text button — accent-tinted, scale-on-press, no chrome. */
@Composable
fun FillinTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    FillinPressContainer(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .clip(FillinShapes.default)
            .padding(horizontal = FillinSpacing.padding8, vertical = FillinSpacing.padding4)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/** Circular icon button styled like iOS toolbar items. */
@Composable
fun FillinIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FillinPressContainer(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .clip(CircleShape)
            .padding(FillinSpacing.padding6)
    ) {
        content()
    }
}

/**
 * Bare press container that gives iOS-style scale-on-press feedback without a
 * Material ripple. Children supply visuals (background, border, content).
 *
 * Implementation notes:
 *   - We use `clickable(indication = null)` so no ripple draws.
 *   - The press state is driven by [MutableInteractionSource] so accessibility
 *     focus + click-on-keyboard still work normally.
 *   - Scale animates to 0.96 over 150 ms (matching iOS `fastAnimationDuration`).
 */
@Composable
fun FillinPressContainer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = FillinAnimationMs.fast),
        label = "FillinPressScale"
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
