package com.app.hihlo.ui.reels.bottom_sheet

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.BottomSheetBlockFlagBinding
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.flag_user.request.FlagUserRequest
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.model.DeleteAccountRequest
import com.app.hihlo.ui.reels.adapter.AdapterBlockFlag
import com.app.hihlo.ui.reels.view_model.BlockFlagViewModel
import com.app.hihlo.ui.signUpToHome.SignupFlowActivity
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.CommonUtils.toPx
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.gson.Gson

class BlockFlagBottomSheet  : BottomSheetDialogFragment() {
    private var screen: String = ""
    private var userId: String = ""
    private var reasonId: String = ""
    private var _binding: BottomSheetBlockFlagBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BlockFlagViewModel by viewModels()
    var onBlockSuccessful: (() -> Unit)? = null


    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBlockFlagBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                // Make bottom sheet fill the screen height
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                it.requestLayout()

                // Configure behavior to open fully
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = false
                behavior.expandedOffset = 0 // full expanded height (0px from top)

                // 3. Optional: Set rounded corners
                val cornerRadius = 25f.toPx(requireContext())
                val shapeDrawable = MaterialShapeDrawable(
                    ShapeAppearanceModel.Builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                        .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                        .build()
                )
                shapeDrawable.fillColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.screen_background_black))
                it.background = shapeDrawable

            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        onClick()
        // Retrieve arguments from the bundle
        screen = arguments?.getString("screen") ?: ""
        userId = arguments?.getString("userId") ?: ""
        when(screen){
            "block"->{
                binding.title.text = "Block"
                viewModel.hitGetBlockReasonsApi()
            }
            "flag"->{
                binding.title.text = "Report"
                viewModel.hitGetFlagReasonsApi()
            }
            "delete_account" ->{
                binding.title.text = "Reason to Delete Account"
                viewModel.hitDeleteAccountReason()
            }
        }
    }

    private fun onClick() {
        binding.submitButton.setOnClickListener {
            if (reasonId.isEmpty()){
                Toast.makeText(requireContext(), "Please select a reason!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when(screen){
                "block"->{
                    viewModel.hitBlockUserApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, BlockUserRequest(blockReasonId = reasonId, blockedUserId = userId ))
                }
                "flag"->{
                    viewModel.hitFlagUserApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, FlagUserRequest(flagReasonId = reasonId, reelId = userId ))
                }
                "delete_account"->{
                    showCustomDialogWithBinding(requireContext(), "Are You Sure ?",
                        onYes = {
                            val model = DeleteAccountRequest(
                                reasonId = reasonId
                            )
                            viewModel.hitDeleteAccount("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,model)
                        },
                        onNo = {
                            //dismiss()
                        }
                    )
                }
            }
        }
        binding.backButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setObserver() {
        viewModel.getBlockReasonsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.listingRecycler.adapter = AdapterBlockFlag(it.data.payload.blockReasons, screen){   reasonId ->
                                this.reasonId = reasonId
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
        viewModel.getFlagReasonsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.listingRecycler.adapter = AdapterBlockFlag(it.data.payload.flagReasons, screen){   reasonId ->
                                this.reasonId = reasonId
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
        viewModel.getBlockUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Block User success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        RTVariable.isBlocked = true
                        onBlockSuccessful?.invoke()
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                    dismiss()
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                    dismiss()
                }
            }
        }
        viewModel.getFlagUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Flag user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        RTVariable.isBlocked = true
                        onBlockSuccessful?.invoke()
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                    dismiss()
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                    dismiss()
                }
            }
        }
        viewModel.getDeleteAccountReasonLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Flag user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        binding.listingRecycler.adapter = AdapterBlockFlag(it.data.payload.deleteReasons, screen){   reasonId ->
                            this.reasonId = reasonId
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
                    dismiss()
                }
            }
        }
        viewModel.getDeleteAccountLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Flag user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        /*if (context != null && activity != null && !requireActivity().isFinishing){
                            showCustomDialogWithBinding(requireContext(), "Account Deleted \nSuccessfully!",
                                onYes = {},
                                onNo = {},
                                showButtons = false,
                                autoDismissInMillis = 1000
                            )
                        }*/
                        performLogout()
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
                    dismiss()
                }
            }
        }
    }

    fun performLogout() {
        Preferences.removeAllPreferencesExcept(requireContext(), listOf(FCM_TOKEN))
        val intent = Intent(requireContext(), SignupFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}