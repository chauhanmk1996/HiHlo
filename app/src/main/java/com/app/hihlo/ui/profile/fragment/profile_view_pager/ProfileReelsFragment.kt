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
import com.app.hihlo.model.get_profile.Data
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.reel.response.Creator
import com.app.hihlo.model.reel.response.Pagination
import com.app.hihlo.model.reel.response.Payload
import com.app.hihlo.model.reel.response.Reel
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.profile.adapter.AdapterProfileMedia
import com.app.hihlo.ui.profile.fragment.PaginatingFragment
import com.app.hihlo.ui.profile.fragment.ProfileFragmentDirections
import com.app.hihlo.ui.profile.view_model.ProfilePostViewModel
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.collections.isNotEmpty
import kotlin.collections.toMutableList
import kotlin.getValue

class ProfileReelsFragment : Fragment(), PaginatingFragment {

    private var _binding: AdapterShowMediaViewPagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var reels: Posts
    private lateinit var isMyProfile: String
    private lateinit var userId: String
    private val viewModel: ProfilePostViewModel by viewModels()
    private var isLoading = true
    private var currentPage = 1
    private lateinit var adapter: AdapterProfileMedia

    companion object {
        fun newInstance(reels: Posts, isMyProfile: String, userId: String) =
            ProfileReelsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("reels_data", reels)
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
        reels = requireArguments().getParcelable("reels_data")!!
        isMyProfile = requireArguments().getString("isMyProfile")!!
        userId = requireArguments().getString("userId")!!

        if (reels.pagination.total == 0) {
            binding.noPostsFoundPlaceholder.isVisible = true
            binding.noPostsFoundPlaceholderText.text =
                if (isMyProfile == "1") "Create your First Play" else "No Reel"
        } else {
            binding.noPostsFoundPlaceholder.isVisible = false
        }

        adapter = AdapterProfileMedia(reels) { position ->
            UserDataManager.setReelsPosition(binding.root.context, position)
            getSelectedPost(position)
        }
        binding.showMediaRecycler.adapter = adapter
        return binding.root
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

    private fun getSelectedPost(reelPosition: Int) {
        RTVariable.IS_PROFILE_POST_LIST = true
        UserPreference.navigatedToMyProfile = true
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToReelsFragment(
                Payload(pagination = Pagination(), reels = reels.data.toReelList().toMutableList()),
                "profile",
                reelPosition.toString()
            )
        )

    }

    fun List<Data>.toReelList(): List<Reel> {
        return this.map { data ->
            Reel(
                assetType = data.asset_type ?: "",
                assetUrl = data.asset_url ?: "",
                caption = data.caption ?: "",
                commentsCount = data.commentsCount ?: 0, // or map from somewhere else if available
                isLiked = data.isLiked ?: 0,
                isFollowing = 0,   // default or map from somewhere
                createdAt = data.created_at ?: "",
                creator = Creator(
                    name = data.creator_name ?: "",
                    profileImage = data.creator_profile_image.toString(),
                    username = data.creator_username ?: "",
                    city = data.userCity.toString(),      // default or fetch if available
                    country = data.userCountry.toString(),    // default or fetch if available
                    user_live_status = data.status.toString(),
                    isStoryUploaded = data.is_story_uploaded ?: 0
                ),
                creatorId = data.creator_id ?: 0,
                id = data.id ?: 0,
                likesCount = data.likesCount ?: 0,    // default or fetch if available
                status = data.status ?: "",
                updatedAt = data.updated_at ?: "",
                lastPlaybackPosition = 0L
            )
        }
    }

    private fun setObserver() {
        viewModel.getProfileLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {

                            if (it.data.payload.reels.data.isNotEmpty()) {
                                isLoading = true
                                if (currentPage == 1) {
                                    if (it.data.payload.reels.data.isNotEmpty()) {
                                        adapter.clearList()
                                        adapter.updateList(it.data.payload.reels)
                                    } else {
                                        adapter.clearList()
                                    }
                                } else {
                                    adapter.updateList(it.data.payload.reels)
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
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            if (it.data.payload.reels.data.isNotEmpty()) {
                                isLoading = true
                            }
                            if (currentPage == 1) {
                                if (it.data.payload.reels.data.isNotEmpty()) {
                                    adapter.clearList()
                                    adapter.updateList(it.data.payload.reels)
                                } else {
                                    adapter.clearList()
                                }
                            } else {
                                adapter.updateList(it.data.payload.reels)
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
