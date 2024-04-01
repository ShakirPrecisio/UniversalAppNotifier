package com.example.universalappnotifier.ui.signin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.universalappnotifier.databinding.ActivityTestBinding
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.outlook.OutlookCalendarEventsFetcher
import com.example.universalappnotifier.utils.Utils
import com.microsoft.identity.client.IAccount
//import com.microsoft.identity.client.AcquireTokenParameters
//import com.microsoft.identity.client.AcquireTokenSilentParameters
//import com.microsoft.identity.client.AuthenticationCallback
//import com.microsoft.identity.client.IAccount
//import com.microsoft.identity.client.IAuthenticationResult
//import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
//import com.microsoft.identity.client.IMultipleAccountPublicClientApplication.RemoveAccountCallback
//import com.microsoft.identity.client.IPublicClientApplication.IMultipleAccountApplicationCreatedListener
//import com.microsoft.identity.client.IPublicClientApplication.LoadAccountsCallback
//import com.microsoft.identity.client.PublicClientApplication
//import com.microsoft.identity.client.SilentAuthenticationCallback
//import com.microsoft.identity.client.exception.MsalClientException
//import com.microsoft.identity.client.exception.MsalException
//import com.microsoft.identity.client.exception.MsalServiceException
//import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.json.JSONObject

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding

    private lateinit var outlookCalendarEventsFetcher: OutlookCalendarEventsFetcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outlookCalendarEventsFetcher = OutlookCalendarEventsFetcher(
                this@TestActivity,
                object: OutlookCalendarEventsFetcher.OutlookCalendarEventsFetcherCallback{
                    override fun onAccountsListFetched(addedAccountsList: List<IAccount>) {
                        Utils.printDebugLog("onAccountsListFetched: $addedAccountsList")
                        if (addedAccountsList.isNotEmpty()) {
                            Utils.printDebugLog("data_data: ${addedAccountsList[0].username}")
                            outlookCalendarEventsFetcher.callGraphApiSilentlyMultipleTimes(addedAccountsList, arrayListOf())
                        }
                    }

                    override fun onEmailIdAdded(emailId: String) {
                        Utils.printDebugLog("emailId: $emailId")
                    }

                    override fun onSingleCalendarEventsFetched(graphResponse: JSONObject) {
                        Utils.printDebugLog("onSingleCalendarEventsFetched")
                    }

                    override fun onMultipleCalendarEventsFetched(graphResponseList: ArrayList<GenericEventModel>) {
                        Utils.printDebugLog("onMultipleCalendarEventsFetched: $graphResponseList")
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

        binding.btnCallGraphInteractively.setOnClickListener {
            outlookCalendarEventsFetcher.callGraphApiInteractively()
        }

        binding.btnCallGraphSilently.setOnClickListener {
            outlookCalendarEventsFetcher.callGraphApiSilently(binding.msgraphUrl.text.toString().toInt())
        }

        binding.btnCallSilentlyLoop.setOnClickListener {
//            outlookCalendarEventsFetcher.callGraphApiSilentlyMultipleTimes(outlookCalendarEventsFetcher.getAccounts())
        }

        binding.btnRemoveAccount.setOnClickListener {
            outlookCalendarEventsFetcher.removeAccount(0)
        }

        outlookCalendarEventsFetcher.initialise()

    }

}