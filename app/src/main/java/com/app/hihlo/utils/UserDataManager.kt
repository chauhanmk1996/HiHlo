package com.app.hihlo.utils

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log

object UserDataManager {

    fun setPause(context: Context, isPaused: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_IsPaused", isPaused)
            apply()
        }
    }

    fun isPaused(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
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
    /// POST MAIN
    fun postMainSP(context: Context, page: Int, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_MainPAGE", page)
            putInt("com.HHA_P_MainPOSITION", position)
            commit()
        }
//        with(prefs.edit()) {
//            putInt("com.HHA_P_MainPOSITION", position)
//            commit()
//        }
    }

    fun postUpdateMainSP(context: Context, page: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_MainPAGE", page)
            commit()
        }
    }

    fun postMainIsSetShow(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_P_MainSHOW", show)
            commit()
        }
    }
    fun get_postMainPage(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_P_MainPAGE", 0)
    }
    fun get_postMainPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_P_MainPOSITION", 0)
    }
    fun get_postMainIsShow(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_P_MainSHOW", false)
    }
    ///// POST COMMENT
    fun postCommentSP(context: Context, page: Int, position: Int, pid: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_PAGE", page)
            putInt("com.HHA_P_POSITION", position)
            putString("com.HHA_P_PID", pid)
            commit()
        }
//        with(prefs.edit()) {
//            putInt("com.HHA_P_POSITION", position)
//            commit()
//        }
//        with(prefs.edit()) {
//            putString("com.HHA_P_PID", pid)
//            commit()
//        }
    }

    fun postCommentSPR(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_POSITION", position)
            commit()
        }
    }

    fun postCommentUpdateSP(context: Context, page: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_PAGE", page)
            commit()
        }
    }

    fun postCommentIsShow(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_P_C_SHOW", show)
            commit()
        }
    }

    fun get_postCommentPage(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_P_PAGE", 0)
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

    fun postCommentPosition(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_COMMENT_POSITION", position)
            commit()
        }
    }

    fun postCommentYPosition(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_P_COMMENT_YPOSITION", position)
            commit()
        }
    }

    fun postCommentExpandState(context: Context, state: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_P_COMMENT_CommentExpandState", state)
            commit()
        }
    }

    fun get_CommentPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_P_COMMENT_POSITION", 0)
    }

    fun get_CommentYPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_P_COMMENT_YPOSITION", 0)
    }

    fun get_CommentExpandState(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_P_COMMENT_CommentExpandState", false)
    }

    fun setCommentToScroll(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_P_C_CommentIsScroll", show)
            commit()
        }
    }

    fun isCommentToScroll(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_P_C_CommentIsScroll", false)
    }

    /////////Navigation
    fun setHomeLoaded(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_HomeLoaded", show)
            commit()
        }
    }
    fun isHomeLoaded(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_HomeLoaded", false)
    }
    fun setGetBackToHome(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_GetBackToHome", show)
            commit()
        }
    }
    fun isGetBackToHome(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_GetBackToHome", false)
    }
    fun setHomeScrollPosition(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_HomeScrollPosition", position)
            commit()
        }
    }
    fun getHomeScrollPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_HomeScrollPosition", 0)
    }
    fun setHomeScrollYPosition(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_HomeScrollYPosition", position)
            commit()
        }
    }
    fun getHomeScrollYPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_HomeScrollYPosition", 0)
    }

    fun setReelsPosition(context: Context, position: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putInt("com.HHA_ReelsScrollPosition", position)
            commit()
        }
    }

    fun getReelsPosition(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getInt("com.HHA_ReelsScrollPosition", 0)
    }

    fun isUserInnerPostIsResume(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("com.HHA_P_UserInnerPostIsResume", false)
    }

    fun setUserInnerPostIsResume(context: Context, show: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(prefs.edit()) {
            putBoolean("com.HHA_P_UserInnerPostIsResume", show)
            commit()
        }
    }

    fun saveChatScrollPosition(context: Context, type: String, position: Int) {
        val pref = context.getSharedPreferences("chat_pref", Context.MODE_PRIVATE)
        pref.edit().putInt("scroll_$type", position).apply()
    }

    fun getChatScrollPosition(context: Context, type: String): Int {
        val pref = context.getSharedPreferences("chat_pref", Context.MODE_PRIVATE)
        return pref.getInt("scroll_$type", 0)
    }

}