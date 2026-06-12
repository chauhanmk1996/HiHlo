package com.app.hihlo.ui.home.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterHomeCreatorsBinding
import com.app.hihlo.model.home.response.Post
import com.bumptech.glide.Glide

class AdapterHomeCreators(
    private val posts: MutableList<Post>,
    val getSelectedPost: (Post, Int, Int) -> Unit,
) :
    RecyclerView.Adapter<AdapterHomeCreators.ViewHolder>() {

    inner class ViewHolder(val binding: AdapterHomeCreatorsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterHomeCreatorsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val layoutParams = holder.itemView.layoutParams
        val randomHeight = (190..300).random()
        layoutParams.height = randomHeight.dpToPx(holder.itemView.context)
        holder.itemView.layoutParams = layoutParams
        holder.binding.apply {
            Glide.with(root.context).load(posts[position].asset_url).into(backgroundImage)
            Glide.with(root.context).load(posts[position].creatorDetail?.profile_image)
                .placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder)
                .into(userImage)
            userName.text = posts[position].creatorDetail?.name
            interest.text = posts[position].creatorDetail?.interest_name
            Glide.with(root.context).load(posts[position].creatorDetail?.interest_image)
                .into(interestImage)
            userLocation.text =
                posts[position].creatorDetail?.city + ", " + posts[position].creatorDetail?.country
            verifiedNameTick.isVisible = posts[position].creatorDetail?.is_creator == 1
            when (posts[position].creatorDetail?.user_live_status) {
                "1" -> {
                    onlineStatusImage.setImageResource(R.drawable.online_status_green)
                    onlineText.text = "Online"
                }

                "2", "3" -> {
                    onlineStatusImage.setImageResource(R.drawable.offline_status_red)
                    onlineText.text = "Offline"
                }
                /*"3"->{
                    onlineStatusImage.setImageResource(R.drawable.busy_status)
                    onlineText.text = "Busy"
                }*/
            }
            mainLayout.setOnClickListener {
                getSelectedPost(posts[position], 1, position)
            }

            userDetailsLayout.setOnClickListener {
                getSelectedPost(posts[position], 1, position)
            }
        }
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    fun updateList(posts: MutableList<Post>) {
        Log.i("TAG", "updateList: $posts")
        val start = this.posts.size
        this.posts.addAll(posts)
        notifyItemRangeInserted(start, posts.size)
    }

    fun clearList() {
        val size = posts.size
        posts.clear()
        notifyItemRangeRemoved(0, size)
    }
}
