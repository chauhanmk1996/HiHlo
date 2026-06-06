package com.app.hihlo.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.core.graphics.drawable.toDrawable
import com.app.hihlo.R

abstract class BaseDialog<T : ViewDataBinding>(
    context: Context,
    @LayoutRes private val layoutId: Int,
) : Dialog(context) {

    protected lateinit var binding: T
        private set

    open fun getDialogWidth(): Int = ViewGroup.LayoutParams.MATCH_PARENT
    open fun getDialogHeight(): Int = ViewGroup.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, null, false)
        setContentView(binding.root)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        initViews(binding)
    }

    fun showToast(message: String?) {
        val inflater = layoutInflater
        val parent = findViewById<ViewGroup>(android.R.id.content)

        val layout: View = inflater.inflate(R.layout.long_toast, parent, false)
        layout.findViewById<TextView>(R.id.tv_toast).text = message

        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    protected abstract fun initViews(viewBinding: T)
}