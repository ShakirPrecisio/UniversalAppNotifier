package com.example.universalappnotifier.models

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.calendar.Calendar

data class EmailData(
    var email_id: String = "",
    var google_account_credential: GoogleAccountCredential?,
    var calendar_service: Calendar?,
)
