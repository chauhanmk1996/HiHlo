package com.app.hihlo.base

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.app.hihlo.BuildConfig
import com.app.hihlo.utils.ErrorUtil
import com.app.hihlo.Global
import com.app.hihlo.R
import com.app.hihlo.connection.ApiServices

open class BaseViewModel : ViewModel() {

    val apiService by lazy {
        ApiServices.create(BuildConfig.BASE_URL_API)
    }

    fun onError(throwable: Throwable) {
        showLoading(false)
        ErrorUtil.handlerGeneralError(Global.baseActivity, throwable)
    }

    fun showLoading(visible: Boolean) {
        Global.showProgress(visible)
    }

    fun showToast(message: String) {
        val parent = Global.baseActivity.findViewById<ViewGroup>(android.R.id.content)
        val layout: View =
            Global.baseActivity.layoutInflater.inflate(R.layout.long_toast, parent, false)

        val text = layout.findViewById<TextView>(R.id.tv_toast)
        text.text = message

        Toast(Global.baseActivity).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }
}