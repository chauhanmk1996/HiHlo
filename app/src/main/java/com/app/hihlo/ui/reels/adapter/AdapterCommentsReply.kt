package com.app.hihlo.ui.reels.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterCommentsReplyBinding
import com.app.hihlo.model.get_reel_comments.response.Replies
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import androidx.core.graphics.toColorInt

class AdapterCommentsReply(
    var comment_id: String,
    var replies: MutableList<Replies>,
    val onReplySelect: (String) -> Unit,               // optional — if you still want inline reply
    val onDeleteClick: (replyId: Int) -> Unit,
    val onReplyProfileSelected: (user_id: Int) -> Unit,// ← new
    val onMentionClick: (user_id: String) -> Unit
) : RecyclerView.Adapter<AdapterCommentsReply.ViewHolder>() {

    private var selectedPosition: Int = -1

    inner class ViewHolder(val binding: AdapterCommentsReplyBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterCommentsReplyBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return replies.size
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.binding.apply {
            if (position == selectedPosition) {
                holder.binding.parentLayout.setBackgroundColor("#1AFFFFFF".toColorInt()) // light highlight
            } else {
                holder.binding.parentLayout.setBackgroundColor(Color.TRANSPARENT)
            }
            Glide.with(root.context)
                .load(replies[position].user.profile_image)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder).into(userImage)
            name.text = replies[position].user.username
//            userId.text = replies[position].user.username
            userId.isVisible=false
            var user_id = replies?.get(position)?.user?.id
            if(replies?.get(position)?.user?.isCreator?.toInt() == 1){
                verifiedNameTick.isVisible = true
            }else{
                verifiedNameTick.isVisible = false
            }
//            userLocation.text = replies[position]?.user?.city+", "+replies[position]?.user?.country
            //comment.text = replies[position].reply
            //var data = "https://d38vqutibeq2uv.cloudfront.net/1757159096618####@@@@####@hihlo####@@@@####hloo you"
            setRichComment(root.context, comment, replies[position])
            var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
            val commentOwner = replies?.get(position)?.comment_owner
            val commentUser = replies?.get(position)?.user?.username
            val commentOwnerUserName = replies?.get(position)?.post_owner_username
            if (commentOwner == 1 || user == commentUser) {
                if(user == commentOwnerUserName){
                    reply.isVisible = true
                }else{
                    if(user == commentUser){
                        reply.isVisible = false
                    } else {
                        reply.isVisible = true
                    }
                }
            } else {
                reply.isVisible = true
            }
            holder.binding.root.setOnLongClickListener {
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    RTVariable.COMMENT_POSITION = position
                    RTVariable.REPLY_POSITION = position
                    replies[position].id?.let { replyId ->
                        onDeleteClick(replyId)
                    }
                    true
                } else {
                    val replyText = replies[position].reply ?: ""
                    val isUserMentioned = replyText.contains(user.toString())
                    if(isUserMentioned){
                        selectedPosition = position
                        notifyDataSetChanged()
                        RTVariable.COMMENT_POSITION = position
                        RTVariable.REPLY_POSITION = position
                        replies[position].id?.let { replyId ->
                            onDeleteClick(replyId)
                        }
                        true
                    }else{
                        false
                    }
                }
            }
            holder.binding.comment.setOnLongClickListener {
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    RTVariable.COMMENT_POSITION = position
                    RTVariable.REPLY_POSITION = position
                    replies[position].id?.let { replyId ->
                        onDeleteClick(replyId)
                    }
                    true
                } else {
                    val replyText = replies[position].reply ?: ""
                    val isUserMentioned = replyText.contains(user.toString())
                    if(isUserMentioned){
                        selectedPosition = position
                        notifyDataSetChanged()
                        RTVariable.COMMENT_POSITION = position
                        RTVariable.REPLY_POSITION = position
                        replies[position].id?.let { replyId ->
                            onDeleteClick(replyId)
                        }
                        true
                    }else{
                        false
                    }
                }
            }
            holder.binding.userImage.setOnLongClickListener {
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    RTVariable.COMMENT_POSITION = position
                    RTVariable.REPLY_POSITION = position
                    replies[position].id?.let { replyId ->
                        onDeleteClick(replyId)
                    }
                    true
                } else {
                    val replyText = replies[position].reply ?: ""
                    val isUserMentioned = replyText.contains(user.toString())
                    if(isUserMentioned){
                        selectedPosition = position
                        notifyDataSetChanged()
                        RTVariable.COMMENT_POSITION = position
                        RTVariable.REPLY_POSITION = position
                        replies[position].id?.let { replyId ->
                            onDeleteClick(replyId)
                        }
                        true
                    }else{
                        false
                    }
                }
            }
            holder.binding.name.setOnLongClickListener {
                val allowLongClick = (commentOwner == 1 || user == commentUser) || user == commentOwnerUserName
                if (allowLongClick) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    RTVariable.COMMENT_POSITION = position
                    RTVariable.REPLY_POSITION = position
                    replies[position].id?.let { replyId ->
                        onDeleteClick(replyId)
                    }
                    true
                } else {
                    val replyText = replies[position].reply ?: ""
                    val isUserMentioned = replyText.contains(user.toString())
                    if(isUserMentioned){
                        selectedPosition = position
                        notifyDataSetChanged()
                        RTVariable.COMMENT_POSITION = position
                        RTVariable.REPLY_POSITION = position
                        replies[position].id?.let { replyId ->
                            onDeleteClick(replyId)
                        }
                        true
                    }else{
                        false
                    }
                }
            }
            holder.binding.reply.setOnClickListener {
                RTVariable.REPLY_COMBINED_IMAGE_USERNAME = replies?.get(position)?.user?.profile_image + RTVariable.REPLY_COMBINED_IMAGE_DELEMETER + replies?.get(position)?.user?.username + RTVariable.REPLY_COMBINED_IMAGE_DELEMETER
                onReplySelect(comment_id)
            }
            holder.binding.userImage.setOnClickListener {
                UserDataManager.postCommentIsShow(root.context, true)
                user_id?.let { p1 -> onReplyProfileSelected(p1) }
            }
            holder.binding.name.setOnClickListener {
                UserDataManager.postCommentIsShow(root.context, true)
                user_id?.let { p1 -> onReplyProfileSelected(p1) }
            }
//            holder.binding.delete.setOnClickListener {
//                RTVariable.COMMENT_POSITION = position
//                RTVariable.REPLY_POSITION = position
//                replies[position].id?.let { replyId ->
//                    onDeleteClick(replyId)
//                }
//            }
            /*sendButton.setOnClickListener {
                getSelectedReply(replyInput.text.toString())
            }*/
        }
    }

    fun removeItems(position: Int){
        Log.e("REPLYLOG", "REPLYLOG>>> REPLY "+position)
        replies.removeAt(position)
        notifyItemRemoved(position)
    }

    fun cancelSection(){
        selectedPosition = -1
        notifyDataSetChanged()
    }

    private fun setRichComment(context: Context, textView: TextView, reply: Replies) {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        val density = context.resources.displayMetrics.density
        val dpValue = 13.5f * (scaledDensity / density)
        val context = textView.context
        val mentionTypeface: Typeface? = ResourcesCompat.getFont(context, R.font.manrope_semibold)
        val rawComment = reply.reply
        if (rawComment.isNullOrEmpty()) {
            textView.text = ""
            return
        }
        if (!rawComment.contains(RTVariable.REPLY_COMBINED_IMAGE_DELEMETER)) {
            textView.text = rawComment
            return
        }
        val parts = rawComment.split(RTVariable.REPLY_COMBINED_IMAGE_DELEMETER)
        if (parts.size < 3) {
            textView.text = rawComment
            return
        }
        Log.e("REPLYLOG", "REPLYLOG>>> REPLY "+parts[1].trim())
        val imageUrl = parts[0].trim()
        val mention = parts[1].trim()
        val restText = parts[2].trim()
        val builder = SpannableStringBuilder()
        builder.append(" ")
        builder.append(" ")
        builder.append(mention)
        builder.append(" ")
        builder.append(restText)
        textView.text = builder
        val targetSize = (textView.textSize * 0.85f).toInt()
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .placeholder(R.drawable.profile_placeholder)
            .error(R.drawable.profile_placeholder)
            .circleCrop()
            .into(object : CustomTarget<Bitmap>(targetSize, targetSize) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val drawable = BitmapDrawable(context.resources, resource)
                    drawable.setBounds(0, 0, targetSize, targetSize)
                    val imageTopMargin = -8
                    val imageSpan = object : ImageSpan(drawable, ImageSpan.ALIGN_BASELINE) {
                        override fun getSize(
                            paint: Paint,
                            text: CharSequence?,
                            start: Int,
                            end: Int,
                            fm: Paint.FontMetricsInt?
                        ): Int {
                            val size = super.getSize(paint, text, start, end, fm)
                            if (fm != null) {
                                val imageHeight = drawable.bounds.height()
                                val fontHeight = fm.bottom - fm.top
                                val padding = (fontHeight - imageHeight) / 2
                                fm.top = padding
                                fm.ascent = padding
                                fm.bottom = padding + imageHeight
                                fm.descent = padding + imageHeight
                            }
                            return size
                        }
                        override fun draw(
                            canvas: Canvas,
                            text: CharSequence?,
                            start: Int,
                            end: Int,
                            x: Float,
                            top: Int,
                            y: Int,
                            bottom: Int,
                            paint: Paint
                        ) {
                            val b = drawable.bounds
                            var transY = (bottom - top - b.height()) / 2 + top
                            transY += imageTopMargin
                            canvas.save()
                            canvas.translate(x, transY.toFloat())
                            drawable.draw(canvas)
                            canvas.restore()
                        }
                    }
                    val spannable = SpannableStringBuilder(textView.text)
                    spannable.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val mentionStart = 2
                    val mentionEnd = mentionStart + mention.length
                    val clickableStart = 0
                    val clickableEnd = mentionEnd
                    val clickableSpan = object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            UserDataManager.postCommentIsShow(context, true)
                            onMentionClick(mention)
                        }
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = Color.parseColor("#B90A66")
                            mentionTypeface?.let {
                                ds.typeface = it
                            }
                        }
                    }
                    spannable.setSpan(clickableSpan, clickableStart, clickableEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    textView.text = spannable
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}