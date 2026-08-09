package com.example.util

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The codebase had no timestamp handling at all -- SavedPaper.timeAgo is a hardcoded String like
 * "2h ago", which is why the feed could not be sorted chronologically.
 */
object TimeFormat {

    /** "now", "4m", "2h", "3d", then a date. Used for list rows where space is tight. */
    fun relative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val delta = (now - epochMillis).coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> shortDate(epochMillis)
        }
    }

    /** "Today" / "Yesterday" / "12 Mar" -- the day-separator label inside a thread. */
    fun daySeparator(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val then = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val today = Calendar.getInstance().apply { timeInMillis = now }

        if (isSameDay(then, today)) return "Today"

        today.add(Calendar.DAY_OF_YEAR, -1)
        if (isSameDay(then, today)) return "Yesterday"

        return shortDate(epochMillis)
    }

    /** A stable key for grouping messages into days. */
    fun dayKey(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** "09:41" -- shown under the last bubble of a run. */
    fun clockTime(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
    }

    private fun shortDate(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
        return "${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]}"
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
