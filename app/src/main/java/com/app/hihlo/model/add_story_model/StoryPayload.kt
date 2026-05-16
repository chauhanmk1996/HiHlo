package com.app.hihlo.model.add_story_model

data class StoryPayload(
    val id: Int? = null,
    val userId: Int? = null,
    val assetUrl: String? = null,
    val assetType: String? = null,
    val caption: String? = null,

    val uploadedStoryCount: Int? = 0,
    val maxStoryLimit: Int? = 0,
    val remainingStories: Int? = 0,

    val stories: List<UserStory>? = emptyList()
)
