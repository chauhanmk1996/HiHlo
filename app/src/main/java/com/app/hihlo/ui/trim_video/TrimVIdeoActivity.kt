package com.app.hihlo.ui.trim_video

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.hihlo.R
import com.app.hihlo.base.BaseActivity
import com.app.hihlo.databinding.ActivityTrimVideoBinding
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.EXTRA_CROPPED_URI
import com.app.hihlo.utils.logD
import java.io.File
import java.nio.ByteBuffer

class TrimVideoActivity : BaseActivity<ActivityTrimVideoBinding>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_trim_video
    }

    private var videoUri: Uri? = null
    private var player: ExoPlayer? = null
    private var originalVideoDurationMs = 0L
    private var trimStartMs = 0L
    private var trimEndMs = 0L
    private val progressHandler = Handler(Looper.getMainLooper())
    //private var videoConvertingPercentageDialog: VideoConvertingPercentageDialog? = null
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hidePlayPauseButton() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val videoUriString = intent.getStringExtra("videoUrl")
        videoUri = videoUriString?.toUri()

        if (videoUri == null) {
            logD("Video URI is null. Cannot proceed with trimming")
            finish()
        } else {
            setVideo()
        }
        onClick()
    }

    private fun setVideo() {
        videoUri?.let {uri->
            player = ExoPlayer.Builder(this).build()
            binding.playerView.player = player
            player?.repeatMode = Player.REPEAT_MODE_OFF
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
            player?.play()
            originalVideoDurationMs = getVideoDuration(uri)

            binding.videoTrimmerView.setVideoUri(uri, originalVideoDurationMs)
            trimStartMs = 0L
            trimEndMs = originalVideoDurationMs

            binding.videoTrimmerView.setOnScrubChangedListener { ms ->
                player?.seekTo(ms)
            }

            binding.videoTrimmerView.setOnTrimChangedListener { start, end ->
                trimStartMs = start
                trimEndMs = end
                player?.pause()
                player?.seekTo(start)
                progressHandler.removeCallbacksAndMessages(null)
                startProgressLoop()
            }

            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    runOnUiThread {
                        binding.btnPlayPause.setImageResource(
                            if (isPlaying) R.drawable.play_icon else R.drawable.pause_icon
                        )
                    }
                }
            })
            startProgressLoop()
        }
    }

    private fun startProgressLoop() {
        progressHandler.post(object : Runnable {
            override fun run() {
                val p = player ?: return
                val current = p.currentPosition
                binding.videoTrimmerView.updateProgress(current)
                if (current >= trimEndMs) {
                    p.seekTo(trimStartMs)
                    p.play()
                }
                val startTime = formatTime(player?.currentPosition ?: 0)
                val endTime = formatTime(trimEndMs)
                binding.tvTime.text = "${startTime}sec - ${endTime}sec"

                logD("Progress Time: ${startTime}sec - ${endTime}sec")
                progressHandler.postDelayed(this, 10)
            }
        })
    }

    private fun getVideoDuration(uriString: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uriString)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 5000L
        } catch (e: Exception) {
            logD(e.message ?: "")
            5000L
        } finally {
            retriever.release()
        }
    }

    private fun onClick() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.btnDone.setOnClickListener {
            trimVideo { outputUri ->
                if (outputUri != null) {
                    logD("Trim URI = $outputUri")
                    UserPreference.seletedUri = outputUri
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_CROPPED_URI, outputUri.toString())
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }

        binding.btnPlayPause.setOnClickListener {
            togglePlayback()
        }

        binding.playerView.setOnClickListener {
            togglePlayback()
        }
    }

    @SuppressLint("WrongConstant")
    fun trimVideo(onComplete: (Uri?) -> Unit) {
        Thread {
            var extractor: MediaExtractor? = null
            var muxer: MediaMuxer? = null
            try {
                val outputFile = File(cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")

                extractor = MediaExtractor()
                extractor.setDataSource(this, videoUri!!, null)

                muxer =
                    MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackMap = HashMap<Int, Int>()

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val dstIndex = muxer.addTrack(format)
                    trackMap[i] = dstIndex
                }

                muxer.start()
                val startUs = trimStartMs * 1000L
                val endUs = trimEndMs * 1000L
                val buffer = ByteBuffer.allocate(1024 * 1024)
                val info = MediaCodec.BufferInfo()

                for (track in 0 until extractor.trackCount) {
                    extractor.unselectTrack(track)
                    extractor.selectTrack(track)
                    extractor.seekTo(
                        startUs,
                        MediaExtractor.SEEK_TO_PREVIOUS_SYNC
                    )

                    while (true) {
                        val sampleTime = extractor.sampleTime
                        if (sampleTime !in 0..endUs) {
                            break
                        }
                        info.offset = 0
                        info.size = extractor.readSampleData(buffer, 0)
                        if (info.size < 0) {
                            break
                        }
                        info.presentationTimeUs = sampleTime - startUs
                        info.flags = extractor.sampleFlags
                        muxer.writeSampleData(trackMap[track]!!, buffer, info)
                        extractor.advance()
                    }
                    extractor.unselectTrack(track)
                }

                muxer.stop()
                muxer.release()
                extractor.release()

                val outputUri = Uri.fromFile(outputFile)
                Handler(Looper.getMainLooper()).post {
                    onComplete(outputUri)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    muxer?.release()
                } catch (_: Exception) {
                }
                try {
                    extractor?.release()
                } catch (_: Exception) {
                }
                Handler(Looper.getMainLooper()).post {
                    onComplete(null)
                }
            }
        }.start()
    }

    private fun togglePlayback() {
        player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
            showPlayPauseButton()
        }
    }

    private fun showPlayPauseButton() {
        autoHideHandler.removeCallbacks(autoHideRunnable)
        binding.btnPlayPause.visibility = View.VISIBLE
        autoHideHandler.postDelayed(autoHideRunnable, 5000L)
    }

    private fun hidePlayPauseButton() {
        binding.btnPlayPause.visibility = View.GONE
    }

    /*private fun showVideoConvertingPercentage(visible: Boolean) {
        if (visible) {
            videoConvertingPercentageDialog?.dismiss()
            videoConvertingPercentageDialog =
                VideoConvertingPercentageDialog(this, originalVideoDurationMs, true)
            videoConvertingPercentageDialog?.setCancelable(false)
            videoConvertingPercentageDialog?.show()
        } else {
            videoConvertingPercentageDialog?.dismiss()
        }
    }*/

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.let {
            if (it.isPlaying) it.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacksAndMessages(null)
        autoHideHandler.removeCallbacksAndMessages(null)
        player?.release()
        //showVideoConvertingPercentage(false)
    }
}