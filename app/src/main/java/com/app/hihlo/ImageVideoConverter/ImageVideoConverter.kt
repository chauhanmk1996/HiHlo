package com.app.hihlo.ImageVideoConverter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.core.app.ActivityCompat
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

class ImageVideoConverter : AppCompatActivity() {

    private lateinit var mediaContainer: FrameLayout
    private lateinit var overlayContainer: FrameLayout
    private lateinit var photoEditorView: PhotoEditorView
    private lateinit var playerView: PlayerView
    private lateinit var btnText: Button
    private lateinit var btnDone: Button
    private lateinit var btnDone2: Button              // "Next" button
    private lateinit var inputLayout: LinearLayout
    private lateinit var etInput: EditText
    private lateinit var tvDone: TextView
    private lateinit var ivBack: ImageView
    private lateinit var videoTrimmerView: VideoTrimmerView

    private var player: ExoPlayer? = null
    private var isVideoMedia = false
    private var editingTextView: TextView? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private var savedUri: Uri? = null
    private var mediaType: String = ""

    private var trimStartMs = 0L
    private var trimEndMs = Long.MAX_VALUE
    private var originalVideoDurationMs = 0L
    private var originalVideoUri: Uri? = null

    // Steps for video flow: step 1 = trim, step 2 = caption/send
    private enum class VideoStep { TRIM, CAPTION }
    private var currentVideoStep = VideoStep.TRIM
    private val progressHandler = Handler(Looper.getMainLooper())
    private lateinit var rotationTooltip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.image_video_converter)
        initViews()
        val uri = intent.getStringExtra("uri") ?: ""
        originalVideoUri = Uri.parse(uri)
        isVideoMedia = intent.getBooleanExtra("isVideo", false)
        if (isVideoMedia) {
            setupVideo(uri)
            goToStep(VideoStep.TRIM)
            //requestAudioPermission()
            attachGesturesToView(playerView, isText = false)   // ✅ restored
        } else {
            setupImage(uri)
            btnDone.text = "Send"
            btnDone.isVisible = true
            btnDone2.isVisible = false
            btnText.isVisible = false
            inputLayout.isVisible = true
            attachGesturesToView(photoEditorView, isText = false)   // ✅ restored
        }
        setupEditor()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isVideoMedia && currentVideoStep == VideoStep.CAPTION) {
                    goToStep(VideoStep.TRIM)
                } else {
                    finish()
                }
            }
        })
        btnDone2.setOnClickListener {
            if (isVideoMedia) {
                trimStartMs = videoTrimmerView.getTrimStartMs()
                trimEndMs = videoTrimmerView.getTrimEndMs()
                goToStep(VideoStep.CAPTION)
            }
        }
        btnDone.setOnClickListener {
            if (isVideoMedia) {
                if (!isRecording) startRecordingWithTrimRange()
            } else {
                saveFinalImage()
            }
        }
        ivBack.setOnClickListener {
            if (isVideoMedia && currentVideoStep == VideoStep.CAPTION) {
                goToStep(VideoStep.TRIM)
            } else {
                finish()
            }
        }
        rotationTooltip = TextView(this).apply {
            setBackgroundResource(R.drawable.bg_rotation_tooltip)
            setTextColor(Color.BLACK)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ImageVideoConverter, R.font.manrope_semibold)  // ✅
            visibility = View.GONE
            setPaddingRelative(
                (16 * resources.displayMetrics.density).toInt(),
                4,
                (16 * resources.displayMetrics.density).toInt(),
                4
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        overlayContainer.addView(rotationTooltip)
    }

    private fun initViews() {
        mediaContainer = findViewById(R.id.mediaContainer)
        overlayContainer = findViewById(R.id.overlayContainer)
        photoEditorView = findViewById(R.id.photoEditorView)
        playerView = findViewById(R.id.playerView)
        btnText = findViewById(R.id.btnText)
        btnDone = findViewById(R.id.btnDone)
        btnDone2 = findViewById(R.id.btnDone2)
        inputLayout = findViewById(R.id.inputLayout)
        etInput = findViewById(R.id.etInput)
        tvDone = findViewById(R.id.tvDone)
        ivBack = findViewById(R.id.ivBack)
        videoTrimmerView = findViewById(R.id.videoTrimmerView)
    }

    private fun setupVideo(uri: String) {
        photoEditorView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.repeatMode = Player.REPEAT_MODE_OFF
        player?.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        player?.prepare()
        player?.play()
        originalVideoDurationMs = getVideoDuration(uri)
        videoTrimmerView.setVideoUri(Uri.parse(uri), originalVideoDurationMs)
        trimStartMs = 0L
        trimEndMs = originalVideoDurationMs
        videoTrimmerView.setOnTrimChangedListener { start, end ->
            trimStartMs = start
            trimEndMs = end
            val current = player?.currentPosition ?: 0L
            when {
                current < trimStartMs -> {
                    player?.seekTo(trimStartMs)
                }
                current > trimEndMs -> {
                    player?.seekTo(trimStartMs)
                }
            }
            player?.playWhenReady = true
        }
        videoTrimmerView.setOnScrubChangedListener { ms ->
            player?.seekTo(ms)
            videoTrimmerView.updateProgress(ms)
        }
        startProgressLoop()
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
        Glide.with(this).load(uri).centerInside().into(photoEditorView.source)
        videoTrimmerView.visibility = View.GONE
    }

    private fun goToStep(step: VideoStep) {
        currentVideoStep = step
        when (step) {
            VideoStep.TRIM -> {
                if (trimEndMs != Long.MAX_VALUE) {
                    videoTrimmerView.setTrimRange(trimStartMs, trimEndMs)
                }
                videoTrimmerView.visibility = View.VISIBLE
                btnDone2.isVisible = true
                btnDone.isVisible = false
                btnText.isVisible = false
                inputLayout.isVisible = false
            }
            VideoStep.CAPTION -> {
                videoTrimmerView.visibility = View.GONE
                btnDone2.isVisible = false
                btnDone.isVisible = true
                btnText.isVisible = true
                inputLayout.isVisible = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachGesturesToView(view: View, isText: Boolean) {
        view.post {
            view.pivotX = view.width / 2f
            view.pivotY = view.height / 2f
        }
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
        var tooltipShown = false
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isText) showDeleteMenu(view)
            }
        })
        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            val pointerCount = event.pointerCount
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mode = 1
                    initialX = event.rawX
                    initialY = event.rawY
                    initialTranslationX = v.translationX
                    initialTranslationY = v.translationY
                    if (isText) v.bringToFront()
                    hideRotationTooltip()       // hide if a new gesture starts
                    tooltipShown = false
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (pointerCount == 2) {
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
                            val dx = event.rawX - initialX
                            val dy = event.rawY - initialY
                            v.translationX = initialTranslationX + dx
                            v.translationY = initialTranslationY + dy
                        }
                        2 -> {
                            if (pointerCount >= 2) {
                                val currentDistance = getRawDistance(event)
                                val currentAngle = getRawAngle(event)
                                val scaleFactor = currentDistance / initialDistance
                                v.scaleX = initialScaleX * scaleFactor
                                v.scaleY = initialScaleY * scaleFactor
                                val angleDelta = currentAngle - initialAngle
                                val newRotation = initialRotation + angleDelta
                                v.rotation = newRotation
                                val rotating = abs(angleDelta) > 0.5f
                                if (rotating) {
                                    showRotationTooltip(v, newRotation)
                                    tooltipShown = true
                                } else if (tooltipShown) {
                                    hideRotationTooltip()
                                    tooltipShown = false
                                }
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = 0
                    hideRotationTooltip()   // ✅
                    tooltipShown = false
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
        val icons = arrayOf(R.drawable.icon_edit, R.drawable.delete_icon)
        val listPopupWindow = ListPopupWindow(this)
        listPopupWindow.anchorView = view
        listPopupWindow.width = 400
        listPopupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.BLACK))
        val adapter = object : ArrayAdapter<String>(this, R.layout.menu_item_layout, R.id.menuText, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val tv = v.findViewById<TextView>(R.id.menuText)
                val iv = v.findViewById<ImageView>(R.id.menuIcon)
                tv.text = items[position]
                iv.setImageResource(icons[position])
                iv.setColorFilter(Color.WHITE)
                return v
            }
        }
        listPopupWindow.setAdapter(adapter)
        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            when (items[position]) {
                "Delete" -> overlayContainer.removeView(view)
                "Edit" -> {
                    val tv = (view as? ViewGroup)?.getChildAt(0) as? TextView
                    openInputBar(tv)
                }
            }
            listPopupWindow.dismiss()
        }
        listPopupWindow.show()
    }

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
        btnDone.isEnabled = false
        btnDone.text = "Converting"

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

        // Now mux with original audio
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

            // ==================== Video Extractor (Silent Recorded) ====================
            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(silentVideoFile.absolutePath)

            val videoTrackIndex = (0 until videoExtractor.trackCount).firstOrNull { index ->
                val format = videoExtractor.getTrackFormat(index)
                format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: throw Exception("No video track found in recorded file")

            // ==================== Audio Extractor (Original Video) ====================
            audioExtractor = MediaExtractor()
            audioExtractor.setDataSource(this@ImageVideoConverter, originalVideoUri!!, null)

            val audioTrackIndex = (0 until audioExtractor.trackCount).firstOrNull { index ->
                val format = audioExtractor.getTrackFormat(index)
                format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }

            // ==================== Setup Muxer ====================
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Add Video Track
            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
            val videoTrack = muxer.addTrack(videoFormat)
            videoExtractor.selectTrack(videoTrackIndex)

            // Add Audio Track (if available)
            var audioTrack = -1
            if (audioTrackIndex != null) {
                val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)
                audioTrack = muxer.addTrack(audioFormat)
                audioExtractor.selectTrack(audioTrackIndex)
            } else {
                Log.w("ImageVideoConverter", "No audio track found in original video")
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            // ==================== Copy Video Track ====================
            var videoFrameCount = 0
            while (true) {
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = getSampleFlags(videoExtractor)
                muxer.writeSampleData(videoTrack, buffer, bufferInfo)
                videoExtractor.advance()
                videoFrameCount++
            }

            Log.d("ImageVideoConverter", "Copied $videoFrameCount video frames")

            // ==================== Copy Trimmed Audio Track ====================
            if (audioTrack != -1) {
                val startUs = trimStartMs * 1000L
                val endUs = trimEndMs * 1000L

                audioExtractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                var audioSampleCount = 0
                while (true) {
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime - startUs
                    if (bufferInfo.presentationTimeUs > (endUs - startUs)) break

                    bufferInfo.flags = getSampleFlags(audioExtractor)
                    muxer.writeSampleData(audioTrack, buffer, bufferInfo)
                    audioExtractor.advance()
                    audioSampleCount++
                }
                Log.d("ImageVideoConverter", "Copied $audioSampleCount audio samples")
            }

            muxer.stop()
            useFinalFile(outputFile)

        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "Muxing failed", e)
            Toast.makeText(this, "Failed to add audio: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            useFinalFile(silentVideoFile) // fallback to silent video
        } finally {
            videoExtractor?.release()
            audioExtractor?.release()
            try { muxer?.release() } catch (_: Exception) {}
            silentVideoFile.delete() // clean temporary silent file
        }
    }

    /** Safe way to get sample flags (compatible with API < 28) */
    private fun getSampleFlags(extractor: MediaExtractor): Int {
        return try {
            extractor.sampleFlags
        } catch (e: Exception) {
            0 // fallback
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

            btnDone.text = "Done!"
            ProcessDialog.dismissDialog(true)
            Handler(Looper.getMainLooper()).postDelayed({ finalize_data() }, 500)
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "File provider error", e)
            ProcessDialog.dismissDialog(true)
            btnDone.isEnabled = true
            btnDone.text = "Send"
            Toast.makeText(this, "Failed to save video", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFinalImage() {
        ProcessDialog.showDialog(this@ImageVideoConverter, true)
        btnDone.text = "Converting"
        val bitmap = Bitmap.createBitmap(mediaContainer.width, mediaContainer.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        mediaContainer.draw(canvas)
        overlayContainer.draw(canvas)
        val imageFile = File(cacheDir, "HiHlo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        try {
            val authority = "${packageName}.provider"
            val contentUri = FileProvider.getUriForFile(this, authority, imageFile)
            savedUri = contentUri
            mediaType = "image"
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "Image save error", e)
            ProcessDialog.dismissDialog(true)
            btnDone.isEnabled = true
            btnDone.text = "Send"
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_LONG).show()
            return
        }
        btnDone.text = "Done!"
        ProcessDialog.dismissDialog(true)
        finalize_data()
    }

    fun finalize_data() {
        val intent = android.content.Intent().apply {
            putExtra("uri", savedUri?.toString() ?: "")
            putExtra("type", mediaType)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setupEditor() {
        btnText.setOnClickListener { openInputBar(null) }
        tvDone.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                if (editingTextView != null) editingTextView?.text = text
                else addTextOverlay(text)
            }
            closeInputBar()
        }
    }

    private fun addTextOverlay(text: String) {
        val container = FrameLayout(this).apply {
            setPadding(40, 20, 40, 20)
            isClickable = true
            isLongClickable = true
        }
        val customTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_regular)
        val tv = TextView(this).apply {
            this.text = text
            this.typeface = customTypeface
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
        }
        container.addView(tv)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        overlayContainer.addView(container, params)
        attachGesturesToView(container, isText = true)
    }

    private fun openInputBar(target: TextView?) {
        editingTextView = target
        inputLayout.visibility = View.VISIBLE
        btnText.visibility = View.GONE
        etInput.setText(target?.text ?: "")
        etInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etInput, 0)
    }

    private fun closeInputBar() {
        inputLayout.visibility = View.GONE
        editingTextView = null
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
        btnText.isVisible = true
    }

//    private fun checkAudioPermission(): Boolean =
//        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
//
//    private fun requestAudioPermission() {
//        if (!checkAudioPermission()) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.RECORD_AUDIO),
//                RECORD_AUDIO_REQUEST_CODE
//            )
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacksAndMessages(null)
        player?.release()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun showRotationTooltip(view: View, rotation: Float) {
        // Update the text and make the tooltip visible
        rotationTooltip.text = "${Math.round(rotation)}°"
        if (rotationTooltip.visibility != View.VISIBLE) {
            rotationTooltip.visibility = View.VISIBLE
        }

        // Measure the tooltip so we know its real size
        rotationTooltip.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val w = rotationTooltip.measuredWidth
        val h = rotationTooltip.measuredHeight

        // Position it exactly in the centre of the overlayContainer (= centre of the screen)
        rotationTooltip.translationX = (overlayContainer.width / 2f) - (w / 2f)
        rotationTooltip.translationY = (overlayContainer.height / 2f) - (h / 2f)
    }

    private fun hideRotationTooltip() {
        if (rotationTooltip.visibility == View.VISIBLE) {
            rotationTooltip.visibility = View.GONE
        }
    }
}