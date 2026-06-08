package com.app.hihlo.ui.search.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentSearchBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.home.response.UserDetails
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.search_user_list.response.SearchUserListResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.chat.fragment.ChatListFragmentDirections
import com.app.hihlo.ui.home.fragment.HomeFragmentDirections
import com.app.hihlo.ui.search.adapter.SearchAdapter
import com.app.hihlo.ui.search.view_model.SearchViewModel
import com.app.hihlo.utils.getString
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson



class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private val viewModel: SearchViewModel by viewModels()
    lateinit var adapter: SearchAdapter
    private var userList: MutableList<SearchUserListResponse.Payload.User> = mutableListOf()

    override fun initView(savedInstanceState: Bundle?) {
        adapter = SearchAdapter(mutableListOf()){ position, click, clickView ->
            when(click){
                0->{
                    val bundle = Bundle().apply {
                        putParcelableArrayList("storyList", ArrayList(listOf<Story>(mapUserToStory(userList[position])) ?: emptyList()))
                        putInt("position", 0)
                    }
                    try {
                        findNavController().navigate(R.id.secondStoryFragment, bundle)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
                    }
//                    findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToStoryFragment(isMyStory = "0", myStoryData = MyStory(), otherStoryData =  mapUserToStory(userList[position])))
                }
                1->{
//                    val bundle = Bundle()
//                    bundle.putParcelable("userDetail",mapUserToUserDetailsX(userList[position]))
//                    findNavController().navigate(R.id.action_searchFragment_to_profileFragment, bundle)

                    findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToProfileFragment("0", userList[position].id.toString()))

                }
            }

        }
        hitSearchUserApi()
        binding.searchRecycler.adapter = adapter
        onClick()
        binding.searchEdittext.doAfterTextChanged {
            binding.crossButton.isVisible = binding.searchEdittext.getString().isNotEmpty()
            hitSearchUserApi()
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
    /*fun mapUserToUserDetailsX(user: SearchUserListResponse.Payload.User): UserDetailsX {
        return UserDetailsX(
            about = user.about,
            city = user.city,
            country = user.country,
            dob = null, // Not available in source
            email = user.email,
            followers_count = null, // Not available in source
            following_count = null, // Not available in source
            gender = user.gender_name,
            id = user.id,
            interest_name = user.interest_name,
            is_verified = user.is_verified.toString(), // Boolean -> String
            name = user.name,
            phone = null, // Not available in source
            posts_count = null, // Not available in source
            blockStatus = null, // Not available in source
            profile_image = user.profile_image,
            profileImage = user.profile_image, // mapped again for different key
            reels_count = null, // Not available in source
            is_following = null, // Not available in source
            is_seen = user.is_seen,
            role = user.role,
            onlineStatus = user.user_live_status,
            isCreator = if (user.is_creator) 1 else 0, // Boolean -> Int
            isStoryUploaded = user.isStoryUploaded,

            username = user.username
        )
    }*/

    private fun onClick() {
        binding.crossButton.setOnClickListener {
            binding.searchEdittext.setText("")
            adapter.clearList()
        }
    }

    fun hitSearchUserApi(){
        viewModel.hitSearchUsersList("Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken ?: "", "1", "20", binding.searchEdittext.getString())
    }
    override fun getLayoutId(): Int {
        return R.layout.fragment_search
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        keyboardListener()
        setObserver()
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
                    requireActivity().findViewById<View>(R.id.bottomAppBar).visibility = View.VISIBLE
                    requireActivity().findViewById<View>(R.id.floatingbtn).visibility = View.VISIBLE
                    requireActivity().findViewById<View>(R.id.imgBtn).visibility = View.VISIBLE
                }
                insets
            }
        }
    }

    private fun setObserver() {
        viewModel.getUsersListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "get search list: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            userList = it.data.payload.users.toMutableList()
                            adapter.clearList()
                            adapter.updateList(it.data.payload.users)
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
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

    }

}