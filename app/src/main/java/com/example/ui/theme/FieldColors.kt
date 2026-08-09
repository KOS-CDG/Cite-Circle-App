package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Per-discipline accent colors. This is where most of the "bold multi-color" energy lives: field
 * chips, the field list, the colored rail on post cards, and person-card banners all draw from
 * here, so the app reads as many-hued without the core palette having to shout.
 *
 * Light `base` values are all >= 4.5:1 on white, verified by ThemeContrastTest. Two are
 * deliberately darker than the obvious pick: a bright emerald (#10A37F) measures 3.20:1 and a
 * bright cyan (#0891B2) measures 3.68:1 -- both fail AA, so they are not used.
 */
enum class AcademicField(val key: String, val label: String) {
    PHYSICS("physics", "Theoretical Physics"),
    BIOLOGY("biology", "Molecular Biology"),
    HISTORY("history", "Ancient History"),
    LINGUISTICS("linguistics", "Computational Linguistics"),
    COGNITIVE("cognitive", "Cognitive Science"),
    ECONOMICS("economics", "Macroeconomics");

    companion object {
        fun fromKey(key: String?): AcademicField =
            entries.firstOrNull { it.key == key } ?: PHYSICS

        fun fromLabel(label: String?): AcademicField =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: PHYSICS
    }
}

@Immutable
data class FieldAccent(
    val base: Color,
    val onBase: Color,
    val container: Color,
    val onContainer: Color,
)

private val LightFieldAccents: Map<AcademicField, FieldAccent> = mapOf(
    AcademicField.PHYSICS to FieldAccent(
        base = Color(0xFF2563EB), onBase = Color(0xFFFFFFFF),
        container = Color(0xFFDCE7FF), onContainer = Color(0xFF0B2B6E),
    ),
    AcademicField.BIOLOGY to FieldAccent(
        base = Color(0xFF15803D), onBase = Color(0xFFFFFFFF),
        container = Color(0xFFD5F5E0), onContainer = Color(0xFF03301A),
    ),
    AcademicField.HISTORY to FieldAccent(
        base = Color(0xFFC2410C), onBase = Color(0xFFFFFFFF),
        container = Color(0xFFFFE2D2), onContainer = Color(0xFF4A1400),
    ),
    AcademicField.LINGUISTICS to FieldAccent(
        base = Color(0xFFC026D3), onBase = Color(0xFFFFFFFF),
        container = Color(0xFFFBDDFF), onContainer = Color(0xFF420049),
    ),
    AcademicField.COGNITIVE to FieldAccent(
        base = Color(0xFF7C3AED), onBase = Color(0xFFFFFFFF),
        container = Color(0xFFEBE1FF), onContainer = Color(0xFF280060),
    ),
    AcademicField.ECONOMICS to FieldAccent(
        base = Color(0xFF0F766E), onBase = Color(0xFFFFFFFF),
        container = Color(0xFFD0F3EF), onContainer = Color(0xFF00201D),
    ),
)

private val DarkFieldAccents: Map<AcademicField, FieldAccent> = mapOf(
    AcademicField.PHYSICS to FieldAccent(
        base = Color(0xFF93B4FF), onBase = Color(0xFF06245F),
        container = Color(0xFF143A8A), onContainer = Color(0xFFDCE7FF),
    ),
    AcademicField.BIOLOGY to FieldAccent(
        base = Color(0xFF7BE0A2), onBase = Color(0xFF00301A),
        container = Color(0xFF0B5C2C), onContainer = Color(0xFFD5F5E0),
    ),
    AcademicField.HISTORY to FieldAccent(
        base = Color(0xFFFFB088), onBase = Color(0xFF4A1400),
        container = Color(0xFF7A2607), onContainer = Color(0xFFFFE2D2),
    ),
    AcademicField.LINGUISTICS to FieldAccent(
        base = Color(0xFFF0A6FF), onBase = Color(0xFF420049),
        container = Color(0xFF74107F), onContainer = Color(0xFFFBDDFF),
    ),
    AcademicField.COGNITIVE to FieldAccent(
        base = Color(0xFFC4A8FF), onBase = Color(0xFF280060),
        container = Color(0xFF4A1F9E), onContainer = Color(0xFFEBE1FF),
    ),
    AcademicField.ECONOMICS to FieldAccent(
        base = Color(0xFF6FDCD0), onBase = Color(0xFF00201D),
        container = Color(0xFF075A52), onContainer = Color(0xFFD0F3EF),
    ),
)

internal fun fieldAccents(darkTheme: Boolean): Map<AcademicField, FieldAccent> =
    if (darkTheme) DarkFieldAccents else LightFieldAccents

/**
 * staticCompositionLocalOf, not compositionLocalOf: the map is constant for a given theme, so
 * there is nothing to track for invalidation.
 */
val LocalFieldAccents = staticCompositionLocalOf<Map<AcademicField, FieldAccent>> {
    error("LocalFieldAccents not provided -- wrap the content in CiteCircleTheme")
}

/** `AcademicField.PHYSICS.accent().base` inside any composable under CiteCircleTheme. */
@Composable
@ReadOnlyComposable
fun AcademicField.accent(): FieldAccent = LocalFieldAccents.current.getValue(this)
