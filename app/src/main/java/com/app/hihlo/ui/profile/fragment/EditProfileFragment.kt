package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentEditProfileBinding
import com.app.hihlo.model.get_profile.UserDetails
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.static.editProfileColumnsList
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.adapter.AdapterEditProfile
import com.app.hihlo.ui.profile.view_model.EditProfileViewModel
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileFragment  : BaseFragment<FragmentEditProfileBinding>() {
    private lateinit var adapter: AdapterEditProfile
    private var userDetails = UserDetailsX()
    private val args: EditProfileFragmentArgs by navArgs()
    private val viewModel: EditProfileViewModel by viewModels()

    override fun initView(savedInstanceState: Bundle?) {
        userDetails = args.userDetails

        Log.i("TAG", "initView: "+userDetails)
        adapter = AdapterEditProfile(editProfileColumnsList, userDetails){ key ->
                when (key) {
                    "name" -> {
                        // Handle name update
                    }
                    "username" -> {
                        // Handle username update
                    }
                    "phone" -> {
                        // Handle phone update
                    }
                    "dob" -> {
                        // Handle DOB update
                    }
                    "email" -> {
                        // Handle email update
                    }
                    "city" -> {
                        // Handle city update
                    }
                    "country" -> {
                        // Handle country update
                    }
                    "interestId" -> {
                        // Handle topic/interest update
                    }
                    "about" -> {
                        // Handle about update
                    }
                    else -> {
                        // Unknown key - handle gracefully
                    }
                }

        }
        binding.editProfileRecycler.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        setObserver()
    }

    private fun onClick() {
        binding.updateButton.setOnClickListener {
//            val editProfileRequest = adapter.collectEditProfileRequest()
//            Log.i("TAG", "onClick: "+editProfileRequest)
      //      viewModel.hitEditProfileDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, editProfileRequest)
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setObserver() {
        viewModel.getEditProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            findNavController().popBackStack()


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
        viewModel.getInterestListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "interest list success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){



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


        viewModel.getCitiesListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "interest list success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){



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

    override fun getLayoutId(): Int {
        return R.layout.fragment_edit_profile
    }

}