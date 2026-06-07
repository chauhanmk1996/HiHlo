package com.app.hihlo.network_call

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.signUpToHome.SignupFlowActivity
import com.app.hihlo.utils.AgoraUtils.isAppOnForeground
import com.app.hihlo.HiHloApplication
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 404||response.code == 402 || response.code==400|| response.code==403) {
            val responseBody = response.peekBody(Long.MAX_VALUE)
            val rawJson = responseBody.string()

            val message = try {
                val jsonObject = JSONObject(rawJson)
                jsonObject.optString("message", "Something went wrong")
            } catch (e: Exception) {
                "Something went wrong"
            }

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(HiHloApplication.appContext, message, Toast.LENGTH_LONG).show()
            }
        }else if (response.code == 401/*||response.code == 500*/){
            if (isAppOnForeground(HiHloApplication.appContext!!)){
                Preferences.removeAllPreferencesExcept(HiHloApplication.appContext!!, listOf(FCM_TOKEN))
                val intent = Intent(HiHloApplication.appContext, SignupFlowActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                HiHloApplication.appContext?.startActivity(intent)
            }
        }
        else if (response.code==502||response.code==429){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(HiHloApplication.appContext, "There is some technical glitch. Internet connection may be interrupted. Please try again later!", Toast.LENGTH_LONG).show()
            }
        }


        if (response.code == 500) {
            Log.e("TokenInterceptor", "Server error 500 occurred")
        }

        return response
    }
}

