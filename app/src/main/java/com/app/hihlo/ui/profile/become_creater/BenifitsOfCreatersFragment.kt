package com.app.hihlo.ui.profile.become_creater

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentBenifitsOfCreatersBinding
import com.app.hihlo.model.get_profile.UserDetails
import com.app.hihlo.ui.profile.adapter.BeniftsAdapter
import com.app.hihlo.ui.profile.become_creater.view_model.CreatorsBenefitsViewModel
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class BenifitsOfCreatersFragment : Fragment() {
    private lateinit var binding: FragmentBenifitsOfCreatersBinding
    private lateinit var beniftsAdapter: BeniftsAdapter
    private val viewModel: CreatorsBenefitsViewModel by viewModels()

    var data = mutableListOf<UserDetails>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBenifitsOfCreatersBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    private fun initViews() {
        loadRcv()
        binding.nexrButton.setOnClickListener {
//            findNavController().navigate(R.id.addYourPhotoFragment)
            findNavController().navigate(R.id.addPhoneNumberFragment)
        }
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        hitCreatorBenefitsApi()
    }

    private fun hitCreatorBenefitsApi() {
        viewModel.hitCreatorsBenefits()
        viewModel.getCreatorBenefitsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            val list = it.data.payload?.benefitsList
                            beniftsAdapter.setData(list)
                            Log.e("TAG", "hitCreatorBenefitsApi: $list", )
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

    private fun loadRcv() {
        binding.rcvBenifits.layoutManager = LinearLayoutManager(requireActivity(),
            LinearLayoutManager.VERTICAL,false)
        beniftsAdapter = BeniftsAdapter(requireActivity())
        binding.rcvBenifits.adapter = beniftsAdapter
    }
}