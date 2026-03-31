package com.app.hihlo.ui.profile.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentEditProfileNewBinding
import com.app.hihlo.model.city_list.response.Cities
import com.app.hihlo.model.edit_profile.request.EditProfileRequest
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.interest_list.response.Interests
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.profile.adapter.CityAdapter
import com.app.hihlo.ui.profile.adapter.InterestSpinnerAdapter
import com.app.hihlo.ui.profile.view_model.EditProfileViewModel
import com.app.hihlo.ui.signup.activity.SignupFlowActivity
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.touchHideKeyBoard
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.common.io.Files.getFileExtension
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.app.hihlo.model.city_list.response.CityListResponse
import com.app.hihlo.model.gender_list.Gender
import com.app.hihlo.ui.profile.adapter.InterestAdapter
import com.app.hihlo.utils.CommonUtils.touchHideKeyBoard
import com.app.hihlo.utils.UsernameInputFilter
import java.text.BreakIterator
import kotlin.collections.isNotEmpty
import kotlin.collections.toMutableList

class EditProfileNewFragment : Fragment() {
    private lateinit var binding:FragmentEditProfileNewBinding
    private var userDetailsMain:UserDetailsX?=null
    private var intrestList:List<Interests>?=null
    var imageUrl = ""
    var newImageUrl = ""
    var intrestName = ""
    var cityId = ""
    var selectedGenderId_:Int?=null
    var citiesList:List<Cities> = listOf()
    var isValidUsername=1
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val viewModel: EditProfileViewModel by viewModels()

    var from = ""

    var selectedImageUri = Uri.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        from = arguments?.getString("from").toString()
        val userDetails = arguments?.getParcelable<UserDetailsX>("userDetail")
        userDetailsMain = userDetails

        Log.e("TAG", "Received user details: $userDetailsMain \n $from")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileNewBinding.inflate(layoutInflater)
        initViews()
        apiOberserver()
//        usernameTextWatcher()
        showCharCount()
        binding.root.setOnClickListener {
//            CommonUtils.hideKeyboard(requireActivity())
            CommonUtils.touchHideKeyBoard(binding.root, requireActivity())
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (from=="social"||from=="normal"){}
            else findNavController().popBackStack()
        }
        binding.etAbout.filters = arrayOf(GraphemeLengthFilter(150))
        return binding.root
    }
    private fun setBottomMargin() {
        val layoutParams = binding.mainLayout.layoutParams as? ViewGroup.MarginLayoutParams
        layoutParams?.let {
            val desiredMarginInPx = when (activity) {
                is HomeActivity -> {
                    if ((requireActivity() as HomeActivity).isGestureNavigation()) CommonUtils.dpToPx(0) else CommonUtils.dpToPx(-40)
                }
                is SignupFlowActivity -> {
                    if ((requireActivity() as SignupFlowActivity).isGestureNavigation()) CommonUtils.dpToPx(0) else CommonUtils.dpToPx(-40)
                }
                else -> CommonUtils.dpToPx(0) // Default or handle other activities
            }
            it.bottomMargin = desiredMarginInPx
            binding.mainLayout.layoutParams = it
        }
    }

    private fun showCharCount() {
        binding.etAbout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //val currentLength = s?.length ?: 0
                val currentLength = getGraphemeCount(s.toString())
                Log.e("LENGTH", "LENGTH>>> "+currentLength+" | full text: ${s}")
                binding.tvCount.text = "$currentLength/150"
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    class GraphemeLengthFilter(private val max: Int) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val newText = StringBuilder(dest)
                .replace(dstart, dend, source?.subSequence(start, end).toString())
                .toString()
            val count = getGraphemeCount(newText)
            if (count <= max) return null
            var allowedText = dest.toString()
            for (i in start until end) {
                val temp = StringBuilder(dest)
                    .replace(dstart, dend, source?.subSequence(start, i + 1).toString())
                    .toString()
                if (getGraphemeCount(temp) > max) break
                allowedText = source?.subSequence(start, i + 1).toString()
            }
            return allowedText
        }
        private fun getGraphemeCount(text: String): Int {
            val it = BreakIterator.getCharacterInstance()
            it.setText(text)
            var count = 0
            while (it.next() != BreakIterator.DONE) {
                count++
            }
            return count
        }
    }

    fun getGraphemeCount(text: String): Int {
        val it = BreakIterator.getCharacterInstance()
        it.setText(text)
        var count = 0
        var start = it.first()
        while (it.next() != BreakIterator.DONE) {
            count++
        }
        return count
    }

    private fun usernameTextWatcher() {
        binding.etUserName.apply {
            doAfterTextChanged {
                if (it.isNullOrEmpty()) return@doAfterTextChanged

                if (!it.startsWith("@")) {
                    it.insert(0, "@")
                }

                if (selectionStart < 1) {
                    setSelection(1)
                }
                viewModel.hitCheckUsernameDataApi(text.toString())
            }
            filters = arrayOf(
                UsernameInputFilter(),
                InputFilter.LengthFilter(40)
            )

            setText("@")
            setSelection(text.length)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    selectionStart == 1 &&
                    text.length == 1
                ) {
                    true // block delete
                } else {
                    false
                }
            }
        }
    }

    private fun initViews(){
        setUserDetail()
        hitGenderList()
        touchHideKeyBoard(binding.root, requireActivity())
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
            imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
//                selectedImageUri = imageUri
                if (imageUri != null) {
                    openCropActivity(imageUri)
                }else{
                    Toast.makeText(requireActivity(), "Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.spinTopic.setOnClickListener {
            loadTopicSpinner(intrestList) { selectedCity ->
                // Use selectedCity object (id, name)
                binding.spinTopic.text = selectedCity.name // Replace Spinner with TextView or set programmatically
                intrestName = selectedCity.name ?: ""            }
        }
        binding.spinCity.setOnClickListener {
            showCitySelectionDialog(citiesList) { selectedCity ->
                // Use selectedCity object (id, name)
                binding.spinCity.text = selectedCity.city_name // Replace Spinner with TextView or set programmatically
                cityId = selectedCity.city_name.toString()
            }
        }
//        binding.spinGender.setOnClickListener {
//        }


    }

    fun hitGenderList(){
        viewModel.hitGenderListApi()
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
//                    binding.ivUserImage.setImageURI(resultUri)
                    Glide.with(requireContext()).load(resultUri).into(binding.ivUserImage)
                    val fileImage = uriToFile(resultUri ?: Uri.EMPTY,requireActivity())
                    uploadImage(fileImage)
                }
            }
        }else {
            Log.w("HomeFragment", " cropping was cancelled or failed with code: $resultCode")
        }

    }
    private fun apiOberserver() {
        viewModel.hitInterestListDataApi()
        viewModel.getInterestListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "interest list success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            val list = it.data.payload.rows
                            intrestList = list
                            Log.e("TAG", "apiOberserver: $intrestList")

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
//        viewModel.hitCitiesListDataApi()
        /*viewModel.getCitiesListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "city list success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            citiesList = it.data.payload.rows
                          //  intrestList = list
//                            loadCitySpinner(citiesList)
//                            Log.e("TAG", "apiOberserver: $list", )

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
        }*/
        viewModel.getEditProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "update profile success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            val list = it.data.payload
                            Log.e("TAG", "apiOberserver: $list")
                            Log.e("TAG", "PRO IMG: $imageUrl | $newImageUrl")
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            val loginData = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)
                            loginData?.payload?.apply {
                                city = list.user.city
                                country = list.user.country
                                dob = list.user.dob
                                email = list.user.email
                                userId = list.user.id ?: loginData.payload.userId
                                name = list.user.name
                                phone = list.user.phone
                                profileImage = if (newImageUrl!="") newImageUrl else imageUrl
                                username = list.user.username
                                userName = list.user.username
                            }
                            Preferences.setCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA, loginData)
                            if(from=="social"||from=="normal"){
                                val intent = Intent(requireActivity(), HomeActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            }else{
                                val imageURL = if (newImageUrl!="") newImageUrl else imageUrl
                                if(requireActivity() is HomeActivity) (requireActivity() as HomeActivity).updateProfileImage(imageURL)
                                findNavController().popBackStack()
                            }

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
        viewModel.getCheckUsernameLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "check username success: ${Gson().toJson(it)}")
                    if (it.data?.status==0||it.data?.status==1){
                        if (it.data.code == 200){
                            binding.apply {
                                isValidUsername=it.data.payload.usernameAvailable
                                if (it.data.payload.usernameAvailable==2||it.data.payload.usernameAvailable==3){
//                                    usernameWarning.isVisible=true
//                                    usernameWarning.text = it.data.message
                                    usernameCheck.setImageResource(R.drawable.cancel_red)
                                }else{
//                                    usernameWarning.isVisible=false
                                    usernameCheck.setImageResource(R.drawable.checked_green)
                                }
                            }
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

    }

    private fun setUserDetail(){
        binding.apply {
            if(from=="social" || from =="normal"){
                backButton.visibility = View.GONE
                tittle.text ="Complete Profile"
                updateButton.text = "Update Profile"
                etName.isEnabled = false
            }else{
                spinGender.isEnabled = false
                spinGender.isClickable = false
                spinGender.isFocusable = false

            }
            if(from == "normal"){
                etName.isEnabled = false
                etPhoneNumber.isEnabled = false
                etEmail.isEnabled = false
                etCountry.isEnabled = false
                spinCity.isEnabled = false
            }
            var char_length = getGraphemeCount(userDetailsMain?.about.toString())
            tvCount.text = "${char_length}/150"
            etName.setText(userDetailsMain?.name)
            setUsername(etUserName, userDetailsMain?.username ?: "")
//            etUserName.setText(userDetailsMain?.username)
            etPhoneNumber.setText(userDetailsMain?.phone)
            etDOB.setText(userDetailsMain?.dob)
            etEmail.setText(userDetailsMain?.email)
            etCountry.setText(userDetailsMain?.country)
            etAbout.setText(userDetailsMain?.about)
            imageUrl = userDetailsMain?.profile_image.toString()
            cityId = userDetailsMain?.city.toString()
            spinCity.text = userDetailsMain?.city ?: ""
            spinTopic.text = userDetailsMain?.interest_name ?: ""
            intrestName = userDetailsMain?.interest_name.toString()
            Glide.with(requireActivity())
                .load(userDetailsMain?.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .into(ivUserImage)
            updateButton.setOnClickListener {
                checkValidation()
            }
            etDOB.setOnClickListener {
                openDateCalender()
            }
            ivEditIcon.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = "image/*"
                }
                imagePickerLauncher.launch(intent)
            }
        }
    }
    private fun setUsername(editText: EditText, username: String?) {

        val clean = username
            ?.replace("@", "")
            ?.lowercase()
            ?: ""

        // SET TEXT FIRST
        editText.setText("@$clean")
        editText.setSelection(editText.text.length)

        // THEN apply filters
        editText.filters = arrayOf(
            UsernameInputFilter(),
            InputFilter.LengthFilter(40) // includes @
        )

        lockAtSymbol(editText)
        attachUsernameWatcher(editText)
    }
    private fun attachUsernameWatcher(editText: EditText) {

        editText.doAfterTextChanged {

            if (editText.selectionStart < 1) {
                editText.setSelection(1)
            }

            // API call only when username part changes
            val username = editText.text.toString().removePrefix("@")
            if (username.isNotEmpty()) {
                viewModel.hitCheckUsernameDataApi(username)
            }
        }
    }
    private fun lockAtSymbol(editText: EditText) {
        editText.setOnKeyListener { _, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    editText.selectionStart == 1 &&
                    editText.text.length == 1
        }
    }


    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        val s3Client = AmazonS3Client(credentials)

        // Increase timeout settings
        val clientConfig = com.amazonaws.ClientConfiguration()
        clientConfig.connectionTimeout = 120000 // 120 sec
        clientConfig.socketTimeout = 120000 // 120 sec
        clientConfig.maxErrorRetry = 5 // Retry in case of network issues

        return AmazonS3Client(credentials, clientConfig)
    }
    fun uploadImageToS3(context: Context, file: File, bucketName: String, objectKey: String, accessKey: String, secretKey: String) {
        // Initialize S3 client
        val s3Client = initializeS3Client( accessKey, secretKey)

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(context)

        // Start the upload
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)

        // Listen to upload events
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                val safeContext = context ?: return
                if (state == TransferState.COMPLETED) {
                    // Upload completed successfully
//                    val imageUrl = "https://$bucketName.s3.amazonaws.com/$objectKey"
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(safeContext, LOGIN_DATA)?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val imageUrl = "$urlCdn$slash$objectKey"
                    newImageUrl = imageUrl
                    println("Image URL: $imageUrl")
                    val loginData = Preferences.getCustomModelPreference<LoginResponse>(safeContext, LOGIN_DATA)
                    loginData?.payload?.apply {
                        profileImage = newImageUrl
                    }
                    Preferences.setCustomModelPreference<LoginResponse>(safeContext, LOGIN_DATA, loginData)

                } else if (state == TransferState.FAILED) {
                    // Handle failure
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

    private fun openDateCalender() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { view, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = formatter.format(selectedCalendar.time)
                binding.etDOB.setText(formattedDate)
                Log.d("DatePicker", "Selected Date: $formattedDate")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        val maxSelectableDate = Calendar.getInstance()
        maxSelectableDate.add(Calendar.YEAR, -13)
        datePickerDialog.datePicker.maxDate = maxSelectableDate.timeInMillis
        //datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()

    }

    private fun checkValidation(){
        val etName = binding.etName.text.trim()
        val etUserName = binding.etUserName.text.trim()
        val etPhone = binding.etPhoneNumber.text.trim()
        val dob = binding.etDOB.text.trim()
        val etEmail = binding.etEmail.text.trim()
        val aboutMe = binding.etAbout.text.trim()
        val city = cityId
        val country = binding.etCountry.text.trim()
        val intrest = intrestName

        if(imageUrl==""&&newImageUrl==""){
            Toast.makeText(requireActivity(), "Please select your image", Toast.LENGTH_SHORT).show()
        }
        else if(etName.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter your name", Toast.LENGTH_SHORT).show()
        }else if(etUserName.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter user name", Toast.LENGTH_SHORT).show()
        }
        else if(isValidUsername!=1){
            Toast.makeText(requireActivity(), "Please enter a valid user name", Toast.LENGTH_SHORT).show()
        }
        else if(etPhone.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter your phone number", Toast.LENGTH_SHORT).show()
        }else if(dob.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter date of birth", Toast.LENGTH_SHORT).show()
        }else if(etEmail.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter your email", Toast.LENGTH_SHORT).show()
        }else if(!CommonUtils.isValidEmail(etEmail.toString())){
            Toast.makeText(requireActivity(), "Please enter valid email", Toast.LENGTH_SHORT).show()
        }else if(cityId.isNullOrBlank()){
            Toast.makeText(requireActivity(), "Please select city", Toast.LENGTH_SHORT).show()
        }
        else if(aboutMe.isEmpty()){
            Toast.makeText(requireActivity(), "Please enter your Bio.", Toast.LENGTH_SHORT).show()
        }else{
            var imageUrlToSend = ""
            if(newImageUrl.isEmpty()){
                imageUrlToSend = imageUrl
            }else{
                imageUrlToSend = newImageUrl
            }
            val gender = binding.spinGender.selectedItemId.toInt()+1
            val model = EditProfileRequest(
                name = etName.toString(),
                username = etUserName.toString(),
                phone = etPhone.toString(),
                dob = dob.toString(),
               // gender_id = gender.toString(),
                gender_id = selectedGenderId_.toString(),
                email = etEmail.toString(),
                city = city.toString(),
                country = country.toString(),
                interestName = intrest.toString(),
                profileImageUrl = imageUrlToSend,
                about = aboutMe.toString(),


            )
            Log.e("TAG", "checkValidation: $model")
            viewModel.hitEditProfileDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, model)
            //updateApi()
        }
    }

    /*private fun loadTopicSpinner(intrestList: List<Interests>) {
        val topicNames = intrestList.map { it.name } ?: emptyList()
        val adapter = ArrayAdapter(
            requireActivity(),
            R.layout.spinner_item_selected, // This layout defines the look of the currently selected item
            topicNames
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinTopic.adapter = adapter
        intrestName.let {
            val position = topicNames.indexOf(it)
            if (position >= 0) {
                binding.spinTopic.setSelection(position)
            }
        }

        binding.spinTopic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCity = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }*/
    private fun loadTopicSpinner(intrestList: List<Interests>?, onSelected: (Interests) -> Unit) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_city_selector)

        // Make dialog fill width
        val window = dialog.window
        window?.setBackgroundDrawable(  Color.TRANSPARENT.toDrawable())
        val params = window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params?.y = 50.dpToPx(requireContext()) // 👈 50dp top margin
        window?.attributes = params

        val etSearch = dialog.findViewById<EditText>(R.id.etSearchCity)
        val rvCities = dialog.findViewById<RecyclerView>(R.id.rvCities)

        val adapter = InterestAdapter(intrestList) {
//            intrestName = it.name ?: ""
            onSelected(it)

            dialog.dismiss()
        }

        rvCities.layoutManager = LinearLayoutManager(requireContext())
        rvCities.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val filtered = intrestList?.filter {
                    it.name?.contains(s.toString(), ignoreCase = true) == true
                }
                adapter.filterList(filtered ?: listOf())
            }
        })

        dialog.show()









        /*val adapter = InterestSpinnerAdapter(requireContext(), intrestList)
        binding.spinTopic.adapter = adapter

        intrestName.let {
            val position = intrestList.indexOfFirst { interest -> interest.name == it }
            if (position >= 0) {
                binding.spinTopic.setSelection(position)
            }
        }

        binding.spinTopic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedInterest = intrestList[position]
                // use selectedInterest.id or name or image_url as needed
//                binding.spinTopic.setSelection(position)
                intrestName = intrestList[position].name
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }*/
    }
    /*private fun loadCitySpinner(list: List<Cities>) {
        val cityName = list.map { it.city_name }
        val adapter = ArrayAdapter(
            requireActivity(),
            R.layout.spinner_item_selected, // This layout defines the look of the currently selected item
            cityName
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinCity.adapter = adapter
        cityId.let {
            val position = cityName.indexOf(it)
            if (position >= 0) {
                binding.spinCity.setSelection(position)
            }
        }
        binding.spinCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
             override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                 val selectedCity = parent?.getItemAtPosition(position).toString()
             }

            override fun onNothingSelected(parent: AdapterView<*>?) {
           }
        }
    }*/
    private fun showCitySelectionDialog(cityList: List<Cities>, onSelected: (Cities) -> Unit) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_city_selector)

        // Make dialog fill width
        val window = dialog.window
        window?.setBackgroundDrawable(  Color.TRANSPARENT.toDrawable())
        val params = window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params?.y = 50.dpToPx(requireContext()) // 👈 50dp top margin
        window?.attributes = params

        val etSearch = dialog.findViewById<EditText>(R.id.etSearchCity)
        val rvCities = dialog.findViewById<RecyclerView>(R.id.rvCities)

        var isLoading = false
        var currentPage=1
        val adapter = CityAdapter(listOf()) {
            onSelected(it)
            dialog.dismiss()
        }
        val cityList : MutableList<Cities> = mutableListOf()
        rvCities.layoutManager = LinearLayoutManager(requireContext())
        rvCities.adapter = adapter
        viewModel.hitCitiesListDataApi("", "10", currentPage.toString())  // Call API with search + limit
        viewModel.getCitiesListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "cities success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1 && it.data.code == 200) {
                        val newList = it.data.payload.cities
                        if (etSearch.text.toString().isNotEmpty()) {
                            adapter.filterList(newList)
                        }else{
                            if (it.data.payload.cities.isNotEmpty() == true) {
                                isLoading=true
                                cityList.addAll(newList.toMutableList())
                                if (currentPage == 1 ) {
                                    adapter.filterList(cityList)
                                } else {
                                    adapter.filterList(cityList)
                                }
                            }

                        }
                    }
                }
                Status.ERROR -> {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                Status.LOADING -> {
                    // Optional: show progress inside dialog
                }
            }
        }
        rvCities.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (layoutManager?.findLastVisibleItemPosition() == adapter.itemCount - 2) {
                    Log.i("TAG", "onScrolled: "+"triggered")
                    if (isLoading) {
                        currentPage++
                        viewModel.hitCitiesListDataApi("", "10", currentPage.toString())  // Call API with search + limit
                    }
                    isLoading = false
                }
            }
        })
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentPage=1
                cityList.clear()
                val query = s.toString().trim()
                viewModel.hitCitiesListDataApi(query, "10")  // Call API with search + limit
            }
        })

        dialog.show()
    }


    private fun initGenderSpinner(gendeList: List<Gender>) {
        val genderNames = gendeList.map { it.gender_name }
        val selectedGenderId = gendeList.map { it.id }

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item_selected, // your custom selected item layout
            genderNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                // ✅ Set background for the selected item shown in spinner
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.black_303030))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                // ✅ Set background for dropdown items
                if (position == binding.spinGender.selectedItemPosition) {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinGender.adapter = adapter

        // ✅ Pre-select gender if exists
        val selectedIndex = genderNames.indexOfFirst { it.equals(userDetailsMain?.gender, ignoreCase = true) }
        if (selectedIndex >= 0) {
            binding.spinGender.setSelection(selectedIndex)
        }

        binding.spinGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedGenderId_ = selectedGenderId[position]
                Log.d("TAG", "onItemSelected: ${selectedGenderId_}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    /* private fun initGenderSpinner(preSelectedGender: String) {
        val genderArray = resources.getStringArray(R.array.Gender)

        val adapter = ArrayAdapter(
            requireActivity(),
            R.layout.spinner_item_selected,
            genderArray
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.spinGender.adapter = adapter

        // ✅ Set pre-selected value if it exists in array
        val selectedIndex = genderArray.indexOfFirst { it.equals(preSelectedGender, ignoreCase = true) }
        if (selectedIndex >= 0) {
            binding.spinGender.setSelection(selectedIndex)
        }

        binding.spinGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGender = parent?.getItemAtPosition(position).toString()
                // Do something with selectedGender
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
*/
    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = requireActivity().currentFocus
                if (focusedView is EditText) {
                    val outRect = Rect()
                    focusedView.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        focusedView.clearFocus()
                        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                    }
                }
            }
            false
        }
        setBottomMargin()
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            // Check if fragment is still attached
            if (!isAdded) return@OnGlobalLayoutListener

            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)

            val screenHeight = binding.root.height
            val keypadHeight = screenHeight - rect.bottom

            val keyboardOpen = keypadHeight > screenHeight * 0.15

            val params = binding.bottomLayout.layoutParams as ConstraintLayout.LayoutParams

            if (keyboardOpen) {
                params.bottomMargin = keypadHeight
            } else {
                // Safe resource access
                val density = resources.displayMetrics.density
                params.bottomMargin = (10 * density).toInt()
            }

            binding.bottomLayout.layoutParams = params
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
//        requireActivity().window.setSoftInputMode(
//            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
//        )
    }

    fun setObserver(){
        viewModel.getGenderLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            var data = it.data.payload.genderList
                            Log.d("TAG", "setOsdcdcbserver: ${it.data.payload}")
                            initGenderSpinner(data)

                            //  setCountData(it.data.payload.userDetails.posts_count, it.data.payload.userDetails.followers_count, it.data.payload.userDetails.following_count)

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

    override fun onDestroyView() {
        // Remove the listener to avoid leaks and crashes
        globalLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        globalLayoutListener = null
        super.onDestroyView()
    }

}