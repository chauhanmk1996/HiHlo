package com.app.hihlo.ui.profile.terms_conditions

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentTermsConditionsBinding
import com.app.hihlo.utils.network_utils.ProcessDialog

class TermsConditionsFragment : Fragment() {
    private lateinit var binding: FragmentTermsConditionsBinding
    var screenFrom = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            screenFrom = it.getString("screen").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTermsConditionsBinding.inflate(layoutInflater)
        initViews()
        onClick()
        return binding.root
    }

    private fun onClick() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initViews() {
        setTittle()
    }

    private fun setTittle() {
        Log.i("TAG", "setTittle: "+screenFrom)
        if(screenFrom=="about"){
            binding.webView.setBackgroundColor(Color.BLACK)
            binding.tvTittle.text = "About Us"
            loadUrlInWebView("https://hihlo.com/about-us")
        }else if(screenFrom=="termsCondition"){
            binding.webView.setBackgroundColor(Color.BLACK)
            binding.tvTittle.text = "Terms and Conditions"
            loadUrlInWebView("https://hihlo.com/terms-condition")
        }else{
            binding.webView.setBackgroundColor(Color.BLACK)
            binding.tvTittle.text = "Privacy Policy"
            loadUrlInWebView("https://hihlo.com/privacy-policy")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun loadUrlInWebView(url: String) {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                ProcessDialog.showDialog(requireActivity(),true)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                ProcessDialog.dismissDialog(true)
                super.onPageFinished(view, url)
            }
        }
        binding.webView.loadUrl(url)
    }

}