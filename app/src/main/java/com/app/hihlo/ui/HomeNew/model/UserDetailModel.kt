package com.app.hihlo.ui.HomeNew.model

data class UserDetailModel(
    val name: String,
    val username: String,
    val profile_image: String,
    val city: String,
    val country: String,
    val gender_id: Int? = null,
    val gender_name: String? = null,
    val role: String,
    val is_creator: Int,
    val is_verified: Int? = 0,
    val user_live_status: String
)
