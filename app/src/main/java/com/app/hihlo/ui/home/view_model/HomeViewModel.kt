package com.app.hihlo.ui.home.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.add_story_model.AddStoryResponse
import com.app.hihlo.model.gender_list.GenderListResponse
import kotlinx.coroutines.launch

import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import com.app.hihlo.model.home.response.HomeResponse
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.home.response.SetRemoveCoverRequest
import com.app.hihlo.model.home.response.SetRemoveCoverResponse
import com.app.hihlo.network_call.repository.ApiRepository

class HomeViewModel:ViewModel() {

    var isHomeDataLoaded = false

    var postsCache: MutableList<Post> = mutableListOf()

    var myStory: MyStory? = null
    var stories: List<Story> = emptyList()
    var isStoryUploaded: Int = 0
    var profileImage: String = ""
    var isRefreshing = false

    var currentPage = 1
    var scroll_position: Int = 0
    var posr_id: String = ""

    var scrollY = 0
    var isPaused = false
    var filterById: Int = 0
    var filterByName: String = ""
    private val homeLiveDate = SingleLiveEvent<Resources<HomeResponse>>()
    private val genderListLiveData = SingleLiveEvent<Resources<GenderListResponse>>()
    private val addStoryLiveDate = SingleLiveEvent<Resources<AddStoryResponse>>()
    private val setRemoveCoverResponse = SingleLiveEvent<Resources<SetRemoveCoverResponse>>()
    fun getGenderLiveData(): LiveData<Resources<GenderListResponse>> {
        return genderListLiveData
    }

    fun hitGenderListApi() {
        try {
            genderListLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    genderListLiveData.postValue(
                        Resources.success(
                            ApiRepository().getGenderListApi()
                        )
                    )
                } catch (ex: Exception) {
                    genderListLiveData.postValue(Resources.error(ex.localizedMessage?:"", null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getHomeLiveData(): LiveData<Resources<HomeResponse>> {
        return homeLiveDate
    }

    fun hitHomeDataApi(token: String, page: String, limit: String, genderId: String) {

        try {
            homeLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    homeLiveDate.postValue(
                        Resources.success(
                            ApiRepository().getHomeDataApi(token, page, limit, genderId
                            )
                        )
                    )
                } catch (ex: Exception) {
                    homeLiveDate.postValue(Resources.error(ex.localizedMessage?:"", null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun addStoryLiveData(): LiveData<Resources<AddStoryResponse>> {
        return addStoryLiveDate
    }

    fun hitAddStoryDataApi(token: String, request:AddStoryRequest) {

        try {
            addStoryLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    addStoryLiveDate.postValue(
                        Resources.success(
                            ApiRepository().addStoryApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    addStoryLiveDate.postValue(Resources.error(ex.localizedMessage?:"", null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun setRemoveCoverApi(): LiveData<Resources<SetRemoveCoverResponse>> {
        return setRemoveCoverResponse
    }

    fun setRemoveCoverApi(token: String, postId: String, isCover: String) {
        try {
            setRemoveCoverResponse.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    setRemoveCoverResponse.postValue(Resources.success(ApiRepository().setRemoveCoverApi(token, postId, isCover)))
                } catch (ex: Exception) {
                    setRemoveCoverResponse.postValue(Resources.error(ex.localizedMessage?:"", null))
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}