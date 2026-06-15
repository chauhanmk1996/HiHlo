package com.app.hihlo.ImageVideoConverter

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.*
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.app.hihlo.R
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.bumptech.glide.Glide
import ja.burhanrashid52.photoeditor.PhotoEditorView
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import androidx.core.net.toUri
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.home.activity.UploadStatusActivity
import com.app.hihlo.utils.ProgressPercentageDialog
import androidx.core.graphics.createBitmap
import com.app.hihlo.utils.VideoConvertingPercentageDialog

class ImageVideoConverter : AppCompatActivity() {

    // Views
    private lateinit var mediaContainer: FrameLayout

    //private lateinit var bottomLayout: ConstraintLayout
    private lateinit var overlayContainer: FrameLayout
    private lateinit var photoEditorView: PhotoEditorView
    private lateinit var playerView: PlayerView
    private lateinit var videoTextureView: TextureView
    private lateinit var btnText: Button
    private lateinit var btnDone: Button

    //private lateinit var btnNext: Button
    //private lateinit var etInput2: EditText
    private lateinit var tvShare: TextView
    private lateinit var ivBack: ImageView
    private lateinit var videoTrimmerView: VideoTrimmerView
    private lateinit var btnPlayPause: ImageView
    private lateinit var btnMuteUnmute: ImageView
    private lateinit var rotationTooltip: TextView
    private lateinit var bgView: View

    private var player: ExoPlayer? = null
    private var isVideoMedia = false
    private var editingTextView: TextView? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var savedUri: Uri? = null
    private var mediaType: String = ""
    private var trimStartMs = 0L
    private var trimEndMs = Long.MAX_VALUE
    private var originalVideoDurationMs = 0L
    private var originalVideoUri: Uri? = null
    //private var headline_caption: String = ""

    private enum class MediaStep {
        TRIM, TEXT_OVERLAY
    }

    private var currentStep = MediaStep.TEXT_OVERLAY

    private val progressHandler = Handler(Looper.getMainLooper())
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hidePlayPauseButton() }
    private val autoHideDelayMs = 5000L
    private var wasPlayingBeforeBackground = false
    private var wasMutedBeforeBackground = false
    private var recordingTriggered = false
    private var isEditingEnabled = true
    private var videoConvertingPercentageDialog: VideoConvertingPercentageDialog? = null
    private var renderThread: HandlerThread? = null

    private val captionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val statusUploaded = result.data?.getStringExtra("statusUploaded") ?: ""
            setResult(RESULT_OK, Intent().apply {
                putExtra("statusUploaded", statusUploaded)
            })
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.image_video_converter)
        initViews()
        mediaContainer.isDrawingCacheEnabled = true
        overlayContainer.isDrawingCacheEnabled = true
        val uri = intent.getStringExtra("uri") ?: ""
        originalVideoUri = uri.toUri()
        isVideoMedia = intent.getBooleanExtra("isVideo", false)
        setupRotationTooltip()
        if (isVideoMedia) {
            setupVideo(uri)
            goToStep(MediaStep.TRIM)
        } else {
            setupImage(uri)
            goToStep(MediaStep.TEXT_OVERLAY)
        }
        setupListeners()
        setupEditor()
        setupBackPress()
        /*etInput2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                headline_caption = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        })*/


        /*window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = window.decorView.height
            val keyboardHeight = screenHeight - rect.bottom
            if (keyboardHeight > screenHeight * 0.15) {
                bottomLayout.translationY = -(keyboardHeight - 220).toFloat()
            } else {
                bottomLayout.translationY = 0f
            }
        }*/
    }

    private fun initViews() {
        mediaContainer = findViewById(R.id.mediaContainer)
        overlayContainer = findViewById(R.id.overlayContainer)
        photoEditorView = findViewById(R.id.photoEditorView)
        playerView = findViewById(R.id.playerView)
        videoTextureView = TextureView(this).apply { visibility = View.GONE }
        btnText = findViewById(R.id.btnText)
        btnDone = findViewById(R.id.btnDone)
        //btnNext = findViewById(R.id.btnNext)
        //etInput2 = findViewById(R.id.etInput2)
        tvShare = findViewById(R.id.tvShare)
        ivBack = findViewById(R.id.ivBack)
        videoTrimmerView = findViewById(R.id.videoTrimmerView)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnMuteUnmute = findViewById(R.id.btnMuteUnmute)
        mediaContainer.addView(
            videoTextureView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        //bottomLayout = findViewById(R.id.bottomLayout)
        bgView = findViewById(R.id.bgView)
    }

    private fun setupRotationTooltip() {
        rotationTooltip = TextView(this).apply {
            setBackgroundResource(R.drawable.bg_rotation_tooltip)
            setTextColor(Color.BLACK)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = ResourcesCompat.getFont(this@ImageVideoConverter, R.font.manrope_semibold)
            visibility = View.GONE
            setPadding(32, 8, 32, 8)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        overlayContainer.addView(rotationTooltip)
    }

    private fun setupListeners() {
        /*btnNext.setOnClickListener {
            when (currentStep) {
                MediaStep.TRIM -> {
                    trimStartMs = videoTrimmerView.getTrimStartMs()
                    trimEndMs = videoTrimmerView.getTrimEndMs()
                    goToStep(MediaStep.TEXT_OVERLAY)
                }

                MediaStep.TEXT_OVERLAY -> goToStep(MediaStep.HEADLINE)

                MediaStep.HEADLINE -> {}
            }
        }*/

        btnDone.setOnClickListener {
            //headline_caption = etInput2.text.toString().trim()
            if (isVideoMedia) {
                if (!isRecording && !recordingTriggered) startRecordingWithTrimRange()
            } else {
                saveFinalImage()
            }
        }

        ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnPlayPause.setOnClickListener { togglePlayback() }
        playerView.setOnClickListener { togglePlayback() }
        btnMuteUnmute.setOnClickListener {
            player?.let { exoPlayer -> checkVolume(exoPlayer, btnMuteUnmute) }
        }
    }

    private fun togglePlayback() {
        player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
            showPlayPauseButton()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    currentStep == MediaStep.TEXT_OVERLAY && isVideoMedia -> goToStep(MediaStep.TRIM)
                    else -> finish()
                }
            }
        })
    }

    private fun goToStep(step: MediaStep) {
        currentStep = step
        when (step) {
            MediaStep.TRIM -> {
                videoTrimmerView.visibility = View.VISIBLE
                //etInput2.visibility = View.GONE
                tvShare.visibility = View.VISIBLE
                //btnNext.isVisible = true
                btnDone.isVisible = false
                btnText.isVisible = false
                val params = btnMuteUnmute.layoutParams as ViewGroup.MarginLayoutParams
                val marginBottomDp = 170
                val marginBottomPx = (marginBottomDp * resources.displayMetrics.density).toInt()
                params.bottomMargin = marginBottomPx
                btnMuteUnmute.layoutParams = params
                isEditingEnabled = true
            }

            MediaStep.TEXT_OVERLAY -> {
                videoTrimmerView.visibility = View.GONE
                //etInput2.visibility = View.GONE
                tvShare.visibility = View.VISIBLE
                //btnNext.isVisible = true
                btnDone.isVisible = false
                btnText.isVisible = true
                val params = btnMuteUnmute.layoutParams as ViewGroup.MarginLayoutParams
                val marginBottomDp = 80
                val marginBottomPx = (marginBottomDp * resources.displayMetrics.density).toInt()
                params.bottomMargin = marginBottomPx
                btnMuteUnmute.layoutParams = params
                isEditingEnabled = true
            }
        }
        showPlayPauseButton()
    }

    /*private fun setupEtInput2HeightListener() {
        etInput2.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val newLineCount = etInput2.lineCount
            if (newLineCount != currentLineCount && currentStep == MediaStep.HEADLINE) {
                currentLineCount = newLineCount.coerceIn(1, 3)
                val extraMarginDp = (currentLineCount - 1) * 25
                val newMarginDp = defaultMuteButtonMarginDp + extraMarginDp
                updateMuteButtonMargin(newMarginDp)
            }
        }
        etInput2.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun updateMuteButtonMargin(marginDp: Int) {
        val params = btnMuteUnmute.layoutParams as ViewGroup.MarginLayoutParams
        val marginPx = (marginDp * resources.displayMetrics.density).toInt()
        params.bottomMargin = marginPx
        btnMuteUnmute.layoutParams = params
    }*/

    private fun showPlayPauseButton() {
        if (!isVideoMedia) {
            btnPlayPause.visibility = View.GONE
            return
        }
        autoHideHandler.removeCallbacks(autoHideRunnable)
        btnPlayPause.visibility = View.VISIBLE
        autoHideHandler.postDelayed(autoHideRunnable, autoHideDelayMs)
    }

    private fun hidePlayPauseButton() {
        btnPlayPause.visibility = View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupVideo(uri: String) {
        btnMuteUnmute.isVisible = true
        photoEditorView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.repeatMode = Player.REPEAT_MODE_OFF
        player?.setMediaItem(MediaItem.fromUri(uri.toUri()))
        player?.prepare()
        player?.play()
        originalVideoDurationMs = getVideoDuration(uri)
        videoTrimmerView.setVideoUri(uri.toUri(), originalVideoDurationMs)
        trimStartMs = 0L
        trimEndMs = originalVideoDurationMs

        videoTrimmerView.setOnTrimChangedListener { start, end ->
            trimStartMs = start
            trimEndMs = end
        }
        videoTrimmerView.setOnScrubChangedListener { ms ->
            player?.seekTo(ms)
        }
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                runOnUiThread {
                    btnPlayPause.setImageResource(
                        if (isPlaying) R.drawable.play_icon else R.drawable.pause_icon
                    )
                }
            }
        })
        startProgressLoop()
        attachGesturesToView(playerView, isText = false)
    }

    private fun startProgressLoop() {
        progressHandler.post(object : Runnable {
            override fun run() {
                val p = player ?: return
                val current = p.currentPosition
                videoTrimmerView.updateProgress(current)
                if (current >= trimEndMs) {
                    p.seekTo(trimStartMs)
                    p.play()
                }
                progressHandler.postDelayed(this, 16)
            }
        })
    }

    private fun getVideoDuration(uriString: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, Uri.parse(uriString))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 5000L
        } catch (e: Exception) {
            5000L
        } finally {
            retriever.release()
        }
    }

    private fun setupImage(uri: String) {
        btnMuteUnmute.isVisible = false
        photoEditorView.visibility = View.VISIBLE
        playerView.visibility = View.GONE
        videoTextureView.visibility = View.GONE
        videoTrimmerView.visibility = View.GONE
        Glide.with(this).load(uri).centerInside().into(photoEditorView.source)
        attachGesturesToView(photoEditorView, isText = false)
    }

    private fun checkVolume(exoPlayer: ExoPlayer, muteVolumeButton: ImageView) {
        val isCurrentlyMuted = exoPlayer.volume == 0f
        if (isCurrentlyMuted) {
            exoPlayer.volume = 1f
            wasMutedBeforeBackground = false
            muteVolumeButton.setImageResource(R.drawable.volume_mute)
        } else {
            exoPlayer.volume = 0f
            wasMutedBeforeBackground = true
            muteVolumeButton.setImageResource(R.drawable.volume_unmute)
        }
    }

    private fun setupEditor() {
        btnText.setOnClickListener {
            editingTextView = null
            showFullscreenTextDialog()
        }

        tvShare.setOnClickListener {
            //headline_caption = etInput2.text.toString().trim()
            if (isVideoMedia) {
                if (!isRecording && !recordingTriggered) startRecordingWithTrimRange()
            } else {
                saveFinalImage()
            }
        }
    }

    private fun showFullscreenTextDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fullscreen_text, null)
        val dialog = Dialog(this, R.style.FullWidthDialog2).apply {
            setContentView(dialogView)
        }
        val window = dialog.window
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusBarColor = Color.parseColor("#000000")
                navigationBarColor = Color.parseColor("#000000")
            }
        }
        val mainRoot = dialogView.findViewById<RelativeLayout>(R.id.main_root)
        val etText = dialogView.findViewById<EditText>(R.id.etFullscreenText)
        val btnClose =
            dialogView.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.btnClose)
        val tvDoneDialog = dialogView.findViewById<Button>(R.id.tvDoneDialog)
        etText.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        etText.setPadding(40, 0, 40, 0)
        etText.setTextColor(Color.WHITE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            etText.textCursorDrawable = null
        }
        etText.setSelection(0)
        etText.background = ContextCompat.getDrawable(this, R.drawable.status_edittext_bg)
        etText.setTextColor(Color.WHITE)
        etText.includeFontPadding = false
        etText.filters = arrayOf(PillInputFilter())
        var isEditingExisting = false
        editingTextView?.let { originalTv ->
            isEditingExisting = true
            val plainText = originalTv.text.toString()
            etText.background = null
            etText.gravity = Gravity.CENTER
            etText.setText(plainText)
            applyRoundedSpansToEditText(etText)
            etText.setSelection(plainText.length)
        }
        etText.addTextChangedListener(object : TextWatcher {
            private var lastText = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s == null) return
                if (s.length > lastText.length && s.endsWith("\n")) {
                    etText.append("\u200B")
                    etText.setSelection(etText.length())
                }
                if (s.isEmpty()) {
                    etText.background = ContextCompat.getDrawable(
                        this@ImageVideoConverter,
                        R.drawable.status_edittext_bg
                    )
                    etText.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                } else {
                    etText.background = null
                    etText.gravity = Gravity.CENTER
                    applyRoundedSpansToEditText(etText)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        if (isEditingExisting) {
            etText.post { applyRoundedSpansToEditText(etText) }
        }
        dialog.setOnShowListener {
            etText.postDelayed({
                etText.requestFocus()
                if (etText.text.isEmpty()) {
                    etText.setSelection(0)
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etText, InputMethodManager.SHOW_IMPLICIT)
            }, 250)
        }
        tvDoneDialog.setOnClickListener {
            hideKeyboard(etText)
            etText.postDelayed({
                val text = etText.text
                if (!text.isNullOrEmpty()) {
                    if (editingTextView != null) {
                        editingTextView?.text = text
                    } else {
                        addTextOverlay(text.toString())
                    }
                }
                editingTextView = null
                dialog.dismiss()
            }, 150)
        }
        mainRoot.setOnClickListener {
            hideKeyboard(etText)
            etText.postDelayed({
                val text = etText.text
                if (!text.isNullOrEmpty()) {
                    if (editingTextView != null) {
                        editingTextView?.text = text
                    } else {
                        addTextOverlay(text.toString())
                    }
                }
                editingTextView = null
                dialog.dismiss()
            }, 150)
        }
        btnClose.setOnClickListener {
            hideKeyboard(etText)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyRoundedSpansToEditText(editText: EditText) {
        val content = editText.text ?: return
        val existingSpans = content.getSpans(0, content.length, RoundedBackgroundSpan::class.java)
        for (span in existingSpans) {
            content.removeSpan(span)
        }
        val textString = content.toString()
        val lines = textString.split("\n")
        var currentPos = 0
        lines.forEachIndexed { index, line ->
            val start = currentPos
            val end = currentPos + line.length
            if (start < content.length) {
                val spanEnd = if (end > start) end else start + 1
                if (spanEnd <= content.length) {
                    content.setSpan(
                        RoundedBackgroundSpan(Color.parseColor("#212328"), 32f, 38f, 12f),
                        start,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            currentPos += line.length + 1
        }
    }

    private fun addTextOverlay(text: String) {
        val spannable = SpannableStringBuilder(text)
        val lines = text.split("\n")
        var currentIndex = 0
        for (line in lines) {
            if (line.isNotEmpty()) {
                val start = currentIndex
                val end = start + line.length
                spannable.setSpan(
                    RoundedBackgroundSpan(
                        backgroundColor = Color.parseColor("#212328"),
                        cornerRadius = 32f,
                        paddingHorizontal = 38f,
                        paddingVertical = 12f
                    ),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            currentIndex += line.length + 1
        }
        val tv = TextView(this).apply {
            this.text = spannable
            typeface = ResourcesCompat.getFont(context, R.font.manrope_regular)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = null
            includeFontPadding = false
            setLineSpacing(15f, 1f)
            setPadding(0, 25, 0, 25)
        }
        val container = FrameLayout(this).apply {
            setPadding(40, 0, 40, 0)
            isClickable = true
            isLongClickable = true
        }
        container.addView(tv)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        overlayContainer.addView(container, params)
        attachGesturesToView(container, isText = true)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachGesturesToView(view: View, isText: Boolean) {
        var mode = 0
        var initialX = 0f
        var initialY = 0f
        var initialTranslationX = 0f
        var initialTranslationY = 0f
        var initialScaleX = 1f
        var initialScaleY = 1f
        var initialRotation = 0f
        var initialDistance = 0f
        var initialAngle = 0f
        var potentialLongClick: Runnable? = null
        val longPressHandler = Handler(Looper.getMainLooper())
        val longPressDelay = ViewConfiguration.getLongPressTimeout().toLong()
        val touchSlop = ViewConfiguration.get(this@ImageVideoConverter).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var longClickTriggered = false
        view.setOnTouchListener { v, event ->
            if (!isEditingEnabled) {
                longPressHandler.removeCallbacksAndMessages(null)
                return@setOnTouchListener false
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isText) showPlayPauseButton()
                    mode = 1
                    initialX = event.rawX
                    initialY = event.rawY
                    initialTranslationX = v.translationX
                    initialTranslationY = v.translationY
                    if (isText) {
                        v.bringToFront()
                        downX = event.rawX
                        downY = event.rawY
                        longClickTriggered = false
                        // Schedule a long-press check
                        potentialLongClick = Runnable {
                            if (!longClickTriggered) {
                                longClickTriggered = true
                                showDeleteMenu(v)
                            }
                        }
                        longPressHandler.postDelayed(potentialLongClick!!, longPressDelay)
                    }
                    hideRotationTooltip()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isText && !longClickTriggered && potentialLongClick != null) {
                        val dx = abs(event.rawX - downX)
                        val dy = abs(event.rawY - downY)
                        if (dx > touchSlop || dy > touchSlop) {
                            longPressHandler.removeCallbacks(potentialLongClick!!)
                            potentialLongClick = null
                        }
                    }
                    when (mode) {
                        1 -> {
                            v.translationX = initialTranslationX + (event.rawX - initialX)
                            v.translationY = initialTranslationY + (event.rawY - initialY)
                        }

                        2 -> {
                            if (event.pointerCount >= 2) {
                                val currentDistance = getRawDistance(event)
                                val currentAngle = getRawAngle(event)
                                val scaleFactor = currentDistance / initialDistance
                                v.scaleX = initialScaleX * scaleFactor
                                v.scaleY = initialScaleY * scaleFactor
                                v.rotation = initialRotation + (currentAngle - initialAngle)
                                if (abs(currentAngle - initialAngle) > 0.5f) {
                                    showRotationTooltip(v, v.rotation)
                                }
                            }
                        }
                    }
                    true
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (isText) {
                        // Cancel long press on second finger
                        longPressHandler.removeCallbacks(
                            potentialLongClick ?: return@setOnTouchListener true
                        )
                        potentialLongClick = null
                    }
                    if (event.pointerCount == 2) {
                        mode = 2
                        initialDistance = getRawDistance(event)
                        initialAngle = getRawAngle(event)
                        initialScaleX = v.scaleX
                        initialScaleY = v.scaleY
                        initialRotation = v.rotation
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    if (isText) {
                        longPressHandler.removeCallbacks(
                            potentialLongClick ?: return@setOnTouchListener true
                        )
                        potentialLongClick = null
                    }
                    mode = 0
                    hideRotationTooltip()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (isText) {
                        longPressHandler.removeCallbacks(
                            potentialLongClick ?: return@setOnTouchListener true
                        )
                        potentialLongClick = null
                    }
                    mode = 0
                    hideRotationTooltip()
                    true
                }

                else -> false
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getRawDistance(event: MotionEvent): Float {
        val dx = event.getRawX(0) - event.getRawX(1)
        val dy = event.getRawY(0) - event.getRawY(1)
        return hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    @SuppressLint("NewApi")
    private fun getRawAngle(event: MotionEvent): Float {
        val dx = (event.getRawX(1) - event.getRawX(0)).toDouble()
        val dy = (event.getRawY(1) - event.getRawY(0)).toDouble()
        return Math.toDegrees(atan2(dy, dx)).toFloat()
    }

    private fun showDeleteMenu(view: View) {
        val items = arrayOf("Edit", "Delete")
        ListPopupWindow(this).apply {
            anchorView = view
            width = 400
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.BLACK))
            setAdapter(MenuAdapter(this@ImageVideoConverter, items))
            setOnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    val tv = (view as? ViewGroup)?.getChildAt(0) as? TextView
                    editingTextView = tv
                    showFullscreenTextDialog()
                } else {
                    overlayContainer.removeView(view)
                }
                dismiss()
            }
            show()
        }
    }

    class MenuAdapter(context: Context, private val items: Array<String>) :
        ArrayAdapter<String>(context, 0, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.menu_item_layout, parent, false)
            val icon =
                view.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.menuIcon)
            val text = view.findViewById<TextView>(R.id.menuText)
            text.text = items[position]
            when (items[position]) {
                "Edit" -> icon.setImageResource(android.R.drawable.ic_menu_edit)
                "Delete" -> icon.setImageResource(android.R.drawable.ic_menu_delete)
                else -> icon.visibility = View.GONE
            }
            return view
        }
    }

    private fun showRotationTooltip(view: View, rotation: Float) {
        rotationTooltip.text = "${rotation.toInt()}°"
        rotationTooltip.visibility = View.VISIBLE
        rotationTooltip.translationX = (overlayContainer.width - rotationTooltip.width) / 2f
        rotationTooltip.translationY = (overlayContainer.height / 2f) - 150f
    }

    private fun hideRotationTooltip() {
        rotationTooltip.visibility = View.GONE
    }

    private fun startRecordingWithTrimRange() {
        if (recordingTriggered) return
        recordingTriggered = true
        var segmentDurationMs = trimEndMs - trimStartMs
        if (segmentDurationMs <= 0L) {
            trimStartMs = 0L
            trimEndMs = originalVideoDurationMs
            segmentDurationMs = originalVideoDurationMs
        }
        player?.seekTo(trimStartMs)
        player?.playWhenReady = true
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    val currentPos = player?.currentPosition ?: 0L
                    if (abs(currentPos - trimStartMs) < 100) {
                        player?.removeListener(this)
                        waitForValidDimensions(segmentDurationMs)
                    }
                }
            }
        }
        player?.addListener(listener)
    }

    private fun waitForValidDimensions(durationMs: Long) {
        if (mediaContainer.width > 0 && mediaContainer.height > 0 &&
            overlayContainer.width > 0 && overlayContainer.height > 0
        ) {
            startAndAutoSaveVideo(durationMs)
        } else {
            mediaContainer.post {
                waitForValidDimensions(durationMs)
            }
        }
    }

    private fun startAndAutoSaveVideo(durationMs: Long) {
        showVideoConvertingPercentage(true)
        val width = (mediaContainer.width / 2) * 2
        val height = (mediaContainer.height / 2) * 2
        if (width <= 0 || height <= 0) {
            showVideoConvertingPercentage(false)
            Toast.makeText(this, "Invalid view size. Please try again.", Toast.LENGTH_SHORT).show()
            recordingTriggered = false
            return
        }
        val videoFile = File(cacheDir, "HiHlo_${System.currentTimeMillis()}.mp4")
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else MediaRecorder()
            mediaRecorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(8000000)
                setOutputFile(videoFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "MediaRecorder start failed", e)
            showVideoConvertingPercentage(false)
            Toast.makeText(this, "Cannot start recording: ${e.message}", Toast.LENGTH_LONG).show()
            cleanupRecorder()
            recordingTriggered = false
            return
        }

        isRecording = true
        tvShare.isEnabled = false
        tvShare.text = "Converting"

        /*val recordingSurface = mediaRecorder!!.surface
        val choreographer = Choreographer.getInstance()
        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRecording) return
                try {
                    val canvas = recordingSurface.lockHardwareCanvas()
                    // If canvas is null, just skip this frame – don't stop recording.
                    if (canvas != null) {
                        canvas.drawColor(Color.BLACK)
                        mediaContainer.draw(canvas)
                        overlayContainer.draw(canvas)
                        recordingSurface.unlockCanvasAndPost(canvas)
                    }
                } catch (e: Exception) {
                    Log.e("ImageVideoConverter", "Frame drawing error", e)
                }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(frameCallback)*/

        val recordingSurface = mediaRecorder!!.surface
        renderThread = HandlerThread("VideoRenderThread")
        renderThread?.start()
        val renderHandler = Handler(renderThread!!.looper)
        val frameInterval = 1000L / 24L // 24 FPS
        val renderRunnable = object : Runnable {
            override fun run() {
                if (!isRecording) return
                try {
                    val canvas = recordingSurface.lockHardwareCanvas()
                    if (canvas != null) {
                        canvas.drawColor(Color.BLACK)
                        mediaContainer.draw(canvas)
                        overlayContainer.draw(canvas)
                        recordingSurface.unlockCanvasAndPost(canvas)
                    }
                } catch (e: Exception) {
                    Log.e("ImageVideoConverter", "Frame drawing error", e)
                }
                renderHandler.postDelayed(this, frameInterval)
            }
        }
        renderHandler.post(renderRunnable)

        Handler(Looper.getMainLooper()).postDelayed({
            stopVideoRecording(videoFile, durationMs)
        }, durationMs)
    }

    private fun stopVideoRecording(videoFile: File, originalDurationMs: Long) {
        if (!isRecording) return
        isRecording = false
        renderThread?.quitSafely()
        renderThread = null

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        cleanupRecorder()
        player?.playWhenReady = false
        Handler(Looper.getMainLooper()).postDelayed({
            muxVideoWithOriginalAudio(videoFile, originalDurationMs)
        }, 100)
    }

    private fun cleanupRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }

    private fun muxVideoWithOriginalAudio(silentVideoFile: File, durationMs: Long) {
        val outputFile = File(cacheDir, "HiHlo_Final_${System.currentTimeMillis()}.mp4")
        var muxer: MediaMuxer? = null
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        try {
            if (originalVideoUri == null) throw Exception("Original video URI is null")
            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(silentVideoFile.absolutePath)
            val videoTrackIndex = (0 until videoExtractor.trackCount).firstOrNull { index ->
                val format = videoExtractor.getTrackFormat(index)
                format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: throw Exception("No video track found in recorded file")
            audioExtractor = MediaExtractor()
            try {
                val fd = contentResolver.openFileDescriptor(originalVideoUri!!, "r")
                if (fd != null) {
                    audioExtractor.setDataSource(fd.fileDescriptor)
                } else {
                    audioExtractor.setDataSource(this@ImageVideoConverter, originalVideoUri!!, null)
                }
            } catch (e: Exception) {
                audioExtractor.setDataSource(this@ImageVideoConverter, originalVideoUri!!, null)
            }
            val audioTrackIndex = (0 until audioExtractor.trackCount).firstOrNull { index ->
                val format = audioExtractor.getTrackFormat(index)
                format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
            val videoTrack = muxer.addTrack(videoFormat)
            videoExtractor.selectTrack(videoTrackIndex)
            var audioTrack = -1
            if (audioTrackIndex != null) {
                val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)
                audioTrack = muxer.addTrack(audioFormat)
                audioExtractor.selectTrack(audioTrackIndex)
            }
            muxer.start()
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = getSampleFlags(videoExtractor)
                muxer.writeSampleData(videoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }
            if (audioTrack != -1) {
                val startUs = trimStartMs * 1000L
                val endUs = trimEndMs * 1000L
                audioExtractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime - startUs
                    if (bufferInfo.presentationTimeUs > (endUs - startUs)) break
                    bufferInfo.flags = getSampleFlags(audioExtractor)
                    muxer.writeSampleData(audioTrack, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }
            muxer.stop()
            useFinalFile(outputFile)
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "Muxing failed", e)
            Toast.makeText(this, "Failed to add audio: ${e.message}", Toast.LENGTH_LONG).show()
            useFinalFile(silentVideoFile)   // fallback to silent video
        } finally {
            videoExtractor?.release()
            audioExtractor?.release()
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            silentVideoFile.delete()
        }
    }

    private fun getSampleFlags(extractor: MediaExtractor): Int {
        return try {
            extractor.sampleFlags
        } catch (e: Exception) {
            0
        }
    }

    private fun useFinalFile(videoFile: File) {
        try {
            if (!videoFile.exists() || videoFile.length() == 0L) {
                throw Exception("Video file is empty")
            }
            val authority = "${packageName}.provider"
            val contentUri = FileProvider.getUriForFile(this, authority, videoFile)
            savedUri = contentUri
            mediaType = "video"
            recordingTriggered = false
            tvShare.isEnabled = true
            tvShare.text = "Share"
            Handler(Looper.getMainLooper()).postDelayed({
                finalizeData()
            }, 500)
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "File provider error", e)
            showVideoConvertingPercentage(false)
            tvShare.isEnabled = true
            tvShare.text = "Share"
            Toast.makeText(this, "Failed to save video", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFinalImage() {
        showVideoConvertingPercentage(true)
        tvShare.text = "Converting"
        val bitmap = createBitmap(mediaContainer.width, mediaContainer.height)
        Canvas(bitmap).apply {
            mediaContainer.draw(this)
            overlayContainer.draw(this)
        }
        val imageFile = File(cacheDir, "HiHlo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        val contentUri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
        savedUri = contentUri
        mediaType = "image"
        tvShare.text = "Share"
        recordingTriggered = false
        tvShare.isEnabled = true
        tvShare.text = "Share"
        finalizeData()
    }

    fun finalizeData() {
        UserPreference.selectedMediaType = if (mediaType == "video") "V" else "I"
        val uri = savedUri?.toString() ?: ""
        val contentUri = uri.toUri()
        val file = getCacheFileFromContentUri(contentUri)
        UserPreference.uploadStatusFile = file
        showVideoConvertingPercentage(false)
        val intent = Intent(this, UploadStatusActivity::class.java)
        captionLauncher.launch(intent)
    }

    private fun getCacheFileFromContentUri(contentUri: Uri): File? {
        return try {
            val cursor = contentResolver.query(
                contentUri,
                null,
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val dataColumn =
                        it.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (dataColumn != -1) {
                        val filePath = it.getString(dataColumn)
                        return File(filePath)
                    }
                }
            }

            // Fallback copy
            val tempFile = File(
                cacheDir,
                "temp_${System.currentTimeMillis()}.${
                    contentUri.lastPathSegment
                        ?.substringAfterLast('.') ?: "file"
                }"
            )

            contentResolver.openInputStream(contentUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            tempFile

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPause() {
        super.onPause()
        player?.let {
            wasPlayingBeforeBackground = it.isPlaying
            wasMutedBeforeBackground = it.volume == 0f
            if (it.isPlaying) it.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        player?.let {
            if (wasMutedBeforeBackground) {
                it.volume = 0f
                btnMuteUnmute.setImageResource(R.drawable.volume_unmute)
            } else {
                it.volume = 1f
                btnMuteUnmute.setImageResource(R.drawable.volume_mute)
            }
            if (isVideoMedia && wasPlayingBeforeBackground && !isRecording) {
                it.play()
            }
        }
        wasPlayingBeforeBackground = false
    }

    override fun onStop() {
        super.onStop()
        player?.let {
            wasPlayingBeforeBackground = it.isPlaying
            wasMutedBeforeBackground = it.volume == 0f
            if (it.isPlaying) it.pause()
        }
        showVideoConvertingPercentage(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacksAndMessages(null)
        autoHideHandler.removeCallbacksAndMessages(null)
        //etInput2.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        player?.release()
        cleanupRecorder()
        showVideoConvertingPercentage(false)
    }

    private fun showVideoConvertingPercentage(visible: Boolean) {
        if (visible) {
            videoConvertingPercentageDialog?.dismiss()
            videoConvertingPercentageDialog =
                VideoConvertingPercentageDialog(this, originalVideoDurationMs)
            videoConvertingPercentageDialog?.setCancelable(false)
            videoConvertingPercentageDialog?.show()
        } else {
            videoConvertingPercentageDialog?.dismiss()
        }
    }
}

class PillInputFilter : InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int,
    ): CharSequence? {
        return null
    }
}