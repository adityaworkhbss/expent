package com.aditya.expent.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Budgets and expenses repeat every month, so the app stores only the day of the month (1-31).
 * The backend still works with full dates, so these helpers convert in both directions.
 */
object DayOfMonth {

    const val MIN = 1
    const val MAX = 31

    private val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "d/M/yyyy"
    )

    /** Day of month from a server / legacy date string, or null when it can't be read. */
    fun from(date: String?): Int? {
        val trimmed = date?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        trimmed.toIntOrNull()?.let { return it.takeIf { day -> day in MIN..MAX } }

        for (pattern in patterns) {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                if (pattern.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = runCatching { parser.parse(trimmed) }.getOrNull() ?: continue
            val calendar = Calendar.getInstance(parser.timeZone).apply { time = parsed }
            return calendar.get(Calendar.DAY_OF_MONTH)
        }
        return null
    }

    /**
     * ISO date in the current month on [day] (clamped to the month's length, e.g. 31 → 30 in April),
     * for API fields that still expect a full date.
     */
    fun toIsoDateThisMonth(day: Int): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(MIN, getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(calendar.time)
    }

    /** "1st", "2nd", "23rd", "11th"… */
    fun withSuffix(day: Int): String {
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }

    fun today(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
}
