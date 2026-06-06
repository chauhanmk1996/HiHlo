package com.app.hihlo.ui.home.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class StoryViewModel:ViewModel() {
    private val seenStoryLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun seenStoryLiveData(): LiveData<Resources<CommonResponse>> {
        return seenStoryLiveData
    }
    fun hitSeenStoryDataApi(token: String, request: StorySeen) {

        try {
            seenStoryLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    seenStoryLiveData.postValue(
                        Resources.success(
                            ApiRepository().storySeenApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    seenStoryLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    private val storyDeleteLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getStoryDeleteLiveData(): LiveData<Resources<CommonResponse>> {
        return storyDeleteLiveData
    }
    fun hitStoryDeleteDataApi(token: String, request: StoryDeleteRequest) {

        try {
            storyDeleteLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    storyDeleteLiveData.postValue(
                        Resources.success(
                            ApiRepository().storyDeleteApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    storyDeleteLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}