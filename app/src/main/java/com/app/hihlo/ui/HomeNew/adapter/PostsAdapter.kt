package com.app.hihlo.ui.HomeNew.adapter

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
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterNewUserPostBinding
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Post
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.fragment.HomeFragmentDirections
import com.app.hihlo.ui.reels.adapter.ReelAdapter.ReelViewHolder
import com.app.hihlo.utils.ExpandableTextViewHelper
import com.app.hihlo.utils.RTVariable
import com.bumptech.glide.Glide

class PostsAdapter(
    private val actionListener: PostActionListener? = null,
    private val onPostClick: ((Post) -> Unit)? = null   // optional - keep only if needed
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return postsList.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID
    }

    enum class PostClickAction {
        LIKE,
        UNLIKE,
        OPTIONS_MENU,
        SHARE,
        COMMENT,
        POST_BODY,
        POST_PROFILE,
        POST_FOLLOW,
        POST_UNFOLLOW,
        GIFT,
        POST_PROFILE_NAME,
        TOWARDS_STORY
    }

    var currentViewHolder: PostViewHolder? = null

    interface PostActionListener {
        fun onPostAction(post: Post, action: PostClickAction, position: Int, view: View)
    }

    private val postsList: MutableList<Post> = mutableListOf()
    private val my_storiesList: MutableList<MyStory> = mutableListOf()
    private val storiesList: MutableList<Story> = mutableListOf()

    fun getMyStoriesList(): List<MyStory> = my_storiesList.toList()
    fun getStoriesList(): List<Story> = storiesList.toList()

    fun addPosts(morePosts: List<Post>, my_story: List<MyStory>, stories_List: List<Story>) {
        val start = postsList.size
        postsList.addAll(morePosts)
        my_storiesList.addAll(my_story)
        storiesList.addAll(stories_List)
        notifyItemRangeInserted(start, morePosts.size)   // ← better than notifyDataSetChanged
    }

    fun updateStories(newStories: List<Story>) {
        storiesList.clear()
        storiesList.addAll(newStories)

        postsList.forEachIndexed { index, post ->
            val hasStory = storiesList.any { it.user_id == post.user_id }
            if (hasStory) {
                notifyItemChanged(index, "STORY_UPDATE")
            }
        }
    }

    fun setPosts(newPosts: List<Post>, my_story: List<MyStory>, stories_List: List<Story>) {
        postsList.clear()
        postsList.addAll(newPosts)
        my_storiesList.addAll(my_story)
        storiesList.addAll(stories_List)
        notifyDataSetChanged()   // ok for first page or refresh
    }

    fun clearList(){
        //var size = postsList.size
        //postsList.clear()
        //notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = AdapterNewUserPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postsList[position])
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.contains("STORY_UPDATE")) {
            holder.updateStoryUI(postsList[position])
        } else {
            holder.bind(postsList[position])
        }
    }

    fun updateFollow(position: Int, isAlreadyFollowed: Int){
        postsList[position].is_follow = isAlreadyFollowed
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int = postsList.size

    fun Int.toPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    inner class PostViewHolder(private val binding: AdapterNewUserPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.userImage.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                var user = Preferences.getCustomModelPreference<LoginResponse>(binding.root.context, LOGIN_DATA)?.payload?.username
                if (post.creatorDetail?.username==user){
                    actionListener?.onPostAction(
                        post,
                        PostClickAction.POST_PROFILE,
                        adapterPosition,
                        binding.userImage
                    )
                }else{
                    if(post.creatorDetail?.isStoryUploaded==1){
                        actionListener?.onPostAction(
                            post,
                            PostClickAction.TOWARDS_STORY,
                            adapterPosition,
                            binding.userImage
                        )
                    }else{
                        actionListener?.onPostAction(
                            post,
                            PostClickAction.POST_PROFILE,
                            adapterPosition,
                            binding.userImage
                        )
                    }
                }
            }
            binding.userName.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                //var user = Preferences.getCustomModelPreference<LoginResponse>(binding.root.context, LOGIN_DATA)?.payload?.username
                actionListener?.onPostAction(
                    post,
                    PostClickAction.POST_PROFILE,
                    adapterPosition,
                    binding.userImage
                )
            }
//            binding.userName.setOnClickListener {
//                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
//                actionListener?.onPostAction(
//                    post,
//                    PostClickAction.POST_PROFILE_NAME,
//                    adapterPosition,
//                    binding.userName
//                )
//            }
            binding.likeImage.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                val action = if (post.isLiked == 1) PostClickAction.UNLIKE else PostClickAction.LIKE
                if (post.isLiked  == 1) {
                    post.isLiked  = 2
                    if (post.likesCount != null) {
                        post.likesCount = post.likesCount?.minus(1)
                        binding.likesCount.text = post.likesCount.toString()
                    }
                    Glide.with(binding.root.context).load(R.drawable.btn_heart_normal).into(binding.likeImage)
                    binding.likeImage.scaleX = 1f
                    binding.likeImage.scaleY = 1f
                    binding.likeImage.alpha = 1f
                } else {
                    post.isLiked = 1
                    if (post.likesCount != null) {
                        post.likesCount = post.likesCount?.plus(1)
                        binding.likesCount.text = post.likesCount.toString()
                    }
                    Glide.with(binding.root.context).load(R.drawable.btn_heart_fill).into(binding.likeImage)
                    binding.likeImage.apply {
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
                actionListener?.onPostAction(post, action, adapterPosition, binding.likeImage)
            }
            binding.btnLike.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                val action = if (post.isLiked == 1) PostClickAction.UNLIKE else PostClickAction.LIKE
                if (post.isLiked  == 1) {
                    post.isLiked  = 2
                    if (post.likesCount != null) {
                        post.likesCount = post.likesCount?.minus(1)
                        binding.likesCount.text = post.likesCount.toString()
                    }
                    Glide.with(binding.root.context).load(R.drawable.btn_heart_normal).into(binding.likeImage)
                    binding.likeImage.scaleX = 1f
                    binding.likeImage.scaleY = 1f
                    binding.likeImage.alpha = 1f
                } else {
                    post.isLiked = 1
                    if (post.likesCount != null) {
                        post.likesCount = post.likesCount?.plus(1)
                        binding.likesCount.text = post.likesCount.toString()
                    }
                    Glide.with(binding.root.context).load(R.drawable.btn_heart_fill).into(binding.likeImage)
                    binding.likeImage.apply {
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
                actionListener?.onPostAction(post, action, adapterPosition, binding.likeImage)
            }
            binding.commentImage.setOnClickListener {   // ← add this if not already clickable
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                RTVariable.POST_ID = post.id.toString()
                RTVariable.COMMENT_FROM = false
                actionListener?.onPostAction(
                    post,
                    PostClickAction.COMMENT,
                    adapterPosition,
                    binding.commentImage
                )
            }
            binding.btnComment.setOnClickListener {   // ← add this if not already clickable
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                RTVariable.POST_ID = post.id.toString()
                RTVariable.COMMENT_FROM = false
                actionListener?.onPostAction(
                    post,
                    PostClickAction.COMMENT,
                    adapterPosition,
                    binding.commentImage
                )
            }
            binding.shareImage.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                actionListener?.onPostAction(
                    post,
                    PostClickAction.SHARE,
                    adapterPosition,
                    binding.shareImage
                )
            }
            binding.followButtonImage.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                if (post.is_follow!=1){
                    actionListener?.onPostAction(post, PostClickAction.POST_FOLLOW, adapterPosition, binding.sideOptions)
                }else{
                    actionListener?.onPostAction(post, PostClickAction.POST_UNFOLLOW, adapterPosition, binding.sideOptions)
                }
            }
            // Options menu / three dots / side options
            binding.sideOptions.setOnClickListener {    // ← assuming @+id/sideOptions exists
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                actionListener?.onPostAction(post, PostClickAction.OPTIONS_MENU, adapterPosition, binding.sideOptions)
            }
            binding.giftImage.setOnClickListener {
                val post = postsList.getOrNull(adapterPosition) ?: return@setOnClickListener
                actionListener?.onPostAction(post, PostClickAction.GIFT, adapterPosition, binding.sideOptions)
            }
        }
        fun bind(post: Post) {
            with(binding) {
                post.creatorDetail?.let {
                    userName.text = it.name
                    userLocation.text = "${it.city}, ${it.country}"
                    verifiedNameTick.isVisible = it.is_creator == 1
                }
                var user = Preferences.getCustomModelPreference<LoginResponse>(root.context, LOGIN_DATA)?.payload?.username
                if (post.creatorDetail?.username==user){
                    followButtonLayout.isVisible = false
                }else {
                    if (post?.is_follow!=1){
                        followButtonLayout.isVisible = true
                        followButtonImage.setImageResource(R.drawable.follow_button_reel)
                    }else{
                        followButtonImage.setImageResource(R.drawable.following_button_reel)
                        followButtonLayout.isVisible = true
                    }
                }
                if (post.creatorDetail?.username==user){
                    giftImage.isVisible = false
                }else{
                    giftImage.isVisible = true
                }
                Log.e("TAG", "IS FOLLOW: "+ post?.is_follow)
                when (post.creatorDetail?.user_live_status) {
                    "1" -> onlineStatusImage.setImageResource(R.drawable.online_status_green)
                    "2", "3" -> onlineStatusImage.setImageResource(R.drawable.offline_status_red)
//                        "3" -> onlineStatusImage.setImageResource(R.drawable.busy_status)
                }
                Glide.with(root.context).load(post.creatorDetail?.profile_image)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(userImage)
                postImage.setImageDrawable(null)
                postImage.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                postImage.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                postImage.requestLayout()
                //Glide.with(root.context).clear(postImage)
                Glide.with(root.context).load(post.asset_url).into(postImage)
                val story = storiesList?.find { story -> story.user_id == post.user_id }
                //Log.e("TTTTT", "SSSSS>>> "+story)
                userImageCardView.background =
                    if(post.creatorDetail?.isStoryUploaded == 1){
                        /*val padding = 6.toPx(root.context)
                        binding.userImageCardView.setPadding(
                            padding,
                            padding,
                            padding,
                            padding
                        )
                        val sizeInDp = 27
                        val scale = root.context.resources.displayMetrics.density
                        val sizeInPx = (sizeInDp * scale).toInt()
                        val params = binding.innerCard.layoutParams
                        params.width = sizeInPx
                        params.height = sizeInPx
                        binding.innerCard.layoutParams = params */
                        if (story != null && story.is_seen == 0) {
                            root.context.resources.getDrawable(R.drawable.gredient_circle, null)
                        } else {
                            root.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
                        }
                    }else{
                        /*binding.userImageCardView.setPadding(0, 0, 0, 0)
                        val sizeInDp = 39
                        val scale = root.context.resources.displayMetrics.density
                        val sizeInPx = (sizeInDp * scale).toInt()
                        val params = binding.innerCard.layoutParams
                        params.width = sizeInPx
                        params.height = sizeInPx
                        binding.innerCard.layoutParams = params */
                        root.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                    }
                likesCount.text = RTVariable.formatCount(post.likesCount ?: 0)
                commentsCount.text = RTVariable.formatCount(post.commentsCount ?: 0)
                if (post.isLiked == 1) {
                    Glide.with(root.context).load(R.drawable.btn_heart_fill).into(likeImage)
                } else {
                    Glide.with(root.context).load(R.drawable.btn_heart_normal).into(likeImage)
                }
                if (!post.caption.isNullOrEmpty()) {
                    val fullText = post.caption
                    binding.captionCollapsed.text = fullText
                    binding.captionCollapsed.maxLines = 1
                    binding.captionCollapsed.ellipsize = TextUtils.TruncateAt.END
                    binding.captionCollapsed.visibility = View.VISIBLE
                    binding.captionExpanded.visibility = View.GONE
                    binding.moreLessText.visibility = View.GONE
                    binding.moreLessText.text = "More"
                    binding.captionCollapsed.post {
                        val layout = binding.captionCollapsed.layout
                        if (layout != null) {
                            val isTruncated = layout.lineCount > 1 || (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)
                            binding.moreLessText.visibility = if (isTruncated) View.VISIBLE else View.GONE
                        }
                    }
                    binding.moreLessText.setOnClickListener {
                        binding.captionCollapsed.visibility = View.GONE
                        binding.moreLessText.visibility = View.GONE
                        binding.captionExpanded.visibility = View.VISIBLE
                        val spannable = SpannableStringBuilder(fullText)
                        val lessText = " Less"
                        spannable.append(lessText)
                        val typeface = ResourcesCompat.getFont(
                            binding.root.context,
                            R.font.manrope_bold
                        )
                        typeface?.let {
                            spannable.setSpan(
                                CustomTypefaceSpan(it),
                                fullText.length,
                                spannable.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                binding.captionExpanded.visibility = View.GONE
                                binding.captionCollapsed.visibility = View.VISIBLE
                                binding.moreLessText.visibility = View.VISIBLE
                            }
                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = false
                                ds.color = binding.root.context.getColor(R.color.theme)
                                ds.bgColor = 0
                            }
                        }
                        spannable.setSpan(
                            clickableSpan,
                            fullText.length,
                            spannable.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.captionExpanded.text = spannable
                        binding.captionExpanded.movementMethod = LinkMovementMethod.getInstance()
                    }
                    binding.captionExpanded.setOnClickListener {
                        binding.captionExpanded.visibility = View.GONE
                        binding.captionCollapsed.visibility = View.VISIBLE
                        binding.moreLessText.visibility = View.VISIBLE
                    }
                } else {
                    binding.captionCollapsed.text = ""
                    binding.captionExpanded.text = ""
                    binding.moreLessText.visibility = View.GONE
                }
            }
        }
        fun updateStoryUI(post: Post) {
            val context = binding.root.context

            val story = storiesList.find { it.user_id == post.user_id }

            binding.userImageCardView.background =
                if (post.creatorDetail?.isStoryUploaded == 1) {
                    if (story != null && story.is_seen == 0) {
                        context.getDrawable(R.drawable.gredient_circle)
                    } else {
                        context.getDrawable(R.drawable.gredient_circle_black)
                    }
                } else {
                    context.getDrawable(R.drawable.gredient_circle_transparent)
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

    private fun setDescriptionText(fullCaption: String?, captionTextView: TextView, moreLessTextView: TextView) {
        if (fullCaption.isNullOrBlank()) {
            captionTextView.text = ""
            moreLessTextView.visibility = View.GONE
            return
        }

        captionTextView.text = fullCaption
        captionTextView.maxLines = 1
        captionTextView.ellipsize = TextUtils.TruncateAt.END
        moreLessTextView.visibility = View.GONE

        captionTextView.post {
            val layout = captionTextView.layout ?: return@post

            val isTruncated = layout.lineCount > 1 ||
                    (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)

            if (isTruncated) {
                moreLessTextView.visibility = View.VISIBLE
                moreLessTextView.text = "More"

                moreLessTextView.setOnClickListener {
                    if (moreLessTextView.text == "More") {
                        captionTextView.maxLines = Int.MAX_VALUE
                        captionTextView.ellipsize = null
                        moreLessTextView.text = "Less"
                    } else {
                        captionTextView.maxLines = 1
                        captionTextView.ellipsize = TextUtils.TruncateAt.END
                        moreLessTextView.text = "More"
                    }
                }
            }
//            captionTextView.post {
//                // If the text is not truncated, hide the "More" button
//                if (captionTextView.layout != null) {
//                    val isTruncated = captionTextView.layout.getEllipsisCount(captionTextView.lineCount - 1) > 0
//                    moreLessTextView.visibility = if (isTruncated) View.VISIBLE else View.GONE
//                }
//            }
        }
    }

//    private fun setDescriptionText(
//        fullText: String,
//        firstLine: TextView,
//        remaining: TextView,
//        moreLess: TextView
//    ) {
//        firstLine.text = fullText
//        firstLine.maxLines = 1
//        firstLine.ellipsize = TextUtils.TruncateAt.END
//        remaining.visibility = View.GONE
//        moreLess.visibility = View.GONE
//        firstLine.post {
//            val layout = firstLine.layout ?: return@post
//            if (layout.getEllipsisCount(0) > 0) {
//                val ellipsisStart = layout.getEllipsisStart(0)
//                val safeCut = fullText.lastIndexOf(' ', ellipsisStart)
//                if (safeCut <= 0) return@post
//                val firstCollapsed = fullText.substring(0, safeCut).trim()
//                val firstLineEnd = layout.getLineEnd(0)
//                val firstFull = fullText.substring(0, firstLineEnd).trim()
//                val lastWord = firstFull.substring(firstCollapsed.length).trim()
//                val remainingText = (lastWord + " " + fullText.substring(firstLineEnd)).trim()
//                firstLine.text = firstCollapsed
//                moreLess.visibility = View.VISIBLE
//                moreLess.text = "More"
//                moreLess.setOnClickListener {
//                    if (moreLess.text == "More") {
//                        firstLine.text = firstCollapsed
//                        firstLine.ellipsize = null
//                        firstLine.maxLines = 1
//                        remaining.text = remainingText
//                        remaining.visibility = View.VISIBLE
//                        firstLine.ellipsize = TextUtils.TruncateAt.END
//                        moreLess.text = "Less"
//                    } else {
//                        firstLine.text = firstCollapsed
//                        firstLine.ellipsize = TextUtils.TruncateAt.END
//                        firstLine.maxLines = 1
//                        remaining.visibility = View.GONE
//                        moreLess.text = "More"
//                    }
//                }
//            }
//        }
//    }
}