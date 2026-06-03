package com.app.hihlo.ui.reels.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentAddReelBinding
import com.app.hihlo.databinding.FragmentPredefinedChatBinding
import com.app.hihlo.databinding.FragmentReelsBinding
import com.app.hihlo.model.add_post.request.AddPostRequest
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.profile.view_model.GetProfileViewModel
import com.app.hihlo.ui.reels.view_model.AddReelViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.dpToPx
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import kotlin.compareTo

class AddReelFragment : Fragment() {
    private lateinit var binding: FragmentAddReelBinding
    private val viewModel: AddReelViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddReelBinding.inflate(layoutInflater)
        setUI()
        onClick()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        CommonUtils.touchHideKeyBoard(view,requireActivity())
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val finalHeight = if (isKeyboardVisible){
                if (isGestureNavigation()) imeHeight else imeHeight-dpToPx(40)
            } else {
                imeHeight+dpToPx(20)
            }
            binding.root.setPadding(0, 0, 0, finalHeight)
            insets
        }
        (requireActivity() as HomeActivity).setOnlineStatusVisibility(true)

        requireActivity().window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = requireActivity().window.decorView.height
            val keyboardHeight = screenHeight - rect.bottom
            if (keyboardHeight > screenHeight * 0.15) {
                binding.captionLayout.translationY = -(keyboardHeight - 750).toFloat()
            } else {
                binding.captionLayout.translationY = 0f
            }
        }
    }
    fun isGestureNavigation(): Boolean {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        return resId > 0 && resources.getInteger(resId) == 2
    }
    private fun setObserver() {
        viewModel.getAddReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add Reel success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            RTVariable.IS_MEDIA_UPLOADED = true
                            Toast.makeText(requireContext(), "Your Reel Uploaded Successfully", Toast.LENGTH_SHORT).show()
                            (context as HomeActivity).profileSelect()
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            RTVariable.IS_MEDIA_UPLOADED = true
                            Toast.makeText(requireContext(), "Your Post Uploaded Successfully", Toast.LENGTH_SHORT).show()
                            (context as HomeActivity).profileSelect()
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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

    private fun onClick(){
        binding.apply {
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }

            binding.uploadButton.setOnClickListener {
                if (caption.getString().isEmpty()){
                    Toast.makeText(requireContext(), "Please enter a caption", Toast.LENGTH_SHORT).show()
                }else{
                    if (UserPreference.selectedMediaType == "I") {
                        val file = MediaUtils.uriToFile(UserPreference.seletedUri, requireActivity())
                        uploadImage(imageFile = file, UserPreference.selectedMediaType)

                    }else{
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
            }else{
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
            if (UserPreference.selectedMediaToUpload=="reel"){
                title.text = "Add Reel"
            }else{
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

    fun uploadImageToS3(context: Context, file: File, bucketName: String, objectKey: String, accessKey: String, secretKey: String, assetType:String) {
        // Initialize S3 client
        val s3Client = initializeS3Client(accessKey, secretKey)

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(context)

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
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val mediaUrl = "$urlCdn$slash$objectKey"
                    println("Image URL: $mediaUrl")
                    if (UserPreference.selectedMediaToUpload=="reel"){
                        viewModel.hitAddReelApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, AddPostRequest(assetUrl = mediaUrl, assetType = UserPreference.selectedMediaType, caption=binding.caption.text.toString()))
                    }else{
                        Log.i("TAG", "onStateChanged: ${UserPreference.selectedCropRatio}")
                        viewModel.hitAddPostApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, AddPostRequest(assetUrl = mediaUrl, assetType = UserPreference.selectedMediaType, caption=binding.caption.text.toString(), postHeightSize = UserPreference.selectedCropRatio))
                    }
                } else if (state == TransferState.FAILED) {
                    Toast.makeText(requireContext(), getString(R.string.some_error_occurred_please_try_again), Toast.LENGTH_SHORT).show()
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
    private fun uploadImage(imageFile: File, assetType:String) {
        val s3Data = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        Log.i("TAG", "uploadImage: "+Gson().toJson(s3Data))
        Log.i("TAG", "uploadImage: "+bucketName)
        Log.i("TAG", "uploadImage: "+objectKey)
        uploadImageToS3(requireContext(), imageFile, bucketName ?: "", objectKey, s3Data?.ACCESS_KEY ?: "", s3Data?.SECRET_KEY ?: "", assetType)
    }
}