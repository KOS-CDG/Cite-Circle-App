package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage for citation rendering — no Android runtime involved, so these run on
 * the host under `./gradlew test`.
 */
class CitationFormatterTest {

    private fun paper(
        title: String = "",
        authors: String = "",
        year: String = "",
        venue: String = "",
        doi: String = "",
        url: String = "",
        content: String = "",
        citationOverride: String = ""
    ) = SavedPaper(
        id = "t",
        authorInitials = "JD",
        authorName = "Dr. Jane Doe",
        affiliation = "AFFILIATION: OXFORD",
        content = content,
        title = title,
        authors = authors,
        year = year,
        venue = venue,
        doi = doi,
        url = url,
        publishedAt = 1_000L,
        citationOverride = citationOverride
    )

    // ------------------------------------------------------------------ APA

    @Test
    fun `apa renders a single author with initials and a doi link`() {
        val result = CitationFormatter.format(
            paper(
                title = "Semantic Structures in Large Language Models",
                authors = "Doe, Jane",
                year = "2026",
                venue = "Folio Preprints",
                doi = "10.1000/cc882xj"
            ),
            CitationStyle.APA
        )
        assertEquals(
            "Doe, J. (2026). Semantic Structures in Large Language Models. " +
                "Folio Preprints. https://doi.org/10.1000/cc882xj",
            result
        )
    }

    @Test
    fun `apa joins two authors with an ampersand`() {
        val result = CitationFormatter.format(
            paper(title = "Entropy", authors = "Doe, Jane; Smith, John", year = "2024"),
            CitationStyle.APA
        )
        assertEquals("Doe, J., & Smith, J. (2024). Entropy.", result)
    }

    @Test
    fun `apa uses serial comma before the final author of three`() {
        val result = CitationFormatter.format(
            paper(
                title = "Decay",
                authors = "Doe, Jane; Smith, John; Brown, Alice Marie",
                year = "2023"
            ),
            CitationStyle.APA
        )
        assertEquals("Doe, J., Smith, J., & Brown, A. M. (2023). Decay.", result)
    }

    @Test
    fun `apa elides the middle of a 21 author list`() {
        val authors = (1..21).joinToString("; ") { "Author$it, Given$it" }
        val result = CitationFormatter.format(
            paper(title = "Big Collaboration", authors = authors, year = "2026"),
            CitationStyle.APA
        )
        assertTrue(result.startsWith("Author1, G., Author2, G."))
        assertTrue(result.contains(", ... Author21, G. (2026)."))
    }

    @Test
    fun `apa substitutes n_d_ for a missing year`() {
        val result = CitationFormatter.format(
            paper(title = "Untitled Preprint", authors = "Doe, Jane", venue = "arXiv"),
            CitationStyle.APA
        )
        assertEquals("Doe, J. (n.d.). Untitled Preprint. arXiv.", result)
    }

    @Test
    fun `apa promotes the title when there is no author`() {
        val result = CitationFormatter.format(
            paper(title = "Special Issue", year = "2026", venue = "Archival Science"),
            CitationStyle.APA
        )
        assertEquals("Special Issue. (2026). Archival Science.", result)
    }

    @Test
    fun `apa does not double a period on a title that already ends in one`() {
        val result = CitationFormatter.format(
            paper(title = "Decay in Archives.", authors = "Doe, Jane", year = "2023"),
            CitationStyle.APA
        )
        assertEquals("Doe, J. (2023). Decay in Archives.", result)
    }

    // ------------------------------------------------------------------ MLA

    @Test
    fun `mla inverts only the first author and abbreviates three or more`() {
        val two = CitationFormatter.format(
            paper(title = "Entropy", authors = "Doe, Jane; Smith, John", year = "2024", venue = "JAS"),
            CitationStyle.MLA
        )
        assertEquals("Doe, Jane, and John Smith. “Entropy.” JAS, 2024.", two)

        val three = CitationFormatter.format(
            paper(
                title = "Entropy",
                authors = "Doe, Jane; Smith, John; Brown, Alice",
                year = "2024",
                venue = "JAS"
            ),
            CitationStyle.MLA
        )
        assertEquals("Doe, Jane, et al. “Entropy.” JAS, 2024.", three)
    }

    @Test
    fun `mla strips a trailing period from the title before quoting it`() {
        val result = CitationFormatter.format(
            paper(title = "Entropy.", authors = "Doe, Jane"),
            CitationStyle.MLA
        )
        assertEquals("Doe, Jane. “Entropy.”", result)
    }

    // -------------------------------------------------------------- CHICAGO

    @Test
    fun `chicago lists every author up to ten`() {
        val result = CitationFormatter.format(
            paper(
                title = "Latent Knowledge",
                authors = "Doe, Jane; Smith, John; Brown, Alice",
                year = "2025",
                venue = "ICML"
            ),
            CitationStyle.CHICAGO
        )
        assertEquals(
            "Doe, Jane, John Smith, and Alice Brown. “Latent Knowledge.” ICML (2025).",
            result
        )
    }

    @Test
    fun `chicago truncates to seven authors plus et al beyond ten`() {
        val authors = (1..12).joinToString("; ") { "Author$it, Given$it" }
        val result = CitationFormatter.format(
            paper(title = "Big Physics", authors = authors, year = "2026"),
            CitationStyle.CHICAGO
        )
        assertTrue(result.startsWith("Author1, Given1, Given2 Author2,"))
        assertTrue(result.contains("Given7 Author7, et al."))
        assertFalse(result.contains("Author8"))
    }

    // ------------------------------------------------------------- parsing

    @Test
    fun `authors given without a comma are read as given-then-family`() {
        val result = CitationFormatter.format(
            paper(title = "Reputation", authors = "Jane Doe; John Q Smith", year = "2026"),
            CitationStyle.APA
        )
        assertEquals("Doe, J., & Smith, J. Q. (2026). Reputation.", result)
    }

    @Test
    fun `doi prefixes pasted by users are stripped`() {
        listOf("doi:10.1234/abc", "https://doi.org/10.1234/abc", "10.1234/abc").forEach { raw ->
            val result = CitationFormatter.format(
                paper(title = "Prefix", authors = "Doe, Jane", year = "2026", doi = raw),
                CitationStyle.APA
            )
            assertEquals(
                "Doe, J. (2026). Prefix. https://doi.org/10.1234/abc",
                result
            )
        }
    }

    @Test
    fun `blank author entries are skipped rather than producing empty names`() {
        val result = CitationFormatter.format(
            paper(title = "Gaps", authors = "Doe, Jane;; ; Smith, John", year = "2026"),
            CitationStyle.APA
        )
        assertEquals("Doe, J., & Smith, J. (2026). Gaps.", result)
    }

    // ------------------------------------------------------ legacy fallback

    @Test
    fun `a paper without a title falls back to its verbatim citation in every style`() {
        val legacy = paper(citationOverride = "Doe, J. (2026). Legacy Row. Folio Preprints.")
        assertFalse(CitationFormatter.isStyleable(legacy))
        CitationStyle.entries.forEach { style ->
            assertEquals(
                "Doe, J. (2026). Legacy Row. Folio Preprints.",
                CitationFormatter.format(legacy, style)
            )
        }
    }

    // ------------------------------------------------------------- exports

    @Test
    fun `bibtex emits a citation key and the recorded fields`() {
        val result = CitationFormatter.export(
            paper(
                title = "Semantic Structures in LLMs",
                authors = "Doe, Jane; Smith, John",
                year = "2026",
                venue = "Folio Preprints",
                doi = "10.1000/cc882xj"
            ),
            ExportFormat.BIBTEX
        )
        assertTrue(result.startsWith("@article{doe2026semantic,"))
        assertTrue(result.contains("author  = {Doe, Jane and Smith, John}"))
        assertTrue(result.contains("title   = {Semantic Structures in LLMs}"))
        assertTrue(result.contains("journal = {Folio Preprints}"))
        assertTrue(result.contains("doi     = {10.1000/cc882xj}"))
        assertTrue(result.trimEnd().endsWith("}"))
    }

    @Test
    fun `bibtex escapes braces that would otherwise break the entry`() {
        val result = CitationFormatter.export(
            paper(title = "A {Braced} Title", authors = "Doe, Jane", year = "2026"),
            ExportFormat.BIBTEX
        )
        assertTrue(result.contains("{A \\{Braced\\} Title}"))
    }

    @Test
    fun `ris emits one AU line per author and terminates with ER`() {
        val result = CitationFormatter.export(
            paper(
                title = "Semantic Structures",
                authors = "Doe, Jane; Smith, John",
                year = "2026",
                venue = "Folio Preprints"
            ),
            ExportFormat.RIS
        )
        val lines = result.split("\r\n")
        assertEquals("TY  - JOUR", lines[0])
        assertEquals("AU  - Doe, Jane", lines[1])
        assertEquals("AU  - Smith, John", lines[2])
        assertEquals("TI  - Semantic Structures", lines[3])
        assertEquals("JO  - Folio Preprints", lines[4])
        assertEquals("PY  - 2026", lines[5])
        assertEquals("ER  - ", lines[6])
    }

    // ---------------------------------------------------------- timestamps

    @Test
    fun `relative timestamps are derived from the stored epoch millis`() {
        val now = 1_000_000_000_000L
        val minute = 60_000L
        assertEquals("just now", formatTimeAgo(now - 30_000L, now))
        assertEquals("5m ago", formatTimeAgo(now - 5 * minute, now))
        assertEquals("3h ago", formatTimeAgo(now - 180 * minute, now))
        assertEquals("2d ago", formatTimeAgo(now - 2 * 24 * 60 * minute, now))
        assertEquals("1w ago", formatTimeAgo(now - 8 * 24 * 60 * minute, now))
    }

    @Test
    fun `an unset timestamp renders as a dash rather than 1970`() {
        assertEquals("—", formatTimeAgo(0L, 1_000_000_000_000L))
    }
}
