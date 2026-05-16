package com.app.hihlo.model.story_upload_status_model

import com.google.gson.annotations.SerializedName

data class StoryUploadStatusPayload(

    @SerializedName("uploadedStoryCount")
    val uploadedStoryCount: Int,

    @SerializedName("maxStoryLimit")
    val maxStoryLimit: Int,

    @SerializedName("remainingStories")
    val remainingStories: Int,

    @SerializedName("stories")
    val stories: List<StoryItem> = emptyList()
)
