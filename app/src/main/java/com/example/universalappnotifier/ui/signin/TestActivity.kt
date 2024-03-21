package com.example.universalappnotifier.ui.signin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.universalappnotifier.R
import com.example.universalappnotifier.databinding.ActivityTestBinding
import com.example.universalappnotifier.utils.Utils
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
import java.util.Arrays
import java.util.Locale

class TestActivity : AppCompatActivity() {

    /*private lateinit var binding: ActivityTestBinding

    private var mMultipleAccountApp: IMultipleAccountPublicClientApplication? = null
    private var accountList: List<IAccount>? = null

//    private lateinit var googleCalendarManager: GoogleCalendarManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create an instance of GoogleCalendarManager passing the activity
//        googleCalendarManager = GoogleCalendarManager(this)

        // Call getEvents method
//        googleCalendarManager.getEvents(
//            onEventsReceived = { events ->
//                Utils.printDebugLog("events_fetch: successful | $events")
//            },
//            onError = { exception ->
//                Utils.printDebugLog("events_fetch: exception | $exception")
//            }
//        )

        /*try {
            Utils.printDebugLog("getting_hash_key")
            val info = packageManager.getPackageInfo(
                "com.example.universalappnotifier",
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d(
                    "KeyHash", "KeyHash:" + Base64.encodeToString(
                        md.digest(),
                        Base64.DEFAULT
                    )
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.printDebugLog("NameNotFoundException: $e")
        } catch (e: NoSuchAlgorithmException) {
            Utils.printDebugLog("NoSuchAlgorithmException: $e")
        } catch (e: Exception) {
            Utils.printDebugLog("Hash_Exception: $e")
        }*/
        initializeUI()

        // Creates a PublicClientApplication object with res/raw/auth_config_multiple_account.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(this@TestActivity,
            R.raw.auth_config_multiple_account,
            object : IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                    mMultipleAccountApp = application
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                    binding.btnRemoveAccount.setEnabled(false)
                    binding.btnCallGraphInteractively.setEnabled(false)
                    binding.btnCallGraphSilently.setEnabled(false)
                }
            })

    }

    private fun initializeUI() {
        val defaultGraphResourceUrl = MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me"
        binding.msgraphUrl.setText(defaultGraphResourceUrl)
        binding.btnRemoveAccount.setOnClickListener(View.OnClickListener {
            if (mMultipleAccountApp == null) {
                return@OnClickListener
            }

            /*Removes the selected account and cached tokens from this app (or device, if the device is in shared mode).*/
            mMultipleAccountApp!!.removeAccount(
            accountList!![binding.spinnerAccountList.getSelectedItemPosition()],
            object : RemoveAccountCallback {
                override fun onRemoved() {
                    Toast.makeText(this@TestActivity, "Account removed.", Toast.LENGTH_SHORT)
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
                .startAuthorizationFromActivity(this@TestActivity)
                .withScopes(Arrays.asList<String>(*getScopes()))
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
        })
        binding.btnCallGraphSilently.setOnClickListener(View.OnClickListener {
            if (mMultipleAccountApp == null) {
                return@OnClickListener
            }
            val selectedAccount = accountList!![binding.spinnerAccountList.getSelectedItemPosition()]
            val silentParameters = AcquireTokenSilentParameters.Builder()
                .forAccount(selectedAccount)
                .fromAuthority(selectedAccount.authority)
                .withScopes(Arrays.asList<String>(*getScopes()))
                .forceRefresh(false)
                .withCallback(getAuthSilentCallback())
                .build()

            /*
                 * Performs acquireToken without interrupting the user.
                 *
                 * This requires an account object of the account you're obtaining a token for.
                 * (can be obtained via getAccount()).
                 */
            mMultipleAccountApp!!.acquireTokenSilentAsync(silentParameters)
        })
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
                Utils.printDebugLog("Authentication failed: $exception")
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

    private fun getAuthSilentCallback(): SilentAuthenticationCallback {
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

    private fun getScopes(): Array<String?> {
        return binding.scope.text.toString().lowercase(Locale.getDefault())
            .split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
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


    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
            this@TestActivity,
            binding.msgraphUrl.text.toString(),
            authenticationResult.getAccessToken(),
            { response -> /* Successfully called graph, process data and send to UI */
                Utils.printDebugLog("Response: $response")
                if (response != null) {
                    displayGraphResult(response)
                } else {
                    Utils.printErrorLog("Response is empty")
                }
            },
            { error ->
                Utils.printDebugLog("Error: $error")
                displayError(error)
            })
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
            this@TestActivity, android.R.layout.simple_spinner_item,
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

    private fun displayGraphResult(graphResponse: JSONObject) {
        binding.txtLog.text = graphResponse.toString()
    }

    private fun displayError(exception: Exception) {
        binding.txtLog.text = exception.toString()
    }*/

}