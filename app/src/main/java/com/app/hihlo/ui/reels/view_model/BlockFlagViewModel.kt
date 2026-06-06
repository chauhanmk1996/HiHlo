package com.app.hihlo.ui.reels.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.block_reasons.response.BlockReasonsResponse
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.flag_user.request.FlagUserRequest
import com.app.hihlo.model.post_comments.response.PostCommentsResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.profile.model.DeleteAccountRequest
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import kotlinx.coroutines.launch

class BlockFlagViewModel:ViewModel() {
    private val getBlockReasonsLiveData = SingleLiveEvent<Resources<BlockReasonsResponse>>()

    fun getBlockReasonsLiveData(): LiveData<Resources<BlockReasonsResponse>> {
        return getBlockReasonsLiveData
    }
    fun hitGetBlockReasonsApi() {

        try {
            getBlockReasonsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getBlockReasonsLiveData.postValue(
                        Resources.success(
                            ApiRepository().getBlockReasonsApi()
                        )
                    )
                } catch (ex: Exception) {
                    getBlockReasonsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val getFlagReasonsLiveData = SingleLiveEvent<Resources<BlockReasonsResponse>>()

    fun getFlagReasonsLiveData(): LiveData<Resources<BlockReasonsResponse>> {
        return getFlagReasonsLiveData
    }
    fun hitGetFlagReasonsApi() {

        try {
            getFlagReasonsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getFlagReasonsLiveData.postValue(
                        Resources.success(
                            ApiRepository().getFlagReasonsApi()
                        )
                    )
                } catch (ex: Exception) {
                    getFlagReasonsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val blockUserLiveData = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getBlockUserLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return blockUserLiveData
    }
    fun hitBlockUserApi(token:String, request:BlockUserRequest) {

        try {
            blockUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    blockUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().blockUserApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    blockUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val flagUserLiveData = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getFlagUserLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return flagUserLiveData
    }
    fun hitFlagUserApi(token:String, request:FlagUserRequest) {

        try {
            flagUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    flagUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().flagUserApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    flagUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val deleteReasonsLiveData = SingleLiveEvent<Resources<BlockReasonsResponse>>()

    fun getDeleteAccountReasonLiveData(): LiveData<Resources<BlockReasonsResponse>> {
        return deleteReasonsLiveData
    }
    fun hitDeleteAccountReason() {

        try {
            deleteReasonsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deleteReasonsLiveData.postValue(
                        Resources.success(
                            ApiRepository().deleteAccountReasons()
                        )
                    )
                } catch (ex: Exception) {
                    deleteReasonsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val deleteAccountLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeleteAccountLiveData(): LiveData<Resources<CommonResponse>> {
        return deleteAccountLiveData
    }
    fun hitDeleteAccount(token: String,model: DeleteAccountRequest) {

        try {
            deleteAccountLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deleteAccountLiveData.postValue(
                        Resources.success(
                            ApiRepository().deleteAccount(token,model)
                        )
                    )
                } catch (ex: Exception) {
                    deleteAccountLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}