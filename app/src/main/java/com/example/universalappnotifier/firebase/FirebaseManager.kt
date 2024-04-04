package com.example.universalappnotifier.firebase

import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.EmailData
import com.example.universalappnotifier.models.OutlookCalendarEmailData
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun signInWithGoogleAccount(authCredential: AuthCredential) :Task<AuthResult> {
        return auth.signInWithCredential(authCredential)
    }

    suspend fun addUserIntoDatabase(name: String, email: String): FirebaseResponse<Boolean> {
        return try {
            val uid = auth.currentUser?.uid.toString()
            val retrievedUser = getUserByUID(uid)
            when (retrievedUser) {
                is FirebaseResponse.Success -> {
                    // User data retrieval successful

                    val userDataExists = retrievedUser.data != null
                    if (userDataExists) {
                        // User data exists, update the existing entry
                        FirebaseResponse.Success(true)
                    } else {
                        // User data doesn't exist, add a new entry
                        databaseReference.child("users")
                            .child(uid)
                            .setValue(
                                UserData(
                                    user_name = name,
                                    user_email = email
                                )
                            ).await()
                    }
                    // Return success response
                    FirebaseResponse.Success(true)
                }
                is FirebaseResponse.Failure -> {
                    // User data retrieval failed, return failure response
                    FirebaseResponse.Failure(retrievedUser.exception)
                }

                else -> {
                    FirebaseResponse.Failure(Exception("Something went wrong"))
                }
            }
        } catch (e: Exception) {
            // Exception occurred, return failure response
            FirebaseResponse.Failure(e)
        }
    }

    suspend fun getCurrentLoggedInUser(): FirebaseResponse<FirebaseUser?> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                FirebaseResponse.Success(auth.currentUser)
            } catch (e: Exception) {
                FirebaseResponse.Failure(e)
            }
        }
    }

    private suspend fun getUserByUID(uid: String): FirebaseResponse<UserData?> {
        return try {
            val userReference = databaseReference.child("users").child(uid)
            val snapshot = userReference.get().await()

            val userData = snapshot.getValue(UserData::class.java)
            FirebaseResponse.Success(userData)
        } catch (e: Exception) {
            FirebaseResponse.Failure(e)
        }
    }


    suspend fun getUserData(userId: String): FirebaseResponse<UserData?> {
        return try {
            val usersRef = databaseReference.child("users").child(userId).get().await()
            if (usersRef.exists()) {
                val userModel = usersRef.getValue(UserData::class.java)
                FirebaseResponse.Success(userModel)
            } else {
                FirebaseResponse.Success(null)
            }
        } catch (e: Exception) {
            FirebaseResponse.Failure(e)
        }
    }

    suspend fun addUserGoogleEmailIdForCalendarEvents(
        userId: String,
        emailId: String,
        color: Int
    ): FirebaseResponse<List<CalendarEmailData>> {
        return withContext(Dispatchers.IO) {
            try {
                val userReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child("google_calendar")

                val snapshot = userReference.get().await()
                val currentEmailIds = snapshot.children.mapNotNull { data ->
                    data.getValue(CalendarEmailData::class.java)
                }

                val updatedEmailIds = currentEmailIds.toMutableList()
                updatedEmailIds.add(CalendarEmailData(emailId, "google", color))

                userReference.setValue(updatedEmailIds).await()

                // Return success response
                FirebaseResponse.Success(updatedEmailIds.toList())

            } catch (e: Exception) {
                // Return failure response
                FirebaseResponse.Failure(e)
            }
        }
    }


    suspend fun addUserOutlookEmailIdForCalendarEvents(
        userId: String,
        emailId: String,
        color: Int
    ): FirebaseResponse<List<OutlookCalendarEmailData>> {
        return withContext(Dispatchers.IO) {
            try {
                val userReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child("outlook_calendar")

                val snapshot = userReference.get().await()
                val currentEmailIds = snapshot.children.mapNotNull { data ->
                    data.getValue(OutlookCalendarEmailData::class.java)
                }

                val updatedEmailIds = currentEmailIds.toMutableList()
                updatedEmailIds.add(OutlookCalendarEmailData(emailId, "outlook", color))

                userReference.setValue(updatedEmailIds).await()

                // Return success response
                FirebaseResponse.Success(updatedEmailIds.toList())

            } catch (e: Exception) {
                // Return failure response
                FirebaseResponse.Failure(e)
            }
        }
    }

    suspend fun getUserEmailIds(userId: String, giveGoogleEmailIds: Boolean, giveOutlookEmailIds: Boolean): FirebaseResponse<ArrayList<CalendarEmailData>> {
        return withContext(Dispatchers.IO) {
            try {
                val userGoogleEmailIdsReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child("google_calendar")

                val userOutlookEmailIdsReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child("outlook_calendar")

                val snapshotGoogleEmailIds = userGoogleEmailIdsReference.get().await()
                val userGoogleEmailIds = snapshotGoogleEmailIds.children.mapNotNull { data ->
                    data.getValue(CalendarEmailData::class.java)
                }

                val snapshotOutlookEmailIds = userOutlookEmailIdsReference.get().await()
                val userOutlookEmailIds = snapshotOutlookEmailIds.children.mapNotNull { data ->
                    data.getValue(CalendarEmailData::class.java)
                }
                val emailIdList = arrayListOf<CalendarEmailData>()
                if (giveGoogleEmailIds) {
                    emailIdList.addAll(userGoogleEmailIds)
                }
                if (giveOutlookEmailIds) {
                    emailIdList.addAll(userOutlookEmailIds)
                }
                FirebaseResponse.Success(emailIdList)

            } catch (e: Exception) {
                FirebaseResponse.Failure(e)
            }
        }
    }

    suspend fun removeEmailId(userId: String, data: CalendarEmailData, emailIdType: String): FirebaseResponse<ArrayList<CalendarEmailData>> {
        return withContext(Dispatchers.IO) {
            try {
                val userGoogleEmailIdsReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child("google_calendar")

                val userOutlookEmailIdsReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child("outlook_calendar")

                val snapshotGoogleEmailIds = userGoogleEmailIdsReference.get().await()
                val userGoogleEmailIds = snapshotGoogleEmailIds.children.mapNotNull { data ->
                    data.getValue(CalendarEmailData::class.java)
                }

                val snapshotOutlookEmailIds = userOutlookEmailIdsReference.get().await()
                val userOutlookEmailIds = snapshotOutlookEmailIds.children.mapNotNull { data ->
                    data.getValue(CalendarEmailData::class.java)
                }

                val newUserGoogleEmailIds = ArrayList(userGoogleEmailIds.filter { it.email_id != data.email_id })

                val newUserOutlookEmailIds = ArrayList(userOutlookEmailIds.filter { it.email_id != data.email_id })

                val allEmailIdList = arrayListOf<CalendarEmailData>()
                if (emailIdType == "google") {
                    userGoogleEmailIdsReference.setValue(newUserGoogleEmailIds).await()
                    allEmailIdList.addAll(newUserGoogleEmailIds)
                }
                if (emailIdType == "outlook") {
                    userOutlookEmailIdsReference.setValue(newUserOutlookEmailIds).await()
                    allEmailIdList.addAll(newUserOutlookEmailIds)
                }
                Utils.printDebugLog("final_list: $allEmailIdList")
                FirebaseResponse.Success(allEmailIdList)
            } catch (exception: Exception) {
                FirebaseResponse.Failure(exception)
            }
        }
    }

    suspend fun removeEmailId2(userId: String, data: CalendarEmailData, emailIdType: String): FirebaseResponse<ArrayList<CalendarEmailData>> {
        return withContext(Dispatchers.IO) {
            try {
                val userCalendarReference = databaseReference
                    .child("users")
                    .child(userId)
                    .child("calendar_events")
                    .child(if (emailIdType == "google") "google_calendar" else "outlook_calendar")

                val snapshot = userCalendarReference.get().await()
                val userEmailIds = snapshot.children.mapNotNull { data ->
                    data.getValue(CalendarEmailData::class.java)
                }

                val updatedUserEmailIds = userEmailIds.filter { it.email_id != data.email_id }

                // Clear the list first, then add the updated items
                userCalendarReference.removeValue().await()
                for (emailId in updatedUserEmailIds) {
                    userCalendarReference.push().setValue(emailId).await()
                }

                FirebaseResponse.Success(ArrayList(updatedUserEmailIds))
            } catch (exception: Exception) {
                FirebaseResponse.Failure(exception)
            }
        }
    }



}