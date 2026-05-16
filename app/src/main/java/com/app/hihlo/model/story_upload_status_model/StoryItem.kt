package com.app.hihlo.model.story_upload_status_model

import com.google.gson.annotations.SerializedName

data class StoryItem(

    @SerializedName("id")
    val id: Int,

    @SerializedName("asset_url")
    val assetUrl: String,

    @SerializedName("asset_type")
    val assetType: String,

    @SerializedName("caption")
    val caption: String?,

    @SerializedName("status")
    val status: String,

    @SerializedName("created_at")
    val createdAt: String
)