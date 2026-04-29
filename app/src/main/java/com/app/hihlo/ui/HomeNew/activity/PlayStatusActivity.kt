package com.app.hihlo.ui.HomeNew.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.ActivityPlayStatusBinding
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.fragment.SecondStoryFragmentDirections
import com.app.hihlo.ui.home.view_model.NewStoryViewModel
import com.app.hihlo.ui.home.view_model.StoryViewModel
import com.app.hihlo.utils.MyApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import kotlinx.coroutines.launch
import kotlin.toString

class PlayStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayStatusBinding

    private var player: ExoPlayer? = null
    private lateinit var storyList: ArrayList<StatusItem>

    private var currentPosition = 0
    private var isPaused = false
    private var isSinglePlayMode = false

    private val handler = Handler(Looper.getMainLooper())
    private val progressHandler = Handler(Looper.getMainLooper())
    private var imageDuration = 5000L
    private var imageRemaining = 5000L
    private var imageStartTime = 0L
    private var isKeyboardVisible = false

    private val viewModel: NewStoryViewModel by viewModels()
    private val viewModel2: StoryViewModel by viewModels()

    private val imageRunnable = Runnable {
        playNext()
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateVideoProgress()
            progressHandler.postDelayed(this, 50)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val json = intent.getStringExtra("story_list") ?: "[]"
        val type = object : TypeToken<ArrayList<StatusItem>>() {}.type
        val fullStoryList: ArrayList<StatusItem> = Gson().fromJson(json, type)

        val isSinglePlay = intent.getBooleanExtra("is_play_single", false)
        val targetUserId = intent.getStringExtra("user_id") ?: ""

        isSinglePlayMode = isSinglePlay

        if (isSinglePlay && targetUserId.isNotEmpty()) {
            val filteredList = fullStoryList.filter { it.user_id.toString() == targetUserId }
            storyList = ArrayList(filteredList)
            currentPosition = 0
            if (storyList.isEmpty()) {
                Toast.makeText(this, "No stories found for this user", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } else {
            storyList = fullStoryList
            currentPosition = intent.getIntExtra("play_position", 0)
        }

        binding.rightClickArea.setOnClickListener {
            playNext()
        }
        binding.leftClickArea.setOnClickListener {
            playPrevious()
        }
        binding.sideOptions.bringToFront()
        binding.sideOptions.setOnClickListener {
            pauseStory()
            openSideOptionsPopup(binding.sideOptions)
        }
        binding.sendEditText.setOnClickListener {
            pauseStory()
        }
        binding.sendEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) pauseStory()
        }
        binding.sendButton.setOnClickListener {
            val message = binding.sendEditText.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(this@PlayStatusActivity, "Please enter something!", Toast.LENGTH_SHORT).show()
            } else {
                sendMessage(storyList[currentPosition], message)
                binding.sendEditText.setText("")
                if (!isKeyboardVisible) resumeStory()
            }
        }
        binding.deleteButton.setOnClickListener {
            pauseStory()
            CommonUtils.showCustomDialogWithBinding(this@PlayStatusActivity, "Delete Story",
                onYes = {
                    getDelete(storyList[currentPosition].id)
                },
                onNo = {
                    resumeStory()
                }
            )
        }
        binding.userImageCardView.setOnClickListener { getClick(0) }
        playStory()
        setObserver()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val keyboardVisible = imeHeight > navHeight
            if (keyboardVisible) {
                isKeyboardVisible = true
                pauseStory()
                binding.bottomLayout.translationY = -(imeHeight - navHeight).toFloat()
            } else {
                isKeyboardVisible = false
                resumeStory()
                binding.bottomLayout.translationY = 0f
            }
            insets
        }

        var downY = 0f
        binding.root.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val diff = event.rawY - downY
                    if (diff > 0) {
                        binding.root.translationY = diff
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val diff = event.rawY - downY
                    if (diff > 250) {
                        finish()
                    } else {
                        binding.root.animate()
                            .translationY(0f)
                            .setDuration(150)
                            .start()
                    }
                }
            }
            true
        }
    }

    fun getClick(click: Int) {
        when (click) {
            0 -> {
                if (currentPosition in storyList.indices) {
                    val userId = storyList[currentPosition].user_id.toString()
                    MyApplication.isStackMode = true
                    RTVariable.IS_STATUS_PROFILE_CLICKED = true
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        // ✅ Use REORDER_TO_FRONT – brings HomeActivity to front, keeps everything else
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        putExtra("navigate_to_profile", true)
                        putExtra("profile_user_id", userId)
                        putExtra("profile_from", "secondStory")
                    }
                    startActivity(intent)
                    // ✅ Do NOT call finish()
                }
            }
        }
    }

    private fun getDelete(story_Id: Int) {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    this@PlayStatusActivity, LOGIN_DATA
                )?.payload?.authToken

                val request = StoryDeleteRequest(storyId = story_Id.toString())
                val response = RetrofitBuilder.apiService.deleteStory(token, request)
                if (response.status == 1) {
                    RTVariable.IS_STATUS_DELETED = true
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playStory() {
        if (currentPosition < 0 || currentPosition >= storyList.size) {
            finish()
            return
        }
        val item = storyList[currentPosition]

        if (currentPosition != 0) {
            viewModel.hitSeenStoryDataApi(
                "Bearer " + (Preferences.getCustomModelPreference<LoginResponse>(
                    this@PlayStatusActivity, LOGIN_DATA
                )?.payload?.authToken ?: ""),
                StorySeen(storyId = item.id.toString())
            )
        }
        if (isSinglePlayMode) {
            binding.seenLayout.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
            binding.sendButton.visibility = View.VISIBLE
            binding.sendEditText.visibility = View.VISIBLE
        } else {
            if (currentPosition == 0) {
                binding.seenLayout.visibility = View.VISIBLE
                binding.deleteButton.visibility = View.VISIBLE
                binding.sendButton.visibility = View.GONE
                binding.sendEditText.visibility = View.GONE
                binding.seenCount.text = item.seen_count.toString()
            } else {
                binding.seenLayout.visibility = View.GONE
                binding.deleteButton.visibility = View.GONE
                binding.sendButton.visibility = View.VISIBLE
                binding.sendEditText.visibility = View.VISIBLE
            }
        }
        binding.statusTime.text = if (item.created_at?.isNotEmpty() == true) CommonUtils.getTimeAgo(item.created_at) + " ago" else ""
        binding.userName.text = item.userDetail.name
        binding.userLocation.text = item.userDetail?.city + ", " + (item.userDetail?.country ?: "India")
        Glide.with(this)
            .load(item.userDetail.profile_image)
            .placeholder(R.drawable.profile_placeholder)
            .into(binding.userImage)
        releasePlayer()
        handler.removeCallbacks(imageRunnable)
        progressHandler.removeCallbacksAndMessages(null)
        resetProgress()
        if (item.asset_type == "I") {
            binding.storyImage.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE
            Glide.with(this)
                .load(item.asset_url)
                .into(binding.storyImage)
            imageRemaining = imageDuration
            startImageProgress(imageRemaining)
            handler.postDelayed(imageRunnable, imageRemaining)
        } else {
            binding.storyImage.visibility = View.GONE
            binding.playerView.visibility = View.VISIBLE
            player = ExoPlayer.Builder(this).build()
            binding.playerView.player = player
            val mediaItem = MediaItem.fromUri(Uri.parse(item.asset_url))
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            startVideoProgress()
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        playNext()
                    }
                }
            })
        }
    }

    private fun pauseStory() {
        if (isPaused) return
        isPaused = true
        player?.pause()
        val item = storyList[currentPosition]
        if (item.asset_type == "I") {
            val elapsed = System.currentTimeMillis() - imageStartTime
            imageRemaining -= elapsed
            if (imageRemaining < 0) imageRemaining = 0
        }
        handler.removeCallbacks(imageRunnable)
        progressHandler.removeCallbacksAndMessages(null)
        binding.blurBackground.isVisible = true
    }

    private fun resumeStory() {
        if (!isPaused) return
        isPaused = false
        val item = storyList[currentPosition]
        if (item.asset_type == "I") {
            startImageProgress(imageRemaining)
            handler.postDelayed(imageRunnable, imageRemaining)
        } else {
            player?.play()
            startVideoProgress()
        }
        binding.blurBackground.isVisible = false
    }

    private fun playNext() {
        currentPosition++
        if (currentPosition >= storyList.size) {
            finish()
        } else {
            playStory()
        }
    }

    private fun playPrevious() {
        if (isSinglePlayMode) {
            finish()
            return
        }
        if (currentPosition == 0) {
            if (storyList.isNotEmpty() && storyList[currentPosition].isStoriesUploaded) {
                finish()
            }
            return
        } else {
            val previousIndex = currentPosition - 1
            if (previousIndex >= 0 && !storyList[previousIndex].isStoriesUploaded) {
                finish()
            } else {
                currentPosition--
                playStory()
            }
        }
    }

    private fun resetProgress() {
        binding.storyProgress.progress = 0
    }

    private fun startImageProgress(duration: Long) {
        imageStartTime = System.currentTimeMillis()
        val startProgress = binding.storyProgress.progress
        progressHandler.post(object : Runnable {
            override fun run() {
                if (isPaused) return
                val elapsed = System.currentTimeMillis() - imageStartTime
                val progressToAdd = ((elapsed * (1000 - startProgress)) / duration).toInt()
                binding.storyProgress.progress = (startProgress + progressToAdd).coerceAtMost(1000)
                if (elapsed < duration) {
                    progressHandler.postDelayed(this, 50)
                }
            }
        })
    }

    private fun startVideoProgress() {
        progressHandler.post(progressRunnable)
    }

    private fun updateVideoProgress() {
        val exo = player ?: return
        val duration = exo.duration
        val current = exo.currentPosition
        if (duration > 0) {
            val progress = ((current * 1000) / duration).toInt()
            binding.storyProgress.progress = progress.coerceAtMost(1000)
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        handler.removeCallbacksAndMessages(null)
        progressHandler.removeCallbacksAndMessages(null)
    }

    private fun openSideOptionsPopup(view: View) {
        val activity = this@PlayStatusActivity
        val inflater = LayoutInflater.from(activity)
        val binding = PopupChatSideOptionsBinding.inflate(inflater)
        binding.title1.text = "Block"
        binding.title2.text = "Report"
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 20f
            showAtLocation(view, Gravity.CENTER, 0, 0)
        }
        binding.title1.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "block")
                putString("userId", storyList[currentPosition].user_id.toString())
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
            }
            bottomSheetFragment.show(activity.supportFragmentManager, "BlockBottomSheet")
        }
        binding.title2.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "flag")
                putString("userId", storyList[currentPosition].user_id.toString())
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
            }
            bottomSheetFragment.show(activity.supportFragmentManager, "FlagBottomSheet")
        }
        popupWindow.setOnDismissListener {
            resumeStory()
        }
    }

    private fun sendMessage(otherStoryData: StatusItem, message: String) {
        viewModel.hitSaveRecentChatDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(this@PlayStatusActivity, LOGIN_DATA)?.payload?.authToken,
            SaveRecentChatRequest(toUserId = otherStoryData.user_id.toString(), message = message)
        )
        sendMessage(
            (Preferences.getCustomModelPreference<LoginResponse>(this@PlayStatusActivity, LOGIN_DATA)?.payload?.userId ?: "").toString(),
            otherStoryData.user_id.toString(),
            otherStoryData.userDetail.name ?: "",
            otherStoryData.userDetail?.profile_image ?: "",
            MediaType.TEXT.name,
            message,
            "0",
            "0"
        )
    }

    private fun sendMessage(
        sender: String,
        receiver: String,
        friendName: String,
        friendImage: String,
        messageType: String,
        message: String,
        pinned: String,
        archived: String,
        url: String? = null,
    ) {
        viewModel.sendMessage(sender, receiver, friendName, friendImage, messageType, message, pinned, archived, url ?: "")
    }

    private fun setObserver() {
        viewModel.seenStoryLiveData().observe(this@PlayStatusActivity) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Story Seen success: ${Gson().toJson(it)}")
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(this@PlayStatusActivity, true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        RTVariable.IS_STATUS_VIEWER_FINISHED = true
        overridePendingTransition(0, R.anim.slide_down)
    }
}