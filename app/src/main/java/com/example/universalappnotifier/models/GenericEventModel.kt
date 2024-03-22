package com.example.universalappnotifier.models

import com.example.universalappnotifier.enums.EventSource

data class GenericEventModel(
    var event_source: EventSource? = null,
    var event_source_email_id: String? = null,
    var created_by: String? = null,
    var title: String? = null,
    var start_time: String? = null,
    var end_time: String? = null,
)
