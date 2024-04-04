package com.example.universalappnotifier.ui.addEmailId

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.GridSpacingItemDecoration
import com.example.universalappnotifier.MyApplication
import com.example.universalappnotifier.adapters.ColorAdapter
import com.example.universalappnotifier.contants.AppConstants
import com.example.universalappnotifier.databinding.ActivityAddEmailIdBinding
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
import com.microsoft.identity.client.IAccount
import org.json.JSONObject

class AddEmailIdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEmailIdBinding
    
    private lateinit var addEmailIdViewModel: AddEmailIdViewModel

    private var outlookCalendarEventsFetcher: OutlookCalendarEventsFetcher? = null
    private lateinit var googleCalendarEventsHelper: GoogleCalendarEventsHelper

    private lateinit var emailIdList: ArrayList<CalendarEmailData>

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
                Utils.showShortToast(this@AddEmailIdActivity, "Something went wrong!")
            }
        } else {
            Utils.printDebugLog("Account picker cancelled or failed")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEmailIdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as MyApplication).appRepository
        addEmailIdViewModel = ViewModelProvider(
            this,
            AddEmailIdViewModelFactory(repository)
        )[AddEmailIdViewModel::class.java]
        
        attachClickListeners()
        attachObservers()
    }

    private fun attachClickListeners() {

        binding.imgBack.setOnClickListener {
            finishThisActivity(false)
        }

        binding.llOutlookCalendarTab.setOnClickListener {
            initiateOutlookSingleCalendarEvents()
        }
        
        binding.llGoogleCalendarTab.setOnClickListener {
            initiateGoogleSingleCalendarEvents()
        }
    }

    private fun attachObservers() {
        addEmailIdViewModel.userEmailIdListLiveData.observe(this@AddEmailIdActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    Utils.printDebugLog("list_list: ${it.data}")
                    if (it.data != null) {
                        emailIdList = it.data
                    }
                    if (emailIdList.isEmpty()) {
                        Utils.showShortToast(this@AddEmailIdActivity, "No Email Id Added!")
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@AddEmailIdActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }

        addEmailIdViewModel.isGoogleEmailAddedLiveData.observe(this@AddEmailIdActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    if (!it.data.isNullOrEmpty()) {
                        showSuccessDialog()
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@AddEmailIdActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }

        addEmailIdViewModel.isOutlookEmailAddedLiveData.observe(this@AddEmailIdActivity) {
            when (it) {
                is FirebaseResponse.Success -> {
                    if (!it.data.isNullOrEmpty()) {
                        showSuccessDialog()
                    }
                }
                is FirebaseResponse.Failure -> {
                    Utils.showShortToast(this@AddEmailIdActivity, "Something went wrong!")
                }
                is FirebaseResponse.Loading -> {

                }
            }
        }
    }

    private fun initiateOutlookSingleCalendarEvents() {
        var emailID = ""
        outlookCalendarEventsFetcher = OutlookCalendarEventsFetcher(
            this@AddEmailIdActivity,
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

    private fun initiateGoogleSingleCalendarEvents() {
        googleCalendarEventsHelper =
            GoogleCalendarEventsHelper(
                this@AddEmailIdActivity,
                this@AddEmailIdActivity,
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
                                            this@AddEmailIdActivity,
                                            arrayOf(Manifest.permission.GET_ACCOUNTS),
                                            AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
                                        )
                                    }
                                    AppErrorCode.GET_ACCOUNTS_PERMISSION_STILL_NOT_GRANTED -> {
                                        Utils.twoOptionAlertDialog(
                                            this@AddEmailIdActivity,
                                            "Permission is needed",
                                            message!!,
                                            "Okay",
                                            "Cancel",
                                            false,
                                            {
                                                ActivityCompat.requestPermissions(
                                                    this@AddEmailIdActivity,
                                                    arrayOf(Manifest.permission.GET_ACCOUNTS),
                                                    AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
                                                )
                                            },
                                            {})
                                    }

                                    AppErrorCode.GOOGLE_PLAY_SERVICES_NOT_PRESENT -> {
                                        Utils.singleOptionAlertDialog(
                                            this@AddEmailIdActivity,
                                            "Google Play Services",
                                            message!!,
                                            "Okay",
                                            true, {})
                                    }
                                    AppErrorCode.APP_AUTHORIZATION_NEEDED -> {
                                        Utils.singleOptionAlertDialog(
                                            this@AddEmailIdActivity,
                                            "Allow access to this app",
                                            message!!,
                                            "Okay",
                                            true, {})
                                    }
                                    AppErrorCode.NO_INTERNET_CONNECTION -> {
                                        Utils.singleOptionAlertDialog(
                                            this@AddEmailIdActivity,
                                            "No internet",
                                            message!!,
                                            "Okay",
                                            true, {})
                                    }
                                    AppErrorCode.SOMETHING_WENT_WRONG -> {
                                        Utils.singleOptionAlertDialog(
                                            this@AddEmailIdActivity,
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
                                Utils.printDebugLog(TextUtils.join("\n", output))
                                Utils.printDebugLog("Google ${output[index].id} | ${output[index].summary} | ${output[index].startDate}")
                            }
                        }
                        Utils.printDebugLog("onEventsFetched: $output")
                    }

                })
        googleCalendarEventsHelper.fetchSingleCalendarEvents()
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
        val padding = Utils.dpToPx(15, this@AddEmailIdActivity)
        val spacing = Utils.dpToPx(15, this@AddEmailIdActivity)

        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(this@AddEmailIdActivity, numColumns)
            setPadding(padding, Utils.dpToPx(20, this@AddEmailIdActivity), padding, padding)
            adapter = ColorAdapter(this@AddEmailIdActivity, colors) { selectedColor ->
                Utils.printDebugLog("selected_color: $selectedColor")
                if (sourceType == EventSource.GOOGLE) {
                    addEmailIdViewModel.addUserGoogleEmailIdForCalendarEvents(emailId, selectedColor)
                } else if (sourceType == EventSource.OUTLOOK) {
                    addEmailIdViewModel.addUserOutlookEmailIdForCalendarEvents(emailId, selectedColor)
                }
                colorPickerDialog.dismiss()
            }
            addItemDecoration(GridSpacingItemDecoration(numColumns, spacing, true))
        }

        colorPickerDialog = AlertDialog.Builder(this)
            .setTitle("Choose a color this Email ID:\n$emailId")
            .setView(recyclerView)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        colorPickerDialog.show()
    }

    private fun showSuccessDialog() {
        Utils.twoOptionAlertDialog(
            this@AddEmailIdActivity,
            "Email Added!",
            "Email ID Added successfully.",
            "OKAY",
            "Add More",
            false,
            {
                finishThisActivity(true)
            },
            {}
        )
    }

    private fun finishThisActivity(isEmailIdAdded: Boolean) {
        val resultIntent = Intent()
        resultIntent.putExtra("is_email_id_added", isEmailIdAdded)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
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