package com.app.hihlo.ui.signUpToHome

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentForgotPasswordBinding
import kotlin.getValue

class ForgotPasswordFragment :
    BaseNewFragment<FragmentForgotPasswordBinding, SignUpToHomeViewModel>(R.layout.fragment_forgot_password) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var from = ""

    override fun onInitDataBinding(viewBinding: FragmentForgotPasswordBinding) {
        mViewModel.mEmailIdLiveData.value = ""
        arguments?.let {
            from = it.getString("from").toString()
        }
        observer()
        onclick(viewBinding)
    }

    private fun observer() {
        mViewModel.forgotPasswordResponse.observe(this) {
            if (it?.peekContent() != null) {
                showToast(it.peekContent().message ?: "")
                val data = SignUpRequest(
                    email = mViewModel.mEmailIdLiveData.value ?: ""
                )
                val bundle = Bundle()
                bundle.putString("from", from)
                bundle.putString("purpose", "forgot_password")
                bundle.putParcelable("data", data)
                findNavController().navigate(R.id.otpFragment, bundle)
                mViewModel.forgotPasswordResponse.value = null
            }
        }
    }

    private fun onclick(viewBinding: FragmentForgotPasswordBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            btnResetPassword.setOnClickListener {
                mViewModel.forgotPasswordApi()
            }
        }
    }


    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}