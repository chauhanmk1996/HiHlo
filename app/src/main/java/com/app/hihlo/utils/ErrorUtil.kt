package com.app.hihlo.utils

import android.content.Context
import com.app.hihlo.R
import com.app.hihlo.utils.SnackBarUtils.displayError
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorUtil {

    fun handlerGeneralError(context: Context?, throwable: Throwable) {
        logD(throwable.message ?: "")
        throwable.printStackTrace()
        if (context == null) return
        when (throwable) {
            is ConnectException -> {
                showToast(context.getString(R.string.network_error_please_again_try_later))
            }

            is SocketTimeoutException ->
                showToast(context.getString(R.string.connection_lost_please_again_try_later))

            is UnknownHostException, is InternalError ->
                showToast(context.getString(R.string.server_error_please_try_again_later))

            is HttpException -> {
                try {
                    when (throwable.code()) {
                        401 -> {
                            displayError(context, throwable)
                            SharedPreferenceUtil.getInstance(context).logoutUser()
                            //TODO Logout
                            //val intent = Intent(context, LoginActivity::class.java)
                            //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            //context.startActivity(intent)
                        }

                        else -> {
                            displayError(context, throwable)
                        }
                    }
                } catch (exception: Exception) {
                    logD(exception.message ?: "")
                }
            }

            else -> {
                showToast(context.getString(R.string.something_went_wrong_please_retry))
            }
        }
    }
}