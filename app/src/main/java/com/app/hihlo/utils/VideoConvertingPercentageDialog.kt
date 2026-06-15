package com.app.hihlo.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.app.hihlo.base.BaseDialog
import com.app.hihlo.R
import com.app.hihlo.databinding.PopUpVideoConvertingPercentageBinding

class VideoConvertingPercentageDialog(context: Context, videoDuration: Long) :
    BaseDialog<PopUpVideoConvertingPercentageBinding>(
        context,
        R.layout.pop_up_video_converting_percentage
    ) {

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var lastUpdatedPercentage: Int = 0
    private val progressRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            val percentage = ((elapsed * 100) / videoDuration).toInt().coerceAtMost(100)

            logD("ProgressDialog :: Last Updated Percentage = $lastUpdatedPercentage")
            logD("ProgressDialog :: Percentage = $percentage")

            if (percentage != 0 && percentage > lastUpdatedPercentage) {
                lastUpdatedPercentage = percentage
                val convertingText = "Converting : $percentage%"
                binding.tvConvertingPercentage.text =convertingText
                logD("ProgressDialog :: ConvertingText = $convertingText")
            }

            if (percentage < 100) {
                handler.postDelayed(this, 200)
            }
        }
    }

    override fun initViews(viewBinding: PopUpVideoConvertingPercentageBinding) {
        startTime = System.currentTimeMillis()
        handler.post(progressRunnable)
    }

    override fun dismiss() {
        handler.removeCallbacks(progressRunnable)
        super.dismiss()
    }
}