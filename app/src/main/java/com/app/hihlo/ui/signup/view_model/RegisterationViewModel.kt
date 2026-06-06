package com.app.hihlo.ui.signup.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.ui.signup.model.SignUp
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent

import kotlinx.coroutines.launch




class RegisterationViewModel: ViewModel() {

    private val RegisterLiveDate = SingleLiveEvent<Resources<LoginResponse>>()

    fun getRegisterLiveData(): LiveData<Resources<LoginResponse>> {
        return RegisterLiveDate
    }

    fun hitRegisterUser(model: SignUp) {

        try {
            RegisterLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    RegisterLiveDate.postValue(
                        Resources.success(
                            ApiRepository().registerUser(model)
                        )
                    )


                } catch (ex: Exception) {
                    RegisterLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}