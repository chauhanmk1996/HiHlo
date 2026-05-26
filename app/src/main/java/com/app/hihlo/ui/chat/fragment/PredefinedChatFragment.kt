package com.app.hihlo.ui.chat.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.databinding.FragmentPredefinedChatBinding
import com.app.hihlo.model.chat.Messages
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.predefined_chats.PredefinedChatsResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.chat.adapter.AdapterPredefinedChat
import com.app.hihlo.ui.chat.view_model.PredefinedChatViewModel
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class PredefinedChatFragment : Fragment() {
    private lateinit var binding: FragmentPredefinedChatBinding
    private val viewModel: PredefinedChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPredefinedChatBinding.inflate(layoutInflater)
        onClick()
        viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
        binding.apply {
            chatEdittext.doAfterTextChanged {
                addButton.isVisible = chatEdittext.getString().isNotEmpty()
            }
        }
        (requireActivity() as? HomeActivity)?.setOnlineStatusVisibility(false)
        return binding.root
    }
    private fun setBottomMargin() {
        val desiredMarginInPx = if ((requireActivity() as HomeActivity).isGestureNavigation()) CommonUtils.dpToPx(0) else CommonUtils.dpToPx(-40)
        val layoutParams = binding.mainLayout.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = desiredMarginInPx
        binding.mainLayout.layoutParams = layoutParams
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private fun onClick() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.addButton.setOnClickListener {
            if (binding.chatEdittext.getString().isEmpty()){
                Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
            }else{
                viewModel.hitAddPredefinedChatApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, binding.chatEdittext.text.toString())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        setBottomMargin()
    }

    private fun setObserver() {
        viewModel.getPredefinedChatOfUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
//                        binding.predefinedTextRecycler.adapter = AdapterPredefinedChat(it.data.payload, ::onLongTap){ it ->
//                            setFragmentResult("my_request_key", bundleOf("my_result_key" to it))
//                            findNavController().popBackStack()
//                        }
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

        viewModel.getAddPredefinedChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "add predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        CommonUtils.hideKeyboard(requireActivity())
                        binding.chatEdittext.setText("")
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
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

        viewModel.getDeletePredefinedChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
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
    private fun onLongTap(message:PredefinedChatsResponse.Payload){
        Log.i("TAG", "onLongTap: "+message)
//        if (listType== toggleChatsType[1]){
        openDeleteMessageConfirmationDialog(message)
    }
    fun openDeleteMessageConfirmationDialog(message: PredefinedChatsResponse.Payload) {
        showCustomDialogWithBinding(requireContext(), "Are you sure you want to delete the message?",
            onYes = {
                viewModel.hitDeletePredefinedChatApi(token =  "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, message.id.toString())
            },
            onNo = {
                //dismiss()
            }
        )
    }
}