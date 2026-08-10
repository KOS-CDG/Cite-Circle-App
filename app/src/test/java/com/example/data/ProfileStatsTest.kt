package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ProfileStatsTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun paper(
        id: String,
        publishedAt: Long = 0L,
        endorsements: Int = 0,
        reposts: Int = 0
    ) = SavedPaper(
        id = id,
        authorInitials = "JD",
        authorName = "Jane Doe",
        affiliation = "Oxford",
        content = "c",
        publishedAt = publishedAt,
        endorsementCount = endorsements,
        repostCount = reposts
    )

    /** Epoch millis for the first of a month, UTC. */
    private fun monthStart(year: Int, monthZeroBased: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, monthZeroBased, 1, 0, 0, 0)
        }.timeInMillis

    @Test
    fun `stats sum the library rather than being invented`() {
        val stats = ProfileStats.from(
            listOf(
                paper("a", endorsements = 3, reposts = 1),
                paper("b", endorsements = 2, reposts = 0),
                paper("c")
            )
        )
        assertEquals(3, stats.entries)
        assertEquals(5, stats.endorsements)
        assertEquals(1, stats.citations)
    }

    @Test
    fun `stats of an empty library are all zero`() {
        val stats = ProfileStats.from(emptyList())
        assertEquals(ProfileStats(0, 0, 0), stats)
    }

    @Test
    fun `no chart series when there are no dated entries`() {
        assertTrue(cumulativeEntriesByMonth(emptyList(), timeZone = utc).isEmpty())
        assertTrue(cumulativeEntriesByMonth(listOf(paper("a")), timeZone = utc).isEmpty())
    }

    @Test
    fun `entries all in the current month still produce a slope worth drawing`() {
        val now = monthStart(2026, Calendar.JUNE) + 5 * 24 * 3600_000L
        val papers = listOf(
            paper("a", publishedAt = monthStart(2026, Calendar.JUNE) + 1000L),
            paper("b", publishedAt = monthStart(2026, Calendar.JUNE) + 2000L)
        )
        // Cumulative count runs 0,0,0,0,0,2 — flat then rising, which is real information.
        val series = cumulativeEntriesByMonth(papers, now = now, timeZone = utc)
        assertEquals(6, series.size)
        assertEquals(0f, series.first(), 0.001f)
        assertEquals(2f, series.last(), 0.001f)
    }

    @Test
    fun `series is cumulative and non-decreasing across months`() {
        val now = monthStart(2026, Calendar.JUNE) + 5 * 24 * 3600_000L
        val papers = listOf(
            paper("a", publishedAt = monthStart(2026, Calendar.FEBRUARY) + 1000L),
            paper("b", publishedAt = monthStart(2026, Calendar.APRIL) + 1000L),
            paper("c", publishedAt = monthStart(2026, Calendar.JUNE) + 1000L)
        )
        val series = cumulativeEntriesByMonth(papers, now = now, timeZone = utc)

        assertEquals(6, series.size)
        series.zipWithNext { earlier, later ->
            assertTrue("series must never decrease", later >= earlier)
        }
        assertEquals("all three entries counted by the final bucket", 3f, series.last(), 0.001f)
    }

    @Test
    fun `entries older than the window still count toward the running total`() {
        val now = monthStart(2026, Calendar.JUNE) + 5 * 24 * 3600_000L
        val papers = listOf(
            paper("old", publishedAt = monthStart(2020, Calendar.JANUARY)),
            paper("new", publishedAt = monthStart(2026, Calendar.JUNE) + 1000L)
        )
        val series = cumulativeEntriesByMonth(papers, now = now, timeZone = utc)
        assertEquals("the 2020 entry is already in the earliest bucket", 1f, series.first(), 0.001f)
        assertEquals(2f, series.last(), 0.001f)
    }
}
