package com.app.hihlo.model.interest_list.response

data class InterestListResponse(
    val code: Int,
    val error: Boolean,
    val message: String,
    val payload: Payload?,
    val status: Int
)