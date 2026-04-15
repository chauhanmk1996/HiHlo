package com.app.hihlo.utils

import android.content.Context

object ScrollPrefManager {

    private const val PREF_NAME = "scroll_pref"

    fun saveScroll(context: Context, key: String, position: Int, offset: Int) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit()
            .putInt("${key}_pos", position)
            .putInt("${key}_offset", offset)
            .apply()
    }

    fun getScroll(context: Context, key: String): ScrollState {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val position = pref.getInt("${key}_pos", -1)
        val offset = pref.getInt("${key}_offset", 0)

        return ScrollState(position, offset)
    }

    fun clear(context: Context, key: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().remove("${key}_pos").remove("${key}_offset").apply()
    }
}