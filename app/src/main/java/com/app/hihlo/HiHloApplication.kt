package com.app.hihlo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.hihlo.constant.IntentConstant
import com.app.hihlo.firebase.AppLifecycleTracker
import com.app.hihlo.utils.ProgressDialog

class HiHloApplication : Application(), LifecycleObserver {

    companion object {
        var appContext: Context? = null
        var isStackMode = false
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AppLifecycleTracker.init(this)
    }
}

@SuppressLint("StaticFieldLeak")
object Global {
    lateinit var baseActivity: Activity
    var selectedLanguage: String? = IntentConstant.ENGLISH
    private var progressDialog: ProgressDialog? = null

    fun setActivity(activity: Activity) {
        baseActivity = activity
    }

    fun setLanguage(language: String) {
        selectedLanguage = language
    }

    fun showProgress(visible: Boolean) {
        if (visible) {
            progressDialog?.dismiss()
            if (!baseActivity.isDestroyed) {
                progressDialog = ProgressDialog(baseActivity)
                progressDialog?.setCancelable(false)
                progressDialog?.show()
            }
        } else {
            progressDialog?.dismiss()
        }
    }
}