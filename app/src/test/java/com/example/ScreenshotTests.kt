package com.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.SavedPaper
import com.example.ui.opportunities.Opportunity
import com.example.ui.opportunities.OpportunityCard
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
 * Screenshot coverage for the presentation-heavy composables, where assertion-based tests would
 * mostly restate the layout code. Baselines are written by `:app:recordRoborazziDebug` and checked
 * by `:app:verifyRoborazziDebug`.
 *
 * Deliberately free of coroutines and Room: every composable here takes plain data and callbacks,
 * so there is no background work that could outlive a test and leak into the next one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreenshotTests {

  @get:Rule val composeTestRule = createComposeRule()

  private val paper =
    SavedPaper(
      id = "screenshot-1",
      authorInitials = "JD",
      authorName = "Dr. Jane Doe",
      timeAgo = "2h ago",
      affiliation = "AFFILIATION: OXFORD",
      content =
        "I just published a new preprint analyzing the semantic structures of large language " +
          "models. The findings suggest a stark shift in latent knowledge representations.",
      citation =
        "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. " +
          "https://cite.circle/refs/882xj",
      isEndorsed = false,
    )

  private val opportunity =
    Opportunity(
      id = "screenshot-opp",
      type = "Grant",
      title = "Early Career Researcher Fellowship",
      institution = "NSF",
      deadline = "Deadline: Nov 15, 2026",
      description = "Funding for innovative theoretical physics research.",
    )

  private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
    composeTestRule.setContent {
      InkAndFieldNotesTheme(darkTheme = darkTheme) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.background,
          content = content,
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
  }

  @Test
  fun postCard_light() =
    capture("post_card_light", darkTheme = false) { PostCard(paper, onEndorse = {}) }

  @Test
  fun postCard_dark() =
    capture("post_card_dark", darkTheme = true) { PostCard(paper, onEndorse = {}) }

  @Test
  fun postCard_endorsed() =
    capture("post_card_endorsed", darkTheme = false) {
      PostCard(paper.copy(isEndorsed = true), onEndorse = {})
    }

  @Test
  fun emptyState_light() =
    capture("empty_state_light", darkTheme = false) {
      EmptyState(
        title = "No papers yet",
        message = "Saved papers will appear here.",
        icon = Icons.Outlined.LibraryBooks,
      )
    }

  @Test
  fun opportunityCard_light() =
    capture("opportunity_card_light", darkTheme = false) { OpportunityCard(opportunity) }

  @Test
  fun opportunityCard_dark() =
    capture("opportunity_card_dark", darkTheme = true) { OpportunityCard(opportunity) }
}
