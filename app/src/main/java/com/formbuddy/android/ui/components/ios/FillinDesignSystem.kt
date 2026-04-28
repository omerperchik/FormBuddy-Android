package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens lifted directly from the iOS app's CoreUI package
 * (`CGFloat+Extensions.swift`). Keep these values in lockstep with iOS so any
 * shared snapshot test diffs are caused by real visual divergence, not by
 * unsynced spacing constants.
 */
object FillinSpacing {
    val padding2 = 2.dp
    val padding4 = 4.dp
    val padding6 = 6.dp
    val padding8 = 8.dp
    val padding10 = 10.dp
    val padding12 = 12.dp
    val padding16 = 16.dp
    val padding24 = 24.dp
    val sectionPadding = 32.dp
    val padding32 = 32.dp
    val padding44 = 44.dp
}

object FillinShapes {
    val field: Shape = RoundedCornerShape(4.dp)
    val default: Shape = RoundedCornerShape(12.dp)
    val large: Shape = RoundedCornerShape(24.dp)
    val capsule: Shape = RoundedCornerShape(percent = 50)
}

object FillinSeparatorWidth {
    val thin: Dp = 0.5.dp
    val thick: Dp = 1.dp
}

object FillinAnimationMs {
    const val default = 300
    const val fast = 150
}

object FillinDimming {
    const val low = 0.15f
    const val medium = 0.4f
    const val heavy = 0.65f
}

/** Provides iOS-platform-look behaviour decisions to components in the tree. */
data class FillinPlatformStyle(
    /** Disables the Material ripple in favour of the iOS scale-press animation. */
    val noRipple: Boolean = true,
    /** Use SF-Pro-like font weights (slightly heavier than Material defaults). */
    val sfProWeights: Boolean = true,
    /** Sticky-bottom buttons sit at 24dp inset on iOS rather than Material 16dp. */
    val bottomActionInset: Dp = FillinSpacing.padding24
)

val LocalFillinPlatformStyle = staticCompositionLocalOf { FillinPlatformStyle() }

@Composable
fun fillinStyle(): FillinPlatformStyle = LocalFillinPlatformStyle.current
