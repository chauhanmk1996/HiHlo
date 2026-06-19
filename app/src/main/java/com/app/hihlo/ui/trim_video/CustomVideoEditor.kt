package com.app.hihlo.ui.trim_video

import com.redevrx.video_trimmer.view.RangeSeekBarView
import com.redevrx.video_trimmer.view.Thumb
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.ui.AspectRatioFrameLayout
import com.redevrx.video_trimmer.R
import com.redevrx.video_trimmer.databinding.TrimmerViewLayoutBinding
import com.redevrx.video_trimmer.event.OnProgressVideoEvent
import com.redevrx.video_trimmer.event.OnRangeSeekBarEvent
import com.redevrx.video_trimmer.event.OnVideoEditedEvent
import com.redevrx.video_trimmer.utils.TrimVideoUtils
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Transformer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.utils.ProgressDialog
import com.app.hihlo.utils.logD
import com.redevrx.video_trimmer.utils.BackgroundExecutor
import com.redevrx.video_trimmer.utils.UiThreadExecutor
import com.redevrx.video_trimmer.view.TimelineAdapter
import kotlin.math.ceil

@UnstableApi
class CustomVideoEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var mPlayer: ExoPlayer
    private lateinit var mSrc: Uri
    private var mFinalPath: String? = null
    private var mMaxDuration: Int = -1
    private var mMinDuration: Int = -1
    private var mListeners: ArrayList<OnProgressVideoEvent> = ArrayList()
    private var mOnVideoEditedListener: OnVideoEditedEvent? = null
    private lateinit var binding: TrimmerViewLayoutBinding
    private var mDuration: Long = 0L
    private var mTimeVideo = 0L
    private var mStartPosition = 0L
    private var mEndPosition = 0L
    private var mResetSeekBar = false
    private val mMessageHandler = MessageHandler(this)
    private var originalVideoWidth: Int = 0
    private var originalVideoHeight: Int = 0
    private var videoPlayerWidth: Int = 0
    private var videoPlayerHeight: Int = 0
    private var isVideoPrepared = false
    private var videoPlayerCurrentPosition = 0L
    private var progressDialog: ProgressDialog? = null
    private lateinit var timelineAdapter: TimelineAdapter
    private val frames = ArrayList<Bitmap>()

    private var destinationPath: String
        get() {
            if (mFinalPath == null) {
                val folder = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "HiHlo"
                )

                if (!folder.exists()) {
                    folder.mkdirs()
                }
                mFinalPath = folder.absolutePath
            }
            return mFinalPath ?: ""
        }
        set(value) {
            mFinalPath = value
        }

    init {
        init(context)
    }

    private fun init(context: Context) {
        binding = TrimmerViewLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        binding.rvTimeline.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        timelineAdapter = TimelineAdapter(frames)
        binding.rvTimeline.adapter = timelineAdapter
        (binding.rvTimeline.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        setUpListeners()
        setUpMargins()
        setLayoutSurfaceListener()
    }

    fun setLayoutSurfaceListener() {
        binding.iconVideoPlay.setOnClickListener {
            onClickVideoPlayPause()
        }

        val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            })

        binding.layoutSurfaceView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        mListeners = ArrayList()
        mListeners.add(object : OnProgressVideoEvent {
            override fun updateProgress(time: Float, max: Long, scale: Long) {
                updateVideoProgress(time.toLong())
            }
        })

        //TODO
        /*binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })*/

        binding.rangeSeekBarView.addOnRangeSeekBarListener(object : OnRangeSeekBarEvent {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                //TODO binding.seekBar.visibility = GONE
                updateVideoFromScroll()
                //onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })

        binding.rvTimeline.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateVideoFromScroll()
            }
        })
    }

    private fun updateVideoFromScroll() {
        val scrollX = binding.rvTimeline.computeHorizontalScrollOffset()
        val leftThumbX = binding.rangeSeekBarView.thumbs[Thumb.LEFT].pos
        val totalWidth = binding.rvTimeline.computeHorizontalScrollRange().toFloat()
        val leftActualX = scrollX + leftThumbX
        val leftProgress = leftActualX / totalWidth
        val newTime = (leftProgress * mDuration).toLong()
        val clampedTime = newTime.coerceIn(mStartPosition, mEndPosition)
        mStartPosition = clampedTime

        val rightThumbX = binding.rangeSeekBarView.thumbs[Thumb.RIGHT].pos
        val rightActualX = scrollX + rightThumbX
        val rightProgress = rightActualX / totalWidth
        val newTime2 = (rightProgress * mDuration).toLong()
        mEndPosition = newTime2.coerceIn(mStartPosition, mDuration)

        mPlayer.seekTo(clampedTime)
        updateVideoProgress(clampedTime)
    }

    //TODO
    /*private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        val duration = (mDuration * progress / 1000L)
        if (fromUser) {
            if (duration < mStartPosition) setProgressBarPosition(mStartPosition)
            else if (duration > mEndPosition) setProgressBarPosition(mEndPosition)
        }
    }*/

    //TODO
    /*private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mPlayer.pause()
        binding.iconVideoPlay.visibility = VISIBLE
        notifyProgressUpdate(false)
    }*/

    //TODO
    /*private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mPlayer.pause()
        binding.iconVideoPlay.visibility = VISIBLE

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        mPlayer.seekTo(duration.toLong())
        notifyProgressUpdate(false)
    }*/

    private fun setProgressBarPosition(position: Long) {
        /*if (mDuration > 0) binding.seekBar.progress = (1000L * position / mDuration).toInt()*/
    }

    private fun setUpMargins() {
        /*val marge = binding.rangeSeekBarView.thumbs[0].widthBitmap
        val lp = binding.timeLineView.layoutParams as LayoutParams
        lp.setMargins(0, 0, 0, 0)
        binding.timeLineView.layoutParams = lp*/
    }

    private fun onClickVideoPlayPause() {
        if (mPlayer.isPlaying) {
            binding.iconVideoPlay.visibility = VISIBLE
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mPlayer.pause()
        } else {
            binding.iconVideoPlay.visibility = GONE
            if (mResetSeekBar) {
                mResetSeekBar = false
                mPlayer.seekTo(mStartPosition)
                //TODO binding.seekBar.visibility = VISIBLE
                setProgressBarPosition(0)
            }
            mResetSeekBar = false
            //TODO binding.seekBar.visibility = VISIBLE
            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
            mPlayer.play()
        }
    }

    private fun onVideoPrepared(mp: ExoPlayer) {
        if (isVideoPrepared) return
        isVideoPrepared = true
        val videoWidth = mp.videoSize.width
        val videoHeight = mp.videoSize.height
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = binding.layoutSurfaceView.width
        val screenHeight = binding.layoutSurfaceView.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = binding.videoLoader.layoutParams

        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        videoPlayerWidth = lp.width
        videoPlayerHeight = lp.height
        binding.videoLoader.layoutParams = lp
        binding.iconVideoPlay.visibility = VISIBLE
        mDuration = mPlayer.duration
        setSeekBarPosition()
        setTimeFrames()
    }

    private fun setSeekBarPosition() {
        when {
            mDuration >= mMaxDuration && mMaxDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMaxDuration / 2
                mEndPosition = mDuration / 2 + mMaxDuration / 2
                binding.rangeSeekBarView.setThumbValue(0, (mStartPosition * 100 / mDuration))
                binding.rangeSeekBarView.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }

            mDuration <= mMinDuration && mMinDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMinDuration / 2
                mEndPosition = mDuration / 2 + mMinDuration / 2
                binding.rangeSeekBarView.setThumbValue(0, (mStartPosition * 100 / mDuration))
                binding.rangeSeekBarView.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }

            else -> {
                mStartPosition = 0L
                mEndPosition = mDuration
            }
        }
        mPlayer.seekTo(mStartPosition)
        mTimeVideo = mDuration
        binding.rangeSeekBarView.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        val startTime = TrimVideoUtils.stringForTime(mStartPosition)
        val endTime = TrimVideoUtils.stringForTime(mEndPosition)
        val time = "$startTime $seconds - $endTime $seconds"
        binding.textTimeSelection.text = time
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = ((mDuration * value / 100L).toLong())
                mPlayer.seekTo(mStartPosition)
                setProgressBarPosition(mStartPosition)
            }

            Thumb.RIGHT -> {
                mEndPosition = ((mDuration * value / 100L).toLong())
                if (mPlayer.currentPosition > mEndPosition) {
                    mPlayer.seekTo(mEndPosition)
                    setProgressBarPosition(mEndPosition)
                }
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mPlayer.pause()
        binding.iconVideoPlay.visibility = VISIBLE
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0L) return
        val position = mPlayer.currentPosition
        if (all) {
            for (item in mListeners) {
                item.updateProgress(position.toFloat(), mDuration, (position * 100 / mDuration))
            }
        } else {
            mListeners[0].updateProgress(
                position.toFloat(),
                mDuration,
                (position * 100 / mDuration)
            )
        }
    }

    private fun updateVideoProgress(time: Long) {
        //TODO
        /*if (time <= mStartPosition && time <= mEndPosition) binding.seekBar.visibility =
            GONE
        else binding.seekBar.visibility = VISIBLE*/
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mPlayer.pause()
            binding.iconVideoPlay.visibility = VISIBLE
            mResetSeekBar = true
            return
        }
        setProgressBarPosition(time)
    }

    fun setVideoBackgroundColor(@ColorInt color: Int) = with(binding) {
        container.setBackgroundColor(color)
        layout.setBackgroundColor(color)
    }

    private var transformer: Transformer? = null

    @SuppressLint("UnsafeOptInUsageError")
    fun saveVideo() {
        val txtTime = binding.textTimeSelection.text.toString()
        val pattern = "\\d{2}:\\d{2}(?::\\d{2})?"
        val regex = Regex(pattern)

        val timeList = regex.findAll(txtTime)
            .map { it.value }
            .toList()

        if (timeList.size < 2) {
            Toast.makeText(context, "Invalid time range", Toast.LENGTH_SHORT).show()
            return
        }

        val startMilliseconds = timeToMilliseconds(timeList[0])
        val endMilliseconds = timeToMilliseconds(timeList[1])

        if (startMilliseconds >= endMilliseconds) {
            Toast.makeText(context, "Invalid trim range", Toast.LENGTH_SHORT).show()
            return
        }

        // Cancel previous export if running
        transformer?.cancel()

        // Create output file safely
        val outputFile = File(
            destinationPath,
            "${UUID.randomUUID()}.mp4"
        )

        val outputPath = outputFile.absolutePath

        transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {

                override fun onCompleted(
                    composition: Composition,
                    exportResult: ExportResult,
                ) {
                    showProgressDialog(false)
                    mOnVideoEditedListener?.getResult(outputFile.toUri())
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    logD("VideoTrim:: Export failed")
                    showProgressDialog(false)
                    mOnVideoEditedListener?.onError(
                        exportException.localizedMessage ?: "Video export failed"
                    )
                }
            })
            .build()

        val inputMediaItem = MediaItem.Builder()
            .setUri(mSrc)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMilliseconds)
                    .setEndPositionMs(endMilliseconds)
                    .build()
            )
            .build()

        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem)
            .setEffects(Effects.EMPTY)
            .build()

        val composition = Composition.Builder(
            listOf(
                EditedMediaItemSequence(
                    listOf(editedMediaItem)
                )
            )
        ).build()
        logD("VideoTrim:: Trimming from $startMilliseconds ms to $endMilliseconds ms")
        showProgressDialog(true)
        transformer?.start(composition, outputPath)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        transformer?.cancel()
    }

    private fun timeToMilliseconds(time: String): Long {
        val parts = time.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid time format: $time")
        }

        val minutes = parts[0].toInt()
        val seconds = parts[1].trim().split(" ")[0].toInt() // Extract seconds
        return ((minutes * 60 + seconds) * 1000).toLong()
    }

    fun setVideoInformationVisibility(visible: Boolean): CustomVideoEditor {
        binding.timeFrame.visibility = if (visible) VISIBLE else GONE
        return this
    }

    fun setOnTrimVideoListener(onVideoEditedListener: OnVideoEditedEvent): CustomVideoEditor {
        mOnVideoEditedListener = onVideoEditedListener
        return this
    }

    fun setMaxDuration(maxDuration: Int): CustomVideoEditor {
        mMaxDuration = maxDuration
        return this
    }

    fun setMinDuration(minDuration: Int): CustomVideoEditor {
        mMinDuration = minDuration
        return this
    }

    fun setDestinationPath(path: String): CustomVideoEditor {
        destinationPath = path
        return this
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun setVideoURI(videoURI: Uri): CustomVideoEditor {
        mSrc = videoURI
        mPlayer = ExoPlayer.Builder(context).build()

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoURI))

        mPlayer.setMediaSource(videoSource)
        mPlayer.prepare()
        mPlayer.playWhenReady = false

        binding.videoLoader.also {
            it.player = mPlayer
            it.useController = false
        }

        mPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                mOnVideoEditedListener?.onError("Something went wrong reason : ${error.localizedMessage}")
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                if (mPlayer.videoSize.width > mPlayer.videoSize.height) {
                    binding.videoLoader.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                } else {
                    binding.videoLoader.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    mPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
                onVideoPrepared(mPlayer)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    mResetSeekBar = true
                    mPlayer.seekTo(mStartPosition)
                    //TODO  binding.seekBar.visibility = VISIBLE
                    setProgressBarPosition(0)
                }
            }
        })

        binding.videoLoader.requestFocus()
        //binding.timeLineView.setVideo(mSrc)
        binding.rvTimeline.post {
            getBitmap(
                binding.rvTimeline.width,
                videoURI
            )
        }

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        val metaDateWidth =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toInt() ?: 0
        val metaDataHeight =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toInt() ?: 0

        //If the rotation is 90 or 270 the width and height will be transposed.
        when (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toInt()) {
            90, 270 -> {
                originalVideoWidth = metaDataHeight
                originalVideoHeight = metaDateWidth
            }

            else -> {
                originalVideoWidth = metaDateWidth
                originalVideoHeight = metaDataHeight
            }
        }

        return this
    }

    private fun getBitmap(viewWidth: Int, videoURI: Uri) {
        BackgroundExecutor.execute(object : BackgroundExecutor.Task("", 0L, "") {
            override fun execute() {
                try {
                    val frameList = ArrayList<Bitmap>()
                    val threshold = 20
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, videoURI)

                    val duration =
                        mediaMetadataRetriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0L

                    val videoLengthInUs = duration * 1000

                    val frameHeight =
                        context.resources.getDimensionPixelOffset(R.dimen.frames_video_height)

                    val initialBitmap = mediaMetadataRetriever.getFrameAtTime(
                        0,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: return

                    val frameWidth =
                        ((initialBitmap.width.toFloat() /
                                initialBitmap.height.toFloat()) * frameHeight).toInt()

                    var numThumbs = ceil(viewWidth.toFloat() / frameWidth.toFloat()).toInt()

                    if (numThumbs < threshold) {
                        numThumbs = threshold
                    }

                    val cropWidth = viewWidth / threshold
                    val interval = videoLengthInUs / numThumbs

                    for (i in 0 until numThumbs) {
                        var bitmap = mediaMetadataRetriever.getFrameAtTime(
                            i * interval,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )

                        if (bitmap != null) {
                            bitmap = Bitmap.createScaledBitmap(
                                bitmap,
                                frameWidth,
                                frameHeight,
                                false
                            )

                            bitmap = Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                cropWidth,
                                bitmap.height
                            )
                            frameList.add(bitmap)
                        }
                    }
                    mediaMetadataRetriever.release()
                    updateFrames(frameList)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun updateFrames(frameList: ArrayList<Bitmap>) {
        UiThreadExecutor.runTask("", {
            frames.clear()
            frames.addAll(frameList)
            timelineAdapter.notifyDataSetChanged()
        }, 0L)
    }

    fun onResume() {
        mPlayer.seekTo(videoPlayerCurrentPosition)
        if (binding.videoLoader.player?.isPlaying == true) {
            //TODO binding.seekBar.visibility = VISIBLE
        }
    }

    fun onPause() {
        videoPlayerCurrentPosition = mPlayer.currentPosition
    }

    private class MessageHandler(view: CustomVideoEditor) : Handler(Looper.getMainLooper()) {
        private val mView: WeakReference<CustomVideoEditor> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get() ?: return
            view.notifyProgressUpdate(true)
            if (view.binding.videoLoader.player?.isPlaying == true) sendEmptyMessageDelayed(0, 10)
        }
    }

    private fun showProgressDialog(visible: Boolean) {
        if (visible) {
            progressDialog?.dismiss()
            progressDialog = ProgressDialog(context)
            progressDialog?.setCancelable(false)
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
        }
    }

    companion object {
        private const val SHOW_PROGRESS = 2
    }
}