package com.app.hihlo.ui.signUpToHome

import com.app.hihlo.base.BaseNewFragment

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentGetStartedBinding
import kotlin.getValue

class GetStartedFragment :
    BaseNewFragment<FragmentGetStartedBinding, SignUpToHomeViewModel>(R.layout.fragment_get_started) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()

    override fun onInitDataBinding(viewBinding: FragmentGetStartedBinding) {
        viewBinding.btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_getStartedFragment_to_signinFragment)
        }
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}