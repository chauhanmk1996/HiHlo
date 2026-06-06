package com.app.hihlo.ui.signup.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.check_username.request.CheckUsernameRequest
import com.app.hihlo.model.check_username.response.CheckUsernameResponse
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.signup.model.SignUp
import com.app.hihlo.ui.signup.model.SocialSignUpRequest
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class SocialSignUpViewModel : ViewModel() {

    private val SocialSignUpLiveDate = SingleLiveEvent<Resources<LoginResponse>>()

    fun getSocialSignUpLiveData(): LiveData<Resources<LoginResponse>> {
        return SocialSignUpLiveDate
    }

    fun hitSocialSignUpUser(model: SocialSignUpRequest) {

        try {
            SocialSignUpLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    SocialSignUpLiveDate.postValue(
                        Resources.success(
                            ApiRepository().socialLogin(model)
                        )
                    )


                } catch (ex: Exception) {
                    SocialSignUpLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val checkUsernameLiveData = SingleLiveEvent<Resources<CheckUsernameResponse>>()

    fun getCheckUsernameLiveData(): LiveData<Resources<CheckUsernameResponse>> {
        return checkUsernameLiveData
    }
    fun hitCheckUsernameDataApi(username: String) {

        try {
            checkUsernameLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    checkUsernameLiveData.postValue(
                        Resources.success(
                            ApiRepository().checkUsernameApi(CheckUsernameRequest(username))
                        )
                    )
                } catch (ex: Exception) {
                    checkUsernameLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}