package com.example.universalappnotifier.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.R
import com.example.universalappnotifier.adapters.GenericEventsAdapter
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityDashboardBinding
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.google.GoogleCalendarEventsFetcher
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.models.OutlookCalendarEmailData
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.outlook.OutlookCalendarEventsFetcher
import com.example.universalappnotifier.ui.emailIdList.EmailIdListActivity
import com.example.universalappnotifier.ui.signin.SignInActivity
import com.example.universalappnotifier.utils.DatePickerUtil
import com.example.universalappnotifier.utils.ProgressDialog
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.microsoft.identity.client.IAccount
import kotlinx.coroutines.launch
import org.json.JSONObject
import pub.devrel.easypermissions.EasyPermissions
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date


class DashboardActivity : AppCompatActivity() {

    private lateinit var googleCalendarEvents: ArrayList<GenericEventModel>
    private lateinit var outlookCalendarEvents: ArrayList<GenericEventModel>
    private lateinit var selectedFormattedDate: String
    private lateinit var genericEventsAdapter: GenericEventsAdapter
    private lateinit var userGoogleCalendarEventsEmailIds: List<CalendarEmailData>
    private lateinit var userOutlookCalendarEventsEmailIds: List<OutlookCalendarEmailData>
    private lateinit var userDataResult: FirebaseResponse<UserData?>
    private lateinit var binding: ActivityDashboardBinding

    private lateinit var dashboardViewModel: DashboardViewModel

    private var googleCalendarEventsList: ArrayList<GenericEventModel> = arrayListOf()
    private lateinit var googleCalendarEventsFetcher: GoogleCalendarEventsFetcher

    private var outlookCalendarEventsList: ArrayList<GenericEventModel> = arrayListOf()
    private var outlookCalendarEmailList: ArrayList<CalendarEmailData> = arrayListOf()
    private var outlookCalendarEventsFetcher: OutlookCalendarEventsFetcher? = null

    private lateinit var colorPickerDialog: AlertDialog
    private var unFormattedDate: Date = Calendar.getInstance().time

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
            val intent = Intent(this, EmailIdListActivity::class.java)
            addEmailLauncher.launch(intent)
        }
        selectedFormattedDate = Utils.formatDate(unFormattedDate)
        binding.tvSelectedDate.text = selectedFormattedDate
        binding.tvSelectedDate.setOnClickListener {
            getDateFromUser()
        }
        binding.tvHeaderTitle.setOnClickListener {
            Utils.showShortToast(this@DashboardActivity, "Signing you out")
            FirebaseAuth.getInstance().signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1071450710202-vrdokmjcrsl8nt3tsv393c0la1hcne52.apps.googleusercontent.com")
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

    private fun getDateFromUser() {
        DatePickerUtil.showDatePickerDialog(this@DashboardActivity,
            unFormattedDate,
            object: DatePickerUtil.DateListener{
                override fun onDateSelected(
                    formattedDate: String,
                    unFormattedDate: Date
                ) {
                    this@DashboardActivity.unFormattedDate = unFormattedDate
                    binding.tvSelectedDate.text = formattedDate
                    selectedFormattedDate = formattedDate
                    ProgressDialog.show(this@DashboardActivity,"Please wait")
                    getCalendarEvents()
                }
            })
    }

    private fun attachObservers() {

        dashboardViewModel.isOutlookCalendarEventsFetchedMLiveData.observe(this@DashboardActivity) {
            if (it) {
                genericEventsAdapter = GenericEventsAdapter(
                    outlookCalendarEvents,
                    this@DashboardActivity
                )
                binding.rvGenericEventsList.adapter = genericEventsAdapter
            }
        }

        dashboardViewModel.isGoogleCalendarEventsFetchedMLiveData.observe(this@DashboardActivity) {
            if (it) {
                genericEventsAdapter = GenericEventsAdapter(
                    googleCalendarEventsList,
                    this@DashboardActivity
                )
                binding.rvGenericEventsList.adapter = genericEventsAdapter
            }
        }
    }

    private fun fetchUserData() {
        ProgressDialog.show(this@DashboardActivity,"Please wait")
        lifecycleScope.launch {
            userDataResult = dashboardViewModel.getUserData()
            when (userDataResult) {
                is FirebaseResponse.Success -> {
                    val userData = (userDataResult as FirebaseResponse.Success<UserData?>).data
                    if (userData != null) {
                        Utils.printDebugLog("Fetching_User_Data :: Success")
                        if (userData.calendar_events != null) {
                            Utils.printDebugLog("Got_email_ids_for_calendar_events :: ${userData.calendar_events!!.google_calendar}")
                            userGoogleCalendarEventsEmailIds = userData.calendar_events!!.google_calendar
                            outlookCalendarEmailList = userData.calendar_events!!.outlook_calendar as ArrayList<CalendarEmailData>
                            if (userGoogleCalendarEventsEmailIds.isNotEmpty()) {
                                if (Utils.isAccountPermissionNotGranted(this@DashboardActivity)) {
                                    if (Utils.isDeviceOnline(this@DashboardActivity)) {
                                        if (isGooglePlayServicesAvailable()) {
                                            getAllCalendarEvents()
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
                            Utils.showShortToast(this@DashboardActivity, "Signing you out")
                            FirebaseAuth.getInstance().signOut()

                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken("1071450710202-vrdokmjcrsl8nt3tsv393c0la1hcne52.apps.googleusercontent.com")
                                .requestEmail()
                                .build()
                            val mGoogleSignInClient = GoogleSignIn.getClient(this@DashboardActivity, gso)
                            mGoogleSignInClient.signOut().addOnCompleteListener(this@DashboardActivity) {
                                Utils.printDebugLog("mGoogleSignInClient: Signing out user")
                                finish()
                                startActivity(Intent(this@DashboardActivity, SignInActivity::class.java))
                            }
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

    private fun getAllCalendarEvents() {
        getGoogleCalendarEvents()
        getOutlookCalendarEvents()

//        Utils.twoOptionAlertDialog(
//            this@DashboardActivity,
//            "No events",
//            "There are no events for ${selectedFormattedDate}",
//            "Okay",
//            "Select another date",
//            false,{},
//            {
//                getDateFromUser()
//            })
    }

    private fun getCalendarEvents() {
        googleCalendarEventsFetcher =
            GoogleCalendarEventsFetcher(
                this@DashboardActivity,
                requestAuthorizationLauncher
            )

        lifecycleScope.launch {
            Utils.printDebugLog("googleCalendarEventsList_: $googleCalendarEventsList")
            if (googleCalendarEventsList.size>0) {
                genericEventsAdapter.clear()
            }
            googleCalendarEventsList = googleCalendarEventsFetcher.fetchEvents(userGoogleCalendarEventsEmailIds, unFormattedDate) as ArrayList<GenericEventModel>
            Utils.printDebugLog("googleCalendarEventsList: $googleCalendarEventsList")
            ProgressDialog.dismiss()
            if (googleCalendarEventsList.isNotEmpty()) {
                for (emailData in userGoogleCalendarEventsEmailIds) {
                    googleCalendarEventsList.forEach {
                        if (emailData.email_id == it.event_source_email_id) {
                            it.color = emailData.color
                        }
                    }
                }
                googleCalendarEventsList = sortTimestamps(googleCalendarEventsList)
                genericEventsAdapter = GenericEventsAdapter(
                    googleCalendarEventsList,
                    this@DashboardActivity
                )
                binding.rvGenericEventsList.adapter = genericEventsAdapter
            } else {
                Utils.twoOptionAlertDialog(
                    this@DashboardActivity,
                    "No events",
                    "There are no events for ${selectedFormattedDate}",
                    "Okay",
                    "Select another date",
                    false,{},
                    {
                        getDateFromUser()
                    })
            }
        }
    }

    private fun getGoogleCalendarEvents() {
        googleCalendarEventsFetcher =
            GoogleCalendarEventsFetcher(
                this@DashboardActivity,
                requestAuthorizationLauncher
            )
        googleCalendarEvents = arrayListOf<GenericEventModel>()
        lifecycleScope.launch {
            if (googleCalendarEvents.size>0) {
                genericEventsAdapter.clear()
            }
            googleCalendarEvents = googleCalendarEventsFetcher.fetchEvents(userGoogleCalendarEventsEmailIds, unFormattedDate) as ArrayList<GenericEventModel>
            Utils.printDebugLog("googleCalendarEvents: $googleCalendarEvents")
            ProgressDialog.dismiss()
            if (googleCalendarEvents.isNotEmpty()) {
                for (emailData in userGoogleCalendarEventsEmailIds) {
                    googleCalendarEvents.forEach {
                        if (emailData.email_id == it.event_source_email_id) {
                            it.color = emailData.color
                        }
                    }
                }
                googleCalendarEvents = sortTimestamps(googleCalendarEvents)
            }
        }
    }

    private fun getOutlookCalendarEvents() {
        outlookCalendarEventsFetcher = null
        var outLookCalendarEvents = arrayListOf<GenericEventModel>()
        outlookCalendarEventsFetcher = OutlookCalendarEventsFetcher(
            this@DashboardActivity,
            object: OutlookCalendarEventsFetcher.OutlookCalendarEventsFetcherCallback{
                override fun onAccountsListFetched(addedAccountsList: List<IAccount>) {
                    Utils.printDebugLog("onAccountsListFetched1: $addedAccountsList")
                    outlookCalendarEventsFetcher!!.callGraphApiSilentlyMultipleTimes(addedAccountsList, outlookCalendarEmailList)
                }

                override fun onEmailIdAdded(emailId: String) {
                    Utils.printDebugLog("onEmailIdAdded1: $emailId")
                }

                override fun onSingleCalendarEventsFetched(graphResponse: JSONObject) {
                    Utils.printDebugLog("onSingleCalendarEventsFetched1")
                }

                override fun onMultipleCalendarEventsFetched(genericEventsList: ArrayList<GenericEventModel>) {
                    Utils.printDebugLog("onMultipleCalendarEventsFetched1")
                    outlookCalendarEvents = genericEventsList
                    dashboardViewModel.updateOutLookCalendarEventsFetchedBoolean(true)
                }

                override fun onUserClosedLoginPage() {
                    Utils.printDebugLog("onUserClosedLoginPage1")
                }

                override fun onAccountRemoved() {
                    Utils.printDebugLog("onAccountRemoved1")
                }

                override fun onError(exception: Exception) {
                    Utils.printDebugLog("onError1: $exception")
                }

            })
        outlookCalendarEventsFetcher!!.initialise()
    }

    private fun sortTimestamps(genericEventsList: ArrayList<GenericEventModel>): ArrayList<GenericEventModel> {
        // Define a DateTimeFormatter to parse the timestamps
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

        // Parse the timestamps as ZonedDateTime objects and sort them
        Utils.printDebugLog("sortTimestamps: ${genericEventsList.sortedBy { ZonedDateTime.parse(it.start_time, formatter) }}")
        return ArrayList(genericEventsList.sortedBy { ZonedDateTime.parse(it.start_time, formatter) })
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

    private val requestAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            //call events list
        }
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

    val addEmailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            Utils.printDebugLog("data: ${data}")
            if (data != null) {
                val emailIdList = data.getParcelableArrayListExtra<CalendarEmailData>("emailIdList")
                Utils.printDebugLog("emailIdList: $emailIdList")
            }
        }
    }


}