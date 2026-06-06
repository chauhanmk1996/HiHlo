package com.app.hihlo.ui.chat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class SendCoinsBottomSheetViewModel : ViewModel(){

    private val rechargeCoinsLiveData = SingleLiveEvent<Resources<RechargePackageListResponse>>()

    fun getRechargeCoinsLiveData(): LiveData<Resources<RechargePackageListResponse>> {
        return rechargeCoinsLiveData
    }
    fun hitRechargeCoinsApi() {

        try {
            rechargeCoinsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    rechargeCoinsLiveData.postValue(
                        Resources.success(
                            ApiRepository().rechargeCoinsListApi(
                            )
                        )
                    )
                } catch (ex: Exception) {
                    rechargeCoinsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}