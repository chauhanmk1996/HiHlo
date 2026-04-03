package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentChangePasswordBinding
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.signup.model.ChangePasswordRequest
import com.app.hihlo.ui.signup.view_model.ResetPasswordViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.setupUIToHideKeyboard
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class ChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private var isOldPassHidden = true
    private var isPassHidden = true
    private var isCnfPassHidden = true
    private val resetPasswordViewModel: ResetPasswordViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePasswordBinding.inflate(layoutInflater)
        initViews()
        (requireContext() as HomeActivity).setOnlineStatusVisibility(true)
        return binding.root
    }

    private fun initViews() {
        setPasswordToggle()
        setupUIToHideKeyboard(binding.root, requireActivity())
        binding.backButton.setOnClickListener {
            (requireContext() as HomeActivity).setOnlineStatusVisibility(false)
            findNavController().popBackStack()
        }
        binding.btnChangePassword.setOnClickListener {
            checkValidation()
        }
    }

    private fun checkValidation() {
        val oldPassword = binding.oldPassword.text.trim().toString()
        val newPassword = binding.password.text.trim().toString()
        val cnfNewPassword = binding.etCnfpassword.text.trim().toString()
        if (oldPassword.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter old password", Toast.LENGTH_SHORT)
                .show()
        } else if (newPassword.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter new password", Toast.LENGTH_SHORT)
                .show()
        } else if (cnfNewPassword.isEmpty()) {
            Toast.makeText(
                requireActivity(),
                "Please enter confirm new password",
                Toast.LENGTH_SHORT
            ).show()
        } else if (newPassword != cnfNewPassword) {
            Toast.makeText(
                requireActivity(),
                "New password and confirm password not matched",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val model = ChangePasswordRequest(
                oldPassword = oldPassword,
                newPassword = newPassword,
                confirmedNewPassword = cnfNewPassword
            )
            hitChangePasswordApi(model)
        }
    }

    private fun hitChangePasswordApi(model: ChangePasswordRequest) {
        resetPasswordViewModel.hitChangePassword(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, model
        )
        resetPasswordViewModel.getChangePasswordLiveDataLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "change pass success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                            findNavController().popBackStack()
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                    ProcessDialog.dismissDialog(true)
                }

                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    private fun setPasswordToggle() {

        //for old password
        binding.oldPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        binding.oldPasswordToggle.setOnClickListener {
            isOldPassHidden = if (isOldPassHidden) {
                binding.oldPasswordToggle.setImageResource(R.drawable.open_eye_2)
                binding.oldPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                binding.oldPasswordToggle.setImageResource(R.drawable.close_eye_2)
                binding.oldPassword.transformationMethod =
                    CommonUtils.DotPasswordTransformationMethod
                true
            }
            binding.oldPassword.setSelection(binding.oldPassword.text.toString().length)
        }

        //for new Password
        binding.password.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        binding.passwordToggle.setOnClickListener {
            isPassHidden = if (isPassHidden) {
                binding.passwordToggle.setImageResource(R.drawable.open_eye_2)
                binding.password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                binding.passwordToggle.setImageResource(R.drawable.close_eye_2)
                binding.password.transformationMethod = CommonUtils.DotPasswordTransformationMethod
                true
            }
            binding.password.setSelection(binding.password.text.toString().length)
        }


        //for confirm new password
        binding.etCnfpassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        binding.cnfPwdToggle.setOnClickListener {
            isCnfPassHidden = if (isCnfPassHidden) {
                binding.cnfPwdToggle.setImageResource(R.drawable.open_eye_2)
                binding.etCnfpassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                binding.cnfPwdToggle.setImageResource(R.drawable.close_eye_2)
                binding.etCnfpassword.transformationMethod =
                    CommonUtils.DotPasswordTransformationMethod
                true
            }
            binding.etCnfpassword.setSelection(binding.etCnfpassword.text.toString().length)
        }


    }
}