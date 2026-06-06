package com.app.hihlo.ui.reels.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.add_post.request.AddPostRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class AddReelViewModel: ViewModel()  {
    private val addReelLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getAddReelLiveData(): LiveData<Resources<CommonResponse>> {
        return addReelLiveData
    }
    fun hitAddReelApi(token: String, request: AddPostRequest) {

        try {
            addReelLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    addReelLiveData.postValue(
                        Resources.success(
                            ApiRepository().addReelApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    addReelLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    private val addPostLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getAddPostLiveData(): LiveData<Resources<CommonResponse>> {
        return addPostLiveData
    }
    fun hitAddPostApi(token: String, request: AddPostRequest) {

        try {
            addPostLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    addPostLiveData.postValue(
                        Resources.success(
                            ApiRepository().addPostApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    addPostLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}