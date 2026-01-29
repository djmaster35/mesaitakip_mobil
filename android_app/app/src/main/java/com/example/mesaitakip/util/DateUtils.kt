package com.example.mesaitakip.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun getMondayOfCurrentWeek(): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal
    }

    fun formatDateForDb(calendar: Calendar): String {
        return dbFormat.format(calendar.time)
    }

    fun formatDateForDisplay(dbDate: String): String {
        return try {
            val date = dbFormat.parse(dbDate)
            displayFormat.format(date!!)
        } catch (e: Exception) {
            dbDate
        }
    }

    fun getWeekRangeString(monday: Calendar): String {
        val sunday = monday.clone() as Calendar
        sunday.add(Calendar.DAY_OF_YEAR, 6)
        return "${displayFormat.format(monday.time)} - ${displayFormat.format(sunday.time)}"
    }
}
