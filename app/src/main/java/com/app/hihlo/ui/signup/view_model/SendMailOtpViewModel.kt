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




class SendMailOtpViewModel : ViewModel() {

    private val loginLiveDate = SingleLiveEvent<Resources<LoginResponse>>()

    fun getLoginLiveData(): LiveData<Resources<LoginResponse>> {
        return loginLiveDate
    }

    fun hitSendEmailOtp(email:String,userName:String?,purpose: String?) {

        try {
            loginLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    loginLiveDate.postValue(
                        Resources.success(
                            ApiRepository().sendEmailOtp(email,userName,purpose)
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
