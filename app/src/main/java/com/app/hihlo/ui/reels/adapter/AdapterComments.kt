package com.app.hihlo.ui.reels.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterCommentsBinding
import com.app.hihlo.model.get_reel_comments.response.Comment
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.HomeNewFragmentDirections
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.reels.bottom_sheet.CommentReelBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import androidx.core.graphics.toColorInt
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.ui.home.view_model.UserPostListViewModel

class AdapterComments(
    var comments: MutableList<Comment>,
    val stories: ArrayList<Story>?,
    val onReplyClick: (replyText: String, parentCommentId: Int) -> Unit,
    val onDeleteClick: (isReply: Boolean, parentCommentId: Int?, itemId: Int) -> Unit,
    val onReplySelected: (commentId: Int) -> Unit,
    val onProfileSelected: (commentId: Int) -> Unit,
    val onProfileImageSelected: (commentId: Int, view: View) -> Unit,
    val onMentionClick: (user_id: String) -> Unit,
    val commentsRecycler: RecyclerView,
    private val viewModel: UserPostListViewModel
) : RecyclerView.Adapter<AdapterComments.ViewHolder>() {

    private var selectedPosition: Int = -1

    lateinit var adapter: AdapterCommentsReply

    // Tracks how many replies are currently visible for each comment (1 = first reply only)
    private val visibleReplyCounts = mutableMapOf<Int, Int>()

    inner class ViewHolder(val binding: AdapterCommentsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterCommentsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.binding.apply {
            val commentItem = comments[position]
            val commentId = commentItem.id ?: -1
            if (position == selectedPosition) {
                holder.binding.parentLayout.setBackgroundColor("#1AFFFFFF".toColorInt()) // light highlight
            } else {
                holder.binding.parentLayout.setBackgroundColor(Color.TRANSPARENT)
            }
            Glide.with(root.context).load(commentItem.user?.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(userImage)
            val story = stories?.find { story -> story.user_id == commentItem.user.id }
            Log.e("TTTTT", "SSSSS>>> 1 "+story)
            userImageCardView.background =
                if(commentItem.user.isStoryUploaded==1){
                    if (story != null && story.is_seen == 0) {
                        root.context.resources.getDrawable(R.drawable.gredient_circle, null)
                    } else {
                        root.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
                    }
                }else{
                    root.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                }
            name.text = commentItem.user?.username
            userId.isVisible = false
            comment.text = commentItem.comment
            verifiedNameTick.isVisible = commentItem.user?.isCreator?.toInt() == 2
//            if(commentItem.user?.isCreator?.toInt() == 1){
//                verifiedNameTick.isVisible = true
//            }else{
//                verifiedNameTick.isVisible = false
//            }

            // ----- Reply Visibility Logic -----
            val totalReplies = commentItem.replies?.size ?: 0
            // Default: show first reply if there is at least one reply
            var visibleCount = visibleReplyCounts.getOrDefault(commentId, if (totalReplies > 0) 1 else 0)
            if (visibleCount > totalReplies) visibleCount = totalReplies
            visibleReplyCounts[commentId] = visibleCount

            val displayReplies = if (visibleCount > 0) {
                commentItem.replies?.take(visibleCount) ?: emptyList()
            } else {
                emptyList()
            }

            adapter = AdapterCommentsReply(
                comment_id = commentItem.id.toString(),
                replies = displayReplies.toMutableList(),
                stories,
                onReplySelect = { comment_id ->
                    onReplySelected(comment_id.toInt())
                },
                onDeleteClick = { replyId ->
                    RTVariable.INNER_COMMENT_POSITION = position
                    onDeleteClick(true, commentItem.id, replyId)
                },
                onReplyProfileSelected = { user_id ->
                    UserDataManager.postCommentPosition(root.context, position)
                    UserDataManager.setCommentToScroll(root.context, true)
                    onProfileSelected(user_id)
                },
                onReplyProfileImageSelected = { user_id, view ->
                    UserDataManager.postCommentPosition(root.context, position)
                    UserDataManager.setCommentToScroll(root.context, true)
                    onProfileImageSelected(user_id, view)
                },
                onMentionClick = { user_name ->
                    UserDataManager.postCommentPosition(root.context, position)
                    UserDataManager.setCommentToScroll(root.context, true)
                    onMentionClick(user_name)
                }
            )
            commentReplyRecycler.adapter = adapter
            commentReplyRecycler.isNestedScrollingEnabled = false

            // ----- Visibility of Delete / Reply Buttons (existing logic) -----
            val user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
            val commentOwner = commentItem.comment_owner
            val commentUser = commentItem.user?.username
            val commentOwnerUserName = commentItem.post_owner_username

//            delete.isVisible = when {
//                (commentOwner == 1 || user == commentUser) && user == commentOwnerUserName -> true
//                else -> false
//            }
            root.setOnLongClickListener {
                val user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                val commentOwner = commentItem.comment_owner
                val commentUser = commentItem.user?.username
                val commentOwnerUserName = commentItem.post_owner_username
                Log.e("DELETE", "DELETE>>> ${user}")
                Log.e("DELETE", "DELETE>>> ${commentOwner}")
                Log.e("DELETE", "DELETE>>> ${commentUser}")
                Log.e("DELETE", "DELETE>>> ${commentOwnerUserName}")
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    commentItem.id?.let { id ->
                        RTVariable.COMMENT_POSITION = position
                        onDeleteClick(false, commentItem.id, id)
                    }
                    true
                } else {
                    if(user == commentOwnerUserName){
                        selectedPosition = position
                        notifyDataSetChanged()
                        commentItem.id?.let { id ->
                            RTVariable.COMMENT_POSITION = position
                            onDeleteClick(false, commentItem.id, id)
                        }
                        true
                    }else{
                        false
                    }
                    //false
                }
            }
            comment.setOnLongClickListener {
                val user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                val commentOwner = commentItem.comment_owner
                val commentUser = commentItem.user?.username
                val commentOwnerUserName = commentItem.post_owner_username
                Log.e("DELETE", "DELETE>>> ${user}")
                Log.e("DELETE", "DELETE>>> ${commentOwner}")
                Log.e("DELETE", "DELETE>>> ${commentUser}")
                Log.e("DELETE", "DELETE>>> ${commentOwnerUserName}")
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    commentItem.id?.let { id ->
                        RTVariable.COMMENT_POSITION = position
                        onDeleteClick(false, commentItem.id, id)
                    }
                    true
                } else {
                    if(user == commentOwnerUserName){
                        selectedPosition = position
                        notifyDataSetChanged()
                        commentItem.id?.let { id ->
                            RTVariable.COMMENT_POSITION = position
                            onDeleteClick(false, commentItem.id, id)
                        }
                        true
                    }else{
                        false
                    }
                    //false
                }
            }
            userImage.setOnLongClickListener {
                val user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                val commentOwner = commentItem.comment_owner
                val commentUser = commentItem.user?.username
                val commentOwnerUserName = commentItem.post_owner_username
                Log.e("DELETE", "DELETE>>> ${user}")
                Log.e("DELETE", "DELETE>>> ${commentOwner}")
                Log.e("DELETE", "DELETE>>> ${commentUser}")
                Log.e("DELETE", "DELETE>>> ${commentOwnerUserName}")
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    commentItem.id?.let { id ->
                        RTVariable.COMMENT_POSITION = position
                        onDeleteClick(false, commentItem.id, id)
                    }
                    true
                } else {
                    if(user == commentOwnerUserName){
                        selectedPosition = position
                        notifyDataSetChanged()
                        commentItem.id?.let { id ->
                            RTVariable.COMMENT_POSITION = position
                            onDeleteClick(false, commentItem.id, id)
                        }
                        true
                    }else{
                        false
                    }
                    //false
                }
            }
            name.setOnLongClickListener {
                val user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                val commentOwner = commentItem.comment_owner
                val commentUser = commentItem.user?.username
                val commentOwnerUserName = commentItem.post_owner_username
                Log.e("DELETE", "DELETE>>> ${user}")
                Log.e("DELETE", "DELETE>>> ${commentOwner}")
                Log.e("DELETE", "DELETE>>> ${commentUser}")
                Log.e("DELETE", "DELETE>>> ${commentOwnerUserName}")
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    commentItem.id?.let { id ->
                        RTVariable.COMMENT_POSITION = position
                        onDeleteClick(false, commentItem.id, id)
                    }
                    true
                } else {
                    if(user == commentOwnerUserName){
                        selectedPosition = position
                        notifyDataSetChanged()
                        commentItem.id?.let { id ->
                            RTVariable.COMMENT_POSITION = position
                            onDeleteClick(false, commentItem.id, id)
                        }
                        true
                    }else{
                        false
                    }
                    //false
                }
            }
            reply.setOnClickListener {
                RTVariable.REPLY_COMBINED_IMAGE_USERNAME = commentItem.user?.profile_image + RTVariable.REPLY_COMBINED_IMAGE_DELEMETER +
                        commentItem.user?.username + RTVariable.REPLY_COMBINED_IMAGE_DELEMETER
                onReplySelected(commentItem.id ?: -1)
            }
            // ----- "View more / Hide" Button Logic -----
            // Only show button if there are more than 1 reply
            viewMoreLayout.isVisible = totalReplies > 1
            if (totalReplies > 1) {
                val remaining = totalReplies - visibleCount
                val text = if (visibleCount >= totalReplies) {
                    "Hide replies"
                } else {
                    "View $remaining more repl${if (remaining > 1) "ies" else "y"}"
                }
                viewReplies.text = text
            }
            commentReplyRecycler.isVisible = visibleCount > 0
            viewReplies.setOnClickListener {
                val currentComment = comments.getOrNull(position) ?: return@setOnClickListener
                val currentId = currentComment.id ?: return@setOnClickListener
                val total = currentComment.replies?.size ?: 0
                val currentVisible = visibleReplyCounts.getOrDefault(currentId, if (total > 0) 1 else 0)

                if (currentVisible >= total) {
                    // Hide all except first reply
                    visibleReplyCounts[currentId] = if (total > 0) 1 else 0
                    notifyItemChanged(position)
                    commentsRecycler.scrollToPosition(position)
                } else {
                    // Show up to 10 more replies
                    val newVisible = currentVisible + 10
                    visibleReplyCounts[currentId] = if (newVisible > total) total else newVisible
                    notifyItemChanged(position)
                }
            }

            userImage.setOnClickListener {
//                NavOptions.Builder()
//                    .setLaunchSingleTop(true)
//                    .setRestoreState(true)
//                    .build()
                Log.e("PO", "PO>>> "+position)
                Log.i("TAG", "onViewCreated: PZ " + position)
                var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                if (commentItem.user?.username==user){
                    UserDataManager.postCommentPosition(root.context, position)
                    UserDataManager.setCommentToScroll(root.context, true)
                    UserDataManager.postCommentIsShow(root.context, true)
                    onProfileSelected(commentItem.user?.id ?: -1)
                }else{
                    if(commentItem.user.isStoryUploaded==1){
                        if (story != null && story.is_seen == 0) {
                            UserDataManager.postCommentPosition(root.context, position)
                            UserDataManager.setCommentToScroll(root.context, true)
                            UserDataManager.postCommentIsShow(root.context, true)
                            onProfileImageSelected(commentItem.user?.id ?: -1, userImage)
                        } else {
                            UserDataManager.postCommentPosition(root.context, position)
                            UserDataManager.setCommentToScroll(root.context, true)
                            UserDataManager.postCommentIsShow(root.context, true)
                            onProfileImageSelected(commentItem.user?.id ?: -1, userImage)
                        }
                    }else{
                        UserDataManager.postCommentPosition(root.context, position)
                        UserDataManager.setCommentToScroll(root.context, true)
                        UserDataManager.postCommentIsShow(root.context, true)
                        onProfileSelected(commentItem.user?.id ?: -1)
                    }
                }
            }
            name.setOnClickListener {
//                NavOptions.Builder()
//                    .setLaunchSingleTop(true)
//                    .setRestoreState(true)
//                    .build()
                UserDataManager.postCommentPosition(root.context, position)
                UserDataManager.setCommentToScroll(root.context, true)
                UserDataManager.postCommentIsShow(root.context, true)
                onProfileSelected(commentItem.user?.id ?: -1)
            }

        }
    }

    fun updateList(comments: List<Comment>) {
        this.comments.clear()
        this.comments.addAll(comments)
        // Reset visible counts – new list, start fresh
        visibleReplyCounts.clear()
        notifyDataSetChanged()
    }

    fun cancelSection(){
        selectedPosition = -1
        adapter.cancelSection()
        notifyDataSetChanged()
    }

    fun removeItems(mode: Int, commentPosition: Int, replyPosition: Int = -1) {
        if (mode == 1) {
            // Remove a top-level comment
            val removedId = comments.getOrNull(commentPosition)?.id
            if (removedId != null) {
                visibleReplyCounts.remove(removedId)
            }
            comments.removeAt(commentPosition)
            notifyItemRemoved(commentPosition)
        } else if (mode == 2 && replyPosition != -1) {
            // Remove a reply
            val replies = comments[commentPosition].replies?.toMutableList() ?: mutableListOf()
            if (replyPosition < replies.size) {
                replies.removeAt(replyPosition)
            }
            comments[commentPosition].replies = replies
            // Do not adjust visibleCount here – will be corrected in onBindViewHolder
            notifyItemChanged(commentPosition)
        }
    }

    fun addItems(newComments: List<Comment>) {
        val start = comments.size
        comments.addAll(newComments)
        // Initialize visible counts for new comments (show first reply if available)
        newComments.forEach { comment ->
            comment.id?.let { id ->
                if ((comment.replies?.size ?: 0) > 0 && !visibleReplyCounts.containsKey(id)) {
                    visibleReplyCounts[id] = 1
                }
            }
        }
        notifyItemRangeInserted(start, newComments.size)
    }

    /* --------------------------------------------------------------------------
        Original setRichComment method (commented out – kept for reference)
    -------------------------------------------------------------------------- */
    /*
    private fun setRichComment(textView: TextView, rawComment: String?) {
        // ... original implementation ...
    }
    */
}