package com.app.hihlo.utils

import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.reel.response.Reel
import com.app.hihlo.model.search_user_list.response.SearchUserListResponse

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

    var REELS_PLAYING_POSITION: Long = 0

    var COMMENT_SCROLL_Y: Int =0

    //var SEARCH_CLICKED: Boolean = false
    var IS_FROM_RESUME: Boolean = false
    var IS_CHAT_CLICKED: Boolean = false
    var IS_CHAT_OTHER: Boolean = false
    var IS_CHAT_SELF_OTHER: Boolean = false

    var CHAT_INSTANCE_KEY_ID: Int =0
    var REELS_INSTANCE_KEY_ID: Int =0

    var IS_REELS_LOADED: Boolean = false

    var reelsCache: MutableList<Reel> = mutableListOf()

    var postsCache: MutableList<Post> = mutableListOf()

    var users_List: MutableList<SearchUserListResponse.Payload.User> = mutableListOf()

    var IS_SEARCH_MAIN_LOADED: Boolean = false

    var SEARCH_SELF_CLICKED: Int =0

    var IS_USER_SEARCH_STARTED: Boolean = false
    var SEARCH_TEXT: String = ""
    var SEARCH_MAIN_CURRENT_PAGE: Int = 0

    var HOME_INSTANCE_KEY_ID: Int =0

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