package com.app.hihlo.ui.profile.fragment.profile_view_pager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.databinding.AdapterShowMediaViewPagerBinding
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.adapter.AdapterProfileMedia
import com.app.hihlo.ui.profile.fragment.PaginatingFragment
import com.app.hihlo.ui.profile.fragment.ProfileFragmentDirections
import com.app.hihlo.ui.profile.view_model.ProfilePostViewModel
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class ProfilePostsFragment : Fragment(), PaginatingFragment {

    private var _binding: AdapterShowMediaViewPagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var posts: Posts
    private lateinit var isMyProfile: String
    private lateinit var userId: String
    private val viewModel: ProfilePostViewModel by viewModels()
    private var isLoading = true
    private var currentPage = 1
    private lateinit var adapter: AdapterProfileMedia

    companion object {
        fun newInstance(posts: Posts, isMyProfile: String, userId: String) =
            ProfilePostsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("posts_data", posts)
                    putString("isMyProfile", isMyProfile)
                    putString("userId", userId)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AdapterShowMediaViewPagerBinding.inflate(inflater, container, false)
        posts = requireArguments().getParcelable("posts_data")!!
        isMyProfile = requireArguments().getString("isMyProfile")!!
        userId = requireArguments().getString("userId")!!

        if (posts.pagination.total == 0) {
            binding.noPostsFoundPlaceholder.isVisible = true
            binding.placeholderImage.isVisible = false
            binding.noPostsFoundPlaceholderText.text =
                if (isMyProfile == "1") "Create your First Post" else "No Post"
        } else {
            binding.noPostsFoundPlaceholder.isVisible = false
        }
        adapter = AdapterProfileMedia(posts) { position ->
            getSelectedPost(position)
        }
        binding.showMediaRecycler.adapter = adapter
        return binding.root
    }

    private fun getSelectedPost(reelPosition: Int) {
        RTVariable.IS_PROFILE_POST_LIST = true
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToUserPostListFragment(
                arrayOf(),
                posts,
                "profile",
                reelPosition.toString()
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }

    private fun hitMyProfileApi(page: Int) {
        viewModel.hitProfileDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, page.toString(), "14"
        )
    }

    private fun hitOtherUserApi(page: Int) {
        viewModel.hitOtherUserProfileDataApi(
            "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                requireContext(),
                LOGIN_DATA
            )?.payload?.authToken, userId, page.toString(), "14"
        )
    }

    private fun setObserver() {
        viewModel.getProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            if (it.data.payload.posts.data.isNotEmpty()) {
                                isLoading = true
                                if (currentPage == 1) {
                                    if (it.data.payload.posts.data.isNotEmpty()) {
                                        adapter.clearList()
                                        adapter.updateList(it.data.payload.posts)
                                    } else {
                                        adapter.clearList()
                                    }
                                } else {
                                    adapter.updateList(it.data.payload.posts)
                                }
                            }
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

        viewModel.getOtherUserProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Posts success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            if (it.data.payload.posts.data.isNotEmpty()) {
                                isLoading = true
                            }
                            if (currentPage == 1) {
                                if (it.data.payload.posts.data.isNotEmpty()) {
                                    adapter.clearList()
                                    adapter.updateList(it.data.payload.posts)
                                } else {
                                    adapter.clearList()
                                }
                            } else {
                                adapter.updateList(it.data.payload.posts)
                            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onParentScrolledToBottom() {
        if (isLoading) {
            currentPage++
            if (isMyProfile == "1") {
                hitMyProfileApi(currentPage)
            } else {
                hitOtherUserApi(currentPage)
            }
        }
        isLoading = false
    }
}