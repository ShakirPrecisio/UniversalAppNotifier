package com.example.universalappnotifier.ui.googleCalEventsDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.universalappnotifier.repo.AppRepository

class GoogleCalEventDetailsViewModelFactory(
    private val appRepository: AppRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoogleCalEventDetailsViewModel(appRepository) as T
    }
}