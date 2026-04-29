package com.formbuddy.android.ui.components.ios

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * iOS profile-completion card (IMG_9042) — sparkle icon, "Profile Completion"
 * title, percentage on the right (in the accent blue), an animated progress
 * track, a hint sentence, and a tappable CTA pill that jumps to the next
 * unfilled field.
 *
 * `percent` is 0..1; the card also accepts a `nextFieldLabel` to render the
 * "Add: Middle Name >" CTA. Pass `null` to hide the CTA when complete.
 */
@Composable
fun FillinProfileCompletionCard(
    percent: Float,
    hintText: String,
    nextFieldLabel: String?,
    onNextFieldClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = FillinAnimationMs.default),
        label = "profile-progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FillinShapes.default)
            .background(Color(0xFF1C1C1E))
            .padding(FillinSpacing.padding16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(FillinSpacing.padding12))
            Text(
                text = "Profile Completion",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(FillinSpacing.padding12))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(FillinShapes.capsule)
                .background(Color(0xFF2C2C2E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(6.dp)
                    .clip(FillinShapes.capsule)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(Modifier.height(FillinSpacing.padding8))

        Text(
            text = hintText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!nextFieldLabel.isNullOrBlank()) {
            Spacer(Modifier.height(FillinSpacing.padding12))
            FillinPressContainer(
                onClick = onNextFieldClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(FillinShapes.default)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FillinSpacing.padding16),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Add: $nextFieldLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
