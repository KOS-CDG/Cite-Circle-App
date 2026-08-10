package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A bright, professional-network palette in the register of LinkedIn and Facebook: white
 * content surfaces floating on a warm neutral page, one confident brand blue, and two
 * sparingly-used semantic accents.
 *
 * Every colour here is a flat value. Gradients are deliberately absent — depth comes from
 * surface elevation and spacing, never from a blend.
 */

// --- brand ------------------------------------------------------------------
val BrandBlue = Color(0xFF0A66C2)
val BrandBluePressed = Color(0xFF09539E)
/** Lifted for legibility on dark surfaces, where the 0xFF0A66C2 blue fails contrast. */
val BrandBlueOnDark = Color(0xFF71B7FB)

// --- light surfaces ---------------------------------------------------------
/** The page itself. Cards sit on top of this, which is what creates the feed's rhythm. */
val PageNeutral = Color(0xFFF4F2EE)
val SurfaceWhite = Color(0xFFFFFFFF)
/** Inset blocks: quoted posts, skeleton bones, secondary panels. */
val SurfaceInset = Color(0xFFEDEDED)
val TextPrimaryLight = Color(0xFF191919)
val TextSecondaryLight = Color(0xFF5E5E5E)
val DividerLight = Color(0xFFE0DFDC)

// --- dark surfaces ----------------------------------------------------------
val PageDark = Color(0xFF1B1F23)
val SurfaceDark = Color(0xFF25292E)
val SurfaceInsetDark = Color(0xFF32373C)
val TextPrimaryDark = Color(0xFFF5F5F5)
val TextSecondaryDark = Color(0xFFA8B0B8)
val DividerDark = Color(0xFF383D43)

// --- semantic accents -------------------------------------------------------
/** Endorsements and anything verified. Used sparingly, never as a surface. */
val AccentGreen = Color(0xFF057642)
val AccentGreenOnDark = Color(0xFF6FCF97)
/** Highlights that must read against the blue citation panel. */
val AccentAmber = Color(0xFFE7A33E)
val ErrorRed = Color(0xFFB3261E)
val ErrorRedOnDark = Color(0xFFF2B8B5)
