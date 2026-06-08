package com.app.hihlo.ui.profile.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentContactUsBinding
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.contact_us.request.ContactUsRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.home.bottom_sheet.UploadMediaBottomSheet
import com.app.hihlo.ui.profile.adapter.AdapterFaq
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.profile.view_model.ContactUsViewModel
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import java.io.File

class ContactUsFragment : Fragment() {
    private lateinit var binding: FragmentContactUsBinding
    private val viewModel: ContactUsViewModel by viewModels()
    private var screenShotUrl=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactUsBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CommonUtils.touchHideKeyBoard(view,requireActivity())
        setObserver()
        keyBoardClose()
    }
    private fun setObserver() {
        viewModel.getContactUsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            Toast.makeText(requireContext(), "Request has been sent.", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
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

    private fun initViews() {
        binding.apply {
            attachImageView.setOnClickListener {
//                val bottomSheetFragment = UploadMediaBottomSheet.newInstance("contactUs").apply {
//                    onGallerySelected = {
//                        dismiss()
//                        checkGalleryPermissionAndPick()
//                    }
//                }
//                bottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")

                val popup = UploadMediaBottomSheet(requireContext(), "contactUs", binding.root).apply {
                    onGallerySelected = {
                        dismiss()
                        checkGalleryPermissionAndPick()
                    }
                }
                popup.show()
            }
            llBack.setOnClickListener {
                findNavController().popBackStack()
            }
            submitButton.setOnClickListener {
                if (etMessage.getString().isEmpty()){
                    Toast.makeText(requireContext(), "Please Enter a Message!", Toast.LENGTH_SHORT).show()
                }else if(etCharCount.getString().isEmpty()){
                    Toast.makeText(requireContext(), "Please Enter a Contact Number!", Toast.LENGTH_SHORT).show()
                }else if (screenShotUrl.isEmpty()){
                    Toast.makeText(requireContext(), "Attach a screenshot!", Toast.LENGTH_SHORT).show()
                }else{
                    hitApi()
                }
            }
        }
    }

    private fun hitApi() {
        viewModel.hitContactUsDataApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, request = ContactUsRequest(message = binding.etMessage.text.toString(), phone = binding.etCharCount.text.toString(), imageUrl = screenShotUrl))

    }
    private fun checkGalleryPermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ needs both image & video read permissions
//            val permissions = arrayOf(
//                Manifest.permission.READ_MEDIA_IMAGES,
//                Manifest.permission.READ_MEDIA_VIDEO
//            )
//            requestMultiplePermissionsLauncher.launch(permissions)
            launchMediaPicker()
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                launchMediaPicker()
            } else {
                requestSinglePermissionLauncher.launch(permission)
            }
        }
    }
    private fun launchMediaPicker() {
        mediaPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mimeType = requireContext().contentResolver.getType(uri)
            if (mimeType?.startsWith("video") == true) {

            } else {
//                val file = MediaUtils.uriToFile(uri, requireActivity())
//                uploadImage(file, "I")
                openCropActivity(uri)
            }
        } else {
            Toast.makeText(requireContext(), "No media selected", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openCropActivity(imageUri: Uri) {
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
        }
        val destinationUri = Uri.fromFile(File(requireActivity().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .start(requireContext(), this)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Always call super
        Log.i("TAG", "onActivityResult: "+"outside")
        if (resultCode==RESULT_OK){
            when(requestCode){
                UCrop.REQUEST_CROP -> {
                    val resultUri = UCrop.getOutput(data!!)
                    Log.i("TAG", "onActivityResult: "+resultUri)
                    Glide.with(requireContext()).load(resultUri).into(binding.selectedImage)
                    val file = MediaUtils.uriToFile(resultUri ?: Uri.EMPTY, requireActivity())
                    uploadImage(file, "I")
                }
            }
        }else {
            Log.w("HomeFragment", " cropping was cancelled or failed with code: $resultCode")
        }

    }
    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3Client(credentials)
    }
    fun uploadImageToS3(context: Context, file: File, bucketName: String, objectKey: String, accessKey: String, secretKey: String, assetType:String) {
        // Initialize S3 client
        val s3Client = initializeS3Client(accessKey, secretKey)

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

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
                    screenShotUrl = mediaUrl

                } else if (state == TransferState.FAILED) {
                    // Handle failure
                    println("Upload failed")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Handle progress
                val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat() * 100).toInt()
                println("Progress: $percentDone%")
            }

            override fun onError(id: Int, ex: Exception) {
                ProcessDialog.dismissDialog(true)
                // Handle error
                ex.printStackTrace()
            }
        })
    }
    private fun uploadImage(imageFile: File, assetType:String) {
        var s3Data = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        uploadImageToS3(requireContext(), imageFile, bucketName ?: "", objectKey, s3Data?.ACCESS_KEY ?: "", s3Data?.SECRET_KEY ?: "", assetType)
    }
    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
    }


    fun keyBoardClose() {
        setupUI(binding.root)
    }
    private fun setupUI(view: View) {
        // Set up touch listener for non-EditText views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                false
            }
        }

        // If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireActivity())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }



}