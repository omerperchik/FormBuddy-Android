package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * iOS-style card / list-row container.
 *
 * Differs from Material `Card` in three ways that matter for fidelity:
 *   1. No tonal elevation, no drop shadow.
 *   2. Uses iOS `secondarySystemBackground` color (slightly off-white in light
 *      mode, near-black in dark) instead of Material `surface`.
 *   3. 12 dp corner radius matches `defaultCornerRadius` in CoreUI.
 */
@Composable
fun FillinCard(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    contentPadding: androidx.compose.ui.unit.Dp = FillinSpacing.padding16,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(FillinShapes.default)
            .background(background)
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * iOS-style grouped list section.
 *
 * Layout matches the `List(.insetGrouped)` look used throughout iOS Settings:
 * a small caption-cased title, then a single rounded container with hairline
 * separators between rows.
 */
@Composable
fun FillinSection(
    title: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    rows: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it.uppercase(),
                modifier = Modifier.padding(
                    start = FillinSpacing.padding16,
                    end = FillinSpacing.padding16,
                    top = FillinSpacing.padding16,
                    bottom = FillinSpacing.padding6
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FillinSpacing.padding16)
                .clip(FillinShapes.default)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            rows()
        }
        footer?.let {
            Text(
                text = it,
                modifier = Modifier.padding(
                    start = FillinSpacing.padding16,
                    end = FillinSpacing.padding16,
                    top = FillinSpacing.padding6,
                    bottom = FillinSpacing.padding16
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Hairline separator between rows in a [FillinSection]. */
@Composable
fun FillinSeparator(modifier: Modifier = Modifier, inset: androidx.compose.ui.unit.Dp = FillinSpacing.padding16) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(FillinSeparatorWidth.thin)
            .background(MaterialTheme.colorScheme.outline)
    )
}

/** Standard list row used inside a [FillinSection]. */
@Composable
fun FillinRow(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12)
    val rowContent: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(FillinSpacing.padding12),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            leading?.invoke()
            Box(modifier = Modifier.weight(1f)) { content() }
            trailing?.invoke()
        }
    }
    if (onClick != null) {
        FillinPressContainer(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) { rowContent() }
    } else {
        rowContent()
    }
}
