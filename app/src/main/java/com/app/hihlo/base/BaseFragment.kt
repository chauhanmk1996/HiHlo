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

abstract class BaseFragment<DB : ViewDataBinding> : Fragment() {
    var _binding: DB? = null
    protected val binding get() = _binding!!
    open fun getLayoutId(): Int {
        return 0 // Default: you should override this
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
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

}
