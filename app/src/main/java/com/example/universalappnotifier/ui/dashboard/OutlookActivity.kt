package com.example.universalappnotifier.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.universalappnotifier.R
import com.example.universalappnotifier.databinding.ActivityOutlookBinding
import com.example.universalappnotifier.utils.Utils
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication.RemoveAccountCallback
import com.microsoft.identity.client.IPublicClientApplication.IMultipleAccountApplicationCreatedListener
import com.microsoft.identity.client.IPublicClientApplication.LoadAccountsCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.json.JSONObject
import java.util.Arrays
import java.util.Locale

class OutlookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOutlookBinding

    private var mMultipleAccountApp: IMultipleAccountPublicClientApplication? = null
    private var accountList: List<IAccount>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutlookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUI()
// Creates a PublicClientApplication object with res/raw/auth_config_single_account.json

        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(this@OutlookActivity,
            R.raw.auth_config_multiple_account,
            object : IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                    mMultipleAccountApp = application
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    Utils.printErrorLog("exception occurred see below reason")
                    displayError(exception)
                    binding.btnRemoveAccount.isEnabled = false
                    binding.btnCallGraphInteractively.isEnabled = false
                    binding.btnCallGraphSilently.isEnabled = false
                }
            })


    }

    private fun initializeUI() {
        val defaultGraphResourceUrl: String =
            MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me/events"
        binding.msgraphUrl.setText(defaultGraphResourceUrl)
        binding.btnRemoveAccount.setOnClickListener(View.OnClickListener {
            if (mMultipleAccountApp == null) {
                return@OnClickListener
            }

            /*
                 * Removes the selected account and cached tokens from this app (or device, if the device is in shared mode).*/
            mMultipleAccountApp!!.removeAccount(
            accountList!![binding.spinnerAccountList.selectedItemPosition],
            object : RemoveAccountCallback {
                override fun onRemoved() {
                    Toast.makeText(this@OutlookActivity, "Account removed.", Toast.LENGTH_SHORT)
                        .show()

                    /* Reload account asynchronously to get the up-to-date list. */
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                }
            })
        })
        binding.btnCallGraphInteractively.setOnClickListener(View.OnClickListener {
            if (mMultipleAccountApp == null) {
                return@OnClickListener
            }
            val parameters = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(this@OutlookActivity)
                .withScopes(Arrays.asList<String>(*getScopes()!!))
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
                 */mMultipleAccountApp!!.acquireToken(parameters)
        })
        binding.btnCallGraphSilently.setOnClickListener(View.OnClickListener {
            if (mMultipleAccountApp == null) {
                return@OnClickListener
            }
            val selectedAccount = accountList!![binding.spinnerAccountList.selectedItemPosition]
            val silentParameters = AcquireTokenSilentParameters.Builder()
                .forAccount(selectedAccount)
                .fromAuthority(selectedAccount.authority)
                .withScopes(Arrays.asList<String>(*getScopes()!!))
                .forceRefresh(false)
                .withCallback(getAuthSilentCallback())
                .build()

            /*
                 * Performs acquireToken without interrupting the user.
                 *
                 * This requires an account object of the account you're obtaining a token for.
                 * (can be obtained via getAccount()).
                 */mMultipleAccountApp!!.acquireTokenSilentAsync(silentParameters)
        })
    }

    private fun loadAccounts() {
        if (mMultipleAccountApp == null) {
            return
        }
        mMultipleAccountApp!!.getAccounts(object : LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>) {
                // You can use the account data to update your UI or your app database.
                accountList = result
                updateUI(accountList!!)
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback? {
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
                Utils.printErrorLog("Authentication failed: $exception")
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
            }
        }
    }

    private fun getAuthSilentCallback(): SilentAuthenticationCallback? {
        return object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Utils.printDebugLog("Successfully authenticated")
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Utils.printErrorLog("Authentication failed: $exception")
                displayError(exception)
                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception is MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                }
            }
        }
    }

    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
            this@OutlookActivity,
            binding.msgraphUrl.text.toString(),
            authenticationResult.accessToken,
            { response ->
                Utils.printDebugLog("Response: $response")
                displayGraphResult(response)
            },
            { error ->
                Utils.printErrorLog("Error: $error")
                displayError(error)
            })
    }

    private fun getScopes(): Array<String?>? {
        return binding.scope.text.toString().lowercase(Locale.getDefault())
            .split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    }

    private fun displayError(exception: Exception) {
        binding.txtLog.text = exception.toString()
        Utils.printErrorLog("exception: $exception")
        Utils.showShortToast(this@OutlookActivity,"exception: $exception")
    }

    //
    // Helper methods manage UI updates
    // ================================
    // displayGraphResult() - Display the graph response
    // displayError() - Display the graph response
    // updateSignedInUI() - Updates UI when the user is signed in
    // updateSignedOutUI() - Updates UI when app sign out succeeds
    //
    private fun displayGraphResult(graphResponse: JSONObject) {
        binding.txtLog.text = graphResponse.toString()
        Utils.printDebugLog("$graphResponse")
    }

    private fun updateUI(result: List<IAccount>) {
        if (result.size > 0) {
            binding.btnRemoveAccount.isEnabled = true
            binding.btnCallGraphInteractively.isEnabled = true
            binding.btnCallGraphSilently.isEnabled = true
        } else {
            binding.btnRemoveAccount.isEnabled = false
            binding.btnCallGraphInteractively.isEnabled = true
            binding.btnCallGraphSilently.isEnabled = false
        }
        val dataAdapter = ArrayAdapter<String>(
            this@OutlookActivity, android.R.layout.simple_spinner_item,
            object : ArrayList<String?>() {
                init {
                    for (account in result) add(account.username)
                }
            }
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAccountList.adapter = dataAdapter
        dataAdapter.notifyDataSetChanged()
    }

}