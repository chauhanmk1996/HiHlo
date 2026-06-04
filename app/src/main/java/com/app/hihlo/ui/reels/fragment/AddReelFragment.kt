package com.app.hihlo.ui.reels.fragment

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentAddReelBinding
import com.app.hihlo.model.add_post.request.AddPostRequest
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.reels.view_model.AddReelViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.logD
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File

class AddReelFragment : Fragment() {
    private val viewModel: AddReelViewModel by viewModels()
    private lateinit var binding: FragmentAddReelBinding
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddReelBinding.inflate(layoutInflater)
        setUI()
        onClick()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setObserver()

        CommonUtils.touchHideKeyBoard(view, requireActivity())

        if (isAdded) {
            (requireActivity() as HomeActivity)
                .setOnlineStatusVisibility(true)
        }

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
                        val translationY = -(overlap + dpToPx(20)).toFloat()
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

    override fun onDestroyView() {
        super.onDestroyView()
        globalLayoutListener?.let {
            requireActivity()
                .window
                .decorView
                .viewTreeObserver
                .removeOnGlobalLayoutListener(it)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun setObserver() {
        viewModel.getAddReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add Reel success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            RTVariable.IS_MEDIA_UPLOADED = true
                            Toast.makeText(
                                requireContext(),
                                "Your Reel Uploaded Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            (context as HomeActivity).profileSelect()
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                    ProcessDialog.dismissDialog(true)
                }

                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.getAddPostLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add Post success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            RTVariable.IS_MEDIA_UPLOADED = true
                            Toast.makeText(
                                requireContext(),
                                "Your Post Uploaded Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            (context as HomeActivity).profileSelect()
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                    ProcessDialog.dismissDialog(true)
                }

                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

    }

    private fun onClick() {
        binding.apply {
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }

            binding.uploadButton.setOnClickListener {
                if (caption.getString().isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a caption", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    if (UserPreference.selectedMediaType == "I") {
                        val file =
                            MediaUtils.uriToFile(UserPreference.seletedUri, requireActivity())
                        uploadImage(imageFile = file, UserPreference.selectedMediaType)

                    } else {
                        val file = File(UserPreference.seletedUri.path)
                        uploadImage(imageFile = file, UserPreference.selectedMediaType)
                    }
                }
            }
        }
    }

    private fun setUI() {
        binding.apply {
            if (UserPreference.selectedMediaType == "I") {
                Glide.with(requireContext()).load(UserPreference.seletedUri).into(selectedImageView)
            } else {
                val file = File(UserPreference.seletedUri.path)
                val uri = context?.let {
                    FileProvider.getUriForFile(
                        it,
                        "${context?.packageName}.provider",
                        file
                    )
                }
                Glide.with(requireContext()).load(uri).into(selectedImageView)
            }
            if (UserPreference.selectedMediaToUpload == "reel") {
                title.text = "Add Reel"
            } else {
                title.text = "New Post"
            }
        }
    }

    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)

        // Increase timeout settings
        val clientConfig = com.amazonaws.ClientConfiguration()
        clientConfig.connectionTimeout = 120000 // 120 sec
        clientConfig.socketTimeout = 120000 // 120 sec
        clientConfig.maxErrorRetry = 5 // Retry in case of network issues

        return AmazonS3Client(credentials, clientConfig)
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

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(
            context
        )

        // Start the upload
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)
        ProcessDialog.showDialog(requireContext(), true)
        // Listen to upload events
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    ProcessDialog.dismissDialog(true)
                    // Upload completed successfully
//                    val mediaUrl = "https://$bucketName.s3.amazonaws.com/$objectKey"
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val mediaUrl = "$urlCdn$slash$objectKey"
                    println("Image URL: $mediaUrl")
                    if (UserPreference.selectedMediaToUpload == "reel") {
                        viewModel.hitAddReelApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken,
                            AddPostRequest(
                                assetUrl = mediaUrl,
                                assetType = UserPreference.selectedMediaType,
                                caption = binding.caption.text.toString()
                            )
                        )
                    } else {
                        Log.i("TAG", "onStateChanged: ${UserPreference.selectedCropRatio}")
                        viewModel.hitAddPostApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken,
                            AddPostRequest(
                                assetUrl = mediaUrl,
                                assetType = UserPreference.selectedMediaType,
                                caption = binding.caption.text.toString(),
                                postHeightSize = UserPreference.selectedCropRatio
                            )
                        )
                    }
                } else if (state == TransferState.FAILED) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.some_error_occurred_please_try_again),
                        Toast.LENGTH_SHORT
                    ).show()
                    println("Upload failed")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                if (bytesTotal > 0) {
                    val percentDone = (bytesCurrent.toFloat() / bytesTotal * 100).toInt()
                    Log.d("UploadProgress", "Uploaded: $percentDone%")
                }
            }


            override fun onError(id: Int, ex: Exception) {
                ProcessDialog.dismissDialog(true)
                ex.printStackTrace()
            }
        })
    }

    private fun uploadImage(imageFile: File, assetType: String) {
        val s3Data = Preferences.getCustomModelPreference<LoginResponse>(
            requireContext(),
            LOGIN_DATA
        )?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        Log.i("TAG", "uploadImage: " + Gson().toJson(s3Data))
        Log.i("TAG", "uploadImage: " + bucketName)
        Log.i("TAG", "uploadImage: " + objectKey)
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
}