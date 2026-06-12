package com.app.hihlo.model.home.response

data class HomeResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: Payload,
    val status: Int
)

data class SetRemoveCoverRequest(
    val post_id:String,
    val is_cover: String,
)

data class SetRemoveCoverResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: SetCoverPayload,
    val status: Int
)

data class SetCoverPayload(
    val is_story_uploaded:Int,
)