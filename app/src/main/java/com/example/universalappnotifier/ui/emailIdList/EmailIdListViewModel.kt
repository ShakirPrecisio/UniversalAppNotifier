package com.example.universalappnotifier.ui.emailIdList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.universalappnotifier.firebase.FirebaseResponse
import com.example.universalappnotifier.models.CalendarEmailData
import com.example.universalappnotifier.models.OutlookCalendarEmailData
import com.example.universalappnotifier.repo.AppRepository
import com.example.universalappnotifier.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmailIdListViewModel(private val appRepository: AppRepository) : ViewModel() {

    private val _userEmailIdListMLiveData =
        MutableLiveData<FirebaseResponse<ArrayList<CalendarEmailData>>>()
    val userEmailIdListLiveData: LiveData<FirebaseResponse<ArrayList<CalendarEmailData>>>
        get() = _userEmailIdListMLiveData

    fun getUserAddedEmailIds(giveGoogleEmailIds: Boolean, giveOutlookEmailIds: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentlySignedInUser = appRepository.getCurrentLoggedInUser()
            if (currentlySignedInUser is FirebaseResponse.Success && currentlySignedInUser.data != null) {
                val result = appRepository.getUserAddedEmailIds(currentlySignedInUser.data.uid, giveGoogleEmailIds, giveOutlookEmailIds)
                Utils.printDebugLog("result: $result")
                if (result is FirebaseResponse.Success) {
                    _userEmailIdListMLiveData.postValue(FirebaseResponse.Success(result.data))
                } else if (result is FirebaseResponse.Failure) {
                    _userEmailIdListMLiveData.postValue(
                        FirebaseResponse.Failure(
                            result.exception
                        )
                    )
                }
            } else if (currentlySignedInUser is FirebaseResponse.Failure) {
                _userEmailIdListMLiveData.postValue(
                    FirebaseResponse.Failure(
                        currentlySignedInUser.exception
                    )
                )
            }
        }
    }

    fun removeEmailId(data: CalendarEmailData, emailIdType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentlySignedInUser = appRepository.getCurrentLoggedInUser()
            if (currentlySignedInUser is FirebaseResponse.Success && currentlySignedInUser.data != null) {
                val result = appRepository.removeEmailId(currentlySignedInUser.data.uid, data, emailIdType)
                Utils.printDebugLog("result: $result")
                if (result is FirebaseResponse.Success) {
                    _userEmailIdListMLiveData.postValue(FirebaseResponse.Success(result.data))
                } else if (result is FirebaseResponse.Failure) {
                    _userEmailIdListMLiveData.postValue(
                        FirebaseResponse.Failure(
                            result.exception
                        )
                    )
                }
            } else if (currentlySignedInUser is FirebaseResponse.Failure) {
                _userEmailIdListMLiveData.postValue(
                    FirebaseResponse.Failure(
                        currentlySignedInUser.exception
                    )
                )
            }
        }
    }

}