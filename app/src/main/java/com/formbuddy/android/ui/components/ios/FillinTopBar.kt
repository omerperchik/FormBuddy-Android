package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * iOS-style navigation bar.
 *
 * - 44 pt content height (the iOS standard) plus status-bar inset.
 * - Title centred, semibold, 17 pt equivalent.
 * - Leading button uses chevron-left + accent-colored title text per
 *   `Apple HIG`. Trailing button is accent-colored text or icon.
 * - No Material 3 elevation/shadow; iOS bars sit flush on the surface.
 */
@Composable
fun FillinTopBar(
    title: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.background(background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(44.dp)
        ) {
            // Leading
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = FillinSpacing.padding8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    onBack != null -> FillinPressContainer(
                        onClick = onBack,
                        modifier = Modifier.padding(horizontal = FillinSpacing.padding4)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = FillinSpacing.padding4)
                            )
                            backLabel?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    leading != null -> leading()
                }
            }

            // Title
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Trailing
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = FillinSpacing.padding8),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailing?.invoke()
            }
        }
        // iOS hairline separator — only when content scrolls underneath; for a clean
        // baseline we draw it always so screens feel consistent.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FillinSeparatorWidth.thin)
                .background(MaterialTheme.colorScheme.outline)
        )
    }
}

/**
 * iOS large-title bar variant.
 * The title is rendered at displayMedium weight and aligned leading. iOS shows
 * the small-title bar above it with the same content; for simplicity we render
 * just the large header and let callers draw a [FillinTopBar] above when they
 * want both layers.
 */
@Composable
fun FillinLargeTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier.padding(
            horizontal = FillinSpacing.padding16,
            vertical = FillinSpacing.padding8
        ),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}
