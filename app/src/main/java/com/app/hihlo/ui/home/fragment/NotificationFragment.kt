package com.app.hihlo.ui.home.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentNotificationBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.notification.response.GetNotificationListResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.adapter.AdapterNotification
import com.app.hihlo.ui.home.view_model.NotificationViewModel
import com.app.hihlo.ui.profile.activity.RechargeCoinsActivity
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson

class NotificationFragment : BaseFragment<FragmentNotificationBinding>() {
    private val viewModel: NotificationViewModel by viewModels()
    lateinit var adapter: AdapterNotification
    var notificationList: MutableList<GetNotificationListResponse.Payload> = mutableListOf()
    private var isLoading = false
    private var currentPage=1

    override fun initView(savedInstanceState: Bundle?) {
        adapter = AdapterNotification(mutableListOf(), ::getSelectedItem)
        binding.notificationRecycler.adapter = adapter
        hitNotificationListApi(currentPage)
        viewModel.hitReadNotificationDataApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
        setPagination()
        onClick()
    }
    private fun getSelectedItem(data:GetNotificationListResponse.Payload){
        when (data.notification_type) {
            "FOLLOW", "REEL_LIKE", "COMMENT", "COMMENT_REPLY", "POST_LIKE" -> {
                findNavController().navigate(R.id.profileFragment)
            }
            "GIFT_RECEIVED"-> {
                startActivity(Intent(requireContext(), RechargeCoinsActivity::class.java))
//                UserPreference.CHAT_PUSH_NOTIFICATION_ID = data.user_id.toString()
//                findNavController().navigate(R.id.chatListFragment)
            }
        }
    }
    private fun setPagination() {
        binding.notificationRecycler.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (layoutManager?.findLastVisibleItemPosition() == adapter.itemCount - 2) {
                    if (isLoading) {
                        currentPage++
                        hitNotificationListApi(currentPage)
                    }
                    isLoading = false
                }
            }
        })
    }
    private fun hitNotificationListApi(page: Int) {
        viewModel.hitNotificationListDataApi(("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken),"15", page.toString())
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }

    private fun setObserver() {
        viewModel.getNotificationLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", " success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            if (it.data.payload.isNotEmpty() == true) {
                                binding.noNotificationFoundPlaceholder.isVisible=false
                                isLoading=true
                                notificationList.addAll(it.data.payload)
                                if (currentPage == 1 ) {
                                    adapter.updateCompleteNewList(it.data.payload.toMutableList())
                                } else {
                                    adapter.updateList(it.data.payload.toMutableList())
                                }
                            } else{
                                if (currentPage==1) {
                                    binding.notificationRecycler.adapter = AdapterNotification(mutableListOf(), ::getSelectedItem)
                                    binding.noNotificationFoundPlaceholder.isVisible=true
                                    binding.deleteButton.isVisible=false
                                }
                            }
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
        viewModel.getReadNotificationLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "read notification success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
//                            binding.notificationRecycler.adapter = AdapterNotification(it.data.payload)
                        }else{
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
//                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.getDeleteNotificationLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "read notification success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.notificationRecycler.adapter = AdapterNotification(mutableListOf(), ::getSelectedItem)
                            binding.noNotificationFoundPlaceholder.isVisible=true
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    private fun onClick() {
        binding.apply {
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }
            deleteButton.setOnClickListener {
                openDeleteCompleteConfirmationDialog()
            }
        }
    }

    fun openDeleteCompleteConfirmationDialog() {
        showCustomDialogWithBinding(requireContext(), "Do you want to Delete All Notifications?",
            onYes = {
                viewModel.hitDeleteNotificationDataApi("Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
            },
            onNo = {
                //dismiss()
            }
        )
    }override fun getLayoutId(): Int {
        return R.layout.fragment_notification
    }

}