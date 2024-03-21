package com.example.universalappnotifier.google

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_ACCOUNT_PICKER
import com.example.universalappnotifier.models.GetEventModel
import com.google.android.gms.common.AccountPicker
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import java.io.IOException

// Define a callback interface
interface CalendarManagerCallback {
    fun onEventsReceived(events: List<GetEventModel>)
    fun onError(exception: Exception)
}

class GoogleCalendarManager(private val activity: AppCompatActivity) {

    private var mCredential: GoogleAccountCredential? = null
    private var mService: Calendar? = null

    init {
        initCredentials()
    }

    private fun initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
            activity,
            arrayListOf(CalendarScopes.CALENDAR)
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
            .setApplicationName("UniversalAppNotifier")
            .build()
    }

    fun getEvents(callback: CalendarManagerCallback) {
        if (!isGooglePlayServicesAvailable()) {
            callback.onError(Exception("Google Play Services not available"))
            return
        }
        if (mCredential!!.selectedAccountName == null) {
            chooseAccount(onAccountChosen = {
                fetchEvents(callback)
            })
        } else if (!isDeviceOnline()) {
            callback.onError(IOException("No internet connection available"))
        } else {
            fetchEvents(callback)
        }
    }

    private fun fetchEvents(callback: CalendarManagerCallback) {
        try {
            val now = DateTime(System.currentTimeMillis())
            val events = mService!!.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
            val items = events.items
            val eventStrings = mutableListOf<GetEventModel>()

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
            callback.onEventsReceived(eventStrings)
        } catch (e: Exception) {
            callback.onError(e)
        }
    }

    private fun chooseAccount(onAccountChosen: () -> Unit) {
        val accountManager = AccountManager.get(activity)
        val accounts = accountManager.getAccountsByType("com.google")

        // If there are no Google accounts on the device, prompt the user to add one
        if (accounts.isEmpty()) {
            // You can handle the case when there are no Google accounts available on the device
            // For example, show a message to the user to add a Google account in the device settings
            // or redirect the user to add an account via the system settings

            // For simplicity, let's assume we redirect the user to add an account via the system settings
            val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT)
            addAccountIntent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            activity.startActivity(addAccountIntent)

            // After the user adds an account, you may want to check again for accounts
            // and call onAccountChosen if an account is selected
        } else {
            // If there are Google accounts, prompt the user to choose one
            val accountPickerIntent = AccountPicker.newChooseAccountIntent(
                null, null, arrayOf("com.google"), false, null, null, null, null
            )
            activity.startActivityForResult(accountPickerIntent, REQUEST_ACCOUNT_PICKER)

            // In your activity's onActivityResult, handle the result and call onAccountChosen if an account is selected
            // For example:
            // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            //     super.onActivityResult(requestCode, resultCode, data)
            //     if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            //         val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            //         // Save the selected account if needed
            //         // For simplicity, let's assume we save the selected account automatically
            //         val settings = activity.getPreferences(Context.MODE_PRIVATE)
            //         val editor = settings?.edit()
            //         editor?.putString(PREF_ACCOUNT_NAME, accountName)
            //         editor?.apply()
            //         onAccountChosen.invoke()
            //     }
            // }
        }
    }


    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun isDeviceOnline(): Boolean {
        val connMgr =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}