package com.app.hihlo.ui.profile.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentLogoutDialogBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.view_model.SettingViewModel
import com.app.hihlo.ui.signUpToHome.SignupFlowActivity
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import kotlin.getValue

class LogoutDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding:FragmentLogoutDialogBinding
    private val viewModel: SettingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            // Explicit black nav bar
            window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.black_1c1c1c)

            // Force nav buttons white
            WindowInsetsControllerCompat(window, window.decorView)
                .isAppearanceLightNavigationBars = false
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogoutDialogBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }
    private fun initViews(){
        binding.apply {
            btnCancel.setOnClickListener {
                dismiss()
            }
            btnSignOut.setOnClickListener {
                viewModel.hitLogoutUserDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
            }
        }
    }
    fun performLogout() {
        dismiss()
        Preferences.removeAllPreferencesExcept(requireContext(), listOf(FCM_TOKEN))
        val intent = Intent(requireContext(), SignupFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    private fun setObserver() {
        viewModel.getLogoutUserLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Logout success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            performLogout()
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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

}