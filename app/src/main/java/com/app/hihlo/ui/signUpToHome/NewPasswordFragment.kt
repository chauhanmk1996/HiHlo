package com.app.hihlo.ui.signUpToHome

import android.text.method.HideReturnsTransformationMethod
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentNewPasswordBinding
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.getLength
import kotlin.getValue

class NewPasswordFragment :
    BaseNewFragment<FragmentNewPasswordBinding, SignUpToHomeViewModel>(R.layout.fragment_new_password) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var isPassHidden = true
    private var isCnfPassHidden = true
    private var data: SignUpRequest? = null

    override fun onInitDataBinding(viewBinding: FragmentNewPasswordBinding) {
        arguments?.let {
            data = it.getParcelable("data")
        }
        observer()
        setPasswordToggle(viewBinding)
        onClick(viewBinding)
    }

    private fun observer() {
        mViewModel.resetPasswordResponse.observe(this) {
            if (it?.peekContent() != null) {
                showToast(it.peekContent().message ?: "")
                findNavController().navigate(
                    R.id.signinFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.signup_flow_nav, true).build()
                )
                mViewModel.resetPasswordResponse.value = null
            }
        }
    }

    private fun setPasswordToggle(viewBinding: FragmentNewPasswordBinding) {
        viewBinding.apply {
            etNewPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
            ivNewPasswordToggle.setOnClickListener {
                isPassHidden = if (isPassHidden) {
                    ivNewPasswordToggle.setImageResource(R.drawable.open_eye_2)
                    etNewPassword.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()
                    false
                } else {
                    ivNewPasswordToggle.setImageResource(R.drawable.close_eye_2)
                    etNewPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
                    true
                }
                etNewPassword.setSelection(etNewPassword.getLength())
            }

            etConfirmPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
            ivConfirmPasswordToggle.setOnClickListener {
                isCnfPassHidden = if (isCnfPassHidden) {
                    ivConfirmPasswordToggle.setImageResource(R.drawable.open_eye_2)
                    etConfirmPassword.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()
                    false
                } else {
                    ivConfirmPasswordToggle.setImageResource(R.drawable.close_eye_2)
                    etConfirmPassword.transformationMethod =
                        CommonUtils.DotPasswordTransformationMethod
                    true
                }
                etConfirmPassword.setSelection(etConfirmPassword.getLength())
            }
        }
    }

    private fun onClick(viewBinding: FragmentNewPasswordBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            btnChangePassword.setOnClickListener {
                mViewModel.resetPasswordApi()
            }
        }
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}