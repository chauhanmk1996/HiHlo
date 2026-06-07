package com.app.hihlo.ui.signUpToHome

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewActivity
import com.app.hihlo.databinding.ActivitySignupFlowBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.login.response.Payload
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import kotlin.getValue

class SignupFlowActivity :
    BaseNewActivity<ActivitySignupFlowBinding, SignUpToHomeViewModel>(R.layout.activity_signup_flow) {

    override fun getBindingVariable(): Int {
        return R.layout.activity_signup_flow
    }

    private val mViewModel: SignUpToHomeViewModel by viewModels()
    private lateinit var navController: NavController

    override fun initViews(viewBinding: ActivitySignupFlowBinding) {
        applyWindowInsets(viewBinding.root)
        setUpNavFragment()
    }

    private fun setUpNavFragment() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.signup_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val loginData = Preferences.getCustomModelPreference<LoginResponse>(
            this,
            LOGIN_DATA
        )?.payload
        if ((loginData?.city.isNullOrBlank() || loginData.profileImage.isNullOrEmpty()) && Preferences.getStringPreference(this, IS_LOGIN) == "2") {
            val bundle = Bundle()
            val userDetails = loginData?.toUserDetailsX()
            bundle.putString("from", "social")
            bundle.putParcelable("userDetail", userDetails)
            navController.navigate(R.id.editProfileNewFragment, bundle)
        }
    }

    fun isGestureNavigation(): Boolean {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        return resId > 0 && resources.getInteger(resId) == 2
    }

    fun Payload.toUserDetailsX(): UserDetailsX {
        return UserDetailsX(
            id = this.userId,
            name = if (this.name?.isNotEmpty() == true && this.name != "") this.name else this.fullName,
            username = this.username,
            email = this.email,
            phone = this.phone,
            dob = this.dob,
            city = this.city,
            country = this.country,
            about = null,   // Payload में नहीं है

            profileImage = this.profileImage,
            profile_image = this.profileImage, // अगर दोनों चाहिए तो same assign कर दो

            isCreator = this.isCreator,
            role = if (this.isCreator == 1) "Creator" else "User", // optional mapping

            // बाकी fields Payload में नहीं हैं तो default null/0 रहेंगे
            followers_count = null,
            following_count = null,
            gender = null,
            interest_name = null,
            is_verified = null,
            posts_count = null,
            blockStatus = null,
            reels_count = null,
            is_following = null,
            is_seen = null,
            user_live_status = null,
            creatorStatus = null,
            isStoryUploaded = null,
            is_story_uploaded = null,
            story = null,
            myStory = null,
            notificationSettings = null
        )
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}