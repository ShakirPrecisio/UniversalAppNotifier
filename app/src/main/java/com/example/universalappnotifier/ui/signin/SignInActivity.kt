package com.example.universalappnotifier.ui.signin

import android.content.Intent
import com.example.universalappnotifier.MyApplication
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.universalappnotifier.databinding.ActivitySignInBinding
import com.example.universalappnotifier.firebase.GoogleSignInCustomCallback
import com.example.universalappnotifier.firebase.GoogleSignInManager2
import com.example.universalappnotifier.ui.dashboard.DashboardActivity
import com.example.universalappnotifier.utils.Utils

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    private lateinit var googleSignInManager2: GoogleSignInManager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val appRepository = (application as MyApplication).appRepository
        val activityResultRegistry = this.activityResultRegistry
        googleSignInManager2 = GoogleSignInManager2(activityResultRegistry, this@SignInActivity,
            this@SignInActivity, appRepository)

        binding.cvSignInWithGoogle.setOnClickListener {
            if (Utils.isInternetAvailable(this@SignInActivity)) {
                googleSignInManager2.signInWithGoogleAccount(object : GoogleSignInCustomCallback {
                    override fun onSuccess() {
                        Utils.printDebugLog("signInWithGoogleAccount: onSuccess ")
                        Utils.showShortToast(this@SignInActivity, "Signed In successfully!")
                        finish()
                        startActivity(Intent(this@SignInActivity, DashboardActivity::class.java))
                    }

                    override fun onFailure(exception: Exception) {
                        Utils.printErrorLog("signInWithGoogleAccount: onFailure :: $exception")
                        Utils.showShortToast(this@SignInActivity, "Something went wrong! Try again.")
                    }

                    override fun onLoading() {
                        Utils.printDebugLog("signInWithGoogleAccount: onLoading ")
                        Utils.showShortToast(this@SignInActivity, "Please Wait")
                    }

                })
            } else {
                Utils.showLongToast(this@SignInActivity, "Please check your internet connection")
            }
        }

    }

}