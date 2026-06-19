package com.app.hihlo.imageVideoConverter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineBackgroundSpan

class RoundedBackgroundSpan(
    private val backgroundColor: Int,
    private val cornerRadius: Float,
    private val paddingHorizontal: Float,
    private val paddingVertical: Float
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas, paint: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        // 1. Text measure karein (newline ko exclude karke)
        val lineText = text.subSequence(start, end).toString()
            .replace("\n", "")
            .replace("\u200B", "") // Ignore invisible char in measurement

        var textWidth = paint.measureText(lineText)

        // Empty line par pill dikhane ke liye minimum width
        if (textWidth <= 0f) {
            textWidth = 25f // Chota sa capsule dikhega cursor ke niche
        }

        // 2. Horizontal calculation
        val centerX = (left + right) / 2f
        var rectLeft = centerX - (textWidth / 2f) - paddingHorizontal
        var rectRight = centerX + (textWidth / 2f) + paddingHorizontal

        // Padding screen se bahar na jaye isliye clamp karein
        if (rectLeft < left) rectLeft = left.toFloat()
        if (rectRight > right) rectRight = right.toFloat()

        // 3. Vertical calculation
        val fm = paint.fontMetrics
        val ascent = if (fm.ascent != 0f) fm.ascent else -paint.textSize * 0.8f
        val descent = if (fm.descent != 0f) fm.descent else paint.textSize * 0.2f

        val rectCenterY = (top + bottom) / 2f
        val halfHeight = (Math.abs(ascent) + descent) / 2f + paddingVertical

        val rectTop = rectCenterY - halfHeight
        // SYNTAX FIX YAHAN HAI:
        val rectBottom = if (rectCenterY + halfHeight > bottom) bottom.toFloat() else rectCenterY + halfHeight

        // 4. Drawing
        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)

        val originalColor = paint.color
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.color = originalColor
    }
}