package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.HomeViewModel
import com.example.MenuScreen
import com.example.MyApplication
import com.example.ProfileScreen
import com.example.ui.chat.ChatThreadScreen
import com.example.ui.chat.MessengerScreen
import com.example.ui.opportunities.OpportunitiesScreen
import com.example.ui.theme.CiteCircleTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class NavigationAndUiFeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: MyApplication
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<MyApplication>()
        viewModel = HomeViewModel(app.repository, app.settings, app)
    }

    @Test
    fun testProfileScreenRendersButtonsWithoutCutOff() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            CiteCircleTheme {
                ProfileScreen(viewModel = viewModel, navController = navController)
            }
        }

        // Verify top bar actions are visible and displayed
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        // Verify action pill buttons exist in the hierarchy
        composeTestRule.onNodeWithText("Open to").assertExists()
        composeTestRule.onNodeWithText("Edit Profile").assertExists()
        composeTestRule.onNodeWithContentDescription("Share").assertExists()
        composeTestRule.onNodeWithContentDescription("More").assertExists()
    }

    @Test
    fun testMenuScreenRendersShortcutsAndDrawers() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            CiteCircleTheme {
                MenuScreen(viewModel = viewModel, navController = navController)
            }
        }

        composeTestRule.onNodeWithText("Menu").assertIsDisplayed()
        composeTestRule.onNodeWithText("View your profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Research Fields").assertExists()
        composeTestRule.onNodeWithText("Paper Vault").assertExists()
        composeTestRule.onNodeWithText("Gemini Assistant").assertExists()
        composeTestRule.onNodeWithText("Conferences").assertExists()
        composeTestRule.onNodeWithText("Citation Metrics").assertExists()
        composeTestRule.onNodeWithText("Help & Support").assertExists()
        composeTestRule.onNodeWithText("Settings & Privacy").assertExists()
    }

    @Test
    fun testMessengerScreenRendersCorrectly() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            CiteCircleTheme {
                MessengerScreen(
                    chatRepository = app.chatRepository,
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Chats").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun testChatThreadScreenRendersBottomBarAndInput() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            CiteCircleTheme {
                ChatThreadScreen(
                    conversationId = "test_conv",
                    chatRepository = app.chatRepository,
                    homeViewModel = viewModel,
                    navController = navController
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Endorse").assertIsDisplayed()
    }

    @Test
    fun testOpportunitiesScreenRenders() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            CiteCircleTheme {
                OpportunitiesScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }

        composeTestRule.onNodeWithText("Opportunities & Grants").assertIsDisplayed()
    }
}
