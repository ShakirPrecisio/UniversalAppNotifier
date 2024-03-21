package com.example.universalappnotifier.ui.signin

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.contants.AppConstants.Constants.PREF_ACCOUNT_NAME
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_ACCOUNT_PICKER
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_AUTHORIZATION
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_GOOGLE_PLAY_SERVICES
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
import com.example.universalappnotifier.databinding.ActivitySignInBinding
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
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    private var mCredential: GoogleAccountCredential? = null //to access user account
    private var mService: Calendar? = null //To access the calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initCredentials()

        binding.cvSignInWithGoogle.setOnClickListener {
            getResultsFromApi()
        }

    }

    private fun initCredentials() {
        Utils.printDebugLog("called: initCredentials")
        mCredential = GoogleAccountCredential.usingOAuth2(
            this@SignInActivity,
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
    }

    private fun getResultsFromApi() {
        Utils.printDebugLog("called: getResultsFromApi")
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices()
        } else if (mCredential!!.selectedAccountName == null) {
            chooseAccount()
        } else if (!isDeviceOnline()) {
            Utils.showShortToast(this@SignInActivity, "No internet connection available.")
        } else {
            makeRequestTask()
        }
    }

    private fun acquireGooglePlayServices() {
        Utils.printDebugLog("called: acquireGooglePlayServices")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this@SignInActivity)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    private fun chooseAccount() {
        Utils.printDebugLog("called: chooseAccount")
        if (EasyPermissions.hasPermissions(
                this@SignInActivity, Manifest.permission.GET_ACCOUNTS
            )
        ) {
            val accountName = this.getPreferences(Context.MODE_PRIVATE)
                ?.getString(PREF_ACCOUNT_NAME, null)
            if (accountName != null) {
                mCredential!!.selectedAccountName = accountName
                getResultsFromApi()
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                    mCredential!!.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER
                )
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                this,
                "This app needs to access your Google account (via Contacts).",
                REQUEST_PERMISSION_GET_ACCOUNTS,
                Manifest.permission.GET_ACCOUNTS
            )
        }
    }

    private fun makeRequestTask() {
        Utils.printDebugLog("called: makeRequestTask")
        var mLastError: Exception? = null

        lifecycleScope.executeAsyncTask(
            onStart = {
                Utils.printDebugLog("makeRequestTask: onStart")
            },
            doInBackground = {
                try {
                    getDataFromCalendar()
                } catch (e: Exception) {
                    mLastError = e
                    lifecycleScope.cancel()
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
                            this.startActivityForResult(
                                (mLastError as UserRecoverableAuthIOException).intent,
                                REQUEST_AUTHORIZATION
                            )
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
            startActivityForResult(exception.intent, REQUEST_AUTHORIZATION)
        } catch (e: IOException) {
            Utils.printDebugLog("Google_Exception: $e")
        }
        return eventStrings
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                Utils.printDebugLog("called: onActivityResult REQUEST_GOOGLE_PLAY_SERVICES")
            Utils.showLongToast(this@SignInActivity, "This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.")
                Utils.printErrorLog("This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.")
            } else {
                getResultsFromApi()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null &&
                data.extras != null
            ) {
                Utils.printDebugLog("called: onActivityResult REQUEST_ACCOUNT_PICKER")
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    val settings = this.getPreferences(Context.MODE_PRIVATE)
                    val editor = settings?.edit()
                    editor?.putString(PREF_ACCOUNT_NAME, accountName)
                    editor?.apply()
                    mCredential!!.selectedAccountName = accountName
                    getResultsFromApi()
                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode == Activity.RESULT_OK) {
                Utils.printDebugLog("called: onActivityResult REQUEST_AUTHORIZATION")
                getResultsFromApi()
            }
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

    fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        Utils.printDebugLog("called: showGooglePlayServicesAvailabilityErrorDialog")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            this,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog?.show()
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        Utils.printDebugLog("called: isGooglePlayServicesAvailable")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this@SignInActivity)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun isDeviceOnline(): Boolean {
        Utils.printDebugLog("called: isDeviceOnline")
        val connMgr =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

}