package com.app.hihlo.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.app.hihlo.R
import com.app.hihlo.utils.ProgressDialog
import com.app.hihlo.utils.ProgressPercentageDialog

abstract class BaseFragment<DB : ViewDataBinding> : Fragment() {
    var _binding: DB? = null
    protected val binding get() = _binding!!
    private var progressDialog: ProgressDialog? = null
    var progressPercentageDialog: ProgressPercentageDialog? = null


    open fun getLayoutId(): Int {
        return 0
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        initView(savedInstanceState)

        return binding.root
    }

    abstract fun initView(savedInstanceState: Bundle?)

    override fun onResume() {
        super.onResume()

        // Get the fragment background color (or the color of a parent view)
        val backgroundColor = (view?.background as? ColorDrawable)?.color ?: Color.WHITE

        // Update status bar icon color
        (activity as? BaseActivity<*>)?.isColorLight(backgroundColor)
            ?.let { (activity as? BaseActivity<*>)?.updateStatusBarIcons(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showToast(message: String?) {
        val inflater = layoutInflater
        val parent =
            requireActivity().window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val layout: View = inflater.inflate(R.layout.long_toast, parent, false)
        layout.findViewById<TextView>(R.id.tv_toast).text = message

        Toast(requireContext()).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
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

    fun showProgressPercentage(visible: Boolean) {
        if (visible) {
            progressPercentageDialog?.dismiss()
            progressPercentageDialog = ProgressPercentageDialog(requireContext(),)
            progressPercentageDialog?.setCancelable(false)
            progressPercentageDialog?.show()
        } else {
            progressPercentageDialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        showProgress(false)
        showProgressPercentage(false)
    }
}
