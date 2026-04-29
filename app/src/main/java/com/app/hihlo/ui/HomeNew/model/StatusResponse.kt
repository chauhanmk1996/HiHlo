package com.app.hihlo.ui.HomeNew.model

data class StatusResponse(
    val error: Boolean,
    val code: Int,
    val status: Int,
    val message: String,
    val payload: List<StatusItem>
)
