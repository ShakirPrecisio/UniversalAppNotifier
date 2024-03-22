package com.example.universalappnotifier.google

import com.example.universalappnotifier.models.GenericEventModel

interface GoogleCalendarCallbacks {
    fun onEventsFetched(eventsList: List<GenericEventModel>?)
}