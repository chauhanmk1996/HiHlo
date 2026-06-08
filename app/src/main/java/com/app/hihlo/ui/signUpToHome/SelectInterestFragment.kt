package com.app.hihlo.ui.signUpToHome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentSelectInterestBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.interest_list.response.Interests
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.CommonUtils
import kotlin.getValue

class SelectInterestFragment :
    BaseNewFragment<FragmentSelectInterestBinding, SignUpToHomeViewModel>(R.layout.fragment_select_interest) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    var signUpRequest: SignUpRequest? = null
    private lateinit var interestAdapter: InterestAdapter
    private var interestList: ArrayList<Interests> = ArrayList()
    private var selectedPosition: Int = -1

    override fun onInitDataBinding(viewBinding: FragmentSelectInterestBinding) {
        arguments?.let {
            signUpRequest = it.getParcelable("data")
        }
        setUpInterestAdapter(viewBinding)
        observer()
        mViewModel.getInterestListApi()
        onClick(viewBinding)
    }

    private fun observer() {
        mViewModel.interestListResponse.observe(this) {
            if (it?.peekContent() != null) {
                it.peekContent().payload?.rows?.let { list ->
                    interestList.clear()
                    interestList.addAll(list)
                    interestAdapter.addList(interestList)
                }
                mViewModel.interestListResponse.value = null
            }
        }

        mViewModel.registerUserResponse.observe(this) {
            if (it?.peekContent() != null) {
                Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                Preferences.setCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA,
                    it.peekContent()
                )
                CommonUtils.hideKeyboard(requireActivity())
                Log.i(
                    "TAG", "setObserver: " + Preferences.getStringPreference(
                        requireContext(),
                        FCM_TOKEN
                    )
                )
                if (it.peekContent().payload?.city.isNullOrBlank() || it.peekContent().payload?.profileImage.isNullOrEmpty()) {
                    val bundle = Bundle()
                    val userDetails = it.peekContent().payload?.toUserDetailsX()
                    bundle.putString("from", "normal")
                    bundle.putParcelable("userDetail", userDetails)
                    findNavController().navigate(R.id.editProfileNewFragment, bundle)
                } else {
                    startActivity(Intent(requireActivity(), HomeActivity::class.java))
                    requireActivity().finish()
                }
                mViewModel.registerUserResponse.value = null
            }
        }
    }

    fun LoginPayload.toUserDetailsX(): UserDetailsX {
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

    private fun setUpInterestAdapter(viewBinding: FragmentSelectInterestBinding) {
        interestAdapter = InterestAdapter { pos ->
            selectedPosition = pos
            interestAdapter.updateSelectedPosition(pos)
        }
        viewBinding.rvInterest.adapter = interestAdapter
    }

    private fun onClick(viewBinding: FragmentSelectInterestBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            btnSubmit.setOnClickListener {
                if (selectedPosition == -1) {
                    showToast(getString(R.string.please_select_your_interest))
                } else {
                    val signUpRequest = SignUpRequest(
                        name = signUpRequest?.name,
                        username = signUpRequest?.username,
                        email = signUpRequest?.email,
                        phoneNumber = signUpRequest?.phoneNumber,
                        gender_id = signUpRequest?.gender_id,
                        dob = signUpRequest?.dob,
                        password = signUpRequest?.password,
                        deviceType = signUpRequest?.deviceType,
                        deviceToken = signUpRequest?.deviceToken,
                        confirmPassword = signUpRequest?.password,
                        city = signUpRequest?.city,
                        interest_id = (interestList[selectedPosition].id ?: 0).toString()
                    )
                    mViewModel.registerUserApi(signUpRequest)
                }
            }
        }
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}