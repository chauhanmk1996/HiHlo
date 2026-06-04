package com.app.hihlo.ui.HomeNew.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.hihlo.R
import com.app.hihlo.databinding.ActivityPlayStatusBinding
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.story_delete.request.StoryDeleteRequest
import com.app.hihlo.model.story_response.Story
import com.app.hihlo.model.story_response.StoryUser
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter
import com.app.hihlo.ui.HomeNew.utility.CircleOutlineProvider
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.view_model.NewStoryViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.toPx
import com.app.hihlo.utils.MyApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.math.hypot
import androidx.core.net.toUri
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

class PlayStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayStatusBinding

    private var player: ExoPlayer? = null
    private lateinit var userStoryList: ArrayList<StoryUser>

    private var currentUserIndex = 0
    private var currentStoryIndex = 0

    private var isPaused = false
    private var isSinglePlayMode = false

    private val handler = Handler(Looper.getMainLooper())
    private val progressHandler = Handler(Looper.getMainLooper())

    private var imageDuration = 10000L
    private var imageRemaining = 5000L
    private var imageStartTime = 0L
    private var isKeyboardVisible = false

    private val viewModel: NewStoryViewModel by viewModels()

    // Swipe down variables
    private var downYRight = 0f
    private var downXRight = 0f
    private var isSwipeDownRight = false
    private var downYLeft = 0f
    private var downXLeft = 0f
    private var isSwipeDownLeft = false

    private var swipeThreshold = 0f
    private var touchSlop = 0
    private var clickThreshold = 0f

    // Hold-to-pause variables
    private var holdRunnable: Runnable? = null
    private var isHolding = false
    private val HOLD_DELAY_MS = 150L
    private val HOLD_MOVE_THRESHOLD_DP = 50f
    private var isHoldPause = false
    private var isDismissing = false
    private var wasMutedBeforeBackground = false
    private var isUserMuteStory = false

    private val imageRunnable = Runnable { playNext() }
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateVideoProgress()
            progressHandler.postDelayed(this, 50)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        binding = ActivityPlayStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RTVariable.IS_STORY_VIEW = true
        RTVariable.IS_STATUS_VIEWER_ACTIVATED = true
        UserDataManager.postCommentIsShow(this@PlayStatusActivity, false)

        // Parse story list
        val json = intent.getStringExtra("story_list") ?: "[]"
        val type = object : TypeToken<ArrayList<StoryUser>>() {}.type
        val fullUserStoryList: ArrayList<StoryUser> = Gson().fromJson(json, type)

        val isSinglePlay = intent.getBooleanExtra("is_play_single", false)
        val targetUserId = intent.getStringExtra("user_id") ?: ""
        isSinglePlayMode = isSinglePlay

        userStoryList = if (isSinglePlay && targetUserId.isNotEmpty()) {
            ArrayList(fullUserStoryList.filter { it.user_id.toString() == targetUserId })
        } else {
            fullUserStoryList
        }

        currentUserIndex = if (isSinglePlayMode) 0 else intent.getIntExtra("play_position", 0)

        if (userStoryList.isEmpty()) {
            finish()
            return
        }

        swipeThreshold = 120 * resources.displayMetrics.density
        clickThreshold = 10 * resources.displayMetrics.density
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val holdMovePx = HOLD_MOVE_THRESHOLD_DP * resources.displayMetrics.density

        // ====== LEFT / RIGHT touch listeners with hold & swipe ======
        binding.rightClickArea.setOnTouchListener { v, event ->
            handleSideTouch(event, v, isRight = true, holdMovePx)
        }
        binding.leftClickArea.setOnTouchListener { v, event ->
            handleSideTouch(event, v, isRight = false, holdMovePx)
        }

        // Touch listeners
        binding.rightClickArea.setOnTouchListener { v, event ->
            handleSideTouch(event, v, isRight = true, holdMovePx)
        }
        binding.leftClickArea.setOnTouchListener { v, event ->
            handleSideTouch(event, v, isRight = false, holdMovePx)
        }

        binding.rightClickArea.setOnClickListener { playNext() }
        binding.leftClickArea.setOnClickListener { playPrevious() }

        binding.sideOptions.bringToFront()
        binding.sideOptions.setOnClickListener {
            pauseStory()
            openSideOptionsPopup(binding.sideOptions)
        }

        binding.sendEditText.setOnClickListener { pauseStory() }
        binding.sendEditText.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) pauseStory() }

        binding.sendButton.setOnClickListener {
            val message = binding.sendEditText.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter something!", Toast.LENGTH_SHORT).show()
            } else {
                sendMessage(getCurrentStory(), message)
                binding.sendEditText.setText("")
                if (!isKeyboardVisible) resumeStory()
            }
        }

        binding.deleteButton.setOnClickListener {
            pauseStory()
            CommonUtils.showCustomDialogWithBinding(
                this, "Delete Story",
                onYes = { getDelete(getCurrentStory().id) },
                onNo = { resumeStory() }
            )
        }

        binding.userImageCardView.setOnClickListener { getClick(0) }
        binding.blurBackground.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                updateKeyboardVisibility(false)
                resumeStory()
            }
            true
        }
        binding.btnMuteUnmute.setOnClickListener {
            if (isUserMuteStory) {
                isUserMuteStory = false
                player?.volume = 1f
                wasMutedBeforeBackground = false
                binding.btnMuteUnmute.setImageResource(R.drawable.volume_mute)
            } else {
                isUserMuteStory = true
                player?.volume = 0f
                wasMutedBeforeBackground = true
                binding.btnMuteUnmute.setImageResource(R.drawable.volume_unmute)
            }
        }

        setupSwipeToDismiss()
        playCurrentStory()
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
    }

    private fun getCurrentUser() = userStoryList[currentUserIndex]
    private fun getCurrentStory() = getCurrentUser().stories[currentStoryIndex]

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeToDismiss() {
        var downY = 0f
        binding.root.setOnTouchListener { _, event ->
            if (isKeyboardVisible || isDismissing) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    isDismissing = false
                    startHoldTimer()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val diff = event.rawY - downY
                    if (kotlin.math.abs(diff) > 20) {
                        cancelHold()
                        if (isHolding) {
                            isHolding = false
                            resumeStory()
                            isHoldPause = false
                        }
                    }
                    if (diff > 0 && !isHolding) {
                        binding.root.translationY = diff
                        val progress = (diff / binding.root.height).coerceIn(0f, 1f)
                        binding.root.scaleX = 1f - (progress * 0.15f)
                        binding.root.scaleY = 1f - (progress * 0.15f)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHold()
                    val diff = event.rawY - downY

                    if (isHolding) {
                        isHolding = false
                        resumeStory()
                        isHoldPause = false
                        return@setOnTouchListener true
                    }

                    if (diff > 250) {
                        isDismissing = true
                        binding.root.animate()
                            .translationY(binding.root.height.toFloat())
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(180)
                            .withEndAction { finish() }
                            .start()
                    } else {
                        binding.root.animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun setupProgressBars() {
        val container = binding.storyProgressContainer
        container.removeAllViews()
        container.isVisible = true

        val count = getCurrentUser().stories.size

        for (i in 0 until count) {
            // Create a FrameLayout to hold track + progress fill
            val progressWrapper = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(4), 1f).apply {
                    marginStart = if (i == 0) 0 else dpToPx(3)
                    marginEnd = if (i == count - 1) 0 else dpToPx(3)
                }
            }

            // Track (background)
            val track = View(this).apply {
                setBackgroundColor(Color.parseColor("#808080"))
            }

            progressWrapper.addView(
                track,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Progress fill (foreground)
            val fill = View(this).apply {
                setBackgroundColor(Color.WHITE)
            }
            val fillParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            fillParams.gravity = Gravity.START
            progressWrapper.addView(fill, fillParams)

            // Store the fill view for later progress updates
            progressWrapper.tag = fill   // we'll use tag to retrieve it

            container.addView(progressWrapper)

            // Set initial progress for already watched stories
            if (i < currentStoryIndex) {
                (fill.layoutParams as FrameLayout.LayoutParams).width =
                    ViewGroup.LayoutParams.MATCH_PARENT
                fill.requestLayout()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun playCurrentStory() {
        if (currentUserIndex !in userStoryList.indices) {
            finish()
            return
        }
        val user = getCurrentUser()
        if (currentStoryIndex !in user.stories.indices) {
            moveToNextUser()
            return
        }

        setupProgressBars()
        val story = getCurrentStory()

        viewModel.hitSeenStoryDataApi(
            "Bearer " + (Preferences.getCustomModelPreference<LoginResponse>(
                this, LOGIN_DATA
            )?.payload?.authToken ?: ""),
            StorySeen(storyId = story.id.toString())
        )

        updateUIForStory(story)
        releasePlayer()
        handler.removeCallbacks(imageRunnable)
        progressHandler.removeCallbacksAndMessages(null)

        if (story.asset_type == "I") {
            binding.storyImage.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE

            Glide.with(this)
                .asBitmap()
                .load(story.asset_url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?,
                    ) {
                        val imageWidth = resource.width.toFloat()
                        val imageHeight = resource.height.toFloat()
                        val imageRatio = imageWidth / imageHeight
                        binding.storyImage.post {
                            val viewWidth = binding.storyImage.width.toFloat()
                            val viewHeight = binding.storyImage.height.toFloat()
                            val viewRatio = viewWidth / viewHeight
                            val difference = kotlin.math.abs(imageRatio - viewRatio)
                            binding.storyImage.scaleType =
                                if (difference < 0.2f) {
                                    ImageView.ScaleType.FIT_XY
                                } else {
                                    ImageView.ScaleType.FIT_CENTER
                                }
                            binding.storyImage.setImageBitmap(resource)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })

            //Glide.with(this).load(story.asset_url).into(binding.storyImage)
            imageRemaining = imageDuration
            startImageProgress()
        } else {
            binding.storyImage.visibility = View.GONE
            binding.playerView.visibility = View.VISIBLE
            player = ExoPlayer.Builder(this).build()
            binding.playerView.player = player
            val mediaItem = MediaItem.fromUri(story.asset_url.toUri())
            player?.setMediaItem(mediaItem)
            player?.prepare()

            if (isUserMuteStory) {
                player?.volume = 0f
                wasMutedBeforeBackground = true
                binding.btnMuteUnmute.setImageResource(R.drawable.volume_unmute)
            } else {
                player?.volume = 1f
                wasMutedBeforeBackground = false
                binding.btnMuteUnmute.setImageResource(R.drawable.volume_mute)
            }

            player?.play()
            startVideoProgress()
            player?.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) playNext()
                }

                @OptIn(UnstableApi::class)
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val videoWidth = videoSize.width.toFloat()
                    val videoHeight = videoSize.height.toFloat()
                    if (videoWidth == 0f || videoHeight == 0f) return

                    binding.playerView.post {

                        val viewWidth = binding.playerView.width.toFloat()
                        val viewHeight = binding.playerView.height.toFloat()

                        val scaleX = viewWidth / videoWidth
                        val scaleY = viewHeight / videoHeight

                        val maxScale = maxOf(scaleX, scaleY)

                        val videoSurface = binding.playerView.videoSurfaceView

                        videoSurface?.scaleX = maxScale
                        videoSurface?.scaleY = maxScale
                    }
                }
            })
        }
    }

    private fun updateUIForStory(story: Story) {
        val user = getCurrentUser()
        binding.userName.text = user.userDetail.name
        binding.userLocation.text =
            "${user.userDetail.city ?: ""}, ${user.userDetail.country ?: "India"}"
        Glide.with(this).load(user.userDetail.profile_image)
            .placeholder(R.drawable.profile_placeholder).into(binding.userImage)

        val TIME = CommonUtils.getTimeAgo(story.created_at)
        binding.statusTime.text = if (TIME.equals("Just now")) TIME else "$TIME ago"

        binding.btnMuteUnmute.isVisible = story.asset_type == "V"

        val loginUserId = Preferences.getCustomModelPreference<LoginResponse>(
            this,
            LOGIN_DATA
        )?.payload?.userId
        if (loginUserId == user.user_id) {
            binding.seenLayout.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.VISIBLE
            binding.sendButton.visibility = View.GONE
            binding.sendEditText.visibility = View.GONE
            binding.seenCount.text = story.seen_count.toString()
        } else {
            binding.seenLayout.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
            binding.sendButton.visibility = View.VISIBLE
            binding.sendEditText.visibility = View.VISIBLE
        }
        setupCaption(story.caption)
    }

    private fun moveToNextUser() {
        currentUserIndex++
        currentStoryIndex = 0
        if (currentUserIndex >= userStoryList.size) finish() else playCurrentStory()
    }

    private fun playNext() {
        currentStoryIndex++
        val user = getCurrentUser()
        if (currentStoryIndex >= user.stories.size) {
            moveToNextUser()
        } else {
            playCurrentStory()
        }
    }

    private fun playPrevious() {
        if (isSinglePlayMode) {
            finish(); return
        }
        if (currentStoryIndex > 0) {
            currentStoryIndex--
        } else if (currentUserIndex > 0) {
            currentUserIndex--
            currentStoryIndex = getCurrentUser().stories.size - 1
        } else {
            finish()
            return
        }
        playCurrentStory()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCaption(fullText: String?) {
        if (fullText.isNullOrEmpty()) {
            binding.captionCollapsed.text = ""
            binding.captionExpanded.text = ""
            binding.moreLessText.visibility = View.GONE
            binding.captionContainer.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        fun setCaptionBackground(isExpanded: Boolean) {
            if (isExpanded) {
                val cornerRadius = 25f.toPx(this)
                val shapeDrawable = MaterialShapeDrawable(
                    ShapeAppearanceModel.Builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                        .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                        .build()
                )
                shapeDrawable.fillColor = ColorStateList.valueOf(Color.parseColor("#70000000"))
                binding.captionContainer.background = shapeDrawable
            } else {
                binding.captionContainer.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        binding.captionCollapsed.text = fullText
        binding.captionCollapsed.maxLines = 1
        binding.captionCollapsed.ellipsize = TextUtils.TruncateAt.END
        binding.captionCollapsed.visibility = View.VISIBLE
        binding.captionExpanded.visibility = View.GONE
        binding.moreLessText.visibility = View.GONE
        binding.moreLessText.text = "More"

        setCaptionBackground(false)

        binding.captionCollapsed.post {
            val layout = binding.captionCollapsed.layout
            if (layout != null) {
                val isTruncated = layout.lineCount > 1 ||
                        (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)
                binding.moreLessText.visibility = if (isTruncated) View.VISIBLE else View.GONE
            }
        }

        binding.moreLessText.setOnClickListener {
            pauseStory()
            setCaptionBackground(true)

            binding.captionCollapsed.visibility = View.GONE
            binding.moreLessText.visibility = View.GONE
            binding.captionExpanded.visibility = View.VISIBLE

            val spannable = SpannableStringBuilder(fullText)
            val lessText = " Less"
            spannable.append(lessText)

            val start = fullText.length
            val end = spannable.length

            val typeface = ResourcesCompat.getFont(this, R.font.manrope_bold)
            typeface?.let {
                spannable.setSpan(
                    PostsAdapter.CustomTypefaceSpan(it),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    binding.captionExpanded.visibility = View.GONE
                    binding.captionCollapsed.visibility = View.VISIBLE
                    binding.moreLessText.visibility = View.VISIBLE
                    setCaptionBackground(false)
                    resumeStory()
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                    ds.color = getColor(R.color.theme)
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            binding.captionExpanded.text = spannable
            binding.captionExpanded.movementMethod = ScrollingMovementMethod()
            binding.captionExpanded.highlightColor = Color.TRANSPARENT
        }

        // Touch conflict fix
        var isScrolling = false
        var downY = 0f
        binding.captionExpanded.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isScrolling = false
                    downY = event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.rawY - downY) > 20) isScrolling = true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isScrolling) {
                        binding.captionExpanded.visibility = View.GONE
                        binding.captionCollapsed.visibility = View.VISIBLE
                        binding.moreLessText.visibility = View.VISIBLE
                        setCaptionBackground(false)
                        resumeStory()
                    }
                }
            }
            false
        }
    }

    private fun startImageProgress() {
        imageStartTime = System.currentTimeMillis()
        progressHandler.post(object : Runnable {
            override fun run() {
                if (isPaused) return
                val elapsed = System.currentTimeMillis() - imageStartTime
                val progressFraction = (elapsed.toFloat() / imageDuration).coerceAtMost(1f)
                val wrapper =
                    binding.storyProgressContainer.getChildAt(currentStoryIndex) as? FrameLayout
                val fill = wrapper?.tag as? View
                fill?.let {
                    val totalWidth = wrapper.width
                    val fillWidth = (totalWidth * progressFraction).toInt()
                    (it.layoutParams as FrameLayout.LayoutParams).width = fillWidth
                    it.requestLayout()
                }

                if (elapsed < imageDuration) {
                    progressHandler.postDelayed(this, 50)
                } else {
                    playNext()
                }
            }
        })
    }

    private fun startVideoProgress() {
        progressHandler.post(progressRunnable)
    }

    private fun updateVideoProgress() {
        player?.let {
            val dur = it.duration
            val cur = it.currentPosition
            if (dur > 0) {
                val progressFraction = (cur.toFloat() / dur).coerceAtMost(1f)
                val wrapper =
                    binding.storyProgressContainer.getChildAt(currentStoryIndex) as? FrameLayout
                val fill = wrapper?.tag as? View
                fill?.let {
                    val totalWidth = wrapper.width
                    val fillWidth = (totalWidth * progressFraction).toInt()
                    (it.layoutParams as FrameLayout.LayoutParams).width = fillWidth
                    it.requestLayout()
                }
            }
        }
    }

    private fun pauseStory() {
        if (isPaused) return
        isPaused = true
        player?.pause()
        if (getCurrentStory().asset_type == "I") {
            val elapsed = System.currentTimeMillis() - imageStartTime
            imageRemaining -= elapsed
            if (imageRemaining < 0) imageRemaining = 0
        }
        handler.removeCallbacks(imageRunnable)
        progressHandler.removeCallbacksAndMessages(null)
        binding.blurBackground.isVisible = !isHoldPause
    }

    private fun resumeStory() {
        if (!isPaused) return
        isPaused = false
        if (getCurrentStory().asset_type == "I") {
            startImageProgress()
        } else {
            player?.play()
            startVideoProgress()
        }
        binding.blurBackground.isVisible = false
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun handleSideTouch(
        event: MotionEvent,
        v: View,
        isRight: Boolean,
        holdMovePx: Float,
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isRight) {
                    downYRight = event.rawY
                    downXRight = event.rawX
                    isSwipeDownRight = false
                } else {
                    downYLeft = event.rawY
                    downXLeft = event.rawX
                    isSwipeDownLeft = false
                }
                startHoldTimer()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val downX = if (isRight) downXRight else downXLeft
                val downY = if (isRight) downYRight else downYLeft
                val diffY = event.rawY - downY
                val diffX = kotlin.math.abs(event.rawX - downX)
                if (kotlin.math.abs(diffY) > holdMovePx || diffX > holdMovePx) {
                    cancelHold()
                    if (isPaused) {
                        resumeStory()
                        isHoldPause = false
                    }
                }

                if (diffY > swipeThreshold && diffX < holdMovePx * 3) {
                    if (isRight) isSwipeDownRight = true else isSwipeDownLeft = true
                }
                if ((isRight && isSwipeDownRight) || (!isRight && isSwipeDownLeft)) {
                    handleSwipeDownAnimation(event, downY)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelHold()
                val downX = if (isRight) downXRight else downXLeft
                val downY = if (isRight) downYRight else downYLeft
                val totalMoveX = kotlin.math.abs(event.rawX - downX)
                val totalMoveY = kotlin.math.abs(event.rawY - downY)
                if ((isRight && isSwipeDownRight) || (!isRight && isSwipeDownLeft)) {
                    handleSwipeDownAnimation(event, downY)
                } else if (totalMoveX < touchSlop && totalMoveY < touchSlop) {
                    if (!isHolding) {
                        v.performClick()
                    } else {
                        isHolding = false
                        resumeStory()
                        isHoldPause = false
                    }
                } else if (isHolding) {
                    isHolding = false
                    resumeStory()
                    isHoldPause = false
                }
                return true
            }

            else -> return false
        }
    }

    private fun handleSwipeDownAnimation(event: MotionEvent, startY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val diff = event.rawY - startY
                if (diff > 0) binding.root.translationY = diff
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val diff = event.rawY - startY

                if (diff > 250) {
                    // Smoothly continue animation downward before closing
                    binding.root.animate()
                        .translationY(binding.root.height.toFloat())
                        .setDuration(180)
                        .withEndAction {
                            // Reset translation before finish so animation works correctly
                            binding.root.translationY = 0f
                            finish() // 🔥 This will call your overridden finish()
                        }
                        .start()
                } else {
                    binding.root.animate()
                        .translationY(0f)
                        .setDuration(150)
                        .start()
                }
                return true
            }

            else -> return false
        }
    }

    private fun startHoldTimer() {
        holdRunnable?.let { handler.removeCallbacks(it) }
        isHolding = false
        val action = Runnable {
            if (!isPaused) {
                isHoldPause = true
                isHolding = true
                pauseStory()
            }
        }
        holdRunnable = action
        handler.postDelayed(action, HOLD_DELAY_MS)
    }

    private fun cancelHold() {
        holdRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateKeyboardVisibility(visible: Boolean) {
        if (isKeyboardVisible == visible) return
        isKeyboardVisible = visible
        if (visible) pauseStory()
        else {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            binding.blurBackground.isVisible = false
            resumeStory()
        }
    }

    fun getClick(click: Int) {
        if (click == 0 && currentUserIndex in userStoryList.indices) {
            val userId = userStoryList[currentUserIndex].user_id.toString()
            MyApplication.isStackMode = true
            RTVariable.IS_STATUS_PROFILE_CLICKED = true
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("navigate_to_profile", true)
                putExtra("profile_user_id", userId)
                putExtra("profile_from", "secondStory")
            }
            startActivity(intent)
        }
    }

    private fun getDelete(storyId: Int) {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    this@PlayStatusActivity, LOGIN_DATA
                )?.payload?.authToken
                val response = RetrofitBuilder.apiService.deleteStory(
                    token,
                    StoryDeleteRequest(storyId = storyId.toString())
                )
                if (response.status == 1) {
                    RTVariable.IS_STATUS_DELETED = true
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openSideOptionsPopup(view: View) {
        val inflater = LayoutInflater.from(this)
        val popBinding = PopupChatSideOptionsBinding.inflate(inflater)
        popBinding.title1.text = "Block"
        popBinding.title2.text = "Report"
        val popup = PopupWindow(
            popBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 20f
            showAtLocation(view, Gravity.CENTER, 0, 0)
        }
        popBinding.title1.setOnClickListener {
            popup.dismiss()
            BlockFlagBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("screen", "block")
                    putString("userId", getCurrentUser().user_id.toString())
                }
            }.show(supportFragmentManager, "BlockBottomSheet")
        }
        popBinding.title2.setOnClickListener {
            popup.dismiss()
            BlockFlagBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("screen", "flag")
                    putString("userId", getCurrentUser().user_id.toString())
                }
            }.show(supportFragmentManager, "FlagBottomSheet")
        }
        popup.setOnDismissListener { resumeStory() }
    }

    private fun sendMessage(other: Story, message: String) {
        viewModel.hitSaveRecentChatDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                this,
                LOGIN_DATA
            )?.payload?.authToken,
            SaveRecentChatRequest(toUserId = getCurrentUser().user_id.toString(), message = message)
        )
    }

    private fun setObserver() {
        viewModel.seenStoryLiveData().observe(this) { res ->
            when (res.status) {
                Status.SUCCESS -> {
                    //ProcessDialog.dismissDialog(true)
                }

                Status.LOADING -> {
                    //ProcessDialog.showDialog(this, true)
                }

                Status.ERROR -> {
                    //Log.e("TAG", "Error: ${res.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        wasMutedBeforeBackground = player?.volume == 0f
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

    override fun finish() {
        val root = binding.root
        if (RTVariable.IS_STATUS_PROFILE_CLICKED) {
            RTVariable.IS_STATUS_VIEWER_FINISHED = true
        }

        val centerX = intent.getIntExtra("start_x", 0).toFloat()
        val centerY = intent.getIntExtra("start_y", 0).toFloat()
        val bubbleW = intent.getIntExtra("start_width", 100).toFloat()
        val bubbleH = intent.getIntExtra("start_height", 100).toFloat()

        root.translationY = 0f
        root.scaleX = 1f
        root.scaleY = 1f
        root.alpha = 1f

        val bubbleLeft = centerX - bubbleW / 2f
        val bubbleTop = centerY - bubbleH / 2f

        val screenW = root.width.toFloat()
        val screenH = root.height.toFloat()
        val targetScaleX = bubbleW / screenW
        val targetScaleY = bubbleH / screenH

        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val viewScreenX = location[0].toFloat()
        val viewScreenY = location[1].toFloat()

        val targetTransX = bubbleLeft - viewScreenX
        val targetTransY = bubbleTop - viewScreenY

        val maxRadius = maxOf(
            hypot(centerX - viewScreenX, centerY - viewScreenY),
            hypot(centerX - (viewScreenX + screenW), centerY - viewScreenY),
            hypot(centerX - viewScreenX, centerY - (viewScreenY + screenH)),
            hypot(centerX - (viewScreenX + screenW), centerY - (viewScreenY + screenH))
        ).toFloat()

        root.pivotX = 0f
        root.pivotY = 0f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 100L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction

                val currScaleX = 1f + (targetScaleX - 1f) * fraction
                val currScaleY = 1f + (targetScaleY - 1f) * fraction
                val currTransX = targetTransX * fraction
                val currTransY = targetTransY * fraction

                root.scaleX = currScaleX
                root.scaleY = currScaleY
                root.translationX = currTransX
                root.translationY = currTransY

                val localCenterX = (centerX - currTransX - viewScreenX) / currScaleX
                val localCenterY = (centerY - currTransY - viewScreenY) / currScaleY

                val currRadius = maxRadius * (1f - fraction)

                root.outlineProvider = CircleOutlineProvider(
                    localCenterX.toInt(), localCenterY.toInt(), currRadius
                )
                root.clipToOutline = true
                root.invalidateOutline()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    root.clipToOutline = false

                    root.scaleX = 0f
                    root.scaleY = 0f
                    root.translationX = 0f
                    root.translationY = 0f

                    super@PlayStatusActivity.finish()
                    overridePendingTransition(0, 0)
                }
            })
            start()
        }
    }
}