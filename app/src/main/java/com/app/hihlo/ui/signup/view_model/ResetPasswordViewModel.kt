package com.app.hihlo.ui.signup.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.signup.model.ChangePasswordRequest
import com.app.hihlo.ui.signup.model.ResetPasswordRequest
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch




class ResetPasswordViewModel : ViewModel() {

    private val resetPasswordLiveDate = SingleLiveEvent<Resources<LoginResponse>>()

    fun getResetPasswordLiveData(): LiveData<Resources<LoginResponse>> {
        return resetPasswordLiveDate
    }

    fun hitResetPassword(model: ResetPasswordRequest) {

        try {
            resetPasswordLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    resetPasswordLiveDate.postValue(
                        Resources.success(
                            ApiRepository().resetPassword(model)
                        )
                    )


                } catch (ex: Exception) {
                    resetPasswordLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val changePasswordLiveDate = SingleLiveEvent<Resources<LoginResponse>>()

    fun getChangePasswordLiveDataLiveData(): LiveData<Resources<LoginResponse>> {
        return changePasswordLiveDate
    }

    fun hitChangePassword(token:String,model: ChangePasswordRequest) {

        try {
            changePasswordLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    changePasswordLiveDate.postValue(
                        Resources.success(
                            ApiRepository().changePassword(token,model)
                        )
                    )


                } catch (ex: Exception) {
                    changePasswordLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

