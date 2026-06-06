package com.app.hihlo.ui.reels.adapter
import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.ItemReelBinding
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.reel.response.Reel
import com.app.hihlo.model.staticModel.reelSideOptionsList
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.adapter.PostsAdapter.CustomTypefaceSpan
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReelAdapter(
    private val currentUserId: String,
    private val reels: MutableList<Reel>,
    private val exoPlayer: ExoPlayer,
    private val followSelected: (Int, Int, Int) -> Unit,
    private val addReelSelected: (String) -> Unit,
    private val openSideOptions: (Int, Int, View) -> Unit,
    private val openProfile: (Int, View) -> Unit,
    private val shareReelSelected: (String) -> Unit,
    val from: String,
    private val getSelected: (Int, Int, Int, Int) -> Unit
) : RecyclerView.Adapter<ReelAdapter.ReelViewHolder>() {

    var currentPlayingPosition: Int = -1
     var currentViewHolder: ReelViewHolder? = null
    var adapterPosition: Int = 0
    private val storiesList: MutableList<Story> = mutableListOf()

    inner class ReelViewHolder(binding: ItemReelBinding) : RecyclerView.ViewHolder(binding.root) {
        val playerView = binding.playerView
         val loader = binding.videoLoader
//        private val title = binding.title
        private val userName = binding.userName
        private val userLocation = binding.userLocation
        private val captionCollapsed = binding.captionCollapsed
        private val moreLessText = binding.moreLessText
        private val captionExpanded = binding.captionExpanded
//        private val addReelButton = binding.addReelButton
        private val sideOptions = binding.sideOptions
        private val userImageCardView = binding.userImageCardView
        private val userImage = binding.userImage
        private val onlineStatusImage = binding.onlineStatusImage
        private val muteUnmuteIcon = binding.muteUnmuteIcon
        private val muteVolumeButton = binding.muteVolumeButton
        private val reelImageView = binding.reelImageView
        private val userDetailsLayout = binding.userDetailsLayout
        val followButtonLayout = binding.followButtonLayout
        val shadowLayerLayout = binding.shadowLayerLayout
        val followButtonImage = binding.followButtonImage
        val sideOptionsRecycler = binding.sideOptionsRecycler
        val shareReel = binding.shareReel
        var sideOptionAdapter:SideOptionsAdapter?=null


        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var muteIconJob: Job? = null
        private var muteIconJob2: Job? = null

        private val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (bindingAdapterPosition != currentPlayingPosition) return

                when (state) {
                    Player.STATE_BUFFERING -> {
                        loader.isVisible = true
                    }
                    Player.STATE_READY -> {
                        loader.isVisible = false
                    }
                    Player.STATE_ENDED -> {
                        // Restart from beginning
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                        loader.isVisible = false
                    }

                    Player.STATE_IDLE -> {
                        loader.isVisible = false
                    }
                }
            }
        }


        @OptIn(UnstableApi::class)
        fun bind(reel: Reel, isCurrent: Boolean, adapter_position: Int) {
            //RTVariable.REELS_POSITION = adapterPosition
            if (from=="profile"){
//                addReelButton.alpha = 0.5f
                if (reel.creatorId== Preferences.getCustomModelPreference<LoginResponse>(userLocation.context, LOGIN_DATA)?.payload?.userId) userLocation.isVisible=false else userLocation.isVisible=true
                Log.e("LOCATION", "LOCATION>>>${reel.creator.city}, ${reel.creator.country}")
                userLocation.text = "${reel.creator.city}, ${reel.creator.country}"

            }else{
                Log.e("LOCATION", "LOCATION>>>${reel.creator.city}, ${reel.creator.country}")
                userLocation.isVisible=true
                userLocation.text = "${reel.creator.city}, ${reel.creator.country}"
//                addReelButton.alpha = 1f
            }
            userName.text = reel.creator.name
//            caption.text = reel.caption
            Glide.with(itemView.context).load(reel.creator.profileImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(userImage)
            val story = storiesList?.find { story -> story.user_id == reel.creatorId }
            userImageCardView.background =
                if(reel.creator.isStoryUploaded==1){
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
                        itemView.context.resources.getDrawable(R.drawable.gredient_circle, null)
                    } else {
                        itemView.context.resources.getDrawable(R.drawable.gredient_circle_black, null)
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
                    itemView.context.resources.getDrawable(R.drawable.gredient_circle_transparent, null)
                }
            if (reel.creatorId.toString() == currentUserId){
                followButtonLayout.isVisible = false
            }else if (reel.isFollowing!=1){
                followButtonImage.setImageResource(R.drawable.follow_button_reel)
            }else{
                followButtonImage.setImageResource(R.drawable.following_button_reel)
            }
            when (reel.creator.user_live_status) {
                "1" -> onlineStatusImage.setImageResource(R.drawable.online_status_green)
                "2", "3" -> onlineStatusImage.setImageResource(R.drawable.offline_status_red)
//                "3" -> onlineStatusImage.setImageResource(R.drawable.busy_status)
            }
            followButtonLayout.setOnClickListener {
                if (reel.isFollowing!=1){
//                    localFollowList.add(reel.creatorId)
                    updateFollowingStatus(reel.creatorId, 1)
                    followSelected(reel.creatorId, position, 2)
                }else{
                    updateFollowingStatus(reel.creatorId, 2)
                    followSelected(reel.creatorId, position, 1)
                }
            }
            userImage.setOnClickListener {
                RTVariable.USER_ID = reel.creatorId.toString()
                openProfile(reel.creator.isStoryUploaded, userImage)
            }
            shareReel.setOnClickListener {
                shareReelSelected(reel.assetUrl)
            }
//            if (!reel.caption.isNullOrEmpty()) {
//                setDescriptionText(reel.caption, caption, moreLessText, shadowLayerLayout)
//            } else {
//                captionCollapsed.text = ""
//                moreLessText.visibility = View.GONE
//            }
            if (!reel.caption.isNullOrEmpty()) {
                val fullText = reel.caption
                captionCollapsed.text = fullText
                captionCollapsed.maxLines = 1
                captionCollapsed.ellipsize = TextUtils.TruncateAt.END
                captionCollapsed.visibility = View.VISIBLE
                captionExpanded.visibility = View.GONE
                moreLessText.visibility = View.GONE
                moreLessText.text = "More"
                captionCollapsed.post {
                    val layout = captionCollapsed.layout
                    if (layout != null) {
                        val isTruncated = layout.lineCount > 1 || (layout.lineCount == 1 && layout.getEllipsisCount(0) > 0)
                        moreLessText.visibility = if (isTruncated) View.VISIBLE else View.GONE
                    }
                }
                moreLessText.setOnClickListener {
                    //userDetailsLayout.setBackgroundColor(Color.parseColor("#70000000"))
                    shadowLayerLayout.isVisible = true
                    captionCollapsed.visibility = View.GONE
                    moreLessText.visibility = View.GONE
                    captionExpanded.visibility = View.VISIBLE
                    val spannable = SpannableStringBuilder(fullText)
                    val lessText = " Less"
                    spannable.append(lessText)
                    val typeface = ResourcesCompat.getFont(
                        itemView.context,
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
                            //userDetailsLayout.setBackgroundColor(Color.TRANSPARENT)
                            shadowLayerLayout.isVisible = false
                            captionExpanded.visibility = View.GONE
                            captionCollapsed.visibility = View.VISIBLE
                            moreLessText.visibility = View.VISIBLE
                        }
                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = itemView.context.getColor(R.color.theme)
                            ds.bgColor = 0
                        }
                    }
                    spannable.setSpan(
                        clickableSpan,
                        fullText.length,
                        spannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    captionExpanded.text = spannable
                    captionExpanded.movementMethod = LinkMovementMethod.getInstance()
                }
                captionExpanded.setOnClickListener {
                    //userDetailsLayout.setBackgroundColor(Color.TRANSPARENT)
                    shadowLayerLayout.isVisible = false
                    captionExpanded.visibility = View.GONE
                    captionCollapsed.visibility = View.VISIBLE
                    moreLessText.visibility = View.VISIBLE
                }
            } else {
                captionCollapsed.text = ""
                captionExpanded.text = ""
                moreLessText.visibility = View.GONE
            }
//            playerView.setControllerHideOnTouch(false)
            playerView.showController()
            playerView.controllerAutoShow = true
            exoPlayer?.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val width = videoSize.width
                    val height = videoSize.height

                    if (width > height) {
                        // Landscape video
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        // Portrait video
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            })
            playerView.setControllerShowTimeoutMs(0)
//            playerView.hideController()

            if (reel.assetType=="I"){
                reelImageView.isVisible=true
                playerView.isVisible=false
                loader.visibility = View.GONE
                Glide.with(itemView.context).load(reel.assetUrl).into(reelImageView)
            }else{
                reelImageView.isVisible=false
                playerView.isVisible=true
                if (isCurrent) {
                    playerView.player = exoPlayer
                    playerView.setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            if (visibility != View.VISIBLE) {
                                playerView.showController()
                            }
                        }
                    )


                    //loader.visibility = View.VISIBLE
//                    setLongPressBehavior()
                    playerView.setOnClickListener {
                        setPlayerViewClick(muteUnmuteIcon, false, adapter_position)
                    }
                } else {
                    playerView.player = null
                    loader.isVisible = false
                    userDetailsLayout.isVisible = true
                    sideOptionsRecycler.isVisible = true
                    playerView.setOnTouchListener(null)
                }
            }
            sideOptionAdapter = SideOptionsAdapter(reelSideOptionsList, reel.id, adapterPosition, reels[position].isLiked, from, reels[position].creatorId, currentUserId, reels[position].likesCount, reels[position].commentsCount) {
                if (it!=0) setPlayerViewClick(muteUnmuteIcon, true, adapter_position)
                getSelected(it, reel.id, position, reel.isLiked)
            }
            sideOptionsRecycler.adapter = sideOptionAdapter

            if (exoPlayer.volume==0f) muteVolumeButton.setImageResource(R.drawable.volume_unmute) else muteVolumeButton.setImageResource(R.drawable.volume_mute)
            muteVolumeButton.setOnClickListener {
                checkVolume(exoPlayer, muteVolumeButton)
            }
            /*addReelButton.setOnClickListener {
                if (from=="profile"){
                }else{
                    addReelSelected("profile")
                }
            }*/
            sideOptions.setOnClickListener {
                openSideOptions(reel.id, position, sideOptions)
            }
            //muteIconJob?.cancel()
            muteIconJob = scope.launch {
                delay(500)
                //muteUnmuteIcon.visibility = View.GONE
                if(UserDataManager.isPaused(itemView.context)){
                    muteUnmuteIcon.setImageResource(R.drawable.pause_icon)
                    muteUnmuteIcon.isVisible = true
                }else{
                    muteUnmuteIcon.setImageResource(R.drawable.play_icon)
                    if(exoPlayer.isPlaying){
                        muteUnmuteIcon.isVisible = false
                    }else{
                        muteUnmuteIcon.isVisible = false
                    }
                }
            }
        }
        fun updateFollowingStatus(creatorIdToMatch: Int, status: Int) {
            reels.forEach { reel ->
                if (reel.creatorId == creatorIdToMatch) {
                    reel.isFollowing = status
                }
            }
        }
        private fun checkVolume(exoPlayer: ExoPlayer, muteVolumeButton: ImageView) {
            val isCurrentlyMuted = exoPlayer.volume == 0f

            if (isCurrentlyMuted) {
                UserDataManager.setReelMute(itemView.context, false)
                exoPlayer.volume = 1f
                muteVolumeButton.setImageResource(R.drawable.volume_mute)
            } else {
                UserDataManager.setReelMute(itemView.context, true)
                exoPlayer.volume = 0f
                muteVolumeButton.setImageResource(R.drawable.volume_unmute)
            }
        }

        private fun setPlayerViewClick(muteUnmuteIcon:ImageView, forcePause: Boolean, adapter_position: Int) {
            val isActuallyPlaying = if (forcePause) true else exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY

            if (isActuallyPlaying) {
                exoPlayer.playWhenReady = false
                muteUnmuteIcon.setImageResource(R.drawable.play_icon)
                UserDataManager.setPause(itemView.context, true)
            } else {
                exoPlayer.playWhenReady = true
                muteUnmuteIcon.setImageResource(R.drawable.pause_icon)
                UserDataManager.setPause(itemView.context, false)
            }
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                UserDataManager.setPosition(itemView.context, pos)
                Log.e("REEL_POS", "Saved position = $pos")
            }
            val pos1 = UserDataManager.getPosition(itemView.context)
            Log.e("REEL_POS", "REEL_POS>>>> $pos1")

            //muteUnmuteIcon.visibility = View.VISIBLE
            muteIconJob?.cancel()
            muteIconJob = scope.launch {
                delay(500)
                //muteUnmuteIcon.visibility = View.GONE
                if(!exoPlayer.isPlaying){
                    muteUnmuteIcon.setImageResource(R.drawable.pause_icon)
                    muteUnmuteIcon.isVisible = true
                }else{
                    if(UserDataManager.isPaused(itemView.context)){
                        muteUnmuteIcon.setImageResource(R.drawable.pause_icon)
                        muteUnmuteIcon.isVisible = true
                    }else{
                        muteUnmuteIcon.setImageResource(R.drawable.play_icon)
                        muteUnmuteIcon.isVisible = false
                    }
                }
            }
//            muteIconJob = scope.launch {
//                delay(500)
//                if(UserDataManager.isPaused(itemView.context)){
//                    muteUnmuteIcon.setImageResource(R.drawable.pause_icon)
//                    muteUnmuteIcon.isVisible = true
//                }else{
//                    muteUnmuteIcon.setImageResource(R.drawable.play_icon)
//                    muteUnmuteIcon.isVisible = false
//                }
//            }
        }
        fun release() {
            scope.cancel()
        }
        @SuppressLint("ClickableViewAccessibility")
        private fun setLongPressBehavior() {
            playerView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        exoPlayer.playWhenReady = false
                        userDetailsLayout.isVisible=false
                        sideOptionsRecycler.isVisible=false
//                        playerView.hideController()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        exoPlayer.playWhenReady = true
                        userDetailsLayout.isVisible=true
                        sideOptionsRecycler.isVisible=true
//                        playerView.showController()
                        true
                    }
                    else -> false
                }
            }
        }
    }


    private fun setDescriptionText(
        fullCaption: String,
        captionTextView: TextView,
        moreLessTextView: TextView,
        shadowLayerLayout: ConstraintLayout
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
                            shadowLayerLayout.isVisible = true
//                            userDetailsLayout.setBackgroundColor(userDetailsLayout.context.getColor(R.color.white))
                        } else {
                            // Revert to one line with ellipsis
                            captionTextView.maxLines = 1
                            captionTextView.ellipsize = android.text.TextUtils.TruncateAt.END
                            captionTextView.text = fullCaption
                            moreLessTextView.text = "More"
                            shadowLayerLayout.isVisible = false
//                            userDetailsLayout.setBackgroundColor(userDetailsLayout.context.getColor(R.color.transparent))
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val binding = ItemReelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        val reel = reels[position]
        adapterPosition = position
        val isCurrent = position == currentPlayingPosition

        // Properly detach player from previous view
        if (isCurrent && holder != currentViewHolder) {
            currentViewHolder?.playerView?.player = null
            currentViewHolder = holder
        }

        holder.bind(reel, isCurrent, position)
    }

    override fun onViewRecycled(holder: ReelViewHolder) {
        super.onViewRecycled(holder)
        if (holder == currentViewHolder) {
            holder.playerView.player = null
            currentViewHolder = null
        }
//        holder.release()
    }

    fun updateList(list: MutableList<Reel>){
//        this.reels.addAll(list)
        var start = if (this.reels.isNotEmpty())
            this.reels.size else 0
        this.reels.addAll(list)
        notifyItemRangeInserted(start, this.reels.size)
    }

    fun appendReels(newReels: MutableList<Reel>) {
        val startIndex = reels.size                    // your internal list name (whatever you use)
        this.reels.addAll(newReels)
        notifyItemRangeInserted(startIndex, newReels.size)
    }

    fun addItems(newItems: List<Reel>) {
        val start = this.reels.size
        this.reels.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }
    fun updateLike(position: Int, updatedLikeStatus:Int){
        reels[position].isLiked=updatedLikeStatus
        if (updatedLikeStatus==1) reels[position].likesCount++ else reels[position].likesCount--
        currentViewHolder?.sideOptionAdapter?.update(updatedLikeStatus)
    }
    fun updateComment(position: Int){
        reels[position].commentsCount++
        currentViewHolder?.sideOptionAdapter?.updateCommentCount()
    }

    fun updateCommentCount(position: Int, count: Int){
        reels[position].commentsCount = count
        currentViewHolder?.sideOptionAdapter?.updateCommentCountDeleted(count)
    }

    fun updateFollow(position: Int, isAlreadyFollowed: Int){
        if (isAlreadyFollowed==2){
            reels[position].isFollowing = 1
            currentViewHolder?.followButtonImage?.setImageResource(R.drawable.following_button_reel)
        }else{
            reels[position].isFollowing = 2
            currentViewHolder?.followButtonImage?.setImageResource(R.drawable.follow_button_reel)
        }
    }

    fun updateStories(stories_List: List<Story>) {
        Log.e("TAG", "updateStories size = ${stories_List.size}")
        storiesList.clear()
        storiesList.addAll(stories_List)
        notifyItemRangeChanged(0, itemCount)
    }

    fun clearList(){
        var size = reels.size
        reels.clear()
        notifyItemRangeRemoved(0, size)
    }
    override fun getItemCount(): Int = reels.size
}



