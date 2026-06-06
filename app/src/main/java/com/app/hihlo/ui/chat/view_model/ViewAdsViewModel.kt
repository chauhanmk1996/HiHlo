package com.app.hihlo.ui.chat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.ads_list.GetAdsListResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class ViewAdsViewModel : ViewModel() {
    private val viewAdsLiveData = SingleLiveEvent<Resources<GetAdsListResponse>>()

    fun getViewAdsLiveData(): LiveData<Resources<GetAdsListResponse>> {
        return viewAdsLiveData
    }
    fun hitViewAdsApi(token: String) {

        try {
            viewAdsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    viewAdsLiveData.postValue(
                        Resources.success(
                            ApiRepository().getAdsApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    viewAdsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }}