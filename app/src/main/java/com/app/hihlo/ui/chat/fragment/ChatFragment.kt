package com.app.hihlo.ui.chat.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.ClipboardManager
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentChatBinding
import com.app.hihlo.databinding.PopupChatMoreOptionsBinding
import com.app.hihlo.model.chat.Messages
import com.app.hihlo.ui.chat.view_model.ChatViewModel
import com.app.hihlo.ui.chat.adapter.MessageAdapter
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.databinding.AdapterChatOtherAudioBinding
import com.app.hihlo.databinding.FragmentUploadMediaBottomSheetBinding
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.static.chatMoreOptionsList
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference.AGORA_TOKEN
import com.app.hihlo.preferences.UserPreference.CALL_TYPE
import com.app.hihlo.preferences.UserPreference.CALL_USER_NAME
import com.app.hihlo.preferences.UserPreference.CHANNEL_NAME
import com.app.hihlo.preferences.UserPreference.OTHER_USER_ID
import com.app.hihlo.preferences.UserPreference.U_ID
import com.app.hihlo.ui.calling.activity.OutgoingVideoCallActivity
import com.app.hihlo.ui.chat.adapter.AdapterChatMoreOptions
import com.app.hihlo.utils.CommonUtils.hideKeyboard
import com.app.hihlo.utils.CommonUtils.touchHideKeyBoard
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.deduct_chat_coin.DeductChatCoinRequest
import com.app.hihlo.model.predefined_chats.PredefinedChatsResponse
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.static.MIN_COINS_FOR_AUDIO
import com.app.hihlo.model.static.MIN_COINS_FOR_VIDEO
import com.app.hihlo.model.static.MIN_COINS_TO_INITIATE_CHAT
import com.app.hihlo.model.static.StaticLists.longPressMessageList
import com.app.hihlo.model.unblock_user.request.UnblockUserRequest
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.CALLER_USER_IMAGE
import com.app.hihlo.ui.chat.adapter.AdapterPredefinedChat
import com.app.hihlo.preferences.UserPreference.CITY_COUNTRY
import com.app.hihlo.preferences.UserPreference.USER_NAME
import com.app.hihlo.ui.calling.activity.OldOutgoingCallActivity
import com.app.hihlo.ui.chat.bottom_sheet.SendCoinsBottomSheetFragment
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.reels.adapter.AdapterListPopup
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.MediaUtils.uriToFile
import com.app.hihlo.utils.MyApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusableAudioVideoPopup
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.getLength
import com.app.hihlo.utils.getString
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.abs

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding>(), MessageAdapter.AudioPlayInterface {
    lateinit var adapter: MessageAdapter
    var listType = ""
    private val viewModel: ChatViewModel by viewModels()
    private var otherUserId : Int = -1
    private var myId : String = ""
    private var myName : String = ""
    private var otherUserDetails: UserDetailsX? = UserDetailsX()
    private var from = ""
    private var isRequestMessageSent = false
    private var isEditPredefinedChatSelected = false
    private var chatId = ""
    private var callerName = ""
    private var callerImage = ""
    private var selectedPredefinedChatId = ""

    private var currentPlayer: MediaPlayer?=null
    private var isPlaying = false
    private var amICreator = 0
    private var isOtherUserCreator = 0
    private var currentPosition = 0
    private lateinit var updateTimeRunnable: Runnable
    private var handler = Handler(Looper.getMainLooper())
    private var playbackPosition=HashMap<Int,Int>()
    private var playButton: ImageView?=null
    private var pauseButton: ImageView?=null
    private var currentAction=""
    private var currentAudioPlayingPos: Int=-1
    private lateinit var audioPlayerBinding : AdapterChatOtherAudioBinding
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var output: String
    private var isRecording = false
    private var isRecordingPressed = false
    private var elapsedTimeSeconds: Int = 0
    var isReplySelected = false
    var repliedMessageText = ""

    private var selectedMediaType: String = "I"

    var totalAvailableCoins: Int?=null
    private var rootView: View? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun initView(savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        initRecyclerView()
        onClick()
//        touchHideKeyBoard(binding.root, requireActivity())
        otherUserDetails = arguments?.getParcelable<UserDetailsX>("userDetail")
        isOtherUserCreator = otherUserDetails?.isCreator ?: 0
        amICreator = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator ?: 0
        from = arguments?.getString("from").toString()
        chatId = arguments?.getString("chatId").toString()
        otherUserId = otherUserDetails?.id ?: -1
        Log.i("TAG", "initView: "+otherUserDetails)
        myId = (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId).toString()
        myName = (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.fullName).toString()
        callerName = otherUserDetails?.name ?: ""
        callerImage = otherUserDetails?.profileImage ?: ""
        setupUi()
//        setBottomBarPadding()
        chatEdittextListener()
//        binding.predefinedTextRecycler.adapter = AdapterPredefinedMessageHorizontal(listOf<PredefinedChatsResponse.Payload>(PredefinedChatsResponse.Payload(predefined_chat = "Hi", id = 0), PredefinedChatsResponse.Payload(predefined_chat = "Hlo", id = 1)), ::getSelectedPredefinedChat)
        //setBottomMargin()

        Log.i("TAG", "initView from"+from)

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.FOREGROUND_SERVICE_CAMERA,
                    Manifest.permission.FOREGROUND_SERVICE_MICROPHONE),
                100
            )
        }*/
    }

    private fun setBottomMargin() {
        val desiredMarginInPx = if ((requireActivity() as HomeActivity).isGestureNavigation()) CommonUtils.dpToPx(0) else CommonUtils.dpToPx(-40)
        val layoutParams = binding.mainLayout.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = desiredMarginInPx
        binding.mainLayout.layoutParams = layoutParams
    }

    private fun chatEdittextListener() {
        binding.apply {
            chatEdittext.doAfterTextChanged {
                if (!binding.preLayout.isVisible){
                    if (chatEdittext.text?.isEmpty() == true){
                        onEmptyChatBox()
                    }else{
                        onNonEmptyChatBox()
                    }
                }
            }
        }
    }

    private fun onNonEmptyChatBox() {
        binding.apply {
            openGallery.isVisible = false
            giftButton.isVisible = false
            recordAudioButton.isVisible = false
            sendButton.isVisible = true
            recordAudioButton.isVisible = false
            openPredefinedChatScreen.isVisible=false
        }
    }

    private fun onEmptyChatBox() {
        binding.apply {
            openGallery.isVisible = true
            giftButton.isVisible = true
            recordAudioButton.isVisible = true
            sendButton.isVisible = false
            recordAudioButton.isVisible=true
            openPredefinedChatScreen.isVisible=true
        }
    }

    override fun onResume() {
        super.onResume()
        UserPreference.isChatFragmentOpen="1"
        (activity as? HomeActivity)?.applyBottomBarPadding = false
    }

    override fun onDestroy() {
        super.onDestroy()
        UserPreference.isChatFragmentOpen="0"
    }
    override fun onPause() {
        super.onPause()
        UserPreference.isChatFragmentOpen="0"
        (activity as? HomeActivity)?.applyBottomBarPadding = true
        // Restore default for other screens
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        setFragmentResultListener("my_request_key") { requestKey, bundle ->
            val result = bundle.getString("my_result_key")
            if (result?.isNotEmpty() == true){
                sendMessage((Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(), otherUserId.toString(), otherUserDetails?.name ?: "" , otherUserDetails?.profileImage ?: "", MediaType.TEXT.name, result, "0", "0")
            }
        }
        //setupKeyboardInsets()
        (activity as? HomeActivity)?.setOnlineStatusVisibility(true)
        (activity as? HomeActivity)?.hideNavigationView()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Capture the views we need
        val root = binding.root
        val bottomLayout = binding.bottomLayout
        rootView = root  // store for removal in onDestroyView
        val defaultMarginPx = (10 * resources.displayMetrics.density).toInt()

        keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = root.height
            val keypadHeight = screenHeight - rect.bottom
            val keyboardOpen = keypadHeight > screenHeight * 0.15

            val params = bottomLayout.layoutParams as ConstraintLayout.LayoutParams
            if (keyboardOpen) {
                params.bottomMargin = keypadHeight
            } else {
                params.bottomMargin = defaultMarginPx
            }
            bottomLayout.layoutParams = params
        }

        root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)

        //rootView?.viewTreeObserver?.addOnGlobalLayoutListener(keyboardLayoutListener)
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }
    private fun setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomLayout) { view, insets ->
            val keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            // Push the entire bottomLayout up exactly by keyboard height
            view.setPadding(0, 0, 0, keyboardHeight)
            insets
        }
    }


    private fun hitSaveRecentChat(fromUserId: String, toUserId: String,  message: String){
        viewModel.hitSaveRecentChatDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
            SaveRecentChatRequest(toUserId = toUserId, message = message))
    }
    private fun setupUi() {
        binding.apply {
            Glide.with(requireContext()).load(otherUserDetails?.profileImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
            userId.text = otherUserDetails?.username
            name.text = otherUserDetails?.name
            userLocation.text = otherUserDetails?.city+", "+otherUserDetails?.country

            Glide.with(requireContext()).load(otherUserDetails?.profileImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImageInner)
            userIdInner.text = otherUserDetails?.username
            nameInner.text = otherUserDetails?.name
            userLocationInner.text = otherUserDetails?.city+", "+otherUserDetails?.country
            if (otherUserDetails?.isCreator ==1){
                verifiedNameTick.isVisible=true
                verifiedNameTickInner.isVisible=true
            }else{
                verifiedNameTick.isVisible=false
                verifiedNameTickInner.isVisible=false
            }
            when(otherUserDetails?.user_live_status){
                "1"->{
                    onlineStatusImage.setImageResource(R.drawable.online_status_green)
                    liveStatusImage.setImageResource(R.drawable.online_status_green)
                    liveStatusText.text = "Online"

                }
                "2", "3"->{
                    onlineStatusImage.setImageResource(R.drawable.offline_status_red)
                    liveStatusImage.setImageResource(R.drawable.offline_status_red)
                    liveStatusText.text = "Offline"
                }
                /*"3"->{
                    onlineStatusImage.setImageResource(R.drawable.busy_status)
                    liveStatusImage.setImageResource(R.drawable.busy_status)
                    liveStatusText.text = "Busy"
                }*/
            }
        }
        viewModel.imageUrl.value = otherUserDetails?.name ?: ""
        viewModel.name.value = otherUserDetails?.name ?: ""
        output = getOutputFilePath()
        if (otherUserDetails?.blockStatus?.isBlocked==1){
            binding.apply {
                predefinedTextRecycler.isVisible=false
                openPredefinedChatScreen.isVisible=false
                bottomLayout.isVisible=false
                sendButton.isVisible=false
                recordAudioButton.isVisible=false
                openGallery.isVisible=false
                giftButton.isVisible=false
            }
        }
        if (otherUserDetails?.blockStatus?.iBlockedThem==1){
            binding.blockedStatusText.isVisible=true
            binding.blockedStatusText.text = "You blocked this user."
        }else if (otherUserDetails?.blockStatus?.theyBlockedMe==1){
            binding.blockedStatusText.isVisible=true
            binding.blockedStatusText.text = "You have been blocked by this user."
        }

    }
    private fun getOutputFilePath(): String {
        return File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.m4a").absolutePath
    }
    private fun onLongTapPredefinedChat(message:PredefinedChatsResponse.Payload, view:View){
        Log.i("TAG", "onLongTap: "+message)
        openDeletePredefinedChatConfirmationDialog(message, view)
    }


    fun openAudioVideoPopup(view: View, createrId: String, string: String) {
        var isAudioEnable = otherUserDetails?.notificationSettings?.audio_call=="1"
        var isVideoEnable = otherUserDetails?.notificationSettings?.video_call=="1"
        val popup = ReusableAudioVideoPopup(
            context = requireContext(),
            anchorView = view, // show popup below this item
            onAudioClick = {
                if (isAudioEnable){
                    if ((totalAvailableCoins ?: 0) > MIN_COINS_FOR_AUDIO){
                        sendMessage(
                            (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(),
                            otherUserId.toString(),
                            otherUserDetails?.name ?: "" ,
                            otherUserDetails?.profileImage ?: "",
                            MediaType.CALL.name,
                            "Audio Call",
                            "0",
                            "0")
                        viewModel.hitGenerateAgoraTokenDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, channelName = UUID.randomUUID().toString(), calleeId = createrId, uid = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), "audio", sender_id = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString())
                    }else{
                        Toast.makeText(requireContext(), "You need atleast $MIN_COINS_FOR_AUDIO coins to make an Audio Call.", Toast.LENGTH_SHORT).show()
                    }
                }else{

                }
            },
            onVideoClick = {
                if (isVideoEnable){
                    if ((totalAvailableCoins ?: 0) > MIN_COINS_FOR_VIDEO){
                        sendMessage(
                            (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(),
                            otherUserId.toString(),
                            otherUserDetails?.name ?: "" ,
                            otherUserDetails?.profileImage ?: "",
                            MediaType.CALL.name,
                            "Video Call",
                            "0",
                            "0")
                        viewModel.hitGenerateAgoraTokenDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, channelName = UUID.randomUUID().toString(), calleeId = createrId, uid = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), "video", sender_id = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString())
                    }else{
                        Toast.makeText(requireContext(), "You need atleast $MIN_COINS_FOR_VIDEO coins to make a Video Call.", Toast.LENGTH_SHORT).show()
                    }
                }
                //viewModel.hitDeletePredefinedChatApi(token =  "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, message.id.toString())
            },
            alignEnd = false,
            audioAlpha = if (isAudioEnable) 1f else 0.5f,
            videoAlpha = if (isVideoEnable) 1f else 0.5f,
            audioCallText = if (isAudioEnable) "Audio Call     ${otherUserDetails?.audio_call_charges} " else "Audio Call    \uD83D\uDEAB",
            videoCallText = if (isVideoEnable) "Video Call     ${otherUserDetails?.video_call_charges} " else "Video Call    \uD83D\uDEAB"
        )
        popup.show()
    }

    fun openDeletePredefinedChatConfirmationDialog(message: PredefinedChatsResponse.Payload, view: View) {
        val popup = ReusablePopup(
            context = requireContext(),
            anchorView = view, // show popup below this item
            onOption1Click = {
                binding.apply {
                    addPredefinedChat.text = "Save"
                    chatEdittext.setText(message.predefined_chat)
                    isEditPredefinedChatSelected = true
                    selectedPredefinedChatId = message.id.toString()
                }
            },
            onOption2Click = {
                viewModel.hitDeletePredefinedChatApi(token =  "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, message.id.toString())
            },
            {},
            {},
            "Edit",
            "<font color='#FF0000'>Delete</font>",
            "Cancel",
            option1ImageRes = R.drawable.edit_chat,
            option2ImageRes = R.drawable.delete,
            option3ImageRes = R.drawable.cancel,
            alignEnd = false
        )
        popup.show()
    }
    override fun onDestroyView() {
        keyboardLayoutListener?.let { listener ->
            rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
        keyboardLayoutListener = null
        rootView = null
        super.onDestroyView()
        viewModel.stopMessagesListener()
    }
    private fun setObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.messagesFlow.collect { messages ->
                    Log.i("TAG", "setObserver: "+messages)
                    addMessageInRecyclerView(messages.toMutableList())
                }
            }
        }
        Log.i("TAG", "setObserver: "+otherUserId.toString()+"  "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId)
        viewModel.startMessagesListener(otherUserId.toString(), Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString())
        lifecycleScope.launch {
            delay(500)
            viewModel.hitCoinDetailsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
            viewModel.hitCheckChatHistoryApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, otherUserId.toString())
            viewModel.hitReadMessagesApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, otherUserId.toString(), /*if (chatId.isEmpty()) otherUserId.toString() else */null )
        }
        viewModel.getEditPredefinedChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "add predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        binding.chatEdittext.setText("")
                        binding.addPredefinedChat.text = "Add"
                        isEditPredefinedChatSelected=false
                        binding.chatEdittext.hint = "Add a fast reply"
                        viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
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

        viewModel.getAddPredefinedChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "add predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        binding.chatEdittext.setText("")
                        viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
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

        viewModel.getDeletePredefinedChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
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

        viewModel.getPredefinedChatOfUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get predefined chat user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        binding.predefinedRecycler.adapter = AdapterPredefinedChat(it.data.payload, ::onLongTapPredefinedChat){ it ->
                            sendMessage((Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(), otherUserId.toString(), otherUserDetails?.name ?: "" , otherUserDetails?.profileImage ?: "", MediaType.TEXT.name, it, "0", "0")
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

        viewModel.getHandleMessageRequestLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "check chat history success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        isRequestMessageSent=true
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
        viewModel.getCheckChatHistoryLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "check chat history success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.payload.isFirstTime==1){
                            viewModel.paid.value = "0"
                            binding.coinDeductionTextLayout.isVisible=true
                        }else{
                            viewModel.paid.value = "1"
                            binding.coinDeductionTextLayout.isVisible=false

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
        viewModel.getReadMessagesLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "check chat read message success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){

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

        viewModel.getChatCoinDeductLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "coins details success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
//                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        viewModel.paid.value = "1"
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
        viewModel.getCoinDetailsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "coins details success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        totalAvailableCoins = it.data.payload.coins
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
        viewModel.getSendGiftLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "sent coins user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        showCustomDialogWithBinding(requireContext(), "Send Successfully!",
                            onYes = {},
                            onNo = {},
                            showButtons = false,
                            autoDismissInMillis = 1000
                        )
                        sendMessage(
                            (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(),
                            otherUserId.toString(),
                            otherUserDetails?.name ?: "" ,
                            otherUserDetails?.profileImage ?: "",
                            MediaType.COIN.name,
                            it.data.payload.gift.coins.toString(),
                            "0",
                            "0")
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
        viewModel.getGenerateAgoraTokenLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Agora token success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            val intent = Intent(requireContext(), OutgoingVideoCallActivity::class.java)
                            intent.putExtra(OTHER_USER_ID, it.data.payload.calleeId)
                            intent.putExtra(AGORA_TOKEN, it.data.payload.agoraToken)
                            intent.putExtra(CHANNEL_NAME, it.data.payload.channelName)
                            intent.putExtra(U_ID, it.data.payload.uid)
                            intent.putExtra(CALL_TYPE, it.data.payload.callType)
                            intent.putExtra(CALL_USER_NAME, callerName)
                            intent.putExtra(CALLER_USER_IMAGE, callerImage)
                            intent.putExtra(USER_NAME, otherUserDetails?.username)
                            val cityCountry = otherUserDetails?.city+", "+otherUserDetails?.country
                            intent.putExtra(CITY_COUNTRY,cityCountry)
                            intent.putExtra("amICreator", amICreator.toString())
                            intent.putExtra("isOtherUserCreator", isOtherUserCreator.toString())
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                            UserPreference.CALL_ID="CALL_ID"
                            startActivity(intent)
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
        viewModel.getSaveRecentChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Save recent success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.scrollView.fullScroll(View.FOCUS_DOWN)
                            lifecycleScope.launch {
                                delay(1000)
                                binding.chatEdittext.requestFocus()
                                binding.chatEdittext.setSelection(binding.chatEdittext.getLength())
                                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(binding.chatEdittext, InputMethodManager.SHOW_IMPLICIT)
                            }

                        }else{
                        }
                    }else{
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "save recent Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        /*viewModel.getPredefinedChatLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Save recent success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.predefinedTextRecycler.adapter = AdapterPredefinedMessageHorizontal(it.data.payload, ::getSelectedPredefinedChat)
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
                    Log.e("TAG", "save recent Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }*/
        viewModel.isChatDeleted.observe(viewLifecycleOwner){
            Log.i("TAG", "setObserver: "+viewModel.isChatDeleted.value)
            if (viewModel.isChatDeleted.value == true){
                adapter.listOfMessage.clear()
                adapter.notifyDataSetChanged()
                viewModel.isChatDeleted.value=false
            }
        }
        viewModel.getBlockUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Block User success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        RTVariable.isBlocked = true
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
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
        viewModel.getUnblockUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "UnBlock User success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        RTVariable.isBlocked = true
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        //findNavController().popBackStack()
                        otherUserDetails?.blockStatus?.iBlockedThem = 0
                        runChatListAPI()
                        runRequestListAPI()
                        runUISet(false)
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

    private fun runUISet(boolean: Boolean){
        if(boolean){
            binding.apply {
                predefinedTextRecycler.isVisible = false
                openPredefinedChatScreen.isVisible = false
                bottomLayout.isVisible = false
                sendButton.isVisible = false
                recordAudioButton.isVisible = false
                openGallery.isVisible = false
                giftButton.isVisible = false
            }
            binding.blockedStatusText.isVisible=true
            binding.blockedStatusText.text = "You blocked this user."
        }else{
            binding.apply {
                predefinedTextRecycler.isVisible = true
                openPredefinedChatScreen.isVisible = true
                bottomLayout.isVisible = true
                sendButton.isVisible = true
                recordAudioButton.isVisible = true
                openGallery.isVisible = true
                giftButton.isVisible = true
            }
            binding.blockedStatusText.isVisible=false
        }
    }

    private fun runChatListAPI(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getRecentChats(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    fromUserId = null,
                    toUserId = null,
                    type = "inbox"
                )
                if (response.status == 1 && response.code == 200) {
                    RTVariable.chat_recentList = emptyList()
                    RTVariable.chat_recentList = response.payload.recentChats
                    Log.e("CCCC", "CCCC>>> "+RTVariable.chat_recentList)
                } else {
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun runRequestListAPI(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getRecentChats(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    fromUserId = null,
                    toUserId = null,
                    type = "request"
                )
                if (response.status == 1 && response.code == 200) {
                    RTVariable.chat_requestUsersList = emptyList()
                    RTVariable.chat_requestUsersList = response.payload.recentChats
                    Log.e("CCCC", "CCCC>>> R"+RTVariable.chat_requestUsersList)
                } else {
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun addMessageInRecyclerView(list: MutableList<Messages>) {
        if (list.isNotEmpty()){
            Log.i("TAG", "addMessageInRecyclerView: "+list)
            adapter.setList(list.toMutableList())
            adapter.notifyDataSetChanged()
            lifecycleScope.launch {
                delay(500)
                if (isAdded) binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }

        }else{
            adapter.setList(mutableListOf())
            adapter.notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onClick() {
        binding.addPredefinedChat.setOnClickListener {
            if (binding.chatEdittext.getString().isEmpty()){
                Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
            }else{
                if (isEditPredefinedChatSelected){
                    viewModel.hitEditPredefinedChatApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, preDefinedChatId = selectedPredefinedChatId, preDefinedChat = binding.chatEdittext.text.toString() )
                }else{
                    viewModel.hitAddPredefinedChatApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, binding.chatEdittext.text.toString())
                }
            }
        }
        binding.closeReply.setOnClickListener {
            binding.apply {
                isReplySelected = false
                repliedMessageText=""
                replyLayout.isVisible=false
                addPredefinedChat.isVisible=false
                openGallery.isVisible=true
                giftButton.isVisible=true
                openPredefinedChatScreen.isVisible=true
                recordAudioButton.isVisible=true
                chatEdittext.hint = "Message"
                chatEdittext.setText("")
            }
        }
        binding.closePredefined.setOnClickListener {
            closePredefined()
        }
        binding.deleteAudio.setOnClickListener {
            isRecording=true
            stopRecording(true)
            binding.bottomLayout.isVisible=true
            binding.waveLayout.isVisible=false
        }
        binding.openPredefinedChatScreen.setOnClickListener {
            isEditPredefinedChatSelected=false
            setChatEditTextConstraints()
            binding.apply {
                replyLayout.isVisible=false
                recordAudioButton.isVisible=false
                preLayout.isVisible=true
                openGallery.isVisible=false
                giftButton.isVisible=false
                openPredefinedChatScreen.isVisible=false
                addPredefinedChat.isVisible=true
                addPredefinedChat.text = "Add"
                chatEdittext.hint = "Add a fast reply"
            }
            binding.predefinedRecycler.adapter = AdapterPredefinedChat(listOf(), ::onLongTapPredefinedChat){ it ->

            }
            viewModel.hitPredefinedChatsOfUserApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
        }
        binding.backButton.setOnClickListener {
            (requireActivity() as? HomeActivity)?.setOnlineStatusVisibility(false)
            findNavController().popBackStack()
        }
        binding.chatRecycler.setOnClickListener {
            hideKeyboard(requireActivity())
//            touchHideKeyBoard(binding.root, requireActivity())
        }
        binding.playPauseAudio.setOnClickListener {
            if (isRecording){
                isRecording=false
                binding.playPauseAudio.setImageResource(R.drawable.audio_pause)
                mediaRecorder.pause()
                binding.waveImageview.visibility = View.INVISIBLE
            }else{
                isRecording=true
                binding.playPauseAudio.setImageResource(R.drawable.voice_pause)
                mediaRecorder.resume()
                startTimer()
                binding.waveImageview.isVisible= true
            }
        }
        binding.recordAudioButton.setOnClickListener {
//            setLongPress()
            longPressAction(true)
        }
        binding.sendAudioButton.setOnClickListener {
            isRecording = true
            longPressAction(false)
        }
        binding.sideOptions.setOnClickListener {
            openSideOptionsPopup()
        }
        binding.callIcon.setOnClickListener {
            if (otherUserDetails?.user_live_status=="1"){
                if (otherUserDetails?.blockStatus?.isBlocked!=1){
                    if (amICreator!=1 && isOtherUserCreator!=1){
                        Toast.makeText(requireContext(), "You can not call to a user.", Toast.LENGTH_SHORT).show()
                    }else{
                        openAudioVideoPopup(binding.callIcon,otherUserId.toString(), otherUserDetails?.name ?: "")
                       // openCallTypeBottomSheet(otherUserId.toString(), otherUserDetails?.name ?: "")
                    }
                }
            }else{
                Toast.makeText(requireContext(), "User is offline now.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.giftButton.setOnClickListener {
            val bottomSheetFragment = SendCoinsBottomSheetFragment(totalAvailableCoins ?: 0).apply {
                onCoinsSelected = { data ->
                    openSendCoinsDialog(data)
                    dismiss()
                }
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "")
        }
        binding.sendButton.setOnClickListener {
            if (isRecordingPressed){
            }else{
                Log.i("TAG", "onClick: "+binding.chatEdittext.text.toString())
                sendMessage((Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(),
                    otherUserId.toString(),
                    otherUserDetails?.name ?: "" ,
                    otherUserDetails?.profile_image ?: "",
                    if (isReplySelected) MediaType.REPLY.name else MediaType.TEXT.name,
                    binding.chatEdittext.text.toString(),
                    "0",
                    "0",
                    repliedMessage = repliedMessageText
                    )

            }
        }
        binding.openGallery.setOnClickListener {
            checkGalleryPermissionAndPick("I")
        }
        binding.userImage.setOnClickListener {
            (requireActivity() as HomeActivity).hideNavigationView()
            findNavController().navigate(ChatFragmentDirections.actionChatFragmentToProfileFragment("0", otherUserId.toString(), "chat"))
        }
        binding.viewProfile.setOnClickListener {
            (requireActivity() as HomeActivity).hideNavigationView()
            findNavController().navigate(ChatFragmentDirections.actionChatFragmentToProfileFragment("0", otherUserId.toString(), "chat"))
        }
        binding.bottomLayout.setOnClickListener {

            binding.chatEdittext.requestFocus()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.chatEdittext, InputMethodManager.SHOW_IMPLICIT)

        }
    }

    private fun closePredefined() {
        binding.apply {
            resetChatEditTextConstraints()
            preLayout.isVisible=false
            openGallery.isVisible=true
            giftButton.isVisible=true
            openPredefinedChatScreen.isVisible=true
            addPredefinedChat.isVisible=false
            binding.chatEdittext.hint = "Message"
            binding.chatEdittext.setText("")
        }
    }

    private fun setChatboxForOpeningPredefinedChat() {
        TODO("Not yet implemented")
    }
    private fun setChatEditTextConstraints() {
        val params = binding.chatEdittext.layoutParams as ConstraintLayout.LayoutParams

        // Set constraints just like in XML
        params.startToEnd = binding.emojiButton.id
        params.endToStart = binding.addPredefinedChat.id
        params.topToBottom = binding.preLayout.id

        binding.chatEdittext.layoutParams = params
    }

    private fun resetChatEditTextConstraints() {
        val params = binding.chatEdittext.layoutParams as ConstraintLayout.LayoutParams

        // Clear previous constraints
        params.startToEnd = ConstraintLayout.LayoutParams.UNSET
        params.endToStart = ConstraintLayout.LayoutParams.UNSET

        // Reset to match parent width again
        params.startToEnd = binding.emojiButton.id
        params.endToStart = binding.addPredefinedChat.id
        params.topToBottom = binding.replyLayout.id

        binding.chatEdittext.layoutParams = params
    }

    fun openSendCoinsDialog(data: RechargePackageListResponse.Payload) {
        showCustomDialogWithBinding(requireContext(), "Do you want to send ${data.coins} coins to ${callerName}",
            onYes = {
                viewModel.hitSendGiftApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        MyApplication.appContext, LOGIN_DATA
                    )?.payload?.authToken,
                    SendGiftRequest(coins = data.coins.toString(), recipientId = otherUserId.toString(), type = "chat")
                )
            },
            onNo = {

            }
        )
    }
    private fun checkGalleryPermissionAndPick(mediaType: String) {
        selectedMediaType = mediaType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            val permissions = arrayOf(
//                Manifest.permission.READ_MEDIA_IMAGES,
//                Manifest.permission.READ_MEDIA_VIDEO
//            )
//            requestMultiplePermissionsLauncher.launch(permissions)
            launchMediaPicker()
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                launchMediaPicker()
            } else {
                requestSinglePermissionLauncher.launch(permission)
            }
        }
    }
    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
    }
    private fun launchMediaPicker() {
        val mediaType = when (selectedMediaType) {
            "I" -> ActivityResultContracts.PickVisualMedia.ImageOnly
            "V" -> ActivityResultContracts.PickVisualMedia.VideoOnly
            else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        }


        mediaPickerLauncher.launch(PickVisualMediaRequest(mediaType))
    }
    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mimeType = requireContext().contentResolver.getType(uri)
            UserPreference.selectedMediaType = selectedMediaType

            if (mimeType?.startsWith("video") == true) {
                /*if (uri != null) {
                    val mimeType = requireContext().contentResolver.getType(uri)
                    Log.e("TAG", "mimmeType $mimeType")
                    if (mimeType?.startsWith("video") == true) {
                        UserPreference.seletedUri = Uri.EMPTY
                        val intent = Intent(requireActivity(),TrimVideoActivity::class.java)
                        intent.putExtra("videoUrl",uri.toString())
                        startActivityForResult(intent,REQUEST_CODE_CROP_VIDEO)
                    }
                } else {
                }*/
            } else {
                openCropActivity(uri)
            }
        } else {
            Toast.makeText(requireContext(), "No media selected", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openCropActivity(imageUri: Uri) {
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
        }
        val destinationUri = Uri.fromFile(File(requireActivity().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .start(requireContext(), this)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Always call super

        if (resultCode==RESULT_OK){
            if (requestCode == REQUEST_CODE_CROP_VIDEO) { // Check if it's the result for our request
//                UserPreference.selectedMediaToUpload = selectedBottomSheetType
//                findNavController().navigate(R.id.action_profileFragment_to_addReelFragment)
            }else if (requestCode==UCrop.REQUEST_CROP){
                val resultUri = UCrop.getOutput(data!!)
                val fileImage = uriToFile(resultUri ?: Uri.EMPTY,requireActivity())
                uploadImage(fileImage, MediaType.IMAGE.name, "")
            }
        }else {
            Log.w("ProfileFragment", " cropping was cancelled or failed with code: $resultCode")
        }
    }

    fun sendMessage(
        sender: String,
        receiver: String,
        friendName: String,
        friendImage: String,
        messageType: String,
        message: String,
        pinned: String,
        archived: String,
        url: String?=null,
        duration: String?=null,
        repliedMessage: String?=null,
    ) {
        Log.d("ChatFragment", "sendMessage called with parameters:")
        Log.d("ChatFragment", "sender: $sender")
        Log.d("ChatFragment", "receiver: $receiver")
        Log.d("ChatFragment", "friendName: $friendName")
        Log.d("ChatFragment", "friendImage: $friendImage")
        Log.d("ChatFragment", "messageType: $messageType")
        Log.d("ChatFragment", "message: $message")
        Log.d("ChatFragment", "pinned: $pinned")
        Log.d("ChatFragment", "archived: $archived")
        Log.d("ChatFragment", "url: $url")
        Log.d("ChatFragment", "duration: $duration")
        Log.d("ChatFragment", "repliedMessage: $repliedMessage")
        if (viewModel.paid.value=="0"){
            if ((totalAvailableCoins ?: 0) < MIN_COINS_TO_INITIATE_CHAT){
                Toast.makeText(requireContext(), "You need atleast $MIN_COINS_TO_INITIATE_CHAT coins to initiate a chat", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.chatAdDetailFragment)
            }else{
//                Toast.makeText(requireContext(), "Coins are deducted", Toast.LENGTH_SHORT).show()
                hitCoinDeductApi()
                viewModel.sendMessage(sender, receiver, friendName , friendImage, messageType, message, pinned, archived, url ?: "", duration, repliedMessage = repliedMessage)
                hitSaveRecentChat(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), otherUserId.toString(), if (messageType== MediaType.COIN.name) message+" Coins" else if (message.isNotEmpty()) message else messageType)
                binding.chatEdittext.setText("")
            }
        }else{
            if (from=="request"&&isRequestMessageSent==false){
                viewModel.hitHandleMessageRequestDataApi("Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken ?: "", chatId= chatId, action = "accept")
            }
            viewModel.sendMessage(sender, receiver, friendName , friendImage, messageType, message, pinned, archived, url ?: "", duration, repliedMessage = repliedMessage)
            hitSaveRecentChat(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), otherUserId.toString(), if (messageType== MediaType.COIN.name) message+" Coins" else if (message.isNotEmpty()) message else messageType)
            binding.chatEdittext.setText("")
        }
        if (isReplySelected){
            isReplySelected=false
            repliedMessageText=""
            binding.replyLayout.isVisible=false
        }
    }

    private fun hitCoinDeductApi() {
        viewModel.hitChatCoinDeductApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, DeductChatCoinRequest(toUserId = otherUserId.toString()))
    }

    private fun openSideOptionsPopup() {
        val inflater = LayoutInflater.from(requireContext())
        val binding = PopupChatSideOptionsBinding.inflate(inflater)

        binding.title1.text = "Delete Chat"
        if (otherUserDetails?.blockStatus?.iBlockedThem!=1){
            binding.title2.text = "Block"
        }else{
            binding.title2.text = "Unblock"
        }
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
//            setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // for outside touch to dismiss
            elevation = 20f
            showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        }
        binding.title1.setOnClickListener {
            popupWindow.dismiss()
            openDeleteCompleteConfirmationDialog()
        }
        binding.title2.setOnClickListener {
            popupWindow.dismiss()
            if (otherUserDetails?.blockStatus?.iBlockedThem==1) {
                openBlockUserConfirmationDialog()
            }
            else{
                val bottomSheetFragment = BlockFlagBottomSheet()
                val bundle = Bundle().apply {
                    putString("screen", "block")  // Add your arguments here
                    putString("userId", otherUserId.toString())  // Add your arguments here
                }
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.onBlockSuccessful = {
                    bottomSheetFragment.dismiss()
                    //findNavController().popBackStack()
                    runChatListAPI()
                    runRequestListAPI()
                    runUISet(true)
                    otherUserDetails?.blockStatus?.iBlockedThem = 1
                }
                bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
            }
        }
    }
    fun openBlockUserConfirmationDialog() {
        showCustomDialogWithBinding(requireContext(), "Are you sure you want to ${if (otherUserDetails?.blockStatus?.iBlockedThem==1)"unblock" else "block"} this user?",
            onYes = {
                if (otherUserDetails?.blockStatus?.iBlockedThem==1) {
                    viewModel.hitUnblockUserApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, UnblockUserRequest(unblockId = otherUserId.toString() ))
                }
                else{
                    viewModel.hitBlockUserApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, BlockUserRequest(blockReasonId = "3", blockedUserId = otherUserId.toString() ))
                }
            },
            onNo = {
                //dismiss()
            }
        )
    }
    fun openDeleteCompleteConfirmationDialog() {
        showCustomDialogWithBinding(requireContext(), "Do you want to Delete All the Messages?",
            onYes = {
                viewModel.deleteChat(sender = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), receiver = otherUserId.toString(), Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken ?: "")
            },
            onNo = {
                //dismiss()
            }
        )
    }
    private fun showPopup() {
        val inflater = LayoutInflater.from(requireContext())
        val popupBinding = PopupChatMoreOptionsBinding.inflate(inflater)

        popupBinding.moreOptionsRecycler.adapter = AdapterChatMoreOptions(chatMoreOptionsList){}

        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // to dismiss on outside touch
            elevation = 20f

            showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        }
    }

    private fun initRecyclerView() {
        val swipeListener = object : MessageAdapter.OnMessageSwipeListener {
            override fun onMessageSwiped(message: Messages) {
                resetChatEditTextConstraints()
                isReplySelected = true
                binding.replyLayout.isVisible = true
                binding.preLayout.isVisible = false
                binding.replyUserMessage.text = if (message.messageType== MediaType.COIN.name ) message.message+" Coins" else if (message.message?.isEmpty()==true) message.messageType.toString() else message.message.toString()
                if (myId==message.sender){
                    repliedMessageText = if (message.messageType== MediaType.COIN.name ) message.message.toString() else if (message.message?.isEmpty()==true) message.messageType.toString() else message.message.toString()
                    binding.replyUserName.text = "You"
                }else{
                    repliedMessageText = if (message.messageType== MediaType.COIN.name ) message.message.toString() else if (message.message?.isEmpty()==true) message.messageType.toString() else message.message.toString()
                    binding.replyUserName.text = otherUserDetails?.name
                }
                CommonUtils.showKeyboard(requireContext(), binding.chatEdittext)
            }
        }

        adapter = MessageAdapter(::getSelectedItem, listType, ::onLongTap, Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId, ::closeKeyboard, this, swipeListener)
        binding.chatRecycler.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val adapter = recyclerView.adapter as MessageAdapter
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return 0

                val message = adapter.listOfMessage[position]

                return if (message.sender == myId) {
                    // 👉 outgoing → allow LEFT swipe only
                    ItemTouchHelper.RIGHT
                } else {
                    // 👉 incoming → allow RIGHT swipe only
                    ItemTouchHelper.RIGHT
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // prevent dismiss
                binding.chatRecycler.adapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
            }

            private var hasVibrated = false

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val swipeThreshold = itemView.width / 3
                    val translationX = dX.coerceIn(-swipeThreshold.toFloat(), swipeThreshold.toFloat())
                    itemView.translationX = translationX

                    // 🔹 vibrate ONCE when crossing threshold
                    if (abs(dX) > swipeThreshold / 2 && !hasVibrated) {
                        hasVibrated = true
                        val vibrator =
                            recyclerView.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(40)
                        }
                    }

                    // reset vibration flag when finger released
                    if (!isCurrentlyActive) {
                        hasVibrated = false
                        itemView.animate().translationX(0f).setDuration(200).start()

                        if (abs(dX) > swipeThreshold) {
                            val position = viewHolder.bindingAdapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                val adapter = recyclerView.adapter as MessageAdapter
                                val message = adapter.listOfMessage[position]
                                swipeListener.onMessageSwiped(message) // 👉 trigger callback
                            }
                        }
                    }
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        })



        itemTouchHelper.attachToRecyclerView(binding.chatRecycler)


    }
    private fun closeKeyboard(){
        if(binding.preLayout.isVisible){
            closePredefined()
        }
        hideKeyboard(requireActivity())
    }
    private fun getSelectedItem(url:String, messageType: String, message: Messages, selection: Int){
        Log.i("TAG", "getSelectedItem: $url  $messageType  $selection  $message")

        when(selection){
            0 ->{
//                when(messageType){
//                    MediaType.DOCUMENT.name ->{
//                        Utils.openFile(url, requireActivity())
//                    }
//                    MediaType.VIDEO.name, MediaType.IMAGE.name ->{
//                        Log.i("TAG", "chat fragment: "+messageType+" "+url)
//
//                        findNavController().navigate(ChatFragmentDirections.actionChatFragmentToOpenImageVideoFragment(messageType, url))
//                    }
//                }
            }
            1->{
                findNavController().navigate(ChatFragmentDirections.actionChatFragmentToOpenImageFragment(imageUrl = message.url ?: ""))
            }
            2->{
                touchHideKeyBoard(binding.root, requireActivity())

            }
        }

    }
    private fun onLongTap(message: Messages, view: View, isMyMessage: Boolean){
        if (myId==message.sender){
            val popup = ReusablePopup(
                context = requireContext(),
                anchorView = view, // show popup below this item
                onOption1Click = {
                    resetChatEditTextConstraints()
                    isReplySelected = true
                    repliedMessageText = if (message.messageType== MediaType.COIN.name ) message.message.toString() else if (message.message?.isEmpty()==true) message.messageType.toString() else message.message.toString()
                    binding.replyLayout.isVisible = true
                    binding.preLayout.isVisible = false
                    binding.replyUserMessage.text = message.message
                    binding.replyUserName.text = "You"
                    CommonUtils.showKeyboard(requireContext(), binding.chatEdittext)

                },
                onOption2Click = {
                    if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) setClipboard(message.message.toString())
                    else viewModel.deleteMessage( "", message, listType)
                },
                onOption3Click = {
                    if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) viewModel.deleteMessage( "", message, listType)
                    else null
                },
                {},
                "Reply",
                if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) "Copy" else "<font color='#FF0000'>Unsend</font>",
                if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) "<font color='#FF0000'>Unsend</font>" else "Cancel",
                if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) "Cancel" else null,
                option1ImageRes = R.drawable.reply,
                option2ImageRes = if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) R.drawable.copy else R.drawable.unsend,
                option3ImageRes = if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) R.drawable.unsend else R.drawable.cancel,
                option4ImageRes = if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) R.drawable.cancel else null,
                alignEnd = if (myId==message.sender) true else false
            )
            popup.show()
        }else{
            val popup = ReusablePopup(
                context = requireContext(),
                anchorView = view, // show popup below this item
                onOption1Click = {
                    resetChatEditTextConstraints()
                    isReplySelected = true
                    repliedMessageText = message.message.toString()
                    binding.replyLayout.isVisible = true
                    binding.preLayout.isVisible = false
                    binding.replyUserMessage.text = message.message
                    binding.replyUserName.text = myName
                    CommonUtils.showKeyboard(requireContext(), binding.chatEdittext)
                },
                onOption2Click = {
                    if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) setClipboard(message.message.toString())
                    else null
                },
                {},
                {},
                "Reply",
                if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) "Copy" else "Cancel",
                if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) "Cancel" else null,
                option1ImageRes = R.drawable.reply,
                option2ImageRes = if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) R.drawable.copy else R.drawable.cancel,
                option3ImageRes = if (message.messageType== MediaType.TEXT.name||message.messageType== MediaType.REPLY.name) R.drawable.cancel else null,
                alignEnd = if (myId==message.sender) true else false
            )
            popup.show()
        }
        Log.i("TAG", "onLongTap: "+message)

    }
    private fun setClipboard(text: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.text = text
        } else {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
        }
//        Toast.makeText(requireContext(), "Selected message text is copied.", Toast.LENGTH_SHORT).show()

    }
    fun openDeleteMessageConfirmationDialog(message: Messages) {
        /*showCustomDialogWithBinding(requireContext(), "Are you sure you want to delete the message?",
            onYes = {
                viewModel.deleteMessage( "", message, listType)
            },
            onNo = {
                //dismiss()
            }
        )*/
        val dialog = BottomSheetDialog(requireContext())
        val binding = FragmentUploadMediaBottomSheetBinding.inflate(layoutInflater) // viewBinding class auto-generated
        binding.listPopupRecycler.adapter = AdapterListPopup(longPressMessageList){
            when(it){
                longPressMessageList[0]->{
                    viewModel.deleteMessage( "", message, listType)
                    dialog.dismiss()
                }
                longPressMessageList[1]->{
                    dialog.dismiss()
                }
            }
        }

        dialog.setContentView(binding.root)
        dialog.show()
    }
    override fun getLayoutId(): Int {
        return R.layout.fragment_chat
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setLongPress() {
        val detector = GestureDetector(object: GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                currentAction = "isScrolling"
                Log.d("TAG", "SCROLLING")
                return true
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onLongPress(e: MotionEvent) {
                Log.d("TAG", "Long press!")
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), getString(R.string.microphone_permission_is_not_granted), Toast.LENGTH_SHORT).show()
                    return
                }
                longPressAction(true)
                currentAction = "isLongPressed"
                super.onLongPress(e)
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d("TAG", "Single tap detected")
                Toast.makeText(requireContext(), getString(R.string.tap_and_hold_button_to_record_audio), Toast.LENGTH_SHORT).show()
                return super.onSingleTapUp(e)
            }
        })

        val gestureListener = View.OnTouchListener(function = { view, event ->
            detector.onTouchEvent(event)

            if(event.getAction() == MotionEvent.ACTION_UP) {
                when (currentAction) {
                    "isScrolling" -> {
                        Log.d("TAG", "Done scrolling")
                        currentAction = ""
                    }
                    "isLongPressed" -> {
                        Log.d("TAG", "Done long pressing")
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(requireContext(), getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                        }else{
                            longPressAction(false)
                            currentAction = ""
                        }
                    }
                }

            }

            false
        })

        binding.recordAudioButton.setOnTouchListener(gestureListener)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun longPressAction(status:Boolean) {
        binding.apply {
            if (status){
                binding.waveImageview.isVisible= true
                sendButton.isVisible=false
                recordAudioButton.isVisible=false
                waveLayout.isVisible=true
                bottomLayout.isVisible=false
                playPauseAudio.setImageResource(R.drawable.voice_pause)
                startRecording()
            }else{
                stopRecording(false)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording() {
//        binding.time.text = "00:00"
//        binding.chatEdittext.setText("00:00")
        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOutputFile(output)

        try {
            mediaRecorder.prepare()
            mediaRecorder.start()
            isRecording = true
            isRecordingPressed=true
//            startWaveformVisualization()
            startTimer()
            startAudioWave()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "${getString(R.string.failed_to_start_recording)}: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            stopRecording(false)
        }

    }

    private fun startAudioWave() {
        binding.apply {
            Glide.with(requireContext())
                .asGif()
                .load(R.drawable.audio_wave_gif)
                .into(waveImageview)
            bottomLayout.setBackgroundDrawable(resources.getDrawable(R.color.transparent, null))
        }
    }
    private fun stopAudioWave() {
        binding.apply {
            Glide.with(requireContext())
                .load(R.color.transparent)
                .into(waveImageview)
            bottomLayout.setBackgroundDrawable(resources.getDrawable(R.drawable.background_25dp_corner_no_stroke, null))
            waveLayout.isVisible=false
            bottomLayout.isVisible=true
            recordAudioButton.isVisible=true
        }
    }

    private fun startTimer() {
        CoroutineScope(Dispatchers.Main).launch {
            while (isRecording) {
                updateTimerText()
                delay(1000)
                elapsedTimeSeconds++
            }
        }
    }
    private fun updateTimerText() {
        val minutes = elapsedTimeSeconds / 60
        val seconds = elapsedTimeSeconds % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        binding.audioTime.text = formattedTime
//        binding.chatEdittext.setText(formattedTime)
    }
    private fun stopTimer() {
        elapsedTimeSeconds = 0
        binding.audioTime.text = ""
    }
    private fun stopRecording(isDeleted: Boolean) {
        stopAudioWave()
        if (isRecording) {
            try {
                Log.i("TAG", "stopRecording: "+"try")
                if (::mediaRecorder.isInitialized){
                    mediaRecorder.stop()
                    mediaRecorder.release()
                }
            } catch (e: RuntimeException) {
                Log.i("TAG", "stopRecording: "+"catch")
                e.printStackTrace()
                Toast.makeText(requireContext(), "${getString(R.string.recording_time_is_too_short)}!", Toast.LENGTH_SHORT).show()
                onEmptyChatBox()
                isRecordingPressed=false
            } finally {
                Log.i("TAG", "stopRecording: "+"finally")
                isRecording = false
                // Get duration of recorded audio
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(output)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMillis = durationStr?.toLongOrNull() ?: 0L
                val durationFormatted = String.format("%02d:%02d", (durationMillis / 1000) / 60, (durationMillis / 1000) % 60)
                Log.i("RecordingDuration", "Duration: $durationFormatted")
                retriever.release()
                if (!isDeleted){
                    uploadImage(File(output), MediaType.AUDIO.name, durationFormatted)
                }
                stopTimer()
            }
        } else {
        }
    }
    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3Client(credentials)
    }
    fun uploadImageToS3(context: Context, file: File, bucketName: String, objectKey: String, accessKey: String, secretKey: String, assetType:String, durationFormatted: String) {
        // Initialize S3 client
        val s3Client = initializeS3Client(accessKey, secretKey)

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        // Start the upload
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)
        ProcessDialog.showDialog(requireContext(), true)
        // Listen to upload events
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    ProcessDialog.dismissDialog(true)
                    // Upload completed successfully
//                    val mediaUrl = "https://$bucketName.s3.amazonaws.com/$objectKey"
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val mediaUrl = "$urlCdn$slash$objectKey"
                    println("Media URL: $mediaUrl")
                    when (assetType) {
                        MediaType.AUDIO.name -> {
                            isRecordingPressed=false
                            sendMessage((Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(), otherUserId.toString(), otherUserDetails?.name ?: "" , otherUserDetails?.profile_image ?: "", MediaType.AUDIO.name, "", "0", "0", mediaUrl, duration = durationFormatted)
                        }
                        MediaType.IMAGE.name -> {
                            sendMessage((Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(), otherUserId.toString(), otherUserDetails?.name ?: "" , otherUserDetails?.profile_image ?: "", MediaType.IMAGE.name, "", "0", "0", mediaUrl)
                        }
                    }
//                    viewModel.hitAddStoryDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, AddStoryRequest(assetUrl = mediaUrl, assetType = assetType))

                } else if (state == TransferState.FAILED) {
                    // Handle failure
                    println("Upload failed")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Handle progress
                val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat() * 100).toInt()
                println("Progress: $percentDone%")
            }

            override fun onError(id: Int, ex: Exception) {
                ProcessDialog.dismissDialog(true)
                // Handle error
                isRecordingPressed=false
                ex.printStackTrace()
            }
        })
    }
    private fun uploadImage(imageFile: File, assetType:String, durationFormatted: String) {
        var s3Data = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        uploadImageToS3(requireContext(), imageFile, bucketName ?: "", objectKey, s3Data?.ACCESS_KEY ?: "", s3Data?.SECRET_KEY ?: "", assetType, durationFormatted)
    }
    fun askVideoCallPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissionFromSettings("Camera")
            }else if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                requestPermissionFromSettings("Microphone")
            }else if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                requestPermissionFromSettings("Notification")
            }else{
//                initCall()
            }
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissionFromSettings("Camera")
            }else if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                requestPermissionFromSettings("Microphone")
            }else{
//                initCall()
            }
        }
    }
    private fun requestPermissionFromSettings(permissionType: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(when (permissionType) {
                "Camera" -> getString(R.string.you_need_to_unable_the_camera_permission_from_settings)
                "Microphone" -> getString(R.string.you_need_to_unable_the_microphone_permission_from_settings)
                "Notification" -> getString(R.string.you_need_to_unable_the_notification_permission_from_settings)
                else -> ""
            })
            .setMessage("Do you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context?.packageName, null)
                intent.data = uri
                context?.startActivity(intent)
                dialog.dismiss()            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    override fun copyText(text: String) {
//        setClipboard(text)
    }

    override fun onPlayOtherAudio(binding: AdapterChatOtherAudioBinding, s: String, position: Int, prevActivePos:Int) {
        if(currentPlayer!=null){
            currentPosition = 0
            isPlaying=false
            binding.playIcon.visibility=View.VISIBLE
            binding.pause.visibility=View.GONE
            currentPlayer?.stop()
            currentPlayer?.release()
            handler.removeCallbacksAndMessages(null)
        }
        currentAudioPlayingPos=position
        audioPlayerBinding = binding
        val mediaPlayer = MediaPlayer()
        playAudio(s,binding.playIcon,binding.idSeekBar,binding.playerTime, mediaPlayer, binding.pause,position, prevActivePos)
        updateTimeRunnable = Runnable {
            updateSeekBar(binding.idSeekBar,mediaPlayer,position)
            handler.postDelayed(updateTimeRunnable, 1000)
        }
        currentPlayer=mediaPlayer
        playButton=binding.playIcon
        pauseButton=binding.pause
        updateButtonVisibility(position, prevActivePos)
    }

    override fun onPauseOtherAudio(binding: AdapterChatOtherAudioBinding, s: String, position: Int, prevActivePos:Int) {
        currentAudioPlayingPos=-1
        pauseAudio(binding.playIcon, binding.idSeekBar, binding.playerTime, currentPlayer!!, binding.pause, position, prevActivePos)
    }
    private fun updateButtonVisibility(activePosition: Int, prevActivePos:Int) {
        if (isAdded){
            val layoutManager = binding.chatRecycler.layoutManager as LinearLayoutManager
            layoutManager.findViewByPosition(activePosition)?.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.playIcon)?.visibility = View.GONE
            layoutManager.findViewByPosition(activePosition)?.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.pause)?.visibility = View.VISIBLE
            if (prevActivePos!=-1&&prevActivePos!=activePosition) {
                layoutManager.findViewByPosition(prevActivePos)?.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.playIcon)?.visibility = View.VISIBLE
                layoutManager.findViewByPosition(prevActivePos)?.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.pause)?.visibility = View.GONE
            }
        }
    }
    private fun updateSeekBar(idSeekBar: SeekBar, mediaPlayer: MediaPlayer, position: Int) {
        if (mediaPlayer.isPlaying) {
            currentPosition = mediaPlayer.currentPosition
            idSeekBar.progress = currentPosition
        }
    }
    private fun playAudio(output: String, playIcon: ImageView, idSeekBar: SeekBar, playerTime: TextView, mediaPlayer: MediaPlayer, pause: ImageView, position: Int, prevActivePos:Int) {

        try {
            val audioPath = output
            mediaPlayer.reset()
            mediaPlayer.setDataSource(audioPath)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                val startPosition = playbackPosition[position]
                if (startPosition != null) {
                    if (startPosition > 0 && startPosition < mediaPlayer.duration) {
                        mediaPlayer.seekTo(startPosition)
                    } else {
                        mediaPlayer.seekTo(0)
                    }
                }
                mediaPlayer.start()
                idSeekBar.max = mediaPlayer.duration
                handler.post(updateTimeRunnable)
                mediaPlayer.setOnCompletionListener {
                    pauseAudio(playIcon,idSeekBar,playerTime, mediaPlayer,pause,position, prevActivePos)
                    idSeekBar.progress = 0
                    playbackPosition[position]=0
                }
            }
            playSeekBar(idSeekBar, playerTime, mediaPlayer,position)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), getString(R.string.error_recording_playing), Toast.LENGTH_SHORT).show()
        }
    }
    private fun playSeekBar(idSeekBar: SeekBar, playerTime: TextView, mediaPlayer: MediaPlayer, position: Int) {
        idSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                try {
                    if (mediaPlayer.isPlaying) {
//                        mediaPlayer.seekTo(progress)
                        updateTextViewCurrentTime(progress, playerTime)
                    }
                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(updateTimeRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                handler.post(updateTimeRunnable)
            }
        })
    }
    private fun updateTextViewCurrentTime(progress: Int, playerTime: TextView) {
        val minutes = progress / 1000 / 60
        val seconds = (progress / 1000) % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        playerTime.text = formattedTime
    }
    private fun pauseAudio(playIcon: ImageView, idSeekBar: SeekBar, playerTime: TextView, mediaPlayer: MediaPlayer, pause: ImageView, position: Int, prevActivePos:Int) {
        mediaPlayer.pause()
        isPlaying = false
        updateButtonVisibility(position, prevActivePos)
        playIcon.visibility = View.VISIBLE
        pause.visibility = View.GONE
        updateSeekBar(idSeekBar, mediaPlayer, position)
        handler.removeCallbacks(updateTimeRunnable)
        playbackPosition[position] = mediaPlayer.currentPosition

    }


}