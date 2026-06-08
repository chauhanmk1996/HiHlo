package com.app.hihlo.ui.calling.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.app.hihlo.R
import com.app.hihlo.base.BaseActivity
import com.app.hihlo.databinding.ActivityOutgoingVideoCallBinding
import com.app.hihlo.model.deduct_call_coin.DeductCallCoinRequest
import com.app.hihlo.model.end_call.request.EndCallRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.save_call.SaveCallRequest
import com.app.hihlo.model.update_call_status.UpdateCallStatusRequest
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.AGORA_TOKEN
import com.app.hihlo.preferences.UserPreference.CALLER_USER_IMAGE
import com.app.hihlo.preferences.UserPreference.CALL_TYPE
import com.app.hihlo.preferences.UserPreference.CALL_USER_NAME
import com.app.hihlo.preferences.UserPreference.CHANNEL_NAME
import com.app.hihlo.preferences.UserPreference.CITY_COUNTRY
import com.app.hihlo.preferences.UserPreference.OTHER_USER_ID
import com.app.hihlo.preferences.UserPreference.USER_NAME
import com.app.hihlo.ui.calling.CallForegroundService
import com.app.hihlo.ui.calling.view_model.OutgoingCallViewModel
import com.app.hihlo.utils.ChatUtils
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.SoundPoolManager
import com.app.hihlo.utils.agora.ReusablePopupSpeaker
import com.app.hihlo.utils.broadcast.CallEndBroadcast
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

class OldOutgoingCallActivity : BaseActivity<ActivityOutgoingVideoCallBinding>(), CallEndBroadcast.CallEndCallback {
    private var mRtcEngine: RtcEngine? = null
    private val PERMISSION_ID = 12
    private val REQUESTED_PERMISSION = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
    private var uid = 0
    private var isMute: Boolean = false
    private var isSwitchcamera: Boolean = false
    private var isJoined = false
    private var remoteSurfaceView: SurfaceView? = null
    private var remoteUid: Int = 0
    private var isSwapped = false
    private var localTextureView: TextureView? = null
    private var remoteTextureView: TextureView? = null
    private var agoraToken: String? = null
    private var channelName: String? = null
    private var callType: String? = null
    private var callerName: String? = null
    private var otherUserId: String? = null
    private var callerImage: String? = null
    private var userName: String? = null
    private var isSpeakerOn = false
    var isLocalVideoEnabled = true // track local video state manually

    private var cityCountry: String? = null
    private lateinit var callEndBroadcast: CallEndBroadcast
    var deviceToken = ""
    private var amICreator = "0"
    private var isOtherUserCreator = "0"
    private val viewModel: OutgoingCallViewModel by viewModels() // Hilt ViewModel
    var callId = 0
    private var callStartTime = 0L
    private var isBluetoothConnected = false
    private var desiredAudioRoute: String = "earpiece" // Default: "earpiece", "speaker", or "bluetooth"
    private var isRouteLocked = false // New: Lock route after user selection
    private var enforcementJob: Job? = null
    private val enforcementHandler = Handler(Looper.getMainLooper())
    private var enforcementRunnable: Runnable? = null
    private var isEnforcementActive = false // Track enforcement state
    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_outgoing_video_call
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    if (state == BluetoothProfile.STATE_CONNECTED){
                        isBluetoothConnected = true
                        desiredAudioRoute = "bluetooth"
                    }else{
                        isBluetoothConnected = false
                    }
//                    isBluetoothConnected = (state == BluetoothProfile.STATE_CONNECTED)
                    Log.i(TAG, "Bluetooth state changed: isBluetoothConnected=$isBluetoothConnected")
                    if (isBluetoothConnected && desiredAudioRoute == "bluetooth" /*&& !isRouteLocked*/) {
                        routeToBluetooth()
                    }else{
                        if(callType == "audio"){
                            desiredAudioRoute = "earpiece"
                            routeToEarpiece()
                        }else{
                            desiredAudioRoute = "speaker"
                            routeToSpeaker()
                        }
                    }
                    updateSpeakerButtonVisibility()
                }
            }
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth receiver not registered: ${e.message}")
        }
    }

    private fun updateSpeakerButtonVisibility() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isScoOn = audioManager.isBluetoothScoOn

        when {
            callType == "audio" -> {
                binding.speakerCardview.visibility = View.VISIBLE
                if (isScoOn && desiredAudioRoute == "bluetooth") {
                    binding.speaker.setImageResource(R.drawable.bluetooth_button_icon)
                } else if (desiredAudioRoute == "speaker" && isRouteLocked) {
                    binding.speaker.setImageResource(R.drawable.speaker_on)
                } else if (desiredAudioRoute == "earpiece" && isRouteLocked) {
                    binding.speaker.setImageResource(R.drawable.speaker_off)
                } else {
                    binding.speaker.setImageResource(R.drawable.speaker_off) // Fallback
                }
            }
            callType == "video" && isBluetoothConnected -> {
                binding.speakerCardview.visibility = View.VISIBLE
                if (isScoOn && desiredAudioRoute == "bluetooth") {
                    binding.speaker.setImageResource(R.drawable.bluetooth_button_icon)
                } else if (desiredAudioRoute == "speaker" && isRouteLocked) {
                    binding.speaker.setImageResource(R.drawable.speaker_on)
                } else {
                    binding.speaker.setImageResource(R.drawable.speaker_on) // Default for video
                }
            }
            else -> {
                binding.speakerCardview.visibility = View.VISIBLE
                binding.speaker.setImageResource(R.drawable.speaker_on)
            }
        }
        Log.i(TAG, "Speaker UI updated: SCO=$isScoOn, Desired=$desiredAudioRoute, Locked=$isRouteLocked")
    }
    private fun pollForScoDisconnection(callback: () -> Unit) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0
        val maxAttempts = 50 // 5 seconds at 100ms
        val pollRunnable = object : Runnable {
            override fun run() {
                if (!audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    callback()
                    Log.i(TAG, "Polling: SCO disconnected - executing callback")
                } else if (attempts < maxAttempts) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    attempts++
                    Log.i(TAG, "Polling: SCO still on (attempt $attempts)")
                    handler.postDelayed(this, 100)
                } else {
                    Log.e(TAG, "Polling: Timeout - SCO not disconnected")
                    audioManager.mode = AudioManager.MODE_NORMAL
                    handler.postDelayed({
                        audioManager.mode = AudioManager.MODE_IN_CALL
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                        callback()
                    }, 500)
                }
            }
        }
        // Preemptive SCO stop
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        pollRunnable.run()
    }
    private fun routeToSpeaker() {
        desiredAudioRoute = "speaker"
        isRouteLocked = true
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL // Break Bluetooth priority
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_IN_CALL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val speakerDevice = audioManager.availableCommunicationDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            speakerDevice?.let { audioManager.setCommunicationDevice(it) }
            mRtcEngine?.setEnableSpeakerphone(true)
            Log.i(TAG, "User selected speaker - setCommunicationDevice")
        } else if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            pollForScoDisconnection {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                mRtcEngine?.disableAudio()
                mRtcEngine?.enableAudio()
                mRtcEngine?.setEnableSpeakerphone(true)
                Log.i(TAG, "Polled SCO disconnect - switched to speaker with Agora reset")
                startEnforcementTimer()
            }
            Log.i(TAG, "Stopping SCO for speaker - polling for confirmation")
        } else {
            mRtcEngine?.disableAudio()
            mRtcEngine?.enableAudio()
            mRtcEngine?.setEnableSpeakerphone(true)
            Log.i(TAG, "Direct route to speaker (no SCO active)")
            startEnforcementTimer()
        }
        updateSpeakerButtonVisibility()
    }
    private fun routeToEarpiece() {
        desiredAudioRoute = "earpiece"
        isRouteLocked = true
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL // Break Bluetooth priority
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_IN_CALL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val earpieceDevice = audioManager.availableCommunicationDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            earpieceDevice?.let { audioManager.setCommunicationDevice(it) }
            mRtcEngine?.setEnableSpeakerphone(false)
            Log.i(TAG, "User selected earpiece - setCommunicationDevice")
        } else if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            pollForScoDisconnection {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                mRtcEngine?.disableAudio()
                mRtcEngine?.enableAudio()
                mRtcEngine?.setEnableSpeakerphone(false)
                Log.i(TAG, "Polled SCO disconnect - switched to earpiece with Agora reset")
                startEnforcementTimer()
            }
            Log.i(TAG, "Stopping SCO for earpiece - polling for confirmation")
        } else {
            mRtcEngine?.disableAudio()
            mRtcEngine?.enableAudio()
            mRtcEngine?.setEnableSpeakerphone(false)
            Log.i(TAG, "Direct route to earpiece (no SCO active)")
            startEnforcementTimer()
        }
        updateSpeakerButtonVisibility()
    }
    private fun startEnforcementTimer() {
        if (isEnforcementActive) return
        isEnforcementActive = true
        enforcementRunnable?.let { enforcementHandler.removeCallbacks(it) }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        enforcementRunnable = object : Runnable {
            var elapsed = 0L
            override fun run() {
                if (isRouteLocked && audioManager.isBluetoothScoOn && desiredAudioRoute != "bluetooth") {
                    audioManager.mode = AudioManager.MODE_NORMAL
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.mode = AudioManager.MODE_IN_CALL
                    mRtcEngine?.disableAudio()
                    mRtcEngine?.enableAudio()
                    mRtcEngine?.setEnableSpeakerphone(desiredAudioRoute == "speaker")
                    Log.i(TAG, "Enforcement: Stopped unwanted SCO reconnection, route=$desiredAudioRoute")
                    updateSpeakerButtonVisibility()
                }
                elapsed += 500
                if (elapsed < 20000) { // 20 seconds
                    enforcementHandler.postDelayed(this, 500) // Check every 500ms
                } else {
                    isEnforcementActive = false
                    Log.i(TAG, "Enforcement timer stopped")
                }
            }
        }
        enforcementHandler.postDelayed(enforcementRunnable!!, 0)
    }
    private fun routeToBluetooth() {
        desiredAudioRoute = "bluetooth"
        isRouteLocked = true
        isEnforcementActive = false // Stop enforcement
        enforcementRunnable?.let { enforcementHandler.removeCallbacks(it) }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val btDevice = audioManager.availableCommunicationDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            btDevice?.let { audioManager.setCommunicationDevice(it) }
        }
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        mRtcEngine?.disableAudio()
        mRtcEngine?.enableAudio()
        mRtcEngine?.setEnableSpeakerphone(false)
        Log.i(TAG, "Initiating route to Bluetooth")
        updateSpeakerButtonVisibility()
    }
    // New method: Periodic enforcement to prevent auto-Bluetooth reconnect
    private fun startRouteEnforcement() {
        enforcementJob?.cancel()
        enforcementJob = CoroutineScope(Dispatchers.Main).launch {
            repeat(30) {  // Run for 30 seconds
                val audioManager = getSystemService(AudioManager::class.java)
                if (desiredAudioRoute != "bluetooth" && audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    Log.i(TAG, "Enforcement: Forced SCO off to maintain $desiredAudioRoute")
                }
                delay(1000L)
            }
        }
    }

    private fun checkSelfPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSION[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSION[1]) == PackageManager.PERMISSION_GRANTED)
    }

    private val backgroundReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isInPictureInPictureMode) {
                return
            }
            finishAndRemoveTask()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("callStartTime", callStartTime)
        outState.putBoolean("isCallJoined", isJoined)
        Log.i(TAG, "onSaveInstanceState: $isLocalVideoEnabled")
        outState.putBoolean("isLocalVideoEnabled", isLocalVideoEnabled)
        outState.putInt("remoteUid", remoteUid)
        outState.putInt("callId", callId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        callStartTime = savedInstanceState.getLong("callStartTime", 0L)
        isJoined = savedInstanceState.getBoolean("isCallJoined", false)
        isLocalVideoEnabled = savedInstanceState.getBoolean("isLocalVideoEnabled", true)
        Log.i(TAG, "onRestoreInstanceState: $isLocalVideoEnabled")
        remoteUid = savedInstanceState.getInt("remoteUid")
        callId = savedInstanceState.getInt("callId")
        Log.e("TAG", "onRestoreInstanceState: $callType $callId")
        if (callType == "video") {
            setUpRemoteVideo(remoteUid)
        } else {
            mRtcEngine?.disableVideo()
        }
        if (isJoined && callStartTime > 0) {
            binding.callTime.base = callStartTime
            binding.callTime.start()
        }
        if (!isLocalVideoEnabled) {
            mRtcEngine?.muteLocalVideoStream(true)
            mRtcEngine?.enableLocalVideo(false)
            localTextureView?.visibility = View.GONE
            binding.videoIcon.setImageResource(R.drawable.video_off_icon)
        }
        if (isInPictureInPictureMode) {
            binding.minimiseIconCardView.isVisible = false
        } else {
            binding.minimiseIconCardView.isVisible = true
        }
    }

    override fun onCallEnd() {
        Log.i(TAG, "onCallEnd: ")
        hitOnlineStatusUpdateApi(1)
        stopRinging()
        if (!isJoined) {
            Toast.makeText(this, "Call Declined", Toast.LENGTH_SHORT).show()
        }
        finishAndRemoveTask()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        val intentFilterBackground = IntentFilter("app_background_action")
        registerReceiver(backgroundReceiver, intentFilterBackground, RECEIVER_NOT_EXPORTED)
        val intentFilter = IntentFilter()
        intentFilter.addAction("callEnd")
        registerReceiver(callEndBroadcast, intentFilter, RECEIVER_NOT_EXPORTED)
        Log.i(TAG, "onResume: ")
        UserPreference.IS_CALLING_SCREEN_OPENED = true
        this.window?.let { window ->
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black_1c1c1c)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, PERMISSION_ID)
        }
        callEndBroadcast = CallEndBroadcast(this)
        agoraToken = intent?.getStringExtra(AGORA_TOKEN).toString()
        callerName = intent?.getStringExtra(CALL_USER_NAME).toString()
        channelName = intent?.getStringExtra(CHANNEL_NAME).toString()
        callType = intent?.getStringExtra(CALL_TYPE).toString()
        otherUserId = intent?.getStringExtra(OTHER_USER_ID).toString()
        callerImage = intent?.getStringExtra(CALLER_USER_IMAGE).toString()
        amICreator = intent?.getStringExtra("amICreator").toString()
        isOtherUserCreator = intent?.getStringExtra("isOtherUserCreator").toString()
        userName = intent?.getStringExtra(USER_NAME).toString()
        cityCountry = intent?.getStringExtra(CITY_COUNTRY).toString()
        Log.i(TAG, "onCreate: isCreatorStatus $amICreator $isOtherUserCreator")
        binding.personName.text = callerName
        binding.tvCityCountry.text = cityCountry
        binding.callTime.text = "Ringing..."
        if (!isNetworkAvailable()) {
            showMessage("No Internet Connection")
            return
        } else {
            initializeRtcEngine()
        }
        mute()
        deviceToken = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(this, LOGIN_DATA)?.payload?.authToken
        if (callType == "video") {
            switchCamera()
            setupDoubleTapListeners()
        } else {
            binding.userImageCardView.isVisible = true
            Glide.with(this).load(callerImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(binding.userImage)
        }
        clickListener()
        overLayPermission()
        joinCall()
        setObserver()
        hitOnlineStatusUpdateApi(2)
        if (UserPreference.CALL_ID == "CALL_ID") {
            viewModel.hitSaveCallDataApi(token = deviceToken, request = SaveCallRequest(call_type = "video", receiver_id = otherUserId?.toIntOrNull(), status = "ongoing", started_at = ChatUtils.getFormattedDateToday("yyyy-MM-dd"), ended_at = null))
        }
    }

    private fun setObserver() {
        viewModel.getEndCallLiveData().observe(this) { response ->
            Log.i(TAG, "setObserver: endcall response $response")
            when (response.status) {
                Status.LOADING -> ProcessDialog.showDialog(this, true)
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    val data = response.data
                    if (data?.status == 1 && data.code == 200) {
                        mRtcEngine?.leaveChannel()
                        mRtcEngine?.stopPreview()
                        RtcEngine.destroy()
                        mRtcEngine = null
                        finishAndRemoveTask()
                    } else {
                        Toast.makeText(this, data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(this, response.message ?: "Error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.getUpdateOnlineStatusLiveData().observe(this) { response ->
            Log.i(TAG, "setObserver: updateOnlineStatus response $response")
            when (response.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> ProcessDialog.dismissDialog(true)
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(this, response.message ?: "Error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.getSaveCallLiveData().observe(this) { response ->
            Log.i(TAG, "setObserver: save call $response")
            when (response.status) {
                Status.LOADING -> ProcessDialog.showDialog(this, true)
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    val data = response.data
                    if (data?.status == 1 && data.code == 200) {
                        callId = response.data.payload.call_id
                        UserPreference.CALL_ID = callId.toString()
                        Log.i(TAG, "setObserver: response $callId")
                    } else {
                        Toast.makeText(this, data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(this, response.message ?: "Error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.getUpdateCallStatusLiveData().observe(this) { response ->
            Log.i(TAG, "setObserver: updateCallStatus response $response")
            when (response.status) {
                Status.LOADING -> ProcessDialog.showDialog(this, true)
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    val data = response.data
                    if (data?.status == 1 && data.code == 200) {
                        hitCoinDeduction()
                    } else {
                        Toast.makeText(this, data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(this, response.message ?: "Error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.getCoinDeductionLiveData().observe(this) { response ->
            Log.i(TAG, "setObserver: coin deduction response $response")
            when (response.status) {
                Status.LOADING -> ProcessDialog.showDialog(this, true)
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    val data = response.data
                    if (data?.status == 1 && data.code == 200) {
                        if ((data.payload.totalCoins ?: 0) < (data.payload.requiredCoins ?: 0)) {
                            leave()
                        }
                    } else {
                        Toast.makeText(this, data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(this, response.message ?: "Error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun overLayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("EXTRA_FINISH", false) == true) {
            stopCallService()
            finishAndRemoveTask()
        }
    }

    private fun startCallService() {
        Log.e("TAG", "startCallService: $callStartTime")
        val intent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_START
            putExtra(CallForegroundService.EXTRA_USER_NAME, callerName)
            putExtra(CallForegroundService.EXTRA_PROFILE_URL, callerImage)
            putExtra(CallForegroundService.CALL_TYPE, callType)
            putExtra(CallForegroundService.Call_TIME, callStartTime.toString())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopCallService() {
        val intent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_STOP
        }
        startService(intent)
    }

    /*private fun clickListener() {
        binding.videoIcon.setOnClickListener {
            if (isLocalVideoEnabled) {
                mRtcEngine?.muteLocalVideoStream(true)
                mRtcEngine?.enableLocalVideo(false)
                mRtcEngine?.stopPreview()
                isLocalVideoEnabled = false
                binding.videoIcon.setImageResource(R.drawable.video_off_icon)
            } else {
                mRtcEngine?.enableLocalVideo(true)
                mRtcEngine?.muteLocalVideoStream(false)
                mRtcEngine?.startPreview()

                isLocalVideoEnabled = true

                binding.videoIcon.setImageResource(R.drawable.video_button_icon)
            }
        }

        binding.minimiseIconCardView.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i(TAG, "onBackPressed: pip")
                enterPipModeWithCloseAction()
            } else {
                Log.i(TAG, "onBackPressed: hitEndcall")
                super.onBackPressed()
            }
        }

        binding.speaker.setOnClickListener {
            isRouteLocked = false // Unlock before showing popup
            when {
                callType == "audio" && isBluetoothConnected -> {
                    ReusablePopupSpeaker(
                        context = this,
                        anchorView = binding.speaker,
                        option1Text = "Bluetooth",
                        option2Text = "Speaker",
                        option3Text = "Earpiece",
                        option1ImageRes = R.drawable.ic_bluetooth,
                        option2ImageRes = R.drawable.ic_speaker,
                        option3ImageRes = R.drawable.ic_mobile,
                        onOption1Click = { routeToBluetooth() },
                        onOption2Click = { routeToSpeaker() },
                        onOption3Click = { routeToEarpiece() },
                        selectedOption = if (desiredAudioRoute=="bluetooth") 1 else if (desiredAudioRoute=="speaker") 2 else 3

                    ).show()
                }
                callType == "audio" && !isBluetoothConnected -> {
                    if(desiredAudioRoute=="earpiece"){
                        desiredAudioRoute = "speaker"
                        isRouteLocked = true
                        mRtcEngine?.setEnableSpeakerphone(true)
                        binding.speaker.setImageResource(R.drawable.speaker_on)
                        startRouteEnforcement()
                    }else{
                        desiredAudioRoute = "earpiece"
                        isRouteLocked = true
                        mRtcEngine?.setEnableSpeakerphone(false)
                        binding.speaker.setImageResource(R.drawable.speaker_off)
                        startRouteEnforcement()
                    }
//                    ReusablePopupSpeaker(
//                        context = this,
//                        anchorView = binding.speaker,
//                        option1Text = "Speaker",
//                        option2Text = "Earpiece",
//                        option1ImageRes = R.drawable.ic_speaker,
//                        option2ImageRes = R.drawable.ic_mobile,
//                        onOption1Click = {
//                            desiredAudioRoute = "speaker"
//                            isRouteLocked = true
//                            mRtcEngine?.setEnableSpeakerphone(true)
//                            binding.speaker.setImageResource(R.drawable.speaker_on)
//                            startRouteEnforcement()
//                        },
//                        onOption2Click = {
//                            desiredAudioRoute = "earpiece"
//                            isRouteLocked = true
//                            mRtcEngine?.setEnableSpeakerphone(false)
//                            binding.speaker.setImageResource(R.drawable.speaker_off)
//                            startRouteEnforcement()
//                        },
//                        selectedOption = if (desiredAudioRoute=="speaker") 1 else 2
//                    ).show()
                }
                callType == "video" && isBluetoothConnected -> {
                    ReusablePopupSpeaker(
                        context = this,
                        anchorView = binding.speaker,
                        option1Text = "Speaker",
                        option2Text = "Bluetooth",
                        option1ImageRes = R.drawable.ic_speaker,
                        option2ImageRes = R.drawable.ic_bluetooth,
                        onOption1Click = { routeToSpeaker() },
                        onOption2Click = { routeToBluetooth() },
                        selectedOption = if (desiredAudioRoute=="speaker") 1 else 2
                    ).show()
                }
            }
        }

        binding.idJoinId.setOnClickListener {}
        binding.leaveBtn.setOnClickListener {
            hitOnlineStatusUpdateApi(1)
            if (isJoined) {
                leave()
            } else {
                hitEndCallApi("missed")
            }
            mRtcEngine?.leaveChannel()
        }
        binding.remoteUser.apply {
            clipToOutline = true
            background = GradientDrawable().apply {
                cornerRadius = 20.dpToPx().toFloat()
                setColor(Color.BLACK)
            }
        }
        binding.localUser.setOnClickListener {
            toggleBottomLayoutVisibility()
        }
    }*/

    private fun hitEndCallApi(status: String) {
        viewModel.hitEndCallDataApi(deviceToken, EndCallRequest(callId = callId.toString(), status = status, receiverId = otherUserId.toString()))
    }

    private fun toggleBottomLayoutVisibility() {
        if (binding.layoutBottom.visibility == View.VISIBLE) {
            binding.layoutBottom.animate()
                .translationY(binding.layoutBottom.height.toFloat())
                .setDuration(300)
                .withEndAction {
                    binding.layoutBottom.visibility = View.GONE
                    binding.layoutBottom.translationY = 0f
                }
                .start()
        } else {
            binding.layoutBottom.visibility = View.VISIBLE
            binding.layoutBottom.translationY = binding.layoutBottom.height.toFloat()
            binding.layoutBottom.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDoubleTapListeners() {
        binding.remoteUser.setOnTouchListener(object : View.OnTouchListener {
            private var dX = 0f
            private var dY = 0f
            private val gestureDetector = GestureDetector(this@OldOutgoingCallActivity, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d(TAG, "Double Tap Detected on Remote User")
                    showMessage("Double Tap Detected on Remote User")
                    swapUserViews()
                    return true
                }
            })

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newX = event.rawX + dX
                        val newY = event.rawY + dY
                        val parent = view.parent as View
                        val parentWidth = parent.width
                        val parentHeight = parent.height
                        val viewWidth = view.width
                        val viewHeight = view.height
                        val clampedX = newX.coerceIn(0f, (parentWidth - viewWidth).toFloat())
                        val clampedY = newY.coerceIn(0f, (parentHeight - viewHeight).toFloat())
                        view.animate()
                            .x(clampedX)
                            .y(clampedY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
        })
    }

    /*private fun setUpLocalVideo() {
        if (localTextureView == null) {
            localTextureView = TextureView(baseContext)
        } else {
            (localTextureView?.parent as? ViewGroup)?.removeView(localTextureView)
        }
        if (localTextureView == null) {
            showMessage("Failed to initialize local video")
            return
        }
        binding.remoteUser.removeAllViews()
        val layoutParams = if (isSwapped) {
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } else {
            FrameLayout.LayoutParams(120.dpToPx(), 160.dpToPx())
        }
        localTextureView?.layoutParams = layoutParams
        binding.remoteUser.addView(localTextureView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }*/

    /*private fun setUpRemoteVideo(uid: Int) {
        Log.d(TAG, "Setting up remote video for UID: $uid")
        if (remoteTextureView == null) {
            Log.d(TAG, "Initializing remoteTextureView")
            remoteTextureView = TextureView(this).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        } else {
            Log.d(TAG, "Reusing existing remoteTextureView")
            (remoteTextureView?.parent as? ViewGroup)?.removeView(remoteTextureView)
        }
        if (remoteTextureView == null) {
            Log.e(TAG, "remoteTextureView is null after initialization!")
            showMessage("Failed to initialize remote video")
            return
        }
        remoteTextureView?.visibility = View.VISIBLE
        binding.localUser.removeAllViews()
        binding.localUser.addView(remoteTextureView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }*/

    /*private fun swapUserViews() {
        if (remoteTextureView == null || localTextureView == null) {
            Log.e(TAG, "swapUserViews: remoteTextureView or localTextureView is null")
            return
        }
        val localParent = binding.localUser
        val remoteParent = binding.remoteUser
        (localTextureView?.parent as? ViewGroup)?.removeView(localTextureView)
        (remoteTextureView?.parent as? ViewGroup)?.removeView(remoteTextureView)
        if (isSwapped) {
            localParent.addView(remoteTextureView, matchParentParams)
            remoteParent.addView(localTextureView, FrameLayout.LayoutParams(120.dpToPx(), 160.dpToPx()))
        } else {
            localParent.addView(localTextureView, matchParentParams)
            remoteParent.addView(remoteTextureView, FrameLayout.LayoutParams(120.dpToPx(), 160.dpToPx()))
        }
        isSwapped = !isSwapped
        mRtcEngine?.setupLocalVideo(VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteTextureView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
        showMessage("Switched Views")
    }*/

    fun hitOnlineStatusUpdateApi(liveStatusId: Int) {
        viewModel.hitUpdateOnlineStatusDataApi(deviceToken, liveStatusId = liveStatusId)
    }

    fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    val matchParentParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

    private fun switchCamera() {
        binding.switchCamera.setOnClickListener {
            if (isSwitchcamera) {
                isSwitchcamera = false
                try {
                    mRtcEngine?.switchCamera()
                } catch (e: Exception) {
                    showMessage("Error switching camera: ${e.message}")
                }
                binding.switchCamera.animate().rotationBy(360f).setDuration(300).start()
            } else {
                try {
                    mRtcEngine?.switchCamera()
                } catch (e: Exception) {
                    showMessage("Error switching camera: ${e.message}")
                }
                binding.switchCamera.animate().rotationBy(360f).setDuration(300).start()
                isSwitchcamera = true
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun mute() {
        binding.mute.setOnClickListener {
            if (isMute) {
                isMute = false
                mRtcEngine?.muteLocalAudioStream(false)
                binding.mute.setImageResource(R.drawable.mute_icon_with_round_border)
            } else {
                isMute = true
                mRtcEngine?.muteLocalAudioStream(true)
                binding.mute.setImageResource(R.drawable.mute_icon_red)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSION[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSION[1]) == PackageManager.PERMISSION_GRANTED) {
            } else {
                Log.d(TAG, "onRequestPermissionsResult: pop up for sendto setting")
            }
        }
    }

    private fun leave() {
        mRtcEngine?.leaveChannel()
        isJoined = false
        stopCallService()
        finishAndRemoveTask()
    }

    private fun joinCall() {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            }
            setUpLocalVideo()
            if (callType == "video") {
                binding.switchCamera.isVisible = true
            } else {
                binding.remoteUser.isVisible = false
                binding.switchCamera.isVisible = false
            }
            if (localTextureView == null) {
                Log.e(TAG, "localTextureView is null in joinCall!")
                showMessage("Failed to initialize local video")
                return
            }
            mRtcEngine?.startPreview()
            Log.i(TAG, "joinCall outgoing: $agoraToken")
            Log.i(TAG, "joinCall: $channelName")
            val result = mRtcEngine?.joinChannel(agoraToken, channelName, Preferences.getCustomModelPreference<LoginResponse>(this@OldOutgoingCallActivity, LOGIN_DATA)?.payload?.userId?.toInt() ?: 0, options)
            Log.d(TAG, "joinChannel Result: $result")
            if (result != 0) {
                showMessage("Join channel failed: Error Code $result")
            } else {
                showMessage("Joining channel...")
            }
            binding.localUser.visibility = View.VISIBLE
//            binding.remoteUser.visibility = View.VISIBLE
            binding.idJoinId.visibility = View.GONE
            if (isInPictureInPictureMode) {
                if (callType == "video") {
                    binding.personName.visibility = View.GONE
                    binding.callTime.visibility = View.GONE
                    binding.remoteUser.visibility = View.VISIBLE
                    binding.switchCamera.visibility = View.VISIBLE
                } else {
                    binding.personName.visibility = View.VISIBLE
                    binding.callTime.visibility = View.VISIBLE
                    binding.remoteUser.visibility = View.GONE
                    binding.switchCamera.visibility = View.GONE
                }
                Glide.with(this).load(callerImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(binding.userImage)
                binding.layoutBottom.visibility = View.GONE
                binding.muteCardview.visibility = View.GONE
                binding.switchCamera.visibility = View.GONE
                binding.leaveBtn.visibility = View.GONE
                binding.tvCityCountry.visibility = View.GONE
                adjustRemoteVideoPosition(isInPictureInPictureMode)
            } else {
                binding.personName.visibility = View.VISIBLE
                binding.callTime.visibility = View.VISIBLE
                Glide.with(this).load(callerImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(binding.userImage)
                binding.layoutBottom.visibility = View.VISIBLE
                binding.muteCardview.visibility = View.VISIBLE
                if (callType != "audio") {
                    binding.switchCamera.visibility = View.VISIBLE
                    binding.videoIconCardview.visibility = View.VISIBLE
                    binding.tvCityCountry.visibility = View.GONE
                } else {
                    binding.switchCamera.visibility = View.GONE
                    binding.videoIconCardview.visibility = View.GONE
                    binding.tvCityCountry.visibility = View.VISIBLE
                }
                binding.leaveBtn.visibility = View.VISIBLE
            }
        } else {
            showMessage("Permission not granted")
        }
    }

    private fun adjustRemoteVideoPosition(isPip: Boolean) {
        val params = binding.remoteUser.layoutParams as ConstraintLayout.LayoutParams

        if (isPip) {
            params.width = CommonUtils.dpToPx(60)
            params.height = CommonUtils.dpToPx(80)
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.setMargins(0, 0, CommonUtils.dpToPx(8), CommonUtils.dpToPx(8))

            // Apply layout params immediately
            binding.remoteUser.layoutParams = params

            // Do a controlled rebind to let Agora recalculate camera output
            Handler(Looper.getMainLooper()).post {
                performPipRebind()
            }
        } else {
            params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            params.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.setMargins(0, 0, 0, 0)

            binding.remoteUser.layoutParams = params

            Handler(Looper.getMainLooper()).post {
                performRestoreRebind()
            }
        }
    }
    private fun performPipRebind() {
        if (localTextureView == null || remoteTextureView == null) return

        try {
            mRtcEngine?.stopPreview()
        } catch (e: Exception) { /* ignore */ }

        safeRemoveFromParent(localTextureView)

        // add localTextureView into the small pip container (binding.remoteUser)
        binding.remoteUser.addView(localTextureView, matchParentParams)

        // 👉 Use RENDER_MODE_HIDDEN for edge-to-edge view
        mRtcEngine?.setupLocalVideo(
            VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                mRtcEngine?.startPreview()
            } catch (e: Exception) { /* ignore */ }
        }, 120)
    }
    private fun performRestoreRebind() {
        if (localTextureView == null || remoteTextureView == null) return

        try {
            mRtcEngine?.stopPreview()
        } catch (e: Exception) { /* ignore */ }

        safeRemoveFromParent(localTextureView)
//        safeRemoveFromParent(remoteTextureView)

        // remote full screen again
        binding.remoteUser.addView(remoteTextureView, matchParentParams)

        // local small preview back to its original corner
//        val smallParams = FrameLayout.LayoutParams(dpToPx(120), dpToPx(160))
//        binding.localUser.addView(localTextureView, smallParams)

        mRtcEngine?.setupLocalVideo(VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_FIT, uid))
//        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteTextureView, VideoCanvas.RENDER_MODE_FIT, remoteUid))

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                mRtcEngine?.startPreview()
            } catch (e: Exception) { /* ignore */ }
        }, 120)
    }
    private fun safeRemoveFromParent(view: View?) {
        try {
            (view?.parent as? ViewGroup)?.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "safeRemoveFromParent: ${e.message}")
        }
    }


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun startRinging() = SoundPoolManager.getInstance(this).playRinging()
    private fun stopRinging() = SoundPoolManager.getInstance(this).stopRinging()

    private fun initializeRtcEngine() {
        if (mRtcEngine == null) {
            val agoraAppId = Preferences.getCustomModelPreference<LoginResponse>(this@OldOutgoingCallActivity, LOGIN_DATA)?.payload?.AGORA_DETAILS?.AGORA_APP_ID
            startRinging()
            try {
                val config = RtcEngineConfig().apply {
                    mContext = applicationContext
                    mAppId = agoraAppId
                    mEventHandler = mRtcEventHandler
                }
                mRtcEngine = RtcEngine.create(config)
                if (callType == "audio") {
                    mRtcEngine?.disableVideo()
                    mRtcEngine?.enableAudio()
                    desiredAudioRoute = "earpiece"
                } else {
                    mRtcEngine?.enableVideo()
                    desiredAudioRoute = "speaker"
                }
                // Initial Bluetooth check
                checkInitialBluetoothState()
                if (isBluetoothConnected) {
                    routeToBluetooth()
                    mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(desiredAudioRoute == "speaker")  // Prefer speaker if selected
                } else {
                    if (callType=="audio") {
                        mRtcEngine?.setEnableSpeakerphone(false)
                    }else{
                        mRtcEngine?.setEnableSpeakerphone(true)
                    }
                }
                updateSpeakerButtonVisibility()
            } catch (e: Exception) {
                showMessage("Error initializing RTC engine: ${e.message}")
                throw RuntimeException("Error initializing RTC engine: ${e.message}")
            }
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            runOnUiThread {
                Log.d(TAG, "Successfully joined channel: $channel with UID: $uid")
                binding.callTime.isVisible = true
                binding.muteCardview.isVisible = true
            }
        }

        override fun onError(err: Int) {
            super.onError(err)
            runOnUiThread {
                Log.e(TAG, "Agora Error: $err")
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            stopRinging()
            remoteUid = uid
            startChronometer(binding.callTime)
            isJoined = true
            runOnUiThread {
                hitUpdateCallStatus("started")
                binding.minimiseIconCardView.isVisible = true
                if (callType == "audio") {
                    startCallService()
                } else {
                    binding.userImageCardView.isVisible = false
                    binding.tvCityCountry.isVisible = false
                    binding.videoIconCardview.isVisible = true
                    setUpRemoteVideo(uid)
                }
                Log.i("TAG", "onUserJoined: $uid")
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            runOnUiThread {
                showMessage("User offline: $uid")
                if (remoteSurfaceView != null) {
                    binding.remoteUser.removeAllViews()
                    remoteSurfaceView = null
                }
                finishAndRemoveTask()
                mRtcEngine?.leaveChannel()
                stopCallService()
            }
        }
    }

    private fun hitUpdateCallStatus(status: String) {
        viewModel.hitUpdateCallStatusDataApi(deviceToken, UpdateCallStatusRequest(call_id = callId, status = status))
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause: ")
        UserPreference.IS_CALLING_SCREEN_OPENED = false
        if (isJoined) {
            showMessage("Your call is still active")
        }
    }

    private fun startChronometer(mChronometer: Chronometer) {
        val stoppedMilliseconds: Int
        mChronometer.base = SystemClock.elapsedRealtime()
        val chronoText = mChronometer.text.toString()
        val array = chronoText.split(":".toRegex()).toTypedArray()
        stoppedMilliseconds = if (array.size == 2) {
            Integer.parseInt((array[0].toInt() * 60 * 1000).toString()) + Integer.parseInt((array[1].toInt() * 1000).toString())
        } else {
            Integer.parseInt((array[0].toInt() * 60 * 60 * 1000).toString()) + Integer.parseInt(
                (array[1].toInt() * 60 * 1000).toString() + Integer.parseInt(
                    (array[2].toInt() * 1000).toString()
                )
            )
        }
        mChronometer.base = SystemClock.elapsedRealtime() - stoppedMilliseconds
        mChronometer.start()
        callStartTime = mChronometer.base
        mChronometer.setOnChronometerTickListener { chronometer ->
            val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
            val seconds = elapsedMillis / 1000
            if (seconds % 60 == 0L && seconds != 0L) {
                Log.i("Chronometer", "One minute passed: $seconds seconds")
                hitCoinDeduction()
            }
        }
    }

    private fun hitCoinDeduction() {
        if (amICreator != "1") {
            viewModel.hitCoinDeductionDataApi(deviceToken, DeductCallCoinRequest(callId, callType ?: "", otherUserId?.toIntOrNull()))
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
        isEnforcementActive = false
        enforcementRunnable?.let { enforcementHandler.removeCallbacks(it) }
        try {
            unregisterReceiver(backgroundReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        endCallAndRelease()
    }
    override fun onStop() {
        Log.e("TAG", "onStop:call ")
        super.onStop()
        try {
            unregisterReceiver(callEndBroadcast)
            unregisterBluetoothReceiver()
            unregisterScoReceiver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStart() {
        super.onStart()
        registerBluetoothReceiver()
        registerScoReceiver()
        checkInitialBluetoothState()
        updateSpeakerButtonVisibility()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkInitialBluetoothState() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothConnected = bluetoothAdapter != null &&
                bluetoothAdapter.isEnabled &&
                bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
    }

    private fun endCallAndRelease() {
        try {
            if (!isInPictureInPictureMode) {
                mRtcEngine?.leaveChannel()
                mRtcEngine?.stopPreview()
                RtcEngine.destroy()
                mRtcEngine = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showMessage(message: String) {}

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "onBackPressed: pip")
            enterPipModeWithCloseAction()
        } else {
            Log.i(TAG, "onBackPressed: hitEndcall")
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            Toast.makeText(this, "call disconnected", Toast.LENGTH_SHORT).show()
            finishAndRemoveTask()
            mRtcEngine?.leaveChannel()
        }
        if (callType == "video") {
            mRtcEngine?.enableVideo()
            if (localTextureView == null) setUpLocalVideo()
            if (remoteUid != 0 && remoteTextureView == null) setUpRemoteVideo(remoteUid)
        } else {
            mRtcEngine?.enableAudio()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isJoined) {
            enterPipModeWithCloseAction()
        } else {
            showMessage("Call not connected yet. Can't enter PiP.")
        }
    }

    @SuppressLint("NewApi")
    private fun enterPipModeWithCloseAction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(10, 16))
                .build()
            if (callType == "video") {
                enterPictureInPictureMode(params)
            } else {
                moveTaskToBack(true)
            }
        }
    }

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            Log.i(TAG, "SCO state changed: $state, Desired=$desiredAudioRoute, Locked=$isRouteLocked")
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (state) {
                AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                    if (desiredAudioRoute != "bluetooth" && isRouteLocked) {
                        audioManager.mode = AudioManager.MODE_NORMAL
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                        audioManager.mode = AudioManager.MODE_IN_CALL
                        mRtcEngine?.setEnableSpeakerphone(desiredAudioRoute == "speaker")
                        Log.i(TAG, "SCO connected but not desired - forcing disconnect with mode reset")
                        updateSpeakerButtonVisibility()
                    } else if (desiredAudioRoute == "bluetooth") {
                        mRtcEngine?.setEnableSpeakerphone(false)
                        Log.i(TAG, "SCO connected - routed to Bluetooth as desired")
                        updateSpeakerButtonVisibility()
                    }
                }
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                    audioManager.isBluetoothScoOn = false
                    if (!isRouteLocked) {
                        when (desiredAudioRoute) {
                            "speaker" -> {
                                mRtcEngine?.setEnableSpeakerphone(true)
                                Log.i(TAG, "SCO disconnected - switched to speaker")
                            }
                            "earpiece" -> {
                                mRtcEngine?.setEnableSpeakerphone(false)
                                Log.i(TAG, "SCO disconnected - switched to earpiece")
                            }
                        }
                    }
                    updateSpeakerButtonVisibility()
                }
                AudioManager.SCO_AUDIO_STATE_ERROR -> {
                    Log.e(TAG, "SCO error - falling back to earpiece")
                    desiredAudioRoute = "earpiece"
                    isRouteLocked = true
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    mRtcEngine?.setEnableSpeakerphone(false)
                    updateSpeakerButtonVisibility()
                    startEnforcementTimer()
                }
            }
        }
    }
    private fun registerScoReceiver() {
        val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        registerReceiver(scoReceiver, filter)
    }

    private fun unregisterScoReceiver() {
        try {
            unregisterReceiver(scoReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "SCO receiver not registered: ${e.message}")
        }
    }























    private fun clickListener() {
        binding.videoIcon.setOnClickListener {
            if (isLocalVideoEnabled) {
                mRtcEngine?.muteLocalVideoStream(true)
                mRtcEngine?.enableLocalVideo(false)
                localTextureView?.visibility = View.GONE
                isLocalVideoEnabled = false
                binding.videoIcon.setImageResource(R.drawable.video_off_icon)
            } else {
                localTextureView?.visibility = View.VISIBLE
                mRtcEngine?.enableLocalVideo(true)
                mRtcEngine?.muteLocalVideoStream(false)
                mRtcEngine?.startPreview()
                isLocalVideoEnabled = true
                binding.videoIcon.setImageResource(R.drawable.video_button_icon)
            }
        }

        binding.minimiseIconCardView.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i(TAG, "onBackPressed: pip")
                enterPipModeWithCloseAction()
            } else {
                Log.i(TAG, "onBackPressed: hitEndcall")
                super.onBackPressed()
            }
        }

        binding.speaker.setOnClickListener {
            isRouteLocked = false // Unlock before showing popup
            when {
                callType == "audio" && isBluetoothConnected -> {
                    ReusablePopupSpeaker(
                        context = this,
                        anchorView = binding.speaker,
                        option1Text = "Bluetooth",
                        option2Text = "Speaker",
                        option3Text = "Earpiece",
                        option1ImageRes = R.drawable.ic_bluetooth,
                        option2ImageRes = R.drawable.ic_speaker,
                        option3ImageRes = R.drawable.ic_mobile,
                        onOption1Click = { routeToBluetooth() },
                        onOption2Click = { routeToSpeaker() },
                        onOption3Click = { routeToEarpiece() },
                        selectedOption = if (desiredAudioRoute=="bluetooth") 1 else if (desiredAudioRoute=="speaker") 2 else 3

                    ).show()
                }
                callType == "audio" && !isBluetoothConnected -> {
                    if(desiredAudioRoute=="earpiece"){
                        desiredAudioRoute = "speaker"
                        isRouteLocked = true
                        mRtcEngine?.setEnableSpeakerphone(true)
                        binding.speaker.setImageResource(R.drawable.speaker_on)
                        startRouteEnforcement()
                    }else{
                        desiredAudioRoute = "earpiece"
                        isRouteLocked = true
                        mRtcEngine?.setEnableSpeakerphone(false)
                        binding.speaker.setImageResource(R.drawable.speaker_off)
                        startRouteEnforcement()
                    }
//                    ReusablePopupSpeaker(
//                        context = this,
//                        anchorView = binding.speaker,
//                        option1Text = "Speaker",
//                        option2Text = "Earpiece",
//                        option1ImageRes = R.drawable.ic_speaker,
//                        option2ImageRes = R.drawable.ic_mobile,
//                        onOption1Click = {
//                            desiredAudioRoute = "speaker"
//                            isRouteLocked = true
//                            mRtcEngine?.setEnableSpeakerphone(true)
//                            binding.speaker.setImageResource(R.drawable.speaker_on)
//                            startRouteEnforcement()
//                        },
//                        onOption2Click = {
//                            desiredAudioRoute = "earpiece"
//                            isRouteLocked = true
//                            mRtcEngine?.setEnableSpeakerphone(false)
//                            binding.speaker.setImageResource(R.drawable.speaker_off)
//                            startRouteEnforcement()
//                        },
//                        selectedOption = if (desiredAudioRoute=="speaker") 1 else 2
//                    ).show()
                }
                callType == "video" && isBluetoothConnected -> {
                    ReusablePopupSpeaker(
                        context = this,
                        anchorView = binding.speaker,
                        option1Text = "Speaker",
                        option2Text = "Bluetooth",
                        option1ImageRes = R.drawable.ic_speaker,
                        option2ImageRes = R.drawable.ic_bluetooth,
                        onOption1Click = { routeToSpeaker() },
                        onOption2Click = { routeToBluetooth() },
                        selectedOption = if (desiredAudioRoute=="speaker") 1 else 2
                    ).show()
                }
            }
        }

        binding.idJoinId.setOnClickListener {}
        binding.leaveBtn.setOnClickListener {
            hitOnlineStatusUpdateApi(1)
            if (isJoined) {
                leave()
            } else {
                hitEndCallApi("missed")
            }
            mRtcEngine?.leaveChannel()
        }
        binding.remoteUser.apply {
            clipToOutline = true
            background = GradientDrawable().apply {
                cornerRadius = 20.dpToPx().toFloat()
                setColor(Color.BLACK)
            }
        }
        binding.localUser.setOnClickListener {
            toggleBottomLayoutVisibility()
        }
    }

    private fun setUpLocalVideo() {
        if (localTextureView == null) {
            localTextureView = TextureView(baseContext)
        } else {
            (localTextureView?.parent as? ViewGroup)?.removeView(localTextureView)
        }
        if (localTextureView == null) {
            showMessage("Failed to initialize local video")
            return
        }
        binding.remoteUser.removeAllViews()
        val layoutParams = if (isSwapped) {
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } else {
            FrameLayout.LayoutParams(120.dpToPx(), 160.dpToPx())
        }
        localTextureView?.layoutParams = layoutParams
        if (!isLocalVideoEnabled) {
            localTextureView?.visibility = View.GONE
        }
        binding.remoteUser.addView(localTextureView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun setUpRemoteVideo(uid: Int) {
        Log.d(TAG, "Setting up remote video for UID: $uid")
        if (remoteTextureView == null) {
            Log.d(TAG, "Initializing remoteTextureView")
            remoteTextureView = TextureView(this).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        } else {
            Log.d(TAG, "Reusing existing remoteTextureView")
            (remoteTextureView?.parent as? ViewGroup)?.removeView(remoteTextureView)
        }
        if (remoteTextureView == null) {
            Log.e(TAG, "remoteTextureView is null after initialization!")
            showMessage("Failed to initialize remote video")
            return
        }
        remoteTextureView?.visibility = View.VISIBLE
        binding.localUser.removeAllViews()
        binding.localUser.addView(remoteTextureView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun swapUserViews() {
        if (remoteTextureView == null || localTextureView == null) {
            Log.e(TAG, "swapUserViews: remoteTextureView or localTextureView is null")
            return
        }
        val localParent = binding.localUser
        val remoteParent = binding.remoteUser
        (localTextureView?.parent as? ViewGroup)?.removeView(localTextureView)
        (remoteTextureView?.parent as? ViewGroup)?.removeView(remoteTextureView)
        if (isSwapped) {
            localParent.addView(remoteTextureView, matchParentParams)
            remoteParent.addView(localTextureView, FrameLayout.LayoutParams(120.dpToPx(), 160.dpToPx()))
        } else {
            localParent.addView(localTextureView, matchParentParams)
            remoteParent.addView(remoteTextureView, FrameLayout.LayoutParams(120.dpToPx(), 160.dpToPx()))
        }
        isSwapped = !isSwapped
        mRtcEngine?.setupLocalVideo(VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteTextureView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
        if (!isLocalVideoEnabled) {
            localTextureView?.visibility = View.GONE
        }
        showMessage("Switched Views")
    }
}