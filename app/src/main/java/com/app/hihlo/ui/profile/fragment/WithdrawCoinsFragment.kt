package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.databinding.FragmentWithdrawCoinsBinding
import com.app.hihlo.model.contact_us.request.ContactUsRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.view_model.ContactUsViewModel
import com.app.hihlo.ui.profile.view_model.WithdrawCoinsViewModel
import com.app.hihlo.ui.signUpToHome.LoginPayload
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson

class WithdrawCoinsFragment : Fragment() {
    private var totalCoins: Int? = 0
    private lateinit var binding: FragmentWithdrawCoinsBinding
    private val viewModel: WithdrawCoinsViewModel by viewModels()
    private var screenShotUrl=""
    var loginResponse: LoginPayload?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWithdrawCoinsBinding.inflate(layoutInflater)
        totalCoins = arguments?.getInt("totalCoins")
        loginResponse = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload ?: LoginPayload()
        initViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }
    private fun setObserver() {
        viewModel.getWithdrawCoinsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "withdraw history success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()

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

    private fun initViews() {
        binding.apply {
            totalCoins.text = this@WithdrawCoinsFragment.totalCoins.toString()
            llBack.setOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }
            submitButton.setOnClickListener {
                withdrawCoinsPopup()
            }
        }
    }

    private fun withdrawCoinsPopup() {
        totalCoins?.let {
            if ((binding.withdrawCoins.text.toString().toIntOrNull() ?: 0) == 0){
                showCustomDialogWithBinding(requireContext(), "Please enter coins.",
                    showButtons = false,
                    autoDismissInMillis = 1000
                )
            }else if ((binding.withdrawCoins.text.toString().toIntOrNull() ?: 0) <1000  || (binding.withdrawCoins.text.toString().toIntOrNull() ?: 0) <1000){
                showCustomDialogWithBinding(requireContext(), "There should be minimum of 1000 coins to withdraw.",
                    showButtons = false,
                    autoDismissInMillis = 1000
                )
            }else if ((binding.withdrawCoins.text.toString().toIntOrNull() ?: 0) > (totalCoins ?: 0)){
                showCustomDialogWithBinding(requireContext(), "You don't have enough coins to withdraw.",
                    showButtons = false,
                    autoDismissInMillis = 1000
                )
            }
            else{
                showCustomDialogWithBinding(requireContext(), "Are you sure you want to withdraw coins?",
                    onYes = {
                        viewModel.hitWithdrawCoinsDataApi("Bearer "+ loginResponse?.authToken, binding.withdrawCoins.text.toString())
                    },
                    onNo = {},
                    showButtons = true,
                    btnYes = "Withdrawl",
                    btnNo = "Cancel",
                    description = "\uD83E\uDE99 ${binding.withdrawCoins.text}"
                )
            }
        }
    }

}