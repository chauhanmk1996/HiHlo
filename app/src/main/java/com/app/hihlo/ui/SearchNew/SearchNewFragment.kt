package com.app.hihlo.ui.SearchNew

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.mutableIntListOf
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentSearchNewBinding
import com.app.hihlo.model.add_story.request.AddStoryRequest
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.gender_list.Gender
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.home.response.UserDetails
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.search_user_list.response.SearchUserListResponse
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.selectedGender
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.adapter.AdapterHomeCreators
import com.app.hihlo.ui.home.adapter.AdapterHomeGenders
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.fragment.HomeFragmentDirections
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.search.adapter.SearchAdapter
import com.app.hihlo.ui.search.fragment.SearchFragmentDirections
import com.app.hihlo.ui.search.view_model.SearchViewModel
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.app.hihlo.utils.CommonUtils.dpToPx
import com.app.hihlo.utils.MediaUtils
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.ReusablePopup
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.common.ScrollDirectionListener
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.app.hihlo.model.story_response.StoryUser
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.HomeNew.adapter.StatusAdapter
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.utils.getLength
import com.app.hihlo.utils.getString
import kotlin.getValue

class SearchNewFragment : BaseFragment<FragmentSearchNewBinding>() {

    private var myStoryData: MyStory = MyStory()
    private var isMediaUploaded: Int = -1
    private lateinit var adapterHomePosts: AdapterHomeCreators
    private val viewModel: HomeViewModel by viewModels()
    private var isLoading = false
    private var currentPage = 1
    private var isRefreshedFromMenu = false
    private var creatorsList: MutableList<Post> = mutableListOf()
    private var isHomeDataLoaded = false
    private var allStory: List<Story>? = null
    private var scrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var scrollChangedListener2: ViewTreeObserver.OnScrollChangedListener? = null
    private val viewModel2: SearchViewModel by viewModels()
    lateinit var search_adapter: SearchAdapter
    private var isSearchStarted = false
    private var userList: MutableList<SearchUserListResponse.Payload.User> = mutableListOf()
    private var scrollListener: ScrollDirectionListener? = null
    private var scrollListener2: ScrollDirectionListener? = null

    private var genderList: List<Gender>? = null
    var FIRSTVisiblePosition = -1
    var offsetY = 0
    var makeRefresh: Boolean = false
    private var searchJob: Job? = null

    private val viewModel6: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StoryUser>

    override fun getLayoutId(): Int = R.layout.fragment_search_new

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        onClick()
        binding.swipeRefresh.setOnRefreshListener {
            binding.searchRecycler.visibility = View.GONE
            binding.creatorsRecycler.visibility = View.VISIBLE
            isSearchStarted = false
            refreshData()
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "home_click",
            viewLifecycleOwner
        ) { _, _ ->
            Log.i("TAG", "onViewCreated: homeIconTap")
        }

        keyboardListener()
        setObserver()
        setPagination()
        setPagination2()
        setupScrollListener()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(500)
                    val isKeyboardOpen = binding.root.isKeyboardVisible()
                    if (isKeyboardOpen) {
                        //Log.e("KEYBOARD", "Keyboard is OPEN")
                    } else {
                        val query = binding.searchEdittext.text?.toString()?.trim() ?: ""
                        if (query.isEmpty()) {
                            isSearchStarted = false
                        }
                    }

                    if (RTVariable.IS_STATUS_VIEWER_ACTIVATED) {
                        RTVariable.IS_STATUS_VIEWER_ACTIVATED = false
                        getRefreshStory(1, 0)
                        getRefreshMainStory(0)
                    }
                }
            }
        }
        binding.swipeRefresh.isEnabled = false
        scrollChangedListener?.let {
            binding.nestedScrollView.viewTreeObserver.addOnScrollChangedListener(it)
        }
        scrollChangedListener2?.let {
            binding.nestedScrollView2.viewTreeObserver.addOnScrollChangedListener(it)
        }
        view.postDelayed({
            (requireActivity() as HomeActivity).fullyResetFloatingButton()
        }, 100)
        showSearchBar()
        lastScrollY = 0
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "self",
            viewLifecycleOwner
        ) { _, _ ->
            Log.i("TAG", "onViewCreated: searchIconTap")
            makeRefresh = true
            binding.nestedScrollView.post {
                binding.nestedScrollView.smoothScrollTo(0, 0)
                //refreshData2()
                binding.searchEdittext.setText("")
                search_adapter.clearList()
                isSearchStarted = false
                binding.nestedScrollView2.visibility = View.GONE
                binding.nestedScrollView.visibility = View.VISIBLE
                binding.searchRecycler.visibility = View.GONE
                binding.creatorsRecycler.visibility = View.VISIBLE
                binding.allButtonContainer.isVisible = true
                //binding.homeFilterGenderRecycler.isVisible=false
                binding.crossButton.isVisible = false
                binding.backBtn.isVisible = false
                RTVariable.SEARCH_TEXT = ""
                RTVariable.IS_USER_SEARCH_STARTED = false
                val text = binding.searchEdittext.text.toString().trim()
                if (text.isEmpty()) {
                    val imm =
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchEdittext.windowToken, 0)
                }
            }
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "other",
            viewLifecycleOwner
        ) { _, _ ->
            Log.i("TAG", "onViewCreated: chatIconTap")
            RTVariable.SEARCH_SELF_CLICKED = 0
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
                    search_adapter.updateStories(viewModel.stories)
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

    fun View.isKeyboardVisible(): Boolean {
        val rect = android.graphics.Rect()
        this.getWindowVisibleDisplayFrame(rect)
        val screenHeight = this.rootView.height
        val keypadHeight = screenHeight - rect.bottom
        return keypadHeight > screenHeight * 0.15
    }

    override fun initView(savedInstanceState: Bundle?) {
        isSearchStarted = false
        viewModel.hitGenderListApi()

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.creatorsRecycler.layoutManager = layoutManager
        adapterHomePosts = AdapterHomeCreators(mutableListOf()) { post, click, position ->
            when (click) {
                0 -> {
                    creatorsList.toTypedArray().forEachIndexed { index, creator ->
                        Log.e("TAG", "Home success: [$index] = $creator")
                    }
                    Log.e("TAG", "Home success: ${Posts()} || ${position.toString()}")
                    //findNavController().navigate(SearchNewFragmentDirections.actionSearchNewFragmentToUserPostListFragment(homePosts = creatorsList.toTypedArray(), profilePosts = Posts(), from = "home", position = position.toString()))
                    findNavController().navigate(
                        SearchNewFragmentDirections.actionSearchNewFragmentToProfileFragment(
                            "0",
                            post.user_id.toString()
                        )
                    )
                }

                1 -> {
                    (requireActivity() as HomeActivity).hideNavigationView()
                    findNavController().navigate(
                        SearchNewFragmentDirections.actionSearchNewFragmentToProfileFragment(
                            "0",
                            post.user_id.toString()
                        )
                    )
                }
            }
        }
        binding.creatorsRecycler.adapter = adapterHomePosts

        search_adapter = SearchAdapter(mutableListOf()) { position, click, clickedView ->
            when (click) {
                0 -> {
                    val bundle = Bundle().apply {
                        putParcelableArrayList(
                            "storyList",
                            ArrayList(
                                listOf<Story>(mapUserToStory(RTVariable.users_List[position]))
                                    ?: emptyList()
                            )
                        )
                        putInt("position", 0)
                    }
                    try {
                        findNavController().navigate(R.id.secondStoryFragment, bundle)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT)
                            .show()
                    }
//                    findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToStoryFragment(isMyStory = "0", myStoryData = MyStory(), otherStoryData =  mapUserToStory(userList[position])))
                }

                1 -> {
//                    val bundle = Bundle()
//                    bundle.putParcelable("userDetail",mapUserToUserDetailsX(userList[position]))
//                    findNavController().navigate(R.id.action_searchFragment_to_profileFragment, bundle)

                    findNavController().navigate(
                        SearchNewFragmentDirections.actionSearchNewFragmentToProfileFragment(
                            "0",
                            RTVariable.users_List[position].id.toString()
                        )
                    )

                }

                2 -> {
                    getSendFollow(RTVariable.USER_ID)
                }

                3 -> {
                    getSendUnFollow(RTVariable.USER_ID)
                }

                4 -> {

                    val targetUserId = RTVariable.USER_ID.toInt().toString()

                    // Safe check: if statusListGlobal is null or empty, show retry message and re-fetch
                    if (statusListGlobal == null || statusListGlobal!!.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Loading stories, please wait...",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel6.hitStatusDataApi(
                            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                requireContext(), LOGIN_DATA
                            )?.payload?.authToken, "0"
                        )
                        // No return – just don't navigate. The lambda will finish normally.
                    } else {
                        if (statusListGlobal!!.isEmpty()) {
                            return@SearchAdapter
                        }
                        val location = IntArray(2)
                        clickedView.getLocationOnScreen(location)
                        val centerX = location[0] + clickedView.width / 2
                        val centerY = location[1] + clickedView.height / 2
                        val intent = Intent(requireContext(), PlayStatusActivity::class.java)
                        val json = Gson().toJson(RTVariable.statusListGlobal)
                        intent.putExtra("story_list", json)
                        intent.putExtra("is_play_single", true)
                        intent.putExtra("user_id", targetUserId)
                        intent.putExtra("start_x", centerX)
                        intent.putExtra("start_y", centerY)
                        intent.putExtra("start_width", clickedView.width)
                        intent.putExtra("start_height", clickedView.height)
                        startActivity(intent)
                        //requireActivity().overridePendingTransition(R.anim.slide_up, 0)
                        requireActivity().overridePendingTransition(0, 0)
                    }
                }
            }

        }
        binding.searchRecycler.adapter = search_adapter

        RTVariable.postsCache.clear()
        viewModel.currentPage = 1
        hitSearchUserApi()
        hitServiceListApi(viewModel.currentPage, selectedGender)

        if (RTVariable.IS_SEARCH_MAIN_LOADED) {
            Log.e("IS_SEARCHING", "IS_SEARCHING>>> " + RTVariable.IS_USER_SEARCH_STARTED)
            Log.e(
                "IS_SEARCH_MAIN_LOADED",
                "IS_SEARCH_MAIN_LOADED>>> I " + RTVariable.postsCache.toMutableList()
            )
            binding.swipeRefresh.isEnabled = false
            binding.allButtonContainer.isVisible = true
            //binding.homeFilterGenderRecycler.isVisible=false
            binding.crossButton.isVisible = false
            binding.backBtn.isVisible = false
            adapterHomePosts.clearList()
            adapterHomePosts.updateList(RTVariable.postsCache.toMutableList())
            isLoading = true
            viewModel.currentPage = RTVariable.SEARCH_MAIN_CURRENT_PAGE
            binding.nestedScrollView2.visibility = View.GONE
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.searchRecycler.visibility = View.GONE
            binding.creatorsRecycler.visibility = View.VISIBLE

            if (RTVariable.IS_USER_SEARCH_STARTED) {
                binding.searchEdittext.setText(RTVariable.SEARCH_TEXT)
                binding.allButtonContainer.isVisible = false
                //binding.homeFilterGenderRecycler.isVisible=false
                binding.crossButton.isVisible = true
                binding.backBtn.isVisible = true
                search_adapter.clearList()
                search_adapter.updateList(RTVariable.users_List)
                binding.nestedScrollView2.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.GONE
                binding.searchRecycler.visibility = View.VISIBLE
                binding.creatorsRecycler.visibility = View.GONE
            }
        }

        if (!RTVariable.IS_SEARCH_MAIN_LOADED) {
            binding.nestedScrollView2.visibility = View.GONE
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.searchRecycler.visibility = View.GONE
            binding.creatorsRecycler.visibility = View.VISIBLE
        }

        binding.searchEdittext.doAfterTextChanged {
            val query = it?.toString()?.trim() ?: ""
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(400)
                if (query.isEmpty()) {
                    isSearchStarted = false
                    RTVariable.SEARCH_TEXT = ""
                    RTVariable.IS_USER_SEARCH_STARTED = false
                    binding.creatorsRecycler.visibility = View.VISIBLE
                    search_adapter.clearList()
                    search_adapter.updateList(mutableListOf())
                } else {
                    isSearchStarted = true
                    RTVariable.SEARCH_TEXT = query
                    RTVariable.IS_USER_SEARCH_STARTED = true
                    binding.nestedScrollView2.visibility = View.VISIBLE
                    binding.nestedScrollView.visibility = View.GONE
                    binding.crossButton.visibility = View.VISIBLE
                    binding.searchRecycler.visibility = View.VISIBLE
                    binding.creatorsRecycler.visibility = View.GONE
                    binding.allButtonContainer.isVisible = false
                    //binding.homeFilterGenderRecycler.isVisible=false
                    RTVariable.users_List.clear()
                    search_adapter.clearList()
                    search_adapter.updateList(mutableListOf())
                    hitSearchUserApi()
                }
            }
        }
    }

    private fun getSendFollow(user_id: String) {
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
                    hitSearchUserApi()
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
            RTVariable.USER_ID = ""
        }
    }

    private fun getSendUnFollow(user_id: String) {
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
                    hitSearchUserApi()
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
            RTVariable.USER_ID = ""
        }
    }

    private fun refreshData() {
        Handler(Looper.getMainLooper()).postDelayed({
            RTVariable.postsCache.clear()
            viewModel.currentPage = 1
            selectedGender = 0
            binding.allButton.text = "All"
            hitServiceListApi(viewModel.currentPage, selectedGender)
            binding.swipeRefresh.isRefreshing = false
            isSearchStarted = false
            binding.nestedScrollView2.visibility = View.GONE
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.searchRecycler.visibility = View.GONE
            binding.creatorsRecycler.visibility = View.VISIBLE
            //binding.homeFilterGenderRecycler.isVisible=false
        }, 1000)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onClick() {
        binding.mainLayout.setOnClickListener {
            //binding.homeFilterGenderRecycler.isVisible=false
        }
        binding.main.setOnClickListener {
            //binding.homeFilterGenderRecycler.isVisible=false
        }

        binding.allButtonContainer.setOnClickListener {
            showGenderPopup(requireActivity(), binding.allButton)
        }
        binding.searchEdittext.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.allButtonContainer.isVisible = false
                //binding.homeFilterGenderRecycler.isVisible=false
                binding.crossButton.isVisible = true
                binding.backBtn.isVisible = true
                binding.searchEdittext.apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    setSelection(getLength())
                }
            }
            false
        }
        binding.searchEdittext.setOnClickListener {
            binding.allButtonContainer.isVisible = false
            //binding.homeFilterGenderRecycler.isVisible=false
            binding.crossButton.isVisible = true
            binding.backBtn.isVisible = true
            binding.searchEdittext.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
                setSelection(getLength())
            }
        }
        binding.crossButton.setOnClickListener {
            binding.searchEdittext.setText("")
            //search_adapter.clearList()
            isSearchStarted = false
            //binding.searchRecycler.visibility = View.GONE
            //binding.creatorsRecycler.visibility = View.VISIBLE
            //binding.allButtonContainer.isVisible = true
            //binding.homeFilterGenderRecycler.isVisible=false
        }
        binding.backBtn.setOnClickListener {
            binding.searchEdittext.setText("")
            search_adapter.clearList()
            isSearchStarted = false
            binding.nestedScrollView2.visibility = View.GONE
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.searchRecycler.visibility = View.GONE
            binding.creatorsRecycler.visibility = View.VISIBLE
            binding.allButtonContainer.isVisible = true
            //binding.homeFilterGenderRecycler.isVisible=false
            binding.crossButton.isVisible = false
            binding.backBtn.isVisible = false
            val text = binding.searchEdittext.text.toString().trim()
            if (text.isEmpty()) {
                val imm =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEdittext.windowToken, 0)
            }
        }
    }

    private fun showGenderPopup(activity: Activity, anchorView: View) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.custom_popup_menu, null)
        val popupWindow = PopupWindow(
            popupView,
            (140 * activity.resources.displayMetrics.density).toInt() + 8,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        val menuContainer: LinearLayout = popupView.findViewById(R.id.menuContainer)
        menuContainer.removeAllViews()
        val genders = genderList.orEmpty()
        if (genders.isEmpty()) {
            val emptyView = inflater.inflate(R.layout.custom_popup_menu_item, null)
            val menuText: TextView = emptyView.findViewById(R.id.menuText)
            val divider: View? = emptyView.findViewById(R.id.view_line)
            divider?.visibility = View.GONE
            menuText.text = "No genders available"
            emptyView.isClickable = false
            menuContainer.addView(emptyView)
        } else {
            val itemCount = genders.size
            for ((index, gender) in genders.withIndex()) {
                val menuItemView = inflater.inflate(R.layout.custom_popup_menu_item, null)
                val menuText: TextView = menuItemView.findViewById(R.id.menuText)
                menuText.text = gender.gender_name
                val divider: View? = menuItemView.findViewById(R.id.view_line)
                if (index == itemCount - 1) {
                    divider?.visibility = View.GONE
                } else {
                    divider?.visibility = View.VISIBLE
                }
                menuItemView.setOnClickListener {
                    popupWindow.dismiss()
                    viewModel.postsCache.clear()
                    viewModel.currentPage = 1
                    if (index == 0) {
                        binding.allButton.text = gender.gender_name
                        selectedGender = 0
                        viewModel.filterById = gender.id
                        viewModel.filterByName = gender.gender_name
                        //Toast.makeText(requireContext(), "Selected: ${gender.gender_name} || ${selectedGender}", Toast.LENGTH_SHORT).show()
                        hitServiceListApi(viewModel.currentPage, selectedGender)
                    } else {
                        onGenderSelected(gender)
                    }
                }
                menuContainer.addView(menuItemView)
            }
        }
        popupWindow.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popupWindow.showAsDropDown(anchorView)
    }

    private fun onGenderSelected(gender: Gender) {
        binding.allButton.text = gender.gender_name
        selectedGender = gender.id
        RTVariable.postsCache.clear()
        viewModel.currentPage = 1
        viewModel.filterById = gender.id
        viewModel.filterByName = gender.gender_name
        hitServiceListApi(viewModel.currentPage, selectedGender)
    }

    private var isBottomBarVisible = true
    private var lastScrollY = 0

    private fun setPagination() {
        scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            val scrollView = binding?.nestedScrollView ?: return@OnScrollChangedListener
            val contentView = scrollView.getChildAt(scrollView.childCount - 1)
            val scrollY = scrollView.scrollY
            val scrollViewHeight = scrollView.height
            val contentBottom = contentView.bottom
            val diff = contentBottom - (scrollY + scrollViewHeight)
            Log.d("SCROLL_MANUAL", "scrollY: $scrollY, contentBottom: $contentBottom, diff: $diff")
            UserDataManager.postSearchCreatorScrollY(binding.root.context, scrollY)
            if (diff <= 300 && diff != 0) { // `300` is a buffer to pre-load before actual bottom
                if (isLoading) {
                    viewModel.currentPage++
                    hitServiceListApi(viewModel.currentPage, selectedGender)
                }
                isLoading = false
                if (isBottomBarVisible) {
                    //scrollListener?.hideBottomElements()
                    //hideSearchBar()
                    isBottomBarVisible = false
                }
            }
            when {
                scrollY > lastScrollY + 10 -> {     // scrolling down + small threshold to avoid jitter
                    if (isBottomBarVisible) {
                        //scrollListener?.hideBottomElements()
                        //hideSearchBar()
                        //binding.homeFilterGenderRecycler.isVisible=false
                        isBottomBarVisible = false
                    }
                }

                scrollY < lastScrollY - 10 -> {     // scrolling up
                    if (!isBottomBarVisible) {
                        //scrollListener?.showBottomElements()
                        //showSearchBar()
                        //binding.homeFilterGenderRecycler.isVisible=false
                        isBottomBarVisible = true
                    }
                }
            }
            lastScrollY = scrollY
        }

    }

    private fun setPagination2() {
        scrollChangedListener2 = ViewTreeObserver.OnScrollChangedListener {
            val scrollView = binding?.nestedScrollView2 ?: return@OnScrollChangedListener
            val contentView = scrollView.getChildAt(scrollView.childCount - 1)
            val scrollY = scrollView.scrollY
            val scrollViewHeight = scrollView.height
            val contentBottom = contentView.bottom
            val diff = contentBottom - (scrollY + scrollViewHeight)
            Log.d(
                "SCROLL_STATE",
                "scrollY 2nd Nested: $scrollY, contentBottom: $contentBottom, diff: $diff"
            )
            UserDataManager.postSearchUserScrollY(binding.root.context, scrollY)
            val pos = UserDataManager.get_SearchUserScrollY(binding.root.context)
            Log.d("SCROLL_STATE", "scrollY 2nd Nested >>> " + pos)
        }

    }

    private val scrollThreshold = 5
    private fun setupScrollListener() {
        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val delta = scrollY - lastScrollY
            when {
                // Scrolling DOWN
                delta > scrollThreshold -> {
                    //hideSearchBar()
                    //binding.homeFilterGenderRecycler.isVisible=false
                }

                delta < -scrollThreshold -> {
                    //showSearchBar()
                    //binding.homeFilterGenderRecycler.isVisible=false
                }
            }

            lastScrollY = scrollY
        })
    }

    private val animationDuration = 180L

    private fun showSearchBar() {
        if (binding.headerLayout2.translationY < 0) {
            binding.headerLayout2.animate()
                .translationY(0f)
                .setDuration(animationDuration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            binding.headerLayout2.isVisible = true
            binding.swipeRefresh.setPadding(0, 0, 0, binding.swipeRefresh.paddingBottom)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        scrollListener = context as? ScrollDirectionListener
        scrollListener2 = context as? ScrollDirectionListener
    }

    override fun onDetach() {
        super.onDetach()
        scrollListener = null
        scrollListener2 = null
    }

    override fun onResume() {
        super.onResume()
        Log.e("IS_SEARCHING", "IS_SEARCHING>>> " + RTVariable.IS_USER_SEARCH_STARTED)
        if (RTVariable.IS_USER_SEARCH_STARTED) {
            val scrollY = UserDataManager.get_SearchUserScrollY(binding.root.context)
            binding.nestedScrollView2.post {
                binding.nestedScrollView2.scrollTo(0, scrollY)
            }
        } else {
            val scrollY = UserDataManager.get_SearchCreatorScrollY(binding.root.context)
            binding.nestedScrollView.post {
                binding.nestedScrollView.scrollTo(0, scrollY)
            }
        }
        //hitServiceListApi(viewModel.currentPage, selectedGender)
    }

    override fun onPause() {
        scrollChangedListener?.let {
            if (binding.nestedScrollView.viewTreeObserver.isAlive) {
                binding.nestedScrollView.viewTreeObserver.removeOnScrollChangedListener(it)
            }
        }
        scrollChangedListener2?.let {
            if (binding.nestedScrollView2.viewTreeObserver.isAlive) {
                binding.nestedScrollView2.viewTreeObserver.removeOnScrollChangedListener(it)
            }
        }
        RTVariable.SEARCH_MAIN_CURRENT_PAGE = viewModel.currentPage
        Log.e("IS_SEARCHING", "IS_SEARCHING>>> " + RTVariable.IS_USER_SEARCH_STARTED)
        Log.e("IS_SEARCHING", "IS_SEARCHING>>> " + RTVariable.SEARCH_TEXT)
        scrollChangedListener = null
        scrollChangedListener2 = null
        super.onPause()
    }

    fun hitSearchUserApi() {
        Log.e("TAG", "get search list: ${binding.searchEdittext.getString()}")
        viewModel2.hitSearchUsersList(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken ?: "", "1", "20", binding.searchEdittext.getString()
        )
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
            )?.payload?.authToken, page.toString(), "10", genderId.toString()
        )
        viewModel6.hitStatusDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, "0"
        )
    }

    private fun setObserver() {
        viewModel.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            isRefreshedFromMenu = false
                            isHomeDataLoaded = true
                            isMediaUploaded = it.data.payload.is_story_uploaded
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            Log.e("TAG", "setObserver: $allStory")
                            if (it.data.payload.posts.isNotEmpty()) {
                                isLoading = true
                                if (viewModel.currentPage == 1) {
                                    RTVariable.postsCache.clear()
                                }

                                it.data.payload.posts.let { list ->
                                    val uniqueUsersPostList =
                                        list.groupBy { it.user_id }.map { (_, posts) ->
                                            // First try to get cover post
                                            posts.find { it.is_cover == "TRUE" }

                                            // If no cover post exists, take first post
                                                ?: posts.first()
                                        }
                                    RTVariable.postsCache.addAll(uniqueUsersPostList)
                                }

                                if (viewModel.currentPage == 1) {
                                    if (RTVariable.postsCache.size > 0) {
                                        adapterHomePosts.clearList()
                                        adapterHomePosts.updateList(RTVariable.postsCache.toMutableList())
                                    } else {
                                        adapterHomePosts.clearList()
                                    }
                                } else {
                                    adapterHomePosts.updateList(RTVariable.postsCache.toMutableList())
                                }
                                search_adapter.addStory(
                                    listOf(
                                        it.data.payload.my_story ?: MyStory()
                                    ), it.data.payload.stories
                                )
                            }
                            RTVariable.IS_SEARCH_MAIN_LOADED = true
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }

                    if (!makeRefresh) {
                        ProcessDialog.dismissDialog(true)
                    }
                    //binding.progressBar.isVisible=false
                }

                Status.LOADING -> {
                    if (viewModel.currentPage == 1) {
                        if (!makeRefresh) {
                            ProcessDialog.showDialog(requireContext(), true)
                        }
                    }
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    if (!makeRefresh) {
                        ProcessDialog.dismissDialog(true)
                    }
                    //binding.progressBar.isVisible=false
                }
            }
        }

        viewModel.addStoryLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Add story success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            hitServiceListApi(currentPage, selectedGender)
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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

        viewModel.getGenderLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels Gender success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            var data = it.data.payload.genderList
                            genderList = data
                            Log.d("TAG", "setOsdcdcbserver: ${it.data.payload}")
                            if (selectedGender == null) {
                                binding.allButton.text = data[0].gender_name
                                viewModel.filterById = data[0].id
                                viewModel.filterByName = data[0].gender_name
                            } else {
                                binding.allButton.text = data[selectedGender ?: 0].gender_name
                            }
                            currentPage = 1
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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

        viewModel2.getUsersListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get search list: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            //userList = it.data.payload.users.toMutableList()
                            RTVariable.users_List.addAll(it.data.payload.users.toMutableList())
                            search_adapter.clearList()
                            search_adapter.updateList(RTVariable.users_List)
                            if (isSearchStarted) {
                                binding.searchRecycler.visibility = View.VISIBLE
                                binding.creatorsRecycler.visibility = View.GONE
                            }
                            RTVariable.IS_SEARCH_MAIN_LOADED = true
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
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
    }

    private fun keyboardListener() {
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets ->
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                if (imeVisible) {
                    requireActivity().findViewById<View>(R.id.bottomAppBar).visibility = View.GONE
                    requireActivity().findViewById<View>(R.id.floatingbtn).visibility = View.GONE
                    requireActivity().findViewById<View>(R.id.imgBtn).visibility = View.GONE
                } else {
                    requireActivity().findViewById<View>(R.id.bottomAppBar).visibility =
                        View.VISIBLE
                    requireActivity().findViewById<View>(R.id.floatingbtn).visibility = View.VISIBLE
                    requireActivity().findViewById<View>(R.id.imgBtn).visibility = View.VISIBLE
                }
                insets
            }
        }
    }

    fun mapUserToStory(user: SearchUserListResponse.Payload.User): Story {
        return Story(
            asset_type = user.myStory.asset_type,
            asset_url = user.myStory.url,
            id = user.myStory.id,
            is_seen = user.is_seen,
            userDetail = UserDetails(
                name = user.name,
                username = user.username,
                profile_image = user.profile_image,
                city = user.city,
                country = user.country,
                gender_id = null, // not present in User
                gender_name = user.gender_name,
                email = user.email,
                role = user.role,
                is_creator = user.is_creator.toString() // converting Boolean to String
            )
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Always call super
        Log.i("TAG", "onActivityResult: " + "outside")
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

        // Initialize TransferUtility
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()

        com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler.getInstance(
            context
        )

        // Start the upload
        val uploadObserver = transferUtility.upload(bucketName, objectKey, file)
        ProcessDialog.showDialog(requireContext(), true)
        // Listen to upload events
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
                    val caption = "Test Caption"
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

    private fun uploadImage(imageFile: File, assetType: String) {
        val s3Data = Preferences.getCustomModelPreference<LoginResponse>(
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