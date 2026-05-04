package com.app.hihlo.ui.reels.bottom_sheet

// Separate updated class for RoundedBottomSheet with pagination support.
// Assumes your AdapterComments has an additional function like:
// fun addItems(newItems: List<Comments>) {
//     val start = this.list.size // Assuming 'list' is the backing list in adapter
//     this.list.addAll(newItems)
//     notifyItemRangeInserted(start, newItems.size)
// }
// If not, add it to your adapter. Replace 'Comments' with the actual type if different.
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.BottomSheetLayoutBinding
import com.app.hihlo.model.delete_comment.DeleteToCommentRequest
import com.app.hihlo.model.get_reel_comments.response.Comment
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.get_reel_comments.response.User
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.post_comments.request.PostCommentsRequest
import com.app.hihlo.model.reply_to_comment.request.ReplyToCommentRequest
import com.app.hihlo.network_call.RetrofitBuilder
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.HomeNewFragmentDirections
import com.app.hihlo.ui.HomeNew.activity.PlayStatusActivity
import com.app.hihlo.ui.home.fragment.UserPostListFragmentDirections
import com.app.hihlo.ui.home.view_model.UserPostListViewModel
import com.app.hihlo.ui.reels.adapter.AdapterComments
import com.app.hihlo.ui.reels.fragment.ReelsFragment
import com.app.hihlo.ui.reels.fragment.ReelsFragmentDirections
import com.app.hihlo.ui.reels.view_model.ReelsViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.gson.Gson
import kotlinx.coroutines.launch

class CommentReelBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetLayoutBinding? = null
    private val binding get() = _binding!!
    var onCommentAction: ((PostCommentsRequest) -> Unit)? = null
    var onReplyAction: ((ReplyToCommentRequest) -> Unit)? = null
    var onDeleteAction: ((DeleteToCommentRequest) -> Unit)? = null
    var onLoadMore: ((Int, Int) -> Unit)? = null
    lateinit var adapter: AdapterComments
    var isReplySelected = false
    var commentId = ""
    private var currentPage = 1
    private val limit = 10
    private var isLoading = false
    private var hasMore = true

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
    private val viewModel2: UserPostListViewModel by viewModels()
    private var behavior: BottomSheetBehavior<FrameLayout>? = null
    private var isExpanding = false
    private var heightChangeRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if(RTVariable.IS_FROM_RESUME){
            isLoading = false
        }
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                behavior = BottomSheetBehavior.from(it)
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val peekHeight = (screenHeight * 0.5).toInt()
                behavior?.peekHeight = requireContext().dpToPx(525)
                behavior?.isFitToContents = true
                behavior?.skipCollapsed = false
                behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                val cornerRadius = 25f.toPx(requireContext())
                val shapeDrawable = MaterialShapeDrawable(
                    ShapeAppearanceModel.Builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                        .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                        .build()
                )
                shapeDrawable.fillColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_color))
                it.background = shapeDrawable
                Log.e("TTTTT", "TTTTT>>> "+UserDataManager.get_CommentExpandState(requireContext()))
                val isExpanded = UserDataManager.get_CommentExpandState(binding.root.context)
                if (isExpanded) {
                    UserDataManager.postCommentExpandState(binding.root.context, true)
                    isExpanding = false
                    it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    scheduleRecyclerViewHeightMatchParent()  // Delay setting height
                } else {
                    isExpanding = true
                    UserDataManager.postCommentExpandState(binding.root.context, false)
                    Log.e("TTTTT", "TTTTT>>> "+UserDataManager.get_CommentExpandState(binding.root.context))
                    it.layoutParams.height = requireContext().dpToPx(400)
                    val params = binding.commentsRecycler.layoutParams
                    params.height = requireContext().dpToPx(360)
                    binding.commentsRecycler.layoutParams = params
                }
                behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (RTVariable.IS_FROM_RESUME && behavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                            return
                        }
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                isExpanding = true

                                context?.let {
                                    UserDataManager.postCommentExpandState(it, false)
                                    Log.e("TTTTT", "TTTTT>>> " + UserDataManager.get_CommentExpandState(it))
                                }
                            }
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                UserDataManager.postCommentExpandState(binding.root.context, false)
                                Log.e("TTTTT", "TTTTT>>> "+UserDataManager.get_CommentExpandState(binding.root.context))
                                it.layoutParams.height = requireContext().dpToPx(400)
                                val params = binding.commentsRecycler.layoutParams
                                params.height = requireContext().dpToPx(360)
                                binding.commentsRecycler.layoutParams = params
                                isExpanding = true
                            }
                            BottomSheetBehavior.STATE_EXPANDED -> {
                                isExpanding = false
                                Log.e("TTTTT", "TTTTT>>> "+UserDataManager.get_CommentExpandState(binding.root.context))
                            }
                            BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                                isExpanding = false
                            }
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }
                })
                binding.mainContain.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        UserDataManager.postCommentExpandState(binding.root.context, true)
                        isExpanding = false
                        it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        scheduleRecyclerViewHeightMatchParent()  // Delay setting height
                    }
                    true
                }
                binding.closeLine.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        UserDataManager.postCommentExpandState(binding.root.context, true)
                        isExpanding = false
                        it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        scheduleRecyclerViewHeightMatchParent()  // Delay setting height
                    }
                    true
                }

                binding.titleTextView.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        UserDataManager.postCommentExpandState(binding.root.context, true)
                        isExpanding = false
                        it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        scheduleRecyclerViewHeightMatchParent()
                    }
                    true
                }

                binding.mainContain.setOnClickListener {
                    UserDataManager.postCommentExpandState(binding.root.context, true)
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    isExpanding = false
                    it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    scheduleRecyclerViewHeightMatchParent()
                }

                binding.closeLine.setOnClickListener {
                    UserDataManager.postCommentExpandState(binding.root.context, true)
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    isExpanding = false
                    it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    scheduleRecyclerViewHeightMatchParent()
                }

                binding.titleTextView.setOnClickListener {
                    UserDataManager.postCommentExpandState(binding.root.context, true)
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    isExpanding = false
                    it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    scheduleRecyclerViewHeightMatchParent()
                }
            }
        }

        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            behavior = BottomSheetBehavior.from(it)
            behavior?.peekHeight = requireContext().dpToPx(525)
            behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            UserDataManager.postCommentExpandState(binding.root.context, false)
        }
        return dialog
    }

    private fun scheduleRecyclerViewHeightMatchParent() {
        heightChangeRunnable?.let {
            binding.commentsRecycler.removeCallbacks(it)
        }

        heightChangeRunnable = Runnable {
            val b = _binding ?: return@Runnable

            val params = b.commentsRecycler.layoutParams
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            b.commentsRecycler.layoutParams = params

            // ✅ IMPORTANT FIX: Force pagination check after layout update
            b.commentsRecycler.post {
                checkPaginationAfterExpand()
            }

        }
        binding.commentsRecycler.postDelayed(heightChangeRunnable!!, 300)
    }

    private fun checkPaginationAfterExpand() {
        val recyclerView = binding.commentsRecycler
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        val isAtBottom = (firstVisibleItemPosition + visibleItemCount) >= totalItemCount
        val isListShort = totalItemCount <= visibleItemCount

        if (!isLoading && hasMore && (isAtBottom || isListShort)) {
            loadMore()
        }else{
            loadMore()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val payload = arguments?.getParcelable<Payload>("comments")
        val myStory = arguments?.getParcelable<MyStory>("myStory")
        val stories = arguments?.getParcelableArrayList<Story>("stories")
        Log.i("TAG", "onViewCreated: " + stories)
        Log.i("TAG", "onViewCreated: " + payload)
        Log.i("TAG", "onViewCreated: BRS " + UserDataManager.get_postCommentShow(requireContext()))
        Log.i("TAG", "onViewCreated: BRPID " + UserDataManager.get_postCommentPid(requireContext()))
        Log.i("TAG", "onViewCreated: BRP " + UserDataManager.get_postCommentPosition(requireContext()))
        val initialComments = payload?.comments ?: listOf()
        Log.i("TAG", "onViewCreated: Z " + initialComments.size)
        adapter = AdapterComments(
            initialComments.toMutableList(),
            stories,
            onReplyClick = { replyText, parentCommentId ->
                val request = ReplyToCommentRequest(
                    reply = replyText,
                    commentId = parentCommentId.toString()
                )
                onReplyAction?.invoke(request)
            },
            onDeleteClick = { isReply, parentCommentId, itemId ->
                val dialog = AlertDialog.Builder(requireContext())
                    .setMessage("Are you sure you want to delete this comment?")
                    .setPositiveButton("Delete") { d, _ ->
                        if (isReply) {
                            adapter.removeItems(
                                mode = 2,
                                commentPosition = RTVariable.INNER_COMMENT_POSITION,
                                replyPosition = RTVariable.REPLY_POSITION
                            )
                            if (RTVariable.COMMENT_FROM) {
                                getSendDeleteReelsComment(
                                    itemId.toString(),
                                    2,
                                    RTVariable.COMMENT_POSITION,
                                    RTVariable.POST_ID
                                )
                            } else {
                                getSendDeleteComment(
                                    itemId.toString(),
                                    2,
                                    RTVariable.COMMENT_POSITION,
                                    RTVariable.POST_ID
                                )
                            }
                        } else {
                            if (RTVariable.COMMENT_FROM) {
                                getSendDeleteReelsComment(
                                    parentCommentId.toString(),
                                    1,
                                    RTVariable.COMMENT_POSITION,
                                    RTVariable.POST_ID
                                )
                            } else {
                                getSendDeleteComment(
                                    parentCommentId.toString(),
                                    1,
                                    RTVariable.COMMENT_POSITION,
                                    RTVariable.POST_ID
                                )
                            }
                        }
                        d.dismiss()
                    }
                    .setNegativeButton("Cancel") { d, _ ->
                        adapter.cancelSection()
                        d.dismiss()
                    }
                    .create()
                dialog.show()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0D1015")))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor("#FFFFFF".toColorInt())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor("#B90A66".toColorInt())
                val messageText = dialog.findViewById<TextView>(android.R.id.message)
                messageText?.setTextColor("#FFFFFF".toColorInt())
            },
            onReplySelected = { commentId ->
                isReplySelected = true
                this.commentId = commentId.toString()
                binding.commentReplyEdittext.setHint("Reply to comment...")
                CommonUtils.openKeyboard(binding.commentReplyEdittext)
            },
            onProfileSelected = { user_id ->
                dismiss()
                if(RTVariable.bottom_page==0){
                    findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", user_id.toString()))
                }else if(RTVariable.bottom_page==2){
                    findNavController().navigate(ReelsFragmentDirections.actionReelsFragmentToProfileFragment("0", user_id.toString()))
                }else{
                    findNavController().navigate(UserPostListFragmentDirections.actionUserPostListFragmentToProfileFragment("0", user_id.toString()))
                }
            },
            onProfileImageSelected = { user_id, view ->
//                val storyPosition = stories?.indexOfFirst { it.user_id == user_id }
//                val bundle = Bundle().apply {
//                    putParcelableArrayList("storyList", ArrayList(stories ?: emptyList()))
//                    putParcelable("myStoryData", myStory)
//                    putInt("position", storyPosition!!)
//                }
//                try {
//                    dismiss()
//                    findNavController().navigate(R.id.secondStoryFragment, bundle)
//                } catch (e: Exception) {
//                    Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
//                    Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
//                }
                if (RTVariable.statusListGlobal.isEmpty()) {

                }else{
                    //val storyPosition = stories?.indexOfFirst { it.user_id == user_id }
                    val targetUserId = user_id.toString()
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)

                    val intent = Intent(requireContext(), PlayStatusActivity::class.java)

                    // normal data
                    //intent.putExtra("play_position", storyPosition)

                    val json = Gson().toJson(RTVariable.statusListGlobal)
                    intent.putExtra("story_list", json)
                    intent.putExtra("is_play_single", true)
                    intent.putExtra("user_id", targetUserId)

                    // instagram style animation data
                    intent.putExtra("start_x", location[0])
                    intent.putExtra("start_y", location[1])
                    intent.putExtra("start_width", view.width)
                    intent.putExtra("start_height", view.height)

                    startActivity(intent)
                    requireActivity().overridePendingTransition(0, 0)
                }
                // clicked view ki position lo
                //dismiss()
                //findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", user_id.toString()))
            },
            onMentionClick = { user_name ->
                Log.e("onMentionClick", "onMentionClick>>> "+user_name)
                getUserId(user_name)
            },
            binding.commentsRecycler,
            viewModel2
        )
        hasMore = initialComments.size >= limit
        Glide.with(requireContext()).load(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.profileImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(binding.userImage)
        binding.commentsRecycler.adapter = adapter
        binding.commentsRecycler.isNestedScrollingEnabled = true
        binding.commentsRecycler.setPadding(
            binding.commentsRecycler.paddingLeft,
            binding.commentsRecycler.paddingTop,
            binding.commentsRecycler.paddingRight,
            requireContext().dpToPx(70)
        )
        binding.commentsRecycler.clipToPadding = false
        //binding.commentsRecycler.setHasFixedSize(true)
        setupPagination()
        binding.sendButton.setOnClickListener {
            val message = binding.commentReplyEdittext.text.toString()
            if (message.isEmpty()) return@setOnClickListener
            binding.sendButton.isEnabled = false
            if (isReplySelected) {
                isReplySelected = false
                val fullComment = RTVariable.REPLY_COMBINED_IMAGE_USERNAME + message
                binding.commentReplyEdittext.setText("")
                val request = ReplyToCommentRequest(reply = fullComment, commentId)
                onReplyAction?.invoke(request)
            } else {
                binding.commentReplyEdittext.setText("")
                hitPostCommentApi(message)
            }
            binding.sendButton.postDelayed({
                binding.sendButton.isEnabled = true
            }, 1000)
        }
        Log.i("TAG", "onViewCreated: PBS " + UserDataManager.get_CommentPosition(binding.root.context))
        if (UserDataManager.isCommentToScroll(binding.root.context)) {
            val position = UserDataManager.get_CommentPosition(binding.root.context)
            UserDataManager.setCommentToScroll(binding.root.context, false)
            val pos = UserDataManager.get_CommentNewPosition(binding.root.context)
            val offSet = UserDataManager.get_CommentNewOffSetPosition(binding.root.context)
            binding.commentsRecycler.post {
                val lm = binding.commentsRecycler.layoutManager as LinearLayoutManager
                lm.scrollToPositionWithOffset(pos, offSet)
                UserDataManager.postCommentNewPosition(binding.root.context, -1)
                UserDataManager.postCommentNewOffSetPosition(binding.root.context, 0)
            }
        }
        binding.commentsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && isRecyclerViewAtTop(recyclerView) && isExpanding && behavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                    isExpanding = false
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }else{
                    isExpanding = true
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstVisibleItemPosition()
                val offset = layoutManager.findViewByPosition(position)?.top ?: 0
                UserDataManager.postCommentNewPosition(binding.root.context, position)
                UserDataManager.postCommentNewOffSetPosition(binding.root.context, offset)
//                else if (dy > 0 && behavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
//                    // ✅ ONLY collapse when scrolling down AND not already collapsed
//                    isExpanding = true
//                    behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
//                }
            }
        })
        //binding.commentReplyEdittext.requestFocus()
    }

    private fun isRecyclerViewAtTop(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
        return firstVisiblePosition == 0 && (firstVisibleView?.top ?: 0) >= 0
    }

    private fun setupPagination() {
        binding.commentsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                // Expand logic
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (!isLoading && hasMore &&
                    (firstVisibleItemPosition + visibleItemCount) >= totalItemCount) {

                    loadMore()
                }else{
                    loadMore()
                }
            }
        })
    }

    fun saveScroll() {
        val lm = binding.commentsRecycler.layoutManager as LinearLayoutManager
        var scrollPosition = lm.findFirstVisibleItemPosition()
        var scrollOffset = lm.findViewByPosition(scrollPosition)?.top ?: 0
        UserDataManager.postCommentYPosition(requireContext(), scrollOffset)
    }

    private fun loadMore() {
        if (isLoading) return  // ✅ prevent duplicate calls
        isLoading = true
        currentPage++
        onLoadMore?.invoke(currentPage, limit)
    }

    private fun getSelectedReply(replyText: String, commentId: Int) {
        if (replyText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
        } else {
            var request = ReplyToCommentRequest(reply = replyText, commentId.toString())
            onReplyAction?.invoke(request)
        }
    }

    private fun getSelectedDelete(replyText: String, commentId: Int) {
        if (replyText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
            //var request = ReplyToCommentRequest(reply = replyText, commentId.toString())
            //onReplyAction?.invoke(request)
        }
    }

    private fun hitPostCommentApi(message: String) {
        var request = PostCommentsRequest(comment = message)
        onCommentAction?.invoke(request)
    }

    fun updateComments(payload: Payload) {
        adapter.updateList(payload.comments)
        currentPage = 1
        hasMore = payload.comments.size >= limit
        binding.commentReplyEdittext.setText("")
        binding.root.post {
            binding.commentReplyEdittext.clearFocus()
            CommonUtils.hideEdittextKeyboard(binding.commentReplyEdittext)
        }
        if (payload.comments.isNotEmpty()) {
            binding.commentsRecycler.scrollToPosition(0)
        }
    }

    fun appendComments(newComments: List<Comment>) {
        if (newComments.size < limit) {
            hasMore = false
        }

        adapter.addItems(newComments)

        // ✅ IMPORTANT FIX
        isLoading = false

        // ✅ Also trigger next load if still not filled screen
        binding.commentsRecycler.post {
            checkPaginationAfterExpand()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        heightChangeRunnable?.let {
            binding.commentsRecycler.removeCallbacks(it)
        }

        _binding = null
    }

    fun Context.dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun Float.toPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    private fun getUserId(user_name: String){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.getUserIdByUserName(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
                    user_name = user_name
                )
                if (response.status == 1 && response.code == 200) {
                    var user_id = response.payload.user_id
                    dismiss()
                    findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", user_id.toString()))
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun getSendDeleteComment(comment_id: String, mode: Int, position: Int, post_id: String){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.deletePostComment(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
                    commentId = comment_id,
                    mode = mode.toString(),
                    post_id = post_id
                )
                if (response.status == 1 && response.code == 200) {
                    Toast.makeText(
                        requireContext(),
                        "Comment deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    val comment_count = response.payload.comment_count
                    RTVariable.COMMENT_COUNT = comment_count
                    Log.e("COMMENT_COUNT", "COMMENT_COUNT ${comment_count}")
                    viewModel2.hitGetReelCommentsApi("Bearer " + Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, RTVariable.POST_ID, "1", "10")
                    adapter.removeItems(mode, position)
//                    if (mode == 2) {
//                        adapter.adapter.removeItems(position)
//                        adapter.adapter.notifyDataSetChanged()
//                    }
                    RTVariable.COMMENT_DELETED = true
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }

    private fun getSendDeleteReelsComment(comment_id: String, mode: Int, position: Int, post_id: String){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitBuilder.apiService.deleteReelsComment(
                    token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA
                    )?.payload?.authToken,
                    commentId = comment_id,
                    mode = mode.toString(),
                    post_id = post_id
                )
                if (response.status == 1 && response.code == 200) {
                    Toast.makeText(
                        requireContext(),
                        "Comment deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    val comment_count = response.payload.comment_count
                    RTVariable.COMMENT_COUNT = comment_count
                    Log.e("COMMENT_COUNT", "COMMENT_COUNT ${comment_count} POS: ${position}")

                    adapter.removeItems(mode, position)
                    RTVariable.COMMENT_DELETED = true
                    //viewModel3.hitGetReelsApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, RTVariable.REELS_CURRENT_PAGE.toString(), "6")
//                    if (mode == 2) {
//                        adapter.adapter.removeItems(position)
//                        adapter.adapter.notifyDataSetChanged()
//                    }

                } else {
                    Toast.makeText(requireContext(), response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception) {
            }
        }
    }
}