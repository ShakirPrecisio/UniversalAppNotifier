package com.example.universalappnotifier.ui.googleCalEventsDetail

import android.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.adapters.GoogleEventAttendeeAdapter
import com.example.universalappnotifier.databinding.ActivityGoogleCalendarEventDetailsBinding
import com.example.universalappnotifier.google.GoogleCalendarEventsHelper
import com.example.universalappnotifier.models.GetEventModel
import com.example.universalappnotifier.utils.Utils
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleCalendarEventDetailsActivity : AppCompatActivity(), GoogleEventAttendeeAdapter.EmailRemovedListener {

    private lateinit var googleEventAttendeeAdapter: GoogleEventAttendeeAdapter
    private lateinit var binding: ActivityGoogleCalendarEventDetailsBinding

    private lateinit var googleCalEventDetailsViewModel: GoogleCalEventDetailsViewModel

    private lateinit var googleCalendarEventsHelper: GoogleCalendarEventsHelper

    private var event: Event? = null

    private var summary: String = ""
    private var startDate: String = ""
    private var startTime: String = ""
    private var endDate: String = ""
    private var endTime: String = ""
    private var timeZone: String = ""
    private var location: String = ""
    private var availabilityPosition: Int? = null
    private var visibilityPosition: Int? = null
    private var attendees: ArrayList<EventAttendee> = arrayListOf()
    private var description: String = ""

    private var emailId: String? = null
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleCalendarEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        emailId = intent?.getStringExtra("email_id")
        eventId = intent?.getStringExtra("event_id")

        val appRepository = (application as MyApplication).appRepository
        googleCalEventDetailsViewModel = ViewModelProvider(
            this,
            GoogleCalEventDetailsViewModelFactory(appRepository))[GoogleCalEventDetailsViewModel::class.java]

        attachTextChangedListeners()

        attachClickListeners()

        googleCalendarEventsHelper = GoogleCalendarEventsHelper(
            this@GoogleCalendarEventDetailsActivity,
            this@GoogleCalendarEventDetailsActivity,
            null,
            null,
            null,
            object : GoogleCalendarEventsHelper.GoogleCallbackInterface {
                override fun onError(exception: Exception) {
                    Utils.printDebugLog("googleCalendarEventsHelper: onError | $exception")
                }

                override fun onEventsFetchedOutput(
                    emailId: String,
                    output: MutableList<GetEventModel>?
                ) {
                    //there is no use of this callback here
                }

            }
        )

        if (!emailId.isNullOrBlank() && !eventId.isNullOrBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                event = googleCalendarEventsHelper.getEventById(emailId!!, eventId!!)
                withContext(Dispatchers.Main) {
                    setData(event)
                }
            }
        }
    }

    private fun attachTextChangedListeners() {
        binding.edtTextInputSummary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                Log.d("TAGjcgdk", "beforeTextChanged: ${p0.toString()}")
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                Log.d("TAGjcgdk", "onTextChanged: ${text.toString()}")
                if (binding.tilSummary.error != null) {
                    binding.tilSummary.error = null
                }
                summary = text.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
//                Log.d("TAGjcgdk", "afterTextChanged: ${p0.toString()}")
            }

        })

        binding.edtTextInputLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                Log.d(TAG, "beforeTextChanged: ${p0.toString()}")
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (binding.tilLocation.error != null) {
                    binding.tilLocation.error = null
                }
                location = text.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
//                Log.d(TAG, "afterTextChanged: ${p0.toString()}")
            }

        })
    }

    private fun attachClickListeners() {
        binding.eventBusyFreeAvailability.setOnClickListener {
            binding.spinnerBusyFree.performClick()
        }

        binding.btnSaveEvent.setOnClickListener {
            if (summary.isEmpty()) {
                binding.tilSummary.error = "This field is mandatory."
                return@setOnClickListener
            }
            if (startDate.isBlank()) {
                return@setOnClickListener
            }
            if (startTime.isBlank()) {
                return@setOnClickListener
            }
            if (endDate.isBlank()) {
                return@setOnClickListener
            }
            if (endTime.isBlank()) {
                return@setOnClickListener
            }
            if (timeZone.isBlank()) {
                return@setOnClickListener
            }
            if (summary.isNotBlank() &&
                startDate.isNotBlank() &&
                startTime.isNotBlank() &&
                endDate.isNotBlank() &&
                endTime.isNotBlank() &&
                timeZone.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val updateEvent = Event().apply {
                        id = eventId
                        summary = "Test Summary"
                        //            start = EventDateTime().apply {
                        //                dateTime = DateTime("2024-04-10T16:00:00+05:30") // Example start time
                        //            }
                        //            end = EventDateTime().apply {
                        //                dateTime = DateTime("2024-04-10T19:00:00+05:30") // Example end time
                        //            }
                        //                start = event.start.dateTime
                        //                    start.dateTime = event!!.start.dateTime
                        //                    start.date = event!!.start.date
                        //                    start.timeZone = event!!.start.timeZone

                        //                end = endDateTime
                        //                    end.dateTime = event!!.end.dateTime
                        //                    end.date = event!!.end.date
                        //                    end.timeZone = event!!.end.timeZone

                        location = "kalua"
                        recurrence = arrayListOf("dkjhgc", "abc")
                        description = "Test description"
                        attendees = listOf(
                            EventAttendee().apply {
                                email = "attendee1@example.com"
                            },
                            EventAttendee().apply {
                                email = "attendee2@example.com"
                            }
                        )
                        colorId = "1"
                        visibility = "private"
                    }
                    googleCalendarEventsHelper.updateCalendarEvent(updateEvent)
                }
            }
        }
    }

    private fun setData(event: Event?) {
        if (event != null) {
            Utils.printDebugLog("event_udated")
            if (event.summary.isNotBlank()) {
                summary = event.summary
                binding.edtTextInputSummary.setText(summary)
            }

            if (event.start.isNotEmpty()) {
                val (date, time) = Utils.returnDateAndTime(event.start.dateTime.toString())
                startDate = date
                startTime = time
                binding.tvEventStartDate.text = startDate
                binding.tvEventStartTime.text = startTime
            }

            if (event.end.isNotEmpty()) {
                val (date, time) = Utils.returnDateAndTime(event.end.dateTime.toString())
                endDate = date
                endTime = time
                binding.tvEventEndDate.text = endDate
                binding.tvEventEndTime.text = endTime
            }

            if (event.start.timeZone.isNotEmpty()) {
                timeZone = "Time Zone: ${event.start.timeZone}"
                binding.tvTimeZone.text = timeZone
            }

            if (!event.location.isNullOrBlank()) {
                location = event.location
                binding.edtTextInputLocation.setText(location)
            }

            availabilityPosition = if (event.transparency.isNullOrBlank()) {
                0
            } else {
                1
            }
            populateEventAvailabilitySpinner()

            visibilityPosition = if (event.visibility.isNullOrBlank()) {
                0
            } else if (event.visibility == "public") {
                1
            } else if (event.visibility == "private") {
                2
            } else if (event.visibility == "confidential") {
                3
            } else {
                0
            }
            populateEventVisibilitySpinner()

            attendees = event.attendees as ArrayList<EventAttendee>
            if (!event.attendees.isNullOrEmpty()) {
                googleEventAttendeeAdapter = GoogleEventAttendeeAdapter(attendees, this)
                binding.rvGuests.adapter = googleEventAttendeeAdapter
            }

            if (event.description != null) {
                description = event.description
                if (description.isNotBlank()) {
                    binding.edtTextInputDescription.setText(description)
                }
            }

        }
    }

    private fun attachObservers() {
//        googleCalEventDetailsViewModel.summaryLiveData.observe(this@GoogleCalendarEventDetailsActivity) {
//            binding.tilSummary.error = it
//        }
//
//        googleCalEventDetailsViewModel.locationLiveData.observe(this@GoogleCalendarEventDetailsActivity) {
//            binding.tilLocation.error = it
//        }
//
//        googleCalEventDetailsViewModel.buttonClickableLiveData.observe(this@GoogleCalendarEventDetailsActivity) {
//            binding.btnSaveEvent.isEnabled = it
//        }

//        googleCalEventDetailsViewModel.googleCalEventLiveData.observe(this@GoogleCalendarEventDetailsActivity) {
//            if (it != null) {
//                Utils.printDebugLog("Event_updated: $it")
//                setData(event)
//            } else {
//                Utils.showLongToast(this@GoogleCalendarEventDetailsActivity, "Failed to get event data.")
//                finish()
//            }
//        }
    }

    private fun populateEventAvailabilitySpinner() {
        val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, googleCalEventDetailsViewModel.availabilityValues.map { it })
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerBusyFree.adapter = spinnerAdapter
        binding.spinnerBusyFree.setSelection(availabilityPosition!!)
        binding.spinnerBusyFree.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = googleCalEventDetailsViewModel.availabilityValues[position]
                availabilityPosition = position
                Utils.printDebugLog("SpinnerSelection: $position, Value: $selectedItem")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Utils.printDebugLog("nothing_selected")
            }
        }
    }

    private fun populateEventVisibilitySpinner() {
        val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, googleCalEventDetailsViewModel.visibilityValues.map { it })
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerVisibility.adapter = spinnerAdapter
        binding.spinnerVisibility.setSelection(visibilityPosition!!)
        binding.spinnerVisibility.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = googleCalEventDetailsViewModel.visibilityValues[position]
                visibilityPosition = position
                Utils.printDebugLog("SpinnerSelection: $position, Value: $selectedItem")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Utils.printDebugLog("nothing_selected")
            }
        }
    }

    override fun onEmailIdRemoved(position: Int, attendee: EventAttendee) {
        googleCalEventDetailsViewModel.removeAttendee(attendee)
    }

}