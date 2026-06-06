package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.coin_details.CoinDetailsResponse
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.get_notification_setting.response.GetNotificationSettingResponse
import com.app.hihlo.model.get_profile.GetProfileResponse
import com.app.hihlo.model.set_notification.SetNotificationRequest
import com.app.hihlo.model.update_call_charge.UpdateCallChargeResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class GetProfileViewModel :ViewModel() {
    private val getPushNotificationLiveData = SingleLiveEvent<Resources<GetNotificationSettingResponse>>()
    fun getPushNotificationLiveData(): LiveData<Resources<GetNotificationSettingResponse>> {
        return getPushNotificationLiveData
    }
    fun hitGetPushNotificationApi(token: String) {
        try {
            getPushNotificationLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getPushNotificationLiveData.postValue(
                        Resources.success(
                            ApiRepository().getNotificationSettingsApi(token)
                        )
                    )
                } catch (ex: Exception) {
                    getPushNotificationLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val setPushNotificationLiveData = SingleLiveEvent<Resources<CommonResponse>>()
    fun pushNotificationLiveData(): LiveData<Resources<CommonResponse>> {
        return setPushNotificationLiveData
    }
    fun hitSetPushNotificationApi(token: String, request: SetNotificationRequest) {
        try {
            setPushNotificationLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    setPushNotificationLiveData.postValue(
                        Resources.success(
                            ApiRepository().setNotificationApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    setPushNotificationLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val profileLiveData = SingleLiveEvent<Resources<GetProfileResponse>>()
    private val updateCoinsLiveData = SingleLiveEvent<Resources<UpdateCallChargeResponse>>()


    fun getProfileLiveData(): LiveData<Resources<GetProfileResponse>> {
        return profileLiveData
    }
    fun getGetUpdateCoinsLiveData(): LiveData<Resources<UpdateCallChargeResponse>> {
        return updateCoinsLiveData
    }
    fun hitProfileDataApi(token: String, page:String, limit:String) {

        try {
            profileLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    profileLiveData.postValue(
                        Resources.success(
                            ApiRepository().getProfileApi(token, page, limit
                            )
                        )
                    )
                } catch (ex: Exception) {
                    profileLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    fun hitUpdateCoinsApi(token: String, audioCoins:String, videoCoins:String) {
        try {
            updateCoinsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    updateCoinsLiveData.postValue(
                        Resources.success(
                            ApiRepository().updateCoinsRequestApi(token, audioCoins, videoCoins)
                        )
                    )
                } catch (ex: Exception) {
                    updateCoinsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val otherUserProfileLiveData = SingleLiveEvent<Resources<GetProfileResponse>>()

    fun getOtherUserProfileLiveData(): LiveData<Resources<GetProfileResponse>> {
        return otherUserProfileLiveData
    }
    fun hitOtherUserProfileDataApi(token: String, userId:String, page:String, limit:String) {

        try {
            otherUserProfileLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    otherUserProfileLiveData.postValue(
                        Resources.success(
                            ApiRepository().getOtherUserProfileApi(token, userId, page, limit
                            )
                        )
                    )
                } catch (ex: Exception) {
                    otherUserProfileLiveData.postValue(Resources.error(ex.localizedMessage, null))

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




    private val updateOnlineStatusLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getUpdateOnlineStatusLiveData(): LiveData<Resources<CommonResponse>> {
        return updateOnlineStatusLiveData
    }
    fun hitUpdateOnlineStatusDataApi(token: String, liveStatusId: Int) {

        try {
            updateOnlineStatusLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    updateOnlineStatusLiveData.postValue(
                        Resources.success(
                            ApiRepository().updateLiveStatusApi(token, liveStatusId)
                        )
                    )
                } catch (ex: Exception) {
                    updateOnlineStatusLiveData.postValue(Resources.error(ex.localizedMessage, null))

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
}