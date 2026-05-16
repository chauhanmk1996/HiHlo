package com.app.hihlo.model.add_story_model

data class AddStoryResponse(
    val error: Boolean,
    val code: Int,
    val status: Int,
    val message: String,
    val payload: StoryPayload?
)
