package com.app.hihlo.ui.reels.bottom_sheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.BottomSheetLayoutBinding
import com.app.hihlo.model.get_reel_comments.response.Comment
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.app.hihlo.model.home.response.MyStory
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.post_comments.request.PostCommentsRequest
import com.app.hihlo.model.reply_to_comment.request.ReplyToCommentRequest
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.HomeNew.HomeNewFragmentDirections
import com.app.hihlo.ui.home.view_model.UserPostListViewModel
import com.app.hihlo.ui.reels.adapter.AdapterComments
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.CommonUtils.toPx
import com.app.hihlo.utils.getString
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.getValue

class RoundedBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetLayoutBinding? = null
    private val binding get() = _binding!!
    var onCommentAction: ((PostCommentsRequest) -> Unit)? = null
    var onReplyAction: ((ReplyToCommentRequest) -> Unit)? = null
    lateinit var adapter:AdapterComments
    var isReplySelected = false
    var commentId = ""
    private val viewModel2: UserPostListViewModel by viewModels()

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                // 1. Start half height (peek)
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val peekHeight = (screenHeight * 0.5).toInt()
                behavior.peekHeight = /*peekHeight*/requireContext().dpToPx(400)

                // 2. Allow it to expand
                behavior.isFitToContents = true
                behavior.skipCollapsed = false
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED

                // 3. Optional: Set rounded corners
                val cornerRadius = 25f.toPx(requireContext())
                val shapeDrawable = MaterialShapeDrawable(
                    ShapeAppearanceModel.Builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                        .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                        .build()
                )
                shapeDrawable.fillColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.screen_background_black))
                it.background = shapeDrawable

                // 4. Scroll to bottom after fully expanded
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                val params = binding.commentsRecycler.layoutParams
                                params.height = requireContext().dpToPx(250) // fixed height when collapsed
                                binding.commentsRecycler.layoutParams = params
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // No-op
                    }
                })
                binding.titleTextView.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val params = binding.commentsRecycler.layoutParams
                        params.height = ViewGroup.LayoutParams.MATCH_PARENT
                        binding.commentsRecycler.layoutParams = params

                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                    true
                }

            }

        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val comments = arguments?.getParcelable<Payload>("comments")
        val myStory = arguments?.getParcelable<MyStory>("myStory")
        val stories = arguments?.getParcelableArrayList<Story>("stories")
        Log.i("TAG", "onViewCreated: "+comments)
        val initialComments = comments?.comments ?: emptyList<Comment>()
        adapter = AdapterComments(
            initialComments.toMutableList(),
            stories,
            onReplyClick = { replyText, parentCommentId ->
                val request = ReplyToCommentRequest(
                    reply = replyText,
                    commentId = parentCommentId.toString()
                )
                onReplyAction?.invoke(request)
                // optionally clear inline reply field if you have one
            },

            onDeleteClick = { isReply, parentCommentId, itemId ->
                // Here you know exactly what to delete
                if (isReply) {
                    // Delete reply
                    // itemId = replyId
                    // parentCommentId = parent comment
                    Toast.makeText(requireContext(), "RDelete reply $itemId", Toast.LENGTH_SHORT).show()
                    // → call your delete reply API here
                } else {
                    // Delete main comment
                    // itemId = commentId
                    Toast.makeText(requireContext(), "RDelete comment $parentCommentId", Toast.LENGTH_SHORT).show()
                    // → call your delete comment API here
                }
            },

            onReplySelected = { commentId ->
                isReplySelected = true
                this.commentId = commentId.toString()
                binding.commentReplyEdittext.setHint("Reply to comment...")
                CommonUtils.openKeyboard(binding.commentReplyEdittext)
            },
            onProfileSelected = { user_id ->
                findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", user_id.toString()))
            },
            onProfileImageSelected = { user_id, view ->
                val storyPosition = stories?.indexOfFirst { it.user_id == user_id }
                val bundle = Bundle().apply {
                    putParcelableArrayList("storyList", ArrayList(stories ?: emptyList()))
                    putParcelable("myStoryData", myStory)
                    putInt("position", storyPosition!!)
                }
                try {
                    dismiss()
                    findNavController().navigate(R.id.secondStoryFragment, bundle)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to open story", Toast.LENGTH_SHORT).show()
                }
                //dismiss()
                //findNavController().navigate(HomeNewFragmentDirections.actionHomeNewFragmentToProfileFragment("0", user_id.toString()))
            },
            onMentionClick = { user_name ->
                //Log.e("onMentionClick", "onMentionClick>>> "+user_name)
            },
            binding.commentsRecycler,
            viewModel2
        )
        Glide.with(requireContext()).load(Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.profileImage).placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(binding.userImage)
        binding.commentsRecycler.adapter = adapter
        binding.sendButton.setOnClickListener {
            if (binding.commentReplyEdittext.getString().isEmpty()) {
                Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
            }else{
                if (isReplySelected) {
                    isReplySelected = false
                    var request = ReplyToCommentRequest(reply = binding.commentReplyEdittext.text.toString(), commentId)
                    onReplyAction?.invoke(request)  // Pass any data here
                }else {
                        hitPostCommentApi()
                }
            }
        }
    }
    private fun getSelectedReply(replyText: String, commentId: Int){
        if (replyText.isEmpty()){
            Toast.makeText(requireContext(), "Please enter something!", Toast.LENGTH_SHORT).show()
        }else{
            var request = ReplyToCommentRequest(reply = replyText, commentId.toString())
            onReplyAction?.invoke(request)
        }
    }
    private fun hitPostCommentApi() {
        var request = PostCommentsRequest(comment = binding.commentReplyEdittext.text.toString())
        onCommentAction?.invoke(request)  // Pass any data here
    }

    fun updateComments(payload: Payload) {
        adapter.updateList(payload.comments)
        binding.commentReplyEdittext.setText("")
        binding.root.post {
            binding.commentReplyEdittext.clearFocus()
            CommonUtils.hideEdittextKeyboard(binding.commentReplyEdittext)
        }
        if (payload.comments.isNotEmpty()){
            binding.commentsRecycler.scrollToPosition(0)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun Context.dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

}
