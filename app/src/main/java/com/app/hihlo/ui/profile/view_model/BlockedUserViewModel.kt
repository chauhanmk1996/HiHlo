package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.blocked_userlist.BlockedUsersResponse
import com.app.hihlo.model.following_list.response.FollowingListResponse
import com.app.hihlo.model.post_comments.response.PostCommentsResponse
import com.app.hihlo.model.unblock_user.request.UnblockUserRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class BlockedUserViewModel : ViewModel() {
    private val blockedUsersLiveData = SingleLiveEvent<Resources<BlockedUsersResponse>>()

    fun getBlockedUsersLiveData(): LiveData<Resources<BlockedUsersResponse>> {
        return blockedUsersLiveData
    }
    fun hitBlockedUsersDataApi(token: String) {

        try {
            blockedUsersLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    blockedUsersLiveData.postValue(
                        Resources.success(
                            ApiRepository().getBlockedUsersApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    blockedUsersLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val unblockUserLiveData = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getUnblockUserLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return unblockUserLiveData
    }
    fun hitUnblockUserApi(token:String, request: UnblockUserRequest) {

        try {
            unblockUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    unblockUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().unblockUserApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    unblockUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}