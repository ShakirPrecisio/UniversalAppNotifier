package com.example.universalappnotifier.outlook

import android.app.Activity
import android.content.Context
import com.example.universalappnotifier.R
import com.example.universalappnotifier.ui.dashboard.MSGraphRequestWrapper
import com.example.universalappnotifier.utils.Utils
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.json.JSONObject

class OutlookCalendarEventsFetcher(private val context: Context, private val outlookSingleCalendarEventsFetcherCallback: OutlookSingleCalendarEventsFetcherCallback) {

    private var mMultipleAccountApp: IMultipleAccountPublicClientApplication? = null
    private lateinit var accountList: List<IAccount>

    fun initialise() {
        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(context,
            R.raw.auth_config_multiple_account,
            object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                    mMultipleAccountApp = application
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    Utils.printErrorLog("exception occurred see below reason")
                    displayError(exception)
                }
            })
    }

    private fun loadAccounts() {
        if (mMultipleAccountApp == null) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }
        mMultipleAccountApp!!.getAccounts(object : IPublicClientApplication.LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>) {
                // You can use the account data to update your UI or your app database.
                accountList = result
                outlookSingleCalendarEventsFetcherCallback.onAccountsListFetched(accountList)
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
    }

    fun callGraphInteractively() {
        if (mMultipleAccountApp == null) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(context as Activity?)
            .withScopes(getScopes())
            .withCallback(getAuthInteractiveCallback())
            .build()
        /*
             * Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount()).
             *
             * If acquireTokenSilent() returns an error that requires an interaction,
             * invoke acquireToken() to have the user resolve the interrupt interactively.
             *
             * Some example scenarios are
             *  - password change
             *  - the resource you're acquiring a token for has a stricter set of requirement than your SSO refresh token.
             *  - you're introducing a new scope which the user has never consented for.
             */
        mMultipleAccountApp!!.acquireToken(parameters)
    }

    fun callGraphSilently(position: Int) {
        if (mMultipleAccountApp == null) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }

        if (accountList.isEmpty()) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("no email added"))
            return
        }

        if (position > accountList.size - 1) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("position exceeded"))
            return
        }
        val selectedAccount = accountList[position]
        val silentParameters = AcquireTokenSilentParameters.Builder()
            .forAccount(selectedAccount)
            .fromAuthority(selectedAccount.authority)
            .withScopes(getScopes())
            .forceRefresh(false)
            .withCallback(getAuthSilentCallback())
            .build()

        /*
             * Performs acquireToken without interrupting the user.
             *
             * This requires an account object of the account you're obtaining a token for.
             * (can be obtained via getAccount()).
             */mMultipleAccountApp!!.acquireTokenSilentAsync(silentParameters)
    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Utils.printDebugLog("Successfully authenticated")
                Utils.printDebugLog("ID Token: ${authenticationResult.account.claims!!["id_token"]}")
                /* call graph */
                callGraphAPI(authenticationResult)

                /* Reload account asynchronously to get the up-to-date list. */
                loadAccounts()
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                displayError(exception)
                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Utils.printErrorLog("User cancelled login.")
                outlookSingleCalendarEventsFetcherCallback.onUserClosedLoginPage()
            }
        }
    }

    private fun getAuthSilentCallback(): SilentAuthenticationCallback {
        return object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Utils.printDebugLog("Successfully authenticated")
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                displayError(Exception("Authentication failed: $exception"))
                when (exception) {
                    is MsalClientException -> {
                        /* Exception inside MSAL, more info inside MsalError.java */
                    }

                    is MsalServiceException -> {
                        /* Exception when communicating with the STS, likely config issue */
                    }

                    is MsalUiRequiredException -> {
                        /* Tokens expired or no session, retry with interactive */
                    }
                }
            }
        }
    }


    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
            context,
            "https://graph.microsoft.com/v1.0/me/events",
            authenticationResult.accessToken,
            "2024-03-30T00:00:00",
            "2024-03-30T23:59:59",
            { graphResponse ->
                Utils.printDebugLog("Response: $graphResponse")
                outlookSingleCalendarEventsFetcherCallback.onEventsFetched(graphResponse)
            },
            { error ->
                Utils.printErrorLog("Error: $error")
                displayError(error)
            })
    }

    fun removeAccount(position: Int) {
        if (mMultipleAccountApp == null) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("IMultipleAccountPublicClientApplication is not initialised"))
            return
        }

        if (accountList.isEmpty()) {
            outlookSingleCalendarEventsFetcherCallback.onError(Exception("Account list is already empty."))
            return
        }

        /* Removes the selected account and cached tokens from this app (or device, if the device is in shared mode).*/
        mMultipleAccountApp!!.removeAccount(
            accountList[position],
            object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
                override fun onRemoved() {
                    outlookSingleCalendarEventsFetcherCallback.onAccountRemoved()

                    /* Reload account asynchronously to get the up-to-date list. */
                    loadAccounts()
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
        outlookSingleCalendarEventsFetcherCallback.onError(exception)
    }

    interface OutlookSingleCalendarEventsFetcherCallback {

        fun onAccountsListFetched(addedAccountsList: List<IAccount>)
        fun onEventsFetched(graphResponse: JSONObject)

        fun onUserClosedLoginPage()
        fun onAccountRemoved()
        fun onError(exception: Exception)
    }


}