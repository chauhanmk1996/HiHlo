package com.app.hihlo.ui.chat.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.databinding.ChatListViewPagerAdapterBinding
import com.app.hihlo.model.get_recent_chat.response.RecentChat
import com.app.hihlo.model.home.response.Story
import com.app.hihlo.utils.ScrollKeys
import com.app.hihlo.utils.UserDataManager

class ChatListViewPagerAdapter(
    private var items: List<RecentChat>,
    private var requestList: List<RecentChat>,
    private val onLongTap: (click: Int, position: Int, data: RecentChat, view: View) -> Unit,
    private val onItemSelected: (click: Int, position: Int, data: RecentChat, view: View) -> Unit,
) : RecyclerView.Adapter<ChatListViewPagerAdapter.PageViewHolder>() {

    private val scrollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null

    private var inboxAdapter: AdapterChatList? = null
    private var requestAdapter: AdapterChatList? = null

    inner class PageViewHolder(val binding: ChatListViewPagerAdapterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var listenerAttached = false
        var isRestoring = false
        var restorationListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(
            ChatListViewPagerAdapterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = 2

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {

        val isInbox = position == 0
        val list = if (isInbox) items else requestList
        val binding = holder.binding

        Log.d("DATA_CHECK", "TAB=${if (isInbox) "INBOX" else "REQUEST"} SIZE=${list.size}")

        // 🔥 FIX: Delay binding until layout ready
        binding.root.post {

            // ✅ Empty state
            binding.chatListRecycler.isVisible = list.isNotEmpty()
            binding.noChatsFoundPlaceholder.isVisible = list.isEmpty()
            binding.noChatsFoundPlaceholder.text =
                if (isInbox) "No Chat" else "No Request"

            setupRecycler(holder, list, isInbox)
        }
    }

    private fun setupRecycler(
        holder: PageViewHolder,
        list: List<RecentChat>,
        isInbox: Boolean
    ) {
        val binding = holder.binding

        // LayoutManager once
        if (binding.chatListRecycler.layoutManager == null) {
            binding.chatListRecycler.layoutManager = LinearLayoutManager(binding.root.context)
        }

        // ALWAYS new adapter
        val adapter = AdapterChatList(
            list,
            { pos, click, view ->
                val data = list.getOrNull(pos) ?: return@AdapterChatList
                onItemSelected(click, pos, data, view)
            },
            { pos, click, view ->
                val data = list.getOrNull(pos) ?: return@AdapterChatList
                onLongTap(click, pos, data, view)
            },
            binding.root,
            isInbox
        )
        binding.chatListRecycler.adapter = adapter
        if (isInbox) {
            inboxAdapter = adapter
        } else {
            requestAdapter = adapter
        }

        // ==========================
        // ✅ RESTORE SCROLL – ABSOLUTE + ULTRA STABLE
        // ==========================
        val key = if (isInbox) ScrollKeys.INBOX else ScrollKeys.REQUEST
        val savedScrollY = UserDataManager.getChatScrollPosition(binding.root.context, key)

        if (holder.restorationListener == null) {
            holder.isRestoring = true

            val observer = binding.chatListRecycler.viewTreeObserver

            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (observer.isAlive) {
                        observer.removeOnGlobalLayoutListener(this)
                    }
                    holder.restorationListener = null
                    binding.nestedScrollView.scrollTo(0, savedScrollY)
                    holder.isRestoring = false
                    // Extra delay so all item heights (images, text wrapping) are final
//                    binding.nestedScrollView.postDelayed({
//                        val child = binding.nestedScrollView.getChildAt(0) ?: return@postDelayed
//                        val maxScroll = child.height - binding.nestedScrollView.height
//
//                        val targetScroll = when {
//                            savedScrollY <= 0 -> 0
//                            maxScroll <= 0 -> 0
//                            else -> savedScrollY.coerceAtMost(maxScroll)
//                        }
//
//                        binding.nestedScrollView.scrollTo(0, targetScroll)
//
//                        Log.d(
//                            "SCROLL",
//                            "✅ RESTORED = $targetScroll (saved=$savedScrollY, max=$maxScroll)"
//                        )
//
//                        holder.isRestoring = false
//                    }, 50)   // 50ms = very reliable for chat lists
                }
            }

            holder.restorationListener = listener
            observer.addOnGlobalLayoutListener(listener)
        }

        // ==========================
        // ✅ SAVE SCROLL – NOW SAVES ABSOLUTE scrollY
        // ==========================
        if (!holder.listenerAttached) {
            holder.listenerAttached = true

            binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                if (holder.isRestoring) return@setOnScrollChangeListener

                val child = binding.nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
                val maxScroll = child.height - binding.nestedScrollView.height
                if (maxScroll <= 0) return@setOnScrollChangeListener

                val saveKey = if (isInbox) ScrollKeys.INBOX else ScrollKeys.REQUEST

                UserDataManager.saveChatScrollPosition(
                    binding.root.context,
                    saveKey,
                    scrollY   // ← ABSOLUTE scrollY (no percent)
                )

                Log.i("SCROLL", "💾 Saved scrollY = $scrollY")
            }
        }
    }

    // ==========================
    // ✅ UPDATE DATA
    // ==========================
    fun updateList(newList: List<RecentChat>, position: Int) {
        if (position == 0) {
            items = newList
            notifyItemChanged(0)
        } else {
            requestList = newList
            notifyItemChanged(1)
        }
    }

    fun updateStory(stories_List: List<Story>){
        inboxAdapter?.updateStories(stories_List)
        requestAdapter?.updateStories(stories_List)
    }

    fun scrollToTop(recyclerView: RecyclerView, tabPosition: Int, isTrue: Boolean) {
        if (!isTrue) return

        recyclerView.post {
            val holder = recyclerView.findViewHolderForAdapterPosition(tabPosition) as? PageViewHolder
                ?: run {
                    // retry if holder not ready yet
                    recyclerView.post { scrollToTop(recyclerView, tabPosition, isTrue) }
                    return@post
                }

            val binding = holder.binding
            val isInbox = tabPosition == 0
            val key = if (isInbox) ScrollKeys.INBOX else ScrollKeys.REQUEST

            holder.isRestoring = true

            // Force top + ensure layout is ready
            binding.nestedScrollView.post {
                UserDataManager.saveChatFromOtherScrollPosition(
                    binding.root.context,
                    key,
                    0
                )
                binding.nestedScrollView.scrollTo(0, 0)

                // ✅ NOW SAVE WITH THE SAME METHOD & KEY as normal scrolling

                Log.d("SCROLL", "⬆️ ScrollToTop + SAVED 0 for ${if (isInbox) "INBOX" else "REQUEST"}")

                holder.isRestoring = false
            }
        }
    }
}