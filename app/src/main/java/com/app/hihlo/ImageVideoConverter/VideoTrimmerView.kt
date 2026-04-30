package com.app.hihlo.ImageVideoConverter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class VideoTrimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val handleTouchArea = 30f.dp
    private val minDurationMs = 1000L

    private var videoDurationMs = 0L
    private var startMs = 0L
    private var endMs = 0L
    private var startX = 0f
    private var endX = 0f

    private var thumbnails: List<Bitmap> = emptyList()
    private var frameCount = 10

    private var onTrimChangedListener: ((startMs: Long, endMs: Long) -> Unit)? = null
    private var draggingLeftHandle = false
    private var draggingRightHandle = false

    // Paints
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80000000")
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3f.dp
        style = Paint.Style.STROKE
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    fun setVideoUri(uri: Uri, durationMs: Long) {
        videoDurationMs = durationMs
        // Default to full range unless later set by setTrimRange
        startMs = 0L
        endMs = durationMs
        updateHandlePositions()
        generateThumbnails(uri)
    }

    /** Allows the external Activity to restore a previously selected range */
    fun setTrimRange(startMs: Long, endMs: Long) {
        if (videoDurationMs <= 0) return
        this.startMs = startMs.coerceIn(0, videoDurationMs)
        this.endMs = endMs.coerceIn(this.startMs + minDurationMs, videoDurationMs)
        updateHandlePositions()
    }

    fun setOnTrimChangedListener(listener: (startMs: Long, endMs: Long) -> Unit) {
        onTrimChangedListener = listener
    }

    fun getTrimStartMs() = startMs
    fun getTrimEndMs() = endMs

    private fun updateHandlePositions() {
        if (videoDurationMs == 0L) return
        startX = (startMs.toFloat() / videoDurationMs) * width
        endX = (endMs.toFloat() / videoDurationMs) * width
        invalidate()
    }

    @SuppressLint("NewApi")
    private fun generateThumbnails(uri: Uri) {
        if (videoDurationMs <= 0) return
        Thread {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val bitmaps = mutableListOf<Bitmap>()
                val interval = if (frameCount > 1) videoDurationMs / (frameCount - 1) else videoDurationMs
                for (i in 0 until frameCount) {
                    val timeUs = (i * interval) * 1000L
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    if (frame != null) {
                        val scaled = Bitmap.createScaledBitmap(frame, 200, height, true)
                        if (scaled != frame) frame.recycle()
                        bitmaps.add(scaled)
                    } else {
                        bitmaps.add(createPlaceholder())
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    thumbnails = bitmaps
                    invalidate()
                }
            } catch (e: Exception) {
                Log.e("VideoTrimmerView", "Cannot extract frames", e)
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }.start()
    }

    private fun createPlaceholder(): Bitmap {
        val bmp = Bitmap.createBitmap(200, height, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.DKGRAY)
        return bmp
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateHandlePositions()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (thumbnails.isEmpty() || videoDurationMs == 0L) return

        val segmentWidth = width.toFloat() / frameCount
        for (i in thumbnails.indices) {
            val left = i * segmentWidth
            canvas.drawBitmap(thumbnails[i], null,
                RectF(left, 0f, left + segmentWidth, height.toFloat()), framePaint)
        }

        if (startX > 0) canvas.drawRect(0f, 0f, startX, height.toFloat(), dimPaint)
        if (endX < width) canvas.drawRect(endX, 0f, width.toFloat(), height.toFloat(), dimPaint)

        drawHandle(canvas, startX)
        drawHandle(canvas, endX)
    }

    private fun drawHandle(canvas: Canvas, x: Float) {
        canvas.drawLine(x, 0f, x, height.toFloat(), handlePaint)
        val r = 6f.dp
        canvas.drawCircle(x, r, r, handleFillPaint)
        canvas.drawCircle(x, height - r, r, handleFillPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (videoDurationMs == 0L) return false
        val x = event.x

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (x >= startX - handleTouchArea && x <= startX + handleTouchArea) {
                    draggingLeftHandle = true
                    return true
                }
                if (x >= endX - handleTouchArea && x <= endX + handleTouchArea) {
                    draggingRightHandle = true
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingLeftHandle) {
                    startX = x.coerceIn(0f, endX - minHandlePixelDistance())
                    startMs = (startX / width * videoDurationMs).toLong()
                    notifyTrimChanged()
                    invalidate()
                    return true
                }
                if (draggingRightHandle) {
                    endX = x.coerceIn(startX + minHandlePixelDistance(), width.toFloat())
                    endMs = (endX / width * videoDurationMs).toLong()
                    notifyTrimChanged()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingLeftHandle = false
                draggingRightHandle = false
                return true
            }
        }
        return false
    }

    private fun minHandlePixelDistance(): Float =
        (minDurationMs.toFloat() / videoDurationMs) * width

    private fun notifyTrimChanged() {
        onTrimChangedListener?.invoke(startMs, endMs)
    }

    private val Float.dp: Float get() = this * context.resources.displayMetrics.density
}