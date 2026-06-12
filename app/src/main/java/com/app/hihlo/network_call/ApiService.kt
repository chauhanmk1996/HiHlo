package com.app.hihlo.network_call

import com.app.hihlo.model.GetUserIdResponse.GetUserIdByUserNameResponse
import com.app.hihlo.model.coin_details.CoinDetailsResponse
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.add_coins.AddCoinsRequest
import com.app.hihlo.model.add_post.request.AddPostRequest
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.add_story_model.AddStoryResponse
import com.app.hihlo.model.ads_list.GetAdsListResponse
import com.app.hihlo.model.block_reasons.response.BlockReasonsResponse
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.blocked_userlist.BlockedUsersResponse
import com.app.hihlo.model.chat_history.ChatHistoryResponse
import com.app.hihlo.model.city_list.response.CityListResponse
import com.app.hihlo.model.contact_us.request.ContactUsRequest
import com.app.hihlo.model.deduct_call_coin.DeductCallCoinRequest
import com.app.hihlo.model.deduct_call_coin.DeductCallCoinResponse
import com.app.hihlo.model.deduct_chat_coin.DeductChatCoinRequest
import com.app.hihlo.model.delete_comment.DeleteResponse
import com.app.hihlo.model.edit_profile.request.EditProfileRequest
import com.app.hihlo.model.edit_profile.response.EditProfileResponse
import com.app.hihlo.model.end_call.request.EndCallRequest
import com.app.hihlo.model.faq.response.FaqResponse
import com.app.hihlo.model.flag_user.request.FlagUserRequest
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.following_list.response.FollowingListResponse
import com.app.hihlo.model.gender_list.GenderListResponse
import com.app.hihlo.model.generate_agora_token.response.AgoraTokenResponse
import com.app.hihlo.model.get_notification_setting.response.GetNotificationSettingResponse
import com.app.hihlo.model.get_profile.GetProfileResponse
import com.app.hihlo.model.get_recent_chat.response.GetRecentChatResponse
import com.app.hihlo.model.get_reel_comments.response.ReelCommentsResponse
import com.app.hihlo.model.home.response.HomeResponse
import com.app.hihlo.model.home.response.SetRemoveCoverRequest
import com.app.hihlo.model.home.response.SetRemoveCoverResponse
import com.app.hihlo.model.interest_list.response.InterestListResponse
import com.app.hihlo.model.notification.response.GetNotificationListResponse
import com.app.hihlo.model.post_comments.request.PostCommentsRequest
import com.app.hihlo.model.post_comments.response.PostCommentsResponse
import com.app.hihlo.model.predefined_chats.PredefinedChatsResponse
import com.app.hihlo.model.rating_review.RatingReviewRequest
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.model.reel.response.ReelsResponse
import com.app.hihlo.model.reply_to_comment.request.ReplyToCommentRequest
import com.app.hihlo.model.reply_to_comment.response.ReplyToCommentResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.save_recent_chat.response.SaveRecentChatResponse
import com.app.hihlo.model.search_user_list.response.SearchUserListResponse
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.model.unblock_user.request.UnblockUserRequest
import com.app.hihlo.model.wallet_history.WalletHistoryResponse
import com.app.hihlo.model.save_call.SaveCallRequest
import com.app.hihlo.model.save_call.SaveCallResponse
import com.app.hihlo.model.send_gift.SendGiftResponse
import com.app.hihlo.model.set_notification.SetNotificationRequest
import com.app.hihlo.model.story_response.StoryResponse
import com.app.hihlo.model.story_upload_status_model.StoryUploadStatusResponse
import com.app.hihlo.model.update_call_charge.UpdateCallChargeResponse
import com.app.hihlo.model.update_call_status.UpdateCallStatusRequest
import com.app.hihlo.model.update_call_status.UpdateCallStatusResponse
import com.app.hihlo.ui.profile.become_creater.model.CreatorsBenefitsResponse
import com.app.hihlo.ui.profile.become_creater.model.SendOtpPhoneRequest
import com.app.hihlo.ui.profile.become_creater.model.UserToCreatorRequest
import com.app.hihlo.ui.profile.become_creater.model.VerifyPhoneOtpRequest
import com.app.hihlo.ui.profile.model.DeleteAccountRequest
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path


interface ApiService {
    @GET("home")
    suspend fun getHomeData(
        @Header("Authorization") token: String,
        @Query("page") page: String? = null,
        @Query("pageSize") limit: String? = null,
        @Query("genderId") genderId: String? = null,
    ): HomeResponse

    @POST("set-cover-photo")
    suspend fun setRemoveCoverApi(
        @Header("Authorization") token: String,
        @Body setRemoveCoverRequest: SetRemoveCoverRequest,
    ): SetRemoveCoverResponse

    @GET("get-all-status-temp")
    suspend fun getStatusData(
        @Header("Authorization") token: String,
        @Query("genderId") genderId: String? = null,
    ): StoryResponse

    @GET("reels")
    suspend fun getReels(
        @Header("Authorization") token: String,
        @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): ReelsResponse

    @GET("reel-comments/{reelId}")
    suspend fun getReelComments(
        @Header("Authorization") token: String, @Path("reelId") reelId: String,
        @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): ReelCommentsResponse

    @POST("post-comments/{reelId}")
    suspend fun postComments(
        @Header("Authorization") token: String,
        @Body requestBody: PostCommentsRequest,
        @Path("reelId") reelId: String,
    ): PostCommentsResponse

    @POST("reply-on-comments/{reelId}")
    suspend fun replyToComment(
        @Header("Authorization") token: String,
        @Body requestBody: ReplyToCommentRequest,
        @Path("reelId") reelId: String,
    ): ReplyToCommentResponse

    @GET("block-reasons")
    suspend fun getBlockReasons(): BlockReasonsResponse

    @GET("flag-reasons")
    suspend fun getFlagReasons(): BlockReasonsResponse

    @POST("block-user")
    suspend fun blockUser(
        @Header("Authorization") token: String,
        @Body requestBody: BlockUserRequest,
    ): PostCommentsResponse

    @POST("unblock-user")
    suspend fun unblockUser(
        @Header("Authorization") token: String,
        @Body requestBody: UnblockUserRequest,
    ): PostCommentsResponse

    @POST("flag-user")
    suspend fun flagUser(
        @Header("Authorization") token: String,
        @Body requestBody: FlagUserRequest,
    ): PostCommentsResponse

    @POST("like-reels/{reelId}")
    suspend fun likeReel(
        @Header("Authorization") token: String,
        @Path("reelId") reelId: String,
    ): PostCommentsResponse

    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): GetProfileResponse

    @POST("edit-profile")
    suspend fun getEditProfile(
        @Header("Authorization") token: String,
        @Body request: EditProfileRequest,
    ): EditProfileResponse

    @GET("profile-detail/{userId}")
    suspend fun getOtherUserProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): GetProfileResponse

    @POST("add-story")
    suspend fun addStory(
        @Header("Authorization") token: String,
        @Body request: AddStoryRequest,
    ): AddStoryResponse

    @POST("story-seen")
    suspend fun storySeen(
        @Header("Authorization") token: String,
        @Body request: StorySeen,
    ): CommonResponse

    @GET("story-upload-status")
    suspend fun getStoryUploadStatus(@Header("Authorization") token: String): StoryUploadStatusResponse

    @POST("delete-story")
    suspend fun deleteStory(
        @Header("Authorization") token: String,
        @Body request: StoryDeleteRequest,
    ): CommonResponse

    @GET("followings-list")
    suspend fun getFollowingList(
        @Header("Authorization") token: String,
        @Query("self") self: String? = null,
        @Query("other") other: String? = null,
        @Query("otherUserId") otherUserId: String? = null,
    ): FollowingListResponse

    @GET("followers-list")
    suspend fun getFollowersList(
        @Header("Authorization") token: String,
        @Query("self") self: String? = null,
        @Query("other") other: String? = null,
        @Query("otherUserId") otherUserId: String? = null,
    ): FollowingListResponse

    @POST("follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Body request: FollowRequest,
    ): CommonResponse

    @POST("unfollow")
    suspend fun unfollowUser(
        @Header("Authorization") token: String,
        @Body request: FollowRequest,
    ): CommonResponse

    @GET("interests")
    suspend fun interestsList(): InterestListResponse

    @GET("cities")
    suspend fun citiesList(
        @Query("search") search: String? = null,
        @Query("limit") limit: String? = null,
        @Query("page") page: String? = null,
    ): CityListResponse

    @POST("add-post")
    suspend fun addPost(
        @Header("Authorization") token: String,
        @Body request: AddPostRequest,
    ): CommonResponse

    @POST("post-reels")
    suspend fun addReel(
        @Header("Authorization") token: String,
        @Body request: AddPostRequest,
    ): CommonResponse

    @GET("rtc-token")
    suspend fun generateAgoraToken(
        @Header("Authorization") token: String,
        @Query("channelName") channelName: String? = null,
        @Query("calleeId") calleeId: String? = null,
        @Query("callerId") uid: String? = null,
        @Query("callType") callType: String? = null,
        @Query("sender_id") sender_id: String? = null,
    ): AgoraTokenResponse

    @GET("delete-reasons")
    suspend fun deleteAccountReasons(): BlockReasonsResponse

    @POST("delete-account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Body request: DeleteAccountRequest,
    ): CommonResponse

    @GET("logout")
    suspend fun logoutUser(@Header("Authorization") token: String): CommonResponse

    @GET("recent-chats")
    suspend fun getRecentChats(
        @Header("Authorization") token: String,
        @Query("fromUserId") fromUserId: String? = null,
        @Query("toUserId") toUserId: String? = null,
        @Query("type") type: String? = null,
    ): GetRecentChatResponse

    @POST("save-recent-chat")
    suspend fun saveRecentChat(
        @Header("Authorization") token: String,
        @Body request: SaveRecentChatRequest,
    ): SaveRecentChatResponse

    @GET("predefined-chats")
    suspend fun getPredefinedChats(@Header("Authorization") token: String): PredefinedChatsResponse

    @GET("creator-benefits")
    suspend fun getCreatorBenefits(): CreatorsBenefitsResponse

    @POST("send-otp")
    suspend fun sendOtpPhone(
        @Header("Authorization") token: String,
        @Body request: SendOtpPhoneRequest,
    ): CreatorsBenefitsResponse

    @POST("verify-otp")
    suspend fun verifyPhoneOtp(
        @Header("Authorization") token: String,
        @Body request: VerifyPhoneOtpRequest,
    ): CreatorsBenefitsResponse

    @POST("user-to-creator")
    suspend fun userToCreator(
        @Header("Authorization") token: String,
        @Body request: UserToCreatorRequest,
    ): CreatorsBenefitsResponse

    @POST("end-call")
    suspend fun endCall(
        @Header("Authorization") token: String,
        @Body request: EndCallRequest,
    ): CommonResponse

    @GET("blocked-users")
    suspend fun getBlockedUsers(@Header("Authorization") token: String): BlockedUsersResponse

    @GET("search-users")
    suspend fun getSearchUserList(
        @Header("Authorization") token: String,
        @Query("searchKey") searchKey: String? = null,
        @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): SearchUserListResponse

    @GET("faqs")
    suspend fun getFaqList(): FaqResponse

    @FormUrlEncoded
    @POST("delete-post")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Field("postId") postId: String,
    ): CommonResponse

    @FormUrlEncoded
    @POST("live-status")
    suspend fun updateLiveStatus(
        @Header("Authorization") token: String,
        @Field("liveStatusId") liveStatusId: Int,
    ): CommonResponse

    @GET("notification-list")
    suspend fun getNotificationList(
        @Header("Authorization") token: String, @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): GetNotificationListResponse

    @POST("delete-chats")
    suspend fun deleteRecentChat(
        @Header("Authorization") token: String,
        @Body request: SaveRecentChatRequest,
    ): CommonResponse

    @GET("recharge-packages")
    suspend fun rechargeCoinsList(): RechargePackageListResponse

    @POST("contact-us")
    suspend fun contactUs(
        @Header("Authorization") token: String,
        @Body request: ContactUsRequest,
    ): CommonResponse

    @POST("feedback")
    suspend fun ratingReview(
        @Header("Authorization") token: String,
        @Body request: RatingReviewRequest,
    ): CommonResponse

    @POST("send-gift")
    suspend fun sendGift(
        @Header("Authorization") token: String,
        @Body request: SendGiftRequest,
    ): SendGiftResponse

    @GET("wallet-history")
    suspend fun walletHistory(@Header("Authorization") token: String): WalletHistoryResponse

    @POST("add-wallet-coins")
    suspend fun addWalletCoins(
        @Header("Authorization") token: String,
        @Body request: AddCoinsRequest,
    ): CommonResponse

    @FormUrlEncoded
    @POST("user-withdrawal-coins")
    suspend fun withdrawCoins(
        @Header("Authorization") token: String,
        @Field("coins") coins: String,
    ): CommonResponse

//    @POST("deduct-call-coin")
//    suspend fun deductCallCoins(@Header("Authorization") token: String, @Body request: DeductCoinRequest): CommonResponse

    @GET("coin-details")
    suspend fun getCoinDetails(@Header("Authorization") token: String): CoinDetailsResponse

    @GET("predefined-chats-of-user")
    suspend fun getPredefinedChatsOfUser(@Header("Authorization") token: String): PredefinedChatsResponse

    @FormUrlEncoded
    @POST("add-predefined-chat")
    suspend fun addPredefinedChat(
        @Header("Authorization") token: String,
        @Field("preDefinedChat") preDefinedChat: String,
    ): CommonResponse

    @FormUrlEncoded
    @POST("edit-predefined-chat")
    suspend fun editPredefinedChat(
        @Header("Authorization") token: String,
        @Field("preDefinedChatId") preDefinedChatId: String,
        @Field("preDefinedChat") preDefinedChat: String,
    ): CommonResponse


    @FormUrlEncoded
    @POST("delete-predefined-chat")
    suspend fun deletePredefinedChat(
        @Header("Authorization") token: String,
        @Field("preDefinedChatId") preDefinedChatId: String,
    ): CommonResponse

    @GET("notification-read")
    suspend fun notificationRead(@Header("Authorization") token: String): CommonResponse

    @POST("notification-delete")
    suspend fun notificationDelete(@Header("Authorization") token: String): CommonResponse

    @POST("save-call-details")
    suspend fun saveCallDetails(
        @Header("Authorization") token: String,
        @Body request: SaveCallRequest,
    ): SaveCallResponse

    @POST("update-call-status")
    suspend fun updateCallStatus(
        @Header("Authorization") token: String,
        @Body request: UpdateCallStatusRequest,
    ): UpdateCallStatusResponse

    @POST("deduct-call-coin")
    suspend fun deductCallCoin(
        @Header("Authorization") token: String,
        @Body request: DeductCallCoinRequest,
    ): DeductCallCoinResponse

    @POST("deduct-coins-on-chat")
    suspend fun deductCallCoinOnChat(
        @Header("Authorization") token: String,
        @Body request: DeductChatCoinRequest,
    ): CommonResponse

    @GET("get-ads")
    suspend fun getAds(@Header("Authorization") token: String): GetAdsListResponse

    @FormUrlEncoded
    @POST("chat-history")
    suspend fun checkChatHistory(
        @Header("Authorization") token: String,
        @Field("toUserId") toUserId: String,
    ): ChatHistoryResponse

    @FormUrlEncoded
    @POST("view-ads")
    suspend fun viewAds(
        @Header("Authorization") token: String,
        @Field("adId") adId: String,
    ): CommonResponse

    @FormUrlEncoded
    @POST("delete-reel")
    suspend fun deleteReel(
        @Header("Authorization") token: String,
        @Field("reelId") reelId: String,
    ): CommonResponse

    @FormUrlEncoded
    @POST("handle-message-request")
    suspend fun handleMessageRequest(
        @Header("Authorization") token: String,
        @Field("chatId") chatId: String,
        @Field("action") action: String,
    ): CommonResponse

    @FormUrlEncoded
    @POST("update-charges")
    suspend fun updateCoinsRequest(
        @Header("Authorization") token: String,
        @Field("audioCallPrice") chatId: String,
        @Field("videoCallPrice") action: String,
    ): UpdateCallChargeResponse

    @GET("gender-list")
    suspend fun getGenderListApi(@Query("type") login: String? = null): GenderListResponse

    @POST("notification-settings")
    suspend fun setNotifications(
        @Header("Authorization") token: String,
        @Body request: SetNotificationRequest,
    ): CommonResponse

    @POST("read-messages/{chatId}")
    suspend fun readMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String? = null,
        @Query("anotherUserId") anotherUserId: String? = null,
    ): CommonResponse

    @GET("get-notification-settings")
    suspend fun getNotificationSettings(@Header("Authorization") token: String): GetNotificationSettingResponse

    @GET("get-post-comments/{postId}")
    suspend fun getPostComments(
        @Header("Authorization") token: String, @Path("postId") postId: String,
        @Query("page") page: String? = null,
        @Query("limit") limit: String? = null,
    ): ReelCommentsResponse

    @POST("comment-on-post/{postId}")
    suspend fun postCommentToPost(
        @Header("Authorization") token: String,
        @Body requestBody: PostCommentsRequest,
        @Path("postId") postId: String,
    ): PostCommentsResponse

    @POST("reply-on-post-comments/{postId}")
    suspend fun replyToPostComment(
        @Header("Authorization") token: String,
        @Body requestBody: ReplyToCommentRequest,
        @Path("postId") postId: String,
    ): ReplyToCommentResponse

    @POST("like-posts/{postId}")
    suspend fun likePost(
        @Header("Authorization") token: String,
        @Path("postId") postId: String,
    ): PostCommentsResponse

    @FormUrlEncoded
    @POST("delete-post-comment")
    suspend fun deletePostComment(
        @Header("Authorization") token: String,
        @Field("comment_id") commentId: String,
        @Field("mode") mode: String,
        @Field("post_id") post_id: String,
    ): DeleteResponse

    @FormUrlEncoded
    @POST("delete-reels-comment")
    suspend fun deleteReelsComment(
        @Header("Authorization") token: String,
        @Field("comment_id") commentId: String,
        @Field("mode") mode: String,
        @Field("post_id") post_id: String,
    ): DeleteResponse

    @FormUrlEncoded
    @POST("get-userid-by-username")
    suspend fun getUserIdByUserName(
        @Header("Authorization") token: String,
        @Field("user_name") user_name: String,
    ): GetUserIdByUserNameResponse

}

