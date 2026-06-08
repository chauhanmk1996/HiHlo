package com.app.hihlo.ui.profile.become_creater

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentPhoneOtpBinding
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.become_creater.model.VerifyPhoneOtpRequest
import com.app.hihlo.ui.profile.become_creater.view_model.AddPhoneNumberViewModel
import com.app.hihlo.ui.profile.model.ImageItem
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.common.io.Files.getFileExtension
import com.google.gson.Gson
import java.io.File
import kotlin.getValue
import androidx.core.net.toUri
import com.app.hihlo.ui.profile.become_creater.model.SendOtpPhoneRequest
import com.app.hihlo.ui.profile.become_creater.model.UserToCreatorRequest


class PhoneOtpFragment : Fragment() {
    private lateinit var binding: FragmentPhoneOtpBinding
    private var countdown: CountDownTimer?=null
    private var phoneNumber = ""
    private var videoFile = ""
    private var imageList: ArrayList<ImageItem>?=null
    private val viewModel: AddPhoneNumberViewModel by viewModels()
    private var uploadImageList: MutableList<String> = mutableListOf()
    private var uploadVideoUrl = ""
    private var totalFilesToUpload = 0
    private var uploadedFilesCount = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoFile = it.getString("videoUri",videoFile)
            imageList =it.getParcelableArrayList<ImageItem>("image_list")
            phoneNumber = it.getString("phone").toString()
            Log.e("TAG", "onCreate: $videoFile \n $imageList \n $phoneNumber")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhoneOtpBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    private fun initViews() {
        customOtp()
       // uploadMedia()
        binding.apply {
            btnConfirm.setOnClickListener {
                checkValidation()
            }
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }
            clMain.setOnClickListener {
                CommonUtils.hideKeyboard(requireActivity())
            }
        }
    }

    private fun checkValidation() {
        val code = "${binding.etOtp1.text}${binding.etOtp2.text}${binding.etOtp3.text}" +
                "${binding.etOtp4.text}"
        if(code.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter otp", Toast.LENGTH_SHORT).show()
        }
        else{
            hitPhoneOtpVerifyApi(code)

        }
    }

    private fun hitPhoneOtpVerifyApi(code: String) {
        /*val bundle = Bundle()
        bundle.putString("from","register")
        // bundle.putParcelable("data",model)
        findNavController().navigate(
            R.id.verificationStatusFragment, // this is Fragment C
            bundle,
            NavOptions.Builder()
                .setPopUpTo(R.id.phoneOtpFragment, true) // remove B (OTP) from back stack
                .build()
        )*/

        val model = VerifyPhoneOtpRequest(
            phone = phoneNumber,
            otp = code
        )
        viewModel.hitVerifyPhoneOtp("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(),LOGIN_DATA)?.payload?.authToken,model)
        viewModel.getVerifyPhoneOtpLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    Log.e("TAG", "otp fragment success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
//                        uploadMedia()
                        findNavController().navigate(R.id.addYourPhotoFragment)

                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
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
        val allFiles = mutableListOf<File>()
        videoFile.let { allFiles.add(File(it)) }

        // 2. Convert image URIs to Files and add
        imageList?.forEach { imageItem ->
            imageItem.imageUri?.let { uri ->
                val imageFile = uriToFile(uri, requireActivity())
                allFiles.add(imageFile)
            }
        }

        totalFilesToUpload = allFiles.size
        uploadedFilesCount = 0

        // ✅ Now you have both video and image files in one list
        Log.e("TAG", "uploadMedia: Combined files list = $allFiles")

        allFiles.forEachIndexed { index, file ->
            Log.d("Upload", "Uploading file #$index: ${file.name}")
            uploadImage(file)
        }

    }

    fun initializeS3Client(context: Context, accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3Client(credentials)
    }
    fun uploadImageToS3(context: Context, file: File, bucketName: String, objectKey: String, accessKey: String, secretKey: String) {
        // Initialize S3 client
        val s3Client = initializeS3Client(context, accessKey, secretKey)

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        // Start the upload
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)

        // Listen to upload events
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    // Upload completed successfully
//                    val imageUrl = "https://$bucketName.s3.amazonaws.com/$objectKey"
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val imageUrl = "$urlCdn$slash$objectKey"
                   // uploadImageList.add(imageUrl)
                   // newImageUrl = imageUrl
                    println("Image URL: $imageUrl \n $imageUrl")
                    if(imageUrl.contains("jpg")||imageUrl.contains("png")){
                        uploadImageList.add(imageUrl)
                    }else{
                        uploadVideoUrl = imageUrl
                    }
                    uploadedFilesCount++
                    checkAllUploadsCompleted()

                } else if (state == TransferState.FAILED) {
                    // Handle failure
                    println("Upload failed")
                    uploadedFilesCount++
                    checkAllUploadsCompleted()
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Handle progress
                val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat() * 100).toInt()
                println("Progress: $percentDone%")
            }

            override fun onError(id: Int, ex: Exception) {
                // Handle error
                ex.printStackTrace()
            }
        })
    }
    private fun uploadImage(imageFile: File) {
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
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
    }


    /**
     * function is used to start timer when otp is sent successfully
     */
    fun startTimer() {
        if (countdown != null) {
            super.onDestroy()
            countdown?.cancel()
        }
        countdown = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                var t1 = ""
                var t2: String
                val min = seconds / 60
                val sec = seconds % 60
                t2 = if (sec < 10) {
                    "0${sec}"
                } else {
                    "$sec"
                }
                if (min < 10) {
                    t1 = "0${min}"
                } else {
                    t2 = "$min"
                }
                binding.tvDidnt.text = "Verify code in "
                binding.tvTime.visibility = View.VISIBLE
                binding.tvTime.text = "$t1:$t2 minute"
                binding.tvResend.visibility = View.GONE
            }
            @SuppressLint("StringFormatInvalid")
            override fun onFinish() {
                binding.tvTime.visibility = View.GONE
                binding.tvResend.visibility = View.VISIBLE
                binding.tvDidnt.text = "Didn't recieve otp?"
                binding.tvResend.setOnClickListener {
                    hitSendOtpPhoneNumberApi()
//                    sendEmailOtpViewModel.hitSendEmailOtp(signUpData?.email.toString(),signUpData?.username.toString())
                }
            }
        }.start()
    }
    fun setObserver(){
        viewModel.getAddPhoneNumberLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Flag user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        Toast.makeText(requireContext(), it.data.message +"${it.data.payload?.otp}", Toast.LENGTH_SHORT).show()
                        startTimer()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }
    private fun hitSendOtpPhoneNumberApi(){
        val phoneNumber = phoneNumber
        val model = SendOtpPhoneRequest(
            phone = phoneNumber
        )
        viewModel.hitAddPhoneNumber("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(),LOGIN_DATA)?.payload?.authToken,model)
    }

    // This function is used for custom otp filed like rounded border and next focus
    private fun customOtp() {
        val etOtp1 = binding.etOtp1
        val etOtp2 = binding.etOtp2
        val etOtp3 = binding.etOtp3
        val etOtp4 = binding.etOtp4
        startTimer()
        etOtp1.addTextChangedListener(GenericTextWatcher(etOtp1,etOtp2))
        etOtp2.addTextChangedListener(GenericTextWatcher(etOtp2,etOtp3))
        etOtp3.addTextChangedListener(GenericTextWatcher(etOtp3,etOtp4))
        etOtp4.addTextChangedListener(GenericTextWatcher(etOtp4,null))

        etOtp1.setOnKeyListener(GenericKeyEvent(etOtp1,null))
        etOtp2.setOnKeyListener(GenericKeyEvent(etOtp2,etOtp1))
        etOtp3.setOnKeyListener(GenericKeyEvent(etOtp3,etOtp2))
        etOtp4.setOnKeyListener(GenericKeyEvent(etOtp4,etOtp3))

    }


    // Class to change focus to another otp filed
    class GenericTextWatcher internal constructor(
        private val currentView: View,
        private val nextView: View?,
    ) :
        TextWatcher {
        override fun afterTextChanged(editable: Editable) { // TODO Auto-generated method stub
            val text = editable.toString()
            when (currentView.id) {
                R.id.etOtp1 -> if (text.length == 1) nextView!!.requestFocus()
                R.id.etOtp2 -> if (text.length == 1) nextView!!.requestFocus()
                R.id.etOtp3 -> if (text.length == 1) nextView!!.requestFocus()
                //R.id.etOtp4 -> if (text.length == 1) nextView!!.requestFocus()
            }
        }

        override fun beforeTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int
        ) {

        }

        override fun onTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int
        ) {
        }
    }

    class GenericKeyEvent internal constructor(private val currentView: EditText, private val previousView: EditText?) : View.OnKeyListener{
        override fun onKey(p0: View?, keyCode: Int, event: KeyEvent?): Boolean {
            if(event!!.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && currentView.id != R.id.etOtp1&& currentView.text.isEmpty()) {
                previousView!!.text = null
                previousView.requestFocus()
                return true
            }
            return false
        }
    }
}