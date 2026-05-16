package com.app.hihlo.ui.HomeNew.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterStoriesRecyclerBinding
import com.app.hihlo.model.story_response.StoryUser
import com.bumptech.glide.Glide

class StatusAdapter(
    private val list: MutableList<StoryUser>,
    private val getSelectedTheStory: (
        type: Int,
        item: StoryUser,
        position: Int,
        view: View
    ) -> Unit
) : RecyclerView.Adapter<StatusAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AdapterStoriesRecyclerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = AdapterStoriesRecyclerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        // =========================================================
        // MY STORY
        // =========================================================
        if (position == 0) {

            holder.binding.plusBottomRight.isVisible = true
            holder.binding.uploadLayout.isVisible = true

            holder.binding.otherStoryCardview.isVisible = false

            holder.binding.myStoryGradient.isVisible = true

            holder.binding.name.visibility = View.INVISIBLE

            Glide.with(holder.binding.root.context)
                .load(item.userDetail.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(holder.binding.myStoryImageView)

            // MY STORY BORDER
            holder.binding.myStoryGradient.background =
                if (item.isStoriesUploaded) {

                    holder.binding.root.resources.getDrawable(
                        R.drawable.story_gradient_border,
                        null
                    )

                } else {

                    holder.binding.root.resources.getDrawable(
                        R.color.transparent,
                        null
                    )
                }

        }

        // =========================================================
        // OTHER USER STORIES
        // =========================================================
        else {

            holder.binding.plusBottomRight.isVisible = false
            holder.binding.uploadLayout.isVisible = false

            holder.binding.otherStoryCardview.isVisible = true

            holder.binding.myStoryGradient.isVisible = false

            holder.binding.name.visibility = View.VISIBLE
            holder.binding.name.text = item.userDetail.name

            Glide.with(holder.binding.root.context)
                .load(item.userDetail.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(holder.binding.otherStoryImageview)

            // =====================================================
            // STORY SEEN CHECK
            // =====================================================

            // TRUE = ALL STORIES SEEN
            // FALSE = AT LEAST ONE STORY UNSEEN

            val isAllStoriesSeen =
                item.stories.all { story ->
                    story.is_seen == 1
                }

            Log.e(
                "IS_SEEN",
                "User: ${item.userDetail.name} | ALL_SEEN = $isAllStoriesSeen"
            )

            // =====================================================
            // BORDER SET
            // =====================================================

            holder.binding.otherStoryCardview.background =
                if (isAllStoriesSeen) {

                    // ALL STORIES SEEN -> GRAY BORDER
                    holder.binding.root.resources.getDrawable(
                        R.drawable.story_gray_border,
                        null
                    )

                } else {

                    // ANY STORY UNSEEN -> GRADIENT BORDER
                    holder.binding.root.resources.getDrawable(
                        R.drawable.story_gradient_border,
                        null
                    )
                }
        }

        // =========================================================
        // STORY CLICK
        // =========================================================
        holder.itemView.setOnClickListener {

            if (position == 0) {

                if (item.isStoriesUploaded) {

                    getSelectedTheStory(
                        2,
                        item,
                        position,
                        holder.binding.root
                    )

                    holder.binding.storyLayout.setBackgroundColor(
                        Color.TRANSPARENT
                    )
                }

            } else {

                getSelectedTheStory(
                    2,
                    item,
                    position,
                    holder.binding.root
                )

                holder.binding.storyLayout.setBackgroundColor(
                    Color.TRANSPARENT
                )
            }
        }

        // =========================================================
        // PLUS BUTTON CLICK
        // =========================================================
        holder.binding.plusBottomRight.setOnClickListener {

            getSelectedTheStory(
                3,
                item,
                position,
                holder.binding.root
            )

            holder.binding.storyLayout.setBackgroundColor(
                Color.TRANSPARENT
            )
        }

        // =========================================================
        // NAME CLICK
        // =========================================================
        holder.binding.name.setOnClickListener {

            getSelectedTheStory(
                3,
                item,
                position,
                holder.binding.root
            )

            holder.binding.storyLayout.setBackgroundColor(
                Color.TRANSPARENT
            )
        }

        // =========================================================
        // UPLOAD LAYOUT CLICK
        // =========================================================
        holder.binding.uploadLayout.setOnClickListener {

            getSelectedTheStory(
                3,
                item,
                position,
                holder.binding.root
            )

            holder.binding.storyLayout.setBackgroundColor(
                Color.TRANSPARENT
            )
        }
    }

    // =============================================================
    // UPDATE STORIES
    // =============================================================
    @SuppressLint("NotifyDataSetChanged")
    fun updateStories(storiesList: List<StoryUser>) {

        Log.e(
            "STATUS_ADAPTER",
            "updateStories size = ${storiesList.size}"
        )

        list.clear()
        list.addAll(storiesList)

        notifyDataSetChanged()
    }
}