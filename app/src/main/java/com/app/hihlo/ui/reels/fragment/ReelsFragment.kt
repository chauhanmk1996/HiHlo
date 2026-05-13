package com.app.hihlo.ui.reels.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.app.hihlo.R
import com.app.hihlo.ReelsDatabase.ReelsDatabaseManager
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentReelsBinding
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.get_reel_comments.response.Comment
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.model.reel.response.Reel
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.static.MIN_COINS_FOR_AUDIO
import com.app.hihlo.model.static.MIN_COINS_FOR_VIDEO
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.AGORA_TOKEN
import com.app.hihlo.preferences.UserPreference.CALLER_USER_IMAGE
import com.app.hihlo.preferences.UserPreference.CALL_TYPE
import com.app.hihlo.preferences.UserPreference.CALL_USER_NAME
import com.app.hihlo.preferences.UserPreference.CHANNEL_NAME
import com.app.hihlo.preferences.UserPreference.OTHER_USER_ID
import com.app.hihlo.preferences.UserPreference.U_ID
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.calling.activity.OldOutgoingCallActivity
import com.app.hihlo.ui.calling.activity.OutgoingVideoCallActivity
import com.app.hihlo.ui.chat.bottom_sheet.SendCoinsBottomSheetFragment
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.bottom_sheet.UploadMediaBottomSheet
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.reels.adapter.ReelAdapter
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.ui.reels.bottom_sheet.CallTypeBottomSheetFragment
import com.app.hihlo.ui.reels.bottom_sheet.CommentReelBottomSheet
import com.app.hihlo.ui.reels.view_model.ReelsViewModel
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.MyApplication
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.VideoCacheManager
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
//import com.google.android.exoplayer2.ExoPlayer
//import com.google.android.exoplayer2.MediaItem
//import com.google.android.exoplayer2.Player
//import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.collections.plus
import kotlin.getValue
import kotlin.isInitialized
import kotlin.toString

@AndroidEntryPoint
class ReelsFragment : BaseFragment<FragmentReelsBinding>() {
    private lateinit var bottomSheetFragment: UploadMediaBottomSheet
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var adapter: ReelAdapter
    private var reelsList: MutableList<Reel> = mutableListOf()
    private val viewModel: ReelsViewModel by viewModels()
    private var reelId = ""
    private var userId = ""
    var isCommentPosted = false
    lateinit var commentsBottomSheetFragment: CommentReelBottomSheet
    private var selectedMediaType: String = "I"
    private var selectedBottomSheetType = ""
    private var callerName = ""
    private var callerImage = ""
    private var isLoading = false
    private var currentPage = 1

    var totalAvailableCoins: Int? = null
    private var isLoadMore = false

    private val viewModel2: HomeViewModel by viewModels()

    //    private val args: ReelsFragmentArgs by navArgs()
    private val args by lazy {
        try {
            ReelsFragmentArgs.fromBundle(requireArguments())
        } catch (e: Exception) {
            null
        }
    }

    //    private var profileReels: com.app.hihlo.model.reel.response.Payload = com.app.hihlo.model.reel.response.Payload()
    private var from = ""
    private var reelPosition = ""
    private var commentOnReelPosition = 0
    //var currentPosition = 0
    private var targetPosition = 0
    private var isRestored = false
    private val viewModel6: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StatusItem>
    /*override fun initView(savedInstanceState: Bundle?) {
        if (!isFirstTime) return
        isFirstTime = false

        from = args?.from ?: "home"
        reelPosition = args?.reelPosition ?: "0"

        if (from == "profile") {
            reelsList = args?.reels?.reels ?: mutableListOf()
        }

        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        currentPage = 1

        Log.i("TAG", "initView: $reelsList")
        Log.i("TAG", "initView: $from")

        viewPagerAdapter(mutableListOf())

        if (from == "profile") {
            adapter.updateList(reelsList)
            lifecycleScope.launch {
                delay(300)
                binding.viewPager.currentItem = reelPosition.toInt()
            }
        } else {
            hitGetReelsApi(currentPage)
            setReelsAdapterPagination()
        }

        viewModel.hitCoinDetailsApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(), LOGIN_DATA
            )?.payload?.authToken
        )
    }*/
    /*override fun onResume() {
        super.onResume()
        Log.i("TAG", "onResume: isFirstTime = $isFirstTime")
        Log.i("TAG", "onResume: reelsList size = ${reelsList.size}")

        if (!isFirstTime && reelsList.isNotEmpty()) {
            binding.viewPager.post {
                adapter.updateList(reelsList)

                // Attach adapter if not already
                if (binding.viewPager.adapter == null) {
                    binding.viewPager.adapter = adapter
                }

                // Restore position
                binding.viewPager.setCurrentItem(reelPosition.toInt(), false)
            }
        }
    }*/


    override fun initView(savedInstanceState: Bundle?) {


    }

    private fun hitGetReelsApi(currentPage: Int) {
        RTVariable.REELS_CURRENT_PAGE = currentPage
        viewModel.commentPayloadCache = null
        UserDataManager.setReelsPosition(requireContext(), 0)
        Log.e("PAGE", "PAGE>>> " + currentPage.toString())
        viewModel.hitGetReelsApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, currentPage.toString(), "6"
        )
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_reels
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        RTVariable.bottom_page = 2
        setObserver()
        viewPagerCallback()
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = true
            refreshReels()
        }
        targetPosition = UserDataManager.getReelsPosition(requireContext())
        Log.e("TAG", "initView: reelPosition " + targetPosition)
        from = args?.from ?: "home"
        Log.i("TAG", "initView: reelPosition " + reelPosition)
        Log.i("TAG", "updatedreelsize: " + targetPosition)

        //val savedPosition = UserDataManager.getReelsPosition(requireContext())

        reelPosition = when {
            targetPosition >= 0 -> targetPosition.toString()   // ✅ highest priority
            !reelPosition.isNullOrEmpty() -> reelPosition    // existing value
            !args?.reelPosition.isNullOrEmpty() -> args?.reelPosition ?: "0"
            else -> "0"
        }
        //reelPosition = targetPosition.toString()
        Log.i("TAG", "initView: reelPosition A " + reelPosition)
        if (from == "profile") {
            RTVariable.reelsCache = args?.reels?.reels ?: mutableListOf()
        }
        viewModel2.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "1", "10", "0")
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        Log.i("TAG", "initView: " + RTVariable.reelsCache)
        Log.i("TAG", "initView: " + from)
        if (from == "profile") {
            binding.swipeRefresh.isEnabled = false
            viewPagerAdapter(mutableListOf())
            adapter.updateList(RTVariable.reelsCache)
            lifecycleScope.launch {
                delay(300) // delay in milliseconds
                binding.viewPager.currentItem = reelPosition.toInt()
            }
            viewPagerAdapter(mutableListOf())
            adapter.updateList(RTVariable.reelsCache)
//            lifecycleScope.launch {
//                delay(300) // delay in milliseconds
//                binding.viewPager.currentItem = reelPosition.toInt()
//            }
            binding.viewPager.setCurrentItem(reelPosition.toInt(), false)
        } else {
            if(!RTVariable.IS_REELS_LOADED){
                currentPage = 1
                RTVariable.reelsCache.clear()
                RTVariable.REELS_CURRENT_PAGE = 1
                binding.swipeRefresh.isEnabled = true
                viewPagerAdapter(mutableListOf())
                hitGetReelsApi(currentPage)
                setReelsAdapterPagination()
            }
            Log.e("TAG", "updatedreelsize: SP" + RTVariable.IS_REELS_LOADED)
            if(RTVariable.IS_REELS_LOADED){
                Log.e("TAG", "updatedreelsize: SP" + reelPosition.toInt())
                Log.e("TAG", "updatedreelsize: S" + RTVariable.reelsCache)
                viewPagerAdapter(mutableListOf())
                currentPage = RTVariable.REELS_LAST_POSITION
                adapter.updateList(RTVariable.reelsCache)
                isLoading = true
                //adapter.notifyDataSetChanged()
                binding.viewPager.setCurrentItem(reelPosition.toInt(), false)
                //val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
                //recyclerView?.scrollToPosition(reelPosition.toInt())
                //binding.viewPager.currentItem = reelPosition.toInt()
            }
        }
//        if (from != "profile") {
//
//        }
        viewModel.hitCoinDetailsApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken
        )
        viewModel6.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    if (RTVariable.COMMENT_DELETED) {
                        RTVariable.COMMENT_DELETED = false
                        //hitGetReelsApi(currentPage)
                        adapter.updateCommentCount(
                            RTVariable.POST_POSITION,
                            RTVariable.COMMENT_COUNT
                        )
                    } else {
                        //Log.e("RRRRR", "RRRRR>>>" + RTVariable.REELS_ID)
                        getReels(currentPage, 6)
                    }
                }
            }
        }
        requireActivity().supportFragmentManager.setFragmentResultListener("self", viewLifecycleOwner) { _, _ ->
            Log.i("TAG", "onViewCreated: chatIconTap")
            refreshReels()
//            val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
//            recyclerView?.scrollToPosition(0)
        }
        requireActivity().supportFragmentManager.setFragmentResultListener("other", viewLifecycleOwner) { _, _ ->
            Log.i("TAG", "onViewCreated: chatIconTap")
            RTVariable.REELS_INSTANCE_KEY_ID = 0
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(500)
                    if(RTVariable.IS_STATUS_VIEWER_ACTIVATED){
                        RTVariable.IS_STATUS_VIEWER_ACTIVATED = false
                        //viewModel5.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
                        getRefreshStory(1, 0)
                        getRefreshMainStory(0)
                    }
                    if(!RTVariable.IS_STORY_CLICKED_FRON_REELS){
                        RTVariable.IS_STORY_CLICKED_FRON_REELS = true
                        exoPlayer.play()
                    }
                }
            }
        }
    }

    private fun getRefreshStory(page: Int, gender_id: Int){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getHomeData(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    page.toString(),
                    10.toString(),
                    gender_id.toString()
                )
                if (response.status == 1 && response.code == 200) {
                    viewModel2.stories = response.payload.stories
                    adapter.updateStories(viewModel2.stories)
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun getRefreshMainStory(gender_id: Int){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getStatusData(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    gender_id.toString()
                )
                if (response.status == 1 && response.code == 200) {
                    statusListGlobal = response.payload
                    RTVariable.statusListGlobal = statusListGlobal
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun refreshReels() {
        currentPage = 1
        RTVariable.reelsCache.clear()
        RTVariable.REELS_CURRENT_PAGE = 1
        binding.swipeRefresh.isEnabled = true
        UserDataManager.setReelsPosition(requireContext(), 0)
        hitGetReelsApi(1)
    }

    private fun getReels(current_page: Int, limit: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getReels(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
                    page = current_page.toString(),
                    limit = limit.toString()
                )
                if (response.status == 1 && response.code == 200) {
                    if (response.payload.reels?.isNotEmpty() == true) {
                        val commentsCount =
                            response.payload.reels?.find { reel -> reel.id == RTVariable.REELS_ID.toInt() }
                                ?.commentsCount ?: 0
                        Log.e("TTTTT", "TTTTT>>>" + commentsCount)
                        adapter.updateCommentCount(RTVariable.POST_POSITION, commentsCount)
                    } else {

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun viewPagerAdapter(reels: MutableList<Reel>) {
        adapter = ReelAdapter(
            Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.userId.toString(),
            reels,
            exoPlayer,
            ::followSelected,
            ::openUploadBottomSheet,
            ::openSideOptions,
            ::openProfile,
            ::shareReelSelected,
            from
        ) { position, reelId, reelPosition, isLikedStatus ->
            commentOnReelPosition = reelPosition
            when (position) {
                0 -> {
//                        locally change the value of liked key and later hit this api only if internet is available
                    RTVariable.reelsCache[reelPosition].isLiked = if (isLikedStatus == 1) 2 else 1
                    adapter.updateLike(reelPosition, if (isLikedStatus == 1) 2 else 1)
                    viewModel.hitLikeReelApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken, reelId.toString()
                    )
                }

                1 -> {
                    //                    openCommentsSection()
                    this.reelId = reelId.toString()
                    viewModel.hitGetReelCommentsApi(
                        "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.authToken, reelId.toString(), "1", "10"
                    ) // Initial call with page 1, limit 10
//                        viewModel.hitGetReelCommentsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, reelId.toString())
                }

                2 -> {
                    openCoinsBottomSheet(
                        reelId,
                        reels[reelPosition].creatorId,
                        reels[position].creator.name
                    )
                }
                /*3->{
//                        triggerPushNotification(reels[position].creatorId, reels[position].creator)
                    if (Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId!=reels[reelPosition].creatorId){
                        openCallTypeBottomSheet(reels[reelPosition].creatorId.toString(), reels[reelPosition].creator.name, reels[position].creator.profileImage)
                    }else{
                        Toast.makeText(requireContext(), "You cannot call yourself.", Toast.LENGTH_SHORT).show()
                    }
                }*/
                3 -> {
                    shareReel(reels[reelPosition].assetUrl)
                }

                4 -> {
                    if (userId == Preferences.getCustomModelPreference<LoginResponse>(
                            requireContext(),
                            LOGIN_DATA
                        )?.payload?.userId.toString()
                    ) {
//                            openDeletePostConfirmationDialog(reelId.toString(), view)

                    } else {
//                            showOptionsPopup()
                    }
                }
            }
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 3

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                adapter.currentViewHolder ?: return

                when (state) {
                    Player.STATE_BUFFERING -> adapter?.currentViewHolder?.loader?.visibility =
                        View.VISIBLE

                    Player.STATE_READY -> adapter?.currentViewHolder?.loader?.visibility = View.GONE
                    Player.STATE_ENDED -> {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                    }

                    else -> {}
                }
            }
        })
    }

    fun openSideOptions(reelId: Int, position: Int, view: View) {
        if (userId == Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.userId.toString()
        ) {
            openDeletePostConfirmationDialog(reelId.toString(), view)
        } else {
            showOptionsPopup(view)
        }
    }

    fun openProfile(isStoryUploaded: Int, imageView: View) {
        reelPosition = binding.viewPager.currentItem.toString()
        if(isStoryUploaded==1){
            RTVariable.IS_STORY_CLICKED_FRON_REELS = true
            exoPlayer.pause()
            if (statusListGlobal.isEmpty()) {
                return
            }
            val location = IntArray(2)
            imageView.getLocationOnScreen(location)
            val centerX = location[0] + imageView.width / 2
            val centerY = location[1] + imageView.height / 2
            val targetUserId = RTVariable.USER_ID.toInt().toString()
            val newList = RTVariable.statusListGlobal.drop(1)
            val intent = Intent(requireContext(), PlayStatusActivity::class.java)
            //intent.putExtra("play_position", storyPosition)
            val json = Gson().toJson(newList)
            intent.putExtra("story_list", json)
            intent.putExtra("is_play_single", true)
            intent.putExtra("user_id", targetUserId)
            intent.putExtra("start_x", centerX)
            intent.putExtra("start_y", centerY)
            intent.putExtra("start_width", imageView.width)
            intent.putExtra("start_height", imageView.height)
            startActivity(intent)
            //requireActivity().overridePendingTransition(R.anim.slide_up, 0)
            requireActivity().overridePendingTransition(0, 0)
        }else{
            findNavController().navigate(
                ReelsFragmentDirections.actionReelsFragmentToProfileFragment(
                    "0",
                    RTVariable.reelsCache[binding.viewPager.currentItem].creatorId.toString(),
                    "reels"
                )
            )
        }
    }

    fun shareReelSelected(assetUrl: String) {
        shareReel(assetUrl)
    }

    fun openDeletePostConfirmationDialog(reelId: String, view: View) {
        val popup = ReusablePopup(
            context = requireContext(),
            anchorView = view,
            onOption1Click = {
                showCustomDialogWithBinding(
                    requireContext(), "Are you sure you want to delete this reel?",
                    onYes = {
                        viewModel.hitDeleteReelDataApi(
                            token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.authToken.toString(), reelId.toString()
                        )
                    },
                    onNo = {
                        //dismiss()
                    }
                )
            },
            onOption2Click = {},
            option1Text = "Delete",
            option2Text = "Cancel",
            option1ImageRes = R.drawable.delete_icon, // Add your own move to request icon
            option2ImageRes = R.drawable.ic_cancel_red
        )
        popup.show()
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
        showCustomDialogWithBinding(
            requireContext(), "Do you want to send ${data.coins} coins to ${name}",
            onYes = {
                viewModel.hitSendGiftApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        MyApplication.appContext, LOGIN_DATA
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

    private fun setReelsAdapterPagination() {

    }

    private fun shareReel(assetUrl: String) {
        lifecycleScope.launch {
            try {
                val imageFile = File(requireContext().cacheDir, "video_thumbnail.jpg")

                withContext(Dispatchers.IO) {
                    val bitmap = Glide.with(requireContext())
                        .asBitmap()
                        .load(assetUrl) // Your video URL
                        .frame(1_000_000) // get frame at 1s (optional)
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
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Check out more such reels on: $appLink")
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Media"))

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Thumbnail sharing failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun openCallTypeBottomSheet(createrId: String, name: String, profileImage: String) {
        val bottomSheetFragment = CallTypeBottomSheetFragment.newInstance("call").apply {
            callerName = name
            callerImage = profileImage
            onCallTypeSelected = {
                dismiss()
                when (it) {
                    0 -> {
                        if ((totalAvailableCoins ?: 0) > MIN_COINS_FOR_AUDIO) {
                            viewModel.hitGenerateAgoraTokenDataApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken,
                                channelName = UUID.randomUUID().toString(),
                                calleeId = createrId,
                                uid = Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.userId.toString(),
                                "audio",
                                sender_id = Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.userId.toString()
                            )
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "You need atleast $MIN_COINS_FOR_AUDIO coins to make an Audio Call.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    1 -> {
                        if ((totalAvailableCoins ?: 0) > MIN_COINS_FOR_VIDEO) {
                            viewModel.hitGenerateAgoraTokenDataApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken,
                                channelName = UUID.randomUUID().toString(),
                                calleeId = createrId,
                                uid = Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.userId.toString(),
                                "video",
                                sender_id = Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.userId.toString()
                            )
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "You need atleast $MIN_COINS_FOR_VIDEO coins to make a Video Call.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    /*0->{
                        viewModel.hitGenerateAgoraTokenDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, channelName = UUID.randomUUID().toString(), calleeId = createrId, uid = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), "audio")
                    }
                    1->{
                        viewModel.hitGenerateAgoraTokenDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, channelName = UUID.randomUUID().toString(), calleeId = createrId, uid = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(), "video")
                    }*/
                }
            }
        }
        bottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")
    }
    private fun viewPagerCallback() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            var currentPosition = 0
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //UserDataManager.setReelsPosition(requireContext(), position)
                val totalItems = RTVariable.reelsCache.size
                Log.i("TAG", "onPageSelected: $totalItems   $position")
                if (position == totalItems - 2 && isLoading) {
                    currentPage++
                    RTVariable.REELS_CURRENT_PAGE = currentPage
                    hitGetReelsApi(currentPage)
                    isLoading = false
                }
                if (currentPosition < RTVariable.reelsCache.size) {
                    RTVariable.reelsCache[currentPosition].lastPlaybackPosition = exoPlayer.currentPosition
                }
                if (RTVariable.reelsCache.isNullOrEmpty() || position >= RTVariable.reelsCache.size) {
                    return
                }
                userId = RTVariable.reelsCache[position].creatorId.toString()
                val previousPosition = currentPosition
                currentPosition = position
                adapter.currentPlayingPosition = position
                adapter.notifyItemChanged(previousPosition)
                adapter.notifyItemChanged(currentPosition)
                adapter.currentPlayingPosition = currentPosition
//                binding.viewPager.post {
//                    if (previousPosition < adapter.itemCount) {
//                        adapter.notifyItemChanged(previousPosition)
//                    }
//                    if (currentPosition < adapter.itemCount) {
//                        adapter.notifyItemChanged(currentPosition)
//                    }
//                }
                val reel = RTVariable.reelsCache[position]
                RTVariable.REELS_POSITION = position
                RTVariable.REELS_ID = reel.id.toString()
                playVideo(reel.assetUrl, reel.lastPlaybackPosition)
            }
        })
    }

    fun openUploadBottomSheet(check: String) {
        /*if(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator ==1){
            selectedBottomSheetType = "reel"
            openUploadBottomSheet("reel")
        }else{
            Toast.makeText(requireContext(), "You are not a creator", Toast.LENGTH_SHORT).show()
        }*/
//        bottomSheetFragment = UploadMediaBottomSheet.newInstance(check).apply {
//            onGallerySelected = {
//                dismiss()
//                when(it){
//                    0->{
//                        selectedBottomSheetType = "post"
//                        openUploadBottomSheet("post")
//                    }
//                    1->{
//                        if(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.isCreator ==1){
//                            selectedBottomSheetType = "reel"
//                            openUploadBottomSheet("reel")
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
        val popup = UploadMediaBottomSheet(requireContext(), check, binding.root).apply {
            onGallerySelected = {
                dismiss()
                when (it) {
                    0 -> {
                        selectedBottomSheetType = "post"
                        openUploadBottomSheet("post")
                    }

                    1 -> {
                        if (Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA
                            )?.payload?.isCreator == 1
                        ) {
                            selectedBottomSheetType = "reel"
                            openUploadBottomSheet("reel")
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "You are not a creator",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            onUploadTypeSelected = {
                dismiss()
                when (it) {
                    0 -> {
                        checkGalleryPermissionAndPick("I")
                    }

                    1 -> {
                        checkGalleryPermissionAndPick("V")
                    }
                }
            }
        }
        popup.show()
    }

    private fun checkGalleryPermissionAndPick(mediaType: String) {
        selectedMediaType = mediaType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            val permissions = arrayOf(
//                Manifest.permission.READ_MEDIA_IMAGES,
//                Manifest.permission.READ_MEDIA_VIDEO
//            )
//            requestMultiplePermissionsLauncher.launch(permissions)
            launchMediaPicker()

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

    private fun launchMediaPicker() {
        val mediaType = when (selectedMediaType) {
            "I" -> ActivityResultContracts.PickVisualMedia.ImageOnly
            "V" -> ActivityResultContracts.PickVisualMedia.VideoOnly
            else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        }

        mediaPickerLauncher.launch(PickVisualMediaRequest(mediaType))
    }

    private val mediaPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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
                    Log.e("TAG", "mimmeType $mimeType")
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Always call super

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CROP_VIDEO) { // Check if it's the result for our request
                UserPreference.selectedMediaToUpload = selectedBottomSheetType
                findNavController().navigate(R.id.action_reelsFragment_to_addReelFragment)
            } else if (requestCode == UCrop.REQUEST_CROP) {
                val resultUri = UCrop.getOutput(data!!)
                UserPreference.seletedUri = resultUri ?: Uri.EMPTY
                UserPreference.selectedMediaToUpload = selectedBottomSheetType
                findNavController().navigate(R.id.action_reelsFragment_to_addReelFragment)
            }
        } else {
            Log.w("ReelFragment", " cropping was cancelled or failed with code: $resultCode")
        }
    }

    private val requestSinglePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) launchMediaPicker()
        else Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
    }
    /*private fun openUploadBottomSheet(check: String) {
        val bottomSheetFragment = UploadReelBottomSheetFragment.newInstance(check).apply {

            onUploadTypeSelected = {
                dismiss()
                when(it){
                    0->{
                        checkGalleryPermissionAndPick("I")
                    }
                    1->{
                        checkGalleryPermissionAndPick("V")
                    }
                }
            }
        }
        bottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")
    }*/

    fun followSelected(id: Int, reelPosition: Int, isAlreadyFollowed: Int) {
        adapter.updateFollow(reelPosition, isAlreadyFollowed)
        if (isAlreadyFollowed == 2) {
            viewModel.hitFollowUserDataApi(
                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA
                )?.payload?.authToken, FollowRequest(following_id = id.toString())
            )
        } else {
            viewModel.hitUnfollowUserDataApi(
                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA
                )?.payload?.authToken, FollowRequest(unfollowId = id.toString())
            )
        }
    }

    fun showOptionsPopup(view: View) {
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


//        val dialog = Dialog(requireContext())
//        val popupBinding = PopupListBinding.inflate(LayoutInflater.from(context))
//        dialog.setContentView(popupBinding.root)
//        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
//
//        dialog.show()
//        dialog.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        popupBinding.listPopupRecycler.adapter = AdapterListPopup(reelScreenPopupList){
//            when(it){
//                reelScreenPopupList[0]->{
//                    dialog.dismiss()
//                    val bottomSheetFragment = BlockFlagBottomSheet()
//                    val bundle = Bundle().apply {
//                        putString("screen", "block")  // Add your arguments here
//                        putString("userId", userId)  // Add your arguments here
//                    }
//                    bottomSheetFragment.arguments = bundle
//                    bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
//
//                }
//                reelScreenPopupList[1]->{
//                    dialog.dismiss()
//                    val bottomSheetFragment = BlockFlagBottomSheet()
//                    val bundle = Bundle().apply {
//                        putString("screen", "flag")  // Add your arguments here
//                        putString("userId", userId)  // Add your arguments here
//                    }
//                    bottomSheetFragment.arguments = bundle
//                    bottomSheetFragment.show(requireActivity().supportFragmentManager, "FlagBottomSheet")
//
//                }
//            }
//        }
    }

    @OptIn(UnstableApi::class)
    private fun playVideo(url: String, resumePosition: Long = 0L) {
        val cacheFactory = VideoCacheManager.buildCacheDataSource(requireContext())
        val mediaItem = MediaItem.fromUri(url)
        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (RTVariable.REELS_PLAYING_POSITION == 0L) {
            RTVariable.REELS_PLAYING_POSITION = 0L
            exoPlayer.seekTo(resumePosition)
        }else{
            exoPlayer.seekTo(RTVariable.REELS_PLAYING_POSITION)
            RTVariable.REELS_PLAYING_POSITION = 0L
        }
        if (UserDataManager.isPaused(requireActivity())) {
            val savedPos = UserDataManager.getPosition(requireContext())
            Log.e("REEL_POS", "REEL_POS>>>> $savedPos  $RTVariable.REELS_POSITION")
            if (savedPos == RTVariable.REELS_POSITION) {
                exoPlayer.playWhenReady = false
            } else {
                UserDataManager.setPause(requireContext(), false)
                exoPlayer.playWhenReady = true
            }
        } else {
            exoPlayer.playWhenReady = true
        }
        if(UserDataManager.isReelMute(requireContext())){
            val savedPos = UserDataManager.getPosition(requireContext())
//            if (savedPos == RTVariable.REELS_POSITION) {
//                exoPlayer.volume = 0f
//            }else{
//                exoPlayer.volume = 1f
//            }
            exoPlayer.volume = 0f
            //exoPlayer.volume = 0f
        }else{
            exoPlayer.volume = 1f
        }
    }

    private fun setObserver() {
        viewModel2.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200) {
                            // ✅ SAVE EVERYTHING IN VIEWMODEL
                            viewModel2.myStory = it.data.payload.my_story ?: MyStory()
                            viewModel2.stories = it.data.payload.stories
                            viewModel2.isStoryUploaded = it.data.payload.is_story_uploaded
                            viewModel2.profileImage = it.data.payload.myProfile.profileImage
                            adapter.updateStories(viewModel2.stories)
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
        viewModel.getSendGiftLiveData().observe(viewLifecycleOwner) {
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
        viewModel.getDeleteReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "delete reel success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        reelsList.removeAt(binding.viewPager.currentItem)
                        viewPagerAdapter(mutableListOf())
                        adapter.updateList(reelsList)
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
        viewModel.getCoinDetailsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "coins details success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        totalAvailableCoins = it.data.payload.coins
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
        viewModel.getReelsLiveData().observe(viewLifecycleOwner) { response ->

            when (response.status) {

                Status.LOADING -> {
                    isLoading = true
                    ProcessDialog.showDialog(requireContext(), true)
                }

                Status.SUCCESS -> {
                    isLoading = true
                    binding.swipeRefresh.isRefreshing = false
                    ProcessDialog.dismissDialog(true)

                    val data = response.data

                    if (data?.status == 1 && data.code == 200) {
                        val newReels = data.payload.reels ?: mutableListOf()

                        if (newReels.isNotEmpty()) {

                            if (currentPage == 1) {
                                // First page → full reset
                                RTVariable.reelsCache.clear()
                                RTVariable.reelsCache.addAll(newReels)

                                adapter.updateList(RTVariable.reelsCache.toMutableList())

                                // Play first reel
                                if (RTVariable.reelsCache.isNotEmpty()) {
                                    val firstReel = RTVariable.reelsCache[0]
                                    adapter.currentPlayingPosition = 0
                                    binding.viewPager.setCurrentItem(0, false)
                                    playVideo(firstReel.assetUrl ?: "", firstReel.lastPlaybackPosition ?: 0L)
                                }
                            } else {
                                // Pagination → append only (this fixes your bug)
                                val oldSize = RTVariable.reelsCache.size
                                RTVariable.reelsCache.addAll(newReels)

                                // Use the new append method
                                adapter.appendReels(newReels)
                                // Optional: smooth scroll if needed
                                // binding.viewPager.setCurrentItem(oldSize, false)
                            }
                            RTVariable.REELS_LAST_POSITION = currentPage
                            RTVariable.IS_REELS_LOADED = true
                        } else {
                            // No more data
                            isLoading = false
                            //Toast.makeText(requireContext(), "No more reels", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        //Toast.makeText(requireContext(), data?.message ?: "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }

                Status.ERROR -> {
                    isLoading = false
                    binding.swipeRefresh.isRefreshing = false
                    ProcessDialog.dismissDialog(true)

                    Log.e("TAG", "Reels Error: ${response.message}")
                    //Toast.makeText(requireContext(), response.message ?: "Error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
//        viewModel.getReelsLiveData().observe(viewLifecycleOwner) {
//            when (it.status) {
//                Status.SUCCESS -> {
//                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
//                    if (it.data?.status == 1) {
//                        if (it.data.code == 200) {
//                            binding.swipeRefresh.isRefreshing = false
//                            if (it.data.payload.reels?.isNotEmpty() == true) {
//
//                                reelsList.addAll(it.data.payload.reels)
//                                adapter.updateList(it.data.payload.reels.toMutableList())
//
//                                val currentSize = it.data.payload.reels.size
//
//                                // ✅ CASE 1: Target position is already loaded
//                                if (!isRestored && targetPosition < currentSize) {
//
//                                    binding.viewPager.post {
//                                        binding.viewPager.setCurrentItem(targetPosition, false)
//
//                                        adapter.currentPlayingPosition = targetPosition
//
//                                        playVideo(
//                                            it.data.payload.reels[targetPosition].assetUrl,
//                                            it.data.payload.reels[targetPosition].lastPlaybackPosition
//                                        )
//                                    }
//
//                                    isRestored = true
//                                }
//
//                                // ✅ CASE 2: Target position NOT loaded yet → load more
//                                else if (!isRestored && targetPosition >= currentSize) {
//
//                                    currentPage++
//                                    hitGetReelsApi(currentPage)
//                                }
//
//                                // ✅ Normal flow after restore
//                                else if (currentPage == 1 && isRestored.not()) {
//                                    binding.viewPager.setCurrentItem(0, false)
//                                } else {
//                                    adapter.updateList(
//                                        it.data.payload.reels?.toMutableList() ?: mutableListOf()
//                                    )
//                                }
//                            }
//                        } else {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
//                                .show()
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                    ProcessDialog.dismissDialog(true)
//                }
//
//                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
//                }
//
//                Status.ERROR -> {
//                    Log.e("TAG", "Login Failed: ${it.message}")
//                    ProcessDialog.dismissDialog(true)
//                }
//            }
//        }
        viewModel.getReelCommentsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {

                Status.SUCCESS -> {

                    if (it.data?.code == 200) {

                        val newPayload = it.data.payload ?: return@observe

                        when {

                            // ✅ COMMENT POSTED
                            isCommentPosted -> {

                                isCommentPosted = false

                                viewModel.commentPayloadCache = newPayload

                                CommentPrefs.savePayload(
                                    requireContext(),
                                    reelId.toInt(),
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
                                    viewModel.commentPayloadCache
                                        ?: CommentPrefs.getPayload(requireContext(), reelId.toInt())

                                if (oldPayload != null) {

                                    val (mergedList, newItemsOnly) =
                                        CommentPrefs.mergeComments(
                                            oldPayload.comments ?: emptyList(),
                                            newPayload.comments ?: emptyList()
                                        )

                                    val updatedPayload = oldPayload.copy(
                                        comments = mergedList
                                    )

                                    viewModel.commentPayloadCache = updatedPayload

                                    // ✅ Save correct list
                                    CommentPrefs.savePayload(
                                        requireContext(),
                                        reelId.toInt(),
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
                                    viewModel.commentPayloadCache = newPayload
                                    CommentPrefs.savePayload(
                                        requireContext(),
                                        reelId.toInt(),
                                        newPayload
                                    )
                                }
                            }

                            // ✅ FIRST LOAD
                            else -> {

                                viewModel.commentPayloadCache = newPayload
                                CommentPrefs.clear(requireContext())
                                CommentPrefs.savePayload(
                                    requireContext(),
                                    reelId.toInt(),
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
        viewModel.getPostCommentLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel post comment success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            isCommentPosted = true
                            viewModel.hitGetReelCommentsApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    requireContext(),
                                    LOGIN_DATA
                                )?.payload?.authToken, reelId.toString(), "1", "10"
                            ) // Initial call with page 1, limit 10
                            adapter.updateComment(commentOnReelPosition)
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
                                )?.payload?.authToken, reelId.toString(), "1", "10"
                            ) // Initial call with page 1, limit 10
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
        viewModel.getLikeReelLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reel reply to comment success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        } else {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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
        viewModel.getGenerateAgoraTokenLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Agora token success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            Log.i("TAG", "setObserver: " + it.data.payload.calleeId)
                            val intent =
                                Intent(requireContext(), OutgoingVideoCallActivity::class.java)
                            intent.putExtra(OTHER_USER_ID, it.data.payload.calleeId)
                            intent.putExtra(AGORA_TOKEN, it.data.payload.agoraToken)
                            intent.putExtra(CHANNEL_NAME, it.data.payload.channelName)
                            intent.putExtra(U_ID, it.data.payload.uid)
                            intent.putExtra(CALL_TYPE, it.data.payload.callType)
                            intent.putExtra(CALL_USER_NAME, callerName)
                            intent.putExtra(CALLER_USER_IMAGE, callerImage)
                            startActivity(intent)
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
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        } else {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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
        viewModel.getUnfollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "follow user success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        } else {
//                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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
        viewModel6.getStatusLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Status success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200) {
                            statusListGlobal = it.data.payload
                            RTVariable.statusListGlobal = statusListGlobal
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

    fun getTotalCommentsCount(comments: List<Comment>): Int {
        var total = 0

        comments.forEach { comment ->
            total += 1
            total += (comment.replies?.size ?: 0)
        }

        return total
    }

    private fun openCommentsBottomSheet(payload: Payload) {
        commentsBottomSheetFragment = CommentReelBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable("comments", payload)
                putParcelableArrayList("stories", ArrayList(viewModel2.stories))
                putParcelable("myStory", viewModel2.myStory)
            }
            onCommentAction = { result ->
                isCommentPosted = true // Set flag before post
                currentPage = 1
                Log.e("CCCCCCC", "CCCCCCC")
                viewModel.hitPostCommentApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, result, reelId
                )
            }
            onReplyAction = { result ->
                isCommentPosted = true // Assuming same flag for reply, adjust if separate
                currentPage = 1
                viewModel.hitReplyToCommentsApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, result, reelId
                )
            }
            onLoadMore = { page, limit ->
                isLoadMore = true // Set flag before load more API call
                viewModel.hitGetReelCommentsApi(
                    "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken, reelId.toString(), page.toString(), limit.toString()
                )
            }
        }

        commentsBottomSheetFragment.show(
            requireActivity().supportFragmentManager,
            "RoundedBottomSheet"
        )
    }
    /*private fun openCommentsBottomSheet(payload: Payload) {
        commentsBottomSheetFragment = RoundedBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable("comments", payload)
            }
            onCommentAction = { result ->
                viewModel.hitPostCommentApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, result, reelId)
            }
            onReplyAction = { result ->
                viewModel.hitReplyToCommentsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, result, reelId)
            }
        }

        commentsBottomSheetFragment.show(requireActivity().supportFragmentManager, "RoundedBottomSheet")
    }*/


    override fun onPause() {
        if (::exoPlayer.isInitialized) {
            RTVariable.REELS_PLAYING_POSITION = exoPlayer.currentPosition
            exoPlayer.pause()
        }
        val position = binding.viewPager.currentItem
        Log.e("LAST POSITION", "LAST POSITION>>> "+position)
        Log.e("LAST POSITION", "LAST POSITION>>> "+reelId)
        //RTVariable.REELS_LAST_POSITION = currentPage
        UserDataManager.setReelsPosition(requireContext(), position)
        super.onPause()
    }

    override fun onDestroyView() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if(!RTVariable.IS_PROFILE_POST_LIST){
            RTVariable.IS_PROFILE_POST_LIST = false
            if(UserDataManager.get_postCommentShow(requireContext())){
                binding.swipeRefresh.isRefreshing = false
                UserDataManager.postCommentIsShow(requireContext(), false)
                //openCommentsBottomSheet(viewModel2.commentPayloadCache ?: Payload())
                //retainCommentBoxData(requireContext(), viewModel.posr_id, "1", "10")
                val cached = CommentPrefs.get2Payload(requireContext())

                if (cached != null) {
                    viewModel.commentPayloadCache = cached
                    RTVariable.IS_FROM_RESUME = true
                    openCommentsBottomSheet(cached)
                }
            }
        }
    }

}