package com.app.hihlo.model.story_response

data class Story(
    val id: Int,
    val asset_url: String,
    val asset_type: String,
    val caption: String,
    val created_at: String,
    val is_seen: Int,
    val seen_count: Int? = 0
)
