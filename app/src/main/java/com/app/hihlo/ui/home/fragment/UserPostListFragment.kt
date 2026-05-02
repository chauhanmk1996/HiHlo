package com.app.hihlo.ui.home.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
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
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.get_profile.Data
import com.app.hihlo.model.get_profile.Posts
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
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.chat.bottom_sheet.SendCoinsBottomSheetFragment
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.adapter.AdapterUserPostList
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.home.view_model.UserPostListViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.ui.reels.bottom_sheet.CommentReelBottomSheet
import com.app.hihlo.ui.reels.view_model.ReelsViewModel
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.MyApplication
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
import kotlin.math.log

class UserPostListFragment : BaseFragment<FragmentUserPostListBinding>() {
    private val viewModel: UserPostListViewModel by viewModels()
    private val viewModel2: UserPostListViewModel by viewModels()
    var isCommentPosted = false
    lateinit var commentsBottomSheetFragment: CommentReelBottomSheet
    val args:UserPostListFragmentArgs by navArgs()
    var homePosts: MutableList<Post> = mutableListOf()
    var profilePosts: Posts = Posts()
    var from: String=""
    var position: Int=0
    var positionToComment: Int=0
    var postId = ""
    var adapter: AdapterUserPostList?=null
    private val viewModel4: ReelsViewModel by viewModels()
    private val viewModel5: HomeViewModel by viewModels()
    var totalAvailableCoins: Int?=null
    private var isLoadMore = false
    private var myStoryData: MyStory = MyStory()
    private var allStory: List<Story>? = null
    private var currentVisiblePosition = 0
    private val viewModel6: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StatusItem>
    override fun initView(savedInstanceState: Bundle?) {
        homePosts = args.homePosts.toMutableList()
        from = args.from
        position = args.position.toInt()
        profilePosts = args.profilePosts
        Log.e("TAG", "initView: ${Gson().toJson(profilePosts)}")
        setAdapter()
        setUI()
    }

    private fun setUI() {
        when(from){
            "home"->{
                binding.headerLayout.isVisible = false
            }
            else -> {
                binding.headerLayout.isVisible = true
            }
        }
    }

    private fun setAdapter() {
        Log.e("TAG", "initView: ${::getSelectedPost}")
        Log.e("TAG", "initViewPost: ${homePosts}")
        Log.e("TAG", "initViewPost: ${profilePosts}")
        Log.e("TAG", "initViewFrom: ${from}")
        Log.e("TAG", "initViewFrom: ${profilePosts}")
        adapter = AdapterUserPostList(homePosts, profilePosts, from, ::getSelectedPost)
        binding.postListRecycler.adapter = adapter
        lifecycleScope.launch {
            if(!UserDataManager.isUserInnerPostIsResume(requireContext())){
                binding.postListRecycler.scrollToPosition(position)
                //ProcessDialog.showDialog(requireContext(), true)
                delay(1300)
                binding.postListRecycler.scrollToPosition(position)
                delay(700)
                //ProcessDialog.dismissDialog(true)
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
                    if(RTVariable.COMMENT_DELETED){
                        RTVariable.COMMENT_DELETED = false
                        adapter?.update_comment_count(RTVariable.COMMENT_COUNT, RTVariable.POST_POSITION)
                        //viewModel.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10")
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
        Log.e("DATAPOS", "DATAPOS>>> "+UserDataManager.get_postCommentPosition(requireContext()))
        if(UserDataManager.isUserInnerPostIsResume(requireContext())){
            UserDataManager.setUserInnerPostIsResume(requireContext(), false)
            scrollToRecyclerPosition(UserDataManager.get_postCommentPosition(requireContext()))
        }
        if(UserDataManager.get_postCommentShow(requireContext())){
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
        viewModel5.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "1", "10", "0")
        viewModel4.hitCoinDetailsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
        viewModel6.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
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
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            viewModel5.myStory = it.data.payload.my_story ?: MyStory()
                            viewModel5.stories = it.data.payload.stories
                            viewModel5.isStoryUploaded = it.data.payload.is_story_uploaded
                            viewModel5.profileImage = it.data.payload.myProfile.profileImage
                            Log.e("TAG", "Home success: ${myStoryData}")
                            Log.e("TAG", "Home success: ${allStory}")
                            adapter?.addStory(listOf(it.data.payload.my_story ?: MyStory()), it.data.payload.stories)
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
        viewModel.getLikeReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Post like success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            val payload = it.data.payload
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }else{
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
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
        viewModel.getPostCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel post comment success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isCommentPosted = true
                            adapter?.updateCommentCount(positionToComment)

                            viewModel.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10") // Initial call with page 1, limit 10
                        }else{
                            //Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        //Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
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
        viewModel.getReplyToCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel reply to comment success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            isCommentPosted=true
                            viewModel.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10") // Initial call with page 1, limit 10
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
        viewModel.getReelCommentsLiveData().observe(viewLifecycleOwner) {
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
        /*viewModel.getReelCommentsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel comments success: ${Gson().toJson(it)}")
//                    if (it.data?.status==1){
                    if (it.data?.code == 200){
                        if (isCommentPosted){
                            isCommentPosted=false
                            if (::commentsBottomSheetFragment.isInitialized){
                                commentsBottomSheetFragment.updateComments(it.data.payload)
                            }
                        }else {
                            openCommentsBottomSheet(it.data.payload ?: Payload())
                        }
//                        }else{
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
//                        }
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
        viewModel.getDeletePostLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete post success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            findNavController().popBackStack()
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
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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
        viewModel6.getStatusLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Status success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200) {
                            statusListGlobal = it.data.payload
                            Log.e("TAG", "Status success: ${statusListGlobal}")
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
                viewModel.hitPostCommentApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, result, postId)
            }
            onReplyAction = { result ->
                isCommentPosted = true // Assuming same flag for reply, adjust if separate
                viewModel.hitReplyToCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, result, postId)
            }
            onLoadMore = { page, limit ->
                isLoadMore = true // Set flag before load more API call
                viewModel.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, page.toString(), limit.toString())
            }
        }

        commentsBottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_user_post_list

    }
    private fun getSelectedPost(post: Post, data:Data, click: Int, position: Int, view:View, clickView: View){
        positionToComment = position
        Log.e("TAG", "sharePost: $post")
        Log.e("TAG", "sharePost: $data")
        Log.e("TAG", "sharePost: $click")
        when(click){
            0->{
                openSideOptionsPopup(post, data, view)
            }
            1->{
                when(from){
                    "home"->{
                        findNavController().navigate(UserPostListFragmentDirections.actionUserPostListFragmentToProfileFragment("0", post.user_id.toString()))
                    }
                    else -> {
                        findNavController().popBackStack()

                    }
                }
            }
            2->{
                when(from){
                    "home"->{
                        viewModel.hitLikeReelApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, post.id.toString())
                    }
                    else -> {
                        viewModel.hitLikeReelApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, data.id.toString())
                    }
                }
            }
            3->{
                when(from){
                    "home"->{
                        this.postId = post.id.toString()
                        viewModel.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10") // Initial call with page 1, limit 10
//                        viewModel.hitGetReelCommentsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, post.id.toString())
                    }
                    else -> {
                        this.postId = data.id.toString()
                        viewModel5.posr_id = post.id.toString()
                        viewModel5.scroll_position = position
                        RTVariable.P_PID = post.id.toString()
                        UserDataManager.postCommentExpandState(requireContext(), false)
                        UserDataManager.postCommentSP(requireContext(), viewModel5.currentPage, position, postId.toString())
                        viewModel.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, postId, "1", "10") // Initial call with page 1, limit 10
//                        viewModel.hitGetReelCommentsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, data.id.toString())
                    }
                }
            }
            4->{
                when(from){
                    "home"->{
                        sharePost(post.asset_url.toString())
                    }
                    else -> {
                        sharePost(data.asset_url.toString())
                    }
                }
            }
            5->{
                //Toast.makeText(requireActivity(), "A ${data.user_id}", Toast.LENGTH_LONG).show()
                getSendFollow(data.user_id.toString(), position)
            }
            6->{
                //Toast.makeText(requireActivity(), "B ${data.user_id}", Toast.LENGTH_LONG).show()
                getSendUnFollow(data.user_id.toString(), position)
            }
            7->{
                //Toast.makeText(requireActivity(), "B ${data.id}", Toast.LENGTH_LONG).show()
                data.user_id?.let { openCoinsBottomSheet(it, it, data.creator_username.toString()) }
            }
            8->{
                if (statusListGlobal.isEmpty()) {
                    return
                }
                val location = IntArray(2)
                clickView.getLocationOnScreen(location)
                val targetUserId = data.user_id.toString()
                val newList = statusListGlobal.drop(1)
                val intent = Intent(requireContext(), PlayStatusActivity::class.java)
                //intent.putExtra("play_position", storyPosition)
                val json = Gson().toJson(newList)
                intent.putExtra("story_list", json)
                intent.putExtra("is_play_single", true)
                intent.putExtra("user_id", targetUserId)
                intent.putExtra("start_x", location[0])
                intent.putExtra("start_y", location[1])
                intent.putExtra("start_width", clickView.width)
                intent.putExtra("start_height", clickView.height)
                startActivity(intent)
                //requireActivity().overridePendingTransition(R.anim.slide_up, 0)
                requireActivity().overridePendingTransition(0, 0)

                /*RTVariable.IS_FROM_PROFILE = true
                val stories = adapter!!.getStoriesList()
                val storyPosition = stories.indexOfFirst { it.user_id == post.user_id }
                val story = stories.find { it.user_id == post.user_id }

// Use the same list for navigation, not a separate `allStory`
                val storyListToPass = stories   // or ensure allStory is properly populated

                if (storyPosition != -1 && storyListToPass.isNotEmpty()) {
                    val bundle = Bundle().apply {
                        putParcelableArrayList("storyList", ArrayList(storyListToPass))
                        putInt("position", storyPosition)
                        // ...
                    }
                    findNavController().navigate(R.id.secondStoryFragment, bundle)
                } else {
                    // Handle error: story not found or list empty
                    Toast.makeText(context, "Cannot open story", Toast.LENGTH_SHORT).show()
                } */




                //Toast.makeText(requireActivity(), "B ${data.id}", Toast.LENGTH_LONG).show()
//                val stories = adapter!!.getStoriesList()
//                //val storyPosition = stories.indexOfFirst { it.user_id == post.user_id }
//                val story = adapter!!.getStoriesList().find { it.user_id == post.user_id }
////                                val my_story = postAdapter.getMyStoriesList().getOrNull(0)
////                                    ?: MyStory()
//                val currentUserId = Preferences.getCustomModelPreference<LoginResponse>(
//                    requireContext(), LOGIN_DATA
//                )?.payload?.userId?.toString() ?: ""
//                //val isMyStoryValue = if (post.user_id.toString() == currentUserId) "1" else "0"
//                Log.e("TTTTT", "SSSSS>>> Story clicked: $story")
//                val bundle = Bundle().apply {
//                    putParcelableArrayList("storyList", ArrayList(allStory ?: emptyList()))
//                    putParcelable("myStoryData", myStoryData)
//                    putInt("position", RTVariable.STORY_POSITION)
//                }
//                try {
//                    findNavController().navigate(R.id.secondStoryFragment, bundle)
//                } catch (e: Exception) {
//                    Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
//                    Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
//                }
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
                    Toast.makeText(
                        requireContext(),
                        "Follow successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    RTVariable.USER_IS_FOLLOWING = true
                    adapter?.updateFollow(position)
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
                    Toast.makeText(
                        requireContext(),
                        "Unfollow successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    adapter?.updateFollow(position)
                    RTVariable.USER_IS_FOLLOWING = false
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
                Toast.makeText(requireContext(), "Image sharing failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSideOptionsPopup(post: Post, data: Data, view:View) {
//        val inflater = LayoutInflater.from(requireContext())
//        val binding = PopupChatSideOptionsBinding.inflate(inflater)
//        val popupWindow = PopupWindow(
//            binding.root,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            true
//        ).apply {
//            isOutsideTouchable = true
//            setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // for outside touch to dismiss
//            elevation = 20f
//            showAtLocation(requireView(), Gravity.CENTER, 0, 0)
//        }
        Log.i("TAG", "openSideOptionsPopup: "+from)
        Log.i("TAG", "openSideOptionsPopup: "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString())
        Log.i("TAG", "openSideOptionsPopup: "+post.user_id)
        Log.i("TAG", "openSideOptionsPopup: "+data.user_id)
        when(from){
            "home"->{
                if (post.user_id.toString()==Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString()){
                    profileOptions(view, post.id.toString(), post.user_id.toString())
                }else{
                    homeOptions(view, post.user_id.toString())
                }
            }
            else -> {
                if (data.user_id.toString()==Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString()){
                    profileOptions(view, data.id.toString(), data.user_id.toString())
                }else{
                    homeOptions(view, data.user_id.toString())
                }

            }
        }
    }

    private fun profileOptions(
        view:View,
        postId: String,
        userId: String
    ) {
//        binding.title1.text = "Delete"
//        binding.title2.isVisible=false
//        binding.view.setBackgroundDrawable(null)
//        binding.title1.setOnClickListener {
//            popupWindow.dismiss()
//            openDeletePostConfirmationDialog(postId)
//        }





        val popup = ReusablePopup(
            context = requireContext(),
            anchorView = view,
            onOption1Click = {
                openDeletePostConfirmationDialog(postId)
            },
            onOption2Click = {},
            option1Text = "Delete",
            option2Text = "Cancel",
            option1ImageRes = R.drawable.delete_icon, // Add your own move to request icon
            option2ImageRes = R.drawable.ic_cancel_red
        )
        popup.show()
    }

    private fun homeOptions(view:View, userId: String) {
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
                bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
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
                bottomSheetFragment.show(requireActivity().supportFragmentManager, "FlagBottomSheet")

            },
            option1Text = "Block",
            option2Text = "Report",
            option1ImageRes = R.drawable.ic_block_white, // Add your own move to request icon
            option2ImageRes = R.drawable.ic_flag_black
        )
        popup.show()
    }

    fun openDeletePostConfirmationDialog(postId: String) {
        showCustomDialogWithBinding(requireContext(), "Are you sure you want to delete this post?",
            onYes = {
                viewModel.hitDeletePostDataApi(token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString(), postId = postId)
            },
            onNo = {
                //dismiss()
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

}