package com.app.hihlo.ui.home.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentUserPostListBinding
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.get_profile.Data
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.story_response.StoryUser
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.chat.bottom_sheet.SendCoinsBottomSheetFragment
import com.app.hihlo.ui.home.adapter.AdapterUserPostList
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.home.view_model.UserPostListViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.ui.reels.bottom_sheet.CommentReelBottomSheet
import com.app.hihlo.ui.reels.view_model.ReelsViewModel
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.HiHloApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.getValue

class UserPostListFragment : BaseFragment<FragmentUserPostListBinding>() {
    private val viewModel: UserPostListViewModel by viewModels()
    private val viewModel2: UserPostListViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    var isCommentPosted = false
    lateinit var commentsBottomSheetFragment: CommentReelBottomSheet
    val args: UserPostListFragmentArgs by navArgs()
    var homePosts: MutableList<Post> = mutableListOf()
    var profilePosts: Posts = Posts()
    var from: String = ""
    var position: Int = 0
    var positionToComment: Int = 0
    var postId = ""
    var adapter: AdapterUserPostList? = null
    private val viewModel4: ReelsViewModel by viewModels()
    private val viewModel5: HomeViewModel by viewModels()
    var totalAvailableCoins: Int? = null
    private var isLoadMore = false
    private var myStoryData: MyStory = MyStory()
    private var allStory: List<Story>? = null
    private var currentVisiblePosition = 0
    private val viewModel6: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StoryUser>
    private var coverPostPosition: Int = 0
    private var coverPostId: Int = 0
    private var coverPostIsCover: String = ""

    override fun initView(savedInstanceState: Bundle?) {
        homePosts = args.homePosts.toMutableList()
        from = args.from
        position = args.position.toInt()
        profilePosts = args.profilePosts
        setAdapter()
        setUI()
    }

    private fun setUI() {
        when (from) {
            "home" -> {
                binding.headerLayout.isVisible = false
            }

            else -> {
                binding.headerLayout.isVisible = true
            }
        }
    }

    private fun setAdapter() {
        adapter = AdapterUserPostList(homePosts, profilePosts, from, ::getSelectedPost)
        binding.postListRecycler.adapter = adapter
        lifecycleScope.launch {
            if (!UserDataManager.isUserInnerPostIsResume(requireContext())) {
                binding.postListRecycler.scrollToPosition(position)
                delay(1300)
                binding.postListRecycler.scrollToPosition(position)
                delay(700)
                binding.postListRecycler.scrollToPosition(position)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        RTVariable.bottom_page = 3
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        onClick()
        hitServiceListApi()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    if (RTVariable.COMMENT_DELETED) {
                        RTVariable.COMMENT_DELETED = false
                        adapter?.update_comment_count(
                            RTVariable.COMMENT_COUNT,
                            RTVariable.POST_POSITION
                        )
                    }

                    if (RTVariable.IS_STATUS_VIEWER_ACTIVATED) {
                        RTVariable.IS_STATUS_VIEWER_ACTIVATED = false
                        getRefreshStory(1, 0)
                        getRefreshMainStory(0)
                    }
                }
            }
        }

        binding.postListRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                    val firstCompletelyVisible =
                        layoutManager.findFirstCompletelyVisibleItemPosition()

                    val fallback = layoutManager.findFirstVisibleItemPosition()

                    currentVisiblePosition =
                        if (firstCompletelyVisible != RecyclerView.NO_POSITION)
                            firstCompletelyVisible
                        else
                            fallback
                    UserDataManager.postCommentSPR(requireContext(), currentVisiblePosition)
                    Log.d("SCROLL", "Saved Position = $currentVisiblePosition")
                }
            }
        })
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
                    viewModel5.stories = response.payload.stories
                    adapter?.updateStories(viewModel5.stories)
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

    override fun onPause() {
        val layoutManager = binding.postListRecycler.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        Log.d("SCROLL", "First: $firstVisible  Last: $lastVisible")
        UserDataManager.setUserInnerPostIsResume(requireContext(), true)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (UserDataManager.isUserInnerPostIsResume(requireContext())) {
            UserDataManager.setUserInnerPostIsResume(requireContext(), false)
            scrollToRecyclerPosition(UserDataManager.get_postCommentPosition(requireContext()))
        }
        if (!RTVariable.IS_PROFILE_POST_LIST) {
            RTVariable.IS_PROFILE_POST_LIST = false
            if (UserDataManager.get_postCommentShow(requireContext())) {
                UserDataManager.postCommentIsShow(requireContext(), false)
                val cached = CommentPrefs.get2Payload(requireContext())

                if (cached != null) {
                    viewModel2.commentPayloadCache = cached
                    RTVariable.IS_FROM_RESUME = true
                    openCommentsBottomSheet(cached)
                }
            }
        }
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
                UserDataManager.setGetBackToHome(requireContext(), false)
            }
        }
    }

    private fun hitServiceListApi() {
        viewModel5.hitHomeDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, "1", "10", "0"
        )
        viewModel4.hitCoinDetailsApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken
        )
        viewModel6.hitStatusDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, "0"
        )
    }

    private fun onClick() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setObserver() {
        viewModel5.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            viewModel5.myStory = it.data.payload.my_story ?: MyStory()
                            viewModel5.stories = it.data.payload.stories
                            viewModel5.isStoryUploaded = it.data.payload.is_story_uploaded
                            viewModel5.profileImage = it.data.payload.myProfile.profileImage
                            Log.e("TAG", "Home success: ${myStoryData}")
                            Log.e("TAG", "Home success: ${allStory}")
                            adapter?.addStory(
                                listOf(it.data.payload.my_story ?: MyStory()),
                                it.data.payload.stories
                            )
                        }
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                }
            }
        }

        viewModel.getLikeReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Post like success: ${Gson().toJson(it)}")
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                }
            }
        }

        viewModel.getPostCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel post comment success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            isCommentPosted = true
                            adapter?.updateCommentCount(positionToComment)
                            viewModel.hitGetReelCommentsApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, postId, "1", "10"
                            ) // Initial call with page 1, limit 10
                        }
                    }
                    ProcessDialog.dismissDialog(true)
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                }
            }
        }

        viewModel.getReplyToCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel reply to comment success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            isCommentPosted = true
                            viewModel.hitGetReelCommentsApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, postId, "1", "10"
                            ) // Initial call with page 1, limit 10
                        }
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                }
            }
        }

        viewModel.getReelCommentsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    if (it.data?.code == 200) {
                        val newPayload = it.data.payload ?: return@observe
                        when {
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

                            isLoadMore -> {
                                isLoadMore = false
                                val oldPayload =
                                    viewModel2.commentPayloadCache
                                        ?: CommentPrefs.getPayload(requireContext(), postId.toInt())

                                if (oldPayload != null) {
                                    val (mergedList, newItemsOnly) =
                                        CommentPrefs.mergeComments(
                                            oldPayload.comments,
                                            newPayload.comments
                                        )
                                    val updatedPayload = oldPayload.copy(
                                        comments = mergedList
                                    )

                                    viewModel2.commentPayloadCache = updatedPayload
                                    CommentPrefs.savePayload(
                                        requireContext(),
                                        postId.toInt(),
                                        updatedPayload
                                    )

                                    if (::commentsBottomSheetFragment.isInitialized &&
                                        commentsBottomSheetFragment.isAdded
                                    ) {
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

        viewModel.getDeletePostLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete post success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            findNavController().popBackStack()
                        }
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                }
            }
        }

        viewModel4.getCoinDetailsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "coins details success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        totalAvailableCoins = it.data.payload.coins
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
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
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                }
            }
        }

        viewModel6.getStatusLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Status success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            statusListGlobal = it.data.payload
                            RTVariable.statusListGlobal = statusListGlobal
                            Log.e("TAG", "Status success: ${statusListGlobal}")
                        }
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                }
            }
        }

        homeViewModel.setRemoveCoverApi().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            val loginUserId = Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.userId.toString()

                            // Remove previous cover
                            val oldCoverIndex = profilePosts.data.indexOfFirst { post ->
                                post.user_id.toString() == loginUserId && post.is_cover == "TRUE"
                            }

                            if (oldCoverIndex != -1) {
                                profilePosts.data[oldCoverIndex].is_cover = "FALSE"
                                adapter?.updateCover(oldCoverIndex, "FALSE")
                            }

                            // Set new cover
                            profilePosts.data[coverPostPosition].is_cover = coverPostIsCover
                            adapter?.updateCover(coverPostPosition, coverPostIsCover)
                        }
                    }
                }

                Status.LOADING -> {
                }

                Status.ERROR -> {
                }
            }
        }
    }

    private fun openCommentsBottomSheet(payload: Payload) {
        commentsBottomSheetFragment = CommentReelBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable("comments", payload)
                putParcelableArrayList("stories", ArrayList(viewModel5.stories))
                putParcelable("myStory", viewModel5.myStory)
            }
            onCommentAction = { result ->
                isCommentPosted = true // Set flag before post
                viewModel.hitPostCommentApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, result, postId
                )
            }
            onReplyAction = { result ->
                isCommentPosted = true // Assuming same flag for reply, adjust if separate
                viewModel.hitReplyToCommentsApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, result, postId
                )
            }
            onLoadMore = { page, limit ->
                isLoadMore = true // Set flag before load more API call
                viewModel.hitGetReelCommentsApi(
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

    override fun getLayoutId(): Int {
        return R.layout.fragment_user_post_list
    }

    private fun getSelectedPost(
        post: Post,
        data: Data,
        click: Int,
        position: Int,
        view: View,
        clickView: View,
    ) {
        positionToComment = position

        when (click) {
            0 -> {
                openSideOptionsPopup(position, data, view)
            }

            1 -> {
                when (from) {
                    "home" -> {
                        findNavController().navigate(
                            UserPostListFragmentDirections.actionUserPostListFragmentToProfileFragment(
                                "0",
                                post.user_id.toString()
                            )
                        )
                    }

                    else -> {
                        findNavController().popBackStack()

                    }
                }
            }

            2 -> {
                when (from) {
                    "home" -> {
                        viewModel.hitLikeReelApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken, post.id.toString()
                        )
                    }

                    else -> {
                        viewModel.hitLikeReelApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken, data.id.toString()
                        )
                    }
                }
            }

            3 -> {
                when (from) {
                    "home" -> {
                        this.postId = post.id.toString()
                        viewModel.hitGetReelCommentsApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken, postId, "1", "10"
                        )
                    }

                    else -> {
                        this.postId = data.id.toString()
                        viewModel5.posr_id = post.id.toString()
                        viewModel5.scroll_position = position
                        RTVariable.P_PID = post.id.toString()
                        UserDataManager.postCommentExpandState(requireContext(), false)
                        UserDataManager.postCommentSP(
                            requireContext(),
                            viewModel5.currentPage,
                            position,
                            postId
                        )
                        viewModel.hitGetReelCommentsApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken, postId, "1", "10"
                        )
                    }
                }
            }

            4 -> {
                when (from) {
                    "home" -> {
                        sharePost(post.asset_url.toString())
                    }

                    else -> {
                        sharePost(data.asset_url.toString())
                    }
                }
            }

            5 -> {
                //Toast.makeText(requireActivity(), "A ${data.user_id}", Toast.LENGTH_LONG).show()
                getSendFollow(data.user_id.toString(), position)
            }

            6 -> {
                //Toast.makeText(requireActivity(), "B ${data.user_id}", Toast.LENGTH_LONG).show()
                getSendUnFollow(data.user_id.toString(), position)
            }

            7 -> {
                //Toast.makeText(requireActivity(), "B ${data.id}", Toast.LENGTH_LONG).show()
                data.user_id?.let { openCoinsBottomSheet(it, it, data.creator_username.toString()) }
            }

            8 -> {
                if (statusListGlobal.isEmpty()) {
                    return
                }
                val location = IntArray(2)
                clickView.getLocationOnScreen(location)
                val centerX = location[0] + clickView.width / 2
                val centerY = location[1] + clickView.height / 2
                val targetUserId = data.user_id.toString()
                val newList = RTVariable.statusListGlobal.drop(1)
                val intent = Intent(requireContext(), PlayStatusActivity::class.java)
                //intent.putExtra("play_position", storyPosition)
                val json = Gson().toJson(newList)
                intent.putExtra("story_list", json)
                intent.putExtra("is_play_single", true)
                intent.putExtra("user_id", targetUserId)
                intent.putExtra("start_x", centerX)
                intent.putExtra("start_y", centerY)
                intent.putExtra("start_width", clickView.width)
                intent.putExtra("start_height", clickView.height)
                startActivity(intent)
                requireActivity().overridePendingTransition(0, 0)
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
                    FollowRequest(following_id = user_id)

                )
                if (response.status == 1 && response.code == 200) {
                    Toast.makeText(
                        requireContext(),
                        "Follow successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    RTVariable.USER_IS_FOLLOWING = true
                    adapter?.updateFollow(position)
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
                    Toast.makeText(
                        requireContext(),
                        "Unfollow successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    adapter?.updateFollow(position)
                    RTVariable.USER_IS_FOLLOWING = false
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
                Toast.makeText(requireContext(), "Image sharing failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSideOptionsPopup(pos: Int, data: Data, view: View) {
        Log.i(
            "TAG",
            "openSideOptionsPopup: " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.userId.toString()
        )

        val loginUserId =
            Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.userId.toString()
        val postId = data.id
        val isCover: String = if (data.is_cover == "TRUE") {
            "FALSE"
        } else {
            "TRUE"
        }
        if (data.user_id.toString() == loginUserId) {
            coverPostPosition = pos
            coverPostId = postId ?: 0
            coverPostIsCover = isCover
            profileOptions(view, postId.toString(), isCover)
        } else {
            homeOptions(view, data.user_id.toString())
        }
    }

    private fun profileOptions(view: View, postId: String, isCover: String) {
        if (isCover == "TRUE") {
            val popup = ReusablePopup(
                context = requireContext(),
                anchorView = view,
                onOption1Click = {
                    val token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken
                    homeViewModel.setRemoveCoverApi(token, postId, isCover)
                },
                onOption2Click = {
                    openDeletePostConfirmationDialog(postId)
                },
                onOption3Click = {
                },
                option1Text = "Set Cover",
                option2Text = "Delete",
                option3Text = "Cancel",
                option1ImageRes = R.drawable.filled_star,
                option2ImageRes = R.drawable.delete_icon,
                option3ImageRes = R.drawable.ic_cancel_red
            )
            popup.show()
        }else{
            val popup = ReusablePopup(
                context = requireContext(),
                anchorView = view,
                onOption1Click = {
                    openDeletePostConfirmationDialog(postId)
                },
                onOption2Click = {
                },
                option1Text = "Delete",
                option2Text = "Cancel",
                option1ImageRes = R.drawable.delete_icon,
                option2ImageRes = R.drawable.ic_cancel_red
            )
            popup.show()
        }
    }

    private fun homeOptions(view: View, userId: String) {
        val popup = ReusablePopup(
            context = requireContext(),
            anchorView = view,
            onOption1Click = {
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
                bottomSheetFragment.show(
                    requireActivity().supportFragmentManager,
                    "BlockBottomSheet"
                )
            },
            onOption2Click = {
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

    fun openDeletePostConfirmationDialog(postId: String) {
        showCustomDialogWithBinding(
            requireContext(), "Are you sure you want to delete this post?",
            onYes = {
                viewModel.hitDeletePostDataApi(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken.toString(), postId = postId
                )
            },
            onNo = {
            }
        )
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
}