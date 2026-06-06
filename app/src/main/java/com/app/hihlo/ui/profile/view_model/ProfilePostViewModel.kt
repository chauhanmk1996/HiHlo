package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.get_profile.GetProfileResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class ProfilePostViewModel : ViewModel() {
    private val profileLiveData = SingleLiveEvent<Resources<GetProfileResponse>>()

    fun getProfileLiveData(): LiveData<Resources<GetProfileResponse>> {
        return profileLiveData
    }
    fun hitProfileDataApi(token: String, page:String, limit:String) {

        try {
            profileLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    profileLiveData.postValue(
                        Resources.success(
                            ApiRepository().getProfileApi(token, page, limit
                            )
                        )
                    )
                } catch (ex: Exception) {
                    profileLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val otherUserProfileLiveData = SingleLiveEvent<Resources<GetProfileResponse>>()

    fun getOtherUserProfileLiveData(): LiveData<Resources<GetProfileResponse>> {
        return otherUserProfileLiveData
    }
    fun hitOtherUserProfileDataApi(token: String, userId:String, page:String, limit:String) {

        try {
            otherUserProfileLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    otherUserProfileLiveData.postValue(
                        Resources.success(
                            ApiRepository().getOtherUserProfileApi(token, userId, page, limit
                            )
                        )
                    )
                } catch (ex: Exception) {
                    otherUserProfileLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}