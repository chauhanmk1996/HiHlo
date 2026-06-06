package com.app.hihlo.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.app.hihlo.Global
import com.app.hihlo.R

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