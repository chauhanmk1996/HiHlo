package com.app.hihlo.model.get_reel_comments.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val city: String,
    val country: String,
    val isCreator: String,
    val isStoryUploaded: Int,
    val id: Int,
    val name: String,
    val profile_image: String,
    val username: String
):Parcelable