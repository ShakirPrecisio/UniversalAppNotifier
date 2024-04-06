package com.example.universalappnotifier.models

import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.enums.EventTime

data class GenericEventModel(
    var event_time: EventTime? = null,
    var event_source: EventSource? = null,
    var event_source_email_id: String? = null,
    var created_by: String? = null,
    var title: String? = null,
    var start_time: String? = null,
    var end_time: String? = null,
    var color: Int? = null,
)
