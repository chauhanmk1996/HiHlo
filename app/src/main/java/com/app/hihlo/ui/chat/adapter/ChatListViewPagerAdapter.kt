package com.app.hihlo.ui.chat.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.databinding.ChatListViewPagerAdapterBinding
import com.app.hihlo.model.get_recent_chat.response.RecentChat
import com.app.hihlo.ui.chat.fragment.ChatListFragment
import com.app.hihlo.utils.UserDataManager

class ChatListViewPagerAdapter(
    private var items: List<RecentChat>,
    private var requestList: List<RecentChat>,
    private val onLongTap: (click: Int, position: Int, data: RecentChat, view: View) -> Unit,
    private val onItemSelected: (click: Int, position: Int, data: RecentChat, view: View) -> Unit,
) : RecyclerView.Adapter<ChatListViewPagerAdapter.PageViewHolder>() {
    private var inboxAdapter: AdapterChatList? = null
    private var requestAdapter: AdapterChatList? = null

    inner class PageViewHolder(val binding: ChatListViewPagerAdapterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(ChatListViewPagerAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val list = if (position == 0) items else requestList
        val isInbox = position == 0
        if (list.isNotEmpty()) {
            initRecentChatRecycler(list, holder.binding, isInbox)
            holder.binding.noChatsFoundPlaceholder.isVisible = false
        } else {
            holder.binding.noChatsFoundPlaceholder.isVisible = true
            if (!isInbox) holder.binding.noChatsFoundPlaceholder.text = "No Request" else holder.binding.noChatsFoundPlaceholder.text = "No Chat"
            initRecentChatRecycler(emptyList(), holder.binding, isInbox)
        }
    }

    override fun getItemCount() = 2

    private fun initRecentChatRecycler(
        list: List<RecentChat>,
        binding: ChatListViewPagerAdapterBinding,
        isInbox: Boolean
    ) {
        val selectedLambda: (Int, Int, View) -> Unit = Unit@{ itemPos, click, view ->
            val data = list.getOrNull(itemPos) ?: return@Unit
            onItemSelected(click, itemPos, data, view)
        }
        val longTapLambda: (Int, Int, View) -> Unit = Unit@{ itemPos, click, view ->
            val data = list.getOrNull(itemPos) ?: return@Unit
            onLongTap(click, itemPos, data, view)
        }
        val adapter = AdapterChatList(list, selectedLambda, longTapLambda, binding.root, isInbox)
        if (isInbox) inboxAdapter = adapter else requestAdapter = adapter
        binding.chatListRecycler.adapter = adapter
        binding.chatListRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstCompletelyVisible =
                        layoutManager.findFirstCompletelyVisibleItemPosition()
                    val fallback = layoutManager.findFirstVisibleItemPosition()
                    var currentVisiblePosition =
                        if (firstCompletelyVisible != RecyclerView.NO_POSITION)
                            firstCompletelyVisible
                        else
                            fallback
                    val type = if (isInbox) ChatListFragment.TYPE_INBOX else ChatListFragment.TYPE_REQUEST
                    UserDataManager.saveChatScrollPosition(binding.root.context, type, currentVisiblePosition)
                    Log.d("SCROLL", "Saved Position = $currentVisiblePosition")
                }
            }
        })
        binding.chatListRecycler.post {
            val type = if (isInbox) ChatListFragment.TYPE_INBOX else ChatListFragment.TYPE_REQUEST
            val savedPosition = UserDataManager.getChatScrollPosition(binding.root.context, type)
            val layoutManager = binding.chatListRecycler.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(savedPosition, 0)
            Log.d("SCROLL", "Restored Position = $savedPosition")
        }
    }

    fun updateList(newList: List<RecentChat>, position: Int) {
        if (position == 0) {
            this.items = newList
            inboxAdapter?.updateList(newList)
            notifyItemChanged(0)
        } else {
            this.requestList = newList
            requestAdapter?.updateList(newList)
            notifyItemChanged(1)
        }
    }
}
