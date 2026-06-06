package com.app.hihlo.ui.chat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.predefined_chats.PredefinedChatsResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class PredefinedChatViewModel : ViewModel() {
    private val predefinedChatLiveData = SingleLiveEvent<Resources<PredefinedChatsResponse>>()

    fun getPredefinedChatLiveData(): LiveData<Resources<PredefinedChatsResponse>> {
        return predefinedChatLiveData
    }
    fun hitPredefinedChatsApi(token: String) {

        try {
            predefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    predefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().getPredefinedChatsApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    predefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }




    private val addPredefinedChatLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getAddPredefinedChatLiveData(): LiveData<Resources<CommonResponse>> {
        return addPredefinedChatLiveData
    }
    fun hitAddPredefinedChatApi(token: String, preDefinedChat: String) {

        try {
            addPredefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    addPredefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().addPreDefinedChatApi(token, preDefinedChat
                            )
                        )
                    )
                } catch (ex: Exception) {
                    addPredefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val deletePredefinedChatLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeletePredefinedChatLiveData(): LiveData<Resources<CommonResponse>> {
        return deletePredefinedChatLiveData
    }
    fun hitDeletePredefinedChatApi(token: String, id: String) {

        try {
            deletePredefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deletePredefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().deletePreDefinedChatApi(token, id
                            )
                        )
                    )
                } catch (ex: Exception) {
                    deletePredefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val predefinedChatOfUserLiveData = SingleLiveEvent<Resources<PredefinedChatsResponse>>()

    fun getPredefinedChatOfUserLiveData(): LiveData<Resources<PredefinedChatsResponse>> {
        return predefinedChatOfUserLiveData
    }
    fun hitPredefinedChatsOfUserApi(token: String) {

        try {
            predefinedChatOfUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    predefinedChatOfUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().getPredefinedChatsOfUserApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    predefinedChatOfUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}