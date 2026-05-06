package com.app.hihlo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.app.hihlo.databinding.PopupYesNoBinding
import com.google.common.io.Files.getFileExtension
import java.io.File
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.*
import android.view.ViewGroup
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

fun blurView(context: Context, view: View, radius: Float = 20f): Bitmap? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        // Use alternative blur for Android 12+
        createBlurredBitmap(context, view, radius)
    } else {
        // Use RenderScript for older versions
        blurWithRenderScript(context, view, radius)
    }
}

private fun createBlurredBitmap(context: Context, view: View, radius: Float): Bitmap? {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)

    // Apply blur effect using alternative methods
    return bitmap
}

@Suppress("DEPRECATION")
private fun blurWithRenderScript(context: Context, view: View, radius: Float): Bitmap? {
    try {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)

        rs.destroy()
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
object CommonUtils {

    fun String.toIconCompat(context: Context): IconCompat? {
        return try {
            val bitmap = Glide.with(context)
                .asBitmap()
                .load(this)
                .submit()
                .get()
            IconCompat.createWithBitmap(bitmap)
        } catch (e: Exception) {
            null
        }
    }

    fun formatTime(
        time: String,
        inputFormat: String,
        outputFormat: String
    ): String {
        return try {
            val inputSdf = SimpleDateFormat(inputFormat, Locale.getDefault())
            inputSdf.timeZone = TimeZone.getTimeZone("UTC") // Required if time is in UTC

            val outputSdf = SimpleDateFormat(outputFormat, Locale.getDefault())
            outputSdf.timeZone = TimeZone.getDefault() // Convert to local time

            val date = inputSdf.parse(time)
            if (date != null) outputSdf.format(date) else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    fun getTimeAgo(isoDate: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            val pastTime = sdf.parse(isoDate)?.time ?: return "Unknown"
            val now = System.currentTimeMillis()

            // ❗ Instagram ignores future → clamp to now
            val diff = maxOf(0, now - pastTime)

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val weeks = days / 7

            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                weeks < 5 -> "${weeks}w"
                else -> {
                    val months = days / 30
                    if (months < 12) "${months}mo" else "${days / 365}y"
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    fun <A : Activity> Context.launchActivityWithBundle(activity: Class<A>, bundle: Bundle) {
        val intent = Intent(this, activity).apply {
            putExtras(bundle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_FROM_BACKGROUND)
            addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    /*fun <A : Activity> Context.launchActivityWithBundle(activity: Class<A>, bundle: Bundle) {
        val intent = Intent(this, activity).apply {
            putExtras(bundle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }*/

    /*fun <A : Activity> Context.launchActivityWithBundle(activity: Class<A>, bundle: Bundle) {
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).also {
            it.putExtra("bundle", bundle)
            it.setClassName("com.app.hihlo", activity.name)
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            it.flags = Intent.FLAG_FROM_BACKGROUND
            it.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(it)
        }
    }*/
    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    fun isValidPassword(password: String): Boolean {
        return Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\$%^&*(),.?\":{}|<>]).+$")
            .matches(password)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun touchHideKeyBoard(view: View, activity: Activity){
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = activity.currentFocus
                if (focusedView is EditText) {
                    val outRect = Rect()
                    focusedView.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        focusedView.clearFocus()
                        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                    }
                }
            }
            false
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    fun setupUIToHideKeyboard(view: View, activity: Activity) {
        // Set touch listener for non-EditText views
        if (view !is EditText) {
            view.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val focusedView = activity.currentFocus
                    if (focusedView is EditText) {
                        val outRect = Rect()
                        focusedView.getGlobalVisibleRect(outRect)
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            focusedView.clearFocus()
                            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                        }
                    }
                }
                false
            }
        }

        // If a layout container, iterate children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupUIToHideKeyboard(view.getChildAt(i), activity)
            }
        }
    }



    fun showCustomDialogWithBinding(
        context: Context,
        title: String,
        description: String? = null,
        onYes: (() -> Unit)? = null,
        onNo: (() -> Unit)? = null,
        showButtons: Boolean = true,
        autoDismissInMillis: Long? = null,
        btnYes: String? = "Yes",
        btnNo: String? = "No"
    ) {
        val binding = PopupYesNoBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.dialogTitle.text = title

        // Set description if provided
        if (!description.isNullOrEmpty()) {
            binding.dialogDescription.visibility = View.VISIBLE
            binding.dialogDescription.text = description
        } else {
            binding.dialogDescription.visibility = View.GONE
        }

        binding.btnYes.text = btnYes
        binding.btnNo.text = btnNo

        if (showButtons) {
            binding.btnYes.setOnClickListener {
                dialog.dismiss()
                onYes?.invoke()
            }
            binding.btnNo.setOnClickListener {
                dialog.dismiss()
                onNo?.invoke()
            }
        } else {
            binding.btnYes.visibility = View.GONE
            binding.btnNo.visibility = View.GONE
            binding.horizontalView.visibility = View.GONE
            (binding.btnYes.parent as? View)?.visibility = View.GONE
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        autoDismissInMillis?.let { delay ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (dialog.isShowing) dialog.dismiss()
            }, delay)
        }
    }




    object DotPasswordTransformationMethod : PasswordTransformationMethod() {
        override fun getTransformation(source: CharSequence, view: View): CharSequence {
            return PasswordCharSequence(super.getTransformation(source, view))
        }

        private class PasswordCharSequence(
            val transformation: CharSequence
        ) : CharSequence by transformation {
            override fun get(index: Int): Char = if (transformation[index] == DOT) {
                STAR
            } else {
                transformation[index]
            }
        }

        private const val DOT = '\u2022'
        private const val STAR = '*'
    }
    fun hideKeyboard(activity: Activity) {
        Log.i("TAG", "hideKeyboard: ")
        try {
            val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager.isActive) {
                inputMethodManager.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
            } else
                return
        } catch (e: Exception) {

        }
    }
    fun hideEdittextKeyboard(view: View) {
        try {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun Float.toPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
    fun openKeyboard(editText: EditText) {
        editText.requestFocus()
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
    fun showKeyboard(context: Context, view: View) {
        view.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    fun clearBackStack(navController: NavController, fragmentInt: Int){
        val options = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, true)
            .build()
        navController.navigate(fragmentInt, null, options)
    }


}