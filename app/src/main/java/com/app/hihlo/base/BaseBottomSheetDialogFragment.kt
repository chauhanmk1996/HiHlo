package com.app.hihlo.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.app.hihlo.BR
import com.app.hihlo.utils.SharedPreferenceUtil
import androidx.core.graphics.drawable.toDrawable
import com.app.hihlo.R

abstract class BaseBottomSheetDialogFragment<T : ViewDataBinding, V : BaseViewModel>(
    @LayoutRes
    private val layoutId: Int,
) : BottomSheetDialogFragment() {

    lateinit var sharedPreference: SharedPreferenceUtil
    private var mActivity: BaseNewActivity<T, V>? = null
    private lateinit var mViewDataBinding: T

    abstract fun onInitDataBinding(viewBinding: T)
    abstract fun getViewModel(): V

    override fun getTheme(): Int {
        return R.style.BottomSheetTopRoundedDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        mViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        mViewDataBinding.setVariable(BR.viewModel, getViewModel())
        mViewDataBinding.lifecycleOwner = viewLifecycleOwner
        sharedPreference = SharedPreferenceUtil.getInstance(requireContext())
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return mViewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInitDataBinding(mViewDataBinding)
    }

    private fun showProgress(visible: Boolean) {
        getBaseActivity()?.showProgress(visible)
    }

    fun showToast(message: String?) {
        val inflater = layoutInflater
        val parent = requireActivity().findViewById<ViewGroup>(android.R.id.content)

        val layout: View = inflater.inflate(R.layout.long_toast, parent, false)
        layout.findViewById<TextView>(R.id.tv_toast).text = message

        Toast(requireContext()).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    open fun getBaseActivity(): BaseNewActivity<T, V>? {
        return mActivity
    }

    open fun getViewDataBinding(): T {
        return mViewDataBinding
    }
}