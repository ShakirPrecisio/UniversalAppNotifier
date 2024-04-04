package com.example.universalappnotifier.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.R
import com.example.universalappnotifier.adapters.DateListAdapter
import com.example.universalappnotifier.adapters.GenericEventsAdapter
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityDashboardBinding
import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.google.GoogleCalendarEventsFetcher
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.DateItemModel
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.outlook.OutlookCalendarEventsFetcher
import com.example.universalappnotifier.ui.emailIdList.EmailIdListActivity
import com.example.universalappnotifier.ui.signin.SignInActivity
import com.example.universalappnotifier.utils.DateUtil
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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class DashboardActivity : AppCompatActivity(), DateListAdapter.OnDateSelectedListener {

    private lateinit var googleCalendarEvents: ArrayList<GenericEventModel>
    private lateinit var outlookCalendarEvents: ArrayList<GenericEventModel>
    private lateinit var genericEventsAdapter: GenericEventsAdapter
    private lateinit var userGoogleCalendarEventsEmailIds: List<CalendarEmailData>
    private lateinit var userDataResult: FirebaseResponse<UserData?>
    private lateinit var binding: ActivityDashboardBinding

    private lateinit var dashboardViewModel: DashboardViewModel

    private var googleCalendarEventsList: ArrayList<GenericEventModel> = arrayListOf()
    private lateinit var googleCalendarEventsFetcher: GoogleCalendarEventsFetcher

    private var outlookCalendarEmailList: ArrayList<CalendarEmailData> = arrayListOf()
    private var outlookCalendarEventsFetcher: OutlookCalendarEventsFetcher? = null

    private var selectedLocalDate: LocalDate = DateUtil.getCurrentDate()
    private var selectedYear: Int = DateUtil.getCurrentYear()
    private var selectedMonth: Int = DateUtil.getCurrentMonth()
    private var selectedDateOfMonth = DateUtil.getOnlyCurrentDateOfMonth()
    private var selectedTotalNumberOfDaysInMonth = DateUtil.getTotalNumberOfDaysInCurrentMonth()

    private var selectedEventSource = EventSource.ALL

    // Initialize the ActivityResultLauncher
    private val isNewEmailIdAddedResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val isEmailIdListUpdated = data?.getBooleanExtra("is_email_list_updated", false)
            Utils.printDebugLog("Dashboard: isNewEmailIdAdded: $isEmailIdListUpdated")
//            if (isNewEmailIdAdded!!) {
//                emailIdListViewModel.getUserAddedEmailIds(
//                    giveGoogleEmailIds = true,
//                    giveOutlookEmailIds = true
//                )
//            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val repository = (application as MyApplication).appRepository
        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(repository)
        )[DashboardViewModel::class.java]

        Utils.printDebugLog("Details: Current_one-> localDate: $selectedLocalDate | selectedYear: $selectedYear | selectedMonth: $selectedMonth | selectedDateOfMonth: $selectedDateOfMonth | selectedTotalDaysInMonth: $selectedTotalNumberOfDaysInMonth")
        setDateFilterList()
        binding.tvSelectedMonthName.text = DateUtil.getSelectedMonthName(selectedLocalDate)
        binding.tvSelectedYear.text = DateUtil.getSelectedYear(selectedLocalDate).toString()
        attachClickListeners()
        fetchUserData()
        attachObservers()
    }

    private fun attachClickListeners() {
        binding.tvAddEmail.setOnClickListener {
            isNewEmailIdAddedResultLauncher.launch(Intent(this, EmailIdListActivity::class.java))
        }

        binding.llCalendarTab.setOnClickListener {
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

        binding.tvSourceAllFilter.setOnClickListener {
            applyEventSourceFilter(EventSource.ALL)
        }

        binding.tvSourceGoogleFilter.setOnClickListener {
            applyEventSourceFilter(EventSource.GOOGLE)
        }

        binding.tvSourceOutlookFilter.setOnClickListener {
            applyEventSourceFilter(EventSource.OUTLOOK)
        }
    }

    private fun applyEventSourceFilter(eventSource: EventSource) {
        selectedEventSource = eventSource
        when (eventSource) {
            EventSource.ALL -> {
                binding.tvSourceAllFilter.apply {
                    setTextColor(Color.WHITE)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.dark_navy_blue)
                }
                binding.tvSourceGoogleFilter.apply {
                    setTextColor(Color.BLACK)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.white)
                }
                binding.tvSourceOutlookFilter.apply {
                    setTextColor(Color.BLACK)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.white)
                }
            }
            EventSource.GOOGLE -> {
                binding.tvSourceAllFilter.apply {
                    setTextColor(Color.BLACK)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.white)
                }
                binding.tvSourceGoogleFilter.apply {
                    setTextColor(Color.WHITE)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.dark_navy_blue)
                }
                binding.tvSourceOutlookFilter.apply {
                    setTextColor(Color.BLACK)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.white)
                }
            }
            EventSource.OUTLOOK -> {
                binding.tvSourceAllFilter.apply {
                    setTextColor(Color.BLACK)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.white)
                }
                binding.tvSourceGoogleFilter.apply {
                    setTextColor(Color.BLACK)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.white)
                }
                binding.tvSourceOutlookFilter.apply {
                    setTextColor(Color.WHITE)
                    backgroundTintList = ContextCompat.getColorStateList(this@DashboardActivity, R.color.dark_navy_blue)
                }
            }
        }
    }

    private fun setDateFilterList() {
        val dayList = arrayListOf<DateItemModel>()
        for (date in 1..selectedTotalNumberOfDaysInMonth) {
            val localDateItem = LocalDate.of(selectedYear, selectedMonth, date)
            val dayOfWeek = localDateItem.dayOfWeek.getDisplayName((TextStyle.SHORT), Locale.getDefault()).uppercase()
            val formattedDate = localDateItem.format(DateTimeFormatter.ofPattern("dd"))
            dayList.add(DateItemModel(localDateItem, selectedYear, selectedMonth, dayOfWeek, formattedDate, (selectedDateOfMonth==date)))
        }
        val dateListAdapter = DateListAdapter(dayList, this@DashboardActivity, this@DashboardActivity)
        binding.rvDateList.adapter = dateListAdapter
        binding.rvDateList.scrollToPosition(selectedDateOfMonth - 1)
    }

    private fun getDateFromUser() {
        DatePickerUtil.showDatePickerDialog(this@DashboardActivity,
            selectedLocalDate,
            object: DatePickerUtil.DateListener{
                override fun onDateSelected(selectedDate: LocalDate) {
                    ProgressDialog.show(this@DashboardActivity,"Please wait")
                    setSelectedDateData(selectedDate)
                    setDateFilterList()
                    Utils.printDebugLog("Details: Selected_one-> localDate: $selectedLocalDate | selectedYear: $selectedYear | selectedMonth: $selectedMonth | selectedDateOfMonth: $selectedDateOfMonth | selectedTotalDaysInMonth: $selectedTotalNumberOfDaysInMonth")
                    getCalendarEvents1()
                }
            })
    }

    private fun setSelectedDateData(selectedDate: LocalDate) {
        selectedLocalDate = selectedDate
        selectedYear = DateUtil.getSelectedYear(selectedDate)
        selectedMonth = DateUtil.getSelectedMonth(selectedDate)
        selectedDateOfMonth = DateUtil.getOnlySelectedDateOfMonth(selectedDate)
        selectedTotalNumberOfDaysInMonth = DateUtil.getTotalNumberOfDaysInSelectedMonth(selectedDate)
        binding.tvSelectedMonthName.text = DateUtil.getSelectedMonthName(selectedDate)
        binding.tvSelectedYear.text = DateUtil.getSelectedYear(selectedDate).toString()
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
                            if (userData.calendar_events!!.outlook_calendar.isNotEmpty()) {
                                outlookCalendarEmailList = userData.calendar_events!!.outlook_calendar as ArrayList<CalendarEmailData>
                            }
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

    private fun getCalendarEvents() {

        getGoogleCalendarEvents()
//        getOutlookCalendarEvents()

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

    private fun getCalendarEvents1() {
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
            googleCalendarEventsList = googleCalendarEventsFetcher.fetchEvents(userGoogleCalendarEventsEmailIds, selectedLocalDate) as ArrayList<GenericEventModel>
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
                    "There are no events for ",
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
            googleCalendarEvents = googleCalendarEventsFetcher.fetchEvents(userGoogleCalendarEventsEmailIds, selectedLocalDate) as ArrayList<GenericEventModel>
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

    override fun onDateSelected(dateItemData: DateItemModel) {
        setSelectedDateData(dateItemData.localDate)
    }


}