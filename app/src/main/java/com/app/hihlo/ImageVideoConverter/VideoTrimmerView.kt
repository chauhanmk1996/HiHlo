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
import androidx.core.graphics.toColorInt
import kotlin.math.max
import androidx.core.content.res.ResourcesCompat
import com.app.hihlo.R

@SuppressLint("ViewConstructor")
class VideoTrimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val handleWidth = 16f * density
    private val cornerRadius = 10f * density
    private val borderStroke = 4f * density
    private val thumbTouchExtra = 28f * density
    private val progressWidth = 3f * density
    private val progressCircleRadius = 6f * density
    private val textPadding = 12f * density
    private val meterHeight = 34f * density
    private val minDurationMs = 1000L
    private val maxTrimDurationMs = 60000L
    private val frameCount = 15
    private var videoDurationMs = 0L
    private var startMs = 0L
    private var endMs = 0L
    private var leftX = 0f
    private var rightX = 0f
    private var progressX = 0f
    private var draggingLeft = false
    private var draggingRight = false
    private var draggingProgress = false
    private var showMeter = false
    private var meterText = "00:00"
    private val thumbnails = mutableListOf<Bitmap>()
    private var trimListener: ((Long, Long) -> Unit)? = null
    private var scrubListener: ((Long) -> Unit)? = null
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#77000000".toColorInt()
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = borderStroke
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = progressWidth
    }

    private val progressCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val meterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFFFFF".toColorInt()
        style = Paint.Style.FILL
    }

    private val meterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#000000".toColorInt()
        textSize = 14f * density
        typeface = ResourcesCompat.getFont(context, R.font.manrope_semibold)
    }

    fun setVideoUri(uri: Uri, durationMs: Long) {
        videoDurationMs = durationMs
        startMs = 0L
        endMs = minOf(durationMs, maxTrimDurationMs)
        meterText = "00:00"
        post {
            leftX = 0f
            rightX = (endMs.toFloat() / videoDurationMs) * width
            progressX = leftX
            invalidate()
            trimListener?.invoke(startMs, endMs)
            generateFrames(uri)
        }
    }

    fun setTrimRange(start: Long, end: Long) {
        if (videoDurationMs <= 0 || width == 0) return
        startMs = start
        endMs = end
        leftX = (startMs.toFloat() / videoDurationMs) * width
        rightX = (endMs.toFloat() / videoDurationMs) * width
        progressX = leftX
        invalidate()
    }

    fun updateProgress(currentMs: Long) {
        if (videoDurationMs <= 0 || width == 0) return
        val safe = currentMs.coerceIn(startMs, endMs)
        progressX = (safe.toFloat() / videoDurationMs) * width
        invalidate()
    }

    fun getTrimStartMs(): Long = startMs
    fun getTrimEndMs(): Long = endMs

    fun setOnTrimChangedListener(listener: (Long, Long) -> Unit) {
        trimListener = listener
    }

    fun setOnScrubChangedListener(listener: (Long) -> Unit) {
        scrubListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawFrames(canvas)
        drawShade(canvas)
        drawBorder(canvas)
        drawHandles(canvas)
        drawProgress(canvas)
        if (showMeter) {
            drawDurationMeter(canvas)
        }
    }

    private fun drawFrames(canvas: Canvas) {
        if (thumbnails.isEmpty()) {
            canvas.drawColor(Color.DKGRAY)
            return
        }
        val previewHeight = height * 0.87f
        val top = (height - previewHeight) / 2f
        val bottom = top + previewHeight
        val eachWidth = width.toFloat() / thumbnails.size
        val path = Path()
        val rect = RectF(0f, top, width.toFloat(), bottom)
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        thumbnails.forEachIndexed { index, bmp ->
            val left = index * eachWidth
            canvas.drawBitmap(
                bmp,
                null,
                RectF(left, top, left + eachWidth, bottom),
                bitmapPaint
            )
        }
        canvas.restore()
    }

    private fun drawShade(canvas: Canvas) {
        val previewHeight = height * 0.87f
        val top = (height - previewHeight) / 2f
        val bottom = top + previewHeight
        val path = Path()
        val rect = RectF(0f, top, width.toFloat(), bottom)
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawRect(0f, top, leftX, bottom, shadePaint)
        canvas.drawRect(rightX, top, width.toFloat(), bottom, shadePaint)
        canvas.restore()
    }

    private fun drawBorder(canvas: Canvas) {
        val rect = RectF(leftX, 0f, rightX, height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f * density
            strokeCap = Paint.Cap.ROUND
        }
        val sideGap = 12f * density
        canvas.drawLine(
            leftX + cornerRadius + sideGap,
            borderStroke / 2f,
            rightX - cornerRadius - sideGap,
            borderStroke / 2f,
            linePaint
        )
        canvas.drawLine(
            leftX + cornerRadius + sideGap,
            height - borderStroke / 2f,
            rightX - cornerRadius - sideGap,
            height - borderStroke / 2f,
            linePaint
        )
    }

    private fun drawHandles(canvas: Canvas) {
        val leftRect = RectF(leftX, 0f, leftX + handleWidth, height.toFloat())
        canvas.drawRoundRect(
            leftRect,
            cornerRadius,
            cornerRadius,
            handlePaint
        )
        val rightRect = RectF(
            rightX - handleWidth,
            0f,
            rightX,
            height.toFloat()
        )
        canvas.drawRoundRect(
            rightRect,
            cornerRadius,
            cornerRadius,
            handlePaint
        )
        val gripTop = height * 0.28f
        val gripBottom = height * 0.72f
        val leftCenterX = leftX + handleWidth / 2f
        val rightCenterX = rightX - handleWidth / 2f
        val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#666666".toColorInt()
            strokeWidth = 3f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(leftCenterX, gripTop, leftCenterX, gripBottom, gripPaint)
        canvas.drawLine(rightCenterX, gripTop, rightCenterX, gripBottom, gripPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        val x = progressX.coerceIn(leftX, rightX)
        canvas.drawLine(x, 0f, x, height.toFloat(), progressPaint)
        canvas.drawCircle(
            x,
            height / 2f,
            progressCircleRadius,
            progressCirclePaint
        )
    }

    private fun drawDurationMeter(canvas: Canvas) {
        val text = meterText
        val textWidth = meterTextPaint.measureText(text)
        val boxW = textWidth + textPadding * 2f
        val centerX = if (draggingProgress) progressX else (leftX + rightX) / 2f
        val left = (centerX - boxW / 2f).coerceIn(0f, width - boxW)
        val rect = RectF(
            left,
            4f,
            left + boxW,
            4f + meterHeight
        )
        canvas.drawRoundRect(rect, 16f * density, 16f * density, meterBgPaint)
        val textY = rect.centerY() - (meterTextPaint.descent() + meterTextPaint.ascent()) / 2f
        canvas.drawText(text, rect.left + textPadding, textY, meterTextPaint)
    }

    @SuppressLint("DefaultLocale")
    private fun formatMs(ms: Long): String {
        val sec = ms / 1000
        val min = sec / 60
        val rem = sec % 60
        return String.format("%02d:%02d", min, rem)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggingLeft = isNearLeft(x)
                draggingRight = isNearRight(x)
                if (!draggingLeft && !draggingRight && x in leftX..rightX) {
                    draggingProgress = true
                }
                showMeter = draggingLeft || draggingRight || draggingProgress
                if (draggingProgress) {
                    val ms = ((progressX / width) * videoDurationMs).toLong()
                    meterText = formatMs(ms)
                } else {
                    meterText = formatMs(endMs - startMs)
                }
                invalidate()
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                when {
                    draggingLeft -> moveLeft(x)
                    draggingRight -> moveRight(x)
                    draggingProgress -> moveProgress(x)
                }
                showMeter = draggingLeft || draggingRight || draggingProgress
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                draggingLeft = false
                draggingRight = false
                draggingProgress = false
                showMeter = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun moveLeft(x: Float) {
        val minGap = minGapPx()
        leftX = x.coerceIn(0f, rightX - minGap)
        startMs = ((leftX / width) * videoDurationMs).toLong()
        val maxAllowed = allowedTrimDuration()
        if (endMs - startMs > maxAllowed) {
            endMs = startMs + maxAllowed
            rightX = (endMs.toFloat() / videoDurationMs) * width
        }
        if (progressX < leftX) progressX = leftX
        meterText = formatMs(endMs - startMs)
        trimListener?.invoke(startMs, endMs)
    }

    private fun moveRight(x: Float) {
        val minGap = minGapPx()
        rightX = x.coerceIn(leftX + minGap, width.toFloat())
        endMs = ((rightX / width) * videoDurationMs).toLong()
        val maxAllowed = allowedTrimDuration()
        if (endMs - startMs > maxAllowed) {
            startMs = endMs - maxAllowed
            leftX = (startMs.toFloat() / videoDurationMs) * width
        }
        if (progressX > rightX) progressX = rightX
        meterText = formatMs(endMs - startMs)
        trimListener?.invoke(startMs, endMs)
    }

    private fun moveProgress(x: Float) {
        progressX = x.coerceIn(leftX, rightX)
        val ms = ((progressX / width) * videoDurationMs).toLong()
        meterText = formatMs(ms)
        scrubListener?.invoke(ms)
    }

    private fun isNearLeft(x: Float): Boolean {
        return x >= leftX - thumbTouchExtra &&
                x <= leftX + handleWidth + thumbTouchExtra
    }

    private fun isNearRight(x: Float): Boolean {
        return x >= rightX - handleWidth - thumbTouchExtra &&
                x <= rightX + thumbTouchExtra
    }

    private fun minGapPx(): Float {
        return (minDurationMs.toFloat() / videoDurationMs) * width
    }

    @SuppressLint("UseKtx")
    private fun generateFrames(uri: Uri) {
        Thread {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val list = mutableListOf<Bitmap>()
                val each = videoDurationMs / frameCount
                for (i in 0 until frameCount) {
                    val bmp = retriever.getFrameAtTime(
                        i * each * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (bmp != null) {
                        list.add(
                            Bitmap.createScaledBitmap(
                                bmp,
                                200,
                                max(1, height),
                                true
                            )
                        )
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    thumbnails.clear()
                    thumbnails.addAll(list)
                    invalidate()
                }
            } catch (e: Exception) {
                Log.e("VideoTrimmerView", "frame error", e)
            } finally {
                retriever.release()
            }
        }.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        thumbnails.forEach {
            if (!it.isRecycled) it.recycle()
        }
        thumbnails.clear()
    }

    private fun allowedTrimDuration(): Long {
        return minOf(videoDurationMs, maxTrimDurationMs)
    }
}