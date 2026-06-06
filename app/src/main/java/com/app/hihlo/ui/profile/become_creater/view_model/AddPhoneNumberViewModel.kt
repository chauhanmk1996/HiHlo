package com.app.hihlo.ui.profile.become_creater.view_model
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.profile.become_creater.PhoneOtpFragment
import com.app.hihlo.ui.profile.become_creater.model.CreatorsBenefitsResponse
import com.app.hihlo.ui.profile.become_creater.model.SendOtpPhoneRequest
import com.app.hihlo.ui.profile.become_creater.model.UserToCreatorRequest
import com.app.hihlo.ui.profile.become_creater.model.VerifyPhoneOtpRequest
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch




class AddPhoneNumberViewModel :ViewModel() {
    private val addPhoneNumberLiveData = SingleLiveEvent<Resources<CreatorsBenefitsResponse>>()

    fun getAddPhoneNumberLiveData(): LiveData<Resources<CreatorsBenefitsResponse>> {
        return addPhoneNumberLiveData
    }
    fun hitAddPhoneNumber(token: String,model: SendOtpPhoneRequest) {

        try {
            addPhoneNumberLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    addPhoneNumberLiveData.postValue(
                        Resources.success(
                            ApiRepository().sendOtpPhone(token,model)
                        )
                    )
                } catch (ex: Exception) {
                    addPhoneNumberLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val verifyPhoneOtpLiveData = SingleLiveEvent<Resources<CreatorsBenefitsResponse>>()

    fun getVerifyPhoneOtpLiveData(): LiveData<Resources<CreatorsBenefitsResponse>> {
        return verifyPhoneOtpLiveData
    }
    fun hitVerifyPhoneOtp(token: String,model: VerifyPhoneOtpRequest) {

        try {
            verifyPhoneOtpLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    verifyPhoneOtpLiveData.postValue(
                        Resources.success(
                            ApiRepository().verifyPhoneOtp(token,model)
                        )
                    )
                } catch (ex: Exception) {
                    verifyPhoneOtpLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

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