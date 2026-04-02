package com.app.hihlo.ui.profile.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterFollowersBinding
import com.app.hihlo.databinding.ItemBlockedUsersBinding
import com.app.hihlo.model.blocked_userlist.BlockedUsersResponse.Payload.BlockedUser
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.utils.RTVariable
import com.bumptech.glide.Glide

class BlockedUserAdapter(
    val getSelectedUser: (click: Int, userId: String) -> Unit,
    val blockedUsers: List<BlockedUser>,
) :
    RecyclerView.Adapter<BlockedUserAdapter.ViewHolder>() {
    var from = ""

    class ViewHolder(var binding: ItemBlockedUsersBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int {
        return blockedUsers?.size ?: 0
    }

    private val my_storiesList: MutableList<MyStory> = mutableListOf()
    private val storiesList: MutableList<Story> = mutableListOf()

    fun getMyStoriesList(): List<MyStory> = my_storiesList.toList()
    fun getStoriesList(): List<Story> = storiesList.toList()

    fun addStory(my_story: List<MyStory>, stories_List: List<Story>) {
        my_storiesList.addAll(my_story)
        storiesList.addAll(stories_List)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBlockedUsersBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            //Log.e("CCCCC", "CCCCCC>>>" + blockedUsers.get(position).userDetails.profileImage)
            Glide.with(root.context).load(blockedUsers.get(position).userDetails.profileImage)
                .placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder)
                .into(userImage)
            name.text = blockedUsers[position].userDetails.name
            userName.text = blockedUsers[position].userDetails.username
            userLocation.text =
                blockedUsers[position].userDetails.city + ", " + blockedUsers[position].userDetails.country
            followButton.text = "Unblock"
            val story = storiesList?.find { story -> story.user_id == blockedUsers[position].blockedUserId.toInt() }
            userImageCardView.background =
                if(blockedUsers[position].userDetails.isStoryUploaded==1){
                    if (story != null && story.is_seen == 0) {
                        root.context.resources.getDrawable(R.drawable.gredient_circle, null)
                    } else {
                        root.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
                    }
                }else{
                    root.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                }
            if (blockedUsers[position].userDetails.isCreator == 2) {
                verifiedNameTick.isVisible = true
            } else {
                verifiedNameTick.isVisible = false
            }
            when (blockedUsers[position].userDetails?.userLiveStatus) {
                "1" -> onlineStatusImage.setImageResource(R.drawable.online_status_green)
                "2", "3" -> onlineStatusImage.setImageResource(R.drawable.offline_status_red)
//                        "3" -> onlineStatusImage.setImageResource(R.drawable.busy_status)
            }
            followButton.setOnClickListener {
                getSelectedUser(1, blockedUsers[position].blockedUserId)
            }
//            userImage.setOnClickListener {
//                if(blockedUsers[position].userDetails.isStoryUploaded==1){
//                    val story = storiesList?.find { story -> story.user_id == blockedUsers[position].blockedUserId.toInt() }
//                    if (story != null) {
//                        val storyPosition = storiesList.indexOfFirst { it.user_id == blockedUsers[position].blockedUserId.toInt() }
//                        RTVariable.STORY_POSITION = storyPosition
//                        getSelectedUser(3, blockedUsers[position].blockedUserId)
//                    } else {
//                        getSelectedUser(3, blockedUsers[position].blockedUserId)
//                    }
//                }else{
//                    getSelectedUser(2, blockedUsers[position].blockedUserId)
//                }
//            }
//            name.setOnClickListener {
//                getSelectedUser(2, blockedUsers[position].blockedUserId)
//            }
        }
    }

}