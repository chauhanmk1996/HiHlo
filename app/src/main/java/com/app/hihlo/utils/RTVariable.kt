package com.app.hihlo.utils

import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story

object RTVariable {
    var MY_USER_ID: String = ""
    var USER_ID: String = ""
    var USER_IS_FOLLOWING: Boolean = false
    var COMMENT_POSITION: Int = 0
    var COMMENT_DELETED: Boolean = false
    var POST_ID: String = ""
    var COMMENT_FROM: Boolean = false
    var COMMENT_COUNT: Int = 0
    var POST_POSITION: Int = 0
    var REELS_CURRENT_PAGE: Int = 0
    var REPLY_POSITION = -1
    var REELS_POSITION: Int = 0
    var REELS_ID: String = ""
    var STORY_POSITION: Int = 0
    var REPLY_COMBINED_IMAGE_USERNAME: String = ""
    var REPLY_COMBINED_IMAGE_DELEMETER: String = "####@@@@####"

    var INNER_COMMENT_POSITION: Int = 0

    var ISHOMECLICKED: Boolean = false
    var ISOTHERCLICKED: Boolean = false

    // Post Comment Remember
    var FRAG_POSITION: Int = 0
    var P_PID: String = ""
    var P_H_SHOW: Boolean = false

    var SCROLL_POS: Int = 0
    var OFF_SET: Int = 0
    var isStable: Boolean = false
    var BOTTOM_REELS_ICON_CLICKED: Boolean = false
    var bottom_page: Int =0

    //var SEARCH_CLICKED: Boolean = false

    fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> {
                val value = count / 1_000_000.0
                if (value % 1 == 0.0) "${value.toInt()}M" else String.format("%.1fM", value)
            }
            count >= 1_000 -> {
                val value = count / 1_000.0
                if (value % 1 == 0.0) "${value.toInt()}K" else String.format("%.1fK", value)
            }
            else -> count.toString()
        }
    }

}