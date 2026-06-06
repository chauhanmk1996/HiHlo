package com.app.hihlo.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.app.hihlo.constant.AppConstants

class SharedPreferenceUtil
private constructor(val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREFERENCE_NAME, Context.MODE_PRIVATE)

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var instance: SharedPreferenceUtil? = null

        fun getInstance(ctx: Context): SharedPreferenceUtil {
            if (instance == null) {
                instance = SharedPreferenceUtil(ctx)
            }
            return instance!!
        }
    }

    var deviceToken: String
        get() = sharedPreferences["deviceToken", "Android"] ?: "Android"
        set(value) = sharedPreferences.set("deviceToken", value)

    var isLanguageSelected: Boolean
        get() = sharedPreferences["isLanguageSelected", false]!!
        set(value) = sharedPreferences.set("isLanguageSelected", value)

    var selectedLanguage: String
        get() = sharedPreferences["selectedLanguage", "en"] ?: "en"
        set(value) = sharedPreferences.set("selectedLanguage", value)

    var isLogin: Boolean
        get() = sharedPreferences["isLogin", false]!!
        set(value) = sharedPreferences.set("isLogin", value)

    var firstName: String
        get() = sharedPreferences["firstName", ""] ?: ""
        set(value) = sharedPreferences.set("firstName", value)

    var lastName: String
        get() = sharedPreferences["lastName", ""] ?: ""
        set(value) = sharedPreferences.set("lastName", value)

    var countryCode: String
        get() = sharedPreferences["countryCode", ""] ?: ""
        set(value) = sharedPreferences.set("countryCode", value)

    var mobileNumber: String
        get() = sharedPreferences["mobileNumber", ""] ?: ""
        set(value) = sharedPreferences.set("mobileNumber", value)

    var emailId: String
        get() = sharedPreferences["emailId", ""] ?: ""
        set(value) = sharedPreferences.set("emailId", value)

    var accessToken: String
        get() = sharedPreferences["accessToken", ""] ?: ""
        set(value) = sharedPreferences.set("accessToken", value)

    var userId: String
        get() = sharedPreferences["userId", ""] ?: ""
        set(value) = sharedPreferences.set("userId", value)

    var profilePic: String
        get() = sharedPreferences["profilePic", ""] ?: ""
        set(value) = sharedPreferences.set("profilePic", value)

    fun logoutUser() {
        isLogin = false
        userId = ""
        accessToken = ""
        countryCode = ""
        mobileNumber = ""
        emailId = ""
        firstName = ""
        lastName = ""
        profilePic = ""
    }

    operator fun SharedPreferences.set(key: String, value: Any?) {
        when (value) {
            is String? -> edit { it.putString(key, value) }
            is Int -> edit { it.putInt(key, value) }
            is Boolean -> edit { it.putBoolean(key, value) }
            is Float -> edit { it.putFloat(key, value) }
            is Long -> edit { it.putLong(key, value) }
            else -> logD("Setting shared pref failed for key: $key and value: $value")
        }
    }

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = this.edit()
        operation(editor)
        editor.apply()
    }

    inline operator fun <reified T : Any> SharedPreferences.get(
        key: String,
        defaultValue: T? = null,
    ): T? {
        return when (T::class) {
            String::class -> getString(key, defaultValue as? String) as T?
            Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
            Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
            Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
            Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }
}