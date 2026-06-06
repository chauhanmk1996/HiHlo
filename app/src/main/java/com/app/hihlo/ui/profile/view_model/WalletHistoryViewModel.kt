package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.wallet_history.WalletHistoryResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class WalletHistoryViewModel : ViewModel() {
    private val walletHistoryLiveData = SingleLiveEvent<Resources<WalletHistoryResponse>>()

    fun getWalletHistoryLiveData(): LiveData<Resources<WalletHistoryResponse>> {
        return walletHistoryLiveData
    }
    fun hitWalletHistoryDataApi(token: String) {

        try {
            walletHistoryLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    walletHistoryLiveData.postValue(
                        Resources.success(
                            ApiRepository().walletHistoryApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    walletHistoryLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val withdrawCoinsLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getWithdrawCoinsLiveData(): LiveData<Resources<CommonResponse>> {
        return withdrawCoinsLiveData
    }
    fun hitWithdrawCoinsDataApi(token: String, coins: String) {

        try {
            withdrawCoinsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    withdrawCoinsLiveData.postValue(
                        Resources.success(
                            ApiRepository().withdrawCoinsApi(token, coins
                            )
                        )
                    )
                } catch (ex: Exception) {
                    withdrawCoinsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}