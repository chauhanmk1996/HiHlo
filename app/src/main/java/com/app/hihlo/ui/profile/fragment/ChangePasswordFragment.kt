package com.app.hihlo.ui.profile.fragment

import android.text.method.HideReturnsTransformationMethod
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentChangePasswordBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.view_model.SettingViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.getLength
import kotlin.getValue

class ChangePasswordFragment :
    BaseNewFragment<FragmentChangePasswordBinding, SettingViewModel>(R.layout.fragment_change_password) {

    val mViewModel: SettingViewModel by activityViewModels()
    private var isOldPassHidden = true
    private var isNewPassHidden = true
    private var isConfirmPassHidden = true

    override fun onInitDataBinding(viewBinding: FragmentChangePasswordBinding) {
        observer()
        setPasswordToggle(viewBinding)
        onClick(viewBinding)
    }

    private fun observer() {
        mViewModel.changePasswordResponse.observe(this) {
            if (it?.peekContent() != null) {
                showToast(it.peekContent().message ?: "")
                findNavController().popBackStack()
                mViewModel.changePasswordResponse.value = null
            }
        }
    }

    private fun setPasswordToggle(viewBinding: FragmentChangePasswordBinding) {
        viewBinding.apply {
            etOldPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
            ivOldPasswordToggle.setOnClickListener {
                isOldPassHidden = if (isOldPassHidden) {
                    ivOldPasswordToggle.setImageResource(R.drawable.open_eye_2)
                    etOldPassword.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()
                    false
                } else {
                    ivOldPasswordToggle.setImageResource(R.drawable.close_eye_2)
                    etOldPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
                    true
                }
                etOldPassword.setSelection(etOldPassword.getLength())
            }

            etNewPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
            ivNewPasswordToggle.setOnClickListener {
                isNewPassHidden = if (isNewPassHidden) {
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
                isConfirmPassHidden = if (isConfirmPassHidden) {
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

    private fun onClick(viewBinding: FragmentChangePasswordBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            btnChangePassword.setOnClickListener {
                val token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA
                )?.payload?.authToken
                mViewModel.changePasswordApi(token)
            }
        }
    }

    override fun getViewModel(): SettingViewModel {
        return mViewModel
    }
}