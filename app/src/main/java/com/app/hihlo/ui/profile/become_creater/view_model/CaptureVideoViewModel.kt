package com.app.hihlo.ui.profile.become_creater.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.profile.become_creater.model.CreatorsBenefitsResponse
import com.app.hihlo.ui.profile.become_creater.model.UserToCreatorRequest
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class CaptureVideoViewModel  : ViewModel(){

    private val userToCreatorLiveData = SingleLiveEvent<Resources<CreatorsBenefitsResponse>>()

    fun getUserToCreatorLiveData(): LiveData<Resources<CreatorsBenefitsResponse>> {
        return userToCreatorLiveData
    }
    fun hitUserToCreator(token: String,model: UserToCreatorRequest) {

        try {
            userToCreatorLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    userToCreatorLiveData.postValue(
                        Resources.success(
                            ApiRepository().userToCreator(token,model)
                        )
                    )
                } catch (ex: Exception) {
                    userToCreatorLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}