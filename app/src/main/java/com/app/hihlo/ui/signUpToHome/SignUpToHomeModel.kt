package com.app.hihlo.ui.signUpToHome

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
    val deviceToken: String? = null,
    val deviceType: String? = null,
)

data class LoginResponse(
    val code: Int? = null,
    val error: Boolean? = null,
    val message: String? = null,
    val status: Int? = null,
    val payload: LoginPayload? = null,
)

data class LoginPayload(
    var userId: Int? = null,
    val authToken: String? = null,
    var name: String? = null,
    val fullName: String? = null,
    var email: String? = null,
    var dob: String? = null,
    var profileImage: String? = null,
    var phone: String? = null,
    val isCreator: Int? = null,
    var audio_call_charges: Int? = null,
    var video_call_charges: Int? = null,
    var city: String? = null,
    var country: String? = null,
    val S3Details: S3Detail? = null,
    val RAZOR_PAY_DETAILS: RazorPayDetails? = null,
    val AGORA_DETAILS: AgoraDetails? = null,
    val AWS_CDN_URL: String? = null,
    var userName: String? = null,
    var username: String? = null,
)

data class RazorPayDetails(
    var RAZOR_PAYID: String? = null,
    var RAZOR_PAY_SECRET: String? = null,
)

data class AgoraDetails(
    var AGORA_APP_ID: String? = null,
    var AGORA_APP_CERTIFICATE: String? = null,
)

data class S3Detail(
    val ACCESS_KEY: String?,
    val BUCKET_NAME: String?,
    val REGION: String?,
    val SECRET_KEY: String?,
)

data class SocialLoginRequest(
    var name: String? = null,
    var email: String? = null,
    var social_id: String? = null,
    var social_type: String? = null,
    var profile_image: String? = null,
    var deviceToken: String? = null,
    var deviceType: String? = null,
)

data class SocialSignUpRequest(
    var name: String? = null,
    var email: String? = null,
    var social_id: String? = null,
    var social_type: String? = null,
    var profile_image: String? = null,
    var deviceToken: String? = null,
    var deviceType: String? = null,
)

data class CheckUserNameRequest(
    val username: String? = null,
)

data class CheckUserNameResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: CheckUserNamePayload,
    val status: Int,
)

data class CheckUserNamePayload(
    val usernameAvailable: Int,
)

data class SendEmailOtpRequest(
    var email: String? = null,
    var username: String? = null,
    var purpose: String? = null,
)

data class VerifyEmailOtpRequest(
    var email: String? = null,
    var otp: String? = null
)







@Keep
@Parcelize
data class SignUpRequest(
    var name: String? = null,
    var username: String? = null,
    var phoneNumber: String? = null,
    var email: String? = null,
    var socialId: String? = null,
    var socialType: String? = null,
    var password: String? = null,
    var deviceType: String? = null,
    var deviceToken: String? = null,
    var dob: String? = null,
    var city: String? = null,
    var gender_id: String? = null,
    var interest_id: String? = null,
    var confirmPassword: String? = null,
) : Parcelable

data class ResetPasswordRequest(
    var email: String? = null,
    var newPassword: String? = null,
    var confirmPassword: String? = null,
)

data class ChangePasswordRequest(
    var oldPassword: String? = null,
    var newPassword: String? = null,
    var confirmedNewPassword: String? = null,
)