package com.example.universalappnotifier.ui.splashscreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.universalappnotifier.R
import com.example.universalappnotifier.ui.dashboard.DashboardActivity
import com.example.universalappnotifier.ui.dashboard.EventListActivity
import com.example.universalappnotifier.ui.signin.SignInActivity
import com.example.universalappnotifier.ui.signin.TestActivity
import com.example.universalappnotifier.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val currentUser = getCurrentlySignedInUser()
//        if (currentUser != null){
//            navigate("DashboardActivity")
//        } else {
//            navigate("SignInActivity")
//        }
        navigate("EventListActivity")

    }

    private fun navigate(nextScreen: String) {
        lifecycleScope.launch {
            delay(1000) // 1 seconds delay for splash screen
            Utils.printDebugLog("Navigating_to: $nextScreen")
            when (nextScreen) {
                "SignInActivity" -> {
                    val intent = Intent(this@SplashActivity, SignInActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                "DashboardActivity" -> {
                    val intent = Intent(this@SplashActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                "EventListActivity" -> {
                    val intent = Intent(this@SplashActivity, EventListActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    fun getCurrentlySignedInUser(): FirebaseUser? {
        return try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }

}