package com.app.hihlo.ui.profile.view_model
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.check_username.request.CheckUsernameRequest
import com.app.hihlo.model.check_username.response.CheckUsernameResponse
import com.app.hihlo.model.city_list.response.Cities
import com.app.hihlo.model.city_list.response.CityListResponse
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.edit_profile.request.EditProfileRequest
import com.app.hihlo.model.edit_profile.response.EditProfileResponse
import com.app.hihlo.model.gender_list.GenderListResponse
import com.app.hihlo.model.interest_list.response.InterestListResponse
import com.app.hihlo.model.interest_list.response.Interests
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch



class EditProfileViewModel :ViewModel() {
    private val _citiesCache = MutableLiveData<List<Cities>>()
    private val genderListLiveData = SingleLiveEvent<Resources<GenderListResponse>>()

    private val citiesListLiveData = SingleLiveEvent<Resources<CityListResponse>>()

    fun getCitiesListLiveData(): LiveData<Resources<CityListResponse>> {
        return citiesListLiveData
    }
    fun hitCitiesListDataApi(search: String? = null, limit: String? = null, page: String? = null) {

        try {
            citiesListLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    citiesListLiveData.postValue(
                        Resources.success(
                            ApiRepository().getCitiesListApi(search, limit, page)
                        )
                    )
                } catch (ex: Exception) {
                    citiesListLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getGenderLiveData(): LiveData<Resources<GenderListResponse>> {
        return genderListLiveData
    }



    private val editProfileLiveData = SingleLiveEvent<Resources<EditProfileResponse>>()

    fun getEditProfileLiveData(): LiveData<Resources<EditProfileResponse>> {
        return editProfileLiveData
    }
    fun hitEditProfileDataApi(token: String, request: EditProfileRequest) {
        try {
            editProfileLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    editProfileLiveData.postValue(
                        Resources.success(
                            ApiRepository().getEditProfileApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    editProfileLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
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


    private val _interestCache = MutableLiveData<List<Interests>>()
    val interestCache: LiveData<List<Interests>> get() = _interestCache

    fun hitInterestListDataApiIfNeeded() {
        if (_interestCache.value.isNullOrEmpty()) {
            hitInterestListDataApi() // Your existing API call
        }
    }
    fun catcheInterstList(list: List<Interests>) {
        _interestCache.value = list
    }


    private val interestListLiveData = SingleLiveEvent<Resources<InterestListResponse>>()

    fun getInterestListLiveData(): LiveData<Resources<InterestListResponse>> {
        return interestListLiveData
    }
    fun hitInterestListDataApi() {

        try {
            interestListLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    interestListLiveData.postValue(
                        Resources.success(
                            ApiRepository().getInterestListApi(
                            )
                        )
                    )
                } catch (ex: Exception) {
                    interestListLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val checkUsernameLiveData = SingleLiveEvent<Resources<CheckUsernameResponse>>()

    fun getCheckUsernameLiveData(): LiveData<Resources<CheckUsernameResponse>> {
        return checkUsernameLiveData
    }
    fun hitCheckUsernameDataApi(username: String) {

        try {
            checkUsernameLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    checkUsernameLiveData.postValue(
                        Resources.success(
                            ApiRepository().checkUsernameApi(CheckUsernameRequest(username))
                        )
                    )
                } catch (ex: Exception) {
                    checkUsernameLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}