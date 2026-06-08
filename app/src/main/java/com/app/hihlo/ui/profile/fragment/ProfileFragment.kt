package com.app.hihlo.ui.profile.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.ImageVideoConverter.ImageVideoConverter
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentProfileBinding
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.get_profile.Data
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.reel.response.Creator
import com.app.hihlo.model.reel.response.Pagination
import com.app.hihlo.model.reel.response.Payload
import com.app.hihlo.model.reel.response.Reel
import com.app.hihlo.model.staticModel.profileDetailList
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.HomeNew.utility.FilePickerBottomsheet
import com.app.hihlo.ui.HomeNew.utility.ImageFilePickerBottomsheet2
import com.app.hihlo.ui.HomeNew.utility.VideoFilePickerBottomsheet
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.bottom_sheet.UploadMediaBottomSheet
import com.app.hihlo.ui.home.bottom_sheet.ViewPostBottomSheetFragment
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.profile.activity.RechargeCoinsActivity
import com.app.hihlo.ui.profile.adapter.AdapterProfileMediaViewPager
import com.app.hihlo.ui.profile.adapter.ShowProfileDetailsAdapter
import com.app.hihlo.ui.profile.fragment.profile_view_pager.ProfilePostsFragment
import com.app.hihlo.ui.profile.view_model.GetProfileViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.Utils
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue


class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    private lateinit var bottomSheetFragment: UploadMediaBottomSheet
    private lateinit var profileMediaViewPager: AdapterProfileMediaViewPager
    private var userDetails = UserDetailsX()
    private val viewModel: GetProfileViewModel by viewModels()
    private val args: ProfileFragmentArgs by navArgs()
    var isMyProfile = ""
    var isFollowing = false
    var userId = ""
    var from = ""
    private var selectedMediaType: String = "I"
    private var selectedBottomSheetType = ""
    private val viewModel2: HomeViewModel by activityViewModels()

    companion object {
        val REQUEST_CODE_CROP_VIDEO = 10
        val EXTRA_CROPPED_URI = "cropped_video_uri_extra"

    }


    override fun onResume() {
        super.onResume()
        if (::profileMediaViewPager.isInitialized) {
            binding.viewPager.post {
                when (UserPreference.selectedMediaToUpload) {
                    "post" -> binding.viewPager.currentItem = 0
                    "reel" -> binding.viewPager.currentItem = 1
                }
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        isMyProfile = args.isMyProfile
        userId = args.userId ?: ""
        //Toast.makeText(requireActivity(), "P ${userId.toString()}", Toast.LENGTH_LONG).show()
        from = args.from
//        profileMediaViewPager = AdapterProfileMediaViewPager(Posts(),Posts(), ::getSelectedPost)
        onClick()
        setShowProfileDetailsRecycler()
        scrollViewListener()
        (requireActivity() as? HomeActivity)?.setOnlineStatusVisibility(false)
    }

    fun scrollViewListener() {
        binding.scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val view = v.getChildAt(v.childCount - 1)
                val diff = view.bottom - (v.height + scrollY)

                if (diff <= 300) { // Close to bottom
                    val currentFragment = getCurrentViewPagerFragment()
                    if (currentFragment is ProfilePostsFragment) {
                        currentFragment.onParentScrolledToBottom()
                    }
                }
            }
        )

    }

    fun getCurrentViewPagerFragment(): Fragment? {
        val currentItem = binding.viewPager.currentItem
        binding.viewPager.adapter as FragmentStateAdapter
        "f$currentItem" // Default tag format

        return childFragmentManager.findFragmentByTag("f$currentItem")
    }

    private fun getSelectedPost(
        asset_url: String,
        caption: String,
        posts: Posts,
        click: Int,
        reelPosition: Int
    ) {
        when (click) {
            0 -> {
                openViewPostBottomSheet(Post(asset_url = asset_url, caption = caption))
            }

            1 -> {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileFragmentToReelsFragment(
                        Payload(
                            pagination = Pagination(),
                            reels = posts.data.toReelList().toMutableList()
                        ), "profile", reelPosition.toString()
                    )
                )
            }
        }
    }

    fun List<Data>.toReelList(): List<Reel> {
        return this.map { data ->
            Reel(
                assetType = data.asset_type ?: "",
                assetUrl = data.asset_url ?: "",
                caption = data.caption ?: "",
                commentsCount = data.commentsCount ?: 0, // or map from somewhere else if available
                isLiked = data.isLiked ?: 0,       // default or map from somewhere
                isFollowing = 0,   // default or map from somewhere
                createdAt = data.created_at ?: "",
                creator = Creator(
                    name = data.creator_name ?: "",
                    profileImage = data.creator_profile_image.toString(),
                    username = data.creator_username ?: "",
                    city = data.userCity ?: "",      // default or fetch if available
                    country = data.userCountry ?: "",   // default or fetch if available
                    user_live_status = data.status.toString(),
                    isStoryUploaded = data.is_story_uploaded!!
                ),
                creatorId = data.creator_id ?: 0,
                id = data.id ?: 0,
                likesCount = data.likesCount ?: 0,    // default or fetch if available
                status = data.status ?: "",
                updatedAt = data.updated_at ?: "",
                lastPlaybackPosition = 0L
            )
        }
    }

    private fun openViewPostBottomSheet(post: Post) {

        val viewPostBottomSheetFragment = ViewPostBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable("post", post)
            }
        }

        viewPostBottomSheetFragment.show(requireActivity().supportFragmentManager, null)
    }

    private fun onClickFollowers() {
        binding.followers.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToFollowersFragment(
                    screenCheck = "followers",
                    isMyProfile = if ((userId == Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.userId.toString()) || userId.isEmpty()
                    ) "1" else "0",
                    userId = userId
                )
            )
        }
    }

    private fun onClickFollowing() {
        binding.following.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToFollowersFragment(
                    screenCheck = "following",
                    isMyProfile = if ((userId == Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.userId.toString()) || userId.isEmpty()
                    ) "1" else "0",
                    userId = userId
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProfileMediaViewPager(Posts(), Posts())
        UserDataManager.setUserInnerPostIsResume(requireContext(), false)
        if (isMyProfile == "0") {
//            (requireActivity() as HomeActivity).hideNavigationView()
            if (userId == Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA
                )?.payload?.userId.toString()
            ) {
//                UserPreference.navigatedToMyProfile=true
                onMyProfileOpened()
//                (requireActivity() as HomeActivity).showNavigationView()
            } else {
                val layoutParams =
                    binding.followersNumberLayout.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.topToBottom = binding.followMessageLayout.id
                binding.followersNumberLayout.layoutParams = layoutParams
                binding.followMessageLayout.isVisible = true
                binding.editShareLayout.isVisible = false
                binding.editProfileButton.isVisible = false
                binding.addReel.isVisible = false
                binding.walletButton.isVisible = false
                binding.backButton.isVisible = true
                binding.sideOptions.setImageResource(R.drawable.side_options_vertical)
                viewModel.hitOtherUserProfileDataApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, userId, "1", "14"
                )
                onClickFollowers()
                onClickFollowing()
            }
        } else {
            onMyProfileOpened()
        }
        setObserver()
    }

    private fun setOnlineStatus(onlineStatus: String) {
        when (onlineStatus) {
            "1" -> {
                binding.onlineStatusImage.setImageResource(R.drawable.online_status_green)
            }

            "2", "3" -> {
                binding.onlineStatusImage.setImageResource(R.drawable.offline_status_red)
            }
            /*"3"->{
                binding.onlineStatusImage.setImageResource(R.drawable.busy_status)
            }*/
        }
    }

    private fun onMyProfileOpened() {
        val layoutParams =
            binding.followersNumberLayout.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.topToBottom = binding.editShareLayout.id
        binding.followersNumberLayout.layoutParams = layoutParams

        binding.followMessageLayout.isVisible = false
        binding.editShareLayout.isVisible = true
        viewModel.hitProfileDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, "1", "14"
        )
        onClickFollowers()
        onClickFollowing()
        (requireActivity() as HomeActivity).selectProfileTabIcon()
    }

    private fun setCountData(postsCount: Int?, followersCount: Int?, followingCount: Int?) {
        binding.apply {
            post.text = "$postsCount \n Post"
            followers.text = "$followersCount \n Followers"
            following.text = "$followingCount \n Following"
        }
    }

    private fun setObserver() {
        viewModel.getProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            setCountData(
                                it.data.payload.userDetails.posts_count,
                                it.data.payload.userDetails.followers_count,
                                it.data.payload.userDetails.following_count
                            )
                            userDetails = it.data.payload.userDetails
                            ///Log.e(TAG, "setObserver: ", )
                            updateProfileDetails(it.data.payload.userDetails)
                            setProfileMediaViewPager(it.data.payload.reels, it.data.payload.posts)
                            setOnlineStatus(it.data.payload.userDetails.user_live_status ?: "2")

                        } else {
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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
        viewModel.getOtherUserProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            userDetails = it.data.payload.userDetails
                            updateProfileDetails(it.data.payload.userDetails)
                            setCountData(
                                it.data.payload.userDetails.posts_count,
                                it.data.payload.userDetails.followers_count,
                                it.data.payload.userDetails.following_count
                            )
                            setProfileMediaViewPager(it.data.payload.reels, it.data.payload.posts)
                            isFollowing = it.data.payload.userDetails.is_following == 1
                            if (it.data.payload.userDetails.is_following == 1) {
                                binding.followUserButton.text = "Unfollow"
                                RTVariable.USER_IS_FOLLOWING = true
                            } else {
                                binding.followUserButton.text = "Follow"
                                RTVariable.USER_IS_FOLLOWING = false
                            }
                            setOnlineStatus(it.data.payload.userDetails.user_live_status ?: "2")
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

        viewModel.getFollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "follow user success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                            viewModel.hitOtherUserProfileDataApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, userId, "", ""
                            )
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

        viewModel.getUnfollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "unfollow user success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                            viewModel.hitOtherUserProfileDataApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, userId, "", ""
                            )
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

        viewModel2.addStoryLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add story success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            RTVariable.IS_STORY_UPDATED_FROM_PROFILE = true
                            //hitServiceListApi(viewModel.currentPage, selectedGender)
                        } else {
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }

                Status.LOADING -> {
                    //if (currentPage==1) ProcessDialog.showDialog(requireContext(), true)
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    private fun updateProfileDetails(userDetails: UserDetailsX) {
        Log.e("TAG", "updateProfileDetails:ddd $userDetails")
        binding.apply {
            verifiedNameTick.isVisible = Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.isCreator == 1
            name.text = userDetails.name
            username.text = userDetails.username
            bioEditText.text = userDetails.about
            var list = profileDetailList
            list[0].title = userDetails.gender.toString()
            list[1].title = userDetails.city ?: ""
            list[2].title = userDetails.country ?: ""
            list[3].title = userDetails.interest_name.toString()
            binding.profileDetailsRecycler.adapter = ShowProfileDetailsAdapter(list)
            Glide.with(requireContext()).load(userDetails.profile_image)
                .placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder)
                .into(profileImage)
            profileImage.bringToFront()
            /*  if (userDetails.is_story_uploaded==1){
                  binding.myStoryGradient.background = resources.getDrawable(R.drawable.story_gradient_border, null)
              }else{
                  binding.myStoryGradient.background = resources.getDrawable(R.drawable.story_gray_border, null)
              }*/
        }
    }

    /*private fun setProfileMediaViewPager(reels: Posts, posts: Posts) {
        profileMediaViewPager = AdapterProfileMediaViewPager(reels, posts, ::getSelectedPost)
        binding.viewPager.adapter = profileMediaViewPager
        viewPagerCallback(reels, posts)
        setTabsClick()
    }*/
    private fun setProfileMediaViewPager(reels: Posts, posts: Posts) {
        profileMediaViewPager =
            AdapterProfileMediaViewPager(this, reels, posts, isMyProfile, userId)
        binding.viewPager.adapter = profileMediaViewPager
        viewPagerCallback()
        setTabsClick()
        lifecycleScope.launch {
            delay(300)
            if (UserPreference.navigatedToMyProfile) {
                UserPreference.navigatedToMyProfile = false
                binding.viewPager.currentItem = 1
            }
        }

    }

    private fun setTabsClick() {
        binding.apply {
            gallery.setOnClickListener {
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                }
            }
            openReel.setOnClickListener {
                if (viewPager.currentItem != 1) {
                    viewPager.currentItem = 1
                }
            }
            addReel.setOnClickListener {
                selectedBottomSheetType = "profile"
                openUploadBottomSheet()
                addReel.setBackgroundColor(Color.TRANSPARENT)

            }
            followUserButton.setOnClickListener {
                if (isFollowing) {
                    viewModel.hitUnfollowUserDataApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken, FollowRequest(unfollowId = userId)
                    )
                } else {
                    viewModel.hitFollowUserDataApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken, FollowRequest(following_id = userId)
                    )

                }
            }
        }
    }

    private fun openUploadBottomSheet() {
        ReusablePopup(
            context = requireContext(),
            anchorView = binding.addReel,
            onOption1Click = {
                if (Preferences.getCustomModelPreference<LoginResponse>(requireContext(),LOGIN_DATA)?.payload?.isCreator == 1) {
                    if (RTVariable.STORY_UPLOAD_LIMIT <= 0) {
                        Utils.showCustom_Snackbar(requireActivity().findViewById(android.R.id.content), "You can upload maximum 4 stories in 24 hours")
                    }else{
                        RTVariable.SELECT_OPTION = true
                        //checkGalleryPermissionAndPick2()
                        val bottomSheet = FilePickerBottomsheet()
                        bottomSheet.setOnMediaSelectedListener { uri, type, headline ->
                            // uri and type are already returned as strings, no Intent parsing needed
                            val mediaType = type          // "image" or "video"
                            val contentUri = Uri.parse(uri)
                            RTVariable.HEADLINE_CAPTION = headline
                            Handler(Looper.getMainLooper()).post {
                                // Ensure your fragment/activity is still attached if needed
                                val file = getCacheFileFromContentUri(contentUri)
                                val typeCode = if (mediaType == "video") "V" else "I"
                                file?.let { uploadImage(it, typeCode) }
                            }
                        }
                        bottomSheet.show(
                            parentFragmentManager,   // or childFragmentManager, depending on where you are
                            "FilePickerBottomSheet"
                        )
                    }
                } else {
                    Utils.showCustom_Snackbar(requireActivity().findViewById(android.R.id.content), "You are not a creator")
                }
            },
            onOption2Click = {
                if (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator == 1) {
                    //selectedBottomSheetType = "post"
//                        openUploadBottomSheet("post")
                    //RTVariable.SELECT_OPTION = false
                    //checkGalleryPermissionAndPick("I")
                    RTVariable.SELECT_OPTION = false
                    selectedBottomSheetType = "post"   // still needed for title/API logic

                    val bottomSheet = ImageFilePickerBottomsheet2()
                    bottomSheet.setOnMediaSelectedListener { uri, type, ratio ->
                        val resultUri = Uri.parse(uri)
                        if (resultUri.scheme == null || resultUri.path == null) {
                            Toast.makeText(requireContext(), "Invalid image", Toast.LENGTH_SHORT).show()
                            return@setOnMediaSelectedListener
                        }
                        UserPreference.seletedUri = resultUri
                        UserPreference.selectedMediaToUpload = selectedBottomSheetType
                        UserPreference.selectedCropRatio = ratio
                        UserPreference.selectedMediaType = "I"   // 🔥 ADD THIS LINE
                        Log.i("TAG", "postratio: ${UserPreference.selectedCropRatio}")
                        findNavController().navigate(R.id.action_profileFragment_to_addReelFragment)
                    }
                    bottomSheet.show(parentFragmentManager, "ImageFilePickerBottomSheet")
                }else{
                    Utils.showCustom_Snackbar(requireActivity().findViewById(android.R.id.content), "You are not a creator")
                }
            },
            onOption3Click = {
                if (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator == 1) {
                    RTVariable.SELECT_OPTION = false
                    selectedBottomSheetType = "reel"

                    val bottomSheet = VideoFilePickerBottomsheet()
                    bottomSheet.setOnVideoPickedListener { uri, _ ->
                        UserPreference.seletedUri = uri
                        UserPreference.selectedMediaToUpload = selectedBottomSheetType
                        UserPreference.selectedMediaType = "V"
                        UserPreference.selectedCropRatio = 1
                        findNavController().navigate(R.id.action_profileFragment_to_addReelFragment)
                    }
                    bottomSheet.show(parentFragmentManager, "VideoFilePickerBottomSheet")
                } else {
                    Utils.showCustom_Snackbar(requireActivity().findViewById(android.R.id.content), "You are not a creator")
                }
            },
            option1Text = "Upload Status",
            option2Text = "Upload Post",
            option3Text = "Upload Video",
            option4Text = "Cancel",
            option1ImageRes = R.drawable.btn_status_icon,
            option2ImageRes = R.drawable.profile_gallery_icon, // Add your own move to request icon
            option3ImageRes = R.drawable.icon_over_video,
            option4ImageRes = R.drawable.ic_cancel_red
        ).show()
//        bottomSheetFragment = UploadMediaBottomSheet.newInstance(check).apply {
//            onGallerySelected = {
//                dismiss()
//                when(it){
//                    0->{
//                        selectedBottomSheetType = "post"
////                        openUploadBottomSheet("post")
//                        checkGalleryPermissionAndPick("I")
//                    }
//                    1->{
//                        if(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator ==1){
//                            selectedBottomSheetType = "reel"
////                            openUploadBottomSheet("reel")
//                            checkGalleryPermissionAndPick("V")
//                        }else{
//                            Toast.makeText(requireContext(), "You are not a creator", Toast.LENGTH_SHORT).show()
//                        }
//
//                    }
//                }
//            }
//            onUploadTypeSelected = {
//                dismiss()
//                when(it){
//                    0->{
//                        checkGalleryPermissionAndPick("I")
//                    }
//                    1->{
//                        checkGalleryPermissionAndPick("V")
//                    }
//                }
//            }
//        }
//        bottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")
//        val popup = UploadMediaBottomSheet(requireContext(), check, binding.root).apply {
//            onGallerySelected = {
//                dismiss()
//                when (it) {
//                    0 -> {
//                        selectedBottomSheetType = "post"
//                        checkGalleryPermissionAndPick("I")
//                    }
//                    1 -> {
//                        if (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator == 1) {
//                            selectedBottomSheetType = "reel"
//                            checkGalleryPermissionAndPick("V")
//                        } else {
//                            Toast.makeText(requireContext(), "You are not a creator", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//            onUploadTypeSelected = {
//                dismiss()
//                when (it) {
//                    0 -> {
//                        checkGalleryPermissionAndPick("I")
//                    }
//                    1 -> {
//                        checkGalleryPermissionAndPick("V")
//                    }
//                }
//            }
//        }
//        popup.show()
    }

    /*    private fun checkGalleryPermissionAndPick(mediaType: String) {
            selectedMediaType = mediaType
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissions = arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
                requestMultiplePermissionsLauncher.launch(permissions)
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
            val mediaType = when (selectedMediaType) {
                "I" -> ActivityResultContracts.PickVisualMedia.ImageOnly
                "V" -> ActivityResultContracts.PickVisualMedia.VideoOnly
                else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
            }

            mediaPickerLauncher.launch(PickVisualMediaRequest(mediaType))
        }*/


    private val requestSinglePermissionLauncher2 = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchMediaPicker2()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun checkGalleryPermissionAndPick2() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchMediaPicker2()
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                launchMediaPicker2()
            } else {
                requestSinglePermissionLauncher2.launch(permission)
            }
        }
    }


    private fun launchMediaPicker2() {
        //pickMedia.launch("*/*")
        mediaPickerLauncher2.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { openPreview(it) }
        }

    private val previewResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val savedUriString = data?.getStringExtra("uri")
                val mediaType = data?.getStringExtra("type")
                val contentUri = Uri.parse(savedUriString)

                val file = getCacheFileFromContentUri(contentUri)
                if (file != null && file.exists()) {
                    val typeCode = if (mediaType == "video") "V" else "I"
                    uploadImage(file, typeCode)

                    // Optional: delete after successful upload
                    // file.delete()
                } else {
                    // handle error
                }
            }
        }

    private fun getCacheFileFromContentUri(contentUri: Uri): File? {
        return try {
            val cursor = requireContext().contentResolver.query(contentUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val dataColumn = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (dataColumn != -1) {
                        val filePath = it.getString(dataColumn)
                        return File(filePath)
                    }
                }
            }
            val cacheDir = requireContext().cacheDir
            // Fallback: copy to a temporary file (if DATA column not available)
            val tempFile = File(
                cacheDir,
                "temp_${System.currentTimeMillis()}.${
                    contentUri.lastPathSegment?.substringAfterLast(
                        '.'
                    ) ?: "file"
                }"
            )
            requireContext().contentResolver.openInputStream(contentUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyToHiHloFolder(uri: Uri, folderType: String, extension: String): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(folderType),
            "HiHlo"
        )
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "file_${System.currentTimeMillis()}.$extension")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun openPreview(uri: Uri) {
        val type = requireContext().contentResolver.getType(uri) ?: ""
        val isVideo = type.startsWith("video")
        val intent = Intent(requireActivity(), ImageVideoConverter::class.java).apply {
            putExtra("uri", uri.toString())
            putExtra("isVideo", isVideo)
        }
        previewResultLauncher.launch(intent)
    }

    private val mediaPickerLauncher2 = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
//            val mimeType = requireContext().contentResolver.getType(uri)
//            if (mimeType?.startsWith("video") == true) {
//                val durationInMillis = getVideoDuration(requireContext(), uri)
//                val durationInSeconds = durationInMillis / 1000
//
//                UserPreference.seletedUri = Uri.EMPTY
//                val intent = Intent(requireActivity(), TrimVideoActivity::class.java)
//                intent.putExtra("videoUrl",uri.toString())
//                intent.putExtra("from","home")
//                startActivityForResult(intent, REQUEST_CODE_CROP_VIDEO)
//            } else {
//                openCropActivity2(uri)
//            }
            openPreview(uri)
        } else {
            Toast.makeText(requireContext(), "No media selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCropActivity2(imageUri: Uri) {
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
        }
        val destinationUri = Uri.fromFile(
            File(
                requireActivity().cacheDir,
                "cropped_${System.currentTimeMillis()}.jpg"
            )
        )
        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .start(requireContext(), this)
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

    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        val clientConfig = com.amazonaws.ClientConfiguration()
        clientConfig.connectionTimeout = 120000 // 120 sec
        clientConfig.socketTimeout = 120000 // 120 sec
        clientConfig.maxErrorRetry = 5 // Retry in case of network issues
        return AmazonS3Client(credentials)
    }

    fun uploadImageToS3(
        context: Context,
        file: File,
        bucketName: String,
        objectKey: String,
        accessKey: String,
        secretKey: String,
        assetType: String
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
        ProcessDialog.showDialog(requireContext(), true)
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    ProcessDialog.dismissDialog(true)
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val mediaUrl = "$urlCdn$slash$objectKey"
                    println("Image URL: $mediaUrl")
                    val caption = RTVariable.HEADLINE_CAPTION
                    RTVariable.HEADLINE_CAPTION = ""
                    viewModel2.hitAddStoryDataApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken,
                        AddStoryRequest(assetUrl = mediaUrl, assetType = assetType, caption = caption)
                    )
                    //viewModel.hitAddStoryDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, AddStoryRequest(assetUrl = mediaUrl, assetType = assetType))
                } else if (state == TransferState.FAILED) {
                    println("Upload failed")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat() * 100).toInt()
                println("Progress: $percentDone%")
            }

            override fun onError(id: Int, ex: Exception) {
                ProcessDialog.dismissDialog(true)
                ex.printStackTrace()
            }
        })
    }

    private fun uploadImage(imageFile: File, assetType: String) {
        var s3Data = Preferences.getCustomModelPreference<LoginResponse>(
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

    private fun checkGalleryPermissionAndPick(mediaType: String) {
        selectedMediaType = mediaType

        // Clear any previous limited access selections for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clearPhotoPickerSelections()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Request only the permission we actually need
            val requiredPermissions = when (mediaType) {
                "I" -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                "V" -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                else -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            requestMultiplePermissionsLauncher.launch(requiredPermissions)
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                launchMediaPicker()
            } else {
                requestSinglePermissionLauncher.launch(permission)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun clearPhotoPickerSelections() {
        try {
            // This clears the previous selections in the photo picker
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // For Android 14+, you can use MediaStore.clearUserSelection() if available
                // MediaStore.clearUserSelection(requireContext().contentResolver)
            }
            // For Android 13, there's no direct API to clear selections
            // The picker will still show previous selections
        } catch (e: Exception) {
            Log.e("PhotoPicker", "Could not clear selections", e)
        }
    }

    private fun launchMediaPicker() {
        val mediaType = when (selectedMediaType) {
            "I" -> ActivityResultContracts.PickVisualMedia.ImageOnly
            "V" -> ActivityResultContracts.PickVisualMedia.VideoOnly
            else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        }

        // Configure picker to allow only single selection
        val request = PickVisualMediaRequest.Builder()
            .setMediaType(mediaType)
            .build()

        mediaPickerLauncher.launch(request)
    }

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasImagePermission = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
                val hasVideoPermission = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false

                // For Android 13+, if any permission was requested but denied,
                // it might still be limited access which works with media picker
                val wasImageRequested =
                    permissions.containsKey(Manifest.permission.READ_MEDIA_IMAGES)
                val wasVideoRequested =
                    permissions.containsKey(Manifest.permission.READ_MEDIA_VIDEO)

                val canProceed = when (selectedMediaType) {
                    "I" -> hasImagePermission || wasImageRequested
                    "V" -> hasVideoPermission || wasVideoRequested
                    else -> hasImagePermission || hasVideoPermission || wasImageRequested || wasVideoRequested
                }

                if (canProceed) {
                    launchMediaPicker()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Required permissions not granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val granted = permissions.values.all { it }
                if (granted) launchMediaPicker()
                else Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    private val mediaPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val mimeType = requireContext().contentResolver.getType(uri)
                UserPreference.selectedMediaType = selectedMediaType
                if (::bottomSheetFragment.isInitialized) {
                    bottomSheetFragment.dismiss()
                }
                if (mimeType?.startsWith("video") == true) {
//                UserPreference.seletedUri = uri
                    if (uri != null) {
                        val mimeType = requireContext().contentResolver.getType(uri)
                        Log.e("TAG", "mimeType $mimeType")
                        if (mimeType?.startsWith("video") == true) {
                            UserPreference.seletedUri = Uri.EMPTY
                            val intent = Intent(requireActivity(), TrimVideoActivity::class.java)
                            intent.putExtra("videoUrl", uri.toString())
                            startActivityForResult(intent, REQUEST_CODE_CROP_VIDEO)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No media selected", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    openCropActivity(uri)
                }
            } else {
                Toast.makeText(requireContext(), "No media selected", Toast.LENGTH_SHORT).show()
            }
        }

    private fun openCropActivity(imageUri: Uri) {
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(false)

            // Supply only the ratios you want (exclude "Original")
            setAspectRatioOptions(
                0, // default selection index
                AspectRatio("1:1", 1f, 1f),
                AspectRatio("9:16", 9f, 16f),
                AspectRatio("16:9", 16f, 9f)
            )
        }

        val destinationUri = Uri.fromFile(
            File(requireActivity().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .start(requireContext(), this)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (RTVariable.SELECT_OPTION) {
            RTVariable.SELECT_OPTION = false
            if (resultCode == RESULT_OK) {
                when (requestCode) {
                    REQUEST_CODE_CROP_VIDEO -> {
                        val file = File(UserPreference.seletedUri.path)
                        uploadImage(file, "V")
                    }

                    UCrop.REQUEST_CROP -> {
                        val resultUri = UCrop.getOutput(data!!)
                        Log.i("TAG", "onActivityResult: " + resultUri)
                        val file = MediaUtils.uriToFile(resultUri ?: Uri.EMPTY, requireActivity())
                        uploadImage(file, "I")
                    }
                }
            } else {
                Log.w("HomeFragment", " cropping was cancelled or failed with code: $resultCode")
            }
        } else {
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_CODE_CROP_VIDEO) {
                    UserPreference.selectedMediaToUpload = selectedBottomSheetType
                    findNavController().navigate(R.id.action_profileFragment_to_addReelFragment)

                } else if (requestCode == UCrop.REQUEST_CROP) {
                    val resultUri = UCrop.getOutput(data!!)
                    if (resultUri != null) {
                        // get cropped image info
                        val extras = data.extras
                        val width = extras?.getInt(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, -1) ?: -1
                        val height = extras?.getInt(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, -1) ?: -1

                        var selectedRatio = 0 // default (unknown)

                        if (width > 0 && height > 0) {
                            val ratio = width.toFloat() / height.toFloat()

                            selectedRatio = when {
                                isCloseTo(ratio, 1f) -> 2   // 1:1
                                isCloseTo(ratio, 9f / 16f) -> 1   // 9:16
                                isCloseTo(ratio, 16f / 9f) -> 3   // 16:9
                                else -> 0 // unknown
                            }
                        }

                        Log.d("ProfileFragment", "Cropped ratio int: $selectedRatio")

                        UserPreference.seletedUri = resultUri
                        UserPreference.selectedMediaToUpload = selectedBottomSheetType
                        UserPreference.selectedCropRatio = selectedRatio
                        Log.i("TAG", "postratio: ${UserPreference.selectedCropRatio}")

                        findNavController().navigate(R.id.action_profileFragment_to_addReelFragment)
                    }
                }
            } else {
                Log.w("ProfileFragment", "cropping was cancelled or failed with code: $resultCode")
            }
        }
    }

    private fun isCloseTo(value: Float, target: Float, tolerance: Float = 0.05f): Boolean {
        return kotlin.math.abs(value - target) <= tolerance
    }

    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    /* private val requestMultiplePermissionsLauncher = registerForActivityResult(
         ActivityResultContracts.RequestMultiplePermissions()
     ) { permissions ->
         val granted = permissions.values.all { it }
         if (granted) launchMediaPicker()
         else Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
             }*/


    private fun viewPagerCallback() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.openReel.setImageResource(R.drawable.reel_icon_unselected)
                        binding.gallery.setImageResource(R.drawable.profile_gallery_icon_selected)
                        binding.reelIndicator.visibility = View.GONE
                        binding.galleryIndicator.visibility = View.VISIBLE
                    }

                    1 -> {
                        binding.openReel.setImageResource(R.drawable.profile_reel_icon_selected)
                        binding.gallery.setImageResource(R.drawable.profile_gallery_icon)
                        binding.reelIndicator.visibility = View.VISIBLE
                        binding.galleryIndicator.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setShowProfileDetailsRecycler() {
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_profile
    }

    private fun onClick() {
        binding.profileImage.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToOpenImageFragment(
                    imageUrl = userDetails.profile_image.toString()
                )
            )
        }
        binding.shareButton.setOnClickListener {
            shareApp()
        }
        binding.walletButton.setOnClickListener {
            val intent = Intent(requireActivity(), RechargeCoinsActivity::class.java)
            startActivity(intent)
            // findNavController().navigate(R.id.walletHistoryFragment)
        }
        binding.backButton.setOnClickListener {
            if (from == "chat" || from == "secondStory" || from == "reels") {
                findNavController().popBackStack()
            } else {
                (requireActivity() as HomeActivity).selectBottomNavTab(R.id.home)
            }
        }
        binding.sideOptions.setOnClickListener {
            if (isMyProfile == "1") {
                val bundle = Bundle()
                bundle.putParcelable("userDetail", userDetails)
                Log.e("TAG", "onClick: $userDetails")
                findNavController().navigate(R.id.profileSettingFragment, bundle)
            } else {
                openSideOptionsPopup()
            }
        }
        binding.editProfileButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putParcelable("userDetail", userDetails)
            Log.e("TAG", "onClick: $userDetails")
            (requireActivity() as? HomeActivity)?.setOnlineStatusVisibility(true)
            findNavController().navigate(R.id.editProfileNewFragment, bundle)
        }
        binding.messageUserButton.setOnClickListener {
            val bundle = Bundle()
            userDetails.profileImage = userDetails.profile_image
            bundle.putParcelable("userDetail", userDetails)
            Log.e("TAG", "onClick: $userDetails")
            (requireActivity() as? HomeActivity)?.setOnlineStatusVisibility(true)
            findNavController().navigate(R.id.action_profileFragment_to_chatFragment, bundle)
        }


    }

    private fun shareApp() {
        val appLink =
            "https://play.google.com/store/apps/details?id=${requireContext().packageName}"

        val shareText = """
        Check out this amazing app!
        Download here: $appLink
    """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share App"))
    }

    private fun openSideOptionsPopup() {
        val inflater = LayoutInflater.from(requireContext())
        val binding = PopupChatSideOptionsBinding.inflate(inflater)

        binding.title1.text = "Block"
        binding.title2.text = "Report"
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // for outside touch to dismiss
            elevation = 20f
            showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        }
        binding.title1.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "block")  // Add your arguments here
                putString("userId", userId)  // Add your arguments here
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
                findNavController().popBackStack()
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
        }
        binding.title2.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "flag")  // Add your arguments here
                putString("userId", userId)  // Add your arguments here
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
                findNavController().popBackStack()
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "FlagBottomSheet")

        }
    }

}

interface PaginatingFragment {
    fun onParentScrolledToBottom()
}
