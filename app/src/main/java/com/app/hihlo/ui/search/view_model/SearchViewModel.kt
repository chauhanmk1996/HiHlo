package com.app.hihlo.ui.search.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.search_user_list.response.SearchUserListResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class SearchViewModel : ViewModel() {
    private val searchUsersListLiveData = SingleLiveEvent<Resources<SearchUserListResponse>>()

    fun getUsersListLiveData(): LiveData<Resources<SearchUserListResponse>> {
        return searchUsersListLiveData
    }

    fun hitSearchUsersList(token:String,page:String, limit:String,searchKey:String) {

        try {
            searchUsersListLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    searchUsersListLiveData.postValue(
                        Resources.success(
                            ApiRepository().getSearchUsersListApi(token, page, limit, searchKey)
                        )
                    )

                } catch (ex: Exception) {
                    searchUsersListLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}