package com.app.hihlo.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterStoriesRecyclerBinding
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.bumptech.glide.Glide

class AdapterStoriesRecycler(
    var isMediaInMyStory: Int,
    var myStoryData: MyStory,
    var storyListing: List<Story>,
    val getSelectedStory: (Int, Story, view: View, Int) -> Unit,
    val myProfileImage: String?,
) : RecyclerView.Adapter<AdapterStoriesRecycler.ViewHolder>() {


    inner class ViewHolder(val binding: AdapterStoriesRecyclerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.adapter_stories_recycler,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return storyListing.size + 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.onlineImage.isVisible = false

        if (position == 0) {
            // Reset visibilities explicitly
            holder.binding.myStoryCardView.isVisible = true
            holder.binding.otherStoryCardview.isVisible = false
            holder.binding.myStoryGradient.isVisible = true
            holder.binding.plusBottomRight.isVisible = isMediaInMyStory != 1
            holder.binding.plusBottomRight.isVisible = false
            holder.binding.uploadLayout.isVisible = isMediaInMyStory != 1
            holder.binding.name.text = "My Story"
            if(isMediaInMyStory==1){
                holder.binding.name.visibility = View.VISIBLE
            }else{
                holder.binding.name.visibility = View.INVISIBLE
            }
            // Load profile image safely
            if (!myProfileImage.isNullOrEmpty()) {
                Glide.with(holder.binding.root.context)
                    .load(myProfileImage)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(holder.binding.myStoryImageView)
            } else {
                holder.binding.myStoryImageView.setImageResource(R.drawable.profile_placeholder)
            }

            // Gradient state
            holder.binding.myStoryGradient.background =
                if (isMediaInMyStory == 1) {
                    holder.binding.root.resources.getDrawable(R.drawable.story_gradient_border, null)
                } else {
                    holder.binding.root.resources.getDrawable(R.color.transparent, null)
                }

        } else {
            // Reset visibilities explicitly
            holder.binding.myStoryCardView.isVisible = false
            holder.binding.otherStoryCardview.isVisible = true
            holder.binding.myStoryGradient.isVisible = false
            holder.binding.plusBottomRight.isVisible = false
            holder.binding.uploadLayout.isVisible = false
            val story = storyListing[position - 1]
            holder.binding.name.visibility = View.VISIBLE
            holder.binding.name.text = story.userDetail?.name ?: ""

            Glide.with(holder.binding.root.context)
                .load(story.userDetail?.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(holder.binding.otherStoryImageview)

            holder.binding.otherStoryCardview.background =
                if (story.is_seen == 0) {
                    holder.binding.root.resources.getDrawable(R.drawable.story_gradient_border, null)
                } else {
                    holder.binding.root.resources.getDrawable(R.drawable.story_gray_border, null)
                }
        }

        // Click listeners
        holder.binding.storyLayout.setOnClickListener {
            if (position == 0) {
                getSelectedStory(position, Story(), holder.binding.root, 1)
                holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                getSelectedStory(position, storyListing[position - 1], holder.binding.root, 2)
                holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        holder.binding.plusBottomRight.setOnClickListener {
            // selector.uploadStory()
            getSelectedStory(position, Story(), holder.binding.root, 3)
            holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        holder.binding.name.setOnClickListener {
            // selector.uploadStory()
            getSelectedStory(position, Story(), holder.binding.root, 3)
            holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        holder.binding.uploadLayout.setOnClickListener {
            // selector.uploadStory()
            getSelectedStory(position, Story(), holder.binding.root, 3)
            holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

}