package com.app.hihlo.utils

import android.content.Context
import com.app.hihlo.base.BaseDialog
import com.app.hihlo.R
import com.app.hihlo.databinding.PopUpProgressBinding

class ProgressDialog(context: Context) :
    BaseDialog<PopUpProgressBinding>(context, R.layout.pop_up_progress) {

    override fun initViews(viewBinding: PopUpProgressBinding) {
    }
}