package com.example.universalappnotifier.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.models.EmailData
import com.example.universalappnotifier.models.GetEventModel
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class GoogleCalendarManager3(
    private val context: Context,
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val requestAuthorizationLauncher: ActivityResultLauncher<Intent>) {

    private var emailList: ArrayList<String>? = null
    private var emailDataList: ArrayList<EmailData> = arrayListOf()

    fun setEmails(newEmailList: ArrayList<String>) {
        emailList = newEmailList
        for (emailId in emailList!!) {
            val credential =
                GoogleAccountCredential.usingOAuth2(
                    context,
                    arrayListOf(CalendarScopes.CALENDAR)
                ).setBackOff(ExponentialBackOff())
            credential!!.selectedAccountName = emailId
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            val service =
                Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("UniversalAppNotifier")
                    .build()
            emailDataList.add(EmailData(emailId, credential, service))
        }
        lifecycleOwner.lifecycleScope.launch {
            val result = fetchDataFromCalendarMultipleTimesAsync(emailDataList)
            Utils.printDebugLog("result_list: $result")
            // Use the result list here
        }
    }

    suspend fun fetchDataFromCalendarMultipleTimesAsync(emailDataList: ArrayList<EmailData>): List<GetEventModel> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<Deferred<List<GetEventModel>>>()
        val resultData = mutableListOf<GetEventModel>()

        for (emailData in emailDataList) {
            resultList.add(async { getDataFromCalendarAsync(emailData.calendar_service) })
        }

        // Wait for all coroutines to complete and accumulate their results
        resultList.forEach { deferredResult ->
            val result = deferredResult.await()
            resultData.addAll(result)
        }

        resultData
    }

    private suspend fun getDataFromCalendarAsync(calendarService: Calendar?): List<GetEventModel> = withContext(Dispatchers.IO) {
        val now = DateTime(System.currentTimeMillis())
        val eventStrings = ArrayList<GetEventModel>()
        try {
            val events = calendarService!!.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
            val items = events.items

            for (event in items) {
                var start = event.start.dateTime
                if (start == null) {
                    start = event.start.date
                }

                //organizor, creator, summary, startDate, endDate,
                eventStrings.add(
                    GetEventModel(
                        summary = event.summary,
                        startDate = start.toString()
                    )
                )
            }
        } catch (exception: UserRecoverableAuthIOException) {
            requestAuthorizationLauncher.launch(exception.intent)
        } catch (e: IOException) {
            Utils.printDebugLog("Google_Exception: $e")
        }
        eventStrings
    }


    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            activity,
            connectionStatusCode,
            AppConstants.Constants.REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog?.show()
    }

}