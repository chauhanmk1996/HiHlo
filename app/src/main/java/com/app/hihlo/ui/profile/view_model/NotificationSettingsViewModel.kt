package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.gender_list.GenderListResponse
import com.app.hihlo.model.get_notification_setting.response.GetNotificationSettingResponse
import com.app.hihlo.model.set_notification.SetNotificationRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class NotificationSettingsViewModel : ViewModel() {
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
}