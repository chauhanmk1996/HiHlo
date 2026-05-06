package com.app.hihlo.ui.HomeNew.utility

import android.view.View
import android.view.ViewOutlineProvider

class CircleOutlineProvider(
    private var cx: Int,
    private var cy: Int,
    private var radius: Float
) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: android.graphics.Outline) {
        outline.setOval(
            (cx - radius).toInt(),
            (cy - radius).toInt(),
            (cx + radius).toInt(),
            (cy + radius).toInt()
        )
    }

    fun updateRadius(r: Float) {
        radius = r
    }
}