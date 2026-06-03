package com.app.hihlo.network_call.repository

import com.app.hihlo.model.add_coins.AddCoinsRequest
import com.app.hihlo.model.add_post.request.AddPostRequest
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.check_username.request.CheckUsernameRequest
import com.app.hihlo.model.contact_us.request.ContactUsRequest
import com.app.hihlo.model.deduct_call_coin.DeductCallCoinRequest
import com.app.hihlo.model.deduct_chat_coin.DeductChatCoinRequest
import com.app.hihlo.model.deduct_coin.request.DeductCoinRequest
import com.app.hihlo.model.edit_profile.request.EditProfileRequest
import com.app.hihlo.model.end_call.request.EndCallRequest
import com.app.hihlo.model.flag_user.request.FlagUserRequest
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.login.request.LoginRequest
import com.app.hihlo.model.post_comments.request.PostCommentsRequest
import com.app.hihlo.model.rating_review.RatingReviewRequest
import com.app.hihlo.model.reply_to_comment.request.ReplyToCommentRequest
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.model.unblock_user.request.UnblockUserRequest
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.model.save_call.SaveCallRequest
import com.app.hihlo.model.set_notification.SetNotificationRequest
import com.app.hihlo.model.update_call_status.UpdateCallStatusRequest
import com.app.hihlo.ui.profile.become_creater.model.SendOtpPhoneRequest
import com.app.hihlo.ui.profile.become_creater.model.UserToCreatorRequest
import com.app.hihlo.ui.profile.become_creater.model.VerifyPhoneOtpRequest
import com.app.hihlo.ui.profile.model.DeleteAccountRequest
import com.app.hihlo.ui.signup.model.ChangePasswordRequest
import com.app.hihlo.ui.signup.model.ResetPasswordRequest
import com.app.hihlo.ui.signup.model.SignUp
import com.app.hihlo.ui.signup.model.SocialLoginRequest
import com.app.hihlo.ui.signup.model.SocialSignUpRequest
import retrofit2.http.Field
import retrofit2.http.Query

class ApiRepository {
    private val service = RetrofitBuilder.apiService

    suspend fun loginApi(request: LoginRequest) = service.login(request)

    suspend fun socialLogin(request: SocialSignUpRequest) = service.socialLogin(request)

    suspend fun resetPassword(request: ResetPasswordRequest) = service.resetPassword(request)

    suspend fun changePassword(token:String,request: ChangePasswordRequest) = service.changePassword(token,request)


    suspend fun getHomeDataApi(
        token: String, page: String,
        limit: String, genderId:String
    ) = service.getHomeData(
        token = token,
        if (page.isEmpty()) null else page,
        if (limit.isEmpty()) null else limit,
        if (genderId.isEmpty()) null else genderId,
    )

    suspend fun getStatusDataApi(
        token: String, genderId: String
    ) = service.getStatusData(
        token = token,
        genderId = genderId
    )

    suspend fun getReelsApi(
        token: String, page: String,
        limit: String,
    ) = service.getReels(
        token = token,
        page.ifEmpty { null },
        limit.ifEmpty { null },
    )

    suspend fun getReelCommentsApi(token: String, reelId:String,
                                   page: String? = null,
                                   limit: String? = null ) = service.getReelComments(token = token, reelId, page,limit )
    suspend fun addPostApi(token: String, request: AddPostRequest) = service.addPost(token, request)
    suspend fun addReelApi(token: String, request: AddPostRequest) = service.addReel(token, request)
    suspend fun postReelCommentApi(token: String, request:PostCommentsRequest, reelId:String) = service.postComments(token = token, request, reelId)
    suspend fun replyToCommentApi(token: String, request:ReplyToCommentRequest, reelId:String) = service.replyToComment(token = token, request, reelId)
    suspend fun getBlockReasonsApi() = service.getBlockReasons()
    suspend fun getFlagReasonsApi() = service.getFlagReasons()
    suspend fun blockUserApi(token: String, request: BlockUserRequest) = service.blockUser(token, request)
    suspend fun unblockUserApi(token: String, request: UnblockUserRequest) = service.unblockUser(token, request)
    suspend fun flagUserApi(token: String, request: FlagUserRequest) = service.flagUser(token, request)
    suspend fun likeReelApi(token: String, reelId: String) = service.likeReel(token, reelId)
    suspend fun getProfileApi(token: String, page: String, limit: String) = service.getProfile(token, if (page.isEmpty()) null else page,
    if (limit.isEmpty()) null else limit,)
    suspend fun getEditProfileApi(token: String, request: EditProfileRequest) = service.getEditProfile(token, request)
    suspend fun getOtherUserProfileApi(token: String, userId:String, page: String, limit: String) = service.getOtherUserProfile(token, userId, if (page.isEmpty()) null else page,
        if (limit.isEmpty()) null else limit,)
    suspend fun addStoryApi(token: String, request: AddStoryRequest) = service.addStory(token, request)
    suspend fun storySeenApi(token: String, request: StorySeen) = service.storySeen(token, request)
    suspend fun storyDeleteApi(token: String, request: StoryDeleteRequest) = service.deleteStory(token, request)
    suspend fun getFollowingListApi(token: String, self: String, other: String, otherUserId: String) = service.getFollowingList(token, if (self.isEmpty()) null else self,if (other.isEmpty()) null else other,if (otherUserId.isEmpty()) null else otherUserId,)
    suspend fun getFollowersListApi(token: String, self: String, other: String, otherUserId: String) = service.getFollowersList(token, if (self.isEmpty()) null else self,if (other.isEmpty()) null else other,if (otherUserId.isEmpty()) null else otherUserId,)
    suspend fun getFollowUserApi(token: String, request:FollowRequest) = service.followUser(token, request)
    suspend fun getUnfollowUserApi(token: String, request:FollowRequest) = service.unfollowUser(token, request)
    suspend fun getInterestListApi() = service.interestsList()
    suspend fun getCitiesListApi(search: String? = null, limit: String? = null, page: String? = null) = service.citiesList(search, limit, page)
    suspend fun generateAgoraToken(
        token: String,
        channelName: String,
        calleeId: String,
        uid:String,
        callType:String,
        sender_id:String,
    ) = service.generateAgoraToken(
        token = token,
        if (channelName.isEmpty()) null else channelName,
        if (calleeId.isEmpty()) null else calleeId,
        if (uid.isEmpty()) null else uid,
        if (callType.isEmpty()) null else callType,
        if (sender_id.isEmpty()) null else sender_id,
    )

    suspend fun logoutUserApi(token: String) = service.logoutUser(token)

    suspend fun sendEmailOtp(email: String,userName:String?,purpose: String?) = service.sendMailOtp(email,userName,purpose)

    suspend fun verifyEmailOtp(email: String,otp:String) = service.verifyEmailOtp(email,otp)

    suspend fun registerUser(model: SignUp) = service.registerUser(model)

    suspend fun socialSignUp(model: SocialSignUpRequest) = service.socialSignUp(model)

    suspend fun getRecentChatApi(token:String,
                                 fromUserId: String? = null,
                                 toUserId: String? = null,
                                 type: String? = null,
    ) = service.getRecentChats(token, fromUserId, toUserId, type)
    suspend fun saveRecentChatApi(token:String, request: SaveRecentChatRequest) = service.saveRecentChat(token, request)

    suspend fun deleteAccountReasons() = service.deleteAccountReasons()

    suspend fun deleteAccount(token: String,model: DeleteAccountRequest) = service.deleteAccount(token,model)

    suspend fun getCreatorBenefits() = service.getCreatorBenefits()

    suspend fun sendOtpPhone(token: String,model: SendOtpPhoneRequest) = service.sendOtpPhone(token,model)

    suspend fun verifyPhoneOtp(token: String,model: VerifyPhoneOtpRequest) = service.verifyPhoneOtp(token,model)

    suspend fun userToCreator(token: String,model: UserToCreatorRequest) = service.userToCreator(token,model)

    suspend fun getPredefinedChatsApi(token: String) = service.getPredefinedChats(token)

    suspend fun endCallApi(token: String, request: EndCallRequest) = service.endCall(token, request)

    suspend fun getBlockedUsersApi(token: String) = service.getBlockedUsers(token)

    suspend fun getSearchUsersListApi(token: String, page: String, limit: String, searchKey: String) = service.getSearchUserList(token,searchKey, page, limit)

    suspend fun getFaqListApi() = service.getFaqList()

    suspend fun deletePostApi(token: String, postId: String) = service.deletePost(token, postId)

    suspend fun checkUsernameApi(request: CheckUsernameRequest) = service.checkUsername(request)

    suspend fun updateLiveStatusApi(token: String, liveStatusId: Int) = service.updateLiveStatus(token, liveStatusId)

    suspend fun getNotificationListApi(token: String, limit: String, page: String) = service.getNotificationList(token, page, limit)

    suspend fun deleteRecentChatApi(token: String, request: SaveRecentChatRequest) = service.deleteRecentChat(token ,request)

    suspend fun rechargeCoinsListApi() = service.rechargeCoinsList()

    suspend fun contactUsApi(token: String, request: ContactUsRequest) = service.contactUs(token ,request)

    suspend fun ratingReviewApi(token: String, request: RatingReviewRequest) = service.ratingReview(token ,request)

    suspend fun sendGiftApi(token: String, request: SendGiftRequest) = service.sendGift(token ,request)

    suspend fun walletHistoryApi(token: String) = service.walletHistory(token)

    suspend fun addCoinsApi(token: String, request: AddCoinsRequest) = service.addWalletCoins(token ,request)

    suspend fun withdrawCoinsApi(token: String, coins: String) = service.withdrawCoins(token, coins)

//    suspend fun deductCoinsApi(token: String, request: DeductCoinRequest) = service.deductCallCoins(token ,request)

    suspend fun coinDetailsApi(token: String) = service.getCoinDetails(token )

    suspend fun getPredefinedChatsOfUserApi(token: String) = service.getPredefinedChatsOfUser(token )

    suspend fun addPreDefinedChatApi(token: String, preDefinedChat: String) = service.addPredefinedChat(token, preDefinedChat)

    suspend fun editPreDefinedChatApi(token: String, preDefinedChatId: String, preDefinedChat: String) = service.editPredefinedChat(token, preDefinedChatId, preDefinedChat)

    suspend fun deletePreDefinedChatApi(token: String, id: String) = service.deletePredefinedChat(token, id)

    suspend fun notificationReadApi(token: String) = service.notificationRead(token)

    suspend fun notificationDeleteApi(token: String) = service.notificationDelete(token)

    suspend fun saveCallDetailsApi(token: String, request: SaveCallRequest) = service.saveCallDetails(token, request)

    suspend fun updateCallStatusApi(token: String, request: UpdateCallStatusRequest) = service.updateCallStatus(token, request)

    suspend fun deductCallCoinsApi(token: String, request: DeductCallCoinRequest) = service.deductCallCoin(token, request)

    suspend fun deductCoinsOnChatApi(token: String, request: DeductChatCoinRequest) = service.deductCallCoinOnChat(token, request)

    suspend fun getAdsApi(token: String) = service.getAds(token)

    suspend fun checkChatHistoryApi(token: String, toUserId: String) = service.checkChatHistory(token, toUserId)

    suspend fun viewAdsApi(token: String, adId: String) = service.viewAds(token, adId)

    suspend fun deleteReelApi(token: String, reelId: String) = service.deleteReel(token, reelId)

    suspend fun handleMessageRequestApi(token: String, chatId: String, action: String) = service.handleMessageRequest(token, chatId, action)
    suspend fun updateCoinsRequestApi(token: String, audioCallPrice:String, videoCallPrice: String) = service.updateCoinsRequest(token, audioCallPrice, videoCallPrice)
    suspend fun getGenderListApi(login: String? = null) = service.getGenderListApi(login)
    suspend fun setNotificationApi(token: String, request: SetNotificationRequest) = service.setNotifications(token, request)
    suspend fun readMessagesApi(token: String, chatId: String?=null, anotherUserId: String?=null) = service.readMessages(token, chatId, anotherUserId)
    suspend fun getNotificationSettingsApi(token: String) = service.getNotificationSettings(token)
    suspend fun getPostCommentsApi(token: String, postId:String,
                                   page: String? = null,
                                   limit: String? = null) = service.getPostComments(token = token, postId, page,limit)

    suspend fun addPostCommentApi(token: String, request:PostCommentsRequest, postId:String) = service.postCommentToPost(token = token, request, postId)
    suspend fun replyToPostCommentApi(token: String, request:ReplyToCommentRequest, postId:String) = service.replyToPostComment(token = token, request, postId)
    suspend fun likePostApi(token: String, reelId: String) = service.likePost(token, reelId)

}