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
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
class VideoTrimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // =========================================================
    // INSTAGRAM STYLE VIDEO TRIMMER
    // Full Ready To Use
    // =========================================================

    private val density = context.resources.displayMetrics.density

    private val handleWidth = 14f * density
    private val cornerRadius = 10f * density
    private val borderStroke = 3f * density
    private val thumbTouchExtra = 24f * density
    private val progressWidth = 2f * density

    private val minDurationMs = 1000L
    private val frameCount = 12

    private var videoDurationMs = 0L
    private var startMs = 0L
    private var endMs = 0L

    private var leftX = 0f
    private var rightX = 0f
    private var progressX = 0f

    private var draggingLeft = false
    private var draggingRight = false
    private var draggingProgress = false

    private var thumbnails: MutableList<Bitmap> = mutableListOf()

    private var trimListener: ((Long, Long) -> Unit)? = null
    private var scrubListener: ((Long) -> Unit)? = null

    // =========================================================
    // PAINTS
    // =========================================================

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = borderStroke
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        strokeWidth = progressWidth
    }

    private val progressCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        style = Paint.Style.FILL
    }

    // =========================================================

    fun setVideoUri(uri: Uri, durationMs: Long) {
        videoDurationMs = durationMs
        startMs = 0L
        endMs = durationMs
        progressX = 0f

        post {
            leftX = 0f
            rightX = width.toFloat()
            progressX = leftX
            invalidate()
            generateFrames(uri)
        }
    }

    fun setTrimRange(startMs: Long, endMs: Long) {
        if (videoDurationMs <= 0) return

        this.startMs = startMs
        this.endMs = endMs

        leftX = (startMs.toFloat() / videoDurationMs) * width
        rightX = (endMs.toFloat() / videoDurationMs) * width

        progressX = leftX
        invalidate()
    }

    fun setOnTrimChangedListener(listener: (Long, Long) -> Unit) {
        trimListener = listener
    }

    fun setOnScrubChangedListener(listener: (Long) -> Unit) {
        scrubListener = listener
    }

    fun getTrimStartMs(): Long = startMs
    fun getTrimEndMs(): Long = endMs

    // =========================================================
    // DRAW
    // =========================================================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        drawFrames(canvas)
        drawShades(canvas)
        drawBorder(canvas)
        drawHandles(canvas)
        drawProgress(canvas)
    }

    private fun drawFrames(canvas: Canvas) {
        if (thumbnails.isEmpty()) {
            canvas.drawColor(Color.DKGRAY)
            return
        }

        val frameWidth = width.toFloat() / thumbnails.size

        thumbnails.forEachIndexed { index, bmp ->
            val left = index * frameWidth

            canvas.drawBitmap(
                bmp,
                null,
                RectF(left, 0f, left + frameWidth, height.toFloat()),
                bitmapPaint
            )
        }
    }

    private fun drawShades(canvas: Canvas) {
        canvas.drawRect(0f, 0f, leftX, height.toFloat(), shadePaint)
        canvas.drawRect(rightX, 0f, width.toFloat(), height.toFloat(), shadePaint)
    }

    private fun drawBorder(canvas: Canvas) {
        val rect = RectF(leftX, 0f, rightX, height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    private fun drawHandles(canvas: Canvas) {

        // left
        val leftRect = RectF(
            leftX,
            0f,
            leftX + handleWidth,
            height.toFloat()
        )

        canvas.drawRoundRect(
            leftRect,
            cornerRadius,
            cornerRadius,
            handlePaint
        )

        // right
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

        // left grip
        val cx1 = leftX + handleWidth / 2
        canvas.drawLine(cx1, height * .35f, cx1, height * .65f, borderPaint)

        // right grip
        val cx2 = rightX - handleWidth / 2
        canvas.drawLine(cx2, height * .35f, cx2, height * .65f, borderPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        val x = progressX.coerceIn(leftX, rightX)

        canvas.drawLine(
            x,
            0f,
            x,
            height.toFloat(),
            progressPaint
        )

        canvas.drawCircle(
            x,
            height / 2f,
            5f * density,
            progressCirclePaint
        )
    }

    // =========================================================
    // TOUCH
    // =========================================================

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        val x = event.x

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                draggingLeft = isNearLeft(x)
                draggingRight = isNearRight(x)

                if (!draggingLeft && !draggingRight) {
                    if (x in leftX..rightX) {
                        draggingProgress = true
                    }
                }

                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {

                when {
                    draggingLeft -> moveLeft(x)
                    draggingRight -> moveRight(x)
                    draggingProgress -> moveProgress(x)
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                draggingLeft = false
                draggingRight = false
                draggingProgress = false
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun moveLeft(x: Float) {

        val minGap = minGapPx()

        leftX = x.coerceIn(
            0f,
            rightX - minGap
        )

        startMs = ((leftX / width) * videoDurationMs).toLong()

        if (progressX < leftX) progressX = leftX

        trimListener?.invoke(startMs, endMs)
    }

    private fun moveRight(x: Float) {

        val minGap = minGapPx()

        rightX = x.coerceIn(
            leftX + minGap,
            width.toFloat()
        )

        endMs = ((rightX / width) * videoDurationMs).toLong()

        if (progressX > rightX) progressX = rightX

        trimListener?.invoke(startMs, endMs)
    }

    private fun moveProgress(x: Float) {

        progressX = x.coerceIn(leftX, rightX)

        val ms = ((progressX / width) * videoDurationMs).toLong()

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

    // =========================================================
    // THUMBNAILS
    // =========================================================

    @SuppressLint("UseKtx")
    private fun generateFrames(uri: Uri) {

        Thread {

            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(context, uri)

                val list = mutableListOf<Bitmap>()

                val each = videoDurationMs / frameCount

                for (i in 0 until frameCount) {

                    val timeUs = i * each * 1000L

                    val bmp = retriever.getFrameAtTime(
                        timeUs,
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
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

        }.start()
    }

    // =========================================================

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        thumbnails.forEach {
            if (!it.isRecycled) it.recycle()
        }

        thumbnails.clear()
    }
}