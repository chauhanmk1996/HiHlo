package com.app.hihlo.ui.home.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.notification.response.GetNotificationListResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class NotificationViewModel: ViewModel() {
    private val notificationListLiveDate = SingleLiveEvent<Resources<GetNotificationListResponse>>()

    fun getNotificationLiveData(): LiveData<Resources<GetNotificationListResponse>> {
        return notificationListLiveDate
    }
    fun hitNotificationListDataApi(token: String, limit: String, page: String) {

        try {
            notificationListLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    notificationListLiveDate.postValue(
                        Resources.success(
                            ApiRepository().getNotificationListApi(token, limit, page
                            )
                        )
                    )
                } catch (ex: Exception) {
                    notificationListLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val readNotificationLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getReadNotificationLiveData(): LiveData<Resources<CommonResponse>> {
        return readNotificationLiveData
    }
    fun hitReadNotificationDataApi(token: String) {

        try {
            readNotificationLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    readNotificationLiveData.postValue(
                        Resources.success(
                            ApiRepository().notificationReadApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    readNotificationLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val deleteNotificationLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeleteNotificationLiveData(): LiveData<Resources<CommonResponse>> {
        return deleteNotificationLiveData
    }
    fun hitDeleteNotificationDataApi(token: String) {

        try {
            deleteNotificationLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deleteNotificationLiveData.postValue(
                        Resources.success(
                            ApiRepository().notificationDeleteApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    deleteNotificationLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}