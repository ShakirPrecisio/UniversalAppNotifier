package com.example.universalappnotifier

import android.app.Application
import com.example.universalappnotifier.firebase.FirebaseManager
import com.example.universalappnotifier.repo.AppRepository
import com.google.firebase.FirebaseApp

class MyApplication: Application() {

    lateinit var appRepository: AppRepository

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        initialize()
    }

    private fun initialize() {
        val firebaseManager = FirebaseManager()
        appRepository = AppRepository(firebaseManager)
    }

}