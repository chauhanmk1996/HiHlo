package com.app.hihlo.ui.HomeNew.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterStoriesRecyclerBinding
import com.app.hihlo.ui.HomeNew.model.StatusItem
import com.bumptech.glide.Glide

class StatusAdapter(
    private val list: List<StatusItem>,
    private val getSelectedTheStory: (Int, StatusItem, Int, view: View) -> Unit
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        if (position == 0) {
            holder.binding.plusBottomRight.isVisible = true
            holder.binding.uploadLayout.isVisible = false
            holder.binding.otherStoryCardview.isVisible = false
            holder.binding.myStoryGradient.isVisible = true
            if(item.isStoriesUploaded){
                holder.binding.name.visibility = View.VISIBLE
                holder.binding.name.text = "Upload"
            }else{
                holder.binding.name.visibility = View.VISIBLE
                holder.binding.name.text = "Upload"
            }
            Glide.with(holder.binding.root.context)
                .load(item.userDetail.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(holder.binding.myStoryImageView)
            holder.binding.myStoryGradient.background =
                if (item.isStoriesUploaded) {
                    holder.binding.root.resources.getDrawable(R.drawable.story_gradient_border, null)
                } else {
                    holder.binding.root.resources.getDrawable(R.color.transparent, null)
                }
        }else{
            holder.binding.plusBottomRight.isVisible = false
            holder.binding.uploadLayout.isVisible = false
            holder.binding.otherStoryCardview.isVisible = true
            holder.binding.myStoryGradient.isVisible = false
            holder.binding.name.visibility = View.VISIBLE
            holder.binding.name.text = item.userDetail.name
            Log.e("ISSEEN", "ISSEEN>>> "+item.is_seen)
            Glide.with(holder.binding.root.context)
                .load(item.userDetail.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(holder.binding.otherStoryImageview)
            holder.binding.otherStoryCardview.background =
                if (item.is_seen == 0) {
                    holder.binding.root.resources.getDrawable(R.drawable.story_gradient_border, null)
                } else {
                    holder.binding.root.resources.getDrawable(R.drawable.story_gray_border, null)
                }
        }
        holder.itemView.setOnClickListener {
            /// This is the play option
            if(position==0){
                if(item.isStoriesUploaded){
                    getSelectedTheStory(2, item, position, holder.binding.root)
                    holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }else{
                getSelectedTheStory(2, item, position, holder.binding.root)
                holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        holder.binding.plusBottomRight.setOnClickListener {
            getSelectedTheStory(3, item, position, holder.binding.root)
            holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        holder.binding.name.setOnClickListener {
            getSelectedTheStory(3, item, position, holder.binding.root)
            holder.binding.storyLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

}