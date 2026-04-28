package com.formbuddy.android.ui.theme

import androidx.compose.ui.graphics.Color

// Accent (matches iOS AccentColor.colorset)
// Light: display-p3 (0.212, 0.384, 0.894) ~= sRGB #3662E4
// Dark: display-p3 (90, 132, 247) ~= sRGB #5A84F7
val AccentLight = Color(0xFF3662E4)
val AccentDark = Color(0xFF5A84F7)

// Convenience aliases used across the codebase
val Primary = AccentLight
val PrimaryLight = Color(0xFF7A9DEF)
val PrimaryDark = Color(0xFF1F3FAE)
val PrimaryContainer = Color(0xFFD9E2FB)

// Pro / premium gradient anchor (iOS Pro.colorset: #743EE4)
val ProColor = Color(0xFF743EE4)
val Secondary = ProColor
val SecondaryContainer = Color(0xFFE9DDFA)

// Neutral surfaces (iOS uses systemBackground / secondarySystemBackground)
val Background = Color(0xFFF2F2F7)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF6F6F8)

val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF2C2C2E)

// Field-type colors — exact match to iOS Assets.xcassets/Colors/*.colorset
val TextFieldColor      = Color(0xFF007AFF) // TextFormField
val NumberFieldColor    = Color(0xFF08494F) // NumberFormField (display-p3 → sRGB approx)
val EmailFieldColor     = Color(0xFF742041) // EmailFormField  (display-p3 → sRGB approx)
val PhoneFieldColor     = Color(0xFFAA733F) // PhoneFormField  (display-p3 → sRGB approx)
val CheckboxFieldColor  = Color(0xFFFF9500) // CheckboxFormField
val DateFieldColor      = Color(0xFFAF52DE) // DateFormField
val SignatureFieldColor = Color(0xFFFF3B30) // SignatureFormField
val AddressFieldColor   = Color(0xFF34C759) // AddressFormField
val WatermarkColor      = Color(0xFF888888) // Watermark

// Status (iOS ErrorRed.colorset: #EB5347)
val Success = Color(0xFF34C759)
val Warning = Color(0xFFFF9500)
val Error   = Color(0xFFEB5347)

// Text — iOS Color.primary / Color.secondary on iOS map to system label colors
val TextPrimary   = Color(0xFF000000)
val TextSecondary = Color(0xFF3C3C43).copy(alpha = 0.6f)
val TextTertiary  = Color(0xFF3C3C43).copy(alpha = 0.3f)
val TextOnPrimary = Color(0xFFFFFFFF)

val DarkTextPrimary   = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFEBEBF5).copy(alpha = 0.6f)
val DarkTextTertiary  = Color(0xFFEBEBF5).copy(alpha = 0.3f)

// System grays (iOS UIColor.systemGray*)
val SystemGray  = Color(0xFF8E8E93)
val SystemGray2 = Color(0xFFAEAEB2)
val SystemGray3 = Color(0xFFC7C7CC)
val SystemGray4 = Color(0xFFD1D1D6)
val SystemGray5 = Color(0xFFE5E5EA)
val SystemGray6 = Color(0xFFF2F2F7)

val Separator = Color(0xFF3C3C43).copy(alpha = 0.29f)
