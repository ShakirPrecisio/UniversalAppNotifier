package com.example.universalappnotifier.models

import java.time.LocalDate

data class DateItemModel(
    var localDate: LocalDate,
    var year: Int,
    var month: Int,
    var day: String,
    var date: String,
    var is_selected: Boolean
)
