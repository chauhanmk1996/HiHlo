package com.app.hihlo.ImageVideoConverter

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.app.hihlo.R
import com.app.hihlo.base.BaseDialog
import com.app.hihlo.databinding.PopUpFullScreenCaptionBinding
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.getString
import com.bumptech.glide.Glide

class FullScreenCaptionDialog(
    context: Context,
    private var selectedUri:Uri?,
    private var caption:String,
    private val uploadClick: (String) -> Unit,
) : BaseDialog<PopUpFullScreenCaptionBinding>(context, R.layout.pop_up_full_screen_caption) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun initViews(viewBinding: PopUpFullScreenCaptionBinding) {
        selectedUri?.let {uri->
            Glide.with(context).load(uri).into(viewBinding.ivImage)
        }

        /*if (UserPreference.selectedMediaType == "I") {
            Glide.with(context).load(UserPreference.seletedUri).into(viewBinding.selectedImageView)
        } else {
            val file = File(UserPreference.seletedUri.path)
            val uri = context.let {
                FileProvider.getUriForFile(
                    it,
                    "${context.packageName}.provider",
                    file
                )
            }
            Glide.with(context).load(uri).into(selectedImageView)
        }*/

        viewBinding.etCaption.setText(caption)
        viewBinding.etCaption.requestFocus()
        viewBinding.etCaption.setSelection(viewBinding.etCaption.getString().length)
        onClick(viewBinding)

        viewBinding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            viewBinding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = viewBinding.root.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (keyboardHeight > screenHeight * 0.15) {
                viewBinding.clCaption.translationY = -(keyboardHeight - 220).toFloat()
                viewBinding.btnOk.visibility = View.VISIBLE
            } else {
                viewBinding.clCaption.translationY = 0f
                viewBinding.btnOk.visibility = View.GONE
            }
        }
    }

    private fun onClick(viewBinding: PopUpFullScreenCaptionBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                dismiss()
            }

            btnOk.setOnClickListener {
                CommonUtils.hideKeyboard(context as ImageVideoConverter)
            }

            btnUpload.setOnClickListener {
                uploadClick(etCaption.getString())
                dismiss()
            }
        }
    }
}