package com.app.hihlo.ui.HomeNew.model

import android.net.Uri

data class MediaModel(
    val uri: Uri,
    val actualPath: String,
    val mediaType: String,
    val fileSize: Long,
    val duration: Long
)
