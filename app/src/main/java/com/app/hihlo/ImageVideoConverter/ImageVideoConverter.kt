package com.app.hihlo.ImageVideoConverter

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
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
import kotlin.math.atan2
import kotlin.math.hypot

class ImageVideoConverter : AppCompatActivity() {

    private lateinit var mediaContainer: FrameLayout
    private lateinit var overlayContainer: FrameLayout
    private lateinit var photoEditorView: PhotoEditorView
    private lateinit var playerView: PlayerView
    private lateinit var btnText: Button
    private lateinit var btnDone: Button
    private lateinit var inputLayout: LinearLayout
    private lateinit var etInput: EditText
    private lateinit var tvDone: TextView
    private lateinit var ivBack: ImageView

    private var player: ExoPlayer? = null
    private var isVideoMedia = false
    private var editingTextView: TextView? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private var savedUri: Uri? = null
    private var mediaType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.image_video_converter)
        initViews()
        val uri = intent.getStringExtra("uri") ?: ""
        isVideoMedia = intent.getBooleanExtra("isVideo", false)
        if (isVideoMedia) {
            setupVideo(uri)
            btnDone.text = "Send"
            requestAudioPermission()
        } else {
            btnDone.text = "Send"
            setupImage(uri)
        }
        setupEditor()
        if (isVideoMedia) {
            attachGesturesToView(playerView, isText = false)
        } else {
            attachGesturesToView(photoEditorView, isText = false)
        }
        btnDone.setOnClickListener {
            if (isVideoMedia) {
                if (!isRecording) {
                    val videoUri = intent.getStringExtra("uri") ?: ""
                    val duration = getVideoDuration(videoUri)
                    startAndAutoSaveVideo(durationMs = duration)
                }
            } else {
                saveFinalImage()
            }
        }
        ivBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        mediaContainer = findViewById(R.id.mediaContainer)
        overlayContainer = findViewById(R.id.overlayContainer)
        photoEditorView = findViewById(R.id.photoEditorView)
        playerView = findViewById(R.id.playerView)
        btnText = findViewById(R.id.btnText)
        btnDone = findViewById(R.id.btnDone)
        inputLayout = findViewById(R.id.inputLayout)
        etInput = findViewById(R.id.etInput)
        tvDone = findViewById(R.id.tvDone)
        ivBack = findViewById(R.id.ivBack)
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

    private fun setupVideo(uri: String) {
        photoEditorView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.repeatMode = Player.REPEAT_MODE_ALL
        player?.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        player?.prepare()
        player?.play()
    }

    private fun setupImage(uri: String) {
        photoEditorView.visibility = View.VISIBLE
        playerView.visibility = View.GONE
        Glide.with(this).load(uri).centerInside().into(photoEditorView.source)
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
                                v.rotation = initialRotation + angleDelta
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = 0
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
                "Delete" -> {
                    overlayContainer.removeView(view)
                }
                "Edit" -> {
                    val tv = (view as? ViewGroup)?.getChildAt(0) as? TextView
                    openInputBar(tv)
                }
            }
            listPopupWindow.dismiss()
        }
        listPopupWindow.show()
    }

    // ---------------------- VIDEO RECORDING (CLEAN, WORKING VERSION) ----------------------
    private fun startAndAutoSaveVideo(durationMs: Long) {
        ProcessDialog.showDialog(this@ImageVideoConverter, true)

        // Make sure layout is measured
        if (mediaContainer.width <= 0 || mediaContainer.height <= 0) {
            ProcessDialog.dismissDialog(true)
            Toast.makeText(this, "Layout not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val width = (mediaContainer.width / 2) * 2   // ensure even
        val height = (mediaContainer.height / 2) * 2

        val videoFile = File(cacheDir, "HiHlo_${System.currentTimeMillis()}.mp4")
        videoFile.parentFile?.mkdirs()
        Log.d("ImageVideoConverter", "Output file: ${videoFile.absolutePath}")

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
            val audioGranted = checkAudioPermission()
            if (audioGranted) {
                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            }

            mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            if (audioGranted) {
                mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder?.setAudioSamplingRate(44100)
                mediaRecorder?.setAudioEncodingBitRate(128000)
            }
            // --- Video-only recording (no audio, avoids many issues) ---
            //mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            //mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder?.setVideoSize(width, height)
            mediaRecorder?.setVideoFrameRate(30)
            mediaRecorder?.setVideoEncodingBitRate(8000000)

            // For audio, uncomment these lines and handle permissions carefully:
//             if (checkAudioPermission()) {
//                 mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
//                 mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//                 mediaRecorder?.setAudioSamplingRate(44100)
//                 mediaRecorder?.setAudioEncodingBitRate(128000)
//             }

            mediaRecorder?.setOutputFile(videoFile.absolutePath)
            mediaRecorder?.prepare()
            mediaRecorder?.start()
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "MediaRecorder prepare/start failed", e)
            ProcessDialog.dismissDialog(true)
            btnDone.isEnabled = true
            btnDone.text = "Send"
            Toast.makeText(this, "Cannot start recording: ${e.message}", Toast.LENGTH_LONG).show()
            mediaRecorder?.release()
            mediaRecorder = null
            return
        }

        val recordingSurface = mediaRecorder!!.surface
        isRecording = true
        btnDone.isEnabled = false
        btnDone.text = "Converting"

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
            stopVideoRecording(videoFile)
        }, durationMs)
    }

    private fun stopVideoRecording(videoFile: File) {
        if (!isRecording) return
        isRecording = false

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "stop() failed", e)
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "release() failed", e)
        }
        mediaRecorder = null

        // Get content URI via FileProvider
        try {
            val authority = "${packageName}.provider"
            val contentUri = FileProvider.getUriForFile(this, authority, videoFile)
            savedUri = contentUri
            mediaType = "video"

            if (!videoFile.exists() || videoFile.length() == 0L) {
                throw Exception("Video file is missing or empty")
            }

            btnDone.text = "Done!"
            ProcessDialog.dismissDialog(true)
            Handler(Looper.getMainLooper()).postDelayed({ finalize_data() }, 500)
        } catch (e: Exception) {
            Log.e("ImageVideoConverter", "File provider error", e)
            ProcessDialog.dismissDialog(true)
            btnDone.isEnabled = true
            btnDone.text = "Send"
            Toast.makeText(this, "Failed to save video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------------- IMAGE SAVING (ALSO FIXED) ----------------------
    private fun saveFinalImage() {
        ProcessDialog.showDialog(this@ImageVideoConverter, true)
        btnDone.text = "Converting"

        val bitmap = Bitmap.createBitmap(mediaContainer.width, mediaContainer.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
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

    private fun createNomediaFile(folderPath: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, ".nomedia")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, folderPath)
            }
            contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        } catch (_: Exception) {}
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

    private fun checkAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        if (!checkAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}