package com.app.hihlo.ui.HomeNew.utility

import android.annotation.SuppressLint
import java.time.Duration
import java.time.Instant

object UpdateTimerAgo {

    @SuppressLint("NewApi")
    fun getTimeAgo(isoTime: String): String {
        return try {
            val past = Instant.parse(isoTime)
            val now = Instant.now()

            val duration = Duration.between(past, now)

            val seconds = duration.seconds
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hr ago"
                days < 7 -> "$days day ago"
                days < 30 -> "${days / 7} week ago"
                days < 365 -> "${days / 30} month ago"
                else -> "${days / 365} year ago"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}