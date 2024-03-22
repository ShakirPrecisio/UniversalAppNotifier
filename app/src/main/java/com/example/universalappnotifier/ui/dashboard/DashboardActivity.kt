package com.example.universalappnotifier.ui.dashboard

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.R
import com.example.universalappnotifier.adapters.GenericEventsAdapter
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityDashboardBinding
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.google.GoogleCalendarEventsFetcher
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.ui.signin.SignInActivity
import com.example.universalappnotifier.utils.ProgressDialog
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.CalendarScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

class DashboardActivity : AppCompatActivity() {

    private lateinit var userDataResult: FirebaseResponse<UserData?>
    private lateinit var binding: ActivityDashboardBinding

    private lateinit var dashboardViewModel: DashboardViewModel

    private var googleCalendarEventsList: List<GenericEventModel>? = null
    private lateinit var googleCalendarEventsFetcher: GoogleCalendarEventsFetcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as MyApplication).appRepository
        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(repository)
        )[DashboardViewModel::class.java]

        if (!Utils.isAccountPermissionNotGranted(this@DashboardActivity)) {
            askForPermission()
        }

        binding.tvAddEmail.setOnClickListener {
            chooseAccount()
        }

        binding.tvSelectedDate.setOnClickListener {
            Utils.showShortToast(this@DashboardActivity, "Signing you out")
            FirebaseAuth.getInstance().signOut()

            Utils.printDebugLog("default_web_client_id: ${getString(R.string.default_web_client_id)}")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                Utils.printDebugLog("mGoogleSignInClient: Signing out user")
                finish()
                startActivity(Intent(this@DashboardActivity, SignInActivity::class.java))
            }
        }
        fetchUserData()
        attachObservers()
    }

    private fun attachObservers() {
        dashboardViewModel.isEmailAddedLiveData.observe(this@DashboardActivity) {
            when (it) {
                 is FirebaseResponse.Success -> {
                     if (it.data!!) {
                         showDialog()
                     }
                 }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@DashboardActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }
    }

    private fun fetchUserData() {
        ProgressDialog.initialize(this@DashboardActivity)
        ProgressDialog.show("Loading your data")
        lifecycleScope.launch {
            userDataResult = dashboardViewModel.getUserData()
            when (userDataResult) {
                is FirebaseResponse.Success -> {
                    val userData = (userDataResult as FirebaseResponse.Success<UserData?>).data
                    if (userData != null) {
                        Utils.printDebugLog("Fetching_User_Data :: Success")
                        if (userData.calendar_events != null) {
                            Utils.printDebugLog("Got_email_ids_for_calendar_events :: ${userData.calendar_events}")
                            val userGoogleCalendarEventsEmailIds = userData.calendar_events!!.google_calendar_email_ids
                            if (userGoogleCalendarEventsEmailIds.isNotEmpty()) {
                                if (Utils.isAccountPermissionNotGranted(this@DashboardActivity)) {
                                    if (Utils.isDeviceOnline(this@DashboardActivity)) {
                                        if (isGooglePlayServicesAvailable()) {
                                            if ((Utils.staticList as ArrayList<String>).isNotEmpty()) {
                                                getCalendarEvents(userGoogleCalendarEventsEmailIds as ArrayList<String>)
                                            } else {
                                                chooseAccount()
                                            }
                                        } else {
                                            acquireGooglePlayServices()
                                        }
                                    } else {
                                        Utils.showLongToast(this@DashboardActivity, "No internet")
                                    }
                                } else {
                                    askForPermission()
                                }
                            } else {
                                ProgressDialog.dismiss()
                                Utils.singleOptionAlertDialog(
                                    this@DashboardActivity,
                                    "No Email Id added.",
                                    "Please click on 'Add Email' to get calendar events",
                                    "Okay",
                                    false)
                            }
                        } else {
                            Utils.printDebugLog("User_Calendar_Events_Not_Found")
                            Utils.showLongToast(this@DashboardActivity, "Please add your email id to get events.")
                        }
                    } else {
                        Utils.printErrorLog("User_Data_Not_Found")
                        Utils.singleOptionAlertDialog(
                            this@DashboardActivity,
                            "Something went wrong",
                            "Please login again.",
                            "OKAY",
                            false
                        ) {
                            ProgressDialog.dismiss()
                            val intent = Intent(this@DashboardActivity, SignInActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }

                is FirebaseResponse.Failure -> {
                    ProgressDialog.dismiss()
                    Utils.printErrorLog("Fetching_User_Data :: Failure: ${(userDataResult as FirebaseResponse.Failure).exception}")
                    Utils.singleOptionAlertDialog(
                        this@DashboardActivity,
                        "Something went wrong",
                        "Please login again.",
                        "OKAY",
                        false
                    ) {
                        val intent = Intent(this@DashboardActivity, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }

                is FirebaseResponse.Loading -> {
                    Utils.printErrorLog("Fetching_User_Data :: Loading")
                }
            }
        }
    }

    private fun chooseAccount() {
        val credential =
            GoogleAccountCredential.usingOAuth2(
                this@DashboardActivity,
                arrayListOf(CalendarScopes.CALENDAR)
            )
                .setBackOff(ExponentialBackOff())
        val intent = credential.newChooseAccountIntent()
        accountPickerLauncher.launch(intent)
    }

    private fun getCalendarEvents(emailIdList: ArrayList<String>) {
        googleCalendarEventsFetcher =
            GoogleCalendarEventsFetcher(
                this@DashboardActivity,
                this@DashboardActivity,
                requestAuthorizationLauncher
            )

        lifecycleScope.launch {
            googleCalendarEventsList = googleCalendarEventsFetcher.fetchEvents(emailIdList)
            Utils.printDebugLog("googleCalendarEventsList: $googleCalendarEventsList")
            if (!googleCalendarEventsList.isNullOrEmpty()) {
                val genericEventsAdapter = GenericEventsAdapter(
                    googleCalendarEventsList!!,
                    this@DashboardActivity
                )
                binding.rvGenericEventsList.adapter = genericEventsAdapter
            } else {
                Utils.showShortToast(this@DashboardActivity, "There are no events for now!")
            }
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode =
            apiAvailability.isGooglePlayServicesAvailable(this@DashboardActivity)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun acquireGooglePlayServices() {
        Utils.printDebugLog("called: acquireGooglePlayServices")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode =
            apiAvailability.isGooglePlayServicesAvailable(this@DashboardActivity)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            this@DashboardActivity,
            connectionStatusCode,
            AppConstants.Constants.REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog?.show()
    }

    private val accountPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                dashboardViewModel.addUserEmailIdForCalendarEvents(result.data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!)
            } else {
                Utils.showShortToast(this@DashboardActivity, "Something went wrong!")
            }
        } else {
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
            this@DashboardActivity,
            "Email Added!",
            "Email ID Added successfully.",
            "Add more",
            "OKAY",
            false,
            {
                chooseAccount()
            },
            {
                Utils.showShortToast(this@DashboardActivity, "Please wait, fetching events")
                getCalendarEvents(arrayListOf())
            }
        )
    }

    private fun askForPermission() {
        // Request the GET_ACCOUNTS permission via a user dialog
        EasyPermissions.requestPermissions(
            this@DashboardActivity,
            "This app needs to access your Google account (via Contacts).",
            AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS,
            Manifest.permission.GET_ACCOUNTS
        )
    }

}