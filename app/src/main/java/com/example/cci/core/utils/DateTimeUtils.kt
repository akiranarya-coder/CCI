package com.example.cci.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private fun dateFmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun dateTimeFmt() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    private fun displayDateFmt() = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private fun shortDateFmt() = SimpleDateFormat("dd MMM", Locale.US)
    private fun timeFmt() = SimpleDateFormat("HH:mm", Locale.US)

    fun todayIso(): String = dateFmt().format(Date())

    fun nowDateTimeIso(): String = dateTimeFmt().format(Date())

    fun nowTime(): String = timeFmt().format(Date())

    fun daysAgoIso(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFmt().format(cal.time)
    }

    fun daysFromNowIso(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return dateFmt().format(cal.time)
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val parsed = dateFmt().parse(isoDate)
            if (parsed != null) displayDateFmt().format(parsed) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatShortDate(isoDate: String): String {
        return try {
            val parsed = dateFmt().parse(isoDate)
            if (parsed != null) shortDateFmt().format(parsed) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatDateTimeReadable(isoDateTime: String): String {
        val datePart = isoDateTime.substringBefore("T")
        val timePart = isoDateTime.substringAfter("T", "")
        return if (timePart.isNotBlank()) "${formatDisplayDate(datePart)} • $timePart"
        else formatDisplayDate(datePart)
    }

    fun currentShift(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..13 -> "A"
            in 14..21 -> "B"
            else -> "C"
        }
    }

    fun generateSampleId(dateIso: String, sequence: Int): String {
        val compact = dateIso.replace("-", "")
        return "CCI-$compact-${String.format(Locale.US, "%02d", sequence)}"
    }

    private fun utcDateFmt(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }

    /**
     * Converts Material3 DatePicker UTC epoch milliseconds to canonical ISO yyyy-MM-dd.
     */
    fun utcMillisToIsoDate(utcMillis: Long): String = utcDateFmt().format(Date(utcMillis))

    /**
     * Converts canonical ISO yyyy-MM-dd to UTC midnight milliseconds for Material3 DatePicker.
     */
    fun isoDateToUtcMillis(isoDate: String): Long? {
        return try {
            utcDateFmt().parse(isoDate)?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns UTC end-of-day millis for today's local calendar date so future dates can be blocked.
     */
    fun todayEndOfDayUtcMillis(): Long {
        val todayMidnightUtc = isoDateToUtcMillis(todayIso()) ?: System.currentTimeMillis()
        return todayMidnightUtc + 86_399_999L
    }

    /**
     * Strictly validates that a date string matches yyyy-MM-dd and is a real calendar date.
     */
    fun isValidIsoDate(isoDate: String): Boolean {
        if (!isoDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) return false
        return isoDateToUtcMillis(isoDate) != null
    }
}
