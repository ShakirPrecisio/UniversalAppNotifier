package com.example.universalappnotifier.ui.googleCalEventsDetail

sealed class ValidationResponse<out T> {
    data class Success<out R>(val data: R?) : ValidationResponse<R>()
    data class Failure(val text: String = "") : ValidationResponse<Nothing>()
}