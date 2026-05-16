package com.app.hihlo.model.story_response

data class StoryResponse(
    val error: Boolean,
    val code: Int,
    val status: Int,
    val message: String,
    val payload: List<StoryUser>
)
