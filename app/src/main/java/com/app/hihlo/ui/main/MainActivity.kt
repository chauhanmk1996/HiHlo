package com.app.hihlo.ui.main

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewActivity
import com.app.hihlo.databinding.ActivityMainBinding
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.signUpToHome.SignupFlowActivity
import com.app.hihlo.utils.getDeviceToken
import com.app.hihlo.utils.startScreen
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseNewActivity<ActivityMainBinding, MainViewModel>(R.layout.activity_main) {

    override fun getBindingVariable(): Int {
        return R.layout.activity_main
    }

    private val mViewModel: MainViewModel by viewModels()

    override fun initViews(viewBinding: ActivityMainBinding) {
        onBackPressedMethod()
        fullScreenLayout()
        FirebaseApp.initializeApp(this)
        getDeviceToken(sharedPreference)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(1000)
            startNextScreen()
        }
    }

    fun startNextScreen() {
        if (Preferences.getStringPreference(this, IS_LOGIN) == "2") {
            if (Preferences.getCustomModelPreference<LoginResponse>(
                    this,
                    LOGIN_DATA
                )?.payload?.city.isNullOrBlank()
                || Preferences.getCustomModelPreference<LoginResponse>(
                    this,
                    LOGIN_DATA
                )?.payload?.profileImage.isNullOrEmpty()
            ) {
                startScreen(SignupFlowActivity())
            } else {
                startScreen(HomeActivity())
            }
        } else {
            startScreen(SignupFlowActivity())
        }
        finish()
    }

    override fun getViewModel(): MainViewModel {
        return mViewModel
    }
}