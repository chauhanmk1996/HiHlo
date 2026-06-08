package com.app.hihlo.ui.home.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentHomeBinding
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.selectedGender
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.adapter.AdapterHomeCreators
import com.app.hihlo.ui.home.adapter.AdapterHomeGenders
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.bottom_sheet.UploadMediaBottomSheet
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson

import java.io.File
import com.yalantis.ucrop.UCrop


class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private var myStoryData: MyStory = MyStory()
    private var isMediaUploaded: Int = -1
    private lateinit var adapterHomePosts:AdapterHomeCreators
    private val viewModel: HomeViewModel by viewModels()
    private var isLoading = false
    private var currentPage=1
    private var isRefreshedFromMenu=false
    private var creatorsList:MutableList<Post> = mutableListOf()
    private var isHomeDataLoaded = false
    private var allStory: List<Story>? = null
    private var scrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null

    override fun initView(savedInstanceState: Bundle?) {
        viewModel.hitGenderListApi()
        val layoutManager = StaggeredGridLayoutManager(2 , StaggeredGridLayoutManager.VERTICAL)
        binding.creatorsRecycler.layoutManager = layoutManager
        Log.i("TAG", "initView: "+ Preferences.getStringPreference(requireContext(), FCM_TOKEN))
        Log.i("TAG", "initView: "+ Preferences.getStringPreference(requireContext(), LOGIN_DATA))
        adapterHomePosts = AdapterHomeCreators(mutableListOf()){ post, click, position ->
            when(click){
                0->{
                    creatorsList.toTypedArray().forEachIndexed { index, creator ->
                        Log.e("TAG", "Home success: [$index] = $creator")
                    }
                    Log.e("TAG", "Home success: ${Posts()} || ${position.toString()}")
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToUserPostListFragment(homePosts = creatorsList.toTypedArray(), profilePosts = Posts(), from = "home", position = position.toString()))
                }
                1->{
                    (requireActivity() as HomeActivity).hideNavigationView()
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToProfileFragment("0", post.user_id.toString()))
                }
            }
        }
        binding.creatorsRecycler.adapter = adapterHomePosts
        creatorsList.clear()
        currentPage=1
        hitServiceListApi(currentPage, selectedGender)
        setPagination()
    }
    override fun getLayoutId(): Int {
        return R.layout.fragment_home
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        onClick()
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
        requireActivity().supportFragmentManager.setFragmentResultListener("home_click", viewLifecycleOwner) { _, _ ->
            Log.i("TAG", "onViewCreated: homeIconTap")

            isRefreshedFromMenu = true
            creatorsList.clear()
            currentPage = 1
            binding.progressBar.isVisible=true
            if (binding.nestedScrollView.scrollY == 0) {
                hitServiceListApi(currentPage, selectedGender)
            } else {
                binding.nestedScrollView.smoothScrollTo(0, 0)
                binding.nestedScrollView.setOnScrollChangeListener(
                    NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                        if (scrollY == 0) {
                            binding.nestedScrollView.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
                            hitServiceListApi(currentPage, selectedGender)
                        }
                    }
                )
            }

        }
    }

    private fun refreshData() {
        Handler(Looper.getMainLooper()).postDelayed({
            creatorsList.clear()
            currentPage = 1
            hitServiceListApi(currentPage, selectedGender)
            binding.swipeRefresh.isRefreshing = false
        }, 1000)
    }

    private fun onClick() {
        binding.mainLayout.setOnClickListener{
            binding.homeFilterGenderRecycler.isVisible=false
        }
        binding.notificationLayout.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationFragment)

        }
        binding.allButton.setOnClickListener {
            if (binding.homeFilterGenderRecycler.isVisible){
                binding.homeFilterGenderRecycler.isVisible=false
            }else{
                binding.homeFilterGenderRecycler.isVisible=true
            }
        }
    }
    private fun setPagination() {
        scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            val scrollView = binding?.nestedScrollView ?: return@OnScrollChangedListener
            val contentView = scrollView.getChildAt(scrollView.childCount - 1)

            val scrollY = scrollView.scrollY
            val scrollViewHeight = scrollView.height
            val contentBottom = contentView.bottom

            val diff = contentBottom - (scrollY + scrollViewHeight)

            Log.d("SCROLL_MANUAL", "scrollY: $scrollY, contentBottom: $contentBottom, diff: $diff")

            if (diff <= 300 && diff  != 0) { // `300` is a buffer to pre-load before actual bottom
                if (isLoading){
                    currentPage++
                    hitServiceListApi(currentPage, selectedGender)
                }
                isLoading = false
            }
        }

    }

    override fun onResume() {
        super.onResume()

        // Ensure listener is created BEFORE adding
        if (scrollChangedListener == null) {
            scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
                val scrollView = binding.nestedScrollView
                val contentView = scrollView.getChildAt(scrollView.childCount - 1)

                val scrollY = scrollView.scrollY
                val scrollViewHeight = scrollView.height
                val contentBottom = contentView.bottom

                val diff = contentBottom - (scrollY + scrollViewHeight)

                Log.d("SCROLL_MANUAL", "scrollY: $scrollY, contentBottom: $contentBottom, diff: $diff")

                if (diff <= 300 && isLoading) {
                    isLoading = false
                    currentPage++
                    hitServiceListApi(currentPage, selectedGender)
                }
            }
        }

        scrollChangedListener?.let {
            binding.nestedScrollView.viewTreeObserver.addOnScrollChangedListener(it)
        }

        // Delay to ensure keyboard has fully closed
        view?.postDelayed({
            (requireActivity() as HomeActivity).fullyResetFloatingButton()
        }, 100)
    }

    override fun onPause() {
        scrollChangedListener?.let {
            if (binding.nestedScrollView.viewTreeObserver.isAlive) {
                binding.nestedScrollView.viewTreeObserver.removeOnScrollChangedListener(it)
            }
        }
        scrollChangedListener = null
        super.onPause()
    }


    private fun hitServiceListApi(page: Int, genderId:Int?=null) {
        Log.e("TAG", "Home success: ${Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken}")
        viewModel.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, page.toString(), "10", if (genderId==null) "" else genderId.toString())
    }
    private fun setObserver() {
        viewModel.getHomeLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isRefreshedFromMenu=false
                            isHomeDataLoaded=true
                            isMediaUploaded = it.data.payload.is_story_uploaded
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            Log.e("TAG", "setObserver: $allStory", )
                            binding.storiesRecycler.adapter = AdapterStoriesRecycler( it.data.payload.is_story_uploaded, it.data.payload.my_story ?: MyStory(), it.data.payload.stories,  ::getSelectedStory, it.data.payload.myProfile.profileImage)
                            if(it.data.payload.unreadNotificationCount==0){
//                                binding.notificationButton.setImageResource(R.drawable.notification_bell)
                                binding.notificationDot.isVisible=false
                            }else{
//                                binding.notificationButton.setImageResource(R.drawable.notification_bell_with_dot)
                                binding.notificationDot.isVisible=true
                            }
                            if (it.data.payload.posts.isNotEmpty()){
                                isLoading=true
                                creatorsList.addAll(it.data.payload.posts)
                                if (currentPage == 1) {
                                    if (creatorsList.size > 0){
                                        adapterHomePosts.clearList()
                                        adapterHomePosts.updateList(it.data.payload.posts.toMutableList())
                                    }else{
                                        adapterHomePosts.clearList()
                                    }
                                } else {
                                    adapterHomePosts.updateList(it.data.payload.posts.toMutableList())
                                }
                            }

                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                    binding.progressBar.isVisible=false
                }
                Status.LOADING -> {
                    if (currentPage==1) {
                        if (isRefreshedFromMenu){
                        }else{
                            ProcessDialog.showDialog(requireContext(), true)
                        }
                    }
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                    binding.progressBar.isVisible=false
                }
            }
        }
        viewModel.addStoryLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add story success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            hitServiceListApi(currentPage, selectedGender)
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    if (currentPage==1) ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.getGenderLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            var data = it.data.payload.genderList
                            Log.d("TAG", "setOsdcdcbserver: ${it.data.payload}")
                            if (selectedGender==null){
                                binding.allButton.text = data[0].gender_name
                            }else{
                                binding.allButton.text = data[selectedGender ?: 0].gender_name
                            }
                            binding.homeFilterGenderRecycler.adapter = AdapterHomeGenders(/*it.data.payload.filters.available_genders*/data){ it,name->
                                binding.homeFilterGenderRecycler.isVisible=false
                                creatorsList.clear()
                                currentPage=1
                                if (it==0){
                                    selectedGender=null
                                }else{
                                    selectedGender=it
                                }
                                hitServiceListApi(currentPage, selectedGender)
                                if (selectedGender==null){
                                    binding.allButton.text = data[0].gender_name
                                }else{
                                    binding.allButton.text = data[selectedGender ?: 0].gender_name
                                }
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


    }

    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun checkGalleryPermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mimeType = requireContext().contentResolver.getType(uri)
            if (mimeType?.startsWith("video") == true) {
                val durationInMillis = getVideoDuration(requireContext(), uri)
                val durationInSeconds = durationInMillis / 1000

                UserPreference.seletedUri = Uri.EMPTY
                val intent = Intent(requireActivity(), TrimVideoActivity::class.java)
                intent.putExtra("videoUrl",uri.toString())
                intent.putExtra("from","home")
                startActivityForResult(intent, REQUEST_CODE_CROP_VIDEO)
            } else {
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
                REQUEST_CODE_CROP_VIDEO->{
                    val file = File(UserPreference.seletedUri.path)
                    uploadImage(file, "V")
                }
                UCrop.REQUEST_CROP -> {
                    val resultUri = UCrop.getOutput(data!!)
                    Log.i("TAG", "onActivityResult: "+resultUri)
                    val file = MediaUtils.uriToFile(resultUri ?: Uri.EMPTY, requireActivity())
                    uploadImage(file, "I")
                }
            }
        }else {
            Log.w("HomeFragment", " cropping was cancelled or failed with code: $resultCode")
        }

    }
    private fun getVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
    fun getSelectedStory(position:Int, story:Story, view:View, intVal: Int){
            if(position==0){
                if(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator ==1){
                    if (isMediaUploaded==1){
                        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToStoryFragment(isMyStory = "1", myStoryData = myStoryData, otherStoryData = allStory?.toTypedArray() ?: emptyArray()))
                    }else if(isMediaUploaded==2){
                        ReusablePopup(
                            context = requireContext(),
                            anchorView = view,
                            onOption1Click = {
                                checkGalleryPermissionAndPick()
                            },
                            onOption2Click = {},
                            option1Text = "Open Gallery",
                            option2Text = "Cancel",
                            option1ImageRes = R.drawable.profile_gallery_icon, // Add your own move to request icon
                            option2ImageRes = R.drawable.ic_cancel_red
                        ).show()



//                        val popup = UploadMediaBottomSheet(requireContext(), "home", binding.storiesRecycler).apply {
//                            onGallerySelected = {
//                                dismiss()
//                                checkGalleryPermissionAndPick()
//                            }
//                        }
//                        popup.show()

                    }
                }else{
                    Toast.makeText(requireContext(), "You are not a creator", Toast.LENGTH_SHORT).show()
                }


            }else{
                val bundle = Bundle().apply {
                    putParcelableArrayList("storyList", ArrayList(allStory ?: emptyList()))
                    putParcelable("myStoryData", myStoryData)
                    putInt("position", position - 1)
                }
                try {
                    findNavController().navigate(R.id.secondStoryFragment, bundle)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
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
                    val caption = "Test Caption"
                    viewModel.hitAddStoryDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, AddStoryRequest(assetUrl = mediaUrl, assetType = assetType, caption = caption))

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
}