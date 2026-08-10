package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.SavedPaper
import com.example.ui.components.EmptyState
import com.example.ui.components.PaperCard
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
 * Screenshot coverage for the shared components every screen is built from.
 *
 * Components rather than whole screens: the screens need a `HomeViewModel` backed by Room, and
 * these run on the JVM. Both themes are captured because the palette is hand-rolled and dynamic
 * colour is disabled, so dark mode will not fix itself.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ComponentScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val samplePaper = SavedPaper(
    id = "1",
    authorInitials = "JD",
    authorName = "Dr. Jane Doe",
    timeAgo = "2h ago",
    affiliation = "AFFILIATION: OXFORD",
    content = "I just published a new preprint analyzing the semantic structures of large " +
      "language models.",
    citation = "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ.",
    isEndorsed = false,
  )

  @Composable
  private fun Themed(darkTheme: Boolean, content: @Composable () -> Unit) {
    InkAndFieldNotesTheme(darkTheme = darkTheme) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
      ) {
        content()
      }
    }
  }

  @Test
  fun paperCard_light() {
    composeTestRule.setContent {
      Themed(darkTheme = false) {
        PaperCard(
          paper = samplePaper,
          onEndorse = {},
          onViewContext = {},
          modifier = Modifier.padding(24.dp),
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/paper_card_light.png")
  }

  @Test
  fun paperCard_dark() {
    composeTestRule.setContent {
      Themed(darkTheme = true) {
        PaperCard(
          paper = samplePaper.copy(isEndorsed = true),
          onEndorse = {},
          onViewContext = {},
          modifier = Modifier.padding(24.dp),
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/paper_card_dark.png")
  }

  @Test
  fun emptyState_light() {
    composeTestRule.setContent {
      Themed(darkTheme = false) {
        EmptyState(
          title = "No Papers Yet",
          message = "Publish a preprint or endorse a colleague's work to start building your " +
            "registry.",
          icon = Icons.Outlined.BookmarkBorder,
          actionLabel = "PUBLISH A PAPER",
          onAction = {},
          modifier = Modifier.background(MaterialTheme.colorScheme.background),
        )
      }
    }
    composeTestRule.onRoot()
      .captureRoboImage(filePath = "src/test/screenshots/empty_state_light.png")
  }
}
