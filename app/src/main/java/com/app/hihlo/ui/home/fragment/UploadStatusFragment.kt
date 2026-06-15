package com.app.hihlo.ui.home.fragment

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentUploadStatusBinding
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.home.activity.UploadStatusActivity
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.logD
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import kotlin.getValue

class UploadStatusFragment : BaseFragment<FragmentUploadStatusBinding>() {

    override fun getLayoutId(): Int {
        return R.layout.fragment_upload_status
    }

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUI()
        setObserver()
        onClick()
    }

    override fun initView(savedInstanceState: Bundle?) {
        activity?.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener {
            if (!isAdded) {
                return@addOnGlobalLayoutListener
            }

            val rect = Rect()
            activity?.window?.decorView?.getWindowVisibleDisplayFrame(rect)
            val screenHeight =
                activity?.window?.decorView?.height ?: return@addOnGlobalLayoutListener
            val keyboardHeight = screenHeight - rect.bottom
            val isKeyboardVisible = keyboardHeight > screenHeight * 0.15

            if (isKeyboardVisible) {
                binding.clCaptain.postDelayed({
                    if (!isAdded) return@postDelayed
                    val location = IntArray(2)
                    binding.clCaptain.getLocationOnScreen(location)
                    val captionBottom = location[1] + binding.clCaptain.height
                    logD("AddReelFragment:: CaptionBottom = $captionBottom")
                    val keyboardTop = rect.bottom
                    logD("AddReelFragment:: KeyboardTop = $keyboardTop")
                    val overlap = captionBottom - keyboardTop
                    logD("AddReelFragment:: Overlap = $overlap")
                    if (overlap > 0) {
                        val translationY = -(overlap + dpToPx()).toFloat()
                        logD("AddReelFragment:: TranslationY = $translationY")
                        binding.clCaptain.animate()
                            .translationY(translationY)
                            .setDuration(150)
                            .start()
                    }
                }, 100)
            } else {
                logD("AddReelFragment:: TranslationY = 0f")
                binding.clCaptain.animate()
                    .translationY(0f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    private fun dpToPx(): Int {
        return (20 * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun setUI() {
        binding.apply {
            UserPreference.uploadStatusFile?.let { file ->
                Glide.with(requireContext())
                    .load(file)
                    .into(selectedImageView)
            }
        }
    }

    private fun setObserver() {
        viewModel.addStoryLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add story success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            (activity as? UploadStatusActivity)?.apply {
                                setResult(
                                    RESULT_OK,
                                    Intent().apply {
                                        putExtra("statusUploaded", "true")
                                    }
                                )
                                finish()
                            }
                        }
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                }
            }
        }
    }

    private fun onClick() {
        binding.apply {
            backButton.setOnClickListener {
                (context as UploadStatusActivity).finish()
            }

            binding.uploadButton.setOnClickListener {
                UserPreference.uploadStatusFile?.let { file ->
                    uploadImage(file, UserPreference.selectedMediaType)
                }
            }
        }
    }

    private fun uploadImage(imageFile: File, assetType: String) {
        val s3Data = Preferences.getCustomModelPreference<LoginResponse>(
            requireContext(),
            LOGIN_DATA
        )?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        uploadImageToS3(
            requireContext(),
            imageFile,
            bucketName ?: "",
            objectKey,
            s3Data?.ACCESS_KEY ?: "",
            s3Data?.SECRET_KEY ?: "",
            assetType
        )
    }

    fun uploadImageToS3(
        context: Context,
        file: File,
        bucketName: String,
        objectKey: String,
        accessKey: String,
        secretKey: String,
        assetType: String,
    ) {
        // Initialize S3 client
        val s3Client = initializeS3Client(accessKey, secretKey)
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()
        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(
            context
        )
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)
        showProgressPercentage(true)
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    showProgressPercentage(false)
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.AWS_CDN_URL

                    val slash = "/"
                    val mediaUrl = "$urlCdn$slash$objectKey"

                    viewModel.hitAddStoryDataApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken,
                        AddStoryRequest(
                            assetUrl = mediaUrl,
                            assetType = assetType,
                            caption = binding.caption.getString()
                        )
                    )
                } else if (state == TransferState.FAILED) {
                    showProgressPercentage(false)
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat() * 100).toInt()
                if (progressPercentageDialog != null) {
                    progressPercentageDialog?.uploadPercentageChange(percentDone)
                }
            }

            override fun onError(id: Int, ex: Exception) {
                showProgressPercentage(false)
            }
        })
    }

    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        val clientConfig = com.amazonaws.ClientConfiguration()
        clientConfig.connectionTimeout = 120000 // 120 sec
        clientConfig.socketTimeout = 120000 // 120 sec
        clientConfig.maxErrorRetry = 5 // Retry in case of network issues
        return AmazonS3Client(credentials)
    }
}