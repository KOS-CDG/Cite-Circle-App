package com.example.ui.opportunities

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
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

/** Pure-logic coverage for the opportunities filter. */
class FilterOpportunitiesTest {

  private val grant = sampleOpportunities.single { it.type == "Grant" }

  @Test
  fun `All returns every opportunity`() {
    assertEquals(sampleOpportunities, filterOpportunities(sampleOpportunities, "All"))
  }

  @Test
  fun `each declared filter narrows to that type`() {
    opportunityFilters
      .filterNot { it == "All" }
      .forEach { filter ->
        val result = filterOpportunities(sampleOpportunities, filter)
        assertTrue("no sample data for filter '$filter'", result.isNotEmpty())
        assertTrue(
          "filter '$filter' returned other types: ${result.map { it.type }}",
          result.all { it.type == filter },
        )
      }
  }

  @Test
  fun `an unknown filter returns nothing`() {
    assertEquals(emptyList<Opportunity>(), filterOpportunities(sampleOpportunities, "Fellowship"))
  }

  @Test
  fun `filtering is case sensitive`() {
    assertEquals(emptyList<Opportunity>(), filterOpportunities(sampleOpportunities, "grant"))
  }

  @Test
  fun `an empty source list stays empty`() {
    assertEquals(emptyList<Opportunity>(), filterOpportunities(emptyList(), "All"))
  }

  @Test
  fun `filtering does not mutate the source list`() {
    val before = sampleOpportunities.toList()
    filterOpportunities(sampleOpportunities, "Grant")
    assertEquals(before, sampleOpportunities)
  }

  @Test
  fun `grant filter selects the expected record`() {
    assertEquals(listOf(grant), filterOpportunities(sampleOpportunities, "Grant"))
  }
}

/** Verifies the tab bar is actually wired to the filter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpportunitiesScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setScreen() {
    composeTestRule.setContent { InkAndFieldNotesTheme { OpportunitiesScreen() } }
  }

  @Test
  fun `defaults to showing the first opportunity`() {
    setScreen()

    composeTestRule
      .onNodeWithText(sampleOpportunities.first().title)
      .assertIsDisplayed()
  }

  @Test
  fun `selecting a tab narrows the list to that type`() {
    setScreen()
    val job = sampleOpportunities.single { it.type == "Academic Job" }
    val grant = sampleOpportunities.single { it.type == "Grant" }

    composeTestRule.onNodeWithText("Academic Job").performClick()

    composeTestRule.onNodeWithText(job.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(grant.title).assertDoesNotExist()
  }

  @Test
  fun `returning to All restores the full list`() {
    setScreen()
    val grant = sampleOpportunities.single { it.type == "Grant" }

    composeTestRule.onNodeWithText("Call for Papers").performClick()
    composeTestRule.onNodeWithText(grant.title).assertDoesNotExist()

    composeTestRule.onNodeWithText("All").performClick()
    composeTestRule.onNodeWithText(grant.title).assertIsDisplayed()
  }
}
