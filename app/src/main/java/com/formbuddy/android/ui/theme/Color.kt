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

// iOS dark mode is pure black (#000) for OLED — confirmed against IMG_9039+.
val DarkBackground = Color(0xFF000000)
// `secondarySystemBackground` in iOS dark — used for grouped-list cards.
val DarkSurface = Color(0xFF1C1C1E)
// `tertiarySystemBackground` in iOS dark — used for nested tiles.
val DarkSurfaceVariant = Color(0xFF2C2C2E)
// "Free Plan" card background and other elevated surfaces.
val DarkSurfaceElevated = Color(0xFF1C1C1E)
// Hairline-separator color used inside grouped lists.
val DarkSeparator = Color(0xFF38383A)
// "PRO" badge purple — matches the bright iOS Pro accent in screenshots.
val ProBadgePurple = Color(0xFFA855F7)

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

// ── iOS-screenshot-derived tokens (IMG_9039 … IMG_9067) ──────────────────────
// These were measured directly from screenshots so the Compose tree renders the
// same way iOS does on a Pixel/Galaxy in dark mode (the only mode shown).

// Primary CTA blue — pulled from the "+ Add your first document" pill and the
// FAB. Slightly brighter than the base iOS systemBlue.
val CtaBlue            = Color(0xFF3F7AF7)
val CtaBlueDimmed      = Color(0xFF2C5BC7)

// Profile-segment & sent-bubble blue — the iMessage-style "you" bubble.
val IMessageBlue       = Color(0xFF1183FE)

// Tab bar pill background (`UIBlurEffect(.systemMaterialDark)` flattened).
val TabBarPill         = Color(0xFF1C1C1E)
val TabBarPillSelected = Color(0xFF2C2C2E)

// Grouped-list cards on the home/profile/settings screens.
val GroupedCard        = Color(0xFF1C1C1E)
val GroupedCardElev    = Color(0xFF2C2C2E)

// Settings "Free Plan" card outline (subtle violet glow).
val FreePlanOutline    = Color(0xFF3A2A66)

// Confetti / celebration palette (IMG_9063).
val ConfettiBlue   = Color(0xFF2D7BF5)
val ConfettiGreen  = Color(0xFF34C759)
val ConfettiYellow = Color(0xFFFFCC00)
val ConfettiPink   = Color(0xFFFF3B7B)
val ConfettiViolet = Color(0xFFAF52DE)

// Pro gradient (paywall hero, IMG_9046) — top→bottom violet on dark.
val PaywallTop    = Color(0xFF6B4FE0)
val PaywallBottom = Color(0xFF3A1F9C)

// Profile completion progress fill (IMG_9042).
val CompletionFill = Color(0xFF1183FE)

// "Time saved" number color (IMG_9064).
val TimeSavedGreen = Color(0xFF34C759)

// Destructive / "Clear" link (IMG_9045).
val DestructiveRed = Color(0xFFEB5347)
