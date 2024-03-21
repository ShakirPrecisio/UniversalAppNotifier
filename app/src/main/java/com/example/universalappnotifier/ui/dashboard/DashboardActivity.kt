package com.example.universalappnotifier.ui.dashboard

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.universalappnotifier.R
import com.example.universalappnotifier.databinding.ActivityDashboardBinding
import com.example.universalappnotifier.google.GoogleCalendarCallbacks
import com.example.universalappnotifier.google.GoogleCalendarManager2
import com.example.universalappnotifier.ui.signin.SignInActivity
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private var googleAccountCredential: GoogleAccountCredential? = null
    private lateinit var binding: ActivityDashboardBinding

    private lateinit var calendarManager2: GoogleCalendarManager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calendarManager2 = GoogleCalendarManager2(
            this@DashboardActivity,
            this@DashboardActivity,
            this@DashboardActivity,
            accountPickerLauncher,
            requestAuthorizationLauncher)

        binding.tvSignOutUserGoogleAccount.setOnClickListener {
            Utils.showShortToast(this@DashboardActivity, "Signing you out")
            FirebaseAuth.getInstance().signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                Utils.printDebugLog("mGoogleSignInClient: Signing out user")
                finish()
                startActivity(Intent(this@DashboardActivity, SignInActivity::class.java))
            }
        }

        binding.tvTemp.setOnClickListener {
            calendarManager2.getCalendarEvents()
        }

    }

    private val accountPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle successful result
            if (result.data != null) {
                calendarManager2.saveAccount(result.data!!)
            } else {
                //account data is null
            }
        } else {
            // Handle failure or cancellation
            Utils.printDebugLog("Account picker cancelled or failed")
        }
    }

    private val requestAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            calendarManager2.giveAuthorization()
        }
    }

}