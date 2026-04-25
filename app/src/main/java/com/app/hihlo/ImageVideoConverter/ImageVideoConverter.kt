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
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.app.hihlo.R
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.bumptech.glide.Glide
import ja.burhanrashid52.photoeditor.PhotoEditorView
import kotlin.math.atan2

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
        val longPressDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isText) showDeleteMenu(view)
            }
        })
        val sgd = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                view.scaleX *= d.scaleFactor
                view.scaleY *= d.scaleFactor
                return true
            }
        })
        var lastX = 0f
        var lastY = 0f
        var prevAngle = 0f
        view.setOnTouchListener { v, event ->
            longPressDetector.onTouchEvent(event)
            sgd.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    if (isText) v.bringToFront()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) prevAngle = getAngle(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        val dx = event.rawX - lastX
                        val dy = event.rawY - lastY
                        v.translationX += dx
                        v.translationY += dy
                        lastX = event.rawX
                        lastY = event.rawY
                    } else if (event.pointerCount == 2) {
                        val currentAngle = getAngle(event)
                        v.rotation += (currentAngle - prevAngle)
                        prevAngle = currentAngle
                    }
                }
            }
            true
        }
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
                    //Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
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

    private fun getAngle(e: MotionEvent): Float {
        val dx = (e.getX(1) - e.getX(0)).toDouble()
        val dy = (e.getY(1) - e.getY(0)).toDouble()
        return Math.toDegrees(atan2(dy, dx)).toFloat()
    }

    private fun startAndAutoSaveVideo(durationMs: Long) {
        ProcessDialog.showDialog(this@ImageVideoConverter, true)
        if (mediaContainer.width <= 0 || mediaContainer.height <= 0) return
        try {
            val width = if (mediaContainer.width % 2 != 0) mediaContainer.width - 1 else mediaContainer.width
            val height = if (mediaContainer.height % 2 != 0) mediaContainer.height - 1 else mediaContainer.height
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "HiHlo_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/HiHlo")
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            savedUri = uri
            mediaType = "video"
            val pfd = contentResolver.openFileDescriptor(uri!!, "rw")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
            if (checkAudioPermission()) mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            if (checkAudioPermission()) {
                mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder?.setAudioSamplingRate(44100)
                mediaRecorder?.setAudioEncodingBitRate(128000)
            }
            mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder?.setVideoSize(width, height)
            mediaRecorder?.setVideoEncodingBitRate(15000000)
            mediaRecorder?.setVideoFrameRate(30)
            mediaRecorder?.setOutputFile(pfd?.fileDescriptor)
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            val recordingSurface = mediaRecorder!!.surface
            isRecording = true
            btnDone.isEnabled = false
            btnDone.text = "Converting"
            val choreographer = Choreographer.getInstance()
            val frameCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isRecording) return
                    try {
                        val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            recordingSurface.lockHardwareCanvas() else recordingSurface.lockCanvas(null)
                        canvas.drawColor(Color.BLACK)
                        mediaContainer.draw(canvas)
                        overlayContainer.draw(canvas)
                        recordingSurface.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) { e.printStackTrace() }
                    choreographer.postFrameCallback(this)
                }
            }
            choreographer.postFrameCallback(frameCallback)
            Handler(Looper.getMainLooper()).postDelayed({ stopVideoRecording() }, durationMs)
        } catch (e: Exception) {
            btnDone.isEnabled = true
        }
    }

    private fun stopVideoRecording() {
        isRecording = false
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                btnDone.text = "Done!"
                Handler(Looper.getMainLooper()).postDelayed({ finalize_data() }, 1000)
            } catch (e: Exception) { e.printStackTrace() }
        }, 500)
    }

    private fun saveFinalImage() {
        ProcessDialog.showDialog(this@ImageVideoConverter, true)
        btnDone.text = "Converting"
        val bitmap = Bitmap.createBitmap(mediaContainer.width, mediaContainer.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        mediaContainer.draw(canvas)
        overlayContainer.draw(canvas)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "HiHlo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HiHlo")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        savedUri = uri
        mediaType = "image"

        uri?.let {
            contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            btnDone.text = "Done!"
            finalize_data()
        }
    }

    fun finalize_data() {
        val intent = android.content.Intent().apply {
            putExtra("uri", savedUri?.toString() ?: "")
            putExtra("type", mediaType)
        }
        setResult(RESULT_OK, intent)
        ProcessDialog.dismissDialog(true)
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

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
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

    private fun checkAudioPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestAudioPermission() {
        if (!checkAudioPermission()) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        mediaRecorder?.release()
    }
}