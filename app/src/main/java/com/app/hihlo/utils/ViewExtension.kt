package com.app.hihlo.utils

import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView

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