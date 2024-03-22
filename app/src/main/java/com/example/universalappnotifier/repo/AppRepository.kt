package com.example.universalappnotifier.repo

import com.example.universalappnotifier.firebase.FirebaseManager
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.firebase.firebaseAwaitOperationCaller
import com.example.universalappnotifier.models.UserData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser

class AppRepository(private val firebaseManager: FirebaseManager) {

    suspend fun signInWithGoogleAccount(authCredential: AuthCredential) =
        firebaseAwaitOperationCaller {
            firebaseManager.signInWithGoogleAccount(authCredential)
        }

    fun addUserIntoFirebase(name: String, email: String): FirebaseResponse<Boolean> {
        return firebaseManager.addUserIntoDatabase(name, email)
    }

    suspend fun getCurrentLoggedInUser(): FirebaseResponse<FirebaseUser?> {
        return firebaseManager.getCurrentLoggedInUser()
    }

    suspend fun getUserData(userId: String): FirebaseResponse<UserData?> {
        return firebaseManager.getUserData(userId)
    }

    fun addEmailIdForCalendarEvents(userId: String, emailId: String): FirebaseResponse<Boolean> {
        return firebaseManager.addUserEmailIdForCalendarEvents(userId, emailId)
    }

}