package com.app.hihlo.ui.chat.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentCaptureVideoBinding
import com.app.hihlo.databinding.FragmentOpenAdBinding
import com.app.hihlo.databinding.FragmentViewAdsBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.chat.view_model.ChatViewModel
import com.app.hihlo.ui.chat.view_model.OpenAdViewModel
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlin.getValue

class OpenAdFragment : Fragment() {
    private val viewModel: OpenAdViewModel by viewModels()
    private var seekBarHandler: Handler? = null
    private var seekBarRunnable: Runnable? = null
    private var seekBarStartTime = 0L
    private var seekBarElapsedTime = 0L
    private var seekBarDuration = 15000L
    private var isSeekBarRunning = false
    private var apiHitAt50 = false // Flag to track if API was already called
    private lateinit var binding: FragmentOpenAdBinding
    var mediaType = ""
    var mediaUrl = ""
    var adId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mediaUrl = it.getString("mediaUrl") ?: ""
            adId = it.getString("adId") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOpenAdBinding.inflate(layoutInflater)
        Glide.with(requireContext()).load(mediaUrl).into(binding.imageView)
        startSeekBarProgress(binding.idSeekBar, 5000)
        return binding.root
    }

    private fun hitViewAdsApi() {
        // Make sure this doesn't block the UI
        viewModel.hitViewAdsApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken,
            adId = adId
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        (requireActivity() as? HomeActivity)?.setOnlineStatusVisibility(false)
    }

    fun startSeekBarProgress(seekBar: SeekBar, durationInMillis: Long) {
        seekBarDuration = durationInMillis
        seekBar.max = 100
        seekBar.progress = 0
        seekBarStartTime = System.currentTimeMillis()
        isSeekBarRunning = true
        apiHitAt50 = false // Reset flag

        seekBarHandler = Handler(Looper.getMainLooper())
        seekBarRunnable = object : Runnable {
            override fun run() {
                if (!isSeekBarRunning) return

                val elapsed = System.currentTimeMillis() - seekBarStartTime + seekBarElapsedTime
                val progress = ((elapsed.toFloat() / seekBarDuration) * 100).toInt()
                seekBar.progress = progress.coerceAtMost(100)

                // Hit API only once at 50%
                if (progress >= 50 && !apiHitAt50) {
                    apiHitAt50 = true
                    hitViewAdsApi()
                }

                // Continue updating regardless of API call
                if (progress < 100) {
                    seekBarHandler?.postDelayed(this, 16)
                } else {
                    // Seek bar completed
                    if (isAdded && isVisible && view != null) {
//                        Toast.makeText(requireContext(), "Coin Added Successfully.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
        seekBarHandler?.post(seekBarRunnable!!)
    }

    private fun setObserver() {
        viewModel.getViewAdsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "view ads success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {

                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    // Don't show loading dialog to avoid UI blocking
                    // ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "api Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                    // Continue seek bar even on error
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler to prevent memory leaks
        isSeekBarRunning = false
        seekBarHandler?.removeCallbacks(seekBarRunnable!!)
        seekBarHandler = null
        seekBarRunnable = null
    }
}