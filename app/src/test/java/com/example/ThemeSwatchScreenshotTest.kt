package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcademicField
import com.example.ui.theme.CiteCircleTheme
import com.example.ui.theme.accent
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * One image per scheme showing every colour role and every field accent side by side.
 *
 * ThemeContrastTest proves the numbers are legal; this proves they look like a system. A palette
 * regression that stays technically AA-compliant -- a role drifting to the wrong hue, a container
 * no longer relating to its base -- is invisible to the contrast assertions but obvious here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ThemeSwatchScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun swatches_light() = capture(darkTheme = false, name = "theme_swatch_light")

    @Test
    fun swatches_dark() = capture(darkTheme = true, name = "theme_swatch_dark")

    private fun capture(darkTheme: Boolean, name: String) {
        composeTestRule.setContent {
            CiteCircleTheme(darkTheme = darkTheme) { Swatches() }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }
}

@Composable
private fun Swatches() {
    val scheme = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to (scheme.primary to scheme.onPrimary),
        "primaryContainer" to (scheme.primaryContainer to scheme.onPrimaryContainer),
        "secondary" to (scheme.secondary to scheme.onSecondary),
        "secondaryContainer" to (scheme.secondaryContainer to scheme.onSecondaryContainer),
        "tertiary" to (scheme.tertiary to scheme.onTertiary),
        "tertiaryContainer" to (scheme.tertiaryContainer to scheme.onTertiaryContainer),
        "background" to (scheme.background to scheme.onBackground),
        "surface" to (scheme.surface to scheme.onSurface),
        "surfaceVariant" to (scheme.surfaceVariant to scheme.onSurfaceVariant),
        "surfContLowest" to (scheme.surfaceContainerLowest to scheme.onSurface),
        "surfContLow" to (scheme.surfaceContainerLow to scheme.onSurface),
        "surfContainer" to (scheme.surfaceContainer to scheme.onSurface),
        "surfContHigh" to (scheme.surfaceContainerHigh to scheme.onSurface),
        "surfContHighest" to (scheme.surfaceContainerHighest to scheme.onSurface),
        "error" to (scheme.error to scheme.onError),
        "errorContainer" to (scheme.errorContainer to scheme.onErrorContainer),
        "inverseSurface" to (scheme.inverseSurface to scheme.inverseOnSurface),
        "outline" to (scheme.outline to scheme.surface),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(roles) { (label, colors) ->
                Chip(label = label, fill = colors.first, ink = colors.second)
            }
            items(AcademicField.entries.toList()) { field ->
                val accent = field.accent()
                Chip(label = field.key, fill = accent.base, ink = accent.onBase)
            }
            items(AcademicField.entries.toList()) { field ->
                val accent = field.accent()
                Chip(label = "${field.key} ctr", fill = accent.container, ink = accent.onContainer)
            }
        }
    }
}

@Composable
private fun Chip(label: String, fill: Color, ink: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(fill)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = ink, style = MaterialTheme.typography.labelSmall)
    }
}
