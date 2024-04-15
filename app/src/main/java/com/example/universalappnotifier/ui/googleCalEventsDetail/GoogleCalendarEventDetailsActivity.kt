package com.example.universalappnotifier.ui.googleCalEventsDetail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.databinding.ActivityGoogleCalendarEventDetailsBinding
import com.example.universalappnotifier.google.GoogleCalendarEventsHelper
import com.example.universalappnotifier.models.GetEventModel
import com.example.universalappnotifier.utils.Utils
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleCalendarEventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleCalendarEventDetailsBinding

    private lateinit var googleCalEventDetailsViewModel: GoogleCalEventDetailsViewModel

    private lateinit var googleCalendarEventsHelper: GoogleCalendarEventsHelper

    private var event: Event? = null

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

        attachObservers()

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
    //            googleCalEventDetailsViewModel.updateEvent(event)
            }
        }
    }

    private fun attachObservers() {
        googleCalEventDetailsViewModel.googleCalEventLiveData.observe(this@GoogleCalendarEventDetailsActivity) {
            if (it != null) {
//                googleCalendarEventsHelper.updateCalendarEvent()
                googleCalEventDetailsViewModel.updateEventData()
            } else {
                Utils.showLongToast(this@GoogleCalendarEventDetailsActivity, "Failed to get event data.")
                finish()
            }
        }
    }
}