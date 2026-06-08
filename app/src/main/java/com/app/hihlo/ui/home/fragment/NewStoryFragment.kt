package com.app.hihlo.ui.home.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.app.hihlo.databinding.FragmentNewStoryBinding
import com.app.hihlo.databinding.PopupChatSideOptionsBinding
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.home.adapter.NewStoryPagerAdapter
import com.app.hihlo.ui.home.adapter.NewStoryPagerAdapter.StoryViewHolder
import com.app.hihlo.ui.home.view_model.NewStoryViewModel
import com.app.hihlo.ui.reels.bottom_sheet.BlockFlagBottomSheet
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.abs

class NewStoryFragment : Fragment(), NewStoryPagerAdapter.OnStoryActionListener {
    private lateinit var binding: FragmentNewStoryBinding
    private lateinit var adapter: NewStoryPagerAdapter
    private var storyList: ArrayList<Story> = arrayListOf()
    private var currentPage = 0
    private val delayMillis = 6000L // 3 seconds
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var exoPlayer: ExoPlayer
    private val viewModel: NewStoryViewModel by viewModels()
    private var isKeyboardVisible = false
    private var lastInsets: WindowInsetsCompat? = null

    private val slideRunnable = object : Runnable {
        override fun run() {
            if (!isAdded || view == null) return

            if (currentPage < storyList.size - 1) {
                lifecycleScope.launch {
                    CommonUtils.hideKeyboard(requireActivity())
                    delay(80)
                    binding.storyViewPager.setCurrentItem(binding.storyViewPager.currentItem + 1, false)

                }
                handler.postDelayed(this, delayMillis)

            } else {
                if (isAdded && view != null) {
//                    findNavController().popBackStack()
                    (requireActivity() as HomeActivity).navigateToHome()

                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            lastInsets = insets
            isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            if (isKeyboardVisible) {
                handler.removeCallbacks(slideRunnable)
            } else {
                if (storyList[currentPage].asset_type == "I") {
                    handler.postDelayed(slideRunnable, delayMillis)
                }
            }
            adapter.handleKeyboardInsets(insets)

            insets
        }
    }
    override fun onSwipeDown() {
        findNavController().popBackStack()
    }

    override fun onSideOptionsClicked() {
        handler.removeCallbacks(slideRunnable)

        getCurrentViewHolder()?.setPlayPauseStory(true)
        openSideOptionsPopup()
    }

    private fun setObserver() {
        viewModel.seenStoryLiveData().observe(viewLifecycleOwner) {

            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Story Seen success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){

                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
//                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            storyList = it.getParcelableArrayList("storyList") ?: arrayListOf()
            currentPage = it.getInt("position")
            Log.e("TAG", "onCreate:dd $storyList \n $currentPage", )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewStoryBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        binding.sideOptions.bringToFront()
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        getCurrentViewHolder()?.binding?.videoLoader?.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        getCurrentViewHolder()?.binding?.videoLoader?.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        onVideoEnd()
                    }
                }
            }
        })


        adapter = NewStoryPagerAdapter(requireContext(), storyList, exoPlayer, this)
        binding.storyViewPager.adapter = adapter
        binding.storyViewPager.offscreenPageLimit = 1
        binding.storyViewPager.setCurrentItem(currentPage, false)

        binding.storyViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.stopSeekBar()
                currentPage = position
                if (storyList[position].asset_type == "V"){

                }else{
                    adapter.notifyItemChanged(position) // 🔁 Force rebind
                }

                binding.storyViewPager.post {
                    bindVisibleStory(currentPage)
                    lastInsets?.let { insets ->
                        getCurrentViewHolder()?.handleKeyboardInsets(insets)
                    }
                }

                handler.removeCallbacks(slideRunnable)
                if (storyList[position].asset_type == "I") {
                    handler.postDelayed(slideRunnable, delayMillis)
                }
                if (storyList[position].is_seen==0){
                    if (isAdded && isVisible){
                        viewModel.hitSeenStoryDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, StorySeen(storyId = storyList[position].id.toString()))
                    }
                }
            }


        })

        //disable to swipe gesture
    /*    val child = binding.storyViewPager.getChildAt(0)
        if (child is RecyclerView) {
            child.setOnTouchListener { _, _ -> true } // consume all touch events
        }
        handler.postDelayed(slideRunnable, delayMillis)
*/
        // Disable swipe without blocking clicks inside items
        (binding.storyViewPager.getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        binding.storyViewPager.isUserInputEnabled = false
        binding.storyViewPager.setCurrentItem(currentPage, false)
        binding.storyViewPager.post {
            bindVisibleStory(currentPage)
        }
    }
    private fun bindVisibleStory(position: Int) {
        val recyclerView = binding.storyViewPager.getChildAt(0) as? RecyclerView
        val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? NewStoryPagerAdapter.StoryViewHolder
        if (storyList[position].asset_type != "V") holder?.bind(storyList[position], position)

        adapter.setCurrentVisibleHolder(holder)
    }


    private fun getCurrentViewHolder(): StoryViewHolder? {
        val recyclerView = binding.storyViewPager.getChildAt(0) as? RecyclerView
        val holder = recyclerView?.findViewHolderForAdapterPosition(currentPage)
        return holder as? StoryViewHolder
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(slideRunnable)

        try {
            if (::exoPlayer.isInitialized) {
                exoPlayer.stop()
                exoPlayer.release()
            }
        } catch (e: Exception) {
            Log.e("ExoPlayer", "Error releasing player: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        if (::exoPlayer.isInitialized) {
            exoPlayer.pause()
        }
    }

    override fun onVideoEnd() {
        if (currentPage < storyList.size - 1) {
            lifecycleScope.launch {
                if (isAdded){
                    CommonUtils.hideKeyboard(requireActivity())
                }
                delay(80)

                binding.storyViewPager.setCurrentItem(binding.storyViewPager.currentItem + 1, false)

            }
        } else {
            if (isAdded && view != null) {
                try {

//                    findNavController().popBackStack()
                    (requireActivity() as HomeActivity).navigateToHome()
                } catch (e: IllegalStateException) {
                    Log.e("NewStoryFragment", "Nav error: ${e.message}")
                }
            }
        }
    }

    override fun sendMessage(otherStoryData: Story, message: String) {
        viewModel.hitSaveRecentChatDataApi("Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken,
            SaveRecentChatRequest(toUserId = otherStoryData.user_id.toString(), message = message))

        sendMessage((Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.userId ?: "").toString(), otherStoryData.user_id.toString(), otherStoryData.userDetail?.name ?: "" , otherStoryData.userDetail?.profile_image ?: "", MediaType.TEXT.name, message, "0", "0")

    }

    override fun leftClickAreaClicked() {
        val prev = binding.storyViewPager.currentItem - 1
        if (prev >= 0) {
            lifecycleScope.launch {
                CommonUtils.hideKeyboard(requireActivity())
                if (storyList[prev].asset_type == "I") delay(80)
                binding.storyViewPager.setCurrentItem(prev, false)
            }
        } else {
            binding.storyViewPager.setCurrentItem(0, false)
        }
    }

    override fun rightClickAreaClicked() {
        handler.removeCallbacks(slideRunnable)
        val next = binding.storyViewPager.currentItem + 1
        if (next < storyList.size) {
            lifecycleScope.launch {
                CommonUtils.hideKeyboard(requireActivity())
                if (storyList[next].asset_type == "I") delay(80)
                binding.storyViewPager.setCurrentItem(next, false)
            }
        } else {
            findNavController().popBackStack()
        }
    }

    override fun getClick(click: Int) {
        when(click){
            0->{
                findNavController().navigate(NewStoryFragmentDirections.actionNewStoryFragmentToProfileFragment("0", storyList.get(binding.storyViewPager.currentItem).user_id.toString(), from = "newStory"))
            }
        }
    }

    private fun openSideOptionsPopup() {
        val inflater = LayoutInflater.from(requireContext())
        val binding = PopupChatSideOptionsBinding.inflate(inflater)

        binding.title1.text = "Block"
        binding.title2.text = "Report"
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // for outside touch to dismiss
            elevation = 20f
            showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        }
        binding.title1.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "block")  // Add your arguments here
                putString("userId", storyList[this@NewStoryFragment.binding.storyViewPager.currentItem].user_id.toString())  // Add your arguments here
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
                findNavController().popBackStack()
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "BlockBottomSheet")
        }
        binding.title2.setOnClickListener {
            popupWindow.dismiss()
            val bottomSheetFragment = BlockFlagBottomSheet()
            val bundle = Bundle().apply {
                putString("screen", "flag")  // Add your arguments here
                putString("userId", storyList[this@NewStoryFragment.binding.storyViewPager.currentItem].user_id.toString())  // Add your arguments here
            }
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.onBlockSuccessful = {
                bottomSheetFragment.dismiss()
                findNavController().popBackStack()
            }
            bottomSheetFragment.show(requireActivity().supportFragmentManager, "FlagBottomSheet")

        }
        popupWindow.setOnDismissListener {

            getCurrentViewHolder()?.setPlayPauseStory(false)
        }
    }
    fun sendMessage(
        sender: String,
        receiver: String,
        friendName: String,
        friendImage: String,
        messageType: String,
        message: String,
        pinned: String,
        archived: String,
        url: String?=null,
    ) {
        viewModel.sendMessage(sender, receiver, friendName , friendImage, messageType, message, pinned, archived, url ?: "")
    }

}