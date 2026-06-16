package com.app.hihlo.model.home.response

data class HomeResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: Payload,
    val status: Int,
)

data class SetRemoveCoverRequest(
    val post_id: String,
    val is_cover: String,
)

data class SetRemoveCoverResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: SetCoverPayload,
    val status: Int,
)

data class SetCoverPayload(
    val is_story_uploaded: Int,
)

data class GetCreatorListResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: ArrayList<Creator>?,
    val status: Int,
)

data class Creator(
    val creator_id: Int?,
    val post_id: Int?,
    val image_type: String?,
    val display_image: String?,
    val is_cover: Boolean?,
    val is_follow: Int?,
    val creatorDetail: CreatorDetail?,
)