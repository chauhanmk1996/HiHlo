package com.app.hihlo.preferences

import android.net.Uri
import java.io.File

object UserPreference {
    var token = ""
    var selectedGender:Int?=null
    var selectedMediaType =""
    var seletedUri :Uri = Uri.EMPTY // while selecting post or reel to upload from gallery
    var selectedMediaToUpload :String = ""  //post or reel
    var selectedCropRatio :Int = 2  //post or reel
    var isChatFragmentOpen="0"
    var SERVER_KEY = "{inserserverkey}"
    var navigatedToMyProfile = false

    var AGORA_TOKEN = "agoraToken"
    var CHANNEL_NAME = "channelName"
    var CALLER_USER_IMAGE = "calluserimage"
    var USER_NAME = "USER_NAME"
    var CITY_COUNTRY = "CITY_COUNTRY"


    var CALL_USER_NAME = "callusernname"
    var OTHER_USER_ID = "otherUserId"
    var CALL_TYPE = "callType"
    var U_ID = "msgId"

    var CALL_ID = "CALL_ID"



    var CHAT_PUSH_NOTIFICATION_ID: String?=null
    var IS_CALLING_SCREEN_OPENED = false

    var uploadStatusFile: File? = null

}
