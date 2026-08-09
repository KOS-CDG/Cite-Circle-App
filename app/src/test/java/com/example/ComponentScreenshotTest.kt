package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.SavedPaper
import com.example.ui.components.Avatar
import com.example.ui.components.CitationChart
import com.example.ui.components.EmptyState
import com.example.ui.components.PostCard
import com.example.ui.components.Reaction
import com.example.ui.components.ReactionBar
import com.example.ui.theme.CiteCircleTheme
import com.example.ui.theme.Spacing
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images for the shared components, in both schemes.
 *
 * Only stateless components are captured. The screens take ViewModels backed by Room and
 * application-scoped repositories, which is more machinery than a screenshot test should stand up
 * -- and the components are where the visual regressions actually live.
 *
 * Caveat worth remembering when reading these: downloadable Google Fonts do not resolve under
 * Robolectric, so every capture renders in the platform fallback face. These verify layout,
 * spacing and colour. They cannot verify typography.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ComponentScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val samplePaper = SavedPaper(
        id = "p-shot",
        authorInitials = "JT",
        authorName = "Dr. Julian Thorne",
        timeAgo = "",
        affiliation = "CERN",
        content = "Replication of the entropy decay result held across all three archival " +
            "clusters. The effect is larger than we expected in the cold-storage condition.",
        citation = "Thorne, J. (2026). Thermodynamic Decay in Archival Structures. " +
            "Journal of Archival Science, 44(2).",
        createdAt = 1_754_000_000_000L,
        authorId = "u-thorne",
        fieldKey = "physics",
        commentCount = 4,
        myReaction = "INSIGHTFUL",
    )

    @Test fun postCard_light() = capture(false, "post_card_light") { PostCardShot() }
    @Test fun postCard_dark() = capture(true, "post_card_dark") { PostCardShot() }

    @Test fun reactionBar_light() = capture(false, "reaction_bar_light") { ReactionBarShot() }
    @Test fun reactionBar_dark() = capture(true, "reaction_bar_dark") { ReactionBarShot() }

    @Test fun citationChart_light() = capture(false, "citation_chart_light") { ChartShot() }
    @Test fun citationChart_dark() = capture(true, "citation_chart_dark") { ChartShot() }

    @Test fun avatars_light() = capture(false, "avatars_light") { AvatarShot() }
    @Test fun avatars_dark() = capture(true, "avatars_dark") { AvatarShot() }

    @Test fun emptyState_light() = capture(false, "empty_state_light") { EmptyStateShot() }
    @Test fun emptyState_dark() = capture(true, "empty_state_dark") { EmptyStateShot() }

    private fun capture(darkTheme: Boolean, name: String, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CiteCircleTheme(darkTheme = darkTheme) {
                // fillMaxSize, not fillMaxWidth: onRoot() captures the whole device frame, so a
                // width-only container leaves the rest of the image transparent.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(Spacing.screenHorizontal),
                ) {
                    content()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }

    @Composable
    private fun PostCardShot() {
        PostCard(paper = samplePaper, onReact = {}, onComment = {})
    }

    @Composable
    private fun ReactionBarShot() {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ReactionBar(selected = null, commentCount = 0, onReact = {}, onComment = {})
            ReactionBar(
                selected = Reaction.ENDORSE,
                commentCount = 3,
                onReact = {},
                onComment = {},
            )
            ReactionBar(
                selected = Reaction.CITE_WORTHY,
                commentCount = 12,
                onReact = {},
                onComment = {},
            )
        }
    }

    @Composable
    private fun ChartShot() {
        CitationChart(
            dataPoints = listOf(18f, 26f, 41f, 58f, 74f, 96f, 121f, 148f),
            hIndex = 24,
        )
    }

    @Composable
    private fun AvatarShot() {
        // Different seeds must produce visibly different gradients -- that is the whole point of
        // the deterministic fallback.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Avatar("JT", "u-thorne", size = 56.dp, showPresence = true, isOnline = true)
            Avatar("AO", "u-okafor", size = 56.dp, showPresence = true, isOnline = false)
            Avatar("EL", "u-lindqvist", size = 56.dp)
            Avatar("SN", "u-navarro", size = 56.dp, ring = true)
        }
    }

    @Composable
    private fun EmptyStateShot() {
        EmptyState(
            title = "No conversations",
            message = "Start a conversation from a researcher's profile.",
            icon = Icons.Outlined.Forum,
        )
    }
}
