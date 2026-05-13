package com.app.hihlo.ImageVideoConverter


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.*
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import androidx.core.content.res.ResourcesCompat

import android.text.InputFilter
import android.text.Spanned
import android.util.TypedValue

class ImageVideoConverter : AppCompatActivity() {

    // Views
    private lateinit var mediaContainer: FrameLayout
    private lateinit var overlayContainer: FrameLayout
    private lateinit var photoEditorView: PhotoEditorView
    private lateinit var playerView: PlayerView
    private lateinit var videoTextureView: TextureView
    private lateinit var btnText: Button
    private lateinit var btnDone: Button
    private lateinit var btnDone2: Button
    private lateinit var inputLayout2: LinearLayout
    private lateinit var etInput2: EditText
    private lateinit var tvDone2: TextView
    private lateinit var ivBack: ImageView
    private lateinit var videoTrimmerView: VideoTrimmerView
    private lateinit var btnPlayPause: ImageView
    private lateinit var rotationTooltip: TextView

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

    private var headline_caption: String = ""

    private enum class MediaStep {
        TRIM, TEXT_OVERLAY, HEADLINE
    }

    private var currentStep = MediaStep.TEXT_OVERLAY

    private val progressHandler = Handler(Looper.getMainLooper())
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hidePlayPauseButton() }
    private val autoHideDelayMs = 5000L
    private var wasPlayingBeforeBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.image_video_converter)
        initViews()
        mediaContainer.isDrawingCacheEnabled = true
        overlayContainer.isDrawingCacheEnabled = true
        val uri = intent.getStringExtra("uri") ?: ""
        originalVideoUri = Uri.parse(uri)
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
        etInput2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                headline_caption = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initViews() {
        mediaContainer = findViewById(R.id.mediaContainer)
        overlayContainer = findViewById(R.id.overlayContainer)
        photoEditorView = findViewById(R.id.photoEditorView)
        playerView = findViewById(R.id.playerView)
        videoTextureView = TextureView(this).apply { visibility = View.GONE }
        btnText = findViewById(R.id.btnText)
        btnDone = findViewById(R.id.btnDone)
        btnDone2 = findViewById(R.id.btnDone2)
        inputLayout2 = findViewById(R.id.inputLayout2)
        etInput2 = findViewById(R.id.etInput2)
        tvDone2 = findViewById(R.id.tvDone2)
        ivBack = findViewById(R.id.ivBack)
        videoTrimmerView = findViewById(R.id.videoTrimmerView)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        //mediaContainer.addView(videoTextureView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun setupRotationTooltip() {
        rotationTooltip = TextView(this).apply {
            setBackgroundResource(R.drawable.bg_rotation_tooltip)
            setTextColor(Color.BLACK)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ImageVideoConverter, R.font.manrope_semibold)
            visibility = View.GONE
            setPadding(32, 8, 32, 8)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        overlayContainer.addView(rotationTooltip)
    }

    private fun setupListeners() {
        btnDone2.setOnClickListener {
            when (currentStep) {
                MediaStep.TRIM -> {
                    trimStartMs = videoTrimmerView.getTrimStartMs()
                    trimEndMs = videoTrimmerView.getTrimEndMs()
                    goToStep(MediaStep.TEXT_OVERLAY)
                }
                MediaStep.TEXT_OVERLAY -> goToStep(MediaStep.HEADLINE)
                MediaStep.HEADLINE -> {}
            }
        }

        btnDone.setOnClickListener {
            headline_caption = etInput2.text.toString().trim()
            if (isVideoMedia) {
                if (!isRecording) startRecordingWithTrimRange()
            } else {
                saveFinalImage()
            }
        }

        ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnPlayPause.setOnClickListener { togglePlayback() }
        playerView.setOnClickListener { togglePlayback() }
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
                    currentStep == MediaStep.HEADLINE -> goToStep(MediaStep.TEXT_OVERLAY)
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
                inputLayout2.visibility = View.GONE
                btnDone2.isVisible = true
                btnDone.isVisible = false
                btnText.isVisible = false
            }
            MediaStep.TEXT_OVERLAY -> {
                videoTrimmerView.visibility = View.GONE
                inputLayout2.visibility = View.GONE
                btnDone2.isVisible = true
                btnDone.isVisible = false
                btnText.isVisible = true
            }
            MediaStep.HEADLINE -> {
                videoTrimmerView.visibility = View.GONE
                inputLayout2.visibility = View.VISIBLE
                btnDone2.isVisible = false
                btnDone.isVisible = false
                btnText.isVisible = false

                if (etInput2.text.toString() != headline_caption) {
                    etInput2.setText(headline_caption)
                    etInput2.setSelection(etInput2.text.length)
                }
                etInput2.requestFocus()
            }
        }
        showPlayPauseButton()
    }

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
        photoEditorView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.repeatMode = Player.REPEAT_MODE_OFF
        player?.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        player?.prepare()
        player?.play()
//        playerView.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN && isVideoMedia) {
//                showPlayPauseButton()
//            }
//            false   // let the player handle the touch normally
//        }
        originalVideoDurationMs = getVideoDuration(uri)
        videoTrimmerView.setVideoUri(Uri.parse(uri), originalVideoDurationMs)
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
        photoEditorView.visibility = View.VISIBLE
        playerView.visibility = View.GONE
        videoTextureView.visibility = View.GONE
        videoTrimmerView.visibility = View.GONE

        Glide.with(this).load(uri).centerInside().into(photoEditorView.source)
        attachGesturesToView(photoEditorView, isText = false)
    }

    private fun setupEditor() {
        btnText.setOnClickListener {
            editingTextView = null
            showFullscreenTextDialog()
        }

//        tvDone2.setOnClickListener {
//            headline_caption = etInput2.text.toString().trim()
//            //etInput2.setText("")
//            // Step change handled by btnDone2
//        }
        tvDone2.setOnClickListener {
            headline_caption = etInput2.text.toString().trim()
            if (isVideoMedia) {
                if (!isRecording) startRecordingWithTrimRange()
            } else {
                saveFinalImage()
            }
        }
    }


    // ================= showFullscreenTextDialog (Keep as is) =================
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
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val tvDoneDialog = dialogView.findViewById<Button>(R.id.tvDoneDialog)
        etText.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        etText.setPadding(40, 0, 40, 0) // This keeps the cursor inside the center-drawn pills
        etText.setTextColor(Color.WHITE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            etText.textCursorDrawable = null // Makes cursor white
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

            // SETUP BEFORE SETTING TEXT
            etText.background = null // Default background remove karein
            etText.gravity = Gravity.CENTER // Gravity center rakhein editing ke liye

            etText.setText(plainText)
            applyRoundedSpansToEditText(etText)
            etText.setSelection(plainText.length)
        }
        // Inside showFullscreenTextDialog
        etText.addTextChangedListener(object : TextWatcher {
            private var lastText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s == null) return

                // New line handle karne ke liye: agar user ne Enter dabaya
                if (s.length > lastText.length && s.endsWith("\n")) {
                    etText.append("\u200B") // Temporary char for pill visibility
                    etText.setSelection(etText.length())
                }

                // Background update karein
                if (s.isEmpty()) {
                    etText.background = ContextCompat.getDrawable(this@ImageVideoConverter, R.drawable.status_edittext_bg)
                    etText.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                } else {
                    etText.background = null
                    etText.gravity = Gravity.CENTER
                    applyRoundedSpansToEditText(etText)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Agar user backspace dabakar \u200B delete kar raha hai, toh natural feel hoga
            }
        })
        if (isEditingExisting) {
            etText.post {
                applyRoundedSpansToEditText(etText)
            }
        }
        dialog.setOnShowListener {
            etText.postDelayed({
                etText.requestFocus()
                // Force selection to start ONLY if it's new text
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun applyRoundedSpansToEditText(editText: EditText) {
        val content = editText.text ?: return

        // 1. Clear existing spans
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

            // Agar line empty hai (sirf newline hai), toh background draw nahi hota.
            // Hum sirf valid characters par hi span lagayenge.
            if (start < content.length) {
                val spanEnd = if (end > start) end else start + 1 // Kam se kam 1 char length

                if (spanEnd <= content.length) {
                    content.setSpan(
                        RoundedBackgroundSpan(Color.parseColor("#212328"), 32f, 38f, 12f),
                        start,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE // Backspace fix ke liye
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

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isText) showDeleteMenu(view)
            }
        })

        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isText) showPlayPauseButton()
                    mode = 1
                    initialX = event.rawX
                    initialY = event.rawY
                    initialTranslationX = v.translationX
                    initialTranslationY = v.translationY
                    if (isText) v.bringToFront()
                    hideRotationTooltip()
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
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
                MotionEvent.ACTION_MOVE -> {
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
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
                if (position == 0) { // Edit
                    val tv = (view as? ViewGroup)?.getChildAt(0) as? TextView
                    editingTextView = tv
                    showFullscreenTextDialog()
                } else { // Delete
                    overlayContainer.removeView(view)
                }
                dismiss()
            }
            show()
        }
    }

    class MenuAdapter(context: Context, private val items: Array<String>) : ArrayAdapter<String>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.menu_item_layout, parent, false)
            val icon = view.findViewById<ImageView>(R.id.menuIcon)
            val text = view.findViewById<TextView>(R.id.menuText)

            text.text = items[position]

            // Optionally set icons based on item name (use your own drawables if needed)
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

    // ====================== Recording Functions ======================
    // ====================== Recording Functions ======================

    private fun startRecordingWithTrimRange() {
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
                        Handler(Looper.getMainLooper()).postDelayed({
                            startAndAutoSaveVideo(segmentDurationMs)
                        }, 50)
                    }
                }
            }
        }
        player?.addListener(listener)
    }

    private fun startAndAutoSaveVideo(durationMs: Long) {
        ProcessDialog.showDialog(this, true)

        val width = (mediaContainer.width / 2) * 2
        val height = (mediaContainer.height / 2) * 2
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
            ProcessDialog.dismissDialog(true)
            Toast.makeText(this, "Cannot start recording: ${e.message}", Toast.LENGTH_LONG).show()
            cleanupRecorder()
            return
        }

        isRecording = true
        tvDone2.isEnabled = false
        tvDone2.text = "Converting"

        val recordingSurface = mediaRecorder!!.surface
        val choreographer = Choreographer.getInstance()

        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRecording) return
                try {
                    val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        recordingSurface.lockHardwareCanvas()
                    } else {
                        recordingSurface.lockCanvas(null)
                    }
                    canvas.drawColor(Color.BLACK)
                    mediaContainer.draw(canvas)
                    overlayContainer.draw(canvas)
                    recordingSurface.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Log.e("ImageVideoConverter", "Frame drawing error", e)
                }
                choreographer.postFrameCallback(this)
            }
        }

        choreographer.postFrameCallback(frameCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            stopVideoRecording(videoFile, durationMs)
        }, durationMs)
    }

    private fun stopVideoRecording(videoFile: File, originalDurationMs: Long) {
        if (!isRecording) return
        isRecording = false

        try { mediaRecorder?.stop() } catch (_: Exception) {}
        cleanupRecorder()

        player?.playWhenReady = false

        ProcessDialog.showDialog(this, true)
        Handler(Looper.getMainLooper()).postDelayed({
            muxVideoWithOriginalAudio(videoFile, originalDurationMs)
        }, 100)
    }

    private fun cleanupRecorder() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
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
            audioExtractor.setDataSource(this@ImageVideoConverter, originalVideoUri!!, null)

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

            // Copy Video
            while (true) {
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = getSampleFlags(videoExtractor)
                muxer.writeSampleData(videoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Copy Trimmed Audio
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
            useFinalFile(silentVideoFile)
        } finally {
            videoExtractor?.release()
            audioExtractor?.release()
            try { muxer?.release() } catch (_: Exception) {}
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

            tvDone2.text = "Done!"
            ProcessDialog.dismissDialog(true)
            Handler(Looper.getMainLooper()).postDelayed({ finalize_data() }, 500)
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "File provider error", e)
            ProcessDialog.dismissDialog(true)
            tvDone2.isEnabled = true
            tvDone2.text = "Send"
            Toast.makeText(this, "Failed to save video", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFinalImage() {
        ProcessDialog.showDialog(this, true)
        tvDone2.text = "Converting"

        val bitmap = Bitmap.createBitmap(mediaContainer.width, mediaContainer.height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            mediaContainer.draw(this)
            overlayContainer.draw(this)
        }

        val imageFile = File(cacheDir, "HiHlo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

        val contentUri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
        savedUri = contentUri
        mediaType = "image"

        tvDone2.text = "Done!"
        ProcessDialog.dismissDialog(true)
        finalize_data()
    }

    fun finalize_data() {
        setResult(RESULT_OK, Intent().apply {
            putExtra("uri", savedUri?.toString() ?: "")
            putExtra("type", mediaType)
            putExtra("headline_caption", headline_caption)
        })
        finish()
    }

    override fun onPause() {
        super.onPause()
        player?.let { wasPlayingBeforeBackground = it.isPlaying; if (it.isPlaying) it.pause() }
    }

    override fun onResume() {
        super.onResume()
        if (isVideoMedia && wasPlayingBeforeBackground && !isRecording) player?.play()
        wasPlayingBeforeBackground = false
    }

    // ... your existing code ...

    override fun onStop() {
        super.onStop()
        player?.let { wasPlayingBeforeBackground = it.isPlaying; if (it.isPlaying) it.pause() }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacksAndMessages(null)
        autoHideHandler.removeCallbacksAndMessages(null)
        player?.release()
        cleanupRecorder()
    }
}

// ====================== INPUT FILTER FOR BETTER DELETION ======================


class PillInputFilter : InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        return null
    }
}