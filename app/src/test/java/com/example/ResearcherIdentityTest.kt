package com.example

import com.example.ui.profile.initialsOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ResearcherIdentityTest {

  @Test
  fun `derives initials from a two-part name`() {
    assertEquals("JD", initialsOf("Jane Doe"))
  }

  @Test
  fun `ignores academic titles`() {
    assertEquals("JD", initialsOf("Dr. Jane Doe"))
    assertEquals("JT", initialsOf("Prof. Julian Thorne"))
  }

  @Test
  fun `falls back to the first two characters of a single name`() {
    assertEquals("AM", initialsOf("Amara"))
  }

  @Test
  fun `handles hyphenated names`() {
    assertEquals("MC", initialsOf("Marie-Claire"))
  }

  @Test
  fun `returns a placeholder for a nameless string`() {
    assertEquals("??", initialsOf("   "))
  }
}
