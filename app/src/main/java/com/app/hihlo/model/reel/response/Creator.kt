package com.app.hihlo.model.reel.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Creator(
    val name: String,
    val profileImage: String,
    val user_live_status: String,
    val isStoryUploaded: Int,
    val username: String,
    val city: String,
    val country: String
): Parcelable