package com.app.hihlo.ui.signUpToHome

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentTermsConditionsBinding
import com.app.hihlo.utils.network_utils.ProcessDialog
import kotlin.getValue

class TermsConditionsFragment :
    BaseNewFragment<FragmentTermsConditionsBinding, SignUpToHomeViewModel>(R.layout.fragment_terms_conditions) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var screenFrom = ""

    override fun onInitDataBinding(viewBinding: FragmentTermsConditionsBinding) {
        arguments?.let {
            screenFrom = it.getString("screen").toString()
        }
        setTittle(viewBinding)
        onClick(viewBinding)
    }

    private fun setTittle(viewBinding: FragmentTermsConditionsBinding) {
        viewBinding.apply {
            webView.setBackgroundColor(Color.BLACK)
            when (screenFrom) {
                "about" -> {
                    tvTitle.text = getString(R.string.about_us)
                    loadUrlInWebView("https://hihlo.com/about-us", viewBinding)
                }

                "termsCondition" -> {
                    tvTitle.text = getString(R.string.terms_and_conditions)
                    loadUrlInWebView("https://hihlo.com/terms-condition", viewBinding)
                }

                else -> {
                    tvTitle.text = getString(R.string.privacy_policy)
                    loadUrlInWebView("https://hihlo.com/privacy-policy", viewBinding)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun loadUrlInWebView(url: String, viewBinding: FragmentTermsConditionsBinding) {
        viewBinding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        viewBinding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                ProcessDialog.showDialog(requireActivity(), true)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                ProcessDialog.dismissDialog(true)
                super.onPageFinished(view, url)
            }
        }
        viewBinding.webView.loadUrl(url)
    }

    private fun onClick(viewBinding: FragmentTermsConditionsBinding) {
        viewBinding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}