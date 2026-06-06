package com.app.hihlo.model.staticModel

import com.app.hihlo.R

data class ProfileDetailModel(val image: Int, var title: String)

val profileDetailList = listOf(
    ProfileDetailModel(R.drawable.profile_gender_icon, "Gender"),
    ProfileDetailModel(R.drawable.profile_location_icon, "City"),
    ProfileDetailModel(R.drawable.profile_earth_icon, "Country"),
    ProfileDetailModel(R.drawable.profile_marriage_icon, "Topic")
)


data class EditProfileColumnModel(val key: String, val title: String, var editTextValue:String?=null)

val editProfileDataMap = mapOf(
    "name" to "Name",
    "username" to "User Name",
    "phone" to "Phone number",
    "dob" to "DOB",
    "email" to "Email",
    "city" to "City",
    "country" to "Country",
    "interestId" to "Topic",
    "about" to "About"
)

val editProfileColumnsList = editProfileDataMap.map { (key, title) ->
    EditProfileColumnModel(key = key, title = title)
}

val audioCallCoinsList = listOf(
    "10 Coins",
    "15 Coins",
    "20 Coins",
    "30 Coins",
    "40 Coins"
)

val videoCallCoinsList = listOf(
    "15 Coins",
    "20 Coins",
    "30 Coins",
    "40 Coins",
    "50 Coins",
)
