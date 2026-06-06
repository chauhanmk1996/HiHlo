package com.app.hihlo.ui.calling.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.deduct_call_coin.DeductCallCoinRequest
import com.app.hihlo.model.deduct_call_coin.DeductCallCoinResponse
import com.app.hihlo.model.deduct_coin.request.DeductCoinRequest
import com.app.hihlo.model.end_call.request.EndCallRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class IncomingCallViewModel : ViewModel() {
    private val endCallLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getEndCallLiveData(): LiveData<Resources<CommonResponse>> {
        return endCallLiveData
    }
    fun hitEndCallDataApi(token: String, request: EndCallRequest) {

        try {
            endCallLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    endCallLiveData.postValue(
                        Resources.success(
                            ApiRepository().endCallApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    endCallLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val coinDeductionLiveData = SingleLiveEvent<Resources<DeductCallCoinResponse>>()

    fun getCoinDeductionLiveData(): LiveData<Resources<DeductCallCoinResponse>> {
        return coinDeductionLiveData
    }
    fun hitCoinDeductionDataApi(token: String, request: DeductCallCoinRequest) {

        try {
            coinDeductionLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    coinDeductionLiveData.postValue(
                        Resources.success(
                            ApiRepository().deductCallCoinsApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    coinDeductionLiveData.postValue(Resources.error(ex.localizedMessage, null))

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
}