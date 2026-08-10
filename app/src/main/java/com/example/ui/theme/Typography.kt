package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val provider = GoogleFont.Provider(
  providerAuthority = "com.google.android.gms.fonts",
  providerPackage = "com.google.android.gms",
  certificates = R.array.com_google_android_gms_fonts_certs
)

private val InterFont = GoogleFont("Inter")

/**
 * One family, four weights. Professional networks carry their whole hierarchy on weight and
 * size rather than on a second typeface, which is what keeps them feeling like software
 * rather than like a publication.
 */
val InterFontFamily = FontFamily(
  Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
  Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
  Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
  Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold)
)

/**
 * Tracking is zero or slightly negative throughout. The previous scale opened headings and
 * labels up to 2sp of letter-spacing, which is a print mannerism and the single loudest
 * signal that this was not an app in the Facebook/LinkedIn register.
 */
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 40.sp,
      lineHeight = 48.sp,
      letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 32.sp,
      lineHeight = 40.sp,
      letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 28.sp,
      lineHeight = 36.sp,
      letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 24.sp,
      lineHeight = 32.sp,
      letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      lineHeight = 28.sp,
      letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 26.sp,
      letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 24.sp,
      letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.sp
    )
  )
