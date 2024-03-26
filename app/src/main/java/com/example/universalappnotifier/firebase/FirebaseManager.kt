package com.example.universalappnotifier.firebase

import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.utils.Utils
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

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

    fun addUserEmailIdForCalendarEvents(
        userId: String,
        emailId: String,
        color: Int
    ): FirebaseResponse<List<CalendarEmailData>> {
        val future = CompletableFuture<FirebaseResponse<List<CalendarEmailData>>>()

        try {
            val userReference = Firebase.database.reference
                .child("users")
                .child(userId)
                .child("calendar_events")
                .child("google_calendar")

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentEmailIds = snapshot.value as? List<CalendarEmailData>?

                    val updatedEmailIds = currentEmailIds?.toMutableList() ?: mutableListOf()

                    updatedEmailIds.add(CalendarEmailData(emailId, color))

                    userReference.setValue(updatedEmailIds)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                future.complete(FirebaseResponse.Success(updatedEmailIds.toList()))
                            } else {
                                future.complete(FirebaseResponse.Failure(Exception("Failed to update email IDs")))
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    future.complete(FirebaseResponse.Failure(Exception("Database operation cancelled: ${error.message}")))
                }
            })
        } catch (e: Exception) {
            future.complete(FirebaseResponse.Failure(e))
        }

        return future.join()
    }

    suspend fun addUserEmailIdForCalendarEvents2(
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
                updatedEmailIds.add(CalendarEmailData(emailId, color))

                userReference.setValue(updatedEmailIds).await()

                // Return success response
                FirebaseResponse.Success(updatedEmailIds.toList())

            } catch (e: Exception) {
                // Return failure response
                FirebaseResponse.Failure(e)
            }
        }
    }



}