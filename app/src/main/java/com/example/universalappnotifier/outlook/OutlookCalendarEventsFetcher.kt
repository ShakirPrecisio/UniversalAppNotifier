package com.example.universalappnotifier.outlook

import android.app.Activity
import android.content.Context
import com.example.universalappnotifier.R
import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.models.OutlookResponseModel
import com.example.universalappnotifier.ui.dashboard.MSGraphRequestWrapper
import com.example.universalappnotifier.utils.Utils
import com.google.gson.Gson
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import org.json.JSONObject

class OutlookCalendarEventsFetcher(private val context: Context, private val outlookCalendarEventsFetcherCallback: OutlookCalendarEventsFetcherCallback) {

    private var numberOfApiCalls = 0
    private var numberOfApiCallsCompleted = 0
    private var mMultipleAccountApp: IMultipleAccountPublicClientApplication? = null
    private lateinit var accountList: List<IAccount>
    private var savedEmailDataList: List<CalendarEmailData> = arrayListOf()

    fun initialise() {
        // Creates a PublicClientApplication object with res/raw/auth_config_multiple_account.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(context,
            R.raw.auth_config_multiple_account,
            object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                    mMultipleAccountApp = application
                    loadAccounts(true)
                }

                override fun onError(exception: MsalException) {
                    Utils.printErrorLog("exception occurred see below reason")
                    displayError(exception)
                }
            })
    }

    private fun loadAccounts(isGiveCallback: Boolean) {
        if (mMultipleAccountApp == null) {
            outlookCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }
        mMultipleAccountApp!!.getAccounts(object : IPublicClientApplication.LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>) {
                accountList = result
                if (isGiveCallback) {
                    outlookCalendarEventsFetcherCallback.onAccountsListFetched(accountList)
                }
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
    }

    fun callGraphApiInteractively() {
        if (mMultipleAccountApp == null) {
            outlookCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(context as Activity?)
            .withScopes(getScopes())
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    Utils.printDebugLog("Successfully authenticated")
                    Utils.printDebugLog("ID Token: ${authenticationResult.account.claims!!["id_token"]}")
                    outlookCalendarEventsFetcherCallback.onEmailIdAdded(authenticationResult.account.username)
                    callGraphAPI(authenticationResult, false)
                    loadAccounts(false)
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                }

                override fun onCancel() {
                    Utils.printErrorLog("User cancelled login.")
                    outlookCalendarEventsFetcherCallback.onUserClosedLoginPage()
                }
            })
            .build()
        mMultipleAccountApp!!.acquireToken(parameters)
    }

    fun callGraphApiSilently(position: Int) {
        if (mMultipleAccountApp == null) {
            outlookCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }

        if (accountList.isEmpty()) {
            outlookCalendarEventsFetcherCallback.onError(Exception("no email added"))
            return
        }

        if (position > accountList.size - 1) {
            outlookCalendarEventsFetcherCallback.onError(Exception("position exceeded"))
            return
        }
        val selectedAccount = accountList[position]
        val silentParameters = AcquireTokenSilentParameters.Builder()
            .forAccount(selectedAccount)
            .fromAuthority(selectedAccount.authority)
            .withScopes(getScopes())
            .forceRefresh(false)
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    Utils.printDebugLog("Successfully authenticated")
                    /* Successfully got a token, use it to call a protected resource - MSGraph */
                    callGraphAPI(authenticationResult, false)
                }

                override fun onError(exception: MsalException) {
                    /* Failed to acquireToken */
                    displayError(Exception("Authentication failed: $exception"))
                }
            })
            .build()
        mMultipleAccountApp!!.acquireTokenSilentAsync(silentParameters)
    }

    fun callGraphApiSilentlyMultipleTimes(accounts: List<IAccount>, emailDataList: ArrayList<CalendarEmailData>) {
        savedEmailDataList = emailDataList
        numberOfApiCalls = accounts.size
        if (mMultipleAccountApp == null) {
            outlookCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }

        if (accountList.isEmpty()) {
            outlookCalendarEventsFetcherCallback.onError(Exception("no email added"))
            return
        }

        if (numberOfApiCalls > accountList.size) {
            outlookCalendarEventsFetcherCallback.onError(Exception("position exceeded"))
            return
        }

        for (account in accountList) {
            val silentParameters = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority)
                .withScopes(getScopes())
                .forceRefresh(false)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        Utils.printDebugLog("Successfully authenticated")
                        callGraphAPI(authenticationResult, true)
                    }

                    override fun onError(exception: MsalException) {
                        /* Failed to acquire Token */
                        displayError(Exception("Authentication failed: $exception"))
                    }
                })
                .build()
            mMultipleAccountApp!!.acquireTokenSilentAsync(silentParameters)
        }

    }

    private fun callGraphAPI(
        authenticationResult: IAuthenticationResult,
        isRecurrenceEnabled: Boolean) {
        val graphResponseList = arrayListOf<JSONObject>()
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
            context,
            "https://graph.microsoft.com/v1.0/me/events",
            authenticationResult.accessToken,
            "2024-03-30T00:00:00",
            "2024-03-30T23:59:59",
            { graphResponse ->
                Utils.printDebugLog("Response: $graphResponse")
                if (isRecurrenceEnabled) {
                    graphResponseList.add(graphResponse)
                    numberOfApiCallsCompleted++
                    if (numberOfApiCalls == numberOfApiCallsCompleted) {
                        val genericEventsList = arrayListOf<GenericEventModel>()
                        for ((index,response) in graphResponseList.withIndex()) {
                            val genericEventData = prepareGenericEventsData("", response, index)
                            if (!genericEventData.isNullOrEmpty()) {
                                genericEventsList.addAll(genericEventData)
                            }
                        }
                        outlookCalendarEventsFetcherCallback.onMultipleCalendarEventsFetched(genericEventsList)
                    }
                } else {
                    outlookCalendarEventsFetcherCallback.onSingleCalendarEventsFetched(graphResponse)
                }
            },
            { error ->
                Utils.printErrorLog("Error: $error")
                displayError(error)
            })
    }

    fun removeAccount(position: Int) {
        if (mMultipleAccountApp == null) {
            outlookCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }

        if (accountList.isEmpty()) {
            outlookCalendarEventsFetcherCallback.onError(Exception("Account list is already empty."))
            return
        }

        mMultipleAccountApp!!.removeAccount(
            accountList[position],
            object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
                override fun onRemoved() {
                    outlookCalendarEventsFetcherCallback.onAccountRemoved()

                    loadAccounts(true)
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                }
            })
    }

    private fun getScopes(): List<String> {
        return listOf("calendars.read")
    }

    private fun displayError(exception: Exception) {
        outlookCalendarEventsFetcherCallback.onError(exception)
    }

    private fun prepareGenericEventsData(emailId: String, eventData: JSONObject, position: Int): ArrayList<GenericEventModel>? {
        val event = Gson().fromJson(eventData.toString(), OutlookResponseModel::class.java)
        val value = event.value
        val genericEventsList = arrayListOf<GenericEventModel>()
        return if (value.isNotEmpty()) {
            for (data in value) {
                val genericEventData = GenericEventModel()
                genericEventData.event_source = EventSource.OUTLOOK
                genericEventData.event_source_email_id = emailId
                genericEventData.created_by = event.value[position].organizer.emailAddress.address
                genericEventData.title = event.value[position].subject
                genericEventData.start_time = event.value[position].start.dateTime
                genericEventData.end_time = event.value[position].end.dateTime
                genericEventData.color = -1111
                genericEventsList.add(genericEventData)
            }
            genericEventsList
        } else {
            null
        }
    }

    interface OutlookCalendarEventsFetcherCallback {
        fun onAccountsListFetched(addedAccountsList: List<IAccount>)
        fun onEmailIdAdded(emailId: String)
        fun onSingleCalendarEventsFetched(graphResponse: JSONObject)
        fun onMultipleCalendarEventsFetched(genericEventsList: ArrayList<GenericEventModel>)
        fun onUserClosedLoginPage()
        fun onAccountRemoved()
        fun onError(exception: Exception)
    }


}