package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.hihlo.base.BaseViewModel
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.signUpToHome.ChangePasswordRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.utils.AppValidator
import com.app.hihlo.utils.LiveDataEvent
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingViewModel : BaseViewModel() {
    private val logoutUserLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getLogoutUserLiveData(): LiveData<Resources<CommonResponse>> {
        return logoutUserLiveData
    }

    fun hitLogoutUserDataApi(token: String) {

        try {
            logoutUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    logoutUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().logoutUserApi(
                                token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    logoutUserLiveData.postValue(Resources.error(ex.localizedMessage ?: "", null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    val mOldPasswordLiveData = MutableLiveData<String>()
    val mNewPasswordLiveData = MutableLiveData<String>()
    val mConfirmPasswordLiveData = MutableLiveData<String>()

    val changePasswordResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()

    fun changePasswordApi(token: String) {
        if (isValidChangePassword()) {
            showLoading(true)
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.changePasswordApi(
                            token = "Bearer $token",
                            changePasswordRequest = ChangePasswordRequest(
                                oldPassword = mOldPasswordLiveData.value ?: "",
                                newPassword = mNewPasswordLiveData.value ?: "",
                                confirmedNewPassword = mConfirmPasswordLiveData.value ?: ""
                            )
                        )
                    }
                    showLoading(false)
                    changePasswordResponse.postValue(LiveDataEvent(response))
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun isValidChangePassword(): Boolean {
        if (mOldPasswordLiveData.value.isNullOrEmpty()) {
            showToast("Please enter old password")
            return false
        }

        if (mNewPasswordLiveData.value.isNullOrEmpty()) {
            showToast("Please enter new password")
            return false
        }

        if (!AppValidator.isValidPassword(mNewPasswordLiveData.value!!)) {
            showToast("Please enter valid new password")
            return false
        }

        if (mConfirmPasswordLiveData.value.isNullOrEmpty()) {
            showToast("Please enter new confirm password")
            return false
        }

        if (mNewPasswordLiveData.value != mConfirmPasswordLiveData.value) {
            showToast("New Password and new confirm password not matched")
            return false
        }
        return true
    }
}