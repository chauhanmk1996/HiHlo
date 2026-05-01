package com.app.hihlo.ui.search.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterSearchUsersFragmentBinding
import com.app.hihlo.databinding.AdapterSearchListBinding
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.search_user_list.response.SearchUserListResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.utils.RTVariable
import com.bumptech.glide.Glide

class SearchAdapter(val users: MutableList<SearchUserListResponse.Payload.User>, val getSelectedChat: (Int, Int, View) -> Unit): RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(AdapterSearchUsersFragmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    fun Int.toPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.binding.apply {
            when(users[position].user_live_status){
                "1"->{
                    onlineStatusImage.setImageResource(R.drawable.online_status_green)
                }
                "2", "3"->{
                    onlineStatusImage.setImageResource(R.drawable.offline_status_red)
                }
                /*"3"->{
                    onlineStatusImage.setImageResource(R.drawable.busy_status)
                }*/
            }
            userLocation.isVisible=true

            verifiedNameTick.isVisible = if (users[position].is_verified) true else false
            name.text = users[position].name
            userId.text = users[position].username
            userLocation.text = users[position].city+", "+users[position].country
//            userImageCardView.strokeWidth = 0
            Glide.with(root.context).load(users[position].profile_image).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
            var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
            if (users[position].username==user){
                /*holder.binding.myStoryGradient.setPadding(0, 0, 0, 0)
                val sizeInDp = 55
                val scale = root.context.resources.displayMetrics.density
                val sizeInPx = (sizeInDp * scale).toInt()
                val params = holder.binding.innerCard.layoutParams
                params.width = sizeInPx
                params.height = sizeInPx
                holder.binding.innerCard.layoutParams = params */
                holder.binding.myStoryGradient.background = holder.binding.root.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
            }else{
                if (users[position].isStoryUploaded==1){
                    /*val padding = 6.toPx(root.context)
                    holder.binding.myStoryGradient.setPadding(
                        padding,
                        padding,
                        padding,
                        padding
                    )
                    val sizeInDp = 42
                    val scale = root.context.resources.displayMetrics.density
                    val sizeInPx = (sizeInDp * scale).toInt()
                    val params = holder.binding.innerCard.layoutParams
                    params.width = sizeInPx
                    params.height = sizeInPx
                    holder.binding.innerCard.layoutParams = params */
                    val story = storiesList?.find { story -> story.user_id == users[position].id }
                    Log.e("TTTTT", "SSSSS>>> "+story)
                    holder.binding.myStoryGradient.background =
                        if (story != null && story.is_seen == 0) {
                            root.context.resources.getDrawable(R.drawable.gredient_circle, null)
                        } else {
                            root.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
                        }
                }else{
                    /*holder.binding.myStoryGradient.setPadding(0, 0, 0, 0)
                    val sizeInDp = 55
                    val scale = root.context.resources.displayMetrics.density
                    val sizeInPx = (sizeInDp * scale).toInt()
                    val params = holder.binding.innerCard.layoutParams
                    params.width = sizeInPx
                    params.height = sizeInPx
                    holder.binding.innerCard.layoutParams = params */
                    holder.binding.myStoryGradient.background = holder.binding.root.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                }
            }
            //var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
            if (users[position].username==user){
                followButtonLayout.isVisible = false
            }else if (users[position].is_follow!=1){
                followButtonLayout.isVisible = true
                followButtonImage.setImageResource(R.drawable.follow_button_reel)
            }else{
                followButtonImage.setImageResource(R.drawable.following_button_reel)
                followButtonLayout.isVisible = true
            }
            userImage.setOnClickListener {
                var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                if (users[position].username==user){
                    getSelectedChat(position, 1, userImage)
                }else{
                    if(users[position].isStoryUploaded == 1){
                        val story = storiesList.find { it.user_id == users[position].id }
                        Log.e("YYYY", "YYYY>>>"+story)
                        if (story != null && story.is_seen == 0) {
                            val storyPosition = storiesList.indexOfFirst { it.user_id == users[position].id }
                            RTVariable.USER_ID = users[position].id.toString()
                            RTVariable.STORY_POSITION = storyPosition
                            getSelectedChat(position, 4, userImage)
                        } else {
                            val storyPosition = storiesList.indexOfFirst { it.user_id == users[position].id }
                            RTVariable.USER_ID = users[position].id.toString()
                            RTVariable.STORY_POSITION = storyPosition
                            getSelectedChat(position, 4, userImage)
                        }
                    }else{
                        getSelectedChat(position, 1, userImage)
                    }
                }
            }
            name.setOnClickListener {
                getSelectedChat(position, 1, name)
            }
            followButtonImage.setOnClickListener {
                //getSelectedUser(data.follower_id, data.isFollowing, 3)
                //RTVariable.MY_USER_ID = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.userId.toString()
                RTVariable.USER_ID = users[position].id.toString()
                if (users[position].is_follow!=1){
                    getSelectedChat(position, 2, followButtonImage)
                }else{
                    getSelectedChat(position, 3, followButtonImage)
                }
            }
            root.setOnClickListener {
                getSelectedChat(position, 1, root)
            }
        }

    }

    override fun getItemCount(): Int {
        return users.size
    }

    inner class ViewHolder(val binding: AdapterSearchUsersFragmentBinding): RecyclerView.ViewHolder(binding.root)

    private val my_storiesList: MutableList<MyStory> = mutableListOf()
    private val storiesList: MutableList<Story> = mutableListOf()

    fun getMyStoriesList(): List<MyStory> = my_storiesList.toList()
    fun getStoriesList(): List<Story> = storiesList.toList()

    fun updateList(users: List<SearchUserListResponse.Payload.User>) {
        Log.i("TAG", "updateList: "+users)
        var start = if (this.users.isNotEmpty())
            this.users.size else 0
        this.users.addAll(users)
        notifyItemRangeInserted(start, this.users.size)
    }
    fun clearList(){
        var size = users.size
        users.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun addStory(my_story: List<MyStory>, stories_List: List<Story>) {
        my_storiesList.addAll(my_story)
        storiesList.addAll(stories_List)
        notifyDataSetChanged()
    }
}