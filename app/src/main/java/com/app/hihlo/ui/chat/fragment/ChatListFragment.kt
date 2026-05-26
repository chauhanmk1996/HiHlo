package com.app.hihlo.ui.chat.fragment

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentChatListBinding
import com.app.hihlo.model.get_recent_chat.response.RecentChat
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.home.response.UserDetails
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.story_response.StoryUser
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.calling.CallForegroundService
import com.app.hihlo.ui.calling.activity.OldIncomingCallActivity
import com.app.hihlo.ui.calling.activity.OldOutgoingCallActivity
import com.app.hihlo.ui.calling.activity.OutgoingVideoCallActivity
import com.app.hihlo.ui.calling.activity.SingleVideoCallActivity
import com.app.hihlo.ui.calling.view_model.CallStateViewModel
import com.app.hihlo.ui.chat.adapter.AdapterChatList
import com.app.hihlo.ui.chat.adapter.ChatListViewPagerAdapter
import com.app.hihlo.ui.chat.view_model.RecentChatListViewModel
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.abs

class ChatListFragment : BaseFragment<FragmentChatListBinding>() {
    private val viewModel: RecentChatListViewModel by viewModels()
    private lateinit var viewPagerAdapter: ChatListViewPagerAdapter
    var isInboxSelected = true
    var recentList: List<RecentChat> = emptyList()
    var requestUsersList: List<RecentChat> = emptyList()
    private var halfWidth = 0
    private var isRightSelected = false
    var deletedUserId = ""
    private val authToken: String? by lazy {
        Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken
    }
    private val callStateViewModel: CallStateViewModel by activityViewModels()
    private val viewModel5: HomeViewModel by viewModels()
    private val viewModel6: StatusViewModel by activityViewModels()
    private var myStoryData: MyStory = MyStory()
    private var allStory: List<Story>? = null
    private lateinit var statusListGlobal: List<StoryUser>


    companion object {
        const val TYPE_INBOX = "inbox"
        const val TYPE_REQUEST = "request"
    }

    var tabPos: Int = 0

    private val searchTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            binding.crossButton.isVisible = binding.searchEdittext.getString().isNotEmpty()
            val query = s.toString().trim()
            val currentPosition = binding.viewPager.currentItem
            val filteredList = if (currentPosition == 0) {
                searchChats(query)
            } else {
                searchRequestChats(query)
            }
            Log.i("TAG", "Search query: $query, Tab: $currentPosition, Filtered list size: ${filteredList.size}")
            filteredList.forEach { Log.i("TAG", "Filtered item: ${it.userDetails?.username}") }
            viewPagerAdapter.updateList(filteredList, currentPosition)
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            isInboxSelected = position == 0
            // Clear search and reset list
            binding.searchEdittext.setText("")
            val listToShow = if (position == 0) RTVariable.chat_recentList else RTVariable.chat_requestUsersList
            viewPagerAdapter.updateList(listToShow, position)
            if (position == 0) {
                moveSelectorToLeft()
            } else {
                moveSelectorToRight()
            }
            Log.d("MyFragment", "Page changed to: $position")
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

    }

    fun ViewPager2.reduceDragSensitivity(factor: Int = 4) {
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(this) as RecyclerView
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * factor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onLongTap(click: Int, position: Int, data: RecentChat, view: View) {
        val popup = if (isInboxSelected) {
            ReusablePopup(
                context = requireContext(),
                anchorView = view,
                onOption1Click = {
                    viewModel.hitHandleMessageRequestDataApi("Bearer $authToken", chatId = data.chatId.toString(), action = "send_to_request")
                },
                onOption2Click = {
                    openDeleteCompleteConfirmationDialog(data.chatId.toString())
                },
                {},
                {},
                "Move to Request",
                "<font color='#FF0000'>Delete</font>",
                "Cancel",
                option1ImageRes = R.drawable.move_to_request,
                option2ImageRes = R.drawable.delete,
                option3ImageRes = R.drawable.cancel,
                alignEnd = false
            )
        } else {
            ReusablePopup(
                context = requireContext(),
                anchorView = view,
                onOption1Click = {
                    openDeleteCompleteConfirmationDialog(data.chatId.toString())
                },
                onOption2Click = {},
                {},
                {},
                "Delete from both sides",
                "Cancel",
                option1ImageRes = R.drawable.delete,
                option2ImageRes = R.drawable.cancel,
                alignEnd = false
            )
        }
        popup.show()
    }

    fun searchChats(query: String): List<RecentChat> {
        return RTVariable.chat_recentList.filter { chat ->
            val username = chat.userDetails?.username
            val name = chat.userDetails?.name
            Log.i("TAG", "Checking chat: id=${chat.chatId}, username=$username, name=$name, query=$query")
            (username?.contains(query, ignoreCase = true) == true) ||
                    (name?.contains(query, ignoreCase = true) == true)
        }.also {
            Log.i("TAG", "searchChats: Input list size=${RTVariable.chat_recentList.size}, Filtered size=${it.size}")
        }
    }

    fun searchRequestChats(query: String): List<RecentChat> {
        return RTVariable.chat_requestUsersList.filter { chat ->
            val username = chat.userDetails?.username
            Log.i("TAG", "Checking request chat: id=${chat.chatId}, username=$username, query=$query")
            username?.contains(query, ignoreCase = true) == true
        }.also {
            Log.i("TAG", "searchRequestChats: Input list size=${RTVariable.chat_requestUsersList.size}, Filtered size=${it.size}")
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setObserver() {
        viewModel.getRecentChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get recent success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1 && it.data.code == 200) {
                        RTVariable.chat_recentList = it.data.payload.recentChats
                        Log.i("TAG", "Recent chats updated: size=${RTVariable.chat_recentList.size}")
                        RTVariable.chat_recentList.forEach { chat ->
                            Log.i("TAG", "Recent chat: id=${chat.chatId}, username=${chat.userDetails?.username}")
                        }
                        viewPagerAdapter.updateList(RTVariable.chat_recentList, 0)
                        viewModel.isLoaded = true
                        if (UserPreference.CHAT_PUSH_NOTIFICATION_ID?.isNotEmpty() == true) {
                            val targetChatId = UserPreference.CHAT_PUSH_NOTIFICATION_ID?.toInt()
                            Log.i("TAG", "setObserver: $targetChatId")
                            val matchedChat = RTVariable.chat_recentList.firstOrNull { it.chatId == targetChatId }
                            matchedChat?.let { chat ->
                                UserPreference.CHAT_PUSH_NOTIFICATION_ID = null
                                Log.d("Match", "Chat matched: ${chat.message}")
                                val bundle = Bundle().apply {
                                    putParcelable("userDetail", chat.userDetails)
                                }
                                findNavController().navigate(R.id.action_chatListFragment_to_chatFragment, bundle)
                            }
                        }
                        RTVariable.IS_CHAT_LOADED = true
                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "get recent Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.getRequestChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get request success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1 && it.data.code == 200) {
                        RTVariable.chat_requestUsersList = it.data.payload.recentChats
                        Log.i("TAG", "Request chats updated: size=${RTVariable.chat_requestUsersList.size}")
                        RTVariable.chat_requestUsersList.forEach { chat ->
                            Log.i("TAG", "Request chat: id=${chat.chatId}, username=${chat.userDetails?.username}")
                        }
                        viewPagerAdapter.updateList(RTVariable.chat_requestUsersList, 1)
                        viewModel.isLoaded = true
                        RTVariable.IS_CHAT_LOADED = true
                        binding.requestButton.text = "Request(${RTVariable.chat_requestUsersList.size})"
                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "", Toast.LENGTH_SHORT).show()
                    }
                    //ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "get recent Failed: ${it.message}")
                    //ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.getDeleteRecentChatLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get delete success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1 && it.data.code == 200) {
                        if (isInboxSelected) {
                            viewModel.hitGetRecentChatDataApi("Bearer $authToken", type = TYPE_INBOX)
                        } else {
                            viewModel.hitGetRequestChatDataApi("Bearer $authToken", type = TYPE_REQUEST)
                        }
                        viewModel.deleteChat(
                            sender = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId.toString(),
                            receiver = deletedUserId,
                            Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken ?: ""
                        )
                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.LOADING -> ProcessDialog.showDialog(requireContext(), true)
                Status.ERROR -> {
                    Log.e("TAG", "get delete Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.getHandleMessageRequestLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get handle request success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1 && it.data.code == 200) {
//                        moveSelectorToLeft()
                        isInboxSelected = true
                        setSelectedTab(true)
                        viewModel.hitGetRecentChatDataApi("Bearer $authToken", type = TYPE_INBOX)
                        viewModel.hitGetRequestChatDataApi("Bearer $authToken", type = TYPE_REQUEST)

                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> ProcessDialog.showDialog(requireContext(), true)
                Status.ERROR -> {
                    Log.e("TAG", "get handle Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel5.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            Log.e("TAG", "Home success: ${myStoryData}")
                            Log.e("TAG", "Home success: ${allStory}")
                            viewPagerAdapter?.updateStory(it.data.payload.stories)
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

    fun openDeleteCompleteConfirmationDialog(id: String) {
        showCustomDialogWithBinding(requireContext(), "Are you sure you want to delete this chat?",
            onYes = {
                deletedUserId = id
                viewModel.hitDeleteRecentChatDataApi("Bearer $authToken", SaveRecentChatRequest(toUserId = id))
            },
            onNo = {}
        )
    }

    fun mapRecentChatToStory(chat: RecentChat): Story {
        return Story(
            asset_type = chat.userDetails?.myStory?.asset_type ?: "",
            asset_url = chat.userDetails?.myStory?.url ?: "",
            created_at =  "",
            id = chat.chatId,
            is_seen =  0,
            userDetail = UserDetails(
                name = chat.userDetails?.name ?: "",
                username = chat.userDetails?.username ?: "",
                profile_image = chat.userDetails?.profileImage ?: chat.userDetails?.profile_image ?: "",
                city = chat.userDetails?.city ?: "",
                country = chat.userDetails?.country ?: "",
                gender_id = chat.userDetails?.gender ?: "",
                gender_name = chat.userDetails?.gender ?: "",
                email = chat.userDetails?.email ?: "",
                role = chat.userDetails?.role ?: "",
                is_creator = chat.userDetails?.isCreator?.toString() ?: ""
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchEdittext.addTextChangedListener(searchTextWatcher)
        viewPagerAdapter = ChatListViewPagerAdapter(RTVariable.chat_recentList, RTVariable.chat_requestUsersList, ::onLongTap) { click, position, data, view ->
            Log.i("TAG", "initView: click=$click, position=$position, data=$data")
            when (click) {
                0 -> {
                    data.userDetails?.let {
                        val bundle = Bundle().apply {
                            putParcelableArrayList("storyList", ArrayList(listOf(mapRecentChatToStory(data))))
                            putInt("position", 0)
                        }
                        try {
                            findNavController().navigate(R.id.secondStoryFragment, bundle)
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                            Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                1 -> {
                    Log.i("TAG", "setObserver: ${UserPreference.CHAT_PUSH_NOTIFICATION_ID}")
                    data.userDetails?.let {
                        val bundle = Bundle().apply {
                            putParcelable("userDetail", it)
                            putString("from", if (isInboxSelected) TYPE_INBOX else TYPE_REQUEST)
                            putString("chatId", data.chatId.toString())
                        }
                        findNavController().navigate(R.id.action_chatListFragment_to_chatFragment, bundle)
                    }
                }
                2 -> {
                    Log.i("TAG", "IMAGE CLICKED:")
                    if (statusListGlobal.isEmpty()) {
                        return@ChatListViewPagerAdapter
                    }
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val centerX = location[0] + view.width / 2
                    val centerY = location[1] + view.height / 2
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
                    intent.putExtra("start_width", view.width)
                    intent.putExtra("start_height", view.height)
                    startActivity(intent)
                    //requireActivity().overridePendingTransition(R.anim.slide_up, 0)
                    requireActivity().overridePendingTransition(0, 0)
                }
            }
        }
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.reduceDragSensitivity(4)
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
        setObserver()
        observeOngoingCall()
        callLayoutListener()
        binding.switchContainer.post {
            halfWidth = binding.switchContainer.width / 2
            binding.viewSelector.layoutParams = binding.viewSelector.layoutParams.apply {
                width = halfWidth
            }
            binding.viewSelector.requestLayout()

            if (!isInboxSelected) {
                binding.viewSelector.translationX = halfWidth.toFloat()
            }

            onClick()
            setupSwipeGesture()
        }
        requireActivity().supportFragmentManager.setFragmentResultListener("self", viewLifecycleOwner) { _, _ ->
            Log.i("TAG", "onViewCreated: chatIconTap")
            if(::viewPagerAdapter.isInitialized){
                var tapOption = ""
                if(tabPos == 0){
                    tapOption = "inbox"
                }else{
                    tapOption = "request"
                }
                UserDataManager.saveChatFromOtherScrollPosition(binding.root.context, tapOption, 1)
                val rv = binding.viewPager.getChildAt(tabPos) as RecyclerView
                viewPagerAdapter.scrollToTop(rv, tabPos, true)
            }
        }
        requireActivity().supportFragmentManager.setFragmentResultListener("other", viewLifecycleOwner) { _, _ ->
            Log.i("TAG", "onViewCreated: chatIconTap")
            RTVariable.CHAT_INSTANCE_KEY_ID = 0
//            if(::viewPagerAdapter.isInitialized){
//                val rv = binding.viewPager.getChildAt(tabPos) as RecyclerView
//                viewPagerAdapter.scrollToTop(rv, tabPos, true)
//            }
        }
        isInboxSelected = true
        setSelectedTab(true)
        if(!RTVariable.IS_CHAT_LOADED){
            viewModel.hitGetRecentChatDataApi("Bearer $authToken", type = TYPE_INBOX)
            viewModel.hitGetRequestChatDataApi("Bearer $authToken", type = TYPE_REQUEST)
//            RTVariable.IS_CHAT_CLICKED = true
//            RTVariable.IS_CHAT_OTHER = false
        }
        if(RTVariable.IS_CHAT_LOADED){
//            var tabSel = ""
//            if(tabPos==0){
//                tabSel = "inbox"
//            }else{
//                tabSel = "request"
//            }
//            UserDataManager.saveChatScrollPosition(
//                binding.root.context,
//                tabSel,
//                0 // store as int
//            )
            viewPagerAdapter.updateList(RTVariable.chat_recentList, 0)
            viewPagerAdapter.updateList(RTVariable.chat_requestUsersList, 1)
        }
        viewModel5.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "1", "10", "0")
        viewModel6.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
        getRefreshStory(1, 0)
        getRefreshMainStory(0)
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                while (true) {
//                    delay(300)
////                    if(RTVariable.IS_CHAT_CLICKED && !RTVariable.IS_CHAT_OTHER){
////                        RTVariable.IS_CHAT_CLICKED = false
////                        RTVariable.IS_CHAT_OTHER = false
////                        if(::viewPagerAdapter.isInitialized){
////                            val rv = binding.viewPager.getChildAt(tabPos) as RecyclerView
////                            viewPagerAdapter.scrollToTop(rv, tabPos, true)
////                        }
////                    }
//                }
//            }
//        }
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                while (true) {
//                    delay(300)
//                    runChatListAPI()
//                    runRequestListAPI()
////                    if(RTVariable.isBlocked){
////                        RTVariable.isBlocked = false
////                        runChatListAPI()
////                        runRequestListAPI()
////                        viewModel.hitGetRecentChatDataApi("Bearer $authToken", type = TYPE_INBOX)
////                        viewModel.hitGetRequestChatDataApi("Bearer $authToken", type = TYPE_REQUEST)
////                    }
//                }
//            }
//        }
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
//                    else{
//                        RTVariable.IS_STATUS_VIEWER_ACTIVATED = false
//                        //viewModel5.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
//                        getRefreshStory(1, 0)
//                        getRefreshMainStory(0)
//                    }
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
                    viewModel5.stories = response.payload.stories
                    viewPagerAdapter?.updateStory(viewModel5.stories)
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

    private fun runChatListAPI(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getRecentChats(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    fromUserId = "",
                    toUserId = "",
                    type = "inbox"
                )
                if (response.status == 1 && response.code == 200) {
                    RTVariable.chat_recentList = emptyList()
                    RTVariable.chat_recentList = response.payload.recentChats
                    Log.e("CCCC", "CCCC>>> "+RTVariable.chat_recentList)
                } else {
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun runRequestListAPI(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getRecentChats(
                    token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    fromUserId = "",
                    toUserId = "",
                    type = "request"
                )
                if (response.status == 1 && response.code == 200) {
                    RTVariable.chat_requestUsersList = emptyList()
                    RTVariable.chat_requestUsersList = response.payload.recentChats
                    Log.e("CCCC", "CCCC>>> R"+RTVariable.chat_requestUsersList)
                } else {
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun callLayoutListener() {
        binding.callLayout.setOnClickListener {
            val state = callStateViewModel.callState.value
            if (state != null && state.isOngoing) {
                val intent = Intent(requireContext(), if (state.incomingType=="incoming") OldIncomingCallActivity::class.java else OutgoingVideoCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(CallForegroundService.EXTRA_USER_NAME, state.userName)
                    putExtra(CallForegroundService.EXTRA_PROFILE_URL, state.profileUrl)
                    putExtra(CallForegroundService.INCOMING_OUTGOING, state.incomingType)
                    putExtra(CallForegroundService.CALL_TYPE, "video") // or audio, if needed
                    putExtra(CallForegroundService.Call_TIME, state.callStartTime.toString())
                }
                startActivity(intent)
            }
        }
    }

    private fun observeOngoingCall() {
        callStateViewModel.callState.observe(viewLifecycleOwner) { state ->
            if (state.isOngoing) {
                binding.callLayout.visibility = View.VISIBLE
                binding.callerName.text = state.userName

                Glide.with(this)
                    .load(state.profileUrl)
                    .placeholder(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(binding.callerProfile)

                // update duration every second
                viewLifecycleOwner.lifecycleScope.launch {
                    while (isActive && state.isOngoing) {
                        val elapsed = (SystemClock.elapsedRealtime() - state.callStartTime) / 1000
                        binding.callTimer.text =
                            String.format("%02d:%02d", elapsed / 60, elapsed % 60)
                        delay(1000)
                    }
                }
            } else {
                binding.callLayout.visibility = View.GONE
            }
        }
    }

    private fun onClick() {
        binding.apply {
            inboxButton.setOnClickListener {
                searchEdittext.setText("")
                viewPager.currentItem = 0
                moveSelectorToLeft()
            }
            requestButton.setOnClickListener {
                searchEdittext.setText("")
                viewPager.currentItem = 1
                moveSelectorToRight()
            }
            crossButton.setOnClickListener {
                searchEdittext.setText("")
            }
        }
    }

    private fun setupSwipeGesture() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) &&
                    kotlin.math.abs(diffX) > SWIPE_THRESHOLD &&
                    kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX > 0) { // Right swipe
                        moveSelectorToLeft()
                    } else { // Left swipe
                        moveSelectorToRight()
                    }
                    return true
                }
                return false
            }
        })

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun moveSelectorToLeft() {
        binding.viewSelector.animate()
            .translationX(0f)
            .setDuration(200)
            .start()
        isInboxSelected = true
        isRightSelected = false
        setSelectedTab(true)
    }

    private fun moveSelectorToRight() {
        binding.viewSelector.animate()
            .translationX(halfWidth.toFloat())
            .setDuration(200)
            .start()
        isInboxSelected = false
        isRightSelected = true
        setSelectedTab(false)
    }

    private fun setSelectedTab(isInboxSelected: Boolean) {
        binding.apply {
            Log.i("TAG", "setSelectedTab: $isInboxSelected")
            Log.i("TAG", "setSelectedTab: recentList size=${RTVariable.chat_recentList.size}")
            if (isInboxSelected) {
                tabPos = 0
//                if (recentList.isEmpty()) viewModel.hitGetRecentChatDataApi("Bearer $authToken", type = TYPE_INBOX)
                inboxButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.theme)
                requestButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black_303030)
            } else {
                tabPos = 1
//                if (requestUsersList.isEmpty()) viewModel.hitGetRequestChatDataApi("Bearer $authToken", type = TYPE_REQUEST)
                requestButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.theme)
                inboxButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black_303030)
            }
        }
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        binding.searchEdittext.removeTextChangedListener(searchTextWatcher)
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_chat_list
    }

}