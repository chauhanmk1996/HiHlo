package com.app.hihlo.utils

import android.content.Context
import com.app.hihlo.base.BaseDialog
import com.app.hihlo.R
import com.app.hihlo.databinding.PopUpProgressPercentageBinding

class ProgressPercentageDialog(context: Context) :
    BaseDialog<PopUpProgressPercentageBinding>(context, R.layout.pop_up_progress_percentage) {

    override fun initViews(viewBinding: PopUpProgressPercentageBinding) {

    }

    fun uploadPercentageChange(percentage: Int) {
        val percentageText = "Uploading : $percentage%"
        binding.tvUploadingPercentage.text = percentageText
    }
}