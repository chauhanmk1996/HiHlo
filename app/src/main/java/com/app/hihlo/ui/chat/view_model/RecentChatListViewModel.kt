package com.app.hihlo.ui.chat.view_model

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.model.chat.RecentChats
import com.app.hihlo.model.get_recent_chat.response.GetRecentChatResponse
import com.app.hihlo.model.get_recent_chat.response.RecentChat
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.HiHloApplication
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class RecentChatListViewModel: ViewModel() {

    var chat_recentList: List<RecentChat> = emptyList()
    var chat_requestUsersList: List<RecentChat> = emptyList()
    var isLoaded: Boolean = false
    val firestore = FirebaseFirestore.getInstance()

    fun getRecentChatLists(userId: String): LiveData<List<RecentChats>> {
        val mainChatList = MutableLiveData<List<RecentChats>>()

        val uid = userId
        if (uid.isEmpty()) {
            mainChatList.value = emptyList()
            return mainChatList
        }

        firestore.collection("recentChats")
            .document(uid)
            .collection("chats")
            .orderBy("time", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->

                if (exception != null || snapshot == null || snapshot.isEmpty) {
                    mainChatList.value = emptyList()
                    return@addSnapshotListener
                }

                val chatlist = snapshot.documents
                    .mapNotNull { it.toObject(RecentChats::class.java) }
                    .filter { it.sender == uid && it.archived != "1" }
                    .sortedWith(
                        compareByDescending<RecentChats> { it.pinned == "1" }
                            .thenByDescending { it.time }
                    )

                mainChatList.value = chatlist
            }

        return mainChatList
    }



    private val recentChatLiveData = SingleLiveEvent<Resources<GetRecentChatResponse>>()

    fun getRecentChatLiveData(): LiveData<Resources<GetRecentChatResponse>> {
        return recentChatLiveData
    }
    fun hitGetRecentChatDataApi(token: String,
                                fromUserId: String? = null,
                                toUserId: String? = null,
                                type: String? = null,
    ) {
        Log.e("TTTTT", "TTTTT>>> "+fromUserId+" | "+toUserId+" | "+type)
        try {
            recentChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    recentChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().getRecentChatApi(token, fromUserId, toUserId, type
                            )
                        )
                    )
                } catch (ex: Exception) {
                    recentChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val requestChatLiveData = SingleLiveEvent<Resources<GetRecentChatResponse>>()

    fun getRequestChatLiveData(): LiveData<Resources<GetRecentChatResponse>> {
        return requestChatLiveData
    }
    fun hitGetRequestChatDataApi(token: String,
                                fromUserId: String? = null,
                                toUserId: String? = null,
                                type: String? = null,
    ) {

        try {
            requestChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    requestChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().getRecentChatApi(token, fromUserId, toUserId, type
                            )
                        )
                    )
                } catch (ex: Exception) {
                    requestChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val deleteRecentChatLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeleteRecentChatLiveData(): LiveData<Resources<CommonResponse>> {
        return deleteRecentChatLiveData
    }
    fun hitDeleteRecentChatDataApi(token: String,
                                request: SaveRecentChatRequest) {

        try {
            deleteRecentChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deleteRecentChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().deleteRecentChatApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    deleteRecentChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val handleMessageRequestLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getHandleMessageRequestLiveData(): LiveData<Resources<CommonResponse>> {
        return handleMessageRequestLiveData
    }
    fun hitHandleMessageRequestDataApi(token: String, chatId: String, action: String) {

        try {
            handleMessageRequestLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    handleMessageRequestLiveData.postValue(
                        Resources.success(
                            ApiRepository().handleMessageRequestApi(token, chatId, action
                            )
                        )
                    )
                } catch (ex: Exception) {
                    handleMessageRequestLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }





    fun deleteChat(sender: String, receiver: String, token: String) {
        val chatId = sender + "_" + receiver
        val chatRef = firestore.collection("Messages").document(chatId).collection("chats")

        chatRef.get().addOnSuccessListener { documents ->
            val batch = firestore.batch()

            for (document in documents) {
                batch.delete(document.reference) // Add all deletes to the batch
            }

            // Commit the batch to delete all messages in one call
            batch.commit().addOnSuccessListener {
                // Now delete the chat document after messages are removed
                firestore.collection("Messages").document(chatId).delete()
                    .addOnSuccessListener {
//                        isChatDeleted.value = true
                        Toast.makeText(
                            HiHloApplication.appContext?.applicationContext,
                            "Chat deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
//                        hitSaveRecentChatDataApi("Bearer $token", SaveRecentChatRequest(toUserId = receiver, message = "No messages"))
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            HiHloApplication.appContext?.applicationContext,
                            "Failed to delete chat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    HiHloApplication.appContext?.applicationContext,
                    "Failed to delete messages",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                HiHloApplication.appContext?.applicationContext,
                "Failed to fetch messages",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}