package com.app.hihlo.ui.signup.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch




class VerifyEmailOtpViewModel: ViewModel() {

    private val verifyLiveData = SingleLiveEvent<Resources<LoginResponse>>()

    fun getLoginLiveData(): LiveData<Resources<LoginResponse>> {
        return verifyLiveData
    }

    fun hitVerifyEmailOtp(email:String,otp:String) {

        try {
            verifyLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    verifyLiveData.postValue(
                        Resources.success(
                            ApiRepository().verifyEmailOtp(email,otp)
                        )
                    )

                } catch (ex: Exception) {
                    verifyLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}