package com.example.universalappnotifier.ui.test

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.universalappnotifier.contants.AppConstants.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
import com.example.universalappnotifier.databinding.ActivityGoogleTestBinding
import com.example.universalappnotifier.exceptionhandling.AppErrorCode
import com.example.universalappnotifier.exceptionhandling.CustomException
import com.example.universalappnotifier.google.GoogleCalendarEventsHelper
import com.example.universalappnotifier.models.GetEventModel
import com.example.universalappnotifier.utils.Utils

class GoogleTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleTestBinding

    private lateinit var googleCalendarEventsHelper: GoogleCalendarEventsHelper

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
                Utils.showShortToast(this@GoogleTestActivity, "Something went wrong!")
            }
        } else {
            Utils.printDebugLog("Account picker cancelled or failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        googleCalendarEventsHelper =
            GoogleCalendarEventsHelper(
                this@GoogleTestActivity,
                this@GoogleTestActivity,
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
                                        this@GoogleTestActivity,
                                        arrayOf(Manifest.permission.GET_ACCOUNTS),
                                        REQUEST_PERMISSION_GET_ACCOUNTS
                                    )
                                }
                                AppErrorCode.GET_ACCOUNTS_PERMISSION_STILL_NOT_GRANTED -> {
                                    Utils.twoOptionAlertDialog(
                                        this@GoogleTestActivity,
                                        "Permission is needed",
                                        "Looks like you have denied the permission. Please give the permission to see calendar events in the app.",
                                        "Okay",
                                        "Cancel",
                                    false,
                                        {
                                            ActivityCompat.requestPermissions(
                                                this@GoogleTestActivity,
                                                arrayOf(Manifest.permission.GET_ACCOUNTS),
                                                REQUEST_PERMISSION_GET_ACCOUNTS
                                            )
                                        },
                                        {})
                                }

                                AppErrorCode.GOOGLE_PLAY_SERVICES_NOT_PRESENT -> {
                                    Utils.singleOptionAlertDialog(
                                        this@GoogleTestActivity,
                                        "Google Play Services",
                                        "Google Play Services is not setup. Please setup Google Play Services in your device first.",
                                        "Okay",
                                        true, {})
                                }
                                AppErrorCode.APP_AUTHORIZATION_NEEDED -> {
                                    Utils.singleOptionAlertDialog(
                                        this@GoogleTestActivity,
                                        "Allow access to this app",
                                        "Looks like you have denied the access. Please allow access for this app to get calendar events.",
                                        "Okay",
                                        true, {})
                                }
                                AppErrorCode.NO_INTERNET_CONNECTION -> {
                                    Utils.singleOptionAlertDialog(
                                        this@GoogleTestActivity,
                                        "No internet",
                                        "Please check your internet connection.",
                                        "Okay",
                                        true, {})
                                }
                                AppErrorCode.SOMETHING_WENT_WRONG -> {
                                    Utils.singleOptionAlertDialog(
                                        this@GoogleTestActivity,
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
                    Utils.printDebugLog("onEventsFetched")
                    if (output == null || output.size == 0) {
                        Utils.printDebugLog("Google: veri yok (there is no data)")
                    } else {
                        for (index in 0 until output.size) {
                            Utils.printDebugLog("${TextUtils.join("\n", output)}")
                            Utils.printDebugLog("Google ${output[index].id.toString()} | ${output[index].summary} | ${output[index].startDate}")
                        }
                    }
                }

            })

        binding.btnTest.setOnClickListener {
            googleCalendarEventsHelper.fetchSingleCalendarEvents()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_GET_ACCOUNTS) {
            Utils.printDebugLog("onRequestPermissionsResult: REQUEST_PERMISSION_GET_ACCOUNTS")
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                googleCalendarEventsHelper.onPermissionGranted(true)
            } else {
                googleCalendarEventsHelper.onPermissionGranted(false)
            }
        } else {
            Utils.printDebugLog("onRequestPermissionsResult: false")
        }
    }
}