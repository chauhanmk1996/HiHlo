package com.app.hihlo.ui.profile.become_creater

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentAddPhoneNumberBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.become_creater.model.SendOtpPhoneRequest
import com.app.hihlo.ui.profile.become_creater.view_model.AddPhoneNumberViewModel
import com.app.hihlo.ui.profile.become_creater.view_model.CreatorsBenefitsViewModel
import com.app.hihlo.ui.profile.model.ImageItem
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.CommonUtils.touchHideKeyBoard
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class AddPhoneNumberFragment : Fragment() {
   private lateinit var binding: FragmentAddPhoneNumberBinding
   private var videoFile = ""
    private var imageList: ArrayList<ImageItem>?=null
    private val viewModel: AddPhoneNumberViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoFile = it.getString("videoUri").toString()
            imageList =it.getParcelableArrayList<ImageItem>("image_list")
            Log.e("TAG", "onCreate: $videoFile \n $imageList", )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding = FragmentAddPhoneNumberBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    private fun initViews() {
        touchHideKeyBoard(binding.root, requireActivity())
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnSubmit.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text
            if(phoneNumber.isNullOrEmpty()){
                Toast.makeText(requireActivity(), "Phone number can't be empty ", Toast.LENGTH_SHORT).show()
            }else if(phoneNumber.length != 10){
                Toast.makeText(requireActivity(), "Please enter valid phone number", Toast.LENGTH_SHORT).show()

            }else{
                hitSendOtpPhoneNumberApi()
            }

        }

        keyBoardClose()

    }
    fun keyBoardClose() {
        setupUI(binding.root)
    }
    private fun setupUI(view: View) {
        // Set up touch listener for non-EditText views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                false
            }
        }

        // If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireActivity())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }



    private fun hitSendOtpPhoneNumberApi(){
        val phoneNumber = binding.etPhoneNumber.text.toString()
        val model = SendOtpPhoneRequest(
            phone = phoneNumber
        )
        viewModel.hitAddPhoneNumber("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(),LOGIN_DATA)?.payload?.authToken,model)
        viewModel.getAddPhoneNumberLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Flag user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message +"${it.data.payload?.otp}", Toast.LENGTH_SHORT).show()
                        val bundle = Bundle()
                        bundle.putString("videoUri",videoFile)
                        bundle.putParcelableArrayList("image_list",imageList)
                        bundle.putString("phone",model.phone)
                        findNavController().navigate(R.id.phoneOtpFragment,bundle)

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