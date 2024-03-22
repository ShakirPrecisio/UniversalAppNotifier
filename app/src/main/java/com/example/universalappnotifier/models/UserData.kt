package com.example.universalappnotifier.models

data class UserData(
    var user_email: String? = "",
    var user_name: String? = "",
    var calendar_events: CalendarEvents? = CalendarEvents()
)
