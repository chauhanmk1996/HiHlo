package com.app.hihlo.ui.profile.become_creater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentCaptureVideoBinding
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.profile.become_creater.model.UserToCreatorRequest
import com.app.hihlo.ui.profile.become_creater.view_model.CaptureVideoViewModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.profile.model.ImageItem
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.common.io.Files.getFileExtension
import com.google.gson.Gson
import java.io.File

class CaptureVideoFragment : Fragment() {
    private lateinit var binding: FragmentCaptureVideoBinding
    private val viewModel : CaptureVideoViewModel by viewModels()
    private var imageList: ArrayList<ImageItem>?=null
    val VIDEO_CAPTURE_REQUEST_CODE = 101
    val VIDEO_CROP_REQUEST_CODE =-102
    var selectVideoUri = ""
    private var totalFilesToUpload = 0
    private var uploadedFilesCount = 0
    private var uploadImageList: MutableList<String> = mutableListOf()
    var isImageUploading = false
    private var uploadVideoUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageList = arguments?.getParcelableArrayList<ImageItem>("image_list")
            Log.e("TAG", "onCreate: $imageList", )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCaptureVideoBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    private fun initViews() {
        Glide.with(requireContext())
            .load(R.drawable.image_capture) // your GIF drawable
            .into(binding.ivCaptureVideo)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.captureButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // seconds
            startActivityForResult(intent, VIDEO_CAPTURE_REQUEST_CODE)
        }

        binding.btnContinue.setOnClickListener {
            if(selectVideoUri.isEmpty()){
                Toast.makeText(requireActivity(), "Please capture a video for verification", Toast.LENGTH_SHORT).show()
            }else{
//                val bundle = Bundle()
//                bundle.putString("videoUri",selectVideoUri)
//                bundle.putParcelableArrayList("image_list",imageList)
//                findNavController().navigate(R.id.addPhoneNumberFragment,bundle)
                uploadMedia()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIDEO_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = data?.data
            if(videoUri!=null){
                val intent = Intent(requireActivity(), TrimVideoActivity::class.java)
                intent.putExtra("videoUrl",videoUri.toString())
                startActivityForResult(intent, VIDEO_CROP_REQUEST_CODE)
            }else{
                Toast.makeText(requireActivity(), "Video capture cancelled", Toast.LENGTH_SHORT).show()
            }
            // You can use this URI to play or upload the video
            Log.d("VideoPath", "Recorded video URI: $videoUri")
        }
        if(requestCode==VIDEO_CROP_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val file = File(UserPreference.seletedUri.path)
            Log.e("TAG", "onActivityResult: ${UserPreference.seletedUri} \n $file", )
            if(UserPreference.seletedUri.path!=null){
                binding.btnContinue.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.theme)
                binding.btnPlay.visibility = View.VISIBLE
                Glide.with(this)
                    .load(UserPreference.seletedUri.path)
                    .thumbnail(0.1f)
                    .into(binding.ivCaptureVideo)
                selectVideoUri = file.toString()
            }else{
                binding.btnContinue.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.gray_303030)
                binding.btnPlay.visibility = View.GONE
            }
        }
    }


















    private fun uploadMedia(){
        /* val bundle = Bundle()
         bundle.putString("videoUri",videoFile)
         bundle.putParcelableArrayList("image_list",imageList)
         bundle.putString("phone",model.phone)
         findNavController().navigate(
             R.id.verificationStatusFragment, // this is Fragment C
             bundle,
             NavOptions.Builder()
                 .setPopUpTo(R.id.phoneOtpFragment, true) // remove B (OTP) from back stack
                 .build()
         )*/
        val allFiles = mutableListOf<UploadMediaType>()
        selectVideoUri.let { allFiles.add(UploadMediaType(File(it), "V")) }

        // 2. Convert image URIs to Files and add
        imageList?.forEach { imageItem ->
            imageItem.imageUri?.let { uri ->
                val imageFile = uriToFile(uri, requireActivity())
                allFiles.add(UploadMediaType(imageFile, "I"))
            }
        }

        totalFilesToUpload = allFiles.size
        uploadedFilesCount = 0

        // ✅ Now you have both video and image files in one list
        Log.e("TAG", "uploadMedia: Combined files list = $allFiles")

        allFiles.forEachIndexed { index, data ->
            Log.d("Upload", "Uploading file #$index: ${data.file.name}")
            ProcessDialog.showDialog(requireContext(), true)
            uploadImage(data)
        }

    }
    /*fun initializeS3Client(context: Context, accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3Client(credentials)
    }*/
    fun initializeS3Client(context: Context, accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        val s3Client = AmazonS3Client(credentials)

        // Increase timeout settings
        val clientConfig = com.amazonaws.ClientConfiguration()
        clientConfig.connectionTimeout = 120000 // 120 sec
        clientConfig.socketTimeout = 120000 // 120 sec
        clientConfig.maxErrorRetry = 5 // Retry in case of network issues

        return AmazonS3Client(credentials, clientConfig)
    }
    fun uploadImageToS3(context: Context, file: UploadMediaType, bucketName: String, objectKey: String, accessKey: String, secretKey: String) {
        // Initialize S3 client
        val s3Client = initializeS3Client(context, accessKey, secretKey)

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(context)

        // Start the upload
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file.file)
        ProcessDialog.showDialog(requireContext(), true)

        // Listen to upload events
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
//                    ProcessDialog.dismissDialog(true)
                    // Upload completed successfully
//                    val imageUrl = "https://$bucketName.s3.amazonaws.com/$objectKey"

                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val imageUrl = "$urlCdn$slash$objectKey"
                    // uploadImageList.add(imageUrl)
                    // newImageUrl = imageUrl
                    println("Image URL: $imageUrl \n $imageUrl")
                    /*if(imageUrl.contains("jpg")||imageUrl.contains("png")){
                        uploadImageList.add(imageUrl)
                    }else{
                        uploadVideoUrl = imageUrl
                    }*/
                    if (file.mediaType=="I"){
                        uploadImageList.add(imageUrl)
                    }else{
                        uploadVideoUrl = imageUrl
                    }
                    uploadedFilesCount++
                    checkAllUploadsCompleted()

                } else if (state == TransferState.FAILED) {
                    ProcessDialog.dismissDialog(true)
                    // Handle failure
                    println("Upload failed")
                    uploadedFilesCount++
                    checkAllUploadsCompleted()
                }
            }

            /*override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Handle progress
                val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat() * 100).toInt()
                println("Progress: $percentDone%")
            }*/
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                if (bytesTotal > 0) {
                    val percentDone = (bytesCurrent.toFloat() / bytesTotal * 100).toInt()
                    Log.d("UploadProgress", "Uploaded: $percentDone%")
                }
            }

            override fun onError(id: Int, ex: Exception) {
                ProcessDialog.dismissDialog(true)
                Toast.makeText(requireContext(), ex.toString(), Toast.LENGTH_SHORT).show()
                // Handle error
                ex.printStackTrace()
            }
        })
    }
    private fun uploadImage(imageFile: UploadMediaType) {
        val s3Data = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        uploadImageToS3(requireContext(), imageFile, bucketName ?: "", objectKey, s3Data?.ACCESS_KEY ?: "", s3Data?.SECRET_KEY ?: "")
    }

    fun uriToFile(uri: Uri, activity: Activity): File {
        val contentResolver = activity.contentResolver
        val fileExtension = getFileExtension(uri.toString())
        val fileName = "image_${System.currentTimeMillis()}.$fileExtension"
        val file = File(activity.cacheDir, fileName)

        contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return file
    }

    private fun checkAllUploadsCompleted() {
        if (uploadedFilesCount == totalFilesToUpload) {
            Log.d("Upload", "✅ All files uploaded. Now hitting the API.")
            hitUserToCreatorApi()
        }
    }
    private fun hitUserToCreatorApi() {
        val model = UserToCreatorRequest(
            imageUrls = uploadImageList,
            videoUrl = uploadVideoUrl
        )
        viewModel.hitUserToCreator("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(),LOGIN_DATA)?.payload?.authToken,model)
        viewModel.getUserToCreatorLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Flag user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(
                            R.id.profileSettingFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.profileSettingFragment, true) // clears everything in backstack
                                .build()
                        )
                        //uploadMedia()

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
}
data class UploadMediaType(var file: File, var mediaType: String)