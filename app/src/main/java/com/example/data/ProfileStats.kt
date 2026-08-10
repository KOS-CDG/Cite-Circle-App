package com.example.data

import java.util.Calendar
import java.util.TimeZone

/**
 * Counts shown on the profile, summed from the library rather than invented.
 *
 * The profile previously hardcoded "h-index 24" and an eight-point chart that never moved,
 * regardless of what was actually in the database.
 */
data class ProfileStats(
    val entries: Int,
    val endorsements: Int,
    val citations: Int
) {
    companion object {
        fun from(papers: List<SavedPaper>): ProfileStats = ProfileStats(
            entries = papers.size,
            endorsements = papers.sumOf { it.endorsementCount },
            citations = papers.sumOf { it.repostCount }
        )
    }
}

/**
 * Cumulative entry count per calendar month across the last [months] months, oldest first.
 *
 * Returns an empty list when there is nothing meaningful to draw. The caller is expected to
 * hide the chart in that case rather than render a flat or invented line — a chart with one
 * point is a decoration, not information.
 */
fun cumulativeEntriesByMonth(
    papers: List<SavedPaper>,
    now: Long = System.currentTimeMillis(),
    months: Int = 6,
    timeZone: TimeZone = TimeZone.getDefault()
): List<Float> {
    val dated = papers.filter { it.publishedAt > 0L }
    if (dated.isEmpty()) return emptyList()

    // Bucket boundaries: the start of each of the last `months` months, oldest first.
    val cal = Calendar.getInstance(timeZone).apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val monthStarts = ArrayList<Long>(months)
    repeat(months) { monthStarts.add(cal.timeInMillis); cal.add(Calendar.MONTH, -1) }
    monthStarts.reverse()

    // Cumulative: everything published up to the end of each month.
    val series = monthStarts.mapIndexed { index, start ->
        val end = if (index + 1 < monthStarts.size) monthStarts[index + 1] else Long.MAX_VALUE
        val cutoff = if (end == Long.MAX_VALUE) Long.MAX_VALUE else end
        dated.count { it.publishedAt < cutoff }.toFloat()
    }

    // A line needs at least two distinct values to say anything.
    return if (series.distinct().size < 2) emptyList() else series
}
