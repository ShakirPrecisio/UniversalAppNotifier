package com.example.universalappnotifier.ui.dashboard

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityEventListBinding
import com.example.universalappnotifier.google.GoogleCalendarCallbacks
import com.example.universalappnotifier.google.GoogleCalendarEventsFetcher
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.CalendarScopes
import pub.devrel.easypermissions.EasyPermissions

class EventListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventListBinding

    private lateinit var googleCalendarEventsFetcher: GoogleCalendarEventsFetcher

    private var emailIdList = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Utils.isAccountPermissionNotGranted(this@EventListActivity)) {
            askForPermission()
        }

        binding.tvAdd.setOnClickListener {
            val credential =
                GoogleAccountCredential.usingOAuth2(
                    this@EventListActivity,
                    arrayListOf(CalendarScopes.CALENDAR))
                    .setBackOff(ExponentialBackOff())
            val intent = credential.newChooseAccountIntent()
            accountPickerLauncher.launch(intent)
        }

        binding.tvTemp.setOnClickListener {
            if (Utils.isAccountPermissionNotGranted(this@EventListActivity)) {
                if (Utils.isDeviceOnline(this@EventListActivity)) {
                    if (isGooglePlayServicesAvailable()) {
                        if ((Utils.staticList as ArrayList<String>).isNotEmpty()) {
                            googleCalendarEventsFetcher =
                                GoogleCalendarEventsFetcher(
                                    this@EventListActivity,
                                    requestAuthorizationLauncher)
//                            googleCalendarEventsFetcher.fetchEvents(Utils.staticList)
                        } else {
                            val credential =
                                GoogleAccountCredential.usingOAuth2(
                                    this@EventListActivity,
                                    arrayListOf(CalendarScopes.CALENDAR))
                                    .setBackOff(ExponentialBackOff())
                            val intent = credential.newChooseAccountIntent()
                            accountPickerLauncher.launch(intent)
                        }
                    } else {
                        acquireGooglePlayServices()
                    }
                } else {
                    Utils.showLongToast(this@EventListActivity, "No internet")
                }
            } else {
                askForPermission()
            }
        }

    }

    private fun askForPermission() {
        // Request the GET_ACCOUNTS permission via a user dialog
        EasyPermissions.requestPermissions(
            this@EventListActivity,
            "This app needs to access your Google account (via Contacts).",
            AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS,
            Manifest.permission.GET_ACCOUNTS
        )
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this@EventListActivity)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun acquireGooglePlayServices() {
        Utils.printDebugLog("called: acquireGooglePlayServices")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this@EventListActivity)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            this@EventListActivity,
            connectionStatusCode,
            AppConstants.Constants.REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog?.show()
    }

    private val accountPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle successful result
            if (result.data != null) {
                Utils.staticList.add(result.data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!)
                Utils.printDebugLog("list: ${Utils.staticList}")
                showDialog()
            } else {
                //account data is null
            }
        } else {
            // Handle failure or cancellation
            Utils.printDebugLog("Account picker cancelled or failed")
        }
    }

    private val requestAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            //call events list
        }
    }

    private fun showDialog() {
        Utils.twoOptionAlertDialog(
            this@EventListActivity,
            "Email Added!",
            "Email ID Added successfully.",
            "Add more",
            "OKAY",
            false,
            {
                val credential =
                    GoogleAccountCredential.usingOAuth2(
                        this@EventListActivity,
                        arrayListOf(CalendarScopes.CALENDAR))
                        .setBackOff(ExponentialBackOff())
                val intent = credential.newChooseAccountIntent()
                accountPickerLauncher.launch(intent)
            },
            {
                Utils.showShortToast(this@EventListActivity, "okay")
            }
        )
    }


}