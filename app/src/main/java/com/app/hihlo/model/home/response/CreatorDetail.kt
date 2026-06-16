package com.app.hihlo.model.home.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreatorDetail(
    val interest_id: Int,
    val interest_name: String,
    val user_live_status: String,
    val profile_image: String,
    val name: String,
    val city: String,
    val country: String,
    val gender_id: Int,
    val gender_name: String,
    val interest_image: String,
    val is_creator: Int,
    val isStoryUploaded: Int?,
    val myStory: MyStory?,
    val username: String
): Parcelable