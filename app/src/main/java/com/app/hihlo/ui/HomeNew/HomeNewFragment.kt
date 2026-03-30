package com.app.hihlo.ui.HomeNew

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentHomeNewBinding
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.selectedGender
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter
import com.app.hihlo.ui.chat.bottom_sheet.SendCoinsBottomSheetFragment
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.adapter.AdapterUserPostList
import com.app.hihlo.ui.home.fragment.HomeFragmentDirections
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.home.view_model.UserPostListViewModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.profile.view_model.FollowersViewModel
import com.app.hihlo.ui.profile.view_model.GetProfileViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.ui.reels.bottom_sheet.CommentReelBottomSheet
import com.app.hihlo.ui.reels.view_model.ReelsViewModel
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.MyApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.common.ScrollDirectionListener
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.getValue

class HomeNewFragment : BaseFragment<FragmentHomeNewBinding>() {

    private var myStoryData: MyStory = MyStory()
    private var currentPage=1
    private var isMediaUploaded: Int = -1
    private val viewModel: HomeViewModel by viewModels()
    private val viewModel2: UserPostListViewModel by viewModels()
    //private val viewModel3: GetProfileViewModel by viewModels()
    private lateinit var postAdapter: PostsAdapter
    private var allStory: List<Story>? = null
    private var isRefreshedFromMenu=false
    private var isHomeDataLoaded = false
    var isCommentPosted = false
    private var isLoadMore = false
    lateinit var commentsBottomSheetFragment: CommentReelBottomSheet
    var postId = ""
    var positionToComment: Int=0
    var adapter: AdapterUserPostList?=null
    var post_position: Int = 0

    // ────────────────────────────────────────
    // New variables for scroll handling
    // ────────────────────────────────────────
    private var isHeaderVisible = true
    private var isLoadingMore = false
    private val viewModel3: ReelsViewModel by viewModels()
    private val viewModel4: ReelsViewModel by viewModels()
    var totalAvailableCoins: Int?=null

    private var pendingScrollPostId: String? = null
    private var pendingScrollPosition: Int = -1   // optional fallback

    override fun getLayoutId(): Int {return R.layout.fragment_home_new}

    override fun initView(savedInstanceState: Bundle?) {
        viewModel.hitGenderListApi()
        currentPage=1
        hitServiceListApi(currentPage, 0)
        setupScrollListener()  // ← Replaced setPagination with this
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setObserver()
        onClick()
        if(UserDataManager.get_postCommentShow(requireContext())){
            UserDataManager.postCommentIsShow(requireContext(), false)
            pendingScrollPostId = UserDataManager.get_postCommentPid(requireContext())
            pendingScrollPosition = UserDataManager.get_postCommentPosition(requireContext())
            binding.swipeRefresh.isRefreshing = false
            postId = pendingScrollPostId ?: ""
            currentPage = UserDataManager.get_postMainPage(requireContext())
            //retainCommentBoxData(requireContext(), postId, "1", "10")
            viewModel2.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10")
            viewModel.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                currentPage.toString(), "10", "0")
            Handler(Looper.getMainLooper()).postDelayed({
                scrollToRecyclerPosition(UserDataManager.get_postCommentPosition(requireContext()))
            }, 1000)
        }
        if(UserDataManager.get_postMainIsShow(requireContext())){
            binding.swipeRefresh.isRefreshing = false
            UserDataManager.postMainIsSetShow(requireContext(), false)
            currentPage = UserDataManager.get_postMainPage(requireContext())
            viewModel.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                currentPage.toString(), "10", "0")
            Handler(Looper.getMainLooper()).postDelayed({
                scrollToRecyclerPosition(UserDataManager.get_postMainPosition(requireContext()))
            }, 1000)
        }
        binding.swipeRefresh.setColorSchemeColors(Color.TRANSPARENT)
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(Color.TRANSPARENT)
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.isVisible = true
            refreshData()
        }
        requireActivity().supportFragmentManager.setFragmentResultListener("home_click", viewLifecycleOwner) { _, _ ->
            Log.i("TAG", "onViewCreated: homeIconTap")
            isRefreshedFromMenu = true
            allStory?.toMutableList()?.clear()
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
        setupScrollListener()  // ← Call here too if needed, but initView is fine
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    if(RTVariable.COMMENT_DELETED){
                        RTVariable.COMMENT_DELETED = false
                        hitServiceListApi(currentPage, 0)
                    }
                }
            }
        }
    }

    fun scrollToRecyclerPosition(position: Int) {
        binding.postListRecycler.post {
            val viewHolder = binding.postListRecycler
                .findViewHolderForAdapterPosition(position)
            if (viewHolder != null) {
                val itemView = viewHolder.itemView
                binding.nestedScrollView.post {
                    binding.nestedScrollView.smoothScrollTo(
                        0,
                        itemView.top + binding.postListRecycler.top
                    )
                }
            } else {
                binding.postListRecycler.scrollToPosition(position)
                binding.postListRecycler.post {
                    scrollToRecyclerPosition(position)
                }
            }
        }
    }

    fun retainCommentBoxData(context: Context, postId: String, page: String, limit: String) {
        Log.e("RETAIN", "RETAIN>>> "+postId+" "+page+" "+limit)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = Preferences
                    .getCustomModelPreference<LoginResponse>(context, LOGIN_DATA)
                    ?.payload?.authToken

                Log.e("RETAIN", "TOKEN>>> $token")

                val response = RetrofitBuilder.apiService.getPostComments(
                    token = "Bearer $token",
                    postId = postId,
                    page = page,
                    limit = limit
                )

                Log.e("RETAIN", "RESPONSE>>> ${Gson().toJson(response)}")

                if (response.status == 1 && response.code == 200) {
                    val payload = response.payload

                    delay(200)
                    openCommentsBottomSheet(payload)
                }

            } catch (e: Exception) {
                Log.e("RETAIN_ERROR", "ERROR>>> ${e.message}", e)
            }
        }
    }

    private fun refreshData() {
        Handler(Looper.getMainLooper()).postDelayed({
            allStory?.toMutableList()?.clear()
            currentPage = 1
            hitServiceListApi(currentPage, 0)
            binding.swipeRefresh.isRefreshing = false
        }, 1000)
    }

    private var isBottomBarVisible = true

    private var scrollListener: ScrollDirectionListener? = null
    private fun setupScrollListener() {
        binding.nestedScrollView.setOnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 10) {
                if (isHeaderVisible) {
                    //hideHeaderAndStories()
                }
            } else if (dy < -10) {
                if (!isHeaderVisible) {
                    //showHeaderAndStories()
                }
            }
            if (scrollY < 100 && !isHeaderVisible) {
                //showHeaderAndStories()
            }
            val contentView = binding.nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            val diff = (contentView.bottom - (v.height + scrollY))
            if (diff < 500 && diff > 0 && !isLoadingMore) {
                isLoadingMore = true
                currentPage++
                hitServiceListApi(currentPage, 0)
            }
        }
    }
    private fun hideHeaderAndStories() {
        isHeaderVisible = false

        // Smooth slide up + fade out
        binding.headerLayout.animate()
            .translationY(-binding.headerLayout.height.toFloat())
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction { binding.headerLayout.isVisible = false }
            .start()

        binding.storiesLayout.animate()
            .translationY(- (binding.storiesLayout.height + 24).toFloat())  // + extra margin if needed
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction { binding.storiesLayout.isVisible = false }
            .start()

        //scrollListener?.hideBottomElements()  // your bottom nav / bar
    }
    private fun showHeaderAndStories() {
        isHeaderVisible = true

        binding.headerLayout.isVisible = true
        binding.storiesLayout.isVisible = true

        // Slide down + fade in (slightly slower for polish)
        binding.headerLayout.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        binding.storiesLayout.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        //scrollListener?.showBottomElements()
    }

    private fun setupRecyclerView() {
        binding.postListRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            postAdapter = PostsAdapter(
                actionListener = object : PostsAdapter.PostActionListener {
                    override fun onPostAction(
                        post: Post,
                        action: PostsAdapter.PostClickAction,
                        position: Int,
                        view: View
                    ) {
                        when (action) {
                            PostsAdapter.PostClickAction.LIKE -> {
                                getSendLikeStatus(post.id.toString(), true, position)
                            }
                            PostsAdapter.PostClickAction.UNLIKE -> {
                                getSendLikeStatus(post.id.toString(), false, position)
                            }
                            PostsAdapter.PostClickAction.OPTIONS_MENU -> {
                                if (post.user_id.toString()==Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString()){
                                    profileOptions( view, post.id.toString(), post.user_id.toString())
                                }else{
                                    openSideOptionsPopup(view, post.id.toString())
                                }
                            }
                            PostsAdapter.PostClickAction.SHARE -> {
                                //Toast.makeText(requireContext(), "Share at position $position", Toast.LENGTH_SHORT).show()
                                sharePost(post.asset_url.toString())
                            }
                            PostsAdapter.PostClickAction.COMMENT -> {
                                postId = post.id.toString()
                                post_position = position
                                UserDataManager.postCommentSP(requireContext(), currentPage, post_position, postId.toString())
                                //Toast.makeText(requireContext(), "Comment at position $position", Toast.LENGTH_SHORT).show()
                                viewModel2.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, post.id.toString(), "1", "10")
                            }
                            PostsAdapter.PostClickAction.POST_BODY -> {
                                //Toast.makeText(requireContext(), "Open post at position $position", Toast.LENGTH_SHORT).show()
                            }
                            PostsAdapter.PostClickAction.POST_PROFILE -> {
                                post_position = position
                                //Toast.makeText(requireContext(), "Open post profile at position $position", Toast.LENGTH_SHORT).show()
                                UserDataManager.postMainSP(requireContext(), currentPage, post_position)
                                UserDataManager.postMainIsSetShow(requireContext(), true)
                                findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", post.user_id.toString()))
                            }
                            PostsAdapter.PostClickAction.POST_PROFILE_NAME -> {
                                post_position = position
                                //Toast.makeText(requireContext(), "Open post profile at position $position", Toast.LENGTH_SHORT).show()
                                UserDataManager.postMainSP(requireContext(), currentPage, post_position)
                                UserDataManager.postMainIsSetShow(requireContext(), true)
                                findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", post.user_id.toString()))
                            }
                            PostsAdapter.PostClickAction.POST_FOLLOW -> {
                                //Toast.makeText(requireContext(), "Open follow at position $position", Toast.LENGTH_SHORT).show()
                                //viewModel3.hitFollowUserDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, FollowRequest(following_id = post.user_id.toString()))
                                getSendFollow(post.user_id.toString(), position)
                            }
                            PostsAdapter.PostClickAction.POST_UNFOLLOW -> {
                                //Toast.makeText(requireContext(), "Open unfollow at position $position", Toast.LENGTH_SHORT).show()
                                //viewModel3.hitUnfollowUserDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, FollowRequest(following_id = post.user_id.toString()))
                                getSendUnFollow(post.user_id.toString(), position)
                            }
                            PostsAdapter.PostClickAction.GIFT -> {
                                post.user_id?.let { openCoinsBottomSheet(it, it, post.creatorDetail?.name.toString()) }
                            }
                            PostsAdapter.PostClickAction.TOWARDS_STORY -> {
                                val stories = postAdapter.getStoriesList()
                                val storyPosition = stories.indexOfFirst { it.user_id == post.user_id }
                                val story = postAdapter.getStoriesList().find { it.user_id == post.user_id }
//                                val my_story = postAdapter.getMyStoriesList().getOrNull(0)
//                                    ?: MyStory()
                                val currentUserId = Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(), LOGIN_DATA
                                )?.payload?.userId?.toString() ?: ""
                                //val isMyStoryValue = if (post.user_id.toString() == currentUserId) "1" else "0"
                                Log.e("TTTTT", "SSSSS>>> Story clicked: $story")
                                val bundle = Bundle().apply {
                                    putParcelableArrayList("storyList", ArrayList(allStory ?: emptyList()))
                                    putParcelable("myStoryData", myStoryData)
                                    putInt("position", storyPosition)
                                }
                                try {
                                    post_position = position
                                    //Toast.makeText(requireContext(), "Open post profile at position $position", Toast.LENGTH_SHORT).show()
                                    UserDataManager.postMainSP(requireContext(), currentPage, post_position)
                                    UserDataManager.postMainIsSetShow(requireContext(), true)
                                    findNavController().navigate(R.id.secondStoryFragment, bundle)
                                } catch (e: Exception) {
                                    Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                                    Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                onPostClick = null
            )
            adapter = postAdapter
        }
    }

    private fun openCoinsBottomSheet(reelId: Int, creatorId: Int, name: String) {
        val bottomSheetFragment = SendCoinsBottomSheetFragment(totalAvailableCoins).apply {
            onCoinsSelected = { data ->
                openSendCoinsDialog(data, reelId, creatorId, name)
                dismiss()
            }
        }
        bottomSheetFragment.show(requireActivity().supportFragmentManager, "")
    }
    fun openSendCoinsDialog(data: RechargePackageListResponse.Payload, reelId: Int, creatorId: Int, name: String) {
        showCustomDialogWithBinding(requireContext(), "Do you want to send ${data.coins} coins to ${name}",
            onYes = {
                viewModel4.hitSendGiftApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        MyApplication.appContext, LOGIN_DATA
                    )?.payload?.authToken,
                    SendGiftRequest(coins = data.coins.toString(), recipientId = creatorId.toString(), type = "reel", reelId = reelId.toString())
                )
            },
            onNo = {

            }
        )
    }

    private fun getSendLikeStatus(post_id: String, isLike: Boolean, position: Int){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.likePost(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    postId = post_id
                )
                if (response.status == 1 && response.code == 200) {
//                    Toast.makeText(
//                        requireContext(),
//                        if (isLike) "Liked successfully" else "Unliked",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    //postAdapter.notifyItemChanged(position)
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun getSendFollow(user_id: String, position: Int){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.followUser(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    FollowRequest(following_id = user_id.toString())

                )
                if (response.status == 1 && response.code == 200) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Follow successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    postAdapter.updateFollow(position, 1)
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun getSendUnFollow(user_id: String, position: Int){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.unfollowUser(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    FollowRequest(unfollowId = user_id)

                )
                if (response.status == 1 && response.code == 200) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Unfollow successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    postAdapter.updateFollow(position, 2)
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun sharePost(imageUrl: String) {
        lifecycleScope.launch {
            try {
                val imageFile = File(requireContext().cacheDir, "shared_image.jpg")
                withContext(Dispatchers.IO) {
                    val bitmap = Glide.with(requireContext())
                        .asBitmap()
                        .load(imageUrl) // load image URL directly
                        .submit()
                        .get()
                    val outputStream = FileOutputStream(imageFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    imageFile
                )
                val appLink = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri) // image file
                    putExtra(Intent.EXTRA_TEXT, "Check out this post!\n\nFind more on: $appLink")
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Post"))
            } catch (e: Exception) {
                e.printStackTrace()
                //Toast.makeText(requireContext(), "Image sharing failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onClick() {
        binding.notificationLayout.setOnClickListener {
            findNavController().navigate(R.id.action_homeNewFragment_to_notificationFragment)
        }
    }

    private fun hitServiceListApi(page: Int, genderId:Int?=null) {
        Log.e("TAG", "Home success: ${Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken}")
        viewModel.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, page.toString(), "10", if (genderId==null) "" else genderId.toString())
        viewModel4.hitCoinDetailsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
    }

    private fun profileOptions( view:View, postId: String, userId: String) {
        val popup = ReusablePopup(
            context = requireContext(),
            anchorView = view,
            onOption1Click = {
                openDeletePostConfirmationDialog(postId)
            },
            onOption2Click = {},
            option1Text = "Delete",
            option2Text = "Cancel",
            option1ImageRes = R.drawable.delete_icon,
            option2ImageRes = R.drawable.ic_cancel_red
        )
        popup.show()
    }

    fun openDeletePostConfirmationDialog(postId: String) {
        showCustomDialogWithBinding(requireContext(), "Are you sure you want to delete this post?",
            onYes = {
                viewModel2.hitDeletePostDataApi(token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString(), postId = postId)
            },
            onNo = {
                //dismiss()
            }
        )
    }

    private fun openSideOptionsPopup(view:View, userId: String) {
        val popup = ReusablePopup(
            context = requireContext(),
            anchorView = view,
            onOption1Click = {
                val bottomSheetFragment = BlockFlagBottomSheet()
                val bundle = Bundle().apply {
                    putString("screen", "block")
                    putString("userId", userId)
                }
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.onBlockSuccessful = {
                    bottomSheetFragment.dismiss()
                    findNavController().popBackStack()
                }
                bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
            },
            onOption2Click = {
                val bottomSheetFragment = BlockFlagBottomSheet()
                val bundle = Bundle().apply {
                    putString("screen", "flag")
                    putString("userId", userId)
                }
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.onBlockSuccessful = {
                    bottomSheetFragment.dismiss()
                    findNavController().popBackStack()
                }
                bottomSheetFragment.show(requireActivity().supportFragmentManager, "FlagBottomSheet")
            },
            option1Text = "Block",
            option2Text = "Report",
            option1ImageRes = R.drawable.ic_block_white, // Add your own move to request icon
            option2ImageRes = R.drawable.ic_flag_black
        )
        popup.show()
    }

    private fun setObserver() {
        if(!UserDataManager.get_postCommentShow(requireContext()) || !UserDataManager.get_postMainIsShow(requireContext())){
            binding.progressBar.isVisible=true
        }
        viewModel.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.progressBar.isVisible = false
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isRefreshedFromMenu=false
                            isHomeDataLoaded=true
                            isMediaUploaded = it.data.payload.is_story_uploaded
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            binding.storiesRecycler.adapter = AdapterStoriesRecycler( it.data.payload.is_story_uploaded, it.data.payload.my_story ?: MyStory(), it.data.payload.stories,  ::getSelectedStory, it.data.payload.myProfile.profileImage)
                            if (it.data.payload.posts.isNotEmpty()){
                                postAdapter.setPosts(it.data.payload.posts,
                                    listOf(it.data.payload.my_story ?: MyStory()), it.data.payload.stories)
                                if (currentPage == 1) {
                                    if (it.data.payload.posts.size > 0){
                                        postAdapter.clearList()
                                        postAdapter.addPosts(it.data.payload.posts.toMutableList(),
                                            listOf(it.data.payload.my_story ?: MyStory()), it.data.payload.stories)
                                    }else{
                                        postAdapter.clearList()
                                    }
                                } else {
                                    postAdapter.addPosts(it.data.payload.posts.toMutableList(),
                                        listOf(it.data.payload.my_story ?: MyStory()), it.data.payload.stories)
                                }
                            }
                            if(it.data.payload.unreadNotificationCount==0){
//                                binding.notificationButton.setImageResource(R.drawable.notification_bell)
                                binding.notificationDot.isVisible=false
                            }else{
//                                binding.notificationButton.setImageResource(R.drawable.notification_bell_with_dot)
                                binding.notificationDot.isVisible=true
                            }
                            isLoadingMore = false  // ← Reset after success
                        }else{
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            isLoadingMore = false  // ← Reset on error too
                        }
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                        isLoadingMore = false
                    }
                    binding.progressBar.isVisible=false
                }
                Status.LOADING -> {
                }
                Status.ERROR -> {
                    binding.progressBar.isVisible=false
                    isLoadingMore = false
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
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
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
        viewModel2.getLikeReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Post like success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }else{
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
//                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
//                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel2.getReelCommentsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel comments success: ${Gson().toJson(it)}")
                    if (it.data?.code == 200) {
                        val payload = it.data.payload ?: Payload()
                        if (isCommentPosted) {
                            isCommentPosted = false
                            if (::commentsBottomSheetFragment.isInitialized) {
                                commentsBottomSheetFragment.updateComments(payload)
                            }
                        } else if (isLoadMore) {
                            isLoadMore = false
                            if (::commentsBottomSheetFragment.isInitialized) {
                                commentsBottomSheetFragment.appendComments(payload.comments ?: emptyList())
                            }
                        } else {
                            openCommentsBottomSheet(payload)
                        }
//                        binding.postListRecycler.post {
//                            val viewHolder = binding.postListRecycler.findViewHolderForAdapterPosition(UserDataManager.get_postCommentPosition(requireContext()))
//                            val itemView = viewHolder?.itemView
//
//                            if (itemView != null) {
//                                val y = itemView.top
//
//                                binding.nestedScrollView.smoothScrollTo(0, y)
//                            }
//                        }
                    } else {
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel2.getPostCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel home post comment success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isCommentPosted = true
                            adapter?.updateCommentCount(positionToComment)
                            viewModel2.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10") // Initial call with page 1, limit 10
                            hitServiceListApi(currentPage, 0)
                        }else{
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel2.getReplyToCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel reply to comment success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isCommentPosted=true
                            viewModel2.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10") // Initial call with page 1, limit 10
                        }else{
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel2.getDeletePostLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete post success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            //findNavController().popBackStack()
                        }else{
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel3.getFollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "follow user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                        }else{
                        }
                    }else{
                    }
                }
                Status.LOADING -> {
                }
                Status.ERROR -> {
                }
            }
        }
        viewModel3.getUnfollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "follow user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                        }else{
                        }
                    }else{
                    }
                }
                Status.LOADING -> {
                }
                Status.ERROR -> {
                }
            }
        }
        viewModel4.getCoinDetailsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "coins details success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        totalAvailableCoins = it.data.payload.coins
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel4.getSendGiftLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "sent coins user success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        showCustomDialogWithBinding(requireContext(), "Send Successfully!",
                            onYes = {},
                            onNo = {},
                            showButtons = false,
                            autoDismissInMillis = 1000
                        )
//                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Safe cast – only HomeActivity implements it
        scrollListener = context as? ScrollDirectionListener
    }

    override fun onDetach() {
        super.onDetach()
        scrollListener = null
    }

    override fun onResume() {
        super.onResume()

        // Delay to ensure keyboard has fully closed
        view?.postDelayed({
            (requireActivity() as HomeActivity).fullyResetFloatingButton()
        }, 100)
    }

    override fun onPause() {
        super.onPause()
    }

    private fun openCommentsBottomSheet(payload: Payload) {
        commentsBottomSheetFragment = CommentReelBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable("comments", payload)
            }
            onCommentAction = { result ->
                isCommentPosted = true // Set flag before post
                viewModel2.hitPostCommentApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, result, postId)
            }
            onReplyAction = { result ->
                isCommentPosted = true // Assuming same flag for reply, adjust if separate
                viewModel2.hitReplyToCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, result, postId)
            }
            onLoadMore = { page, limit ->
                isLoadMore = true // Set flag before load more API call
                viewModel2.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, page.toString(), limit.toString())
            }
        }

        commentsBottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")
    }

    fun getSelectedStory(position:Int, story:Story, view:View){
        if(position==0){
            if(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator ==1){
                if (isMediaUploaded==1){
                    findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToStoryFragment(isMyStory = "1", myStoryData = myStoryData, otherStoryData = allStory?.toTypedArray() ?: emptyArray()))
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

    fun initializeS3Client(accessKey: String, secretKey: String): AmazonS3Client {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        val clientConfig = com.amazonaws.ClientConfiguration()
        clientConfig.connectionTimeout = 120000 // 120 sec
        clientConfig.socketTimeout = 120000 // 120 sec
        clientConfig.maxErrorRetry = 5 // Retry in case of network issues
        return AmazonS3Client(credentials)
    }
    fun uploadImageToS3(context: Context, file: File, bucketName: String, objectKey: String, accessKey: String, secretKey: String, assetType:String) {
        // Initialize S3 client
        val s3Client = initializeS3Client(accessKey, secretKey)
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()
        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(context)
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)
        ProcessDialog.showDialog(requireContext(), true)
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    ProcessDialog.dismissDialog(true)
                    val urlCdn = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.AWS_CDN_URL
                    val slash = "/"
                    val mediaUrl = "$urlCdn$slash$objectKey"
                    println("Image URL: $mediaUrl")
                    viewModel.hitAddStoryDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, AddStoryRequest(assetUrl = mediaUrl, assetType = assetType))
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
    private fun uploadImage(imageFile: File, assetType:String) {
        var s3Data = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.S3Details
        val bucketName = s3Data?.BUCKET_NAME
        val objectKey = "${System.currentTimeMillis()}"
        uploadImageToS3(requireContext(), imageFile, bucketName ?: "", objectKey, s3Data?.ACCESS_KEY ?: "", s3Data?.SECRET_KEY ?: "", assetType)
    }

}