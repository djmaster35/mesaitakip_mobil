package com.example.mesaitakip.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

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

    fun formatDate(calendar: Calendar): String {
        return dateFormat.format(calendar.time)
    }

    fun getWeekRangeString(monday: Calendar): String {
        val sunday = monday.clone() as Calendar
        sunday.add(Calendar.DAY_OF_YEAR, 6)
        return "${formatDate(monday)} - ${formatDate(sunday)}"
    }
}
