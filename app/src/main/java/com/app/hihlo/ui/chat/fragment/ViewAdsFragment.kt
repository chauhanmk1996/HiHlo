package com.app.hihlo.ui.chat.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentViewAdsBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.chat.adapter.AdapterAdsList
import com.app.hihlo.ui.chat.view_model.ViewAdsViewModel
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class ViewAdsFragment : Fragment() {
    private lateinit var binding: FragmentViewAdsBinding
    private val viewModel: ViewAdsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewAdsBinding.inflate(layoutInflater)
        onClick()
        return binding.root
    }
    fun onClick(){
        binding.apply {
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        viewModel.hitViewAdsApi( "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
    }
    private fun setObserver() {
        viewModel.getViewAdsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get ads success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                        if (it.data.payload.ads.isNotEmpty()){
                            binding.noAdsFoundPlaceholder.isVisible = false
                            binding.adsListRecycler.adapter = AdapterAdsList(it.data.payload.ads){ ad ->
                                val bundle = Bundle().apply {
                                    putString("mediaUrl", ad.imageUrl)
                                    putString("adId", ad.id.toString())
                                }
                                findNavController().navigate(R.id.openAdFragment, bundle)
                            }
                        }else{
                            binding.noAdsFoundPlaceholder.isVisible = true
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