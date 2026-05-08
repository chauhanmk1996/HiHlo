package com.app.hihlo.ui.HomeNew.model

data class StatusItem(
    val id: Int,
    val user_id: Int,
    val asset_url: String,
    val asset_type: String,
    val caption: String,
    val created_at: String,
    val is_seen: Int,
    val seen_count: Int? = 0,
    val isStoriesUploaded: Boolean,
    val userDetail: UserDetailModel
)
