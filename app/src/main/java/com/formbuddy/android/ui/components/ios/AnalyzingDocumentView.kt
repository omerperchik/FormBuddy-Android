package com.formbuddy.android.ui.components.ios

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.formbuddy.android.ui.theme.IMessageBlue
import kotlinx.coroutines.delay

/**
 * Mirrors iOS `AnalyzingDocumentView` exactly — the centerpiece is the same
 * `analyzing_doc_dark.json` Lottie that ships with the iOS app
 * (`Fillin/SupportingFiles/Lottie/analyzing_doc_dark.json`). We copy that
 * file into the Android `assets/lottie/` folder so the two platforms render
 * frame-for-frame the same animation.
 *
 * Layout:
 *   - Pure-black full-screen background.
 *   - Cancel pill in the top-right.
 *   - Centered Lottie (220-260 dp tall) playing the analyzing-doc sequence.
 *   - Bottom rounded card with title, dynamic phase subtitle, an
 *     indeterminate progress bar, and the standard hint about
 *     backgrounding being safe.
 */
@Composable
fun AnalyzingDocumentOverlay(
    isVisible: Boolean,
    onCancel: () -> Unit
) {
    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Cancel pill — top-right.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16)
        ) {
            FillinPressContainer(
                onClick = onCancel,
                modifier = Modifier
                    .clip(FillinShapes.capsule)
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding8)
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Centered Lottie — same file as iOS, so animation matches frame for frame.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 96.dp)
                .size(280.dp)
        ) {
            AnalyzingLottie()
        }

        // Bottom progress card.
        AnalyzingProgressCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(FillinSpacing.padding16)
        )
    }
}

@Composable
private fun AnalyzingLottie() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/analyzing_doc_dark.json")
    )
    if (composition == null) {
        // Asset missing — render nothing rather than a fallback. The bottom
        // progress card already communicates "we're working".
        return
    }
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun AnalyzingProgressCard(modifier: Modifier = Modifier) {
    val phaseLabels = remember {
        listOf(
            "Capturing document…",
            "Detecting fields…",
            "Drafting questions…",
            "Almost there…"
        )
    }
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            phase = (phase + 1) % phaseLabels.size
        }
    }

    val infinite = rememberInfiniteTransition(label = "progress")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing)),
        label = "progress-anim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = FillinSpacing.padding16, vertical = FillinSpacing.padding16)
    ) {
        Text(
            text = "Analyzing Document",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(FillinSpacing.padding8))
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "phase-text"
        ) { p ->
            Text(
                text = phaseLabels[p],
                color = Color(0xFF9C9CA1),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(FillinSpacing.padding8))
        IndeterminateProgressBar(progress = progress)
        Spacer(Modifier.height(FillinSpacing.padding12))
        Text(
            text = "You can safely send the app to the background",
            color = Color(0xFF6C6C72),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * Thin indeterminate-style track: a 30%-wide segment that slides across.
 * Implemented with [BoxWithConstraints] so we can convert the [0..1] phase
 * into a real Dp x-offset against the measured track width.
 */
@Composable
private fun IndeterminateProgressBar(progress: Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E))
    ) {
        val segmentFraction = 0.3f
        val trackWidth = maxWidth
        val segmentWidth = trackWidth * segmentFraction
        val xOffset = trackWidth * (progress * (1f + segmentFraction)) - segmentWidth
        Box(
            modifier = Modifier
                .offset(x = xOffset)
                .width(segmentWidth)
                .height(3.dp)
                .clip(CircleShape)
                .background(IMessageBlue)
        )
    }
}
