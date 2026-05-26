package com.app.hihlo.ui.trim_video

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.exoplayer.ExoPlayer
import com.app.hihlo.R
import com.app.hihlo.base.BaseActivity
import com.app.hihlo.databinding.ActivityTrimVideoBinding
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.EXTRA_CROPPED_URI
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.redevrx.video_trimmer.event.OnVideoEditedEvent

class TrimVideoActivity : BaseActivity<ActivityTrimVideoBinding>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_trim_video
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val videoUriString = intent.getStringExtra("videoUrl")
        val from = intent.getStringExtra("from") ?: ""
        val videoUri: Uri? = videoUriString?.toUri()

        if (videoUri == null) {
            Log.e("VideoCroppingActivity", "Video URI is null. Cannot proceed with trimming.")
            finish()
            return
        }

        // Check resolution before loading into trimmer
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(this, videoUri)
            val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            retriever.release()

            Log.d("VideoCroppingActivity", "Video resolution: ${width}x$height")

            if (width > 3840 || height > 2160) {
                Toast.makeText(
                    this,
                    "Videos above 4K resolution are not supported.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        binding.videoTrimmer.apply {
            setVideoBackgroundColor(resources.getColor(R.color.black))
            setVideoURI(videoUri)
            setOnTrimVideoListener(object : OnVideoEditedEvent {
                override fun getResult(uri: Uri) {
                    ProcessDialog.dismissDialog(true)
                    Log.e("VideoCroppingActivity", "Trim result received")

                    UserPreference.seletedUri = uri
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_CROPPED_URI, uri.toString())   // ✅ FIXED: sends the real trimmed URI
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }

                override fun onError(message: String) {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(this@TrimVideoActivity, "Some error occurred! Please try again.", Toast.LENGTH_SHORT).show()
                    Log.e("VideoCroppingActivity", "Error during trimming: $message")
                }

                override fun onProgress(percentage: Int) {
                    Log.d("VideoCroppingActivity", "Trimming progress: $percentage%")
                }
            })
            setDestinationPath(path.absolutePath)
            setVideoInformationVisibility(true)
            setMaxDuration(if (from == "home") 15 else 60)
            setMinDuration(0)
        }

        binding.backButton.setOnClickListener { onBackPressed() }
        binding.btnDone.setOnClickListener {
            ProcessDialog.showDialog(this, true)
            binding.videoTrimmer.saveVideo()
        }
    }

    override fun onResume() {
        super.onResume()
        this?.window?.let { window ->
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black_1c1c1c)
            WindowInsetsControllerCompat(window, window.decorView)
                .isAppearanceLightNavigationBars = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseTrimmerPlayer()
    }

    override fun onStop() {
        super.onStop()
        releaseTrimmerPlayer()
    }

    private fun releaseTrimmerPlayer() {
        try {

            val field = binding.videoTrimmer.javaClass.getDeclaredField("mPlayer")
            field.isAccessible = true

            val exoPlayer = field.get(binding.videoTrimmer) as? ExoPlayer

            exoPlayer?.apply {
                pause()
                stop()
                clearMediaItems()
                release()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}