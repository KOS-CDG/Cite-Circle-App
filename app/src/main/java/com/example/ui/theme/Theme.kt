package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

val InkAndFieldNotesDark =
  darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = CharcoalInk,
    secondary = TerracottaLight,
    onSecondary = CharcoalInk,
    tertiary = WarmOchreLight,
    onTertiary = CharcoalInk,
    background = DarkBackground,
    onBackground = LightTextDark,
    surface = DarkSurface,
    onSurface = LightTextDark,
    surfaceVariant = Color.DarkGray,
    onSurfaceVariant = FadedTextDark,
    error = ErrorRed,
    onError = LightTextDark,
  )

val InkAndFieldNotesLight =
  lightColorScheme(
    primary = ForestGreen,
    onPrimary = ParchmentCream,
    secondary = Terracotta,
    onSecondary = ParchmentCream,
    tertiary = WarmOchre,
    onTertiary = CharcoalInk,
    background = ParchmentCream,
    onBackground = CharcoalInk,
    surface = ParchmentCream,
    onSurface = CharcoalInk,
    surfaceVariant = CitationBackground,
    onSurfaceVariant = FadedInk,
    error = ErrorRed,
    onError = ParchmentCream,
  )

@Composable
fun InkAndFieldNotesTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Do NOT use dynamic color to enforce our strict editorial palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      darkTheme -> InkAndFieldNotesDark
      else -> InkAndFieldNotesLight
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
