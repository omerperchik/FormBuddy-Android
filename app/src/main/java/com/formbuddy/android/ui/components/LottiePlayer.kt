package com.formbuddy.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Wraps `LottieAnimation` so the rest of the app can stay agnostic to the lib.
 *
 * Usage:
 * ```
 * LottiePlayer(spec = LottieCompositionSpec.Asset("animations/celebrate.json"))
 * ```
 *
 * If the asset is missing the composition fails to load and we render nothing
 * — that way we can ship the player wired up even before designers deliver
 * the final `.json` files. iOS uses `Lottie` from Airbnb's library too, so the
 * source `.lottie` files can be reused 1:1 across platforms.
 */
@Composable
fun LottiePlayer(
    spec: LottieCompositionSpec,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    size: Dp = 96.dp
) {
    val compositionResult = rememberLottieComposition(spec)
    val composition = compositionResult.value
    if (composition != null) {
        LottieAnimation(
            composition = composition,
            iterations = iterations,
            modifier = modifier.size(size)
        )
    }
}
