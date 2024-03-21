package com.example.universalappnotifier.firebase

interface GoogleSignInCustomCallback {
    fun onSuccess()
    fun onFailure(exception: Exception)
    fun onLoading()
}