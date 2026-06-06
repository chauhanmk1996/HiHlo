package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.following_list.response.FollowingListResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class FollowersViewModel :ViewModel() {
    private val followersLiveData = SingleLiveEvent<Resources<FollowingListResponse>>()

    fun getFollowersLiveData(): LiveData<Resources<FollowingListResponse>> {
        return followersLiveData
    }
    fun hitFollowersDataApi(token: String, self: String, other: String, otherUserId: String) {

        try {
            followersLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    followersLiveData.postValue(
                        Resources.success(
                            ApiRepository().getFollowersListApi(token, self, other, otherUserId
                            )
                        )
                    )
                } catch (ex: Exception) {
                    followersLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val followingListLiveData = SingleLiveEvent<Resources<FollowingListResponse>>()

    fun getFollowingListLiveData(): LiveData<Resources<FollowingListResponse>> {
        return followingListLiveData
    }
    fun hitFollowingListDataApi(token: String, self: String, other: String, otherUserId: String) {

        try {
            followingListLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    followingListLiveData.postValue(
                        Resources.success(
                            ApiRepository().getFollowingListApi(token, self, other, otherUserId
                            )
                        )
                    )
                } catch (ex: Exception) {
                    followingListLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val followUserLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getFollowUserLiveData(): LiveData<Resources<CommonResponse>> {
        return followUserLiveData
    }
    fun hitFollowUserDataApi(token: String, request:FollowRequest) {

        try {
            followUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    followUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().getFollowUserApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    followUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val unfollowUserLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getUnfollowUserLiveData(): LiveData<Resources<CommonResponse>> {
        return unfollowUserLiveData
    }
    fun hitUnfollowUserDataApi(token: String, request:FollowRequest, ) {

        try {
            unfollowUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    unfollowUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().getUnfollowUserApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    unfollowUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}