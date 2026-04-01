package com.app.hihlo.ui.home.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.get_reel_comments.response.ReelCommentsResponse
import com.app.hihlo.model.post_comments.request.PostCommentsRequest
import com.app.hihlo.model.post_comments.response.PostCommentsResponse
import com.app.hihlo.model.reply_to_comment.request.ReplyToCommentRequest
import com.app.hihlo.model.reply_to_comment.response.ReplyToCommentResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPostListViewModel @Inject constructor() : ViewModel(){

    var commentPayloadCache: Payload? = null
    var scroll_position: Int = 0
    private val getReelCommentsLiveDate = SingleLiveEvent<Resources<ReelCommentsResponse>>()

    fun getReelCommentsLiveData(): LiveData<Resources<ReelCommentsResponse>> {
        return getReelCommentsLiveDate
    }
    fun hitGetReelCommentsApi(token:String, reelId:String,
                              page: String? = null,
                              limit: String? = null) {

        try {
            getReelCommentsLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getReelCommentsLiveDate.postValue(
                        Resources.success(
                            ApiRepository().getPostCommentsApi(token, reelId, page, limit)
                        )
                    )
                } catch (ex: Exception) {
                    getReelCommentsLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val postCommentLiveDate = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getPostCommentLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return postCommentLiveDate
    }
    fun hitPostCommentApi(token:String, request:PostCommentsRequest, reelId:String) {

        try {
            postCommentLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    postCommentLiveDate.postValue(
                        Resources.success(
                            ApiRepository().addPostCommentApi(token, request, reelId)
                        )
                    )
                } catch (ex: Exception) {
                    postCommentLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val replyToCommentLiveDate = SingleLiveEvent<Resources<ReplyToCommentResponse>>()

    fun getReplyToCommentLiveData(): LiveData<Resources<ReplyToCommentResponse>> {
        return replyToCommentLiveDate
    }
    fun hitReplyToCommentsApi(token:String, request:ReplyToCommentRequest, reelId:String) {

        try {
            replyToCommentLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    replyToCommentLiveDate.postValue(
                        Resources.success(
                            ApiRepository().replyToPostCommentApi(token, request, reelId)
                        )
                    )
                } catch (ex: Exception) {
                    replyToCommentLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val deletePostLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeletePostLiveData(): LiveData<Resources<CommonResponse>> {
        return deletePostLiveData
    }
    fun hitDeletePostDataApi(token: String, postId: String) {

        try {
            deletePostLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deletePostLiveData.postValue(
                        Resources.success(
                            ApiRepository().deletePostApi(token, postId
                            )
                        )
                    )
                } catch (ex: Exception) {
                    deletePostLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val likeReelLiveData = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getLikeReelLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return likeReelLiveData
    }
    fun hitLikeReelApi(token:String, reelId:String) {

        try {
            likeReelLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    likeReelLiveData.postValue(
                        Resources.success(
                            ApiRepository().likePostApi(token, reelId)
                        )
                    )
                } catch (ex: Exception) {
                    likeReelLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}