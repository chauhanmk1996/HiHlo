package com.app.hihlo.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.Html
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.app.hihlo.R
import com.app.hihlo.databinding.LayoutPopupMenuBinding

class ReusablePopup(
    private val context: Context,
    private val anchorView: View,
    private val onOption1Click: () -> Unit,
    private val onOption2Click: () -> Unit,
    private val onOption3Click: (() -> Unit)? = null,
    private val onOption4Click: (() -> Unit)? = null,
    private val option1Text: String = "Option 1",
    private val option2Text: String = "Option 2",
    private val option3Text: String? = null,
    private val option4Text: String? = null,
    private val option1ImageRes: Int? = null,
    private val option2ImageRes: Int? = null,
    private val option3ImageRes: Int? = null,
    private val option4ImageRes: Int? = null,
    private val alignEnd: Boolean? = false,
) {
    private var popupWindow: PopupWindow? = null

    fun show() {
        val binding = LayoutPopupMenuBinding.inflate(LayoutInflater.from(context))

        // Option 1
        binding.tvOption1.text = Html.fromHtml(option1Text, Html.FROM_HTML_MODE_LEGACY)
        option1ImageRes?.let { binding.ivOption1.setImageResource(it) }
        binding.ivOption1.visibility = if (option1ImageRes != null) View.VISIBLE else View.GONE

        // Option 2
        binding.tvOption2.text = Html.fromHtml(option2Text, Html.FROM_HTML_MODE_LEGACY)
        option2ImageRes?.let { binding.ivOption2.setImageResource(it) }
        binding.ivOption2.visibility = if (option2ImageRes != null) View.VISIBLE else View.GONE

        binding.tvOption1.setOnClickListener {
            onOption1Click()
            popupWindow?.dismiss()
        }

        binding.tvOption2.setOnClickListener {
            onOption2Click()
            popupWindow?.dismiss()
        }

        // Option 3
        if (onOption3Click != null && option3Text != null) {
            binding.tvOption3.apply {
                visibility = View.VISIBLE
                text = Html.fromHtml(option3Text, Html.FROM_HTML_MODE_LEGACY)
                option3ImageRes?.let { binding.ivOption3.setImageResource(it) }
                binding.ivOption3.visibility =
                    if (option3ImageRes != null) View.VISIBLE else View.GONE
                setOnClickListener {
                    onOption3Click.invoke()
                    popupWindow?.dismiss()
                }
            }
        } else {
            binding.tvOption3.visibility = View.GONE
            binding.ivOption3.visibility = View.GONE
        }

        // Option 4
        if (onOption4Click != null && option4Text != null) {
            binding.tvOption4.apply {
                visibility = View.VISIBLE
                text = Html.fromHtml(option4Text, Html.FROM_HTML_MODE_LEGACY)
                option4ImageRes?.let { binding.ivOption4.setImageResource(it) }
                binding.ivOption4.visibility =
                    if (option4ImageRes != null) View.VISIBLE else View.GONE
                setOnClickListener {
                    onOption4Click.invoke()
                    popupWindow?.dismiss()
                }
            }
        } else {
            binding.tvOption4.visibility = View.GONE
            binding.ivOption4.visibility = View.GONE
        }

        popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 10f
            setOnDismissListener {
                anchorView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        anchorView.setBackgroundColor(ContextCompat.getColor(context, R.color.white_40))

        anchorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        // Measure popup
        binding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val popupHeight = binding.root.measuredHeight
        val popupWidth = binding.root.measuredWidth

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorY = location[1]
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        val spaceBelow = screenHeight - (anchorY + anchorView.height + CommonUtils.dpToPx(100))
        val shouldShowAbove = spaceBelow < popupHeight

        // Center horizontally
        val centerX = (screenWidth - popupWidth) / 2

        if (shouldShowAbove) {
            popupWindow?.showAtLocation(
                anchorView,
                Gravity.TOP or Gravity.START,
                centerX,
                anchorY - popupHeight - 15
            )
        } else {
            popupWindow?.showAtLocation(
                anchorView,
                Gravity.TOP or Gravity.START,
                centerX,
                anchorY + anchorView.height + 15
            )
        }
    }
}




