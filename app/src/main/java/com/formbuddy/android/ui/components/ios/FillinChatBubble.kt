package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.formbuddy.android.ui.theme.IMessageBlue

/**
 * iMessage-style asymmetric bubble — three corners are heavily rounded
 * (~22 dp radius) and the corner adjacent to the speaker tail uses a
 * shallower radius (~6 dp) so the bubble visibly "leans" toward the tail.
 *
 * Mirrors `Fillin/Packages/CoreUI/Sources/CoreUI/Shapes/ChatBubbleShape.swift`.
 *
 * Implemented as a custom [Shape] so we can convert Dp→Px via the supplied
 * [Density] without needing a Composable scope.
 */
private class ChatBubbleShape(private val isSent: Boolean) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { 22.dp.toPx() }
        val tail = with(density) { 6.dp.toPx() }
        val brRadius = if (isSent) tail else r
        val blRadius = if (isSent) r else tail
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    topLeft = CornerRadius(r),
                    topRight = CornerRadius(r),
                    bottomRight = CornerRadius(brRadius),
                    bottomLeft = CornerRadius(blRadius)
                )
            )
        }
        return Outline.Generic(path)
    }
}

private fun chatBubbleShape(isSent: Boolean): Shape = ChatBubbleShape(isSent)

/**
 * iOS-style chat bubble with asymmetric corner-radius "tail".
 *
 * Sent bubbles are right-aligned in [IMessageBlue] with white text and a
 * shallow bottom-right corner (the tail). Received bubbles are
 * left-aligned in dark gray with white text and a shallow bottom-left
 * corner.
 */
@Composable
fun FillinChatBubble(
    text: String,
    isSent: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding4),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(chatBubbleShape(isSent))
                .background(if (isSent) IMessageBlue else Color(0xFF3A3A3C))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Suggestion chip rendered under a received bubble (mirrors iOS
 * "Suggestion from your profile: Omer Perchik" with an Accept link).
 */
@Composable
fun FillinSuggestionChip(
    suggestion: String,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding4),
        horizontalArrangement = Arrangement.End
    ) {
        FillinPressContainer(
            onClick = onAccept,
            modifier = Modifier
                .clip(FillinShapes.capsule)
                .background(Color(0xFF3A3A3C))
                .padding(horizontal = FillinSpacing.padding12, vertical = FillinSpacing.padding6)
        ) {
            Text(
                text = "Use \"$suggestion\"",
                color = IMessageBlue,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
