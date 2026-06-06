package com.app.hihlo.ui.profile.become_creater.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.profile.become_creater.model.CreatorsBenefitsResponse
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class CreatorsBenefitsViewModel :ViewModel() {
    private val creatorsBenefitsLiveData = SingleLiveEvent<Resources<CreatorsBenefitsResponse>>()

    fun getCreatorBenefitsLiveData(): LiveData<Resources<CreatorsBenefitsResponse>> {
        return creatorsBenefitsLiveData
    }
    fun hitCreatorsBenefits() {

        try {
            creatorsBenefitsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    creatorsBenefitsLiveData.postValue(
                        Resources.success(
                            ApiRepository().getCreatorBenefits()
                        )
                    )
                } catch (ex: Exception) {
                    creatorsBenefitsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}