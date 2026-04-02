package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentBecomeCreatorStatusBinding
import com.app.hihlo.databinding.FragmentProfileSettingBinding
import com.app.hihlo.ui.home.activity.HomeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BecomeCreatorStatusFragment : Fragment() {
    private lateinit var binding: FragmentBecomeCreatorStatusBinding
    private var status = ""
    val navArgs : BecomeCreatorStatusFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBecomeCreatorStatusBinding.inflate(layoutInflater)
        status = navArgs.status
        (requireContext() as HomeActivity).setOnlineStatusVisibility(true)
        initViews()
        onClick()
        return binding.root
    }

    private fun onClick() {
        binding.exitButton.setOnClickListener {
            (requireContext() as HomeActivity).setOnlineStatusVisibility(false)
            findNavController().popBackStack()
        }
    }

    private fun initViews() {
        binding.apply {
            when(status){
                "Accepted"->{
                    verifiedLayout.isVisible=true
                    underReviewLayout.isVisible=false
                }
                "Pending"->{
                    verifiedLayout.isVisible=false
                    underReviewLayout.isVisible=true
                }
            }
        }
    }

}