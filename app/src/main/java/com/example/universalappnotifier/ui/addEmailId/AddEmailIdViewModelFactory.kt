package com.example.universalappnotifier.ui.addEmailId

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.universalappnotifier.repo.AppRepository

class AddEmailIdViewModelFactory(private val appRepository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddEmailIdViewModel(appRepository) as T
    }

}