package com.example.universalappnotifier.utils

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Date

object DatePickerUtil {

    fun showDatePickerDialog(context: Context, unFormattedDate: Date, dateListener: DateListener) {
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = unFormattedDate
        DatePickerDialog(
            context,
            { _, selectedYear, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val unformattedDate = calendar.time
                dateListener.onDateSelected(Utils.formatDate(unformattedDate), unformattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    interface DateListener {
        fun onDateSelected(
            formattedDate: String,
            unFormattedDate: Date
        )
    }

}