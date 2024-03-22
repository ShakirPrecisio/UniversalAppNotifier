package com.example.universalappnotifier.firebase

import com.example.universalappnotifier.models.UserData
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

    fun addUserIntoDatabase(name: String, email: String): FirebaseResponse<Boolean> {
        return try {
            databaseReference.child("users")
                .child(auth.currentUser?.uid.toString())
                .setValue(
                    UserData(
                        user_name = name,
                        user_email = email
                    )
                )
            FirebaseResponse.Success(true)
        } catch (e: Exception) {
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
        emailId: String
    ): FirebaseResponse<Boolean> {
        val future = CompletableFuture<FirebaseResponse<Boolean>>()

        try {
            val userReference = Firebase.database.reference.child("users").child(userId).child("calendar_events")

            // Fetch the current list of email IDs
            userReference.child("google_calendar_email_ids").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentEmailIds = snapshot.value as? List<*>

                    if (currentEmailIds != null) {
                        // Create a mutable copy of the current list or initialize an empty list
                        val mutableEmailIds = currentEmailIds.toMutableList()

                        // Add the new emailId to the list
                        mutableEmailIds.add(emailId)

                        // Update the database with the updated list
                        userReference.child("google_calendar_email_ids").setValue(mutableEmailIds)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    future.complete(FirebaseResponse.Success(true))
                                } else {
                                    future.complete(FirebaseResponse.Failure(task.exception))
                                }
                            }
                    } else {
                        // If the current list is null, initialize a new list with the new emailId
                        userReference.child("google_calendar_email_ids").setValue(listOf(emailId))
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    future.complete(FirebaseResponse.Success(true))
                                } else {
                                    future.complete(FirebaseResponse.Failure(task.exception))
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    future.complete(FirebaseResponse.Failure(error.toException()))
                }
            })
        } catch (e: Exception) {
            future.complete(FirebaseResponse.Failure(e))
        }

        return future.join()
    }


}