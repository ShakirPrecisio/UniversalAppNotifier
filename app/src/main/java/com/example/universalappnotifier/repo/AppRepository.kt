package com.example.universalappnotifier.repo

import com.example.universalappnotifier.firebase.FirebaseManager
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.firebase.firebaseAwaitOperationCaller
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.OutlookCalendarEmailData
import com.example.universalappnotifier.models.UserData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser

class AppRepository(private val firebaseManager: FirebaseManager) {

    suspend fun signInWithGoogleAccount(authCredential: AuthCredential) =
        firebaseAwaitOperationCaller {
            firebaseManager.signInWithGoogleAccount(authCredential)
        }

    suspend fun addUserIntoFirebase(name: String, email: String): FirebaseResponse<Boolean> {
        return firebaseManager.addUserIntoDatabase(name, email)
    }

    suspend fun getCurrentLoggedInUser(): FirebaseResponse<FirebaseUser?> {
        return firebaseManager.getCurrentLoggedInUser()
    }

    suspend fun getUserData(userId: String): FirebaseResponse<UserData?> {
        return firebaseManager.getUserData(userId)
    }

    suspend fun addUserGoogleEmailIdForCalendarEvents(userId: String, emailId: String, color: Int): FirebaseResponse<List<CalendarEmailData>> {
        return firebaseManager.addUserGoogleEmailIdForCalendarEvents(userId, emailId, color)
    }

    suspend fun addUserOutlookEmailIdForCalendarEvents(userId: String, emailId: String, color: Int): FirebaseResponse<List<OutlookCalendarEmailData>> {
        return firebaseManager.addUserOutlookEmailIdForCalendarEvents(userId, emailId, color)
    }

    suspend fun getUserAddedEmailIds(userId: String, giveGoogleEmailIds: Boolean, giveOutlookEmailIds: Boolean): FirebaseResponse<ArrayList<CalendarEmailData>> {
        return firebaseManager.getUserEmailIds(userId, giveGoogleEmailIds, giveOutlookEmailIds)
    }

}