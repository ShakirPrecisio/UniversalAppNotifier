package com.example.universalappnotifier.ui.googleCalEventsDetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.universalappnotifier.repo.AppRepository
import com.google.api.services.calendar.model.Event

class GoogleCalEventDetailsViewModel(private val appRepository: AppRepository): ViewModel() {

    private var _googleCalEventMLiveData = MutableLiveData<Event?>()
    val googleCalEventLiveData: LiveData<Event?>
        get() = _googleCalEventMLiveData

    fun updateEvent(event: Event?) {
        _googleCalEventMLiveData.postValue(event)
    }

    fun updateEventData() {
        _googleCalEventMLiveData.value
    }

}