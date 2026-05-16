package com.app.hihlo.ui.profile.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentFollowersBinding
import com.app.hihlo.model.follow.request.FollowRequest
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.story_response.StoryUser
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.home.fragment.HomeFragmentDirections
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.profile.adapter.AdapterFollowers
import com.app.hihlo.ui.profile.view_model.FollowersViewModel
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

class FollowersFragment : BaseFragment<FragmentFollowersBinding>() {
    private var screenCheck: String = ""
    private var isMyProfile: String = ""
    private var userId: String = ""
    private val args: FollowersFragmentArgs by navArgs()
    private val viewModel: FollowersViewModel by viewModels()
    private val viewModel5: HomeViewModel by viewModels()
    private lateinit var adapterFollowers: AdapterFollowers
    private var myStoryData: MyStory = MyStory()
    private var allStory: List<Story>? = null

    private val viewModel6: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StoryUser>


    override fun getLayoutId(): Int {
        return R.layout.fragment_followers
    }

    override fun initView(savedInstanceState: Bundle?) {
        screenCheck = args.screenCheck
        isMyProfile = args.isMyProfile
        userId = args.userId
        setUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        onClick()
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
                    adapterFollowers.updateStories(viewModel5.stories)
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

    private fun onClick() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        loadRcv()
    }
    private fun loadRcv(){
        binding.followersRecycler.layoutManager = LinearLayoutManager(requireActivity(),LinearLayoutManager.VERTICAL,false)
        adapterFollowers = AdapterFollowers(isMyProfile, ::getSelectedUser)
        adapterFollowers.from = screenCheck
        binding.followersRecycler.adapter = adapterFollowers
    }
    private fun getSelectedUser(userId: Int, isFollowing: Int, click:Int, view: View){
        when(click){
            0->{
                when(screenCheck){
                    "followers"->{
                        if (isFollowing==1){
                            viewModel.hitUnfollowUserDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, FollowRequest(unfollowId = userId.toString()))
                        }else{
                            viewModel.hitFollowUserDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, FollowRequest(following_id = userId.toString()))
                        }
                    }
                    "following"->{
                        viewModel.hitUnfollowUserDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, FollowRequest(unfollowId = userId.toString()))
                    }
                }
            }
            1->{
                findNavController().navigate(FollowersFragmentDirections.actionFollowersFragmentToProfileFragment("0", userId.toString()))
            }
            2->{
                getSendFollow(userId.toString())
            }
            3->{
                getSendUnFollow(userId.toString())
            }
            4->{
                if (statusListGlobal.isEmpty()) {
                    return
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


                /*RTVariable.IS_FROM_PROFILE = true
                val stories = adapterFollowers.getStoriesList()
                val storyPosition = stories.indexOfFirst { it.user_id == RTVariable.USER_ID.toInt() }
                val story = stories.find { it.user_id == RTVariable.USER_ID.toInt() }

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

//                //Toast.makeText(requireActivity(), "B ${data.id}", Toast.LENGTH_LONG).show()
//                val stories = adapterFollowers!!.getStoriesList()
//                //val storyPosition = stories.indexOfFirst { it.user_id == post.user_id }
//                val story = adapterFollowers!!.getStoriesList().find { it.user_id == RTVariable.USER_ID.toInt() }
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

    private fun getSendFollow(user_id: String){
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
                    when(screenCheck){
                        "followers"->{
                            viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                        }
                        "following"->{
                            viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun getSendUnFollow(user_id: String){
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
                    when(screenCheck){
                        "followers"->{
                            viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                        }
                        "following"->{
                            viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun setUI() {
        binding.apply {
            if (isMyProfile=="1"){
                when(screenCheck){
                    "followers"->{
                        binding.title.text = "Followers"
//                    binding.followersRecycler.adapter = AdapterFollowers(listOf(), screenCheck)
                        viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "true", other = "", otherUserId = "")
                    }
                    "following"->{
                        binding.title.text = "Following"
//                    binding.followersRecycler.adapter = AdapterFollowers(listOf(), screenCheck)
                        viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "true", other = "", otherUserId = "")
                    }
                }
            }else{
                when(screenCheck){
                    "followers"->{
                        binding.title.text = "Followers"
                        Log.e("IDID", "IDID>>>> "+userId)
//                    binding.followersRecycler.adapter = AdapterFollowers(listOf(), screenCheck)
                        viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                    }
                    "following"->{
                        binding.title.text = "Following"
                        Log.e("IDID", "IDID>>>> "+userId)
//                    binding.followersRecycler.adapter = AdapterFollowers(listOf(), screenCheck)
                        viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                    }
                }
            }
            viewModel5.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "1", "10", "0")
            viewModel6.hitStatusDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "0")
        }
    }
    private fun setObserver() {
        viewModel.getFollowersLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success 1: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            if (it.data.payload.followersList.isEmpty()){
                                binding.noDataFoundPlaceholder.text = "No Followers"
                                binding.noDataFoundPlaceholder.isVisible=true
                                adapterFollowers.setData(listOf())
                            }else{
                                binding.noDataFoundPlaceholder.isVisible=false
                                adapterFollowers.setData(it.data.payload.followersList)
                            }
                           // binding.followersRecycler.adapter = AdapterFollowers(it.data.payload.followersList, screenCheck)
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
        viewModel.getFollowingListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success 2: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            if (it.data.payload.followingList.isEmpty()){
                                binding.noDataFoundPlaceholder.text = "No Following"
                                binding.noDataFoundPlaceholder.isVisible=true
                                adapterFollowers.setData(listOf())
                            }else{
                                binding.noDataFoundPlaceholder.isVisible=false
                                adapterFollowers.setData(it.data.payload.followingList)
                            }
                           // binding.followersRecycler.adapter = AdapterFollowers(it.data.payload.followingList, screenCheck)
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
        viewModel.getFollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Follow User success 3: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            if (isMyProfile=="1"){
                                when(screenCheck){
                                    "followers"->{
                                        viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "true", other = "", otherUserId = "")
                                    }
                                    "following"->{
                                        viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "true", other = "", otherUserId = "")
                                    }
                                }
                            }else{
                                when(screenCheck){
                                    "followers"->{
                                        viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                                    }
                                    "following"->{
                                        viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                                    }
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
        viewModel.getUnfollowUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "UnFollow user success 4: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            if (isMyProfile=="1"){
                                when(screenCheck){
                                    "followers"->{
                                        viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "true", other = "", otherUserId = "")
                                    }
                                    "following"->{
                                        viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "true", other = "", otherUserId = "")
                                    }
                                }
                            }else{
                                when(screenCheck){
                                    "followers"->{
                                        viewModel.hitFollowersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                                    }
                                    "following"->{
                                        viewModel.hitFollowingListDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, self = "", other = "true", otherUserId = userId)
                                    }
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
                            adapterFollowers?.addStory(listOf(it.data.payload.my_story ?: MyStory()), it.data.payload.stories)
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
}