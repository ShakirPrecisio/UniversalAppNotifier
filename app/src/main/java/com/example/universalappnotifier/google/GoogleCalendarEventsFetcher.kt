package com.example.universalappnotifier.google

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.EmailData
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.utils.Utils
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Date
import java.util.Calendar as JavaUtilCalendar

class GoogleCalendarEventsFetcher(
    private val context: Context,
    private val requestAuthorizationLauncher: ActivityResultLauncher<Intent>) {

    private var emailList: List<CalendarEmailData>? = null
    private var emailDataList: ArrayList<EmailData> = arrayListOf()

    private lateinit var unFormattedDate: Date

    suspend fun fetchEvents(newEmailList: List<CalendarEmailData>, unFormattedDate: Date): List<GenericEventModel> {
        this.emailList = newEmailList
        this.unFormattedDate = unFormattedDate
        for (item in emailList!!) {
            val credential =
                GoogleAccountCredential.usingOAuth2(
                    context,
                    arrayListOf(CalendarScopes.CALENDAR)
                ).setBackOff(ExponentialBackOff())
            credential!!.selectedAccountName = item.email_id
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            val service =
                Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("UniversalAppNotifier")
                    .build()
            emailDataList.add(EmailData(item.email_id, credential, service))
        }
        return fetchDataFromCalendarMultipleTimesAsync(emailDataList)
    }

    private suspend fun fetchDataFromCalendarMultipleTimesAsync(emailDataList: ArrayList<EmailData>): List<GenericEventModel> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<Deferred<List<GenericEventModel>>>()
        val resultData = mutableListOf<GenericEventModel>()

        for (emailData in emailDataList) {
            resultList.add(async { getDataFromCalendarAsync(emailData) })
        }

        // Wait for all coroutines to complete and accumulate their results
        resultList.forEach { deferredResult ->
            val result = deferredResult.await()
            resultData.addAll(result)
        }

        resultData
    }

    private suspend fun getDataFromCalendarAsync(emailData: EmailData): List<GenericEventModel> = withContext(Dispatchers.IO) {
        val calendar = JavaUtilCalendar.getInstance()
        calendar.time = unFormattedDate

        val genericEventsList = ArrayList<GenericEventModel>()
        try {
            val startOfDay = calendar.clone() as JavaUtilCalendar
            startOfDay.set(JavaUtilCalendar.HOUR_OF_DAY, 0)
            startOfDay.set(JavaUtilCalendar.MINUTE, 0)
            startOfDay.set(JavaUtilCalendar.SECOND, 0)
            startOfDay.set(JavaUtilCalendar.MILLISECOND, 0)

            val endOfDay = calendar.clone() as JavaUtilCalendar
            endOfDay.set(JavaUtilCalendar.HOUR_OF_DAY, 23)
            endOfDay.set(JavaUtilCalendar.MINUTE, 59)
            endOfDay.set(JavaUtilCalendar.SECOND, 59)
            endOfDay.set(JavaUtilCalendar.MILLISECOND, 999)
            Utils.printDebugLog("fetching_google_calendar_events_for: $calendar")
            val events = emailData.calendar_service!!.events().list("primary")
                .setMaxResults(50)
                .setTimeMin(DateTime(startOfDay.time))
                .setTimeMax(DateTime(endOfDay.time))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
            val items = events.items

            for (event in items) {
                val genericEventData = prepareGenericEventData(emailData, event)
                if (genericEventData != null) {
                    genericEventsList.add(genericEventData)
                }
            }
        } catch (exception: UserRecoverableAuthIOException) {
            requestAuthorizationLauncher.launch(exception.intent)
        } catch (e: IOException) {
            Utils.printDebugLog("Google_Exception: $e")
        }
        genericEventsList
    }

    private fun prepareGenericEventData(emailData: EmailData, event: Event?): GenericEventModel? {
        return if (event!=null) {
            val genericEventData = GenericEventModel()
            genericEventData.event_source = EventSource.GOOGLE
            genericEventData.event_source_email_id = emailData.email_id
            genericEventData.created_by = event.creator.email
            genericEventData.title = event.summary
            genericEventData.start_time = event.start.dateTime.toString()
            genericEventData.end_time = event.end.dateTime.toString()
            genericEventData
        } else {
            null
        }
    }

}