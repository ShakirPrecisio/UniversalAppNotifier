package com.example.universalappnotifier.google

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.contants.AppConstants
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.EasyPermissions
import java.io.IOException

class GoogleCalendarManager2(
    private val context: Context,
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val accountPickerLauncher: ActivityResultLauncher<Intent>,
    private val requestAuthorizationLauncher: ActivityResultLauncher<Intent>
) {

    private var mCredential: GoogleAccountCredential? = null //to access user account
    private var mService: Calendar? = null //To access the calendar

    init {
        initCredentials()
    }


    private fun initCredentials() {
        Utils.printDebugLog("called: initCredentials")
        mCredential = GoogleAccountCredential.usingOAuth2(
            context,
            arrayListOf(CalendarScopes.CALENDAR)
        )
            .setBackOff(ExponentialBackOff())
        initCalendarBuild(mCredential)
    }

    private fun initCalendarBuild(credential: GoogleAccountCredential?) {
        Utils.printDebugLog("called: initCalendarBuild")
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        mService = Calendar.Builder(
            transport, jsonFactory, credential
        )
            .setApplicationName("UniversalAppNotifier")
            .build()

        if (isPermissionNotGranted()) {
            askForPermission()
        }
    }

    fun getCalendarEvents() {
        if (isPermissionNotGranted()) {
            askForPermission()
        }

        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices()
        }

        if (!isDeviceOnline()) {
            Utils.showShortToast(context, "No internet connection available.")
        }

        if (!isAnyAccountAlreadyAdded()) {
            addAccount()
        } else {
            makeRequestTask()
        }



    }

    private fun isAnyAccountAlreadyAdded(): Boolean {
        Utils.printDebugLog("called: isAnyAccountAdded")
        val accountName = activity.getPreferences(Context.MODE_PRIVATE)
            ?.getString(AppConstants.Constants.PREF_ACCOUNT_NAME, null)
        Utils.printDebugLog("isAnyAccountAdded: $accountName")
        return false
    }

    private fun addAccount() {
        val intent = mCredential?.newChooseAccountIntent()
        Utils.printDebugLog("called: addAccount: $intent | $mCredential")
        accountPickerLauncher.launch(intent)
    }

    fun saveAccount(data: Intent) {
        val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val settings = activity.getSharedPreferences(AppConstants.Constants.PREF_ACCOUNT_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(AppConstants.Constants.PREF_ACCOUNT_NAME, accountName)
        editor.apply()
        mCredential!!.selectedAccountName = accountName
        makeRequestTask()
    }

    fun giveAuthorization() {
        makeRequestTask()
    }


    private fun makeRequestTask() {
        Utils.printDebugLog("called: makeRequestTask")
        var mLastError: Exception? = null

        lifecycleOwner.lifecycleScope.executeAsyncTask(
            onStart = {
                Utils.printDebugLog("makeRequestTask: onStart")
            },
            doInBackground = {
                try {
                    mCredential!!.selectedAccountName = "shakir.precisio@gmail.com"
                    getDataFromCalendar()
                } catch (e: Exception) {
                    mLastError = e
                    lifecycleOwner.lifecycleScope.cancel()
                    Utils.printDebugLog("error: $e")
                    null

                }
            },
            onPostExecute = { output ->
                Utils.printDebugLog("makeRequestTask: onPostExecute")
                if (output == null || output.size == 0) {
                    Utils.printDebugLog("makeRequestTask: onPostExecute - veri yok")
                } else {
                    for (index in 0 until output.size) {
//                        Utils.printDebugLog("makeRequestTask: onPostExecute - ${(TextUtils.join("\n", output))}")
                        Utils.printDebugLog("makeRequestTask: onPostExecute - ${output[index].id.toString() + " " + output[index].summary + " " + output[index].startDate}")
                    }
                }
            },
            onCancelled = {
                Utils.printDebugLog("makeRequestTask: onCancelled")
                if (mLastError != null) {
                    when (mLastError) {
                        is GooglePlayServicesAvailabilityIOException -> {
                            showGooglePlayServicesAvailabilityErrorDialog(
                                (mLastError as GooglePlayServicesAvailabilityIOException)
                                    .connectionStatusCode
                            )
                        }

                        is UserRecoverableAuthIOException -> {
                            requestAuthorizationLauncher.launch((mLastError as UserRecoverableAuthIOException).intent)
                        }

                        else -> {
                            Utils.printDebugLog("makeRequestTask: onCancelled - The following error occurred:\\n\" + mLastError!!.message")
                        }
                    }
                } else {
                    Utils.printDebugLog("makeRequestTask: onCancelled - Request cancelled.")
                }
            }
        )
    }

    private fun getDataFromCalendar(): MutableList<GetEventModel> {
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
            requestAuthorizationLauncher.launch(exception.intent)
        } catch (e: IOException) {
            Utils.printDebugLog("Google_Exception: $e")
        }
        return eventStrings
    }

    private fun isPermissionNotGranted(): Boolean {
        return !EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)
    }

    private fun askForPermission() {
        // Request the GET_ACCOUNTS permission via a user dialog
        EasyPermissions.requestPermissions(
            activity,
            "This app needs to access your Google account (via Contacts).",
            AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS,
            Manifest.permission.GET_ACCOUNTS
        )
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun acquireGooglePlayServices() {
        Utils.printDebugLog("called: acquireGooglePlayServices")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
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

    private fun isDeviceOnline(): Boolean {
        Utils.printDebugLog("called: isDeviceOnline")
        val connMgr =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
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

}