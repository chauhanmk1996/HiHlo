package com.app.hihlo.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.ONLINE_STATUS
import com.app.hihlo.preferences.Preferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyApplication : Application(), LifecycleObserver {

    companion object {
        var appContext: Context? = null
        var isStackMode = false
    }

    private val apiRepository = ApiRepository()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContext = this
//        registerPhoneAccount(this)
        // Observe app background/foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        // App went to background
        appScope.launch {
            try {
                val token ="Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(appContext, LOGIN_DATA)?.payload?.authToken.toString()
//                apiRepository.updateLiveStatusApi(token, liveStatusId = 2) // e.g., 0 = "offline"
            } catch (e: Exception) {
                Log.e("API", "App stop API failed: ${e.message}")
            }
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Log.i("TAG", "onCreate myapp: "+Preferences.getStringPreference(appContext, ONLINE_STATUS))
        if (Preferences.getStringPreference(appContext, ONLINE_STATUS)=="online"){
            appScope.launch {
                try {
                    val token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(appContext, LOGIN_DATA)?.payload?.authToken.toString()
//                    apiRepository.updateLiveStatusApi(token, liveStatusId = 1) // e.g., 1 = "live"
                } catch (e: Exception) {
                    Log.e("API", "App start API failed: ${e.message}")
                }
            }
        }
    }


}
