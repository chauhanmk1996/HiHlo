package com.app.hihlo.ui.home.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.MetricAffectingSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterUserPostsBinding
import com.app.hihlo.model.get_profile.Data
import com.app.hihlo.model.get_profile.Posts
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter.CustomTypefaceSpan
import com.app.hihlo.utils.RTVariable
import com.bumptech.glide.Glide

class AdapterUserPostList(
    val homePosts: MutableList<Post>,
    val profilePosts: Posts,
    val from: String,
    val getSelectedPost: (Post, Data, Int, Int, View, View) -> Unit
) : RecyclerView.Adapter<AdapterUserPostList.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(AdapterUserPostsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                "commentCount" -> {
                    holder.binding.commentsCount.text =
                        if (from == "home") homePosts[position].commentsCount.toString()
                        else profilePosts.data[position].commentsCount.toString()
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        holder.binding.apply {
            when(from) {
                "home" -> {
                    Glide.with(root.context).load(homePosts[position].asset_url).into(postImage)
                    Glide.with(root.context).load(homePosts[position].creatorDetail?.profile_image)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(userImage)
                    userName.text = homePosts[position].creatorDetail?.name
                    likesCount.text = RTVariable.formatCount(homePosts[position].likesCount ?: 0)
                    commentsCount.text = RTVariable.formatCount(homePosts[position].commentsCount ?: 0)
                    userLocation.text = "${homePosts[position].creatorDetail?.city}, ${homePosts[position].creatorDetail?.country}"
                    if (homePosts[position].user_id.toString() == Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.userId.toString()) {
                        onlineStatusImage.isVisible = false
                    } else {
                        onlineStatusImage.isVisible = true
                    }
                    if (homePosts[position]?.is_follow!=1){
                        followButtonLayout.isVisible = true
                        followButtonImage.setImageResource(R.drawable.follow_button_reel)
                    }else{
                        followButtonImage.setImageResource(R.drawable.following_button_reel)
                        followButtonLayout.isVisible = true
                    }
                    if (homePosts[position].isLiked == 1) {
                        Glide.with(root.context).load(R.drawable.btn_heart_fill).into(likeImage)
                    } else {
                        Glide.with(root.context).load(R.drawable.btn_heart_normal).into(likeImage)
                    }
                    if (homePosts[position].creatorDetail?.is_creator == 2) {
                        verifiedNameTick.isVisible = true
                    } else {
                        verifiedNameTick.isVisible = false
                    }
                    /*if (!homePosts[position].caption.isNullOrEmpty()) {
                        setDescriptionText(
                            homePosts[position].caption!!,
                            captionFirstLine,
                            captionRemaining,
                            moreLessText
                        )
                    } else {
//                        caption.text = ""
                        moreLessText.visibility = View.GONE
                    }*/
                    when (homePosts[position].creatorDetail?.user_live_status) {
                        "1" -> onlineStatusImage.setImageResource(R.drawable.online_status_green)
                        "2", "3" -> onlineStatusImage.setImageResource(R.drawable.offline_status_red)
//                        "3" -> onlineStatusImage.setImageResource(R.drawable.busy_status)
                    }
                    sideOptions.setOnClickListener {
                        getSelectedPost(homePosts[position], Data(), 0, position, sideOptions, sideOptions)
                    }
                    userImageCardView.setOnClickListener {
                        getSelectedPost(homePosts[position], Data(), 1, position, sideOptions, userImageCardView)
                    }
                    likeImage.setOnClickListener {
                        if (homePosts[position].isLiked == 1) {
                            homePosts[position].isLiked = 2
                            if (homePosts[position].likesCount != null) {
                                homePosts[position].likesCount = homePosts[position].likesCount?.minus(1)
                                likesCount.text = homePosts[position].likesCount.toString()
                            }
                            Glide.with(root.context).load(R.drawable.btn_heart_normal).into(likeImage)
                        } else {
                            homePosts[position].isLiked = 1
                            if (homePosts[position].likesCount != null) {
                                homePosts[position].likesCount = homePosts[position].likesCount?.plus(1)
                                likesCount.text = homePosts[position].likesCount.toString()
                            }
                            Glide.with(root.context).load(R.drawable.btn_heart_fill).into(likeImage)
                        }
                        getSelectedPost(homePosts[position], Data(), 2, position, sideOptions, likeImage)
                    }
                    commentImage.setOnClickListener {
                        RTVariable.POST_ID = homePosts[position].id.toString()
                        RTVariable.COMMENT_FROM = false
                        Toast.makeText(root.context, "H", Toast.LENGTH_SHORT).show()
                        getSelectedPost(homePosts[position], Data(), 3, position, sideOptions, commentImage)
                    }
                    shareImage.setOnClickListener {
                        getSelectedPost(homePosts[position], Data(), 4, position, sideOptions, shareImage)
                    }
                    followButtonImage.setOnClickListener {
                        if (homePosts[position]?.is_follow!=1){
                            getSelectedPost(homePosts[position], Data(), 5, position, sideOptions, followButtonImage)
                        }else{
                            getSelectedPost(homePosts[position], Data(), 6, position, sideOptions, followButtonImage)
                        }
                    }
                }
                else -> {
                    Log.e("POSTDATA", "POSTDATA>>> "+profilePosts.data[position])
                    verifiedNameTick.isVisible = false
                    onlineStatusImage.isVisible = false
                    postImage.setImageDrawable(null)
                    postImage.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    postImage.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    postImage.requestLayout()
                    Glide.with(root.context).load(profilePosts.data[position].asset_url).into(postImage)
                    Glide.with(root.context).load(profilePosts.data[position].creator_profile_image)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(userImage)
                    userName.text = profilePosts.data[position].creator_name
                    likesCount.text = RTVariable.formatCount(profilePosts.data[position].likesCount ?: 0)
                    commentsCount.text = RTVariable.formatCount(profilePosts.data[position].commentsCount ?: 0)
                    userLocation.text = "${profilePosts.data[position].userCity}, ${profilePosts.data[position].userCountry}"
                    if (profilePosts.data[position].user_id.toString() == Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.userId.toString()) {
                        onlineStatusImage.isVisible = false
                    } else {
                        onlineStatusImage.isVisible = true
                    }
                    if (profilePosts.data[position].isLiked == 1) {
                        Glide.with(root.context).load(R.drawable.btn_heart_fill).into(likeImage)
                    } else {
                        Glide.with(root.context).load(R.drawable.btn_heart_normal).into(likeImage)
                    }
                    var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                    Log.e("USERNAME", "USERNAME>>> ${profilePosts.data[position].creator_username}")
                    if (profilePosts.data[position].creator_username==user){
                        followButtonLayout.isVisible = false
                    }else{
                        if(RTVariable.USER_IS_FOLLOWING){
                            followButtonLayout.isVisible = true
                            followButtonImage.setImageResource(R.drawable.following_button_reel)
                        }else{
                            followButtonLayout.isVisible = true
                            followButtonImage.setImageResource(R.drawable.follow_button_reel)
                        }
                    }
                    if (profilePosts.data[position].creator_username==user){
                        giftImage.isVisible = false
                    }else{
                        giftImage.isVisible = true
                    }
                    //var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                    //Log.e("USERNAME", "USERNAME>>> ${profilePosts.data[position].creator_username}")
                    if (profilePosts.data[position].creator_username==user){
                        giftImage.isVisible = false
                    }else{
                        giftImage.isVisible = true
                    }
//                    else if (homePosts[position]?.is_follow!=1){
//                        followButtonLayout.isVisible = true
//                        followButtonImage.setImageResource(R.drawable.follow_button_reel)
//                    }else{
//                        followButtonImage.setImageResource(R.drawable.following_button_reel)
//                        followButtonLayout.isVisible = true
//                    }
//                    if (!profilePosts.data[position].caption.isNullOrEmpty()) {
//                        setDescriptionText(
//                            profilePosts.data[position].caption ?: "",
//                            captionFirstLine,
//                            captionRemaining,
//                            moreLessText
//                        )
//                    } else {
////                        caption.text = ""
//                        moreLessText.visibility = View.GONE
//                    }
                    if (!profilePosts.data[position].caption.isNullOrEmpty()) {
                        //val fullText = "Effective post captions for English content should be concise, engaging, and directly related to the visual, often using a \"hook\" to grab attention within the first sentence. Popular styles include short, punchy phrases, or more detailed, storytelling paragraphs that provide context and value.".trim()
                        val fullText = profilePosts.data[position].caption
                        // Collapsed setup
                        captionCollapsed.text = fullText
                        captionCollapsed.maxLines = 1
                        captionCollapsed.ellipsize = TextUtils.TruncateAt.END
                        captionCollapsed.visibility = View.VISIBLE
                        captionExpanded.visibility = View.GONE
                        moreLessText.visibility = View.GONE
                        moreLessText.text = "More"

                        // Detect if truncation is needed
                        captionCollapsed.post {
                            val layout = captionCollapsed.layout
                            if (layout != null) {
                                val isTruncated = layout.lineCount > 1 || (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)
                                moreLessText.visibility = if (isTruncated) View.VISIBLE else View.GONE
                            }
                        }

                        // "More" → switch to expanded with "Less" appended
                        moreLessText.setOnClickListener {
                            captionCollapsed.visibility = View.GONE
                            moreLessText.visibility = View.GONE
                            captionExpanded.visibility = View.VISIBLE

                            // Create spannable with " Less" at the end
                            val spannable = SpannableStringBuilder(fullText)
                            val lessText = " Less"
                            spannable.append(lessText)
                            val typeface = ResourcesCompat.getFont(
                                root.context,
                                R.font.manrope_bold
                            )
                            typeface?.let {
                                fullText?.let { it1 ->
                                    spannable.setSpan(
                                        CustomTypefaceSpan(it),
                                        it1.length,
                                        spannable.length,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                            }
                            val clickableSpan = object : ClickableSpan() {
                                override fun onClick(widget: View) {
                                    // "Less" clicked → collapse
                                    captionExpanded.visibility = View.GONE
                                    captionCollapsed.visibility = View.VISIBLE
                                    moreLessText.visibility = View.VISIBLE  // re-show "More" if needed
                                }

                                override fun updateDrawState(ds: TextPaint) {
                                    super.updateDrawState(ds)
                                    ds.isUnderlineText = false
                                    ds.color = root.context.getColor(R.color.theme) // or your @color/theme
                                    ds.bgColor = 0 // no background, or set if you want pill effect
                                }
                            }

                            fullText?.let { it1 ->
                                spannable.setSpan(
                                    clickableSpan,
                                    it1.length,
                                    spannable.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                            captionExpanded.text = spannable
                            captionExpanded.movementMethod = LinkMovementMethod.getInstance()
                        }
                        captionExpanded.setOnClickListener {
                            captionExpanded.visibility = View.GONE
                            captionCollapsed.visibility = View.VISIBLE
                            moreLessText.visibility = View.VISIBLE
                        }
                    } else {
                        captionCollapsed.text = ""
                        captionExpanded.text = ""
                        moreLessText.visibility = View.GONE
                    }
                    val story = storiesList?.find { story -> story.user_id == profilePosts.data[position].user_id }
                    //Log.e("TTTTT", "SSSSS>>> "+story)
                    userImageCardView.background =
                        if(profilePosts.data[position].is_story_uploaded==1){
                            /*val padding = 6.toPx(root.context)
                            userImageCardView.setPadding(
                                padding,
                                padding,
                                padding,
                                padding
                            )
                            val sizeInDp = 27
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
                            val sizeInDp = 39
                            val scale = root.context.resources.displayMetrics.density
                            val sizeInPx = (sizeInDp * scale).toInt()
                            val params = innerCard.layoutParams
                            params.width = sizeInPx
                            params.height = sizeInPx
                            innerCard.layoutParams = params */
                            root.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                        }
                    sideOptions.setOnClickListener {
                        getSelectedPost(Post(), profilePosts.data[position], 0, position, sideOptions, sideOptions)
                    }
                    userImageCardView.setOnClickListener {
                        var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                        if (profilePosts.data[position].creator_name==user){
                            getSelectedPost(Post(), profilePosts.data[position], 1, position, sideOptions, userImageCardView)
                        }else{
                            val story = storiesList.find { it.user_id == profilePosts.data[position].user_id }
                            if (story != null) {
                                val storyPosition = storiesList.indexOfFirst { it.user_id == profilePosts.data[position].user_id }
                                RTVariable.STORY_POSITION = storyPosition
                                getSelectedPost(Post(), profilePosts.data[position], 8, position, sideOptions, userImageCardView)
                            } else {
                                getSelectedPost(Post(), profilePosts.data[position], 1, position, sideOptions, userImageCardView)
                            }
                        }
                    }
                    userName.setOnClickListener {
                        getSelectedPost(Post(), profilePosts.data[position], 1, position, sideOptions, userName)
                    }
                    verifiedNameTick.isVisible = true
                    likeImage.setOnClickListener {
                        if (profilePosts.data[position].isLiked == 1) {
                            profilePosts.data[position].isLiked = 2
                            if (profilePosts.data[position].likesCount != null) {
                                profilePosts.data[position].likesCount = profilePosts.data[position].likesCount?.minus(1)
                                likesCount.text = profilePosts.data[position].likesCount.toString()
                            }
                            Glide.with(root.context).load(R.drawable.btn_heart_normal).into(likeImage)
                            likeImage.scaleX = 1f
                            likeImage.scaleY = 1f
                            likeImage.alpha = 1f
                        } else {
                            profilePosts.data[position].isLiked = 1
                            if (profilePosts.data[position].likesCount != null) {
                                profilePosts.data[position].likesCount = profilePosts.data[position].likesCount?.plus(1)
                                likesCount.text = profilePosts.data[position].likesCount.toString()
                            }
                            Glide.with(root.context).load(R.drawable.btn_heart_fill).into(likeImage)
                            likeImage.apply {
                                scaleX = 0.7f
                                scaleY = 0.7f
                                alpha = 0.5f
                                animate()
                                    .scaleX(1.4f)
                                    .scaleY(1.4f)
                                    .alpha(1f)
                                    .setDuration(180)
                                    .withEndAction {
                                        animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(180)
                                            .start()
                                    }
                                    .start()
                            }
                        }
                        getSelectedPost(Post(), profilePosts.data[position], 2, position, sideOptions, likeImage)
                    }
                    btnLike.setOnClickListener {
                        if (profilePosts.data[position].isLiked == 1) {
                            profilePosts.data[position].isLiked = 2
                            if (profilePosts.data[position].likesCount != null) {
                                profilePosts.data[position].likesCount = profilePosts.data[position].likesCount?.minus(1)
                                likesCount.text = profilePosts.data[position].likesCount.toString()
                            }
                            Glide.with(root.context).load(R.drawable.btn_heart_normal).into(likeImage)
                            likeImage.scaleX = 1f
                            likeImage.scaleY = 1f
                            likeImage.alpha = 1f
                        } else {
                            profilePosts.data[position].isLiked = 1
                            if (profilePosts.data[position].likesCount != null) {
                                profilePosts.data[position].likesCount = profilePosts.data[position].likesCount?.plus(1)
                                likesCount.text = profilePosts.data[position].likesCount.toString()
                            }
                            Glide.with(root.context).load(R.drawable.btn_heart_fill).into(likeImage)
                            likeImage.apply {
                                scaleX = 0.7f
                                scaleY = 0.7f
                                alpha = 0.5f
                                animate()
                                    .scaleX(1.4f)
                                    .scaleY(1.4f)
                                    .alpha(1f)
                                    .setDuration(180)
                                    .withEndAction {
                                        animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(180)
                                            .start()
                                    }
                                    .start()
                            }
                        }
                        getSelectedPost(Post(), profilePosts.data[position], 2, position, sideOptions, btnLike)
                    }
                    commentImage.setOnClickListener {
                        RTVariable.POST_POSITION = position
                        RTVariable.POST_ID = profilePosts.data[position].id.toString()
                        RTVariable.COMMENT_FROM = false
                        //Toast.makeText(root.context, "P ${profilePosts.data[position].id}", Toast.LENGTH_SHORT).show()
                        getSelectedPost(Post(), profilePosts.data[position], 3, position, sideOptions, commentImage)
                    }
                    btnComment.setOnClickListener {
                        RTVariable.POST_POSITION = position
                        RTVariable.POST_ID = profilePosts.data[position].id.toString()
                        RTVariable.COMMENT_FROM = false
                        //Toast.makeText(root.context, "P ${profilePosts.data[position].id}", Toast.LENGTH_SHORT).show()
                        getSelectedPost(Post(), profilePosts.data[position], 3, position, sideOptions, btnComment)
                    }
                    shareImage.setOnClickListener {
                        getSelectedPost(Post(), profilePosts.data[position], 4, position, sideOptions, shareImage)
                    }
                    followButtonImage.setOnClickListener {
                        //Toast.makeText(root.context, "A ${profilePosts.data[position].user_id}", Toast.LENGTH_LONG).show()
                        if(RTVariable.USER_IS_FOLLOWING){
                            getSelectedPost(Post(), profilePosts.data[position], 6, position, sideOptions, followButtonImage)
                        }else{
                            getSelectedPost(Post(), profilePosts.data[position], 5, position, sideOptions, followButtonImage)
                        }
                    }
                    giftImage.setOnClickListener {
                        //val post = profilePosts.data[position] ?: return@setOnClickListener
                        getSelectedPost(Post(), profilePosts.data[position], 7, position, sideOptions, giftImage)
                    }
                }
            }
        }
    }

    class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {

        override fun updateDrawState(tp: TextPaint) {
            apply(tp)
        }

        override fun updateMeasureState(tp: TextPaint) {
            apply(tp)
        }

        private fun apply(paint: TextPaint) {
            paint.typeface = typeface
            paint.flags = paint.flags or Paint.SUBPIXEL_TEXT_FLAG
        }
    }

    fun Int.toPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private val my_storiesList: MutableList<MyStory> = mutableListOf()
    private val storiesList: MutableList<Story> = mutableListOf()

    fun getMyStoriesList(): List<MyStory> = my_storiesList.toList()
    fun getStoriesList(): List<Story> = storiesList.toList()

    fun updateFollow(position: Int){
        notifyItemRangeChanged(0, itemCount)
    }

    fun update_comment_count(count: Int, position: Int){
        profilePosts.data[position].commentsCount = count
        notifyItemChanged(position)
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

    private fun setDescriptionText(
        fullText: String,
        firstLine: TextView,
        remaining: TextView,
        moreLess: TextView
    ) {
        // Initial collapsed state
        firstLine.text = fullText
        firstLine.maxLines = 1
        firstLine.ellipsize = TextUtils.TruncateAt.END

        remaining.visibility = View.GONE
        moreLess.visibility = View.GONE

        firstLine.post {
            val layout = firstLine.layout ?: return@post

            if (layout.getEllipsisCount(0) > 0) {
                // Find the last visible character before ellipsis
                val ellipsisStart = layout.getEllipsisStart(0)
                val safeCut = fullText.lastIndexOf(' ', ellipsisStart)
                if (safeCut <= 0) return@post

                // Collapsed text: first line without last word
                val firstCollapsed = fullText.substring(0, safeCut).trim()

                // Full first line (before ellipsis) to compute the last word
                val firstLineEnd = layout.getLineEnd(0)
                val firstFull = fullText.substring(0, firstLineEnd).trim()

                // Last word of first line (to move to remaining)
                val lastWord = firstFull.substring(firstCollapsed.length).trim()

                // Remaining text: last word + rest of text
                val remainingText = (lastWord + " " + fullText.substring(firstLineEnd)).trim()

                // Set collapsed text
                firstLine.text = firstCollapsed
                moreLess.visibility = View.VISIBLE
                moreLess.text = "More"

                moreLess.setOnClickListener {
                    if (moreLess.text == "More") {
                        // EXPAND
                        firstLine.text = firstCollapsed
                        firstLine.ellipsize = null
                        firstLine.maxLines = 1

                        remaining.text = remainingText
                        remaining.visibility = View.VISIBLE

                        moreLess.text = "Less"
                    } else {
                        // COLLAPSE
                        firstLine.text = firstCollapsed
                        firstLine.ellipsize = TextUtils.TruncateAt.END
                        firstLine.maxLines = 1

                        remaining.visibility = View.GONE
                        moreLess.text = "More"
                    }
                }
            }
        }
    }












   /* private fun setDescriptionText(
        fullCaption: String,
        captionTextView: TextView,
        moreLessTextView: TextView
    ) {
        // Set initial state: show full text to measure, hide More/Less button
        captionTextView.text = fullCaption
        captionTextView.maxLines = 1 // Initially limit to one line
        moreLessTextView.visibility = View.GONE

        // Post to ensure layout is available
        captionTextView.post {
            val layout = captionTextView.layout
            if (layout != null) {
                // Check if text exceeds one line
                val isTruncated = layout.lineCount > 1 ||
                        (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)

                if (isTruncated) {
                    // Text exceeds one line, show "More" button
                    moreLessTextView.visibility = View.VISIBLE
                    moreLessTextView.text = "More"

                    // Ensure initial display is one line with ellipsis
                    captionTextView.maxLines = 1
                    captionTextView.ellipsize = android.text.TextUtils.TruncateAt.END

                    moreLessTextView.setOnClickListener {
                        if (moreLessTextView.text == "More") {
                            // Show full text
                            captionTextView.maxLines = Integer.MAX_VALUE
                            captionTextView.ellipsize = null
                            captionTextView.text = fullCaption
                            moreLessTextView.text = "Less"
                        } else {
                            // Revert to one line with ellipsis
                            captionTextView.maxLines = 1
                            captionTextView.ellipsize = android.text.TextUtils.TruncateAt.END
                            captionTextView.text = fullCaption
                            moreLessTextView.text = "More"
                        }
                    }
                } else {
                    // Text fits in one line, no need for "More" button
                    captionTextView.maxLines = 1
                    captionTextView.ellipsize = null
                    captionTextView.text = fullCaption
                    moreLessTextView.visibility = View.GONE
                }
            }
        }
    }*/

    override fun getItemCount(): Int = if (from == "home") homePosts.size else profilePosts.data.size

    fun updateCommentCount(position: Int) {
        if (from == "home") {
            homePosts[position].commentsCount = homePosts[position].commentsCount?.plus(1)
        } else {
            profilePosts.data[position].commentsCount = profilePosts.data[position].commentsCount?.plus(1)
        }
        notifyItemChanged(position, "commentCount") // use payload for partial update
    }

    inner class ViewHolder(val binding: AdapterUserPostsBinding) : RecyclerView.ViewHolder(binding.root)

}