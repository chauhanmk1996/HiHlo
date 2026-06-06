package com.app.hihlo.ui.signup.view_model

import android.app.Application
import android.content.Context
import android.util.Patterns
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.R
import com.app.hihlo.model.login.request.LoginRequest
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.signup.model.SocialSignUpRequest
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class SigninViewModel : ViewModel() {
    val email = ObservableField<String>()
    val password = ObservableField<String>()
    val isTermsChecked = MutableLiveData(true)


    private val _validationMessage = MutableLiveData<String>()
    val validationMessage: LiveData<String> = _validationMessage

    private val loginLiveDate = SingleLiveEvent<Resources<LoginResponse>>()

    fun getLoginLiveData(): LiveData<Resources<LoginResponse>> {
        return loginLiveDate
    }
    fun hitLoginDataApi(request: LoginRequest) {

        try {
            loginLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    loginLiveDate.postValue(
                        Resources.success(
                            ApiRepository().loginApi(request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    loginLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun loginApi(context: Context) {
        val emailInput = email.get()?.trim()
        val passwordInput = password.get()?.trim()

        if (emailInput.isNullOrEmpty()) {
            _validationMessage.value = "Please Enter Email"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            _validationMessage.value = "Please Enter Valid Email"
            return
        }

        if (passwordInput.isNullOrEmpty()) {
            _validationMessage.value = "Please Enter Password"
            return
        }
        if (isTermsChecked.value != true) {
            _validationMessage.value = "Please agree to the terms"
            return
        }

        hitLoginDataApi(LoginRequest(email = emailInput, password = passwordInput, deviceToken = Preferences.getStringPreference(context, FCM_TOKEN), deviceType = "A", ))
    }

    private val socialLoginLiveData = SingleLiveEvent<Resources<LoginResponse>>()

    fun getSocialLiveData(): LiveData<Resources<LoginResponse>> {
        return socialLoginLiveData
    }
    fun hitSocialApi(request: SocialSignUpRequest) {

        try {
            socialLoginLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    socialLoginLiveData.postValue(
                        Resources.success(
                            ApiRepository().socialLogin(request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    loginLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}