package com.example.universalappnotifier.ui.emailIdList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.universalappnotifier.repo.AppRepository

class EmailIdListViewModelFactory(private val appRepository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EmailIdListViewModel(appRepository) as T
    }

}