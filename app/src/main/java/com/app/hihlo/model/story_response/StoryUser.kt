package com.app.hihlo.model.story_response

data class StoryUser(
    val user_id: Int,
    val isStoriesUploaded: Boolean,
    val userDetail: UserDetail,
    var stories: ArrayList<Story>
)
