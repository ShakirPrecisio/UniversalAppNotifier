package com.example.universalappnotifier.ui.dashboard

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.contants.AppConstants.Constants.PREF_ACCOUNT_NAME
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_ACCOUNT_PICKER
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_AUTHORIZATION
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_GOOGLE_PLAY_SERVICES
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
import com.example.universalappnotifier.databinding.ActivityGoogleCalendarBinding
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

class GoogleCalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleCalendarBinding

    private var mCredential: GoogleAccountCredential? = null //hesabımıza erişim için
    private var mService: Calendar? = null //Takvime erişim için

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initCredentials()
        binding.btnTest.setOnClickListener {
            getResultsFromApi()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                Utils.printDebugLog("This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.")
            } else {
                getResultsFromApi()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null &&
                data.extras != null
            ) {
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
                getResultsFromApi()
            }
        }
    }

    private fun initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
            this@GoogleCalendarActivity,
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
            .setApplicationName("GetEventCalendar")
            .build()
    }

    private fun getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices()
        } else if (mCredential!!.selectedAccountName == null) {
            chooseAccount()
        } else if (!isDeviceOnline()) {
            Utils.printDebugLog("No network connection available.")
        } else {
            makeRequestTask()
        }
    }

    private fun chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this@GoogleCalendarActivity, Manifest.permission.GET_ACCOUNTS
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    //Google consolea erişim izni olup olmadıgına bakıyoruz
    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this@GoogleCalendarActivity)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    //Cihazın Google play servislerini destekleyip desteklemediğini kontrol ediyor
    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this@GoogleCalendarActivity)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            this,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog?.show()
    }

    private fun isDeviceOnline(): Boolean {
        val connMgr =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun makeRequestTask() {
        var mLastError: Exception? = null

        lifecycleScope.executeAsyncTask(
            onStart = {
                Utils.printDebugLog("onStart")
            },
            doInBackground = {
                try {
                    getDataFromCalendar()
                } catch (e: Exception) {
                    Utils.printDebugLog("e1")
                    mLastError = e
                    lifecycleScope.cancel()
                    null
                }
            },
            onPostExecute = { output ->
                Utils.printDebugLog("onPostExecute")
                if (output == null || output.size == 0) {
                    Utils.printDebugLog("Google: veri yok (there is no data)")
                } else {
                    for (index in 0 until output.size) {
                        Utils.printDebugLog("${TextUtils.join("\n", output)}")
                        Utils.printDebugLog("Google ${output[index].id.toString()} | ${output[index].summary} | ${output[index].startDate}")
                    }
                }
            },
            onCancelled = {
                Utils.printDebugLog("onCancelled")
                if (mLastError != null) {
                    if (mLastError is GooglePlayServicesAvailabilityIOException) {
                        Utils.printDebugLog("e2")
                        showGooglePlayServicesAvailabilityErrorDialog(
                            (mLastError as GooglePlayServicesAvailabilityIOException)
                                .connectionStatusCode
                        )
                    } else if (mLastError is UserRecoverableAuthIOException) {
                        Utils.printDebugLog("e3")
                        this.startActivityForResult(
                            (mLastError as UserRecoverableAuthIOException).intent,
                            REQUEST_AUTHORIZATION
                        )
                    } else {
                        Utils.printDebugLog("e4")
                            Utils.printDebugLog("The following error occurred: ${mLastError!!.message}")
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

        }  catch (exception: UserRecoverableAuthIOException) {
            Utils.printDebugLog("e3.5")
            startActivityForResult(exception.intent, REQUEST_AUTHORIZATION)
        } catch (e: IOException) {
            Utils.printDebugLog("e4")
            Utils.printDebugLog("Google ${e.message}")
        } catch (exception: Exception) {
            Utils.printDebugLog("excepion occurred ${exception.message}")
            Utils.printDebugLog("e5")
        }
        return eventStrings
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