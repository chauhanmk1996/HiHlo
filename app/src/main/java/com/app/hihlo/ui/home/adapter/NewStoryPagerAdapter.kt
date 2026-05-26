package com.app.hihlo.ui.home.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.NewStoryItemBinding
import com.app.hihlo.model.chat.Messages
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.VideoCacheManager
import com.app.hihlo.utils.getString
import com.bumptech.glide.Glide


class NewStoryPagerAdapter(
    private val context: Context,
    private val stories: List<Story>,
    private val sharedPlayer: ExoPlayer,
    private val listener: OnStoryActionListener
) :
    RecyclerView.Adapter<NewStoryPagerAdapter.StoryViewHolder>() {
    private var seekBarHandler: Handler? = null
    private var seekBarRunnable: Runnable? = null
    private var seekBarStartTime = 0L
    private var seekBarElapsedTime = 0L
    private var seekBarDuration = 6000L
    private var isSeekBarRunning = false
    private var currentVisibleHolder: StoryViewHolder? = null

    private var lastWindowInsets: WindowInsetsCompat? = null


    inner class StoryViewHolder(val binding: NewStoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var originalBottomPadding = 0
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2 != null) {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x

                    if (Math.abs(diffY) > Math.abs(diffX)) {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                // Swipe Down
                                listener.onSwipeDown()
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })

        fun bind(otherStoryData: Story, position: Int) {
            Log.e("TAG", "bind:fsya $otherStoryData ", )
            originalBottomPadding = binding.bottomLayout.paddingBottom
            binding.apply {
                sendButton.isVisible=true
                sendEditText.isVisible=true
                seenLayout.isVisible=false
                deleteButton.isVisible=false
                Glide.with(binding.root.context).load(otherStoryData.userDetail?.profile_image).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
                userName.text = otherStoryData.userDetail?.username
                userLocation.text = otherStoryData.userDetail?.city+", "+(otherStoryData.userDetail?.country ?: "")

                if (otherStoryData.asset_type == "I") {
                    storyImage.isVisible = true
                    playerView.isVisible = false
                    storySeekBar.isVisible = true

                    Glide.with(context).load(otherStoryData.asset_url).into(storyImage)

                    // ✅ Always reset seekbar state
                    stopSeekBar()
                    storySeekBar.clearAnimation()
                    storySeekBar.progress = 0
                    seekBarElapsedTime = 0L
                    seekBarStartTime = 0L

                    // ✅ Re-start seekbar progress
                    startSeekBarProgress(storySeekBar, 6000)
                }

                else {
                    // Video story setup
                    storyImage.isVisible = false
                    playerView.isVisible = true
                    storySeekBar.isVisible = false

                    stopSeekBar() // Just in case
                    setupPlayer(otherStoryData.asset_url ?: "", binding)
                }
//                sideOptions.setOnClickListener {
//                    listener.onSideOptionClicked()
//                }
                userImageCardView.setOnClickListener {
                    listener.getClick(0)
                }
                sendButton.setOnClickListener {
                    if (sendEditText.getString().isEmpty()){
                        Toast.makeText(root.context, "Please enter something!", Toast.LENGTH_SHORT).show()
                    }else{
                        listener.sendMessage(otherStoryData, sendEditText.text.toString())
                        sendEditText.setText("")
                    }
                }

                leftClickArea.setOnClickListener {
                    listener.leftClickAreaClicked()
                }
                rightClickArea.setOnClickListener {
                    listener.rightClickAreaClicked()
                }
                sideOptions.setOnClickListener {
                    listener.onSideOptionsClicked()
                }
                binding.root.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    true
                }
                binding.playerView.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    true
                }

            }
            lastWindowInsets?.let {
                handleKeyboardInsets(it)
            }

        }

        fun handleKeyboardInsets(insets: WindowInsetsCompat) {
            val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
            val story = stories[position]

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            setPlayPause()
            if (imeVisible) {

                if (story.asset_type == "I") {
                    pauseSeekBar()
                } else {
                    sharedPlayer.playWhenReady = false
                }

            } else {

                if (story.asset_type == "I") {
                    resumeSeekBar()
                } else {
                    sharedPlayer.playWhenReady = true
                }
            }
        }

        fun setPlayPauseStory(isPaused: Boolean) {
            val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
            val story = stories[position]

            setPlayPause()
            if (isPaused) {

                if (story.asset_type == "I") {
                    pauseSeekBar()
                } else {
                    sharedPlayer.playWhenReady = false
                }

            } else {

                if (story.asset_type == "I") {
                    resumeSeekBar()
                } else {
                    sharedPlayer.playWhenReady = true
                }
            }
        }

        private fun setPlayPause() {

        }


    }


    fun handleKeyboardInsets(insets: WindowInsetsCompat) {
        lastWindowInsets = insets
        currentVisibleHolder?.handleKeyboardInsets(insets)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = NewStoryItemBinding.inflate(inflater, parent, false)
        return StoryViewHolder(binding)
    }

    override fun getItemCount(): Int = stories.size

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        val binding = holder.binding

        binding.apply {
            sendButton.isVisible = true
            sendEditText.isVisible = true
            seenLayout.isVisible = false
            deleteButton.isVisible = false

            rightClickArea.bringToFront()
            leftClickArea.bringToFront()
            sideOptions.bringToFront()
            playerView.isClickable=false
            playerView.setOnClickListener {  }
            Glide.with(context).load(story.userDetail?.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(userImage)
            statusTime.text = CommonUtils.getTimeAgo(story.created_at ?: "")
            userName.text = story.userDetail?.username
            userLocation.text = story.userDetail?.city + ", " + (story.userDetail?.country ?: "")

            // Release player for image
            sharedPlayer.pause()
            sharedPlayer.clearMediaItems()

            if (story.asset_type == "I") {
                // Image story
                storyImage.isVisible = true
                playerView.isVisible = false
                storySeekBar.isVisible = true

                Glide.with(context).load(story.asset_url).into(storyImage)

                stopSeekBar()
                storySeekBar.progress = 0
                startSeekBarProgress(storySeekBar, 6000)

            } else {
                // Video story
                storyImage.isVisible = false
                playerView.isVisible = true
                storySeekBar.isVisible = false

                stopSeekBar()
                setupPlayer(story.asset_url ?: "", binding)
            }

        }
    }

    fun stopSeekBar() {
        isSeekBarRunning = false
        seekBarHandler?.removeCallbacks(seekBarRunnable ?: return)
        seekBarElapsedTime = 0L
    }

    fun startSeekBarProgress(seekBar: SeekBar, durationInMillis: Long) {
        stopSeekBar() // stop any old progress
        seekBar.max = 100
        seekBar.progress = 0
        seekBarStartTime = System.currentTimeMillis()
        isSeekBarRunning = true
        seekBarDuration = durationInMillis
        seekBarElapsedTime = 0L

        seekBarHandler = Handler(Looper.getMainLooper())
        seekBarRunnable = object : Runnable {
            override fun run() {
                if (!isSeekBarRunning) return
                val elapsed = System.currentTimeMillis() - seekBarStartTime
                val progress = ((elapsed.toFloat() / seekBarDuration) * 100).toInt()
                seekBar.progress = progress.coerceAtMost(100)

                if (progress < 100) {
                    seekBarHandler?.postDelayed(this, 16)
                } else {
                    listener.onVideoEnd() // called even for images
                }
            }
        }
        seekBarHandler?.post(seekBarRunnable!!)
    }
    fun pauseSeekBar() {
        isSeekBarRunning = false
        seekBarElapsedTime = System.currentTimeMillis() - seekBarStartTime // ✅ Save current time
        seekBarHandler?.removeCallbacks(seekBarRunnable ?: return)
    }

    fun resumeSeekBar() {
        if (!isSeekBarRunning && seekBarElapsedTime < seekBarDuration) {
            seekBarStartTime = System.currentTimeMillis() - seekBarElapsedTime // ✅ Resume from pause point
            isSeekBarRunning = true
            seekBarHandler?.post(seekBarRunnable!!)
        }
    }


    @OptIn(UnstableApi::class)
    private var currentVideoUrl: String? = null

    @OptIn(UnstableApi::class)
    private fun setupPlayer(videoUrl: String, binding: NewStoryItemBinding) {
        if (currentVideoUrl == videoUrl && sharedPlayer.isPlaying) {
            return  // Already playing this video
        }

        currentVideoUrl = videoUrl

        binding.playerView.player = sharedPlayer
        sharedPlayer.stop()
        sharedPlayer.clearMediaItems()
        sharedPlayer.seekTo(0)

        val mediaItem = MediaItem.fromUri(videoUrl)
        val cacheFactory = VideoCacheManager.buildCacheDataSource(context)
        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
            .createMediaSource(mediaItem)

        sharedPlayer.setMediaSource(mediaSource)
        sharedPlayer.prepare()
        sharedPlayer.playWhenReady = true

        binding.videoLoader.visibility = View.VISIBLE
    }

    fun setCurrentVisibleHolder(holder: StoryViewHolder?) {
        currentVisibleHolder = holder
    }


    interface OnStoryActionListener{
        fun onVideoEnd()
        fun onSwipeDown()
        fun onSideOptionsClicked()
        fun sendMessage(otherStoryData: Story, messages: String)
        fun leftClickAreaClicked()
        fun rightClickAreaClicked()
        fun getClick(click:Int)
    }

}

