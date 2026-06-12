
package com.app.hihlo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.app.hihlo.Global
import com.app.hihlo.R
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

fun AppCompatTextView.getString(): String {
    return this.text.trim().toString()
}

fun AppCompatTextView.getLength(): Int {
    return this.text.trim().toString().length
}

fun AppCompatEditText.getString(): String {
    return this.text?.trim()?.toString() ?: ""
}

fun AppCompatEditText.getLength(): Int {
    return this.text?.trim()?.toString()?.length ?: 0
}

fun showToast(message: String?) {
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

fun showLongToast(message: String) {
    val parent = Global.baseActivity.findViewById<ViewGroup>(android.R.id.content)
    val layout: View =
        Global.baseActivity.layoutInflater.inflate(R.layout.long_toast, parent, false)

    val text = layout.findViewById<TextView>(R.id.tv_toast)
    text.text = message

    Toast(Global.baseActivity).apply {
        duration = Toast.LENGTH_LONG
        view = layout
        show()
    }
}

fun showShortToast(message: String, context: Context) {
    val inflater = Global.baseActivity.layoutInflater
    val layout: View = inflater.inflate(R.layout.long_toast, null)
    val text = layout.findViewById<TextView>(R.id.tv_toast)
    text.text = message
    val toast = Toast(context)
    toast.duration = Toast.LENGTH_SHORT
    toast.view = layout
    toast.show()
}

fun getDeviceToken(sharedPreference: SharedPreferenceUtil) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener {
        try {
            if (it.isComplete) {
                it.result?.let { token ->
                    sharedPreference.deviceToken = token
                    logD("Device Token = $token")
                }
            }
        } catch (e: Exception) {
            logD("Device Token Error = ${e.message ?: ""}")
        }
    }
}

fun Activity.startScreen(activity: Activity) {
    startActivity(Intent(this, activity::class.java))
}

fun Fragment.startScreen(activity: Activity) {
    startActivity(Intent(requireContext(), activity::class.java))
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun String.toUpperString(): String {
    return this.uppercase(Locale.getDefault())
}