package com.app.hihlo.model.home.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val asset_url: String?=null,
    var isLiked: Int?=null,
    var is_follow: Int?=null,
    var likesCount: Int?=null,
    var commentsCount: Int?=null,
    val caption: String?=null,
    val created_at: String?=null,
    val id: Int?=null,
    val user_id: Int?=null,
    val post_height_size: Int?=null,
    val creatorDetail:CreatorDetail?=null,
    var is_cover: String?=null,
): Parcelable