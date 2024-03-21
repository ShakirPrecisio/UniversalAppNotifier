package com.example.universalappnotifier.google

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential

interface GoogleCalendarCallbacks {
    fun onInitCalendarBuild(credential: GoogleAccountCredential?)
}