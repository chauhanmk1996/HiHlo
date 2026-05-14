package com.app.hihlo.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import com.app.hihlo.R
import com.google.android.material.snackbar.Snackbar

object Utils {

    @SuppressLint("RestrictedApi")
    fun showCustom_Snackbar(view: View, message: String) {
        val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE)
        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
        val customView = LayoutInflater.from(view.context)
            .inflate(R.layout.custom_snackbar, null)
        val txtMessage = customView.findViewById<TextView>(R.id.txtMessage)
        txtMessage.text = message
        snackbarLayout.removeAllViews()
        snackbarLayout.addView(customView)
        val params = snackbarLayout.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT
        params.setMargins(40, 0, 40, 40)
        snackbarLayout.layoutParams = params
        snackbar.duration = 2000
        snackbar.show()
    }

}