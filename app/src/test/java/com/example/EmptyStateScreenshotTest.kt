package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.InkAndFieldNotesTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Replaces the old GreetingScreenshotTest, which referenced a `MyApplicationTheme` and a
 * `Greeting` composable that no longer exist anywhere in the project. That file failed to
 * compile, which failed :app:compileDebugUnitTestKotlin and took the whole `test` task with it.
 *
 * Note: downloadable Google Fonts do not resolve under Robolectric, so these captures render in
 * the system fallback typeface. They verify layout, spacing and color -- never typography.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class EmptyStateScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun emptyState_light() {
    composeTestRule.setContent {
      InkAndFieldNotesTheme(darkTheme = false) {
        EmptyState("No Fields Found", "Try adjusting your search criteria.", Icons.Outlined.FolderOff)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/empty_state_light.png")
  }

  @Test
  fun emptyState_dark() {
    composeTestRule.setContent {
      InkAndFieldNotesTheme(darkTheme = true) {
        EmptyState("No Fields Found", "Try adjusting your search criteria.", Icons.Outlined.FolderOff)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/empty_state_dark.png")
  }
}
