package com.example.universalappnotifier.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.universalappnotifier.exceptionhandling.CustomException
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.OutlookCalendarEmailData
import com.example.universalappnotifier.models.UserData
import com.example.universalappnotifier.repo.AppRepository
import com.example.universalappnotifier.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel(private val appRepository: AppRepository) : ViewModel() {

    private val _isGoogleCalendarEventsFetchedMLiveData = MutableLiveData(false)
    val isGoogleCalendarEventsFetchedMLiveData: LiveData<Boolean>
        get() = _isGoogleCalendarEventsFetchedMLiveData

    private val _isOutlookCalendarEventsFetchedMLiveData = MutableLiveData(false)
    val isOutlookCalendarEventsFetchedMLiveData: LiveData<Boolean>
        get() = _isOutlookCalendarEventsFetchedMLiveData


    suspend fun getUserData(): FirebaseResponse<UserData?> {
        val currentlySignedInUser = appRepository.getCurrentLoggedInUser()
        return if (currentlySignedInUser is FirebaseResponse.Success) {
            if (currentlySignedInUser.data != null) {
                Utils.printDebugLog("Found_Currently_Signed_in_User :: uid: ${currentlySignedInUser.data.uid}")
                val result = appRepository.getUserData(currentlySignedInUser.data.uid)
                when (result) {
                    is FirebaseResponse.Success -> {
                        Utils.printDebugLog("Fetched_User_Data :: ${result.data}")
                        FirebaseResponse.Success(result.data)
                    }

                    is FirebaseResponse.Failure -> {
                        Utils.printErrorLog("Fetching_User_Data :: Failure: ${result.exception}")
                        FirebaseResponse.Failure(result.exception)
                    }

                    else -> {
                        Utils.printErrorLog("Fetching_User_Data :: Failure:")
                        FirebaseResponse.Failure(
                            CustomException(
                                "100000",
                                "Something went wrong!"
                            )
                        )
                    }
                }
            } else {
                FirebaseResponse.Failure(
                    CustomException(
                        "100000",
                        "Something went wrong!"
                    )
                )
            }
        } else if (currentlySignedInUser is FirebaseResponse.Failure) {
            FirebaseResponse.Failure(
                currentlySignedInUser.exception
            )
        } else {
            FirebaseResponse.Failure(
                CustomException(
                    "100000",
                    "Something went wrong!"
                )
            )
        }
    }

    fun updateOutLookCalendarEventsFetchedBoolean(boolean: Boolean) {
        _isOutlookCalendarEventsFetchedMLiveData.postValue(boolean)
    }

}