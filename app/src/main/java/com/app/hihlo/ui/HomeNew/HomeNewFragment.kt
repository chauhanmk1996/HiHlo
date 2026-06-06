package com.app.hihlo.ui.HomeNew

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
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
import com.app.hihlo.model.story_response.StoryUser
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.selectedGender
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter
import com.app.hihlo.ui.HomeNew.adapter.StatusAdapter
import com.app.hihlo.ui.HomeNew.utility.FilePickerBottomsheet
import com.app.hihlo.ui.HomeNew.utility.ImageFilePickerBottomsheet
import com.app.hihlo.ui.HomeNew.utility.VideoFilePickerBottomsheet
import com.app.hihlo.ui.chat.bottom_sheet.SendCoinsBottomSheetFragment
import com.app.hihlo.ui.home.adapter.AdapterUserPostList
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.home.view_model.UserPostListViewModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.ui.reels.bottom_sheet.CommentReelBottomSheet
import com.app.hihlo.ui.reels.view_model.ReelsViewModel
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.HiHloApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.Utils
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
    private var isMediaUploaded: Int = -1
    private val viewModel: HomeViewModel by activityViewModels()
    private val viewModel2: UserPostListViewModel by viewModels()
    private lateinit var postAdapter: PostsAdapter
    private var allStory: List<Story>? = null
    var isCommentPosted = false
    private var isLoadMore = false
    lateinit var commentsBottomSheetFragment: CommentReelBottomSheet
    var postId = ""
    var positionToComment: Int = 0
    var adapter: AdapterUserPostList? = null
    var post_position: Int = 0
    private var isHeaderVisible = true
    private var isLoadingMore = false
    private val viewModel3: ReelsViewModel by viewModels()
    private val viewModel4: ReelsViewModel by viewModels()
    var totalAvailableCoins: Int? = null
    var FIRSTVisiblePosition = -1
    var offsetY = 0
    var isRestoringScroll = false
    private var selectedBottomSheetType = ""
    private val viewModel5: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StoryUser>
    private lateinit var statusAdapter: StatusAdapter

    override fun getLayoutId(): Int {
        return R.layout.fragment_home_new
    }

    override fun initView(savedInstanceState: Bundle?) {
        viewModel.hitGenderListApi()
//        binding.swipeRefresh.setColorSchemeColors(
//            ContextCompat.getColor(requireContext(), R.color.white)
//        )
//        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
//            ContextCompat.getColor(requireContext(), R.color.white_10)
//        )
        //binding.swipeRefresh.setSize(SwipeRefreshLayout.DEFAULT)
//        if (!viewModel.isHomeDataLoaded) {
//            if(!UserDataManager.isGetBackToHome(requireContext())){
//                Log.e("HIT", "HIT>>> IH")
//                binding.progressBar.isVisible = false
//                viewModel.currentPage = 1
//                binding.swipeRefresh.isRefreshing = true
//                viewModel.isRefreshing = false
//                hitServiceListApi(viewModel.currentPage, 0)
//            }
//        }
//        if (!viewModel.isHomeDataLoaded) {
//            viewModel.currentPage = 1
//            hitServiceListApi(viewModel.currentPage, 0)
//        } else {
//            // 🔥 restore UI from cache
//            allStory?.let { postAdapter.setPosts(viewModel.postsCache, listOf(myStoryData), it) }
//            binding.storiesRecycler.adapter = AdapterStoriesRecycler(
//                viewModel.isStoryUploaded,
//                viewModel.myStory ?: MyStory(),
//                viewModel.stories,
//                ::getSelectedStory,
//                viewModel.profileImage
//            )
//        }
        //setupScrollListener()  // ← Replaced setPagination with this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        RTVariable.bottom_page = 0
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setObserver()
        onClick()

        if (viewModel.isHomeDataLoaded) {
            UserDataManager.setGetBackToHome(requireContext(), false)
            postAdapter.setPosts(
                viewModel.postsCache,
                listOf(viewModel.myStory ?: MyStory()),
                viewModel.stories
            )
            binding.storiesLayout.visibility = View.VISIBLE

            statusAdapter = StatusAdapter(
                RTVariable.statusListGlobal.toMutableList(),   // initial list
                ::getSelectedTheStory                // click handler
            )
            binding.storiesRecycler.adapter = statusAdapter

            if (!UserDataManager.isGetBackToHome(requireContext())) {
                val scrollYp = UserDataManager.getHomeScrollYPosition(requireContext())
                binding.nestedScrollView.post {
                    binding.nestedScrollView.scrollTo(0, scrollYp)
                }
            } else {
                val scrollYp = UserDataManager.getHomeScrollYPosition(requireContext())
                binding.nestedScrollView.post {
                    binding.nestedScrollView.scrollTo(0, scrollYp)
                }
            }
        } else {
            Log.e("HIT", "HIT>>> Initial load (process death / fresh open)")
            viewModel.currentPage = 1
            viewModel.isRefreshing = false
            binding.swipeRefresh.isRefreshing = true          // shows loading indicator
            binding.progressBar.isVisible = false

            hitServiceListApi(viewModel.currentPage, 0)
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.post {
                binding.swipeRefresh.isRefreshing = true
            }
            binding.progressBar.isVisible = false
            refreshData()
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "home_click",
            viewLifecycleOwner
        ) { _, _ ->
            Log.i("TAG", "onViewCreated: homeIconTap")
//            isRefreshedFromMenu = true
//            allStory?.toMutableList()?.clear()
//            viewModel.currentPage = 1
//            binding.progressBar.isVisible=false
//            binding.swipeRefresh.setColorSchemeColors(
//                ContextCompat.getColor(requireContext(), R.color.white)
//            )
//            binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
//                ContextCompat.getColor(requireContext(), R.color.white_10)
//            )
//            binding.swipeRefresh.setSize(SwipeRefreshLayout.DEFAULT)
//
//            if(!UserDataManager.isGetBackToHome(requireContext())){
//                binding.swipeRefresh.isRefreshing = true
//                viewModel.isRefreshing = false
//            }

            //binding.swipeRefresh.isRefreshing = true
            if (binding.nestedScrollView.scrollY == 0) {
                if (!viewModel.isHomeDataLoaded) {
                    if (!UserDataManager.isGetBackToHome(requireContext())) {
                        Log.e("HIT", "HIT>>> IH")
                        binding.progressBar.isVisible = false
                        viewModel.currentPage = 1
                        viewModel.isRefreshing = false

                        binding.swipeRefresh.isRefreshing = true

                        hitServiceListApi(viewModel.currentPage, 0)
                    }
                } else if (RTVariable.ISHOMECLICKED) {
                    UserDataManager.setGetBackToHome(requireContext(), false)
                    RTVariable.ISHOMECLICKED = false
                    Log.e("HIT", "HIT>>> IHE")
                    binding.progressBar.isVisible = false
                    viewModel.currentPage = 1
                    viewModel.isRefreshing = false

                    binding.swipeRefresh.isRefreshing = true
                    hitServiceListApi(viewModel.currentPage, 0)
                }
            } else {
                if (binding.nestedScrollView.scrollY > 0) {
                    if (RTVariable.ISHOMECLICKED) {
                        UserDataManager.setGetBackToHome(requireContext(), false)
                        Log.e("HIT", "HIT>>> IHE")
                        binding.progressBar.isVisible = false
                        viewModel.currentPage = 1
                        viewModel.isRefreshing = false
                        binding.swipeRefresh.isRefreshing = true
                        binding.nestedScrollView.post {
                            binding.nestedScrollView.scrollTo(0, 0)
                            hitServiceListApi(viewModel.currentPage, 0)
                        }
                    }
                } else {
                    binding.nestedScrollView.smoothScrollTo(0, 0)
//                    binding.nestedScrollView.setOnScrollChangeListener(
//                        NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
//                            if (scrollY == 0) {
//                                //binding.nestedScrollView.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
//                                //hitServiceListApi(viewModel.currentPage, selectedGender)
//                            }
//                        }
//                    )
                }
            }
        }

        setupScrollListener()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    if (RTVariable.COMMENT_DELETED) {
                        RTVariable.COMMENT_DELETED = false
                        hitServiceListApi(viewModel.currentPage, 0)
                    }
                    if (RTVariable.ISHOMECLICKED) {
                        RTVariable.ISHOMECLICKED = false
                        binding.progressBar.isVisible = false
                        viewModel.currentPage = 1
                        viewModel.isRefreshing = false
                        binding.swipeRefresh.isRefreshing = true
                        hitServiceListApi(viewModel.currentPage, 0)
                    }
                    if (RTVariable.IS_MEDIA_UPLOADED) {
                        RTVariable.IS_MEDIA_UPLOADED = false
                        binding.progressBar.isVisible = false
                        viewModel.currentPage = 1
                        viewModel.isRefreshing = false
                        binding.swipeRefresh.isRefreshing = true
                        hitServiceListApi(viewModel.currentPage, 0)
                    }
                    if (RTVariable.IS_STATUS_DELETED) {
                        RTVariable.IS_STATUS_DELETED = false
                        viewModel5.hitStatusDataApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken, "0"
                        )
                    }
                    Log.e(
                        "RTVariable.IS_STATUS_VIEWER_ACTIVATED",
                        "RTVariable.IS_STATUS_VIEWER_ACTIVATED>>> " + RTVariable.IS_STATUS_VIEWER_ACTIVATED
                    )
                    if (RTVariable.IS_STATUS_VIEWER_ACTIVATED) {
                        RTVariable.IS_STATUS_VIEWER_ACTIVATED = false
                        //viewModel5.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
                        getRefreshStory(1, 0)
                        getRefreshMainStory(0)
                    }
                }
            }
        }
    }

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun scrollToRecyclerPosition(position: Int) {
        binding.postListRecycler.post {
            val layoutManager = binding.postListRecycler.layoutManager as? LinearLayoutManager
                ?: return@post
            layoutManager.scrollToPositionWithOffset(position, 0)
            binding.postListRecycler.post {
                val viewHolder = binding.postListRecycler.findViewHolderForAdapterPosition(position)
                val itemView = viewHolder?.itemView ?: return@post
                val y = itemView.y + binding.postListRecycler.y
                binding.nestedScrollView.scrollTo(0, viewModel.scrollY)
                UserDataManager.setGetBackToHome(requireContext(), false)
                binding.nestedScrollView.post {
                    binding.nestedScrollView.smoothScrollTo(0, viewModel.scrollY)
                    UserDataManager.setGetBackToHome(requireContext(), false)
                }
            }
        }
    }

    private fun refreshData() {
        Handler(Looper.getMainLooper()).postDelayed({
            allStory?.toMutableList()?.clear()
            viewModel.currentPage = 1
            viewModel.isRefreshing = true
            hitServiceListApi(viewModel.currentPage, 0)
        }, 1000)
    }

    private var scrollListener: ScrollDirectionListener? = null
    private fun setupScrollListener() {

        binding.nestedScrollView.setOnScrollChangeListener { v, _, scrollY, _, oldScrollY ->

            // 🔥 VERY IMPORTANT: block during restore
            if (isRestoringScroll) return@setOnScrollChangeListener

            val dy = scrollY - oldScrollY

            // 👉 Your header logic (unchanged)
            if (dy > 10) {
                if (isHeaderVisible) {
                    // hideHeaderAndStories()
                }
            } else if (dy < -10) {
                if (!isHeaderVisible) {
                    // showHeaderAndStories()  ///
                }
            }

            if (scrollY < 100 && !isHeaderVisible) {
                // showHeaderAndStories()
            }

            // 👉 Get RecyclerView position + offset
            val layoutManager =
                binding.postListRecycler.layoutManager as? LinearLayoutManager
                    ?: return@setOnScrollChangeListener

            val firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
            val firstVisible = if (firstCompletelyVisible != -1) {
                firstCompletelyVisible
            } else {
                layoutManager.findFirstVisibleItemPosition()
            }

            if (firstVisible != -1) {
                val child = layoutManager.findViewByPosition(firstVisible)
                val offset = child?.top ?: 0

                FIRSTVisiblePosition = firstVisible
                offsetY = offset
                RTVariable.SCROLL_POS = FIRSTVisiblePosition
                RTVariable.OFF_SET = offsetY
                UserDataManager.setHomeScrollPosition(requireContext(), FIRSTVisiblePosition)
                UserDataManager.setHomeScrollYPosition(requireContext(), offsetY)
            }
            Log.e(
                "SCROLL GOING",
                "Position=$FIRSTVisiblePosition | Offset=$offsetY | ScrollY=$scrollY"
            )
            val contentView = binding.nestedScrollView.getChildAt(0)
                ?: return@setOnScrollChangeListener
            val diff = (contentView.bottom - (v.height + scrollY))
            if (diff < 500 && diff > 0 && !isLoadingMore) {
                isLoadingMore = true
                viewModel.currentPage++
                hitServiceListApi(viewModel.currentPage, 0)
            }
        }
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
                        view: View,
                    ) {
                        when (action) {
                            PostsAdapter.PostClickAction.LIKE -> {
                                getSendLikeStatus(post.id.toString(), true, position)
                            }

                            PostsAdapter.PostClickAction.UNLIKE -> {
                                getSendLikeStatus(post.id.toString(), false, position)
                            }

                            PostsAdapter.PostClickAction.OPTIONS_MENU -> {
                                if (post.user_id.toString() == Preferences.getCustomModelPreference<LoginResponse>(
                                        requireContext(),
                                        LOGIN_DATA
                                    )?.payload?.userId.toString()
                                ) {
                                    profileOptions(
                                        view,
                                        post.id.toString(),
                                        post.user_id.toString()
                                    )
                                } else {
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
                                viewModel.posr_id = post.id.toString()
                                viewModel.scroll_position = position
                                RTVariable.P_PID = post.id.toString()
                                UserDataManager.postCommentExpandState(requireContext(), false)
                                UserDataManager.postCommentSP(
                                    requireContext(),
                                    viewModel.currentPage,
                                    post_position,
                                    postId
                                )
                                //Toast.makeText(requireContext(), "Comment at position $position", Toast.LENGTH_SHORT).show()
                                viewModel2.hitGetReelCommentsApi(
                                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                        requireContext(),
                                        LOGIN_DATA
                                    )?.payload?.authToken, post.id.toString(), "1", "10"
                                )
                            }

                            PostsAdapter.PostClickAction.POST_BODY -> {
                                //Toast.makeText(requireContext(), "Open post at position $position", Toast.LENGTH_SHORT).show()
                            }

                            PostsAdapter.PostClickAction.POST_PROFILE -> {
                                post_position = position
                                //Toast.makeText(requireContext(), "Open post profile at position $position", Toast.LENGTH_SHORT).show()
                                //UserDataManager.postMainSP(requireContext(), currentPage, post_position)
                                UserDataManager.postMainIsSetShow(requireContext(), true)
                                viewModel.scroll_position = position
                                findNavController().navigate(
                                    HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment(
                                        "0",
                                        post.user_id.toString()
                                    )
                                )
                            }

                            PostsAdapter.PostClickAction.POST_PROFILE_NAME -> {
                                post_position = position
                                viewModel.scroll_position = position
                                //Toast.makeText(requireContext(), "Open post profile at position $position", Toast.LENGTH_SHORT).show()
                                //UserDataManager.postMainSP(requireContext(), currentPage, post_position)
                                UserDataManager.postMainIsSetShow(requireContext(), true)
                                findNavController().navigate(
                                    HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment(
                                        "0",
                                        post.user_id.toString()
                                    )
                                )
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
                                post.user_id?.let {
                                    openCoinsBottomSheet(
                                        it,
                                        it,
                                        post.creatorDetail?.name.toString()
                                    )
                                }
                            }

                            PostsAdapter.PostClickAction.TOWARDS_STORY -> {
                                if (statusListGlobal.isEmpty()) {
                                    return
                                }
                                // clicked view ki position lo
                                val targetUserId = post.user_id.toString()
                                val location = IntArray(2)
                                view.getLocationOnScreen(location)
                                val centerX = location[0] + view.width / 2
                                val centerY = location[1] + view.height / 2
                                val intent = Intent(requireContext(), PlayStatusActivity::class.java)

                                // normal data
                                intent.putExtra("play_position", position)

                                val json = Gson().toJson(statusListGlobal)
                                intent.putExtra("story_list", json)
                                intent.putExtra("is_play_single", true)
                                intent.putExtra("user_id", targetUserId)

                                // instagram style animation data
                                intent.putExtra("start_x", centerX)
                                intent.putExtra("start_y", centerY)
                                intent.putExtra("start_width", view.width)
                                intent.putExtra("start_height", view.height)

                                startActivity(intent)
                                requireActivity().overridePendingTransition(0, 0)
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

    fun openSendCoinsDialog(
        data: RechargePackageListResponse.Payload,
        reelId: Int,
        creatorId: Int,
        name: String,
    ) {
        showCustomDialogWithBinding(
            requireContext(), "Do you want to send ${data.coins} coins to ${name}",
            onYes = {
                viewModel4.hitSendGiftApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        HiHloApplication.appContext, LOGIN_DATA
                    )?.payload?.authToken,
                    SendGiftRequest(
                        coins = data.coins.toString(),
                        recipientId = creatorId.toString(),
                        type = "reel",
                        reelId = reelId.toString()
                    )
                )
            },
            onNo = {

            }
        )
    }

    private fun getSendLikeStatus(post_id: String, isLike: Boolean, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.likePost(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
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
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun getSendFollow(user_id: String, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.followUser(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
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
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun getSendUnFollow(user_id: String, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.unfollowUser(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
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
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun getRefreshStory(page: Int, gender_id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getHomeData(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
                    page.toString(),
                    10.toString(),
                    gender_id.toString()
                )
                if (response.status == 1 && response.code == 200) {
                    viewModel.stories = response.payload.stories
                    postAdapter.updateStories(viewModel.stories)
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun getRefreshMainStory(gender_id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getStatusData(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
                    gender_id.toString()
                )
                if (response.status == 1 && response.code == 200) {
                    statusListGlobal = response.payload
                    RTVariable.statusListGlobal = statusListGlobal
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        if (!isAdded || _binding == null) return@launch
//                        binding.storiesRecycler.adapter =
//                            StatusAdapter(statusListGlobal.toMutableList(), ::getSelectedTheStory)
                        statusAdapter.updateStories(statusListGlobal)
                        binding.storiesLayout.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
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
                val appLink =
                    "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
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

    private fun hitServiceListApi(page: Int, genderId: Int? = null) {
        Log.e(
            "TAG",
            "Home success: ${
                Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA
                )?.payload?.authToken
            }"
        )
        viewModel.hitHomeDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, page.toString(), "10", "0"
        )
        viewModel4.hitCoinDetailsApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken
        )
        viewModel5.hitStatusDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, "0"
        )
    }

    private fun profileOptions(view: View, postId: String, userId: String) {
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
        showCustomDialogWithBinding(
            requireContext(), "Are you sure you want to delete this post?",
            onYes = {
                viewModel2.hitDeletePostDataApi(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken.toString(), postId = postId
                )
            },
            onNo = {
                //dismiss()
            }
        )
    }

    private fun openSideOptionsPopup(view: View, userId: String) {
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
                bottomSheetFragment.show(
                    requireActivity().supportFragmentManager,
                    "BlockBottomSheet"
                )
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
                bottomSheetFragment.show(
                    requireActivity().supportFragmentManager,
                    "FlagBottomSheet"
                )
            },
            option1Text = "Block",
            option2Text = "Report",
            option1ImageRes = R.drawable.ic_block_white, // Add your own move to request icon
            option2ImageRes = R.drawable.ic_flag_black
        )
        popup.show()
    }

    private fun setObserver() {
//        if(UserDataManager.get_postCommentShow(requireContext()) || UserDataManager.get_postMainIsShow(requireContext())){
//            binding.progressBar.isVisible=false
//        }else{
//            binding.progressBar.isVisible=true
//        }
        viewModel.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            viewModel.myStory = it.data.payload.my_story ?: MyStory()
                            viewModel.stories = it.data.payload.stories
                            isMediaUploaded = it.data.payload.is_story_uploaded
                            viewModel.isStoryUploaded = it.data.payload.is_story_uploaded
                            viewModel.profileImage = it.data.payload.myProfile.profileImage
                            if (viewModel.currentPage == 1) {
                                if (viewModel.isRefreshing) {
                                    viewModel.postsCache.clear()
                                    viewModel.isRefreshing = false
                                } else {
                                    viewModel.postsCache.clear()
                                    viewModel.isRefreshing = false
                                }
                                viewModel.postsCache.addAll(it.data.payload.posts)
                                postAdapter.setPosts(
                                    viewModel.postsCache,
                                    listOf(viewModel.myStory ?: MyStory()),
                                    viewModel.stories
                                )
                            } else {
                                viewModel.postsCache.addAll(it.data.payload.posts)
                                postAdapter.addPosts(
                                    it.data.payload.posts.toMutableList(),
                                    listOf(viewModel.myStory ?: MyStory()),
                                    viewModel.stories
                                )
                            }
//                            binding.storiesRecycler.adapter = AdapterStoriesRecycler(
//                                viewModel.isStoryUploaded,
//                                viewModel.myStory ?: MyStory(),
//                                viewModel.stories,
//                                ::getSelectedStory,
//                                viewModel.profileImage
//                            )
                            viewModel.isHomeDataLoaded = true
                            isLoadingMore = false
                            if (RTVariable.ISHOMECLICKED) {
                                RTVariable.ISHOMECLICKED = false
                                binding.nestedScrollView.smoothScrollTo(0, 0)
                            }
                        } else {
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            isLoadingMore = false  // ← Reset on error too
                        }
                    } else {
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                        isLoadingMore = false
                    }
                    binding.progressBar.isVisible = false
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    binding.progressBar.isVisible = false
                    isLoadingMore = false
                }
            }
        }
        viewModel.addStoryLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add story success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            val uploadedStoryCount = it.data.payload?.uploadedStoryCount
                            hitServiceListApi(viewModel.currentPage, selectedGender)
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
        viewModel2.getLikeReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Post like success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        } else {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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

                    if (it.data?.code == 200) {

                        val newPayload = it.data.payload ?: return@observe

                        when {

                            // ✅ COMMENT POSTED
                            isCommentPosted -> {

                                isCommentPosted = false

                                viewModel2.commentPayloadCache = newPayload

                                CommentPrefs.savePayload(
                                    requireContext(),
                                    postId.toInt(),
                                    newPayload
                                )

                                if (::commentsBottomSheetFragment.isInitialized &&
                                    commentsBottomSheetFragment.isAdded
                                ) {
                                    commentsBottomSheetFragment.updateComments(newPayload)
                                }
                            }

                            // ✅ LOAD MORE (FIXED 🔥)
                            isLoadMore -> {

                                isLoadMore = false

                                val oldPayload =
                                    viewModel2.commentPayloadCache
                                        ?: CommentPrefs.getPayload(requireContext(), postId.toInt())

                                if (oldPayload != null) {

                                    val (mergedList, newItemsOnly) =
                                        CommentPrefs.mergeComments(
                                            oldPayload.comments ?: emptyList(),
                                            newPayload.comments ?: emptyList()
                                        )

                                    val updatedPayload = oldPayload.copy(
                                        comments = mergedList
                                    )

                                    viewModel2.commentPayloadCache = updatedPayload

                                    // ✅ Save correct list
                                    CommentPrefs.savePayload(
                                        requireContext(),
                                        postId.toInt(),
                                        updatedPayload
                                    )

                                    if (::commentsBottomSheetFragment.isInitialized &&
                                        commentsBottomSheetFragment.isAdded
                                    ) {

                                        // 🔥 IMPORTANT: append ONLY new items (no duplicate)
                                        if (newItemsOnly.isNotEmpty()) {
                                            commentsBottomSheetFragment.appendComments(newItemsOnly)
                                        } else {
                                            Log.e("PAGINATION", "No new items to append")
                                        }
                                    }

                                } else {
                                    viewModel2.commentPayloadCache = newPayload
                                    CommentPrefs.savePayload(
                                        requireContext(),
                                        postId.toInt(),
                                        newPayload
                                    )
                                }
                            }

                            // ✅ FIRST LOAD
                            else -> {

                                viewModel2.commentPayloadCache = newPayload
                                CommentPrefs.clear(requireContext())
                                CommentPrefs.savePayload(
                                    requireContext(),
                                    postId.toInt(),
                                    newPayload
                                )

                                openCommentsBottomSheet(newPayload)
                            }
                        }
                    }
                }

                Status.LOADING -> {}

                Status.ERROR -> {
                    Log.e("TAG", "Error: ${it.message}")
                }
            }
        }
        viewModel2.getPostCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel home post comment success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            isCommentPosted = true
                            adapter?.updateCommentCount(positionToComment)
                            viewModel2.hitGetReelCommentsApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, postId, "1", "10"
                            ) // Initial call with page 1, limit 10
                            hitServiceListApi(viewModel.currentPage, 0)
                        } else {
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
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
        viewModel2.getReplyToCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel reply to comment success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            isCommentPosted = true
                            viewModel2.hitGetReelCommentsApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, postId, "1", "10"
                            ) // Initial call with page 1, limit 10
                        } else {
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
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
        viewModel2.getDeletePostLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete post success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            //findNavController().popBackStack()
                        } else {
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
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
        viewModel3.getFollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "follow user success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                        } else {
                        }
                    } else {
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
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                        } else {
                        }
                    } else {
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
                    if (it.data?.status == 1) {
                        totalAvailableCoins = it.data.payload.coins
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
        viewModel4.getSendGiftLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "sent coins user success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        showCustomDialogWithBinding(
                            requireContext(), "Send Successfully!",
                            onYes = {},
                            onNo = {},
                            showButtons = false,
                            autoDismissInMillis = 1000
                        )
//                        Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
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
        viewModel5.getStatusLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Status success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            statusListGlobal = it.data.payload
                            RTVariable.statusListGlobal = statusListGlobal
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(1000)
                                if (!isAdded || _binding == null) return@launch
                                // Where you first set the adapter (e.g., in setObserver or onViewCreated)
                                statusAdapter = StatusAdapter(
                                    statusListGlobal.toMutableList(),   // initial list
                                    ::getSelectedTheStory                // click handler
                                )
                                binding.storiesRecycler.adapter = statusAdapter
                                binding.storiesLayout.visibility = View.VISIBLE
                            }
                        } else {
                        }
                    } else {
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
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

    override fun onPause() {
        super.onPause()
        viewModel.scrollY = binding.nestedScrollView.scrollY
        viewModel.isHomeDataLoaded = true
        UserDataManager.setHomeScrollPosition(requireContext(), RTVariable.SCROLL_POS)
        UserDataManager.setHomeScrollYPosition(requireContext(), RTVariable.OFF_SET)
    }

    override fun onResume() {
        super.onResume()
        if (UserDataManager.isGetBackToHome(requireContext())) {
            val position = UserDataManager.getHomeScrollPosition(requireContext())
            scrollToRecyclerPosition(position)
        } else {
            scrollToRecyclerPosition(viewModel.scroll_position)
        }
        if (UserDataManager.get_postCommentShow(requireContext())) {
            binding.swipeRefresh.isRefreshing = false
            UserDataManager.postCommentIsShow(requireContext(), false)
            //openCommentsBottomSheet(viewModel2.commentPayloadCache ?: Payload())
            //retainCommentBoxData(requireContext(), viewModel.posr_id, "1", "10")
            val cached = CommentPrefs.get2Payload(requireContext())

            if (cached != null) {
                viewModel2.commentPayloadCache = cached
                RTVariable.IS_FROM_RESUME = true
                openCommentsBottomSheet(cached)
            }
        }
        if (UserDataManager.get_postMainIsShow(requireContext())) {
            binding.swipeRefresh.isRefreshing = false
            UserDataManager.postMainIsSetShow(requireContext(), false)
        }
        if (RTVariable.IS_STORY_UPDATED_FROM_PROFILE) {
            refreshData()
        }
    }

    private fun openCommentsBottomSheet(payload: Payload) {
        commentsBottomSheetFragment = CommentReelBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable("comments", payload)
                putParcelableArrayList("stories", ArrayList(viewModel.stories))
                putParcelable("myStory", viewModel.myStory)
            }
            onCommentAction = { result ->
                isCommentPosted = true // Set flag before post
                viewModel2.hitPostCommentApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, result, postId
                )
            }
            onReplyAction = { result ->
                isCommentPosted = true // Assuming same flag for reply, adjust if separate
                viewModel2.hitReplyToCommentsApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, result, postId
                )
            }
            onLoadMore = { page, limit ->
                isLoadMore = true // Set flag before load more API call
                viewModel2.hitGetReelCommentsApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, postId, page.toString(), limit.toString()
                )
            }
        }
        commentsBottomSheetFragment.show(
            requireActivity().supportFragmentManager,
            "RoundedBottomSheet"
        )
    }

    fun getSelectedTheStory(option: Int, value: StoryUser, position: Int, itemView: View) {
        if (option == 2) {
            val imageView: View = if (position == 0) {
                itemView.findViewById<View>(R.id.myStoryImageView)
            } else {
                itemView.findViewById<View>(R.id.otherStoryImageview)
            }
            val location = IntArray(2)
            imageView.getLocationOnScreen(location)

            val centerX = location[0] + imageView.width / 2
            val centerY = location[1] + imageView.height / 2

            val intent = Intent(requireContext(), PlayStatusActivity::class.java).apply {
                putExtra("play_position", position)
                putExtra("story_list", Gson().toJson(RTVariable.statusListGlobal))
                putExtra("is_play_single", false)
                putExtra("user_id", "")

                putExtra("start_x", centerX)
                putExtra("start_y", centerY)
                putExtra("start_width", imageView.width)
                putExtra("start_height", imageView.height)
            }
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_up, 0)
        }
        if (option == 3) {
            ReusablePopup(
                context = requireContext(),
                anchorView = itemView,
                onOption1Click = {
                    if (Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.isCreator == 1
                    ) {
                        viewLifecycleOwner.lifecycleScope.launch {

                            val canUploadStory = isStoryLimitReached()

                            if (canUploadStory) {
                                RTVariable.SELECT_OPTION = true
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

                            } else {

                                Utils.showCustom_Snackbar(
                                    requireActivity().findViewById(android.R.id.content),
                                    "You can upload 4 stories in 24 hours"
                                )
                            }
                        }
//                        if (RTVariable.STORY_UPLOAD_LIMIT <= 0) {
//                            Utils.showCustom_Snackbar(requireActivity().findViewById(android.R.id.content), "You can upload maximum 4 stories in 24 hours")
//                        }else{
//
//                        }
                    } else {
                        Utils.showCustom_Snackbar(
                            requireActivity().findViewById(android.R.id.content),
                            "You are not a creator"
                        )
                        //Toast.makeText(requireContext(), "You are not a creator", Toast.LENGTH_SHORT).show()
                    }
                },
                onOption2Click = {
                    if (Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.isCreator == 1
                    ) {
                        RTVariable.SELECT_OPTION = false
                        selectedBottomSheetType = "Image"   // still needed for title/API logic

                        val bottomSheet = ImageFilePickerBottomsheet()
                        bottomSheet.setOnMediaSelectedListener { uri, type, ratio ->
                            val resultUri = Uri.parse(uri)
                            if (resultUri.scheme == null || resultUri.path == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Invalid image",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnMediaSelectedListener
                            }
                            UserPreference.seletedUri = resultUri
                            UserPreference.selectedMediaToUpload = selectedBottomSheetType
                            UserPreference.selectedCropRatio = ratio
                            UserPreference.selectedMediaType = "I"
                            findNavController().navigate(R.id.addReelFragment)
                        }
                        bottomSheet.show(parentFragmentManager, "ImageFilePickerBottomSheet")
                    } else {
                        Utils.showCustom_Snackbar(
                            requireActivity().findViewById(android.R.id.content),
                            "You are not a creator"
                        )
                    }
                },
                onOption3Click = {
                    if (Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.isCreator == 1
                    ) {
                        RTVariable.SELECT_OPTION = false
                        selectedBottomSheetType = "reel"
                        val bottomSheet = VideoFilePickerBottomsheet()
                        bottomSheet.setOnVideoPickedListener { uri, _ ->
                            UserPreference.seletedUri = uri
                            UserPreference.selectedMediaToUpload = selectedBottomSheetType
                            UserPreference.selectedMediaType = "V"
                            UserPreference.selectedCropRatio = 1
                            findNavController().navigate(R.id.addReelFragment)
                        }
                        bottomSheet.show(parentFragmentManager, "VideoFilePickerBottomSheet")
                    } else {
                        Utils.showCustom_Snackbar(
                            requireActivity().findViewById(android.R.id.content),
                            "You are not a creator"
                        )
                    }
                },

                option1Text = "Upload Status",
                option2Text = "Upload Post",
                option3Text = "Upload Video",

                option1ImageRes = R.drawable.btn_status_icon, // Add your own move to request icon
                option2ImageRes = R.drawable.profile_gallery_icon, // Add your own move to request icon
                option3ImageRes = R.drawable.icon_over_video,

                ).show()
        }
    }

    private suspend fun isStoryLimitReached(): Boolean {
        return try {

            val response = RetrofitBuilder.apiService.getStoryUploadStatus(
                token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA
                )?.payload?.authToken
            )

            if (response.status == 1 && response.code == 200) {

                val remainingStories = response.payload?.remainingStories ?: 0

                RTVariable.STORY_UPLOAD_LIMIT = remainingStories

                Log.e("STORY LIMIT", "STORY LIMIT >>> $remainingStories")

                // true when user can upload more stories
                remainingStories > 0

            } else {
                false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            false
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
                    findNavController().navigate(R.id.action_homeNewFragment_to_addReelFragment)

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

                        findNavController().navigate(R.id.action_homeNewFragment_to_addReelFragment)
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
                    viewModel.hitAddStoryDataApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken,
                        AddStoryRequest(
                            assetUrl = mediaUrl,
                            assetType = assetType,
                            caption = caption
                        )
                    )
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

}