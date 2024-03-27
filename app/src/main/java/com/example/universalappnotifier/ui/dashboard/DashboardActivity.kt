package com.example.universalappnotifier.ui.dashboard

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.GridSpacingItemDecoration
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.adapters.ColorAdapter
import com.example.universalappnotifier.adapters.GenericEventsAdapter
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityDashboardBinding
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.google.GoogleCalendarEventsFetcher
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.ui.signin.SignInActivity
import com.example.universalappnotifier.utils.DatePickerUtil
import com.example.universalappnotifier.utils.ProgressDialog
import com.example.universalappnotifier.utils.Utils
import com.example.universalappnotifier.utils.Utils.dpToPx
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

class DashboardActivity : AppCompatActivity() {

    private lateinit var selectedFormattedDate: String
    private lateinit var genericEventsAdapter: GenericEventsAdapter
    private lateinit var userGoogleCalendarEventsEmailIds: List<CalendarEmailData>
    private lateinit var userDataResult: FirebaseResponse<UserData?>
    private lateinit var binding: ActivityDashboardBinding

    private lateinit var dashboardViewModel: DashboardViewModel

    private var googleCalendarEventsList: ArrayList<GenericEventModel> = arrayListOf()
    private lateinit var googleCalendarEventsFetcher: GoogleCalendarEventsFetcher

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
            chooseAccount()
        }
        selectedFormattedDate = Utils.formatDate(unFormattedDate)
        binding.tvSelectedDate.text = selectedFormattedDate
        binding.tvSelectedDate.setOnClickListener {
//            Utils.showShortToast(this@DashboardActivity, "Signing you out")
//            FirebaseAuth.getInstance().signOut()
//
//            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken("1071450710202-vrdokmjcrsl8nt3tsv393c0la1hcne52.apps.googleusercontent.com")
//                .requestEmail()
//                .build()
//            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
//            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
//                Utils.printDebugLog("mGoogleSignInClient: Signing out user")
//                finish()
//                startActivity(Intent(this@DashboardActivity, SignInActivity::class.java))
//            }
            getDateFromUser()
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
        dashboardViewModel.isEmailAddedLiveData.observe(this@DashboardActivity) {
            when (it) {
                 is FirebaseResponse.Success -> {
                     if (!it.data.isNullOrEmpty()) {
                         userGoogleCalendarEventsEmailIds = it.data
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
                            if (userGoogleCalendarEventsEmailIds.isNotEmpty()) {
                                if (Utils.isAccountPermissionNotGranted(this@DashboardActivity)) {
                                    if (Utils.isDeviceOnline(this@DashboardActivity)) {
                                        if (isGooglePlayServicesAvailable()) {
                                            getCalendarEvents()
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

    private fun chooseAccount() {
        val credential =
            GoogleAccountCredential.usingOAuth2(
                this@DashboardActivity,
                arrayListOf(CalendarScopes.CALENDAR)
            ).setBackOff(ExponentialBackOff())
        val intent = credential.newChooseAccountIntent()
        accountPickerLauncher.launch(intent)
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

    private val accountPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                chooseColorForSelectedEmailId(result.data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!)
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
                getCalendarEvents()
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

    private fun chooseColorForSelectedEmailId(emailId: String) {

        val colors = listOf(
            Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN,
            Color.rgb(244, 164, 96),
            Color.BLUE, Color.RED, Color.rgb(72, 61, 139),
            Color.rgb(205, 92, 92), Color.rgb(255, 165, 0),
            Color.rgb(102, 205, 170),
            Color.BLACK, Color.DKGRAY
        )

        val numColumns = 5 // Desired number of columns
        val padding = dpToPx(15, this@DashboardActivity) // Convert 15 dp to pixels
        val spacing = dpToPx(15, this@DashboardActivity) // Set the spacing between items in dp

        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(this@DashboardActivity, numColumns)
            setPadding(padding, dpToPx(20, this@DashboardActivity), padding, padding) // Convert padding to pixels
            adapter = ColorAdapter(this@DashboardActivity, colors) { selectedColor ->
                // Do something with the selected color

                // Change Background Color
                Utils.printDebugLog("selected_color: $selectedColor")
                dashboardViewModel.addUserEmailIdForCalendarEvents(emailId, selectedColor)
//                content.setBackgroundColor(selectedColor)
                // Change the App Bar Background Color
//                supportActionBar?.setBackgroundDrawable(ColorDrawable(selectedColor))

                colorPickerDialog.dismiss()
            }
            addItemDecoration(GridSpacingItemDecoration(numColumns, spacing, true))
        }

        colorPickerDialog = AlertDialog.Builder(this)
            .setTitle("Choose a color")
            .setView(recyclerView)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        colorPickerDialog.show()
    }

}