package com.app.hihlo.model.get_profile

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Data(
    val asset_type: String?=null,
    val asset_url: String?=null,
    var isPostLiked: Int?=null,
    var commentsCount: Int?=null,
    var likesCount: Int?=null,
    var isLiked: Int?=null,
    val caption: String?=null,
    val created_at: String?=null,
    val creator_name: String?=null,
    val creator_profile_image: String?=null,
    val creator_username: String?=null,
    val id: Int?=null,
    val status: String?=null,
    val updated_at: String?=null,
    val userCity: String?=null,
    val userCountry: String?=null,
    val user_id: Int?=null,
    val creator_id: Int?=null,
    val is_story_uploaded:  Int? = null,
    var is_cover:  String? = null
): Parcelable