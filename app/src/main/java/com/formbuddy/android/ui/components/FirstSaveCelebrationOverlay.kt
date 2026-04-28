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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.formbuddy.android.ui.components.ios.FillinFilledButton
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mirrors iOS `FirstSaveCelebrationOverlay` — a confetti burst with a
 * call-to-action that doubles as the entry point to the Play in-app review.
 *
 * Triggers from the FillingViewModel after the user's first successful save.
 * The animation runs for ~3 s; the dismiss button always appears so the
 * user can opt out immediately.
 */
@Composable
fun FirstSaveCelebrationOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRequestReview: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Confetti()

            Column(
                modifier = Modifier
                    .padding(FillinSpacing.padding24)
                    .fillMaxWidth()
                    .clip(FillinShapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(FillinSpacing.padding24),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(FillinSpacing.padding12))
                Text(
                    text = "First form saved!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(FillinSpacing.padding8))
                Text(
                    text = "Nice work. Loving FormBuddy?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(FillinSpacing.padding24))
                FillinFilledButton(
                    text = "Rate FormBuddy",
                    onClick = {
                        onRequestReview()
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(FillinSpacing.padding8))
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Not now")
                }
            }
        }
    }

    if (isVisible) {
        LaunchedEffect(Unit) {
            // Auto-dismiss the curtain after a few seconds if the user does nothing.
            delay(6_000)
            onDismiss()
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
    val particles = remember {
        List(60) {
            ConfettiParticle(
                seedX = Random.nextFloat(),
                speed = 0.6f + Random.nextFloat() * 0.8f,
                drift = (Random.nextFloat() - 0.5f) * 0.4f,
                hue = listOf(
                    Color(0xFF34C759), Color(0xFFFF9500), Color(0xFFAF52DE),
                    Color(0xFF007AFF), Color(0xFFFF3B30)
                ).random(),
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
