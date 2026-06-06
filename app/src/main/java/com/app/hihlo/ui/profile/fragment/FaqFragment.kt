package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentFaqBinding
import com.app.hihlo.ui.profile.adapter.AdapterFaq
import com.app.hihlo.ui.profile.view_model.FaqViewModel
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson



class FaqFragment : BaseFragment<FragmentFaqBinding>() {
    private val viewModel: FaqViewModel by viewModels()
    override fun getLayoutId(): Int {
        return R.layout.fragment_faq
    }

    override fun initView(savedInstanceState: Bundle?) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        viewModel.hitFaqDataApi()
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setObserver() {
        viewModel.getFaqLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.faqRecycler.adapter = AdapterFaq(it.data.payload.faqsList)
                            // binding.followersRecycler.adapter = AdapterFollowers(it.data.payload.followersList, screenCheck)
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