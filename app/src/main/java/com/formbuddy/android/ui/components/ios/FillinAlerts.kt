package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.formbuddy.android.ui.theme.DestructiveRed
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iOS-style alert dialog (IMG_9062 — "Are you sure you want to close this
 * document?").
 *
 * Visual specifics:
 *   - Translucent square-cornered card (~14 dp radius), 270 dp wide,
 *     centered.
 *   - Title in semibold body, message below in body small.
 *   - Hairline divider above the buttons + between buttons.
 *   - Two side-by-side buttons: leading = primary (blue, semibold) or
 *     destructive (red, regular), trailing = neutral cancel (blue, regular).
 *
 *   ┌────────────────┐
 *   │   Title (bold) │
 *   │   message...   │
 *   ├────────────────┤
 *   │  Save │ Discard│
 *   └────────────────┘
 */
@Composable
fun FillinAlertDialog(
    title: String,
    message: String,
    primaryButtonText: String,
    onPrimary: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondary: (() -> Unit)? = null,
    isPrimaryDestructive: Boolean = false,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(270.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(Color(0xFF2C2C2E).copy(alpha = 0.95f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFF3A3A3C))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AlertButton(
                        text = primaryButtonText,
                        color = if (isPrimaryDestructive) DestructiveRed else IMessageBlue,
                        bold = true,
                        onClick = { onPrimary(); onDismiss() },
                        modifier = Modifier.weight(1f)
                    )
                    if (secondaryButtonText != null && onSecondary != null) {
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(44.dp)
                                .background(Color(0xFF3A3A3C))
                        )
                        AlertButton(
                            text = secondaryButtonText,
                            color = if (isPrimaryDestructive) IMessageBlue else DestructiveRed,
                            bold = false,
                            onClick = { onSecondary(); onDismiss() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertButton(
    text: String,
    color: Color,
    bold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FillinPressContainer(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
