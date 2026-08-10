package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Role mapping, so screens can be read without cross-referencing hex values:
 *
 * - `primary`      brand blue: actions, links, the citation panel, active navigation
 * - `secondary`    green: endorsement and anything verified
 * - `tertiary`     amber: highlights that must survive on top of the blue panel
 * - `background`   the neutral page
 * - `surface`      white cards floating on it
 * - `surfaceVariant` inset panels, quoted posts, skeleton bones
 * - `outlineVariant` hairlines and dividers
 */
val CiteCircleLight =
  lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandBluePressed,
    onPrimaryContainer = SurfaceWhite,
    secondary = AccentGreen,
    onSecondary = SurfaceWhite,
    tertiary = AccentAmber,
    onTertiary = TextPrimaryLight,
    background = PageNeutral,
    onBackground = TextPrimaryLight,
    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceInset,
    onSurfaceVariant = TextSecondaryLight,
    outline = TextSecondaryLight,
    outlineVariant = DividerLight,
    error = ErrorRed,
    onError = SurfaceWhite,
  )

val CiteCircleDark =
  darkColorScheme(
    primary = BrandBlueOnDark,
    onPrimary = PageDark,
    primaryContainer = BrandBlue,
    onPrimaryContainer = SurfaceWhite,
    secondary = AccentGreenOnDark,
    onSecondary = PageDark,
    tertiary = AccentAmber,
    onTertiary = PageDark,
    background = PageDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceInsetDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextSecondaryDark,
    outlineVariant = DividerDark,
    error = ErrorRedOnDark,
    onError = PageDark,
  )

/**
 * Dynamic colour stays off. A professional network is recognisable partly by being the same
 * blue on every device, and wallpaper-derived palettes would undo that.
 */
@Composable
fun CiteCircleTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) CiteCircleDark else CiteCircleLight,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
