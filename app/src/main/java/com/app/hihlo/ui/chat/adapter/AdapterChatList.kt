package com.app.hihlo.ui.chat.adapter

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterChatListBinding
import com.app.hihlo.model.get_recent_chat.response.RecentChat
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.utils.ChatUtils.getChatListDay
import com.app.hihlo.utils.ChatUtils.getCurrentTime
import com.app.hihlo.utils.ChatUtils.getDay
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import java.util.Locale
import java.util.Locale.getDefault

class AdapterChatList(
    var chats: List<RecentChat>,
    val getSelectedChat: (position: Int, click: Int, view: View) -> Unit,
    val onLongTap: (position: Int, click: Int, view: View) -> Unit,
    val view: View,
    val isInboxSelected: Boolean
) : RecyclerView.Adapter<AdapterChatList.ViewHolder>() {

    inner class ViewHolder(val binding: AdapterChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var longPressTriggered = false

        private val gestureDetector = GestureDetector(binding.root.context,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    longPressTriggered = false
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    longPressTriggered = true
                    onLongTap(bindingAdapterPosition, 0, binding.root)
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (!longPressTriggered) {
                        getSelectedChat(bindingAdapterPosition, 1, binding.root)
                    }
                    return true
                }
            }
        )

        init {
            binding.root.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterChatListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = chats.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            name.text = chats[position].userDetails?.name
            userId.text = chats[position].message
            var messageTime = getChatListDay(chats[position].chatUpdatedAt ?: "", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            lastMessageTime.text = if(messageTime != "Today") messageTime else getCurrentTime("HH:mm a").uppercase(
                getDefault()
            )
//            Glide.with(root.context).load(chats[position].userDetails?.profileImage)
//                .placeholder(R.drawable.profile_placeholder)
//                .error(R.drawable.profile_placeholder)
//                .into(userImage)
            Glide.with(root.context)
                .load(chats[position].userDetails?.profileImage)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .skipMemoryCache(false)
                .thumbnail(0.1f)
                .override(200, 200)
                .centerCrop()
                .dontAnimate()
                .into(userImage)
//            Glide.with(userImage)
//                .load(chats[position].userDetails?.profileImage)
////                .placeholder(R.drawable.profile_placeholder)
////                .error(R.drawable.profile_placeholder)
////                .diskCacheStrategy(DiskCacheStrategy.ALL)
////                .skipMemoryCache(false)
////                .signature(ObjectKey(chats[position].userDetails?.name ?: "1"))
//                .into(userImage)

            verifiedNameTick.isVisible = chats[position].userDetails?.isCreator == 1

            when (chats[position].userDetails?.user_live_status) {
                "1" -> onlineStatusImage.setImageResource(R.drawable.online_status_green)
                "2", "3" -> onlineStatusImage.setImageResource(R.drawable.offline_status_red)
//                "3" -> onlineStatusImage.setImageResource(R.drawable.busy_status)
            }
            if (position==chats.size-1) bottomLine.isVisible=false else bottomLine.isVisible=true
            if (chats[position].userDetails?.is_story_uploaded == 1) {
                if (chats[position].userDetails?.is_seen == 0) {
                    myStoryGradient.background = root.resources.getDrawable(
                        R.drawable.gredient_circle, null
                    )
                } else {
                    myStoryGradient.background = root.resources.getDrawable(
                        R.drawable.gredient_circle_black, null
                    )
                }
                userImage.setOnClickListener {
                    getSelectedChat(position, 0, root)
                }
            } else {
                myStoryGradient.background = root.resources.getDrawable(
                    R.drawable.gredient_circle_transparent, null
                )
            }
            if (isInboxSelected){
                if (Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.userId==chats[position].messageSentBy){
                    incomingMessageStatus.isVisible=false
                    outgoingMessageStatus.isVisible=true
                    if (chats[position].readStatus=="read"){
                        outgoingMessageStatus.setImageResource(R.drawable.seen_icon)
                    }else{
                        outgoingMessageStatus.setImageResource(R.drawable.send)
                    }
                }else{
                    incomingMessageStatus.isVisible=true
                    outgoingMessageStatus.isVisible=false
                    if (chats[position].readStatus=="read"){
                        incomingMessageStatus.isVisible=false
                    }else{
                        incomingMessageStatus.setImageResource(R.drawable.seen_chat)
                    }
                }
                incomingMessageStatus.isVisible=true
            }else{
                incomingMessageStatus.isVisible=true
                outgoingMessageStatus.isVisible=false

                incomingMessageStatus.setImageResource(R.drawable.request_chat_icon)

            }
        }
    }

    fun updateList(list: List<RecentChat>) {
        chats = list
        notifyDataSetChanged()
    }
}
