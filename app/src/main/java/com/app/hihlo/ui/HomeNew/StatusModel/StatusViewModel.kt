package com.app.hihlo.ui.HomeNew.StatusModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.story_response.StoryResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class StatusViewModel:ViewModel() {

    private val statusLiveDate = SingleLiveEvent<Resources<StoryResponse>>()

    fun getStatusLiveData(): LiveData<Resources<StoryResponse>> {
        return statusLiveDate
    }

    fun hitStatusDataApi(token: String, genderId: String) {

        try {
            statusLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    statusLiveDate.postValue(
                        Resources.success(
                            ApiRepository().getStatusDataApi(token, genderId)
                        )
                    )
                } catch (ex: Exception) {
                    statusLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}