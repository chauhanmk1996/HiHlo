package com.app.hihlo.ui.profile.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.ui.profile.fragment.profile_view_pager.ProfilePostsFragment
import com.app.hihlo.ui.profile.fragment.profile_view_pager.ProfileReelsFragment

class AdapterProfileMediaViewPager(
    fragment: Fragment,
    private val reels: Posts,
    private val posts: Posts,
    val isMyProfile: String,
    val userId: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProfilePostsFragment.newInstance(posts, isMyProfile, userId)
            1 -> ProfileReelsFragment.newInstance(reels, isMyProfile, userId)
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}

