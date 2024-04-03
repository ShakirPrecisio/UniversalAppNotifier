package com.example.universalappnotifier.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtil {

    fun getCurrentTime(): LocalTime {
        return LocalTime.now()
    }

    fun getOnlyCurrentDateOfMonth(): Int {
        val currentDate = LocalDate.now()
        return currentDate.format(DateTimeFormatter.ofPattern("dd")).toInt()
    }

    fun getCurrentDate(): LocalDate {
        return LocalDate.now()
    }

    fun getCurrentYear(): Int {
        return LocalDate.now().year
    }

    fun getCurrentMonth(): Int {
        return LocalDate.now().monthValue
    }

    fun getTotalNumberOfDaysInCurrentMonth(): Int {
        return LocalDate.now().lengthOfMonth()
    }

    fun getDayOfWeekForCurrentDate(): String {
        val formatter = DateTimeFormatter.ofPattern("EEE")
        return LocalDate.now().format(formatter)
    }

    fun getOnlySelectedDateOfMonth(localDate: LocalDate): Int {
        return localDate.format(DateTimeFormatter.ofPattern("dd")).toInt()
    }

    fun getSelectedYear(localDate: LocalDate): Int {
        return localDate.year
    }

    fun getSelectedMonth(localDate: LocalDate): Int {
        return localDate.monthValue
    }

    fun getTotalNumberOfDaysInSelectedMonth(localDate: LocalDate): Int {
        return localDate.lengthOfMonth()
    }

    fun getSelectedMonthName(localDate: LocalDate): String {
        return localDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    }

}
