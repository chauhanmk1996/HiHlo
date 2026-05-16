package com.app.hihlo.model.add_story_model

data class UserStory(
    val id: Int,
    val user_id: Int,
    val asset_url: String,
    val asset_type: String,
    val caption: String?,
    val status: String,
    val created_at: String
)
