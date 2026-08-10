package com.example

import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

/**
 * Covers [formatCitation], extracted from the `CitationBlock` composable.
 *
 * The MLA and CHICAGO branches are naive string replacements, and these tests pin what they
 * actually produce today -- including two defects, each flagged below with the ignored test that
 * states the intended output.
 */
class CitationFormatTest {

  private val apa =
    "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. " +
      "https://cite.circle/refs/882xj"

  private val tail = "Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. " +
    "https://cite.circle/refs/882xj"

  @Test
  fun `apa returns the citation unchanged`() {
    assertEquals(apa, formatCitation(apa, "APA"))
  }

  /**
   * Known defect: replacing `" (2026). "` with `". "` leaves the author's own trailing period in
   * place, so the result has a doubled period ("Doe, J.." rather than "Doe, J.").
   */
  @Test
  fun `mla drops the year but leaves a doubled period`() {
    assertEquals("Doe, J.. $tail", formatCitation(apa, "MLA"))
  }

  @Test
  fun `chicago moves the year after the author`() {
    assertEquals("Doe, J., 2026. $tail", formatCitation(apa, "CHICAGO"))
  }

  @Test
  fun `an unrecognised format falls back to apa`() {
    assertEquals(apa, formatCitation(apa, "BIBTEX"))
    assertEquals(apa, formatCitation(apa, ""))
    assertEquals(apa, formatCitation(apa, "mla"))
  }

  @Test
  fun `a citation with no year is passed through untouched`() {
    val noYear = "Doe, J. Untitled Draft."
    assertEquals(noYear, formatCitation(noYear, "MLA"))
    assertEquals(noYear, formatCitation(noYear, "CHICAGO"))
  }

  /**
   * Known defect: the year is hardcoded to 2026, so a citation from any other year is returned
   * verbatim and renders as APA under an MLA or Chicago label.
   */
  @Test
  fun `a non-2026 citation is silently not converted`() {
    val other = "Doe, J. (2024). An Older Paper. Folio Preprints."

    assertEquals(other, formatCitation(other, "MLA"))
    assertEquals(other, formatCitation(other, "CHICAGO"))
  }

  @Ignore("Known bug: formatCitation hardcodes the year 2026. Un-ignore when it parses any year.")
  @Test
  fun `any year should be converted`() {
    val other = "Doe, J. (2024). An Older Paper. Folio Preprints."

    assertEquals("Doe, J. An Older Paper. Folio Preprints.", formatCitation(other, "MLA"))
    assertEquals("Doe, J., 2024. An Older Paper. Folio Preprints.", formatCitation(other, "CHICAGO"))
  }

  @Ignore("Known bug: the MLA branch leaves a doubled period after the author.")
  @Test
  fun `mla should not leave a doubled period`() {
    assertEquals("Doe, J. $tail", formatCitation(apa, "MLA"))
  }
}
