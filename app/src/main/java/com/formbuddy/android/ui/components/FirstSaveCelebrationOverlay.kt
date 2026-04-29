package com.formbuddy.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.theme.ConfettiBlue
import com.formbuddy.android.ui.theme.ConfettiGreen
import com.formbuddy.android.ui.theme.ConfettiPink
import com.formbuddy.android.ui.theme.ConfettiViolet
import com.formbuddy.android.ui.theme.ConfettiYellow
import com.formbuddy.android.ui.theme.IMessageBlue
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * iOS-matching first-save celebration (IMG_9063).
 *
 * Visual:
 *   - Top status-bar-inset RIBBON pill with green check, "Nice work!"
 *     headline, subtitle "You've just saved your first document.", and a
 *     blue "Done" link on the right.
 *   - Confetti exploding from the ribbon outward, falling across the screen
 *     for ~3.5 s.
 *   - Tapping anywhere outside the ribbon dismisses; tapping "Done"
 *     fires the in-app review prompt.
 *
 * Replaces the older centered-dialog version that didn't match iOS.
 */
@Composable
fun FirstSaveCelebrationOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRequestReview: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
        ) {
            // Confetti behind everything, ignoring touches.
            Confetti()

            // Top ribbon — slides in from above the status bar.
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8)
            ) {
                Ribbon(
                    onDone = {
                        onRequestReview()
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
            }

            // Tap-anywhere-else dismisser.
            FillinPressContainer(
                onClick = onDismiss,
                modifier = Modifier.fillMaxSize()
            ) { /* invisible */ }
        }
    }

    if (isVisible) {
        LaunchedEffect(Unit) {
            delay(6_000)
            onDismiss()
        }
    }
}

@Composable
private fun Ribbon(onDone: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FillinShapes.capsule)
            .background(Color(0xFF1F1F1F))
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ConfettiGreen.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = ConfettiGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(FillinSpacing.padding12))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nice work! 🎉",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "You've just saved your first document.",
                color = Color(0xFF9C9CA1),
                style = MaterialTheme.typography.bodySmall
            )
        }
        FillinPressContainer(onClick = onDone, modifier = Modifier.padding(start = FillinSpacing.padding8)) {
            Text(
                text = "Done",
                color = IMessageBlue,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun Confetti() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_500, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiT"
    )
    val palette = remember {
        listOf(ConfettiBlue, ConfettiGreen, ConfettiYellow, ConfettiPink, ConfettiViolet)
    }
    val particles = remember {
        List(60) {
            ConfettiParticle(
                seedX = Random.nextFloat(),
                speed = 0.5f + Random.nextFloat() * 0.8f,
                drift = (Random.nextFloat() - 0.5f) * 0.4f,
                hue = palette.random(),
                size = 4f + Random.nextFloat() * 6f
            )
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (p in particles) {
            val phase = (t + p.seedX) % 1f
            val cx = (p.seedX + p.drift * phase) * size.width
            val cy = phase * size.height * p.speed
            val rotation = phase * 4 * PI
            val rx = (p.size * cos(rotation)).toFloat()
            val ry = (p.size * sin(rotation)).toFloat()
            drawRect(
                color = p.hue,
                topLeft = Offset(cx - rx, cy - ry),
                size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.6f)
            )
        }
    }
}

private data class ConfettiParticle(
    val seedX: Float,
    val speed: Float,
    val drift: Float,
    val hue: Color,
    val size: Float
)
