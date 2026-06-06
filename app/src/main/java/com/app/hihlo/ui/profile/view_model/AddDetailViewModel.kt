package com.app.hihlo.ui.profile.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.gender_list.GenderListResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class AddDetailViewModel :ViewModel() {
    private val genderListLiveData = SingleLiveEvent<Resources<GenderListResponse>>()
    fun getGenderLiveData(): LiveData<Resources<GenderListResponse>> {
        return genderListLiveData
    }
    fun hitGenderListApi() {
        try {
            genderListLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    genderListLiveData.postValue(
                        Resources.success(
                            ApiRepository().getGenderListApi("login")
                        )
                    )
                } catch (ex: Exception) {
                    genderListLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}