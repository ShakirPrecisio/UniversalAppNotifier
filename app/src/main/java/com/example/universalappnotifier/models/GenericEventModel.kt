package com.example.universalappnotifier.models

import com.example.universalappnotifier.enums.EventSource

data class GenericEventModel(
    var event_source: EventSource?,
    var event_source_email_id: String?,
    var created_by: String?,
    var title: String?,
    var start_time: String?,
    var end_time: String?,
)
