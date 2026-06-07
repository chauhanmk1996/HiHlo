package com.app.hihlo.ui.signUpToHome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.hihlo.Global
import com.app.hihlo.R
import com.app.hihlo.base.BaseViewModel
import com.app.hihlo.utils.AppValidator
import com.app.hihlo.utils.LiveDataEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpToHomeViewModel : BaseViewModel() {

    companion object {
        const val TAG = "SignUpToHomeViewModel"
    }

    val mDeviceTokenLiveData = MutableLiveData<String>()
    val mEmailIdLiveData = MutableLiveData<String>()
    val mPasswordLiveData = MutableLiveData<String>()
    val mTermsCheckedLiveData = MutableLiveData<Boolean>()
    val mNameLiveData = MutableLiveData<String>()
    val mUserNameLiveData = MutableLiveData<String>()
    val mMobileNumberLiveData = MutableLiveData<String>()
    val mConfirmPasswordLiveData = MutableLiveData<String>()
    val mOtpLiveData = MutableLiveData<String>()
    val resendOtpText = MutableLiveData<String>()


    val checkUserNameResponse = MutableLiveData<LiveDataEvent<CheckUserNameResponse>>()
    val socialSignUpResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()
    val sendEmailOtpResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()
    val verifyEmailOtpResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()


    val loginResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()
    val socialLoginResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()
    val forgotPasswordResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()
    val resetPasswordResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()


    val signUpResponse = MutableLiveData<LiveDataEvent<LoginResponse>>()

    fun checkUsernameApi() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.checkUsernameApi(
                        checkUserNameRequest = CheckUserNameRequest(
                            username = mUserNameLiveData.value ?: ""
                        )
                    )
                }
                checkUserNameResponse.postValue(LiveDataEvent(response))
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun socialSignUpApi(socialSignUpRequest: SocialSignUpRequest) {
        showLoading(true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.socialSignUpApi(
                        socialSignUpRequest = socialSignUpRequest
                    )
                }
                showLoading(false)
                socialSignUpResponse.postValue(LiveDataEvent(response))
            } catch (e: Exception) {
                onError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    fun sendEmailApi(purpose: String) {
        showLoading(true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.sendMailOtpApi(
                        sendEmailOtpRequest = SendEmailOtpRequest(
                            email = mEmailIdLiveData.value ?: "",
                            username = mUserNameLiveData.value ?: "",
                            purpose = purpose
                        )
                    )
                }
                showLoading(false)
                sendEmailOtpResponse.postValue(LiveDataEvent(response))
            } catch (e: Exception) {
                onError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    fun verifyEmailOtpApi() {
        if (isValidOtp()) {
            showLoading(true)
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.verifyEmailOtpApi(
                            verifyEmailOtpRequest = VerifyEmailOtpRequest(
                                email = mEmailIdLiveData.value ?: "",
                                otp = mOtpLiveData.value ?: "",
                            )
                        )
                    }
                    showLoading(false)
                    verifyEmailOtpResponse.postValue(LiveDataEvent(response))
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun isValidOtp(): Boolean {
        if (mEmailIdLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_email_id))
            return false
        }

        if (!AppValidator.isValidEmail(mEmailIdLiveData.value!!)) {
            showToast(Global.baseActivity.getString(R.string.please_enter_valid_email_id))
            return false
        }

        if (mPasswordLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_password))
            return false
        }
        return true
    }


    fun loginApi() {
        if (isValidLogin()) {
            showLoading(true)
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.loginApi(
                            loginRequest = LoginRequest(
                                email = mEmailIdLiveData.value ?: "",
                                password = mPasswordLiveData.value ?: "",
                                deviceToken = mDeviceTokenLiveData.value ?: "",
                                deviceType = "A",
                            )
                        )
                    }
                    showLoading(false)
                    loginResponse.postValue(LiveDataEvent(response))
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun isValidLogin(): Boolean {
        if (mEmailIdLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_email_id))
            return false
        }

        if (!AppValidator.isValidEmail(mEmailIdLiveData.value!!)) {
            showToast(Global.baseActivity.getString(R.string.please_enter_valid_email_id))
            return false
        }

        if (mPasswordLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_password))
            return false
        }
        return true
    }

    fun socialLoginApi(socialLoginRequest: SocialLoginRequest) {
        showLoading(true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.socialLoginApi(
                        socialLoginRequest = socialLoginRequest
                    )
                }
                showLoading(false)
                loginResponse.postValue(LiveDataEvent(response))
            } catch (e: Exception) {
                onError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    fun forgotPasswordApi() {
        if (isValidForgotPassword()) {
            showLoading(true)
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.sendMailOtpApi(
                            sendEmailOtpRequest = SendEmailOtpRequest(
                                email = mEmailIdLiveData.value ?: "",
                                username = null,
                                purpose = "forgot_password",
                            )
                        )
                    }
                    showLoading(false)
                    forgotPasswordResponse.postValue(LiveDataEvent(response))
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun isValidForgotPassword(): Boolean {
        if (mEmailIdLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_email_id))
            return false
        }

        if (!AppValidator.isValidEmail(mEmailIdLiveData.value!!)) {
            showToast(Global.baseActivity.getString(R.string.please_enter_valid_email_id))
            return false
        }
        return true
    }

    fun resetPasswordApi() {
        if (isValidResetPassword()) {
            showLoading(true)
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.resetPasswordApi(
                            resetPasswordRequest = ResetPasswordRequest(
                                email = mEmailIdLiveData.value ?: "",
                                newPassword = mPasswordLiveData.value ?: "",
                                confirmPassword = mConfirmPasswordLiveData.value ?: ""
                            )
                        )
                    }
                    showLoading(false)
                    resetPasswordResponse.postValue(LiveDataEvent(response))
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun isValidResetPassword(): Boolean {
        if (mPasswordLiveData.value.isNullOrEmpty()) {
            showToast("Please enter new password")
            return false
        }

        if (!AppValidator.isValidPassword(mPasswordLiveData.value!!)) {
            showToast("Please enter valid valid password")
            return false
        }
        if (mConfirmPasswordLiveData.value.isNullOrEmpty()) {
            showToast("Please enter new confirm password")
            return false
        }

        if (mPasswordLiveData.value != mConfirmPasswordLiveData.value) {
            showToast("New Password and new confirm password not matched")
            return false
        }
        return true
    }




    fun registerUserApi() {
        if (isValidSignUp()) {
            showLoading(true)
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.registerUserApi(
                            signUpRequest = SignUpRequest(
                                email = mEmailIdLiveData.value ?: "",
                                password = mPasswordLiveData.value ?: "",
                                deviceToken = mDeviceTokenLiveData.value ?: "",
                                deviceType = "A",
                            )
                        )
                    }
                    showLoading(false)
                    loginResponse.postValue(LiveDataEvent(response))
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun isValidSignUp(): Boolean {
        if (mNameLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_your_name))
            return false
        }

        if (!AppValidator.isValidName(mNameLiveData.value!!)) {
            showToast("lease enter a valid user name")
            return false
        }

        if (mUserNameLiveData.value.isNullOrEmpty()) {
            showToast("Please enter your user name")
            return false
        }

        if (mEmailIdLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_email_id))
            return false
        }

        if (!AppValidator.isValidEmail(mEmailIdLiveData.value!!)) {
            showToast(Global.baseActivity.getString(R.string.please_enter_valid_email_id))
            return false
        }

        if (mMobileNumberLiveData.value.isNullOrEmpty()) {
            showToast("Please your phone number")
            return false
        }

        if (!AppValidator.isValidMobileNumber(mMobileNumberLiveData.value!!)) {
            showToast("Please your valid phone number")
            return false
        }

        if (mPasswordLiveData.value.isNullOrEmpty()) {
            showToast(Global.baseActivity.getString(R.string.please_enter_password))
            return false
        }

        if (!AppValidator.isValidPassword(mPasswordLiveData.value!!)) {
            showToast("Please your valid password")
            return false
        }
        if (mConfirmPasswordLiveData.value.isNullOrEmpty()) {
            showToast("Please enter confirm password")
            return false
        }

        if (mPasswordLiveData.value != mConfirmPasswordLiveData.value) {
            showToast("Password and confirm password not matched")
            return false
        }
        return true
    }

    fun signUpApi() {
        showLoading(true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.sendMailOtpApi(
                        sendEmailOtpRequest = SendEmailOtpRequest(
                            email = mEmailIdLiveData.value ?: "",
                            username = mUserNameLiveData.value ?: "",
                            purpose = "signup",
                        )
                    )
                }
                showLoading(false)
                signUpResponse.postValue(LiveDataEvent(response))
            } catch (e: Exception) {
                onError(e)
            } finally {
                showLoading(false)
            }
        }
    }
}