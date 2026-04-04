package com.app.hihlo.ui.profile.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentProfileSettingBinding
import com.app.hihlo.model.get_notification_setting.response.GetNotificationSettingResponse
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.set_notification.SetNotificationRequest
import com.app.hihlo.model.static.NotificationsTypeModel
import com.app.hihlo.model.static.audioCallCoinsList
import com.app.hihlo.model.static.videoCallCoinsList
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.LOGIN_TYPE
import com.app.hihlo.preferences.NOTIFICATION_TOGGLE
import com.app.hihlo.preferences.ONLINE_STATUS
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.profile.activity.RechargeCoinsActivity
import com.app.hihlo.ui.profile.model.ScrollViewModel
import com.app.hihlo.ui.profile.view_model.GetProfileViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils.dpToPx
import com.app.hihlo.utils.MyApplication.Companion.appContext
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.github.tomeees.scrollpicker.ScrollPicker
import com.google.gson.Gson

class ProfileSettingFragment : Fragment() {
    private var audioClicked = true
    private var userDetailsMain: UserDetailsX?=null
    private lateinit var binding:FragmentProfileSettingBinding
    private val viewModel: GetProfileViewModel by viewModels()
    private val viewModel2: ScrollViewModel by activityViewModels()
    var isAudioEnable: Boolean?=null
    var isVideoEnable: Boolean?=null
    var selectedLiveStatusId: String?=null
    var scrollYPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userDetails = arguments?.getParcelable<UserDetailsX>("userDetail")
        userDetailsMain = userDetails
        Log.e("TAG", "onCreate: $userDetails", )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileSettingBinding.inflate(layoutInflater)
        initViews()
        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewModel2.scrollPosition = binding.scrollView.scrollY
        }
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, viewModel2.scrollPosition)
        }
        return binding.root
    }

    private fun initViews() {
        val token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString()
        viewModel.hitGetPushNotificationApi(token)
        setUserDetail()
        setObserver()
        setOnlineStatusToggle()
        binding.apply {
            clFaq.setOnClickListener {
                findNavController().navigate(R.id.faqFragment)
            }
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }
            tvUpdate.setOnClickListener {
//                showCustomCoinDialog()
            }
            clCallChargeMain.setOnClickListener {
                showCustomCoinDialog()
                /*if(audioClicked){
                    clAudioTen.visibility = View.VISIBLE
                    tvUpdate.visibility = View.VISIBLE
                    //clVideoCall.visibility = View.VISIBLE
                    ivCallChargeArrow.setImageDrawable(resources.getDrawable(R.drawable.ic_arrow_down))
                    audioClicked = false

                }else{
                    clAudioTen.visibility = View.GONE
                    tvUpdate.visibility = View.GONE

                    // clVideoCall.visibility = View.GONE
                    audioClicked = true
                    ivCallChargeArrow.setImageDrawable(resources.getDrawable(R.drawable.arrow_right))

                }*/
            }
            clLogoutMain.setOnClickListener {
                findNavController().navigate(R.id.logoutDialogFragment)
            }
            clBlockedUserMain.setOnClickListener {
                findNavController().navigate(R.id.blockedUserFragment)
            }
            clNotificationMain.setOnClickListener {
                findNavController().navigate(R.id.notificationSettingFragment)
            }
            clDeleteAccountMain.setOnClickListener {
                findNavController().navigate(R.id.deleteConfirmationFragment)
            }
            clPasswordMain.setOnClickListener {
                findNavController().navigate(R.id.changePasswordFragment)
            }
            clRateUsMain.setOnClickListener {
                findNavController().navigate(R.id.rateUsFragment)
            }
            clContactUs.setOnClickListener {
                findNavController().navigate(R.id.contactUsFragment)
            }
            clAboutMain.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("screen","about")
                findNavController().navigate(R.id.termsConditionsFragment, bundle)
            }
            clWalletMain.setOnClickListener {
                val intent = Intent(requireActivity(), RechargeCoinsActivity::class.java)
                startActivity(intent)
              //  findNavController().navigate(R.id.walletHistoryFragment)
            }
            clShareUs.setOnClickListener {
                shareApp()
            }

        }
    }

    override fun onPause() {
        super.onPause()
        viewModel2.scrollPosition = binding.scrollView.scrollY
    }

    override fun onResume() {
        super.onResume()
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, viewModel2.scrollPosition)
        }
    }

    private fun setObserver() {
        viewModel.getPushNotificationLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isAudioEnable = it.data.payload.audioCall=="1"
                            isVideoEnable = it.data.payload.videoCall=="1"
                        }else{
                        }
                    }else{
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

    private fun shareApp() {
        val appLink = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"

        val shareText = """
        Check out this amazing app!
        Download here: $appLink
    """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share App"))
    }

    private fun setOnlineStatusToggle() {
        binding.apply {
            if (Preferences.getStringPreference(requireContext(), ONLINE_STATUS)=="online"){
                onlineStatusText.text = "Online"
                onlineStatusToggle.isChecked = true
            }else{
                onlineStatusText.text = "Offline"
                onlineStatusToggle.isChecked = false
            }


            onlineStatusToggle.setOnCheckedChangeListener { _, isChecked ->
                val token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString()

                if (isChecked) {
                    Preferences.setStringPreference(requireContext(), ONLINE_STATUS, "online")
                    onlineStatusText.text = "Online"
                    onlineStatusToggle.isChecked = true
                    selectedLiveStatusId = "1"
                    viewModel.hitUpdateOnlineStatusDataApi(token, liveStatusId = 1)
                } else {
                    Preferences.setStringPreference(requireContext(), ONLINE_STATUS, "offline")
                    onlineStatusText.text = "Offline"
                    onlineStatusToggle.isChecked = false
                    selectedLiveStatusId = "2"
                    viewModel.hitUpdateOnlineStatusDataApi(token, liveStatusId = 2)
                }
            }
        }
    }

    private fun setUserDetail() {
        setPasswordLayoutVisibility()
        getProfileApi()
    }

    private fun setPasswordLayoutVisibility() {
        if (Preferences.getStringPreference(requireContext(), LOGIN_TYPE)=="G"){
            binding.clPasswordMain.visibility = View.GONE
        }else{
            binding.clPasswordMain.visibility = View.VISIBLE
        }
    }

    private fun getProfileApi() {
        viewModel.hitProfileDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "1", "10")
        viewModel.getProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                          //  setCountData(it.data.payload.userDetails.posts_count, it.data.payload.userDetails.followers_count, it.data.payload.userDetails.following_count)
                            userDetailsMain = it.data.payload.userDetails
                            setCreatorStatus(userDetailsMain?.creatorStatus)
                            setOnlineStatus(it.data.payload.userDetails.user_live_status ?: "2")

                            binding.apply {
                                tvUserName.text = it.data.payload.userDetails.username
                                tvName.text = it.data.payload.userDetails.name
                                tvLocation.text = it.data.payload.userDetails.city +" ,"+it.data.payload.userDetails?.country
                                Glide.with(requireActivity())
                                    .load(it.data.payload.userDetails.profile_image)
                                    .placeholder(R.drawable.profile_placeholder)
                                    .into(ivUserImage)
                            }
                            viewModel.hitCoinDetailsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)

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
        viewModel.getUpdateOnlineStatusLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            userDetailsMain?.user_live_status = selectedLiveStatusId
                            setOnlineStatus(userDetailsMain?.user_live_status ?: "2")

                        }else{
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
//                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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
        viewModel.getCoinDetailsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.totalCoins.text = it.data.payload.coins.toString()
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
        viewModel.getGetUpdateCoinsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "update coin success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            var loginResponse = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)
                            loginResponse?.payload?.video_call_charges = it.data.payload.updatedCharges.video_call
                            loginResponse?.payload?.audio_call_charges = it.data.payload.updatedCharges.audio_call
                            Preferences.setCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA, loginResponse)

                        }else{
                        }
                    }else{
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
    private fun setOnlineStatus(onlineStatus: String) {
        when(onlineStatus){
            "1"->{
                binding.onlineStatusImage.setImageResource(R.drawable.online_status_green)
            }
            "2", "3"->{
                binding.onlineStatusImage.setImageResource(R.drawable.offline_status_red)
            }
            /*"3"->{
                binding.onlineStatusImage.setImageResource(R.drawable.busy_status)
            }*/
        }
        (requireActivity() as? HomeActivity)?.setOnlineStatus(onlineStatus)
    }
    private fun setCreatorStatus(creatorStatus: String?) {
        binding.apply {
            when(creatorStatus){
                "Accepted"->{
                    binding.tvVerification.text = "Verified"
                    clVerification.setOnClickListener {
                        findNavController().navigate(ProfileSettingFragmentDirections.actionProfileSettingFragmentToBecomeCreatorStatusFragment("Accepted"))
                    }
                }
                "Rejected"->{
                    binding.tvVerification.text = "Verification(as a creator)"
                    clVerification.setOnClickListener {
                        findNavController().navigate(R.id.benifitsOfCreatersFragment)
                    }
                }
                "Pending"->{
                    binding.tvVerification.text = "Request Pending"
                    clVerification.setOnClickListener {
                        findNavController().navigate(ProfileSettingFragmentDirections.actionProfileSettingFragmentToBecomeCreatorStatusFragment("Pending"))
                    }
                }
                "No request"->{
                    binding.tvVerification.text = "Verification(as a creator)"
                    clVerification.setOnClickListener {
                        findNavController().navigate(R.id.benifitsOfCreatersFragment)
                    }
                }
            }
        }
    }
    private fun showCustomCoinDialog() {
        var selectedAudioCoinValue = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.audio_call_charges
        var selectedVideoCoinValue = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.video_call_charges
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val btnYes = dialogView.findViewById<Button>(R.id.btnYes)
        val audioCoinsPicker = dialogView.findViewById<ScrollPicker>(R.id.audioCoinsPicker)
        val videoCoinsPicker = dialogView.findViewById<ScrollPicker>(R.id.videoCoinsPicker)
        val switchAudioCall = dialogView.findViewById<SwitchCompat>(R.id.switchAudioCall)
        val switchVideo = dialogView.findViewById<SwitchCompat>(R.id.switchVideo)
        audioCoinsPicker.setShownItemCount(3)
        audioCoinsPicker.setItems(audioCallCoinsList)
        audioCoinsPicker.setSelectedTextSize(20f)
        audioCoinsPicker.setSelectorColor(binding.root.context.getColor(R.color.theme))
        audioCoinsPicker.setSelectedTextColor(binding.root.context.getColor(R.color.theme))
        audioCoinsPicker.setTextColor(binding.root.context.getColor(R.color.black))
        audioCoinsPicker.setTextBold(true)
        audioCoinsPicker.setSelectorLineWidth(5F)
        val initialIndex = audioCallCoinsList.indexOfFirst { it.startsWith("${selectedAudioCoinValue.toString().trim()} ") }
        if (initialIndex != -1) {
            audioCoinsPicker.value = initialIndex // set index directly
        }else{
            audioCoinsPicker.value = 0
        }
        audioCoinsPicker.addOnValueChangedListener {
            selectedAudioCoinValue = audioCoinsPicker.selectedItemText.substringBefore(" ").toIntOrNull() ?: 0
        }
        videoCoinsPicker.setShownItemCount(3)
        videoCoinsPicker.setItems(videoCallCoinsList)
        videoCoinsPicker.setSelectedTextSize(20f)
        videoCoinsPicker.setSelectorColor(binding.root.context.getColor(R.color.theme))
        videoCoinsPicker.setSelectedTextColor(binding.root.context.getColor(R.color.theme))
        videoCoinsPicker.setTextColor(binding.root.context.getColor(R.color.black))
        videoCoinsPicker.setTextBold(true)
        val initialVideoCoinIndex = videoCallCoinsList.indexOfFirst { it.startsWith("${selectedVideoCoinValue.toString().trim()} ") }
        if (initialVideoCoinIndex != -1) {
            videoCoinsPicker.value = initialVideoCoinIndex // set index directly
        }else{
            videoCoinsPicker.value = 0
        }
        videoCoinsPicker.addOnValueChangedListener {
            selectedVideoCoinValue = videoCoinsPicker.selectedItemText.substringBefore(" ").toIntOrNull() ?: 0
        }
        btnYes.setOnClickListener {
                val token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(), LOGIN_DATA
                )?.payload?.authToken
                viewModel.hitUpdateCoinsApi(token, selectedAudioCoinValue.toString(), selectedVideoCoinValue.toString())
                dialog.dismiss()
            }
        switchVideo.isChecked = isVideoEnable == true
        switchAudioCall.isChecked = isAudioEnable == true
        switchVideo.setOnCheckedChangeListener { _, isChecked ->
            isVideoEnable = isChecked
            hitSetPushNotificationsApi(SetNotificationRequest(videoCall = isChecked.toInt()))
        }
        switchAudioCall.setOnCheckedChangeListener { _, isChecked ->
            isAudioEnable = isChecked
            hitSetPushNotificationsApi(SetNotificationRequest(audioCall = isChecked.toInt()))
        }
        dialog.show()
    }
    fun hitSetPushNotificationsApi(request: SetNotificationRequest){
        val token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString()
        viewModel.hitSetPushNotificationApi(token, request = request)
    }
    fun Boolean.toInt() = if (this) 1 else 2
}