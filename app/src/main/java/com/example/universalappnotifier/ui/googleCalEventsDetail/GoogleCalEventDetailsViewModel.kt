package com.example.universalappnotifier.ui.googleCalEventsDetail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.universalappnotifier.repo.AppRepository
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee

class GoogleCalEventDetailsViewModel(private val appRepository: AppRepository): ViewModel() {

    val availabilityValues = listOf("opaque", "transparent")
    val visibilityValues = listOf("default", "public", "private", "confidential")

    private var _summaryMLiveData = MutableLiveData<String>()
    val summaryLiveData: LiveData<String>
        get() = _summaryMLiveData

    private var _startDateMLiveData = MutableLiveData<String>()
    val startDateLiveData: LiveData<String>
        get() = _startDateMLiveData

    private var _startTimeMLiveData = MutableLiveData<String>()
    val startTimeLiveData: LiveData<String>
        get() = _startTimeMLiveData

    private var _endDateMLiveData = MutableLiveData<String>()
    val endDateLiveData: LiveData<String>
        get() = _endDateMLiveData

    private var _endTimeMLiveData = MutableLiveData<String>()
    val endTimeLiveData: LiveData<String>
        get() = _endTimeMLiveData

    private var _timeZoneMLiveData = MutableLiveData<String>()
    val timeZoneLiveData: LiveData<String>
        get() = _timeZoneMLiveData

    private var _locationMLiveData = MutableLiveData<String>()
    val locationLiveData: LiveData<String>
        get() = _locationMLiveData

    private var _availabilityMLiveData = MutableLiveData<String>()
    val availabilityLiveData: LiveData<String>
        get() = _availabilityMLiveData

    private var _visibilityMLiveData = MutableLiveData<String>()
    val visibilityLiveData: LiveData<String>
        get() = _visibilityMLiveData

    private var _attendeesMLiveData = MutableLiveData<ArrayList<EventAttendee>>()
    val attendeesLiveData: LiveData<ArrayList<EventAttendee>>
        get() = _attendeesMLiveData

    private var _descriptionMLiveData = MutableLiveData<String>()
    val descriptionLiveData: LiveData<String>
        get() = _descriptionMLiveData

    private var _buttonClickableMLiveData = MutableLiveData<Boolean>(false)
    val buttonClickableLiveData: LiveData<Boolean>
        get() = _buttonClickableMLiveData

    private var _eventMLiveData = MutableLiveData<Event?>()
    val eventLiveData: LiveData<Event?>
        get() = _eventMLiveData

    fun setSummary(summary: String) {
        _eventMLiveData.value!!.summary = summary
        if (summary.isBlank()) {
            _summaryMLiveData.postValue("This Field is Mandatory")
        }
        validateAllFields()
    }

    fun setStartDate(startDate: String) {
        _startDateMLiveData.postValue(startDate)
//        _eventMLiveData.value!!.start.date = start
    }

    fun setStartTime(startTime: String) {
        _startTimeMLiveData.postValue(startTime)
    }

    fun setEndDate(endDate: String) {
        _endDateMLiveData.postValue(endDate)
    }

    fun setEndTime(endTime: String) {
        _endTimeMLiveData.postValue(endTime)
    }

    fun setTimeZone(timeZone: String) {
        _timeZoneMLiveData.postValue(timeZone)
    }

    fun setLocation(location: String) {
        _locationMLiveData.postValue(location)
//        validateAllFields()
    }

    fun setAvailability(availability: String) {
        _availabilityMLiveData.postValue(availability)
    }

    fun setVisibility(visibility: String) {
        _visibilityMLiveData.postValue(visibility)
    }

    fun addAttendee(newAttendee: EventAttendee) {
        //
    }

    fun removeAttendee(attendee: EventAttendee) {
        Log.d("TAG", "attendees: ${_eventMLiveData.value?.attendees}")
        Log.d("TAG", "removeAttendee: $attendee")
        val currentEvent = _eventMLiveData.value
        currentEvent?.let { event ->
            val updatedAttendees = event.attendees?.filter { it.email != attendee.email }
            event.attendees = updatedAttendees
//            _attendeesMLiveData.postValue()
            _eventMLiveData.postValue(event)
            Log.d("TAG", "new_attendees: ${_eventMLiveData.value?.attendees}")
        }
    }

    fun updateDescription(description: String) {
        _descriptionMLiveData.postValue(description)
    }

    fun updateEvent(event: Event?) {
        _eventMLiveData.postValue(event)
    }

    fun updateEventAvailability(transparency: String) {
        _eventMLiveData.value!!.transparency = transparency
    }

    fun updateEventVisibility(visibility: String) {
        _eventMLiveData.value!!.visibility = visibility
    }

    fun validateAllFields() {
//        _buttonClickableMLiveData.postValue(
//            (summaryLiveData.value!!.isNotBlank() &&
//                    startDateLiveData.value!!.isNotBlank() &&
//                    startTimeLiveData.value!!.isNotBlank() &&
//                    endDateLiveData.value!!.isNotBlank() &&
//                    endTimeLiveData.value!!.isNotBlank() &&
//                    timeZoneLiveData.value!!.isNotBlank())
//        )
        _buttonClickableMLiveData.postValue(
            (eventLiveData.value!!.summary.isNotBlank())
        )
    }
}