package com.app.hihlo.ui.home.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentSecondStoryBinding
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.view_model.NewStoryViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.dpToPx
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.VideoCacheManager
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecondStoryFragment : Fragment() {
    private lateinit var binding: FragmentSecondStoryBinding
    private var storyList: ArrayList<Story> = arrayListOf()
    private var currentPage = 0
    private val delayMillis = 6000L // 6 seconds
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var exoPlayer: ExoPlayer
    private val viewModel: NewStoryViewModel by viewModels()
    private var isKeyboardVisible = false
    private var lastInsets: WindowInsetsCompat? = null

    // SeekBar management
    private var seekBarHandler: Handler? = null
    private var seekBarRunnable: Runnable? = null
    private var seekBarStartTime = 0L
    private var seekBarElapsedTime = 0L
    private var seekBarDuration = 6000L
    private var isSeekBarRunning = false

    private var isNavigating = false
    private var isVideoEnded = false
    private val navigationJob = Job()
    private val navigationScope = CoroutineScope(Dispatchers.Main + navigationJob)

    private lateinit var gestureDetector: GestureDetector
    private var myStoryData: MyStory = MyStory()
    var isMyStory: Boolean = false

    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var rootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as HomeActivity).setOnlineStatusVisibility(true)
        arguments?.let {
            storyList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isMyStory = false
                it.getParcelableArrayList("storyList", Story::class.java) ?: arrayListOf()
            } else {
                isMyStory = false
                @Suppress("DEPRECATION")
                it.getParcelableArrayList("storyList") ?: arrayListOf()
            }
            currentPage = it.getInt("position", 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isMyStory = true
                myStoryData = it.getParcelable("myStoryData", MyStory::class.java) ?: MyStory()
            }
            Log.e("TAG", "onCreate: storyList=$storyList, currentPage=$currentPage")
        } ?: run {
            storyList = arrayListOf()
            currentPage = 0
            Log.e("TAG", "onCreate: No arguments provided")
        }
        Log.i("TAG", "onCreate: $storyList")
    }

    private fun getDummyStories(): ArrayList<Story> {
        return arrayListOf(
            Story(asset_type = "V", asset_url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", id = 1),
            Story(asset_type = "V", asset_url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", id = 2),
            Story(asset_type = "I", asset_url = "https://m.media-amazon.com/images/I/511XEZbADQL._UF1000,1000_QL80_.jpg", id = 3),
            Story(asset_type = "V", asset_url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", id = 4),
            Story(asset_type = "V", asset_url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", id = 5),
            Story(asset_type = "I", asset_url = "https://m.media-amazon.com/images/I/51fY0wjRGTL._UF1000,1000_QL80_.jpg", id = 6),
            Story(asset_type = "V", asset_url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", id = 7)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSecondStoryBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50
            private val SWIPE_VELOCITY_THRESHOLD = 50

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2 != null) {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (Math.abs(diffY) > Math.abs(diffX)) {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                Log.d("Swipe", "Swipe down detected")
                                onSwipeDown()
                                return true
                            } else {
                                Log.d("Swipe", "Swipe up detected")
                                onSwipeUp()
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        binding.storyImage.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.sideOptions.bringToFront()
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> binding.videoLoader.visibility = View.VISIBLE
                    Player.STATE_READY -> binding.videoLoader.visibility = View.GONE
                    Player.STATE_ENDED -> onVideoEnd()
                }
            }
        })

        binding.playerView.setOnClickListener {
            CommonUtils.hideKeyboard(requireActivity())
        }
        binding.leftClickArea.setOnClickListener { leftClickAreaClicked() }
        binding.rightClickArea.setOnClickListener { rightClickAreaClicked() }
        binding.sideOptions.setOnClickListener { onSideOptionsClicked() }
        binding.userImageCardView.setOnClickListener { getClick(0) }
        binding.sendButton.setOnClickListener {
            val message = binding.sendEditText.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
            } else {
                sendMessage(storyList[currentPage], message)
                binding.sendEditText.setText("")
                // If keyboard is already hidden, resume manually
                if (!isKeyboardVisible) resumeStory()
            }
        }
        binding.sendEditText.setOnClickListener {
            binding.blurBackground.isVisible = true
            pauseStory()
        }
        binding.sendEditText.setOnFocusChangeListener { _, hasFocus ->
            binding.blurBackground.isVisible = true
            if (hasFocus) pauseStory()
        }
        binding.blurBackground.setOnClickListener {
            updateKeyboardVisibility(false)
            CommonUtils.hideKeyboard(requireActivity())
        }

        bindStory(currentPage)
    }

//    fun View.hideKeyboard() {
//        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        inputManager.hideSoftInputFromWindow(windowToken, 0)
//    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (storyList[currentPage].is_seen == 0 && isAdded && isVisible) {
            viewModel.hitSeenStoryDataApi(
                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                StorySeen(storyId = storyList[currentPage].id.toString())
            )
        }

        // Window insets listener
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val isKeyboardNowVisible = imeHeight > 0
            updateKeyboardVisibility(isKeyboardNowVisible)
            handleKeyboardInsets(insets, isKeyboardNowVisible)
            insets
        }

        // Fallback layout listener for keyboard visibility
        var rootView = binding.root
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisibleFallback = keypadHeight > screenHeight * 0.15
                if (isKeyboardVisibleFallback != isKeyboardVisible) {
                    updateKeyboardVisibility(isKeyboardVisibleFallback)
                }
            }
        })
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

    private fun updateKeyboardVisibility(visible: Boolean) {
        if (isKeyboardVisible == visible) return
        isKeyboardVisible = visible
        if (visible){
            pauseStory()
        } else {
            binding.blurBackground.isVisible = false
            resumeStory()
        }
    }

    private fun bindStory(position: Int) {
        if (storyList.isEmpty()) {
            findNavController().popBackStack()
            return
        }
        if (position < 0 || position >= storyList.size) {
            findNavController().popBackStack()
            return
        }
        currentPage = position
        val story = storyList[currentPage]
        binding.root.alpha = 1f
        isVideoEnded = false

        binding.apply {
            sendButton.isVisible = true
            sendEditText.isVisible = true
            seenLayout.isVisible = false
            deleteButton.isVisible = false

            rightClickArea.bringToFront()
            leftClickArea.bringToFront()
            sideOptions.bringToFront()
            playerView.isClickable = false
            playerView.setOnClickListener { }

            Glide.with(requireContext()).load(story.userDetail?.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(userImage)
            statusTime.text = if (story.created_at?.isNotEmpty() == true) CommonUtils.getTimeAgo(story.created_at) + " ago" else ""
            userName.text = story.userDetail?.name
            Log.e("userDetail", "userDetail>>> "+story.userDetail?.city+ " | "+story.userDetail?.country)
            userLocation.text = story.userDetail?.city + ", " + (story.userDetail?.country ?: "India")
            when (story.userDetail?.user_live_status) {
                "1" -> onlineStatusImage.setImageResource(R.drawable.online_status_green)
                "2", "3" -> onlineStatusImage.setImageResource(R.drawable.offline_status_red)
            }

            stopSeekBar()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            if (story.asset_type == "I") {
                storyImage.isVisible = true
                playerView.isVisible = false
                storySeekBar.isVisible = true
                videoLoader.isVisible = false
                Glide.with(requireContext()).load(story.asset_url).into(storyImage)
                storySeekBar.progress = 0
                startSeekBarProgress()
            } else {
                storyImage.isVisible = false
                playerView.isVisible = true
                storySeekBar.isVisible = false
                setupPlayer(story.asset_url ?: "")
            }
        }

        if (story.is_seen == 0 && isAdded && isVisible) {
            viewModel.hitSeenStoryDataApi(
                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                StorySeen(storyId = story.id.toString())
            )
        }
    }

    private fun navigateToNextStory(forward: Boolean) {
        if (isNavigating) return
        isNavigating = true
        val direction = if (forward) 1 else -1
        val newPos = currentPage + direction
        if (newPos < 0 || newPos >= storyList.size) {
            isNavigating = false
            if (!forward) {
                val navOptions = NavOptions.Builder().setPopUpTo(R.id.secondStoryFragment, inclusive = true).build()
                try {
                    findNavController().navigate(
                        SecondStoryFragmentDirections.actionSecondStoryFragmentToStoryFragment(
                            isMyStory = "1",
                            myStoryData = myStoryData,
                            otherStoryData = storyList.toTypedArray()
                        ), navOptions
                    )
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
                }
                return
            } else {
                findNavController().popBackStack()
                return
            }
        }
        pauseStory()
        val width = binding.root.width.toFloat()
        binding.root.translationX = if (forward) width else -width
        currentPage = newPos
        bindStory(currentPage)
        val slideAnim = ObjectAnimator.ofFloat(binding.root, "translationX", binding.root.translationX, 0f)
        val fadeOutAnim = ObjectAnimator.ofFloat(binding.root, "alpha", 1f, 0.2f)
        val fadeInAnim = ObjectAnimator.ofFloat(binding.root, "alpha", 0.2f, 1f)
        AnimatorSet().apply {
            play(slideAnim).with(fadeOutAnim).before(fadeInAnim)
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isNavigating = false
                    binding.root.alpha = 1f
                    resumeStory()
                }
            })
            start()
        }
    }

    override fun onDestroyView() {
        navigationJob.cancel()
        stopSeekBar()
        try {
            if (::exoPlayer.isInitialized) {
                exoPlayer.stop()
                exoPlayer.release()
            }
        } catch (e: Exception) {
            Log.e("ExoPlayer", "Error releasing player: ${e.message}")
        }
        super.onDestroyView()
    }

    private fun pauseStory() {
        if (!::binding.isInitialized) return
        val story = storyList.getOrNull(currentPage) ?: return
        binding.leftClickArea.isClickable = false
        binding.rightClickArea.isClickable = false
        if (story.asset_type == "I") {
            pauseSeekBar()
        } else {
            if (::exoPlayer.isInitialized) {
                exoPlayer.playWhenReady = false
            }
        }
    }

    private fun resumeStory() {
        if (!::binding.isInitialized || isKeyboardVisible) return
        val story = storyList.getOrNull(currentPage) ?: return
        binding.leftClickArea.isClickable = true
        binding.rightClickArea.isClickable = true
        if (story.asset_type == "I") {
            resumeSeekBar()
        } else {
            if (::exoPlayer.isInitialized) {
                exoPlayer.playWhenReady = true
            }
        }
    }

    private fun handleKeyboardInsets(insets: WindowInsetsCompat, isKeyboardVisible: Boolean) {
        val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        var finalHeight = imeHeight
        if (isKeyboardVisible) {
            finalHeight = if (isGestureNavigation()) imeHeight else imeHeight - dpToPx(40)
        } else {
            finalHeight = imeHeight + dpToPx(20)
        }
        binding.bottomLayout.setPadding(0, 0, 0, finalHeight)
    }

    private fun isGestureNavigation(): Boolean {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        return resId > 0 && resources.getInteger(resId) == 2
    }

    private fun stopSeekBar() {
        isSeekBarRunning = false
        seekBarHandler?.removeCallbacks(seekBarRunnable ?: return)
        seekBarElapsedTime = 0L
    }

    private fun startSeekBarProgress() {
        stopSeekBar()
        binding.storySeekBar.max = 100
        binding.storySeekBar.progress = 0
        seekBarStartTime = System.currentTimeMillis()
        isSeekBarRunning = true
        seekBarDuration = delayMillis
        seekBarElapsedTime = 0L
        seekBarHandler = Handler(Looper.getMainLooper())
        seekBarRunnable = object : Runnable {
            override fun run() {
                if (!isSeekBarRunning) return
                val elapsed = System.currentTimeMillis() - seekBarStartTime
                val progress = ((elapsed.toFloat() / seekBarDuration) * 100).toInt()
                binding.storySeekBar.progress = progress.coerceAtMost(100)
                if (progress < 100) {
                    seekBarHandler?.postDelayed(this, 16)
                } else {
                    onVideoEnd()
                }
            }
        }
        seekBarHandler?.post(seekBarRunnable!!)
    }

    private fun pauseSeekBar() {
        isSeekBarRunning = false
        seekBarElapsedTime = System.currentTimeMillis() - seekBarStartTime
        seekBarHandler?.removeCallbacks(seekBarRunnable ?: return)
    }

    private fun resumeSeekBar() {
        if (!isSeekBarRunning && seekBarElapsedTime < seekBarDuration) {
            seekBarStartTime = System.currentTimeMillis() - seekBarElapsedTime
            isSeekBarRunning = true
            seekBarHandler?.post(seekBarRunnable!!)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer(videoUrl: String) {
        binding.playerView.player = exoPlayer
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.seekTo(0)
        val mediaItem = MediaItem.fromUri(videoUrl)
        val cacheFactory = VideoCacheManager.buildCacheDataSource(requireContext())
        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory).createMediaSource(mediaItem)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = !isKeyboardVisible
        binding.videoLoader.visibility = View.VISIBLE
    }

    fun onVideoEnd() {
        Log.e("onVideoEnd", "onVideoEnd:")
        if (isVideoEnded || isNavigating || isKeyboardVisible) return
        isVideoEnded = true
        if (currentPage < storyList.size - 1) {
            navigationScope.launch {
                CommonUtils.hideKeyboard(requireActivity())
                delay(80)
                navigateToNextStory(true)
            }
        } else {
            if (isAdded && view != null) {
                try {
                    findNavController().popBackStack()
                } catch (e: IllegalStateException) {
                    Log.e("NewStoryFragment", "Nav error: ${e.message}")
                }
            }
        }
    }

    fun onSwipeDown() {
        findNavController().popBackStack()
//        if (isAdded && isVisible) {
//            try {
//                findNavController().popBackStack()
//            } catch (e: IllegalStateException) {
//                Log.e("TAG", "Navigation error on swipe down: ${e.message}", e)
//            }
//        }
    }

    private fun onSwipeUp() {
        if (isKeyboardVisible) return
        pauseStory()
        binding.blurBackground.isVisible = true
        binding.sendEditText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.sendEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun onSideOptionsClicked() {
        pauseStory()
        openSideOptionsPopup()
    }

    fun leftClickAreaClicked() {
        Log.e("leftClickAreaClicked", "leftClickAreaClicked>> "+isMyStory)
        if (isNavigating) return
        navigationScope.launch {
            CommonUtils.hideKeyboard(requireActivity())
            delay(80)
            navigateToNextStory(false)
        }
    }

    fun rightClickAreaClicked() {
        Log.e("leftClickAreaClicked", "leftClickAreaClicked>> "+isMyStory)
        if (isNavigating) return
        navigationScope.launch {
            CommonUtils.hideKeyboard(requireActivity())
            delay(80)
            navigateToNextStory(true)
        }
    }

    fun getClick(click: Int) {
        when (click) {
            0 -> {
                findNavController().navigate(
                    SecondStoryFragmentDirections.actionSecondStoryFragmentToProfileFragment(
                        "0",
                        storyList[currentPage].user_id.toString(),
                        from = "secondStory"
                    )
                )
            }
        }
    }

    fun sendMessage(otherStoryData: Story, message: String) {
        viewModel.hitSaveRecentChatDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
            SaveRecentChatRequest(toUserId = otherStoryData.user_id.toString(), message = message)
        )
        sendMessage(
            (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(),
            otherStoryData.user_id.toString(),
            otherStoryData.userDetail?.name ?: "",
            otherStoryData.userDetail?.profile_image ?: "",
            MediaType.TEXT.name,
            message,
            "0",
            "0"
        )
    }

    private fun setObserver() {
        viewModel.seenStoryLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Story Seen success: ${Gson().toJson(it)}")
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {}
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    private fun openSideOptionsPopup() {
        val inflater = LayoutInflater.from(requireContext())
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
            showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        }
        binding.title1.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "block")
                putString("userId", storyList[currentPage].user_id.toString())
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
                findNavController().popBackStack()
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
        }
        binding.title2.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "flag")
                putString("userId", storyList[currentPage].user_id.toString())
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
                findNavController().popBackStack()
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "FlagBottomSheet")
        }
        popupWindow.setOnDismissListener {
            resumeStory()
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
        url: String? = null,
    ) {
        viewModel.sendMessage(sender, receiver, friendName, friendImage, messageType, message, pinned, archived, url ?: "")
    }

    override fun onPause() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.pause()
        }
        super.onPause()
    }
}