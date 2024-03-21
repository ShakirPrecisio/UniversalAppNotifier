package com.example.universalappnotifier.firebase

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInManager(private val activityResultRegistry: ActivityResultRegistry,
                          private val lifeCycleOwner: LifecycleOwner,
                          private val activity: Activity
) {

    private var googleSignInClient: GoogleSignInClient
    private lateinit var callback: GoogleSignInCustomCallback

    private val launcher: ActivityResultLauncher<Intent> =
        activityResultRegistry.register("key", lifeCycleOwner,
            ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    if (task.isSuccessful) {
                        val account: GoogleSignInAccount? = task.result
                        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                        val auth = FirebaseAuth.getInstance()
                        auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                callback.onSuccess()
                            } else {
                                callback.onFailure(signInTask.exception ?: Exception("Sign-in failed"))
                            }
                        }
                    }
                } else {
                    callback.onFailure(Exception("Sign-in cancelled and email id is not selected. || ${result.resultCode} | ${result.data}"))
                }
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1071450710202-vrdokmjcrsl8nt3tsv393c0la1hcne52.apps.googleusercontent.com")
            .requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun signIn(callback: GoogleSignInCustomCallback) {
        this.callback = callback
        val signInClient = googleSignInClient.signInIntent
        launcher.launch(signInClient)
    }
}