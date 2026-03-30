package com.app.hihlo.utils

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log

object UserDataManager {

    fun setPause(context: Context, isPaused: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        with(prefs.edit()) {
            putBoolean("com.HHA_IsPaused", isPaused)
            apply()
        }
    }

    fun isPaused(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val value = prefs.getBoolean("com.HHA_IsPaused", false)
        return value
    }

    private const val KEY_POS = "com.HHA_Position"

    fun setPosition(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val success = prefs.edit().putInt(KEY_POS, position).commit() // synchronous
        Log.e("REEL_POS", "SharedPref Saved = $position , success = $success")
    }

    fun getPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val pos = prefs.getInt(KEY_POS, -1)
        Log.e("REEL_POS", "SharedPref Read = $pos")
        return pos
    }
    ///// POST COMMENT
    fun postCommentSP(context: Context, position: Int, pid: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        with(prefs.edit()) {
            putInt("com.HHA_P_POSITION", position)
            apply()
        }
        with(prefs.edit()) {
            putString("com.HHA_P_PID", pid)
            apply()
        }
    }

    fun postCommentIsShow(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        with(prefs.edit()) {
            putBoolean("com.HHA_P_C_SHOW", show)
            apply()
        }
    }

    fun get_postCommentPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_P_POSITION", 0)
    }

    fun get_postCommentPid(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getString("com.HHA_P_PID", "") ?: ""
    }

    fun get_postCommentShow(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_P_C_SHOW", false)
    }

}