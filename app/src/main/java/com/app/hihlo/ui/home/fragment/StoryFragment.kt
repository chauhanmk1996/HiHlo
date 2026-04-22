package com.app.hihlo.ui.home.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentStoryBinding
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.login.response.Payload
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.home.view_model.StoryViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.VideoCacheManager
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
//import com.google.android.exoplayer2.ExoPlayer
//import com.google.android.exoplayer2.MediaItem
//import com.google.android.exoplayer2.Player
//import com.google.android.exoplayer2.source.ProgressiveMediaSource
//import com.google.android.exoplayer2.ui.PlayerView
import com.google.gson.Gson
import kotlin.math.abs

class StoryFragment : BaseFragment<FragmentStoryBinding>() {
    private var myDetails: Payload = Payload()
    private lateinit var isMyStory: String
    private lateinit var myStoryData: MyStory
    private lateinit var otherStoryData: List<Story>
    private val args:StoryFragmentArgs by navArgs()
    private val viewModel: StoryViewModel by viewModels()
    private lateinit var exoPlayer: ExoPlayer

    private var seekBarHandler: Handler? = null
    private var seekBarRunnable: Runnable? = null
    private var seekBarStartTime = 0L
    private var seekBarElapsedTime = 0L
    private var seekBarDuration = 6000L
    private var isSeekBarRunning = false

    private lateinit var gestureDetector: GestureDetector // Move initialization to initViews


    @SuppressLint("ClickableViewAccessibility")
    override fun initView(savedInstanceState: Bundle?) {
        isMyStory = args.isMyStory
        myStoryData = args.myStoryData
        otherStoryData = args.otherStoryData.toList()
        Log.i("TAG", "initView: "+myStoryData)
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50
            private val SWIPE_VELOCITY_THRESHOLD = 50

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2 != null) {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x

                    if (abs(diffY) > abs(diffX)) {
                        if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                Log.d("Swipe", "Swipe down detected: diffY=$diffY, velocityY=$velocityY")
                                onSwipeDown()
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })
        // Set up touch listeners for swipe down
        binding.root.setOnTouchListener { _, event ->
            Log.e("TAG", "Touch event received: ${event.action}")
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
        if (isMyStory=="1"){
            setMyStoryView()
        }else{
//            viewModel.hitSeenStoryDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, StorySeen(storyId = otherStoryData.id.toString()))
//            setOtherStoryView()
        }
        onClick()
    }
    fun onSwipeDown() {
        Log.e("TAG", "Swipe down detected, navigating back")
        if (isAdded && isVisible) {
            try {
                findNavController().popBackStack()
            } catch (e: IllegalStateException) {
                Log.e("TAG", "Navigation error on swipe down: ${e.message}", e)
            }
        } else {
            Log.e("TAG", "Fragment not in valid state for navigation: isAdded=$isAdded, isVisible=$isVisible")
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as HomeActivity).setOnlineStatusVisibility(true)
        setObserver()
    }
    private fun onClick() {
        binding.crossButton.setOnClickListener {
            (requireActivity() as HomeActivity).setOnlineStatusVisibility(false)
            findNavController().popBackStack()
        }
        binding.rightClickArea.setOnClickListener {
            findNavController().popBackStack()
            //navigateToOtherStory()
        }
        binding.deleteButton.setOnClickListener {
            pauseSeekBar() // Pause when dialog is shown
            pausePlayer()       // Pause video playback (if video)

            showCustomDialogWithBinding(requireContext(), "Delete Story",
                onYes = {
                    viewModel.hitStoryDeleteDataApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(), LOGIN_DATA
                        )?.payload?.authToken,
                        StoryDeleteRequest(storyId = myStoryData.id)
                    )
                },
                onNo = {
                    resumeSeekBar(binding.storySeekBar) // Resume if user cancels
                    resumePlayer()                       // Resume video
                }
            )
        }
//        binding.storyImage.setOnClickListener {
//            CommonUtils.hideKeyboard(requireActivity())
//        }
//        binding.playerView.setOnClickListener {
//            CommonUtils.hideKeyboard(requireActivity())
//        }
        binding.userImage.setOnClickListener {
            (requireActivity() as HomeActivity).setOnlineStatusVisibility(false)
            findNavController().navigate(StoryFragmentDirections.actionStoryFragmentToProfileFragment("1"))
        }
    }
    private fun pauseSeekBar() {
        isSeekBarRunning = false
        seekBarElapsedTime += System.currentTimeMillis() - seekBarStartTime
        seekBarHandler?.removeCallbacks(seekBarRunnable!!)
    }

    private fun resumeSeekBar(seekBar: SeekBar) {
        isSeekBarRunning = true
        seekBarStartTime = System.currentTimeMillis()
        seekBarHandler?.post(seekBarRunnable!!)
    }

    private fun pausePlayer() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    private fun resumePlayer() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    private fun setMyStoryView() {
        binding.apply {
            Log.i("TAG", "setMyStoryView: $myStoryData")
            onlineStatusImage.isVisible=false
            seenCount.text = myStoryData.seen_count.toString()
            sendButton.isVisible=false
            sendEditText.isVisible=false
            seenLayout.isVisible=true
            deleteButton.isVisible=true
            myDetails = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload ?: Payload()
            Glide.with(requireContext()).load(myDetails.profileImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
            userName.text = myDetails.fullName
            statusTime.text = CommonUtils.getTimeAgo(myStoryData.created_at ?: "")+" ago"
            userLocation.text = myDetails.city+", "+(myDetails?.country ?: "")
            if (myStoryData.asset_type=="I"){
                storyImage.isVisible=true
                playerView.isVisible=false
                Glide.with(requireContext()).load(myStoryData.url).into(storyImage)
                startSeekBarProgress(binding.storySeekBar, 6000)
            }else{
                playerView.isVisible=true
                storyImage.isVisible=false
                storySeekBar.isVisible=false
                setupPlayer(myStoryData.url ?: "")
            }
        }
    }
    private fun setOtherStoryView(){
//        binding.apply {
//            sendButton.isVisible=true
//            sendEditText.isVisible=true
//            seenLayout.isVisible=false
//            deleteButton.isVisible=false
//            Glide.with(requireContext()).load(otherStoryData.userDetail?.profile_image).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
//            userName.text = otherStoryData.userDetail?.username
//            userLocation.text = otherStoryData.userDetail?.city+", "+(otherStoryData.userDetail?.country ?: "")
//            if (otherStoryData.asset_type=="I"){
//                storyImage.isVisible=true
//                playerView.isVisible=false
//                Glide.with(requireContext()).load(otherStoryData.asset_url).into(storyImage)
//                startSeekBarProgress(binding.storySeekBar, 15000)
//            }else{
//                playerView.isVisible=true
//                storyImage.isVisible=false
//                storySeekBar.isVisible=false
//                setupPlayer(otherStoryData.asset_url ?: "")
//            }
//        }
    }
    fun startSeekBarProgress(seekBar: SeekBar, durationInMillis: Long) {
        seekBarDuration = durationInMillis
        seekBar.max = 100
        seekBar.progress = 0
        seekBarStartTime = System.currentTimeMillis()
        isSeekBarRunning = true

        seekBarHandler = Handler(Looper.getMainLooper())
        seekBarRunnable = object : Runnable {
            override fun run() {
                if (!isSeekBarRunning) return  // Exit early if paused

                val elapsed = System.currentTimeMillis() - seekBarStartTime + seekBarElapsedTime
                val progress = ((elapsed.toFloat() / seekBarDuration) * 100).toInt()
                seekBar.progress = progress.coerceAtMost(100)

                if (progress < 100) {
                    seekBarHandler?.postDelayed(this, 16)
                } else {
                    if (isAdded && isVisible && view != null) {
                        //navigateToOtherStory()
                    }
                }
            }
        }
        seekBarHandler?.post(seekBarRunnable!!)
    }
    private fun navigateToOtherStory() {
        if (otherStoryData.isNotEmpty()) {
            val bundle = Bundle().apply {
                putParcelableArrayList("storyList", ArrayList(otherStoryData))
                putParcelable("myStoryData", myStoryData)
                putInt("position", 0)
            }
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.storyFragment, inclusive = true).build()
            try {
                findNavController().navigate(R.id.secondStoryFragment, bundle, navOptions)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
            }

        }else{
            findNavController().popBackStack()
        }
    }
    override fun getLayoutId(): Int {
        return R.layout.fragment_story
    }
    private fun setObserver() {
        viewModel.seenStoryLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Story Seen success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){

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
        viewModel.getStoryDeleteLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Story Delete success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            showCustomDialogWithBinding(requireContext(), "Story Deleted \nSuccessfully!",
                                onYes = {},
                                onNo = {},
                                showButtons = false,
                                autoDismissInMillis = 1000
                            )
                            findNavController().popBackStack()
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

    @OptIn(UnstableApi::class)
    private fun setupPlayer(videoUrl: String) {
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = exoPlayer

        // Required settings
//        binding.playerView.controllerAutoShow = true
//        binding.playerView.setControllerShowTimeoutMs(0)
//        binding.playerView.showController()

        // Disable tap-to-hide behavior
//        binding.playerView.setOnTouchListener { _, _ ->
//            true
//        }

        val mediaItem = MediaItem.fromUri(videoUrl)
        val cacheFactory = VideoCacheManager.buildCacheDataSource(requireContext())
        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory).createMediaSource(mediaItem)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> binding.videoLoader.visibility = View.VISIBLE
                    Player.STATE_READY -> binding.videoLoader.visibility = View.GONE
                    Player.STATE_ENDED -> {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                        //navigateToOtherStory()
                        findNavController().popBackStack()
                    }
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = false
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
    }

}