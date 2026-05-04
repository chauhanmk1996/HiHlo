package com.app.hihlo.ui.profile.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterFollowersBinding
import com.app.hihlo.model.following_list.response.Following
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter.PostClickAction
import com.app.hihlo.utils.RTVariable
import com.bumptech.glide.Glide

class AdapterFollowers(
    val isMyProfile: String,
    val getSelectedUser: (userId: Int, isFollowing:Int, click: Int, view: View) -> Unit,
) :
    RecyclerView.Adapter<AdapterFollowers.ViewHolder>() {
    private val my_storiesList: MutableList<MyStory> = mutableListOf()
    private val storiesList: MutableList<Story> = mutableListOf()

    fun getMyStoriesList(): List<MyStory> = my_storiesList.toList()
    fun getStoriesList(): List<Story> = storiesList.toList()
    var resultList: List<Following>? = null
    var from = ""

    fun Int.toPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    inner class ViewHolder(var binding: AdapterFollowersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Following, position: Int) {
            binding.apply {
                name.text = data.userDetails.name
                userName.text = data.userDetails.username
                userLocation.text = data.userDetails.city+", "+data.userDetails.country
                Glide.with(itemView.context).load(data.userDetails.profile_image).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
                when(data.userDetails.user_live_status){
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
                if (data.userDetails.isCreator==1){
                    verifiedNameTick.isVisible=true
                }else{
                    verifiedNameTick.isVisible=false
                }
                if (isMyProfile=="0"){
                    followButton.isVisible=false
                    followButtonLayout.isVisible=true
                    followButtonLayout2.isVisible=false
                }else{
                    followButton.isVisible=false
                    followButtonLayout.isVisible=false
                    followButtonLayout2.isVisible=true
                }
                if (isMyProfile=="0"){
                    Log.e("FOLLOW", "FOLLOW>>> "+data.isFollowing)
                    var user = Preferences.getCustomModelPreference<LoginResponse>(binding.root.context, LOGIN_DATA)?.payload?.username
                    if (data.userDetails.username==user){
                        followButtonLayout.isVisible = false
                    }else{
                        if (data.isFollowedByMe!=1){
                            followButtonLayout.isVisible = true
                            followButtonImage.setImageResource(R.drawable.follow_button_reel)
                        }else{
                            followButtonImage.setImageResource(R.drawable.following_button_reel)
                            followButtonLayout.isVisible = true
                        }
                    }
                }else{
                    if(from=="followers"){
                        if (data.isFollowing==1){
                            //followButton.text = "Unfollow"
                            followButtonImage2.setImageResource(R.drawable.following_button_reel)
                            followButtonLayout2.isVisible = true
                        }else{
                            //followButton.text = "Follow Back"
                            followButtonLayout2.isVisible = true
                            followButtonImage2.setImageResource(R.drawable.follow_button_reel)
                        }
                    }else{
                        //followButton.text = "Unfollow"
                        followButtonImage2.setImageResource(R.drawable.following_button_reel)
                        followButtonLayout2.isVisible = true
                    }
                }
                if(from=="followers"){
                    val story = storiesList?.find { story -> story.user_id == data.follower_id }
                    Log.e("TTTTT", "SSSSS>>> 1 "+story)
                    userImageCardView.background =
                        if(data.isStoryUploaded==1){
                            /*val padding = 6.toPx(root.context)
                            userImageCardView.setPadding(
                                padding,
                                padding,
                                padding,
                                padding
                            )
                            val sizeInDp = 42
                            val scale = root.context.resources.displayMetrics.density
                            val sizeInPx = (sizeInDp * scale).toInt()
                            val params = innerCard.layoutParams
                            params.width = sizeInPx
                            params.height = sizeInPx
                            innerCard.layoutParams = params */
                            if (story != null && story.is_seen == 0) {
                                root.context.resources.getDrawable(R.drawable.gredient_circle, null)
                            } else {
                                root.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
                            }
                        }else{
                            /*userImageCardView.setPadding(0, 0, 0, 0)
                            val sizeInDp = 55
                            val scale = root.context.resources.displayMetrics.density
                            val sizeInPx = (sizeInDp * scale).toInt()
                            val params = innerCard.layoutParams
                            params.width = sizeInPx
                            params.height = sizeInPx
                            innerCard.layoutParams = params */
                            root.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                        }
                }else{
                    val story = storiesList?.find { story -> story.user_id == data.following_id }
                    Log.e("TTTTT", "SSSSS>>> 2 "+story)
                    userImageCardView.background =
                        if(data.isStoryUploaded==1){
                            /*val padding = 6.toPx(root.context)
                            userImageCardView.setPadding(
                                padding,
                                padding,
                                padding,
                                padding
                            )
                            val sizeInDp = 42
                            val scale = root.context.resources.displayMetrics.density
                            val sizeInPx = (sizeInDp * scale).toInt()
                            val params = innerCard.layoutParams
                            params.width = sizeInPx
                            params.height = sizeInPx
                            innerCard.layoutParams = params */
                            if (story != null && story.is_seen == 0) {
                                root.context.resources.getDrawable(R.drawable.gredient_circle, null)
                            } else {
                                root.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
                            }
                        }else{
                            /*userImageCardView.setPadding(0, 0, 0, 0)
                            val sizeInDp = 55
                            val scale = root.context.resources.displayMetrics.density
                            val sizeInPx = (sizeInDp * scale).toInt()
                            val params = innerCard.layoutParams
                            params.width = sizeInPx
                            params.height = sizeInPx
                            innerCard.layoutParams = params */
                            root.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                        }
                }
                if (isMyProfile=="0"){
                    userImageCardView.setOnClickListener {
                        var user = Preferences.getCustomModelPreference<LoginResponse>(binding.root.context, LOGIN_DATA)?.payload?.username
                        if (data.userDetails.username==user){
                            if(from=="followers"){
                                getSelectedUser(data.follower_id, data.follower_id, 1, userImageCardView)
                            }else{
                                getSelectedUser(data.following_id, data.following_id, 1, userImageCardView)
                            }
                        }else{
                            if(data?.isStoryUploaded==1){
                                if(from=="followers"){
                                    RTVariable.USER_ID = data.follower_id.toString()
                                    getSelectedUser(data.follower_id, data.follower_id, 4, userImageCardView)
                                }else{
                                    RTVariable.USER_ID = data.following_id.toString()
                                    getSelectedUser(data.following_id, data.following_id, 4, userImageCardView)
                                }
                            }else{
                                if(from=="followers"){
                                    getSelectedUser(data.follower_id, data.follower_id, 1, userImageCardView)
                                }else{
                                    getSelectedUser(data.following_id, data.following_id, 1, userImageCardView)
                                }
                            }
                        }
                    }
                    userName.setOnClickListener {
                        if(from=="followers"){
                            getSelectedUser(data.follower_id, data.follower_id, 1, userName)
                        }else{
                            getSelectedUser(data.following_id, data.following_id, 1, userName)
                        }
                    }
                }else{
                    userImageCardView.setOnClickListener {
                        if(data?.isStoryUploaded==1){
                            if(from=="followers"){
                                RTVariable.USER_ID = data.follower_id.toString()
                                getSelectedUser(data.follower_id, data.follower_id, 4, userImageCardView)
                            }else{
                                RTVariable.USER_ID = data.following_id.toString()
                                getSelectedUser(data.following_id, data.following_id, 4, userImageCardView)
                            }
                        }else{
                            if(from=="followers"){
                                getSelectedUser(data.follower_id, data.follower_id, 1, userImageCardView)
                            }else{
                                getSelectedUser(data.following_id, data.following_id, 1, userImageCardView)
                            }
                        }
                    }
                    userName.setOnClickListener {
                        if(from=="followers"){
                            getSelectedUser(data.follower_id, data.follower_id, 1, userName)
                        }else{
                            getSelectedUser(data.following_id, data.following_id, 1, userName)
                        }
                    }
                }
                followButton.setOnClickListener {
                    if(from=="followers"){
                        if (data.isFollowing==1){
                            getSelectedUser(data.follower_id, data.isFollowing, 0, followButton)
                        }else{
                            getSelectedUser(data.follower_id, data.isFollowing, 0, followButton)
                        }
                    }else{
                        getSelectedUser(data.following_id, -1, 0, followButton)
                    }
                }
                followButtonImage.setOnClickListener {
                    Log.e("FOLLOW", "FOLLOW>>> "+data.isFollowing)
                    //getSelectedUser(data.follower_id, data.isFollowing, 3)
                    if(from=="followers"){
                        if (data.isFollowedByMe!=1){
                            getSelectedUser(data.follower_id, data.isFollowing, 2, followButtonImage)
                        }else{
                            getSelectedUser(data.follower_id, data.isFollowing, 3, followButtonImage)
                        }
                    }else{
                        if (data.isFollowedByMe!=1){
                            getSelectedUser(data.following_id, data.isFollowing, 2, followButtonImage)
                        }else{
                            getSelectedUser(data.following_id, data.isFollowing, 3, followButtonImage)
                        }
                    }
                }
                followButtonImage2.setOnClickListener {
                    Log.e("FOLLOW", "FOLLOW>>> "+data.isFollowing)
                    //getSelectedUser(data.follower_id, data.isFollowing, 3)
                    if(from=="followers"){
                        if (data.isFollowing==1){
                            getSelectedUser(data.follower_id, data.isFollowing, 0, followButtonImage2)
                        }else{
                            getSelectedUser(data.follower_id, data.isFollowing, 0, followButtonImage2)
                        }
                    }else{
                        getSelectedUser(data.following_id, -1, 0, followButtonImage2)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        if (resultList == null) {
            return 0
        } else {
            return resultList?.size!!
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterFollowersBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(resultList!![position], position)
    }

    fun setData(it: List<Following>) {
        resultList = it
        notifyDataSetChanged()
    }

    fun addStory(my_story: List<MyStory>, stories_List: List<Story>) {
        my_storiesList.addAll(my_story)
        storiesList.addAll(stories_List)
        notifyDataSetChanged()
    }

    fun updateStories(stories_List: List<Story>) {
        Log.e("TAG", "updateStories size = ${stories_List.size}")
        storiesList.clear()
        storiesList.addAll(stories_List)
        notifyItemRangeChanged(0, itemCount)
    }

}