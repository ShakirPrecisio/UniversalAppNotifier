package com.example.universalappnotifier.google

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.exceptionhandling.AppErrorCode
import com.example.universalappnotifier.exceptionhandling.AppErrorMessages
import com.example.universalappnotifier.exceptionhandling.CustomException
import com.example.universalappnotifier.models.GetEventModel
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.Exception

class GoogleCalendarEventsHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val accountPickerLauncher: ActivityResultLauncher<Intent>?,
    private val authorizationLauncher: ActivityResultLauncher<Intent>?,
    private val googlePlayServicesLauncher: ActivityResultLauncher<Intent>?,
    private val googleCallbackInterface: GoogleCallbackInterface
) {

    private var mCredential: GoogleAccountCredential? = null
    private var mService: Calendar? = null
    private lateinit var emailId: String


    init {
        initCredentials()
    }

    private fun initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
            context,
            CalendarScopes.all()
        )
            .setBackOff(ExponentialBackOff())
        initCalendarBuild(mCredential)
    }

    private fun initCalendarBuild(credential: GoogleAccountCredential?) {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        mService = Calendar.Builder(
            transport, jsonFactory, credential
        )
            .setApplicationName("GetEventCalendar")
            .build()
    }

    fun fetchSingleCalendarEvents() {
        chooseAccount()
    }

    fun onPermissionGranted(value: Boolean) {
        if (value) {
            chooseAccount()
        } else {
            googleCallbackInterface.onError(
                CustomException(
                    AppErrorCode.GET_ACCOUNTS_PERMISSION_STILL_NOT_GRANTED,
                    AppErrorMessages.GET_ACCOUNTS_PERMISSION_STILL_NOT_GRANTED
                )
            )
        }
    }

    private fun chooseAccount() {
        if (!performValidationChecks()) {
            return
        }
        val credential =
            GoogleAccountCredential.usingOAuth2(
                context,
                CalendarScopes.all()
            ).setBackOff(ExponentialBackOff())
        val intent = credential.newChooseAccountIntent()
        accountPickerLauncher?.launch(intent)
    }

    fun handleChooseAccountResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            Utils.printDebugLog("handleChooseAccountResult: called")
            val resultData = result.data
            if (resultData != null && resultData!!.extras != null) {
                val accountName = resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    mCredential!!.selectedAccountName = accountName
                    emailId = accountName
                    makeRequest()
                } else {
                    googleCallbackInterface.onError(
                        CustomException(
                            AppErrorCode.SOMETHING_WENT_WRONG,
                            AppErrorMessages.SOMETHING_WENT_WRONG
                        )
                    )
                }
            } else {
                googleCallbackInterface.onError(
                    CustomException(
                        AppErrorCode.SOMETHING_WENT_WRONG,
                        AppErrorMessages.SOMETHING_WENT_WRONG
                    )
                )
            }
        } else {
            Utils.printDebugLog("Account picker cancelled or failed")
        }
    }

    fun handleAppAuthorizationResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            Utils.printDebugLog("handleAppAuthorizationResult: called")
            if (result.data != null) {
                makeRequest()
            } else {
                googleCallbackInterface.onError(
                    CustomException(
                        AppErrorCode.SOMETHING_WENT_WRONG,
                        AppErrorMessages.SOMETHING_WENT_WRONG
                    )
                )
            }
        } else {
            googleCallbackInterface.onError(
                CustomException(
                    AppErrorCode.APP_AUTHORIZATION_NEEDED,
                    AppErrorMessages.APP_AUTHORIZATION_NEEDED
                )
            )
        }
    }

    private fun makeRequest() {
        if (!performValidationChecks()) {
            return
        }
        var mLastError: Exception? = null

        lifecycleOwner.lifecycleScope.executeAsyncTask(
            onStart = {
                Utils.printDebugLog("onStart")
            },
            doInBackground = {
                try {
                    getDataFromCalendar()
                } catch (e: Exception) {
                    Utils.printDebugLog("e1")
                    mLastError = e
                    lifecycleOwner.lifecycleScope.cancel()
                    null
                }
            },
            onPostExecute = { output ->
                Utils.printDebugLog("onPostExecute")
                googleCallbackInterface.onEventsFetchedOutput(emailId, output)
            },
            onCancelled = {
                Utils.printDebugLog("onCancelled")
                if (mLastError != null) {
                    when (mLastError) {
                        is GooglePlayServicesAvailabilityIOException -> {
                            Utils.printDebugLog("e2")
                            googleCallbackInterface.onError(
                                CustomException(
                                    AppErrorCode.GOOGLE_PLAY_SERVICES_NOT_PRESENT,
                                    AppErrorMessages.GOOGLE_PLAY_SERVICES_NOT_PRESENT
                                )
                            )
                        }

                        is UserRecoverableAuthIOException -> {
                            Utils.printDebugLog("e3")
                            authorizationLauncher?.launch((mLastError as UserRecoverableAuthIOException).intent)
                        }

                        else -> {
                            Utils.printDebugLog("e4")
                            Utils.printDebugLog("The following error occurred: ${mLastError!!.message}")
                        }
                    }
                } else {
                    Utils.printDebugLog("Request cancelled.")
                }
            }
        )
    }

    fun getDataFromCalendar(): MutableList<GetEventModel> {
        val now = DateTime(System.currentTimeMillis())
        val eventStrings = ArrayList<GetEventModel>()
        try {
            val events = mService!!.events().list("primary")
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

                eventStrings.add(
                    GetEventModel(
                        summary = event.summary,
                        startDate = start.toString()
                    )
                )
            }
            return eventStrings

        } catch (exception: UserRecoverableAuthIOException) {
            Utils.printDebugLog("e3.5")
            authorizationLauncher?.launch(exception.intent)
        } catch (e: IOException) {
            Utils.printDebugLog("e4")
            Utils.printDebugLog("Google ${e.message}")
        } catch (exception: Exception) {
            Utils.printDebugLog("excepion occurred ${exception.message}")
            Utils.printDebugLog("e5")
        }
        return eventStrings
    }

    suspend fun getEventById(emailId: String, eventId: String): Event? {
        return try {
            val googleCalendarEvent = withContext(Dispatchers.IO) {
                mCredential!!.selectedAccountName = emailId
                mService!!.events().get("primary", eventId).execute()
            }
            Utils.printDebugLog("eventsById: $googleCalendarEvent")
            Utils.printDebugLog("eventsById_: ${googleCalendarEvent.transparency}")
            googleCalendarEvent
        } catch (exception: Exception) {
            Utils.printErrorLog("exception___: $exception")
            null
        }
    }

    suspend fun updateCalendarEvent(
        event: Event
    ): Event? {
        return try {
//            val list = mService!!.colors().get().execute()
            val updateEvent = Event().apply {
                summary = event.summary
    //            start = EventDateTime().apply {
    //                dateTime = DateTime("2024-04-10T16:00:00+05:30") // Example start time
    //            }
    //            end = EventDateTime().apply {
    //                dateTime = DateTime("2024-04-10T19:00:00+05:30") // Example end time
    //            }
//                start = event.start.dateTime
//                start.dateTime = event.start.dateTime
//                start.date = event.start.date
//                start.timeZone = event.start.timeZone
//
////                end = endDateTime
//                end.dateTime = event.end.dateTime
//                end.date = event.end.date
//                end.timeZone = event.end.timeZone

                location = event.location
                recurrence = arrayListOf("dkjhgc")
                description = event.description
                attendees = listOf(
                    EventAttendee().apply {
                        email = "attendee1@example.com"
                    },
                    EventAttendee().apply {
                        email = "attendee2@example.com"
                    }
                )
                colorId = "1"
                visibility = event.visibility
            }
            val updatedEvent = lifecycleOwner.lifecycleScope.async(Dispatchers.IO) {
                mService!!.events().update("primary", event.id, updateEvent).execute()
            }.await()
             Utils.printDebugLog("uppp: $updatedEvent")
            updatedEvent
        } catch (e: Exception) {
            Utils.printDebugLog("uppp_exce: $e")
            null
        }
    }

    fun <R> CoroutineScope.executeAsyncTask(
        onStart: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit,
        onCancelled: () -> Unit
    ) = launch {
        onStart()
        val result = withContext(Dispatchers.IO) {
            doInBackground() // runs in background thread without blocking the Main Thread
        }
        onPostExecute(result) // runs in Main Thread
        onCancelled()
    }

    private fun performValidationChecks(): Boolean {
        if (!isGooglePlayServicesAvailable()) {
            googleCallbackInterface.onError(
                CustomException(
                    AppErrorCode.GOOGLE_PLAY_SERVICES_NOT_PRESENT,
                    AppErrorMessages.GOOGLE_PLAY_SERVICES_NOT_PRESENT
                )
            )
            return false
        }

        if (!checkPermission()) {
            googleCallbackInterface.onError(
                CustomException(
                    AppErrorCode.GET_ACCOUNTS_PERMISSION_NOT_GRANTED,
                    AppErrorMessages.GET_ACCOUNTS_PERMISSION_NOT_GRANTED
                )
            )
            return false
        }

        if (!isDeviceOnline()) {
            googleCallbackInterface.onError(
                CustomException(
                    AppErrorCode.NO_INTERNET_CONNECTION,
                    AppErrorMessages.NO_INTERNET_CONNECTION
                )
            )
            return false
        }

        return true
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.GET_ACCOUNTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun isDeviceOnline(): Boolean {
        val connMgr =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    interface GoogleCallbackInterface {
        fun onError(exception: Exception)
        fun onEventsFetchedOutput(emailId: String, output: MutableList<GetEventModel>?)
    }

}