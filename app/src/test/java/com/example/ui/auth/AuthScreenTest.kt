package com.example.ui.auth

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.InkAndFieldNotesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins [AuthScreen]'s sign-in outcomes -- including the demo bypass, which lets a *failed* sign-in
 * through to the app. That bypass is intentional for unconfigured builds, but nothing else records
 * it, so this test is what makes shipping it a deliberate choice rather than an oversight. If the
 * bypass is removed, `failed sign in still proceeds` fails and must be updated in the same change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private class FakeGoogleSignIn(private val result: Boolean) : GoogleSignIn {
    var callCount = 0

    override suspend fun signInWithGoogle(): Boolean {
      callCount++
      return result
    }
  }

  private fun setScreen(signIn: GoogleSignIn, onAuthSuccess: () -> Unit) {
    composeTestRule.setContent {
      InkAndFieldNotesTheme { AuthScreen(onAuthSuccess = onAuthSuccess, signIn = signIn) }
    }
  }

  @Test
  fun `successful sign in proceeds`() {
    var succeeded = 0
    val signIn = FakeGoogleSignIn(result = true)
    setScreen(signIn) { succeeded++ }

    composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, signIn.callCount)
    assertEquals(1, succeeded)
  }

  /** Documents the deliberate bypass: a failed sign-in still enters the app. */
  @Test
  fun `failed sign in still proceeds - demo bypass`() {
    var succeeded = 0
    val signIn = FakeGoogleSignIn(result = false)
    setScreen(signIn) { succeeded++ }

    composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, signIn.callCount)
    assertEquals("demo bypass changed: a failed sign-in no longer proceeds", 1, succeeded)
  }

  @Test
  fun `sign in button is shown before any attempt`() {
    setScreen(FakeGoogleSignIn(result = true)) {}

    composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").assertExists()
    composeTestRule.onNodeWithText("Cite Circle").assertExists()
  }

  @Test
  fun `sign in is not attempted until the button is tapped`() {
    val signIn = FakeGoogleSignIn(result = true)
    setScreen(signIn) {}

    composeTestRule.waitForIdle()

    assertTrue(signIn.callCount == 0)
  }
}
