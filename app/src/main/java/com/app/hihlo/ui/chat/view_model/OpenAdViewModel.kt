package com.app.hihlo.ui.chat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.deduct_chat_coin.DeductChatCoinRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class OpenAdViewModel : ViewModel() {
    private val viewAdsLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getViewAdsLiveData(): LiveData<Resources<CommonResponse>> {
        return viewAdsLiveData
    }
    fun hitViewAdsApi(token: String, adId: String) {

        try {
            viewAdsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    viewAdsLiveData.postValue(
                        Resources.success(
                            ApiRepository().viewAdsApi(token, adId)
                        )
                    )
                } catch (ex: Exception) {
                    viewAdsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}