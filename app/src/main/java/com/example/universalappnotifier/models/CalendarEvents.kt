package com.example.universalappnotifier.models

data class CalendarEvents(
    var google_calendar: List<CalendarEmailData> = listOf(),
    var outlook_calendar: List<CalendarEmailData> = listOf()
)
