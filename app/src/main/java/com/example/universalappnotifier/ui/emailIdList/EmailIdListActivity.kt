package com.example.universalappnotifier.ui.emailIdList

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.GridSpacingItemDecoration
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.R
import com.example.universalappnotifier.adapters.AddedEmailIdAdapter
import com.example.universalappnotifier.adapters.ColorAdapter
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityEmailIdListBinding
import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.exceptionhandling.AppErrorCode
import com.example.universalappnotifier.exceptionhandling.CustomException
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.google.GoogleCalendarEventsHelper
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.models.GetEventModel
import com.example.universalappnotifier.outlook.OutlookCalendarEventsFetcher
import com.example.universalappnotifier.utils.Utils
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.CalendarScopes
import com.microsoft.identity.client.IAccount
import org.json.JSONObject

class EmailIdListActivity : AppCompatActivity() {

    private lateinit var emailIdList: ArrayList<CalendarEmailData>
    private lateinit var binding: ActivityEmailIdListBinding

    private lateinit var emailIdListViewModel: EmailIdListViewModel

    private var outlookCalendarEventsFetcher: OutlookCalendarEventsFetcher? = null
    private lateinit var googleCalendarEventsHelper: GoogleCalendarEventsHelper

    private lateinit var colorPickerDialog: AlertDialog

    private val accountPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleCalendarEventsHelper.handleChooseAccountResult(result)
    }

    private val authorizationLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleCalendarEventsHelper.handleAppAuthorizationResult(result)
    }

    private val googlePlayServicesLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Utils.printDebugLog("googlePlayServicesLauncher: called")
            if (result.data != null) {

            } else {
                Utils.showShortToast(this@EmailIdListActivity, "Something went wrong!")
            }
        } else {
            Utils.printDebugLog("Account picker cancelled or failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailIdListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as MyApplication).appRepository
        emailIdListViewModel = ViewModelProvider(
            this,
            EmailIdListViewModelFactory(repository)
        )[EmailIdListViewModel::class.java]

        emailIdListViewModel.getUserAddedEmailIds(
            giveGoogleEmailIds = true,
            giveOutlookEmailIds = true
        )

        binding.fabAddEmailId.setOnClickListener {
            showOptionDialog(this@EmailIdListActivity)
        }

        attachObservers()

    }

    private fun attachObservers() {
        emailIdListViewModel.userEmailIdListLiveData.observe(this@EmailIdListActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    if (!it.data.isNullOrEmpty()) {
                        Utils.printDebugLog("list_list: ${it.data}")
                        emailIdList = it.data
                        val adapter = AddedEmailIdAdapter(it.data, this@EmailIdListActivity)
                        binding.rvAddedEmailIds.adapter = adapter
                    } else {
                        Utils.showShortToast(this@EmailIdListActivity, "No Email Id Added!")
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@EmailIdListActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }

        emailIdListViewModel.isGoogleEmailAddedLiveData.observe(this@EmailIdListActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    if (!it.data.isNullOrEmpty()) {
                        showSuccessDialog()
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@EmailIdListActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }

        emailIdListViewModel.isOutlookEmailAddedLiveData.observe(this@EmailIdListActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    if (!it.data.isNullOrEmpty()) {
                        showSuccessDialog()
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@EmailIdListActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }
    }

    private fun showOptionDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_calendar_option, null)
        val llOutlookCalendarOption = dialogView.findViewById<LinearLayout>(R.id.ll_outlook_calendar)
        val llGoogleCalendarOption = dialogView.findViewById<LinearLayout>(R.id.ll_google_calendar)
        val builder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Select email type")
        val dialog = builder.create()
        llOutlookCalendarOption.setOnClickListener {
            initiateOutlookSingleCalendarEvents()
            dialog.dismiss()
        }
        llGoogleCalendarOption.setOnClickListener {
            initiateGoogleSingleCalendarEvents()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun initiateGoogleSingleCalendarEvents() {
        googleCalendarEventsHelper =
            GoogleCalendarEventsHelper(
                this@EmailIdListActivity,
                this@EmailIdListActivity,
                accountPickerLauncher,
                authorizationLauncher,
                googlePlayServicesLauncher,
                object: GoogleCalendarEventsHelper.GoogleCallbackInterface {
                    override fun onError(exception: Exception) {
                        Utils.printDebugLog("exception__: $exception")
                        when (exception) {
                            is CustomException -> {
                                val code = exception.errorCode
                                val message = exception.message
                                when (code) {
                                    AppErrorCode.GET_ACCOUNTS_PERMISSION_NOT_GRANTED -> {
                                        ActivityCompat.requestPermissions(
                                            this@EmailIdListActivity,
                                            arrayOf(Manifest.permission.GET_ACCOUNTS),
                                            AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
                                        )
                                    }
                                    AppErrorCode.GET_ACCOUNTS_PERMISSION_STILL_NOT_GRANTED -> {
                                        Utils.twoOptionAlertDialog(
                                            this@EmailIdListActivity,
                                            "Permission is needed",
                                            message!!,
                                            "Okay",
                                            "Cancel",
                                            false,
                                            {
                                                ActivityCompat.requestPermissions(
                                                    this@EmailIdListActivity,
                                                    arrayOf(Manifest.permission.GET_ACCOUNTS),
                                                    AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
                                                )
                                            },
                                            {})
                                    }

                                    AppErrorCode.GOOGLE_PLAY_SERVICES_NOT_PRESENT -> {
                                        Utils.singleOptionAlertDialog(
                                            this@EmailIdListActivity,
                                            "Google Play Services",
                                            message!!,
                                            "Okay",
                                            true, {})
                                    }
                                    AppErrorCode.APP_AUTHORIZATION_NEEDED -> {
                                        Utils.singleOptionAlertDialog(
                                            this@EmailIdListActivity,
                                            "Allow access to this app",
                                            message!!,
                                            "Okay",
                                            true, {})
                                    }
                                    AppErrorCode.NO_INTERNET_CONNECTION -> {
                                        Utils.singleOptionAlertDialog(
                                            this@EmailIdListActivity,
                                            "No internet",
                                            message!!,
                                            "Okay",
                                            true, {})
                                    }
                                    AppErrorCode.SOMETHING_WENT_WRONG -> {
                                        Utils.singleOptionAlertDialog(
                                            this@EmailIdListActivity,
                                            "Something went wrong",
                                            "Please try again.",
                                            "Okay",
                                            true, {})
                                    }
                                }
                            }
                        }
                        Utils.printErrorLog("exception: $exception")
                    }

                    override fun onEventsFetchedOutput(emailId: String, output: MutableList<GetEventModel>?) {
                        if (output == null || output.size == 0) {
                            Utils.printDebugLog("Google: veri yok (there is no data)")
                        } else {
                            chooseColorForSelectedEmailId(emailId, EventSource.GOOGLE)
                            for (index in 0 until output.size) {
                                Utils.printDebugLog("${TextUtils.join("\n", output)}")
                                Utils.printDebugLog("Google ${output[index].id.toString()} | ${output[index].summary} | ${output[index].startDate}")
                            }
                        }
                        Utils.printDebugLog("onEventsFetched")
                    }

                })
        googleCalendarEventsHelper.fetchSingleCalendarEvents()
    }

    private fun initiateOutlookSingleCalendarEvents() {
        var emailID = ""
        outlookCalendarEventsFetcher = OutlookCalendarEventsFetcher(
            this@EmailIdListActivity,
            object: OutlookCalendarEventsFetcher.OutlookCalendarEventsFetcherCallback{
                override fun onAccountsListFetched(addedAccountsList: List<IAccount>) {
                    Utils.printDebugLog("onAccountsListFetched: $addedAccountsList")
                    outlookCalendarEventsFetcher!!.callGraphApiInteractively()
//                    if (addedAccountsList.isNotEmpty()) {
//                        outlookCalendarEventsFetcher!!.callGraphApiInteractively()
//                    }
                }

                override fun onEmailIdAdded(emailId: String) {
                    Utils.printDebugLog("onEmailIdAdded: $emailId")
                    emailID = emailId
                }

                override fun onSingleCalendarEventsFetched(graphResponse: JSONObject) {
                    Utils.printDebugLog("onSingleCalendarEventsFetched")
                    chooseColorForSelectedEmailId(emailID, EventSource.OUTLOOK)
                }

                override fun onMultipleCalendarEventsFetched(genericEventsList: ArrayList<GenericEventModel>) {
                    Utils.printDebugLog("onMultipleCalendarEventsFetched")
                }

                override fun onUserClosedLoginPage() {
                    Utils.printDebugLog("onUserClosedLoginPage")
                }

                override fun onAccountRemoved() {
                    Utils.printDebugLog("onAccountRemoved")
                }

                override fun onError(exception: Exception) {
                    Utils.printDebugLog("onError: $exception")
                }

            })
        outlookCalendarEventsFetcher!!.initialise()
    }

    private fun chooseColorForSelectedEmailId(emailId: String, sourceType: EventSource) {

        val colors = listOf(
            Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN,
            Color.rgb(244, 164, 96),
            Color.BLUE, Color.RED, Color.rgb(72, 61, 139),
            Color.rgb(205, 92, 92), Color.rgb(255, 165, 0),
            Color.rgb(102, 205, 170),
            Color.BLACK, Color.DKGRAY
        )

        val numColumns = 5
        val padding = Utils.dpToPx(15, this@EmailIdListActivity)
        val spacing = Utils.dpToPx(15, this@EmailIdListActivity)

        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(this@EmailIdListActivity, numColumns)
            setPadding(padding, Utils.dpToPx(20, this@EmailIdListActivity), padding, padding)
            adapter = ColorAdapter(this@EmailIdListActivity, colors) { selectedColor ->
                Utils.printDebugLog("selected_color: $selectedColor")
                if (sourceType == EventSource.GOOGLE) {
                    emailIdListViewModel.addUserGoogleEmailIdForCalendarEvents(emailId, selectedColor)
                } else if (sourceType == EventSource.OUTLOOK) {
                    emailIdListViewModel.addUserOutlookEmailIdForCalendarEvents(emailId, selectedColor)
                }
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

    private fun showSuccessDialog() {
        Utils.twoOptionAlertDialog(
            this@EmailIdListActivity,
            "Email Added!",
            "Email ID Added successfully.",
            "Add more",
            "OKAY",
            false,
            {
                showOptionDialog(this@EmailIdListActivity)
            },
            {
                emailIdListViewModel.getUserAddedEmailIds(
                    giveGoogleEmailIds = true,
                    giveOutlookEmailIds = true
                )
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                googleCalendarEventsHelper.onPermissionGranted(true)
            } else {
                googleCalendarEventsHelper.onPermissionGranted(false)
            }
        }
    }

}