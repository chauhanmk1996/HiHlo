package com.app.hihlo.ui.get_started.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentGetStartedBinding
import com.app.hihlo.databinding.FragmentProfileBinding




class GetStartedFragment : BaseFragment<FragmentGetStartedBinding>() {
    override fun initView(savedInstanceState: Bundle?) {
        onClick()
    }

    private fun onClick() {
        binding.apply {
            startedButton.setOnClickListener {
                findNavController().navigate(R.id.action_getStartedFragment_to_signinFragment)
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_get_started
    }

}