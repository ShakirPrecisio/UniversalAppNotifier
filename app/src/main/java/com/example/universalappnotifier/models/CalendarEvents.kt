package com.example.universalappnotifier.models

data class CalendarEvents(
    var google_calendar_email_ids: List<String> = listOf(),
    var outlook_calendar_email_ids: List<String> = listOf()
)