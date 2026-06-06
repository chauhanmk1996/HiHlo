package com.app.hihlo.ui.reels.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.coin_details.CoinDetailsResponse
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.generate_agora_token.response.AgoraTokenResponse
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.get_reel_comments.response.ReelCommentsResponse
import com.app.hihlo.model.post_comments.request.PostCommentsRequest
import com.app.hihlo.model.post_comments.response.PostCommentsResponse
import com.app.hihlo.model.reel.response.ReelsResponse
import com.app.hihlo.model.reply_to_comment.request.ReplyToCommentRequest
import com.app.hihlo.model.reply_to_comment.response.ReplyToCommentResponse
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.send_gift.SendGiftResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch
import com.app.hihlo.model.reel.response.Reel

class ReelsViewModel:ViewModel() {

    var isreelsDataLoaded: Boolean = false

    var reelsCache: MutableList<Reel> = mutableListOf()
    var current_page: Int = 1
    var scroll_position: Int = 0
    var commentPayloadCache: Payload? = null
    private val deletePostLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeleteReelLiveData(): LiveData<Resources<CommonResponse>> {
        return deletePostLiveData
    }
    fun hitDeleteReelDataApi(token: String, reelId: String) {

        try {
            deletePostLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deletePostLiveData.postValue(
                        Resources.success(
                            ApiRepository().deleteReelApi(token, reelId
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


    private val getCoinDetailsLiveData = SingleLiveEvent<Resources<CoinDetailsResponse>>()

    fun getCoinDetailsLiveData(): LiveData<Resources<CoinDetailsResponse>> {
        return getCoinDetailsLiveData
    }
    fun hitCoinDetailsApi(token: String) {

        try {
            getCoinDetailsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getCoinDetailsLiveData.postValue(
                        Resources.success(
                            ApiRepository().coinDetailsApi(token)
                        )
                    )
                } catch (ex: Exception) {
                    getCoinDetailsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val getSendGiftLiveData = SingleLiveEvent<Resources<SendGiftResponse>>()

    fun getSendGiftLiveData(): LiveData<Resources<SendGiftResponse>> {
        return getSendGiftLiveData
    }
    fun hitSendGiftApi(token: String, request: SendGiftRequest) {

        try {
            getSendGiftLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getSendGiftLiveData.postValue(
                        Resources.success(
                            ApiRepository().sendGiftApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    getSendGiftLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val getReelsLiveDate = SingleLiveEvent<Resources<ReelsResponse>>()

    fun getReelsLiveData(): LiveData<Resources<ReelsResponse>> {
        return getReelsLiveDate
    }
    fun hitGetReelsApi(token:String, page:String, limit:String) {

        try {
            getReelsLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getReelsLiveDate.postValue(
                        Resources.success(
                            ApiRepository().getReelsApi(token, page, limit
                            )
                        )
                    )
                } catch (ex: Exception) {
                    getReelsLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



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
                            ApiRepository().getReelCommentsApi(token, reelId, page, limit)
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
                            ApiRepository().postReelCommentApi(token, request, reelId)
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
                            ApiRepository().replyToCommentApi(token, request, reelId)
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
                            ApiRepository().likeReelApi(token, reelId)
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

    private val generateAgoraTokenLiveDate = SingleLiveEvent<Resources<AgoraTokenResponse>>()

    fun getGenerateAgoraTokenLiveData(): LiveData<Resources<AgoraTokenResponse>> {
        return generateAgoraTokenLiveDate
    }
    fun hitGenerateAgoraTokenDataApi(token: String, channelName: String, calleeId: String, uid:String, callType: String, sender_id: String ) {

        try {
            generateAgoraTokenLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    generateAgoraTokenLiveDate.postValue(
                        Resources.success(
                            ApiRepository().generateAgoraToken(token, channelName, calleeId, uid, callType, sender_id
                            )
                        )
                    )
                } catch (ex: Exception) {
                    generateAgoraTokenLiveDate.postValue(Resources.error(ex.localizedMessage, null))

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
    fun hitUnfollowUserDataApi(token: String, request:FollowRequest) {

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