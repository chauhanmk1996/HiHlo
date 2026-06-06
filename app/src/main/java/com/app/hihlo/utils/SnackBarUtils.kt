package com.app.hihlo.utils

import android.content.Context
import com.app.hihlo.R
import com.google.gson.Gson
import retrofit2.HttpException

object SnackBarUtils {

    fun displayError(context: Context, exception: HttpException) {
        try {
            GsonUtil.mGsonInstance = Gson()
            val errorBody = GsonUtil.mGsonInstance!!.fromJson(
                exception.response()?.errorBody()?.charStream(), ErrorBean::class.java
            )
            val message = errorBody.message ?: errorBody.msg ?: errorBody.error ?: ""
            showShortToast(message, context)
        } catch (e: Exception) {
            logD(e.message ?: "")
            somethingWentWrong(context)
        }
    }

    private fun somethingWentWrong(context: Context) {
        showToast(context.getString(R.string.something_went_wrong_please_retry))
    }
}