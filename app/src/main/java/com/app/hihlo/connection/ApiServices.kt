package com.app.hihlo.connection

import com.app.hihlo.constant.ApiConstant
import com.app.hihlo.model.city_list.response.CityListResponse
import com.app.hihlo.model.gender_list.GenderListResponse
import com.app.hihlo.model.interest_list.response.InterestListResponse
import com.app.hihlo.ui.signUpToHome.ChangePasswordRequest
import com.app.hihlo.ui.signUpToHome.CheckUserNameRequest
import com.app.hihlo.ui.signUpToHome.CheckUserNameResponse
import com.app.hihlo.ui.signUpToHome.LoginRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.ui.signUpToHome.ResetPasswordRequest
import com.app.hihlo.ui.signUpToHome.SendEmailOtpRequest
import com.app.hihlo.ui.signUpToHome.SignUpRequest
import com.app.hihlo.ui.signUpToHome.SocialLoginRequest
import com.app.hihlo.ui.signUpToHome.SocialSignUpRequest
import com.app.hihlo.ui.signUpToHome.VerifyEmailOtpRequest
import com.app.hihlo.utils.logD
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

interface ApiServices {

    companion object {
        fun create(
            baseUrl: String,
            connectTimeoutInSec: Long = 30,
            readTimeoutInSec: Long = 30,
            writeTimeoutInSec: Long = 60,
        ): ApiServices {
            val client = OkHttpClient.Builder()
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                logD("okhttp:: $message")
            }
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            client.addInterceptor(loggingInterceptor)

            val headersInterceptor = Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            client.addInterceptor(headersInterceptor)

            client.connectTimeout(connectTimeoutInSec, TimeUnit.SECONDS)
            client.readTimeout(readTimeoutInSec, TimeUnit.SECONDS)
            client.writeTimeout(writeTimeoutInSec, TimeUnit.SECONDS)
            val gson = GsonBuilder().setLenient().create()
            val retrofit =
                Retrofit.Builder().addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addConverterFactory(ScalarsConverterFactory.create()).client(client.build())
                    .baseUrl(baseUrl).build()
            return retrofit.create(ApiServices::class.java)
        }
    }

    @POST(ApiConstant.CHECK_USER_NAME)
    suspend fun checkUsernameApi(
        @Body checkUserNameRequest: CheckUserNameRequest,
    ): CheckUserNameResponse

    @POST(ApiConstant.SOCIAL_SIGN_UP)
    suspend fun socialSignUpApi(
        @Body socialSignUpRequest: SocialSignUpRequest,
    ): LoginResponse

    @POST(ApiConstant.SEND_MAILE)
    suspend fun sendMailOtpApi(
        @Body sendEmailOtpRequest: SendEmailOtpRequest,
    ): LoginResponse

    @POST(ApiConstant.VERIFY_EMAIL_OTP)
    suspend fun verifyEmailOtpApi(
        @Body verifyEmailOtpRequest: VerifyEmailOtpRequest,
    ): LoginResponse

    @GET(ApiConstant.GET_INTEREST_LIST)
    suspend fun getInterestListApi(
    ): InterestListResponse

    @GET(ApiConstant.GET_GENDER_LIST)
    suspend fun getGenderListApi(
        @Query("type") login: String? = null,
    ): GenderListResponse

    @GET(ApiConstant.GET_CITY_LIST)
    suspend fun getCityListApi(
        @Query("search") search: String? = null,
        @Query("limit") limit: String? = "20",
        @Query("page") page: Int = 1,
    ): CityListResponse

    @POST(ApiConstant.REGISTER_USER)
    suspend fun registerUserApi(
        @Body signUpRequest: SignUpRequest,
    ): LoginResponse

    @POST(ApiConstant.LOGIN)
    suspend fun loginApi(
        @Body loginRequest: LoginRequest,
    ): LoginResponse

    @POST(ApiConstant.SOCIAL_AUTH)
    suspend fun socialLoginApi(
        @Body socialLoginRequest: SocialLoginRequest,
    ): LoginResponse

    @POST(ApiConstant.RESET_PASSWORD)
    suspend fun resetPasswordApi(
        @Body resetPasswordRequest: ResetPasswordRequest,
    ): LoginResponse

    @POST(ApiConstant.CHANGE_PASSWORD)
    suspend fun changePasswordApi(
        @Header("Authorization") token: String,
        @Body changePasswordRequest: ChangePasswordRequest,
    ): LoginResponse

}