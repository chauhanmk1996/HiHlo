package com.app.hihlo.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun logD(message: String) {
    val logMessage = "${getCurrentTimeStamp()} [Kuber]: $message\n"
    Log.d("HiHlo", logMessage)
}

private fun getCurrentTimeStamp(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date())
}