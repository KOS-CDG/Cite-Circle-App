package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Cite Circle palette -- "lab notebook at a party".
 *
 * Vivid indigo carries the brand, raspberry drives social actions (reactions, alerts), and amber
 * is the warm accent kept from the previous editorial theme. Backgrounds are violet-tinted rather
 * than neutral grey, in both schemes: the dark theme is a deep indigo-navy, never a flat #121212,
 * so it reads as a deliberate color rather than an absence of one.
 *
 * Every (onX, X) pairing below is verified by ThemeContrastTest, which asserts WCAG AA (4.5:1) for
 * text roles and 3:1 for outlines -- including body text against all five surfaceContainer tiers.
 *
 * Usage rule: `secondary` and `tertiary` are FILL-ONLY roles. Against white they measure 2.99:1
 * and 2.15:1 respectively, so they are legal for chips, icons, bars, borders and large display
 * type, but never for running academic prose. Same for every gradient in Gradients.kt.
 */

// ---------------------------------------------------------------- light

val IndigoPrimary = Color(0xFF5B3DF5)
val IndigoOnPrimary = Color(0xFFFFFFFF)
val IndigoContainer = Color(0xFFE6E0FF)
val IndigoOnContainer = Color(0xFF170065)

val RaspberrySecondary = Color(0xFFD81E5B)
val RaspberryOnSecondary = Color(0xFFFFFFFF)
val RaspberryContainer = Color(0xFFFFD9E2)
val RaspberryOnContainer = Color(0xFF3F0018)

val AmberTertiary = Color(0xFFFFB300)
val AmberOnTertiary = Color(0xFF2B1700)
val AmberContainer = Color(0xFFFFE9B0)
val AmberOnContainer = Color(0xFF3D2A00)

val LightBackground = Color(0xFFF3F0FA)
val LightOnBackground = Color(0xFF1B1B22)
val LightSurface = Color(0xFFFFFDFF)
val LightOnSurface = Color(0xFF1B1B22)
val LightSurfaceVariant = Color(0xFFE5E0EC)
val LightOnSurfaceVariant = Color(0xFF4A4458)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFAF7FF)
val LightSurfaceContainer = Color(0xFFF4F0FA)
val LightSurfaceContainerHigh = Color(0xFFEEEAF5)
val LightSurfaceContainerHighest = Color(0xFFE8E4EF)
val LightSurfaceDim = Color(0xFFDED8E8)
val LightSurfaceBright = Color(0xFFFFFDFF)

val LightOutline = Color(0xFF7B7488)
val LightOutlineVariant = Color(0xFFCBC4D4)
val LightInverseSurface = Color(0xFF303038)
val LightInverseOnSurface = Color(0xFFF3EFF7)
val LightInversePrimary = Color(0xFFC7BCFF)

// ---------------------------------------------------------------- dark

val IndigoPrimaryDark = Color(0xFFC7BCFF)
val IndigoOnPrimaryDark = Color(0xFF2C0F87)
val IndigoContainerDark = Color(0xFF44309E)
val IndigoOnContainerDark = Color(0xFFE6E0FF)

val RaspberrySecondaryDark = Color(0xFFFFB1C4)
val RaspberryOnSecondaryDark = Color(0xFF5E1133)
val RaspberryContainerDark = Color(0xFFA4224B)
val RaspberryOnContainerDark = Color(0xFFFFD9E2)

val AmberTertiaryDark = Color(0xFFFFC94A)
val AmberOnTertiaryDark = Color(0xFF3D2A00)
val AmberContainerDark = Color(0xFF6B4A00)
val AmberOnContainerDark = Color(0xFFFFE9B0)

val DarkBackground = Color(0xFF121016)
val DarkOnBackground = Color(0xFFE7E1EC)
val DarkSurface = Color(0xFF121016)
val DarkOnSurface = Color(0xFFE7E1EC)
val DarkSurfaceVariant = Color(0xFF48434F)
val DarkOnSurfaceVariant = Color(0xFFCAC3D4)

val DarkSurfaceContainerLowest = Color(0xFF0C0A10)
val DarkSurfaceContainerLow = Color(0xFF1A1720)
val DarkSurfaceContainer = Color(0xFF1E1B25)
val DarkSurfaceContainerHigh = Color(0xFF29252F)
val DarkSurfaceContainerHighest = Color(0xFF34303A)
val DarkSurfaceDim = Color(0xFF121016)
val DarkSurfaceBright = Color(0xFF38343E)

val DarkOutline = Color(0xFF948E9E)
val DarkOutlineVariant = Color(0xFF48434F)
val DarkInverseSurface = Color(0xFFE7E1EC)
val DarkInverseOnSurface = Color(0xFF322F38)
val DarkInversePrimary = Color(0xFF5B3DF5)

// ---------------------------------------------------------------- shared

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val Scrim = Color(0xFF000000)

/** Semantic status colors, used by notifications, connection state and delivery receipts. */
val SuccessGreen = Color(0xFF15803D)
val SuccessGreenDark = Color(0xFF7BE0A2)
val WarningAmber = Color(0xFFB45309)
val WarningAmberDark = Color(0xFFFFC94A)
val PresenceOnline = Color(0xFF22C55E)
