package com.app.hihlo.ui.HomeNew.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
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
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
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
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter.CustomTypefaceSpan
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.HomeNew.utility.CircleOutlineProvider
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.view_model.NewStoryViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.MyApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.math.hypot

class PlayStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayStatusBinding

    private var player: ExoPlayer? = null
    private lateinit var storyList: ArrayList<StatusItem>

    private var currentPosition = 0
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

    // Hold‑to‑pause variables
    private var holdRunnable: Runnable? = null
    private var isHolding = false
    private val HOLD_DELAY_MS = 150L
    private val HOLD_MOVE_THRESHOLD_DP = 50f
    private var isHoldPause = false   // 🔥 NEW: distinguishes hold pause from other pauses
    private var isDismissing = false

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
        RTVariable.IS_STATUS_VIEWER_ACTIVATED = true
        //binding.root.alpha = 0f
        //binding.root.post { startOpenAnimation() }

        // Parse story list
        val json = intent.getStringExtra("story_list") ?: "[]"
        val type = object : TypeToken<ArrayList<StatusItem>>() {}.type
        val fullStoryList: ArrayList<StatusItem> = Gson().fromJson(json, type)
        val isSinglePlay = intent.getBooleanExtra("is_play_single", false)
        val targetUserId = intent.getStringExtra("user_id") ?: ""
        isSinglePlayMode = isSinglePlay

        if (isSinglePlay && targetUserId.isNotEmpty()) {
            storyList = ArrayList(fullStoryList.filter { it.user_id.toString() == targetUserId })
            currentPosition = 0
            if (storyList.isEmpty()) {
                finish()
                return
            }
        } else {
            storyList = fullStoryList
            currentPosition = intent.getIntExtra("play_position", 0)
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

        // Keep the original click listeners for navigation
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
        binding.sendEditText.setOnClickListener { pauseStory() }
        binding.sendEditText.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) pauseStory() }
        binding.sendButton.setOnClickListener {
            val message = binding.sendEditText.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter something!", Toast.LENGTH_SHORT).show()
            } else {
                sendMessage(storyList[currentPosition], message)
                binding.sendEditText.setText("")
                if (!isKeyboardVisible) resumeStory()
            }
        }
        binding.deleteButton.setOnClickListener {
            pauseStory()
            CommonUtils.showCustomDialogWithBinding(
                this, "Delete Story",
                onYes = { getDelete(storyList[currentPosition].id) },
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

                    // ✅ Cancel hold EARLY when user starts moving
                    if (kotlin.math.abs(diff) > 20) {   // 🔥 reduce threshold
                        cancelHold()
                        if (isHolding) {
                            isHolding = false
                            resumeStory()
                            isHoldPause = false
                        }
                    }

                    if (diff > 0 && !isHolding) {
                        binding.root.translationY = diff

                        // 🔥 Optional smooth scale (feels better)
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
                        isDismissing = true   // ✅ prevent re-trigger

                        binding.root.animate()
                            .translationY(binding.root.height.toFloat())
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(180)
                            .withEndAction {
                                binding.root.translationY = 0f
                                binding.root.scaleX = 1f
                                binding.root.scaleY = 1f
                                finish()
                            }
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
//        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                //RTVariable.IS_STATUS_VIEWER_FINISHED = true
//                finish()
//            }
//        })
    }

    private fun handleSideTouch(
        event: MotionEvent,
        v: View,
        isRight: Boolean,
        holdMovePx: Float
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

    private fun handleSwipeDownAnimation(event: MotionEvent, startY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val diff = event.rawY - startY
                if (diff > 0) binding.root.translationY = diff
                return true
            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                val diff = event.rawY - startY
//                if (diff > 250) finish()
//                else binding.root.animate().translationY(0f).setDuration(150).start()
//                return true
//            }
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

//    private fun startOpenAnimation() {
//        val startX = intent.getIntExtra("start_x", 0).toFloat()
//        val startY = intent.getIntExtra("start_y", 0).toFloat()
//        val startW = intent.getIntExtra("start_width", 100).toFloat()
//        val startH = intent.getIntExtra("start_height", 100).toFloat()
//        val screenW = binding.root.width.toFloat()
//        val screenH = binding.root.height.toFloat()
//        val scaleX = startW / screenW
//        val scaleY = startH / screenH
//        binding.root.pivotX = 0f
//        binding.root.pivotY = 0f
//        binding.root.scaleX = scaleX
//        binding.root.scaleY = scaleY
//        binding.root.translationX = startX
//        binding.root.translationY = startY
//        binding.root.alpha = 1f
//        binding.root.animate()
//            .translationX(0f)
//            .translationY(0f)
//            .scaleX(1f)
//            .scaleY(1f)
//            .setDuration(280)
//            .start()
//    }

    override fun finish() {
        val root = binding.root
        RTVariable.IS_STATUS_VIEWER_FINISHED = true
//        if(RTVariable.IS_STATUS_PROFILE_CLICKED){
//            (this as HomeActivity).goBackTOHome()
//        }
        // EXTRAS FROM LAUNCH: start_x/start_y are the CENTER of the story bubble
        val centerX = intent.getIntExtra("start_x", 0).toFloat()
        val centerY = intent.getIntExtra("start_y", 0).toFloat()
        val bubbleW = intent.getIntExtra("start_width", 100).toFloat()
        val bubbleH = intent.getIntExtra("start_height", 100).toFloat()

        // Reset transforms
        root.translationY = 0f
        root.scaleX = 1f
        root.scaleY = 1f
        root.alpha = 1f

        // Top-left of the bubble
        val bubbleLeft = centerX - bubbleW / 2f
        val bubbleTop  = centerY - bubbleH / 2f

        val screenW = root.width.toFloat()
        val screenH = root.height.toFloat()
        val targetScaleX = bubbleW / screenW
        val targetScaleY = bubbleH / screenH

        // Current view position on screen (should be 0,0 normally)
        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val viewScreenX = location[0].toFloat()
        val viewScreenY = location[1].toFloat()

        // Target translation of the view's top‑left corner
        val targetTransX = bubbleLeft - viewScreenX
        val targetTransY = bubbleTop  - viewScreenY

        // Maximum radius that covers the whole view from the target center
        val maxRadius = maxOf(
            hypot(centerX - viewScreenX, centerY - viewScreenY),
            hypot(centerX - (viewScreenX + screenW), centerY - viewScreenY),
            hypot(centerX - viewScreenX, centerY - (viewScreenY + screenH)),
            hypot(centerX - (viewScreenX + screenW), centerY - (viewScreenY + screenH))
        ).toFloat()

        root.pivotX = 0f
        root.pivotY = 0f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
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

                    // Reset transforms
                    root.scaleX = 1f
                    root.scaleY = 1f
                    root.translationX = 0f
                    root.translationY = 0f

                    // Finish immediately
                    super@PlayStatusActivity.finish()
                    overridePendingTransition(0, 0)
                }
            })
            start()
        }
    }

    private fun playStory() {
        if (currentPosition !in storyList.indices) {
            finish()
            return
        }
        val item = storyList[currentPosition]
        viewModel.hitSeenStoryDataApi(
            "Bearer " + (Preferences.getCustomModelPreference<LoginResponse>(
                this, LOGIN_DATA
            )?.payload?.authToken ?: ""),
            StorySeen(storyId = item.id.toString())
        )
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
        Log.e("TIMER", "TIMER>>> API TIME "+item.created_at)
        val TIME = CommonUtils.getTimeAgo(item.created_at)
        Log.e("TIMER", "TIMER>>> CONVERTED TIME "+TIME)
        var FTIME = ""
        if(TIME.equals("Just now")){
            FTIME = CommonUtils.getTimeAgo(item.created_at)
        }else{
            FTIME = CommonUtils.getTimeAgo(item.created_at) + " ago"
        }
        //Log.e("TIMER", "TIMER>>> CONVERTED TIME "+CommonUtils.getTimeAgo(item.created_at))
        binding.statusTime.text = if (!item.created_at.isNullOrEmpty()) FTIME else ""
        binding.userName.text = item.userDetail.name
        binding.userLocation.text = "${item.userDetail?.city ?: ""}, ${item.userDetail?.country ?: "India"}"
        Glide.with(this).load(item.userDetail.profile_image).placeholder(R.drawable.profile_placeholder).into(binding.userImage)
        if (!item.caption.isNullOrEmpty()) {
            val fullText = item.caption
            binding.captionCollapsed.text = fullText
            binding.captionCollapsed.maxLines = 1
            binding.captionCollapsed.ellipsize = TextUtils.TruncateAt.END
            binding.captionCollapsed.visibility = View.VISIBLE
            binding.captionExpanded.visibility = View.GONE
            binding.moreLessText.visibility = View.GONE
            binding.moreLessText.text = "More"
            binding.captionCollapsed.post {
                val layout = binding.captionCollapsed.layout
                if (layout != null) {
                    val isTruncated = layout.lineCount > 1 || (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)
                    binding.moreLessText.visibility = if (isTruncated) View.VISIBLE else View.GONE
                }
            }
            binding.moreLessText.setOnClickListener {
                binding.captionCollapsed.visibility = View.GONE
                binding.moreLessText.visibility = View.GONE
                binding.captionExpanded.visibility = View.VISIBLE
                val spannable = SpannableStringBuilder(fullText)
                val lessText = " Less"
                spannable.append(lessText)
                val typeface = ResourcesCompat.getFont(
                    binding.root.context,
                    R.font.manrope_bold
                )
                typeface?.let {
                    spannable.setSpan(
                        CustomTypefaceSpan(it),
                        fullText.length,
                        spannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        binding.captionExpanded.visibility = View.GONE
                        binding.captionCollapsed.visibility = View.VISIBLE
                        binding.moreLessText.visibility = View.VISIBLE
                    }
                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.color = binding.root.context.getColor(R.color.theme)
                        ds.bgColor = 0
                    }
                }
                spannable.setSpan(
                    clickableSpan,
                    fullText.length,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.captionExpanded.text = spannable
                binding.captionExpanded.movementMethod = LinkMovementMethod.getInstance()
            }
            binding.captionExpanded.setOnClickListener {
                binding.captionExpanded.visibility = View.GONE
                binding.captionCollapsed.visibility = View.VISIBLE
                binding.moreLessText.visibility = View.VISIBLE
            }
        } else {
            binding.captionCollapsed.text = ""
            binding.captionExpanded.text = ""
            binding.moreLessText.visibility = View.GONE
        }
        releasePlayer()
        handler.removeCallbacks(imageRunnable)
        progressHandler.removeCallbacksAndMessages(null)
        resetProgress()

        if (item.asset_type == "I") {
            binding.storyImage.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE
            Glide.with(this).load(item.asset_url).into(binding.storyImage)
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
                    if (state == Player.STATE_ENDED) playNext()
                }
            })
        }
    }

    private fun pauseStory() {
        if (isPaused) return
        isPaused = true
        player?.pause()
        val item = storyList.getOrNull(currentPosition) ?: return
        if (item.asset_type == "I") {
            val elapsed = System.currentTimeMillis() - imageStartTime
            imageRemaining -= elapsed
            if (imageRemaining < 0) imageRemaining = 0
        }
        handler.removeCallbacks(imageRunnable)
        progressHandler.removeCallbacksAndMessages(null)
        // 🔥 Show blur ONLY if this pause was NOT from a hold
        binding.blurBackground.isVisible = !isHoldPause
    }

    private fun resumeStory() {
        if (!isPaused) return
        val item = storyList.getOrNull(currentPosition) ?: return
        isPaused = false
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
            currentPosition = storyList.size - 1
            finish()
        } else playStory()
    }

    private fun playPrevious() {
        if (isSinglePlayMode) { finish(); return }
        if (currentPosition == 0) {
            if (storyList.isNotEmpty() && storyList[currentPosition].isStoriesUploaded) finish()
            return
        }
        val prev = currentPosition - 1
        if (prev >= 0 && !storyList[prev].isStoriesUploaded) finish()
        else {
            currentPosition--
            playStory()
        }
    }

    private fun resetProgress() { binding.storyProgress.progress = 0 }
    private fun startImageProgress(duration: Long) {
        imageStartTime = System.currentTimeMillis()
        val startProgress = binding.storyProgress.progress
        progressHandler.post(object : Runnable {
            override fun run() {
                if (isPaused) return
                val elapsed = System.currentTimeMillis() - imageStartTime
                val toAdd = ((elapsed * (1000 - startProgress)) / duration).toInt()
                binding.storyProgress.progress = (startProgress + toAdd).coerceAtMost(1000)
                if (elapsed < duration) progressHandler.postDelayed(this, 50)
            }
        })
    }
    private fun startVideoProgress() { progressHandler.post(progressRunnable) }
    private fun updateVideoProgress() {
        val dur = player?.duration ?: 0
        val cur = player?.currentPosition ?: 0
        if (dur > 0) binding.storyProgress.progress = ((cur * 1000) / dur).toInt().coerceAtMost(1000)
    }
    private fun releasePlayer() { player?.release(); player = null }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onStop() {
        super.onStop()
        player?.pause()
    }
    override fun onResume() {
        super.onResume()
//        RTVariable.IS_STATUS_VIEWER_FINISHED = true
//        RTVariable.IS_STATUS_PROFILE_CLICKED = true
        player?.play()
    }
    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        handler.removeCallbacksAndMessages(null)
        progressHandler.removeCallbacksAndMessages(null)
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
        if (click == 0 && currentPosition in storyList.indices) {
            val userId = storyList[currentPosition].user_id.toString()
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
                val response = RetrofitBuilder.apiService.deleteStory(token, StoryDeleteRequest(storyId = storyId.toString()))
                if (response.status == 1) {
                    RTVariable.IS_STATUS_DELETED = true
                    finish()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun openSideOptionsPopup(view: View) {
        val inflater = LayoutInflater.from(this)
        val popBinding = PopupChatSideOptionsBinding.inflate(inflater)
        popBinding.title1.text = "Block"
        popBinding.title2.text = "Report"
        val popup = PopupWindow(popBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 20f
            showAtLocation(view, Gravity.CENTER, 0, 0)
        }
        popBinding.title1.setOnClickListener {
            popup.dismiss()
            BlockFlagBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("screen", "block")
                    putString("userId", storyList[currentPosition].user_id.toString())
                }
                onBlockSuccessful = { dismiss() }
            }.show(supportFragmentManager, "BlockBottomSheet")
        }
        popBinding.title2.setOnClickListener {
            popup.dismiss()
            BlockFlagBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("screen", "flag")
                    putString("userId", storyList[currentPosition].user_id.toString())
                }
                onBlockSuccessful = { dismiss() }
            }.show(supportFragmentManager, "FlagBottomSheet")
        }
        popup.setOnDismissListener { resumeStory() }
    }

    private fun sendMessage(other: StatusItem, message: String) {
        viewModel.hitSaveRecentChatDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(this, LOGIN_DATA)?.payload?.authToken,
            SaveRecentChatRequest(toUserId = other.user_id.toString(), message = message)
        )
        sendMessage(
            (Preferences.getCustomModelPreference<LoginResponse>(this, LOGIN_DATA)?.payload?.userId ?: "").toString(),
            other.user_id.toString(),
            other.userDetail.name ?: "",
            other.userDetail?.profile_image ?: "",
            MediaType.TEXT.name, message, "0", "0"
        )
    }

    private fun sendMessage(
        sender: String, receiver: String, name: String, image: String,
        type: String, msg: String, pinned: String, archived: String, url: String? = null
    ) {
        viewModel.sendMessage(sender, receiver, name, image, type, msg, pinned, archived, url ?: "")
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
}