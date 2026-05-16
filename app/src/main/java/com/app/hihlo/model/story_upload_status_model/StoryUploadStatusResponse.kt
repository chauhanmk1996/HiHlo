package com.app.hihlo.model.story_upload_status_model

import com.google.gson.annotations.SerializedName

data class StoryUploadStatusResponse(

    @SerializedName("error")
    val error: Boolean,

    @SerializedName("code")
    val code: Int,

    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("payload")
    val payload: StoryUploadStatusPayload?
)