package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentBlockedUserBinding
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.get_profile.UserDetails
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.unblock_user.request.UnblockUserRequest
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.StatusModel.StatusViewModel
import com.app.hihlo.ui.HomeNew.adapter.StatusAdapter
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.app.hihlo.ui.home.adapter.AdapterStoriesRecycler
import com.app.hihlo.ui.home.fragment.UserPostListFragmentDirections
import com.app.hihlo.ui.home.view_model.HomeViewModel
import com.app.hihlo.ui.profile.adapter.BlockedUserAdapter
import com.app.hihlo.ui.profile.view_model.BlockedUserViewModel
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

class BlockedUserFragment : Fragment() {
    private lateinit var binding:FragmentBlockedUserBinding
    private lateinit var blockedUserAdapter: BlockedUserAdapter
    private val viewModel: BlockedUserViewModel by viewModels()
    private val viewModel2: HomeViewModel by viewModels()
    private var myStoryData: MyStory = MyStory()
    private var allStory: List<Story>? = null
    private val viewModel5: StatusViewModel by activityViewModels()
    private lateinit var statusListGlobal: List<StatusItem>

    var data = mutableListOf<UserDetails>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding = FragmentBlockedUserBinding.inflate(layoutInflater)
        return binding.root
    }
    private fun getSelectedUser(click: Int, userId: String, view: View){
        when(click){
            1->{
                openUnblockUserConfirmationDialog(userId)
            }
            2->{
                Log.e("CCCCC", "CCCCCC>>>"+userId)
                findNavController().navigate(UserPostListFragmentDirections.actionUserPostListFragmentToProfileFragment("0", userId))
            }
            3->{
                RTVariable.IS_FROM_PROFILE = true
                val stories = blockedUserAdapter.getStoriesList()
                val storyPosition = stories.indexOfFirst { it.user_id == userId.toInt() }
                val story = stories.find { it.user_id == userId.toInt() }

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
                }




//                Log.e("CCCCC", "CCCCCC>>>"+userId)
//                //Toast.makeText(requireActivity(), "B ${data.id}", Toast.LENGTH_LONG).show()
//                val stories = blockedUserAdapter!!.getStoriesList()
//                //val storyPosition = stories.indexOfFirst { it.user_id == post.user_id }
//                val story = blockedUserAdapter!!.getStoriesList().find { it.user_id == userId.toInt() }
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
    fun openUnblockUserConfirmationDialog(userId: String) {
        showCustomDialogWithBinding(requireContext(), "Are you sure you want to unblock this user?",
            onYes = {
                viewModel.hitUnblockUserApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
                    UnblockUserRequest(unblockId = userId.toString()))
            },
            onNo = {
                //dismiss()
            }
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        onClick()
        //viewModel.hitBlockedUsersDataApi("Bearer "+ "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MTQzLCJlbWFpbCI6ImFqYXlyYXk3OThAZ21haWwuY29tIiwiaWF0IjoxNzc1MDUwMTQyLCJleHAiOjE3NzU2NTQ5NDJ9.VElXwMH0VJTSZRBhiPHz5OMtzJ3xyC81-ITozT08PBk")
        viewModel.hitBlockedUsersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
        viewModel2.hitHomeDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, "1", "10", "0")
    }

    private fun setObserver() {
        viewModel.getBlockedUsersLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Blocked List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            val blockedUsers = it.data.payload.blockedUsers
                            if (blockedUsers.isNullOrEmpty()) {
                                binding.rcvBlockedUsers.visibility = View.GONE
                                binding.noBlockId.visibility = View.VISIBLE
                            } else {
                                binding.rcvBlockedUsers.visibility = View.VISIBLE
                                binding.noBlockId.visibility = View.GONE
                                blockedUserAdapter = BlockedUserAdapter(::getSelectedUser, blockedUsers)
                                binding.rcvBlockedUsers.adapter = blockedUserAdapter
                            }
                            Log.i("TAG", "setObserver: "+it.data.payload.blockedUsers)
                            Log.i("TAG", "setObserver: "+it.data.payload)
                           /* blockedUserAdapter= BlockedUserAdapter(::getSelectedUser, it.data.payload.blockedUsers)
                            binding.rcvBlockedUsers.adapter = blockedUserAdapter
                       */     // binding.followersRecycler.adapter = AdapterFollowers(it.data.payload.followersList, screenCheck)
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
        viewModel2.getHomeLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    Log.e("TAG", "Home success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200) {
                            myStoryData = it.data.payload.my_story ?: MyStory()
                            allStory = it.data.payload.stories
                            Log.e("TAG", "Home success: ${myStoryData}")
                            Log.e("TAG", "Home success: ${allStory}")
                            if (::blockedUserAdapter.isInitialized) {
                                blockedUserAdapter.addStory(
                                    listOf(it.data.payload.my_story ?: MyStory()),
                                    it.data.payload.stories
                                )
                            }
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
        viewModel.getUnblockUserLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "unblock success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            //findNavController().popBackStack()
                            viewModel.hitBlockedUsersDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken)
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
//        viewModel5.getStatusLiveData().observe(viewLifecycleOwner) {
//            when (it.status) {
//                Status.SUCCESS -> {
//                    Log.e("TAG", "Status success: ${Gson().toJson(it)}")
//                    if (it.data?.status==1){
//                        if (it.data.code == 200) {
//                            statusListGlobal = it.data.payload
//                            RTVariable.statusListGlobal = statusListGlobal
//                        }else{
//                        }
//                    }else{
//                    }
//                }
//                Status.LOADING -> {
//                }
//                Status.ERROR -> {
//                }
//            }
//        }
    }

    private fun onClick() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}