package com.app.hihlo.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.app.hihlo.BR
import com.app.hihlo.R
import com.app.hihlo.utils.ProgressDialog
import com.app.hihlo.utils.SharedPreferenceUtil

abstract class BaseNewFragment<T : ViewDataBinding, V : BaseViewModel>(
    @LayoutRes
    private val layoutId: Int,
) : Fragment() {

    lateinit var sharedPreference: SharedPreferenceUtil
    private var mActivity: BaseNewActivity<T, V>? = null
    private lateinit var mViewDataBinding: T
    private var progressDialog: ProgressDialog? = null

    abstract fun onInitDataBinding(viewBinding: T)

    abstract fun getViewModel(): V

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseNewActivity<*, *>) {
            mActivity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        mViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        mViewDataBinding.setVariable(BR.viewModel, getViewModel())
        mViewDataBinding.lifecycleOwner = viewLifecycleOwner
        sharedPreference = SharedPreferenceUtil.getInstance(requireContext())
        return mViewDataBinding.root
    }

    override fun onDetach() {
        mActivity = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewDataBinding.executePendingBindings()
        onInitDataBinding(mViewDataBinding)
    }

    fun showProgress(visible: Boolean) {
        if (visible) {
            progressDialog?.dismiss()
            progressDialog = ProgressDialog(requireContext())
            progressDialog?.setCancelable(false)
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        showProgress(false)
    }

    open fun getBaseActivity(): BaseNewActivity<T, V>? {
        return mActivity
    }

    open fun getViewDataBinding(): T {
        return mViewDataBinding
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

    fun showLongToast(message: String?) {
        val inflater = layoutInflater
        val parent = requireActivity().findViewById<ViewGroup>(android.R.id.content)

        val layout: View = inflater.inflate(R.layout.long_toast, parent, false)
        layout.findViewById<TextView>(R.id.tv_toast).text = message

        Toast(requireContext()).apply {
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }
}