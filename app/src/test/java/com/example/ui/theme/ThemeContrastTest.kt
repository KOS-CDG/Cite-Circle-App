package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.pow

/**
 * Guards the palette against contrast regressions.
 *
 * This exists because the palette this replaced was genuinely unreadable in places: its dark
 * scheme scored 2.16:1 for onPrimary against primary (CharcoalInk on ForestGreenLight), 4.34:1
 * for onSecondary, and 3.72:1 for onSurfaceVariant. Nothing caught that, because nothing checked.
 *
 * A saturated multi-hue palette makes this failure mode much easier to hit, so it is checked on
 * every build. Body text is asserted against all five surfaceContainer tiers, not just `surface`,
 * because that is where cards, bubbles and sheets actually sit.
 *
 * Runs under Robolectric only so that androidx.compose.ui.graphics.Color resolves without the
 * "not mocked" default; the arithmetic itself is pure JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeContrastTest {

    /** WCAG 2.1 relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertContrast(label: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertTrue(
            "$label is %.2f:1, needs >= %.1f:1".format(ratio, min),
            ratio >= min,
        )
    }

    private fun ColorScheme.checkAll(schemeName: String) {
        // Text on its designated container. AA normal text.
        val textPairs = listOf(
            Triple("onPrimary/primary", onPrimary, primary),
            Triple("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer),
            Triple("onSecondary/secondary", onSecondary, secondary),
            Triple("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer),
            Triple("onTertiary/tertiary", onTertiary, tertiary),
            Triple("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer),
            Triple("onBackground/background", onBackground, background),
            Triple("onSurface/surface", onSurface, surface),
            Triple("onSurfaceVariant/surfaceVariant", onSurfaceVariant, surfaceVariant),
            Triple("onError/error", onError, error),
            Triple("onErrorContainer/errorContainer", onErrorContainer, errorContainer),
            Triple("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface),
        )
        textPairs.forEach { (label, fg, bg) ->
            assertContrast("[$schemeName] $label", fg, bg, MIN_TEXT)
        }

        // Body text has to survive on every elevation tier, since cards, bubbles and sheets all
        // paint one of these rather than `surface`.
        val tiers = listOf(
            "surfaceContainerLowest" to surfaceContainerLowest,
            "surfaceContainerLow" to surfaceContainerLow,
            "surfaceContainer" to surfaceContainer,
            "surfaceContainerHigh" to surfaceContainerHigh,
            "surfaceContainerHighest" to surfaceContainerHighest,
        )
        tiers.forEach { (tierName, tier) ->
            assertContrast("[$schemeName] onSurface on $tierName", onSurface, tier, MIN_TEXT)
            assertContrast("[$schemeName] onSurfaceVariant on $tierName", onSurfaceVariant, tier, MIN_TEXT)
        }

        // Non-text: borders and disabled affordances only need 3:1.
        assertContrast("[$schemeName] outline/surface", outline, surface, MIN_NON_TEXT)
        assertContrast("[$schemeName] outline/background", outline, background, MIN_NON_TEXT)
    }

    @Test
    fun lightScheme_meetsContrastFloor() {
        CiteCircleLightScheme.checkAll("light")
    }

    @Test
    fun darkScheme_meetsContrastFloor() {
        CiteCircleDarkScheme.checkAll("dark")
    }

    /**
     * Field accents are used as foreground (chip labels, icons, the count text on a field row), so
     * each `base` must clear AA against the surface it is drawn on, and each container pairing
     * must clear it too.
     */
    @Test
    fun fieldAccents_meetContrastFloor() {
        listOf(false to CiteCircleLightScheme, true to CiteCircleDarkScheme)
            .forEach { (isDark, scheme) ->
                val name = if (isDark) "dark" else "light"
                fieldAccents(isDark).forEach { (field, accent) ->
                    assertContrast(
                        "[$name] ${field.key} base on surface",
                        accent.base, scheme.surface, MIN_TEXT,
                    )
                    assertContrast(
                        "[$name] ${field.key} onBase on base",
                        accent.onBase, accent.base, MIN_TEXT,
                    )
                    assertContrast(
                        "[$name] ${field.key} onContainer on container",
                        accent.onContainer, accent.container, MIN_TEXT,
                    )
                }
            }
    }

    private companion object {
        const val MIN_TEXT = 4.5
        const val MIN_NON_TEXT = 3.0
    }
}
