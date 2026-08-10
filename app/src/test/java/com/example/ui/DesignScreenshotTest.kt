package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.components.PostCardSkeleton
import com.example.ui.theme.CiteCircleTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual baselines for the design system.
 *
 * Record with `./gradlew recordRoborazziDebug`, then `verifyRoborazziDebug` fails any change
 * that moves pixels. This is what stops the palette and spacing drifting back.
 *
 * Caveat worth knowing when reading the images: downloadable Google Fonts do not resolve
 * under Robolectric, so Inter falls back to the platform sans throughout. Layout, colour and
 * spacing regressions are still caught; the typeface itself is not proven here.
 *
 * The composables covered are the ones with no ViewModel dependency. PostCard and
 * CitationBlock take a HomeViewModel and would need a fake, which is a bigger piece of
 * scaffolding than it is worth until there is a reason to fake one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DesignScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CiteCircleTheme(darkTheme = dark) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .width(400.dp)
                        .padding(16.dp)
                ) { content() }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }

    @Test
    fun postCardSkeleton_light() = capture("post-skeleton-light") {
        PostCardSkeleton(Modifier.fillMaxWidth())
    }

    @Test
    fun postCardSkeleton_dark() = capture("post-skeleton-dark", dark = true) {
        PostCardSkeleton(Modifier.fillMaxWidth())
    }

    @Test
    fun listRowSkeleton_light() = capture("list-skeleton-light") {
        ListRowSkeleton(Modifier.fillMaxWidth())
    }

    @Test
    fun emptyState_light() = capture("empty-state-light") {
        EmptyState(
            title = "Your feed is empty",
            message = "Publish your first entry to start building your circle.",
            icon = Icons.Outlined.Article,
            actionLabel = "Write an entry",
            onAction = {}
        )
    }

    @Test
    fun emptyState_dark() = capture("empty-state-dark", dark = true) {
        EmptyState(
            title = "Your feed is empty",
            message = "Publish your first entry to start building your circle.",
            icon = Icons.Outlined.Article,
            actionLabel = "Write an entry",
            onAction = {}
        )
    }

}
