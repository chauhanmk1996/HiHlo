package com.app.hihlo.ui.signUpToHome

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentOtpBinding
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.hide
import com.app.hihlo.utils.show

class OtpFragment :
    BaseNewFragment<FragmentOtpBinding, SignUpToHomeViewModel>(R.layout.fragment_otp) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var from = ""
    private var purpose = ""
    private var signUpData: SignUpRequest? = null

    override fun onInitDataBinding(viewBinding: FragmentOtpBinding) {
        arguments?.let {
            from = it.getString("from").toString()
            signUpData = it.getParcelable("data")
            purpose = it.getString("purpose").toString()

            mViewModel.mEmailIdLiveData.value = signUpData?.email ?: ""
            mViewModel.mUserNameLiveData.value = signUpData?.username ?: ""
        }
        setUpObserver(viewBinding)
        resendTimer(viewBinding)
        otpListener(viewBinding)
        onClick(viewBinding)
    }

    private fun setUpObserver(viewBinding: FragmentOtpBinding) {
        mViewModel.sendEmailOtpResponse.observe(this) {
            if (it?.peekContent() != null) {
                showToast(it.peekContent().message ?: "")
                resendTimer(viewBinding)
                mViewModel.sendEmailOtpResponse.value = null
            }
        }

        mViewModel.verifyEmailOtpResponse.observe(this) {
            if (it?.peekContent() != null) {
                showToast(it.peekContent().message ?: "")
                val model = SignUpRequest(
                    name = signUpData?.name ?: "",
                    email = signUpData?.email ?: "",
                    username = signUpData?.username ?: "",
                    phoneNumber = signUpData?.phoneNumber ?: "",
                    deviceType = "A",
                    password = signUpData?.password ?: "",
                    confirmPassword = signUpData?.password ?: "",
                    deviceToken = Preferences.getStringPreference(requireActivity(), FCM_TOKEN),
                )
                val bundle = Bundle()
                bundle.putString("from", from)
                bundle.putParcelable("data", model)
                if (from == "forgot_password") {
                    findNavController().navigate(
                        R.id.newPasswordFragment,
                        NavOptions.Builder().setPopUpTo(R.id.otpFragment, true).build()
                    )
                } else {
                    findNavController().navigate(
                        R.id.addDetailsFragment,
                        bundle,
                        NavOptions.Builder().setPopUpTo(R.id.otpFragment, true).build()
                    )
                }
                mViewModel.verifyEmailOtpResponse.value = null
            }
        }
    }

    private fun resendTimer(viewBinding: FragmentOtpBinding) {
        viewBinding.underline.hide()
        val countDownTimer: CountDownTimer = object : CountDownTimer(90000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondLeft = millisUntilFinished / 1000
                val secLeftText: String = if (secondLeft < 10) {
                    "00:0$secondLeft"
                } else {
                    "00:$secondLeft"
                }
                mViewModel.resendOtpText.value = secLeftText
            }

            override fun onFinish() {
                mViewModel.resendOtpText.value = getString(R.string.resend)
                viewBinding.underline.show()
            }
        }
        countDownTimer.start()
    }

    private fun otpListener(viewBinding: FragmentOtpBinding) {
        viewBinding.otp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.length == 4) {
                        CommonUtils.hideKeyboard(requireActivity())
                    }
                    mViewModel.mOtpLiveData.value = s.toString()
                }
            }
        })
    }

    private fun onClick(viewBinding: FragmentOtpBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            btnConfirm.setOnClickListener {
                mViewModel.verifyEmailOtpApi()
            }

            tvResend.setOnClickListener {
                if (mViewModel.resendOtpText.value == getString(R.string.resend)) {
                    mViewModel.sendEmailApi(purpose)
                }
            }
        }
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}