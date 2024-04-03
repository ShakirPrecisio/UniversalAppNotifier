package com.example.universalappnotifier.utils

import android.app.DatePickerDialog
import android.content.Context
import java.time.LocalDate

object DatePickerUtil {

    fun showDatePickerDialog(context: Context, givenDate: LocalDate, dateListener: DateListener) {
        Utils.printDebugLog("givenDate: $givenDate")
        val initialYear = givenDate.year
        val initialMonth = givenDate.monthValue - 1 // Months are 1-indexed in DatePickerDialog
        val initialDayOfMonth = givenDate.dayOfMonth

        DatePickerDialog(
            context,
            { _, selectedYear, monthOfYear, dayOfMonth ->
                val selectedDate = LocalDate.of(selectedYear, monthOfYear + 1, dayOfMonth)
                Utils.printDebugLog("selectedDate: $selectedDate")
                dateListener.onDateSelected(selectedDate)
            },
            initialYear,
            initialMonth,
            initialDayOfMonth
        ).show()
    }

    interface DateListener {
        fun onDateSelected(selectedDate: LocalDate)
    }

    // Other utility methods if needed
}