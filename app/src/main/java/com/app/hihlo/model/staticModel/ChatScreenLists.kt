package com.app.hihlo.model.staticModel

import com.app.hihlo.R

data class ChatMoreOptionsModel(val image: Int, var title: String)

val chatMoreOptionsList = listOf(
    ProfileDetailModel(R.drawable.emoji_icon, "Emojis"),
    ProfileDetailModel(R.drawable.pin_icon_black, "Attach files"),
    ProfileDetailModel(R.drawable.camera_icon_black, "Camera"),
    ProfileDetailModel(R.drawable.mike_icon_black, "Mike")
)

val MIN_COINS_TO_INITIATE_CHAT = 10
val MIN_COINS_FOR_AUDIO = 10
val MIN_COINS_FOR_VIDEO = 15
