package com.app.hihlo.ui.chat.view_model

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.coin_details.CoinDetailsResponse
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.block_user.request.BlockUserRequest
import com.app.hihlo.model.chat.MessageStatus
import com.app.hihlo.model.chat.Messages
import com.app.hihlo.model.chat_history.ChatHistoryResponse
import com.app.hihlo.model.deduct_chat_coin.DeductChatCoinRequest
import com.app.hihlo.model.generate_agora_token.response.AgoraTokenResponse
import com.app.hihlo.model.post_comments.response.PostCommentsResponse
import com.app.hihlo.model.predefined_chats.PredefinedChatsResponse
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.save_recent_chat.response.SaveRecentChatResponse
import com.app.hihlo.model.send_gift.SendGiftRequest
import com.app.hihlo.model.send_gift.SendGiftResponse
import com.app.hihlo.model.unblock_user.request.UnblockUserRequest
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.ChatUtils
import com.app.hihlo.HiHloApplication
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    val isChatDeleted = MutableLiveData(false)
    var paid = MutableLiveData("0")
    var creator = MutableLiveData("0")


    private val checkChatHistoryLiveData = SingleLiveEvent<Resources<ChatHistoryResponse>>()

    fun getCheckChatHistoryLiveData(): LiveData<Resources<ChatHistoryResponse>> {
        return checkChatHistoryLiveData
    }
    fun hitCheckChatHistoryApi(token: String, toUserId: String) {

        try {
            checkChatHistoryLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    checkChatHistoryLiveData.postValue(
                        Resources.success(
                            ApiRepository().checkChatHistoryApi(token, toUserId)
                        )
                    )
                } catch (ex: Exception) {
                    checkChatHistoryLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val readMessagesLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getReadMessagesLiveData(): LiveData<Resources<CommonResponse>> {
        return readMessagesLiveData
    }
    fun hitReadMessagesApi(token: String, toUserId: String?=null, anotherUserId: String?=null) {

        try {
            readMessagesLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    readMessagesLiveData.postValue(
                        Resources.success(
                            ApiRepository().readMessagesApi(token, toUserId, anotherUserId)
                        )
                    )
                } catch (ex: Exception) {
                    readMessagesLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val getCoinDetailsLiveData = SingleLiveEvent<Resources<CoinDetailsResponse>>()

    fun getCoinDetailsLiveData(): LiveData<Resources<CoinDetailsResponse>> {
        return getCoinDetailsLiveData
    }
    fun hitCoinDetailsApi(token: String) {

        try {
            getCoinDetailsLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getCoinDetailsLiveData.postValue(
                        Resources.success(
                            ApiRepository().coinDetailsApi(token)
                        )
                    )
                } catch (ex: Exception) {
                    getCoinDetailsLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val chatCoinDeductLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getChatCoinDeductLiveData(): LiveData<Resources<CommonResponse>> {
        return chatCoinDeductLiveData
    }
    fun hitChatCoinDeductApi(token: String, request: DeductChatCoinRequest) {

        try {
            chatCoinDeductLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    chatCoinDeductLiveData.postValue(
                        Resources.success(
                            ApiRepository().deductCoinsOnChatApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    chatCoinDeductLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val getSendGiftLiveData = SingleLiveEvent<Resources<SendGiftResponse>>()

    fun getSendGiftLiveData(): LiveData<Resources<SendGiftResponse>> {
        return getSendGiftLiveData
    }
    fun hitSendGiftApi(token: String, request: SendGiftRequest) {

        try {
            getSendGiftLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    getSendGiftLiveData.postValue(
                        Resources.success(
                            ApiRepository().sendGiftApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    getSendGiftLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val blockUserLiveData = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getBlockUserLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return blockUserLiveData
    }
    fun hitBlockUserApi(token:String, request:BlockUserRequest) {

        try {
            blockUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    blockUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().blockUserApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    blockUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val unblockUserLiveData = SingleLiveEvent<Resources<PostCommentsResponse>>()

    fun getUnblockUserLiveData(): LiveData<Resources<PostCommentsResponse>> {
        return unblockUserLiveData
    }
    fun hitUnblockUserApi(token:String, request: UnblockUserRequest) {

        try {
            unblockUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    unblockUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().unblockUserApi(token, request)
                        )
                    )
                } catch (ex: Exception) {
                    unblockUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val saveRecentChatLiveData = SingleLiveEvent<Resources<SaveRecentChatResponse>>()

    fun getSaveRecentChatLiveData(): LiveData<Resources<SaveRecentChatResponse>> {
        return saveRecentChatLiveData
    }
    fun hitSaveRecentChatDataApi(token: String,
                                request: SaveRecentChatRequest) {

        try {
            saveRecentChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    saveRecentChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().saveRecentChatApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    saveRecentChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    private val generateAgoraTokenLiveDate = SingleLiveEvent<Resources<AgoraTokenResponse>>()
    fun getGenerateAgoraTokenLiveData(): LiveData<Resources<AgoraTokenResponse>> {
        return generateAgoraTokenLiveDate
    }

    fun hitGenerateAgoraTokenDataApi(token: String, channelName: String, calleeId: String, uid:String, callType: String , sender_id: String) {

        try {
            generateAgoraTokenLiveDate.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    generateAgoraTokenLiveDate.postValue(
                        Resources.success(
                            ApiRepository().generateAgoraToken(token, channelName, calleeId, uid, callType, sender_id)
                        )
                    )
                } catch (ex: Exception) {
                    generateAgoraTokenLiveDate.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val predefinedChatLiveData = SingleLiveEvent<Resources<PredefinedChatsResponse>>()

    fun getPredefinedChatLiveData(): LiveData<Resources<PredefinedChatsResponse>> {
        return predefinedChatLiveData
    }
    fun hitPredefinedChatsApi(token: String) {

        try {
            predefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    predefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().getPredefinedChatsApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    predefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    val name = MutableLiveData<String>()
    val message = MutableLiveData<String>()
    val imageUrl = MutableLiveData<String>()
    val firestore = FirebaseFirestore.getInstance()

    fun sendMessage(
        sender: String,
        receiver: String,
        friendName: String,
        friendImage: String,
        messageType: String,
        message: String,
        pinned: String,
        archived: String,
        url: String,
        duration: String?,
        repliedMessage: String?=null,
    ) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TAG", "sendMessage: "+repliedMessage)
        try {
            if (message.isEmpty() && messageType== MediaType.TEXT.name) {
                Log.e("ChatAppViewModel", "Message is null or empty")
                return@launch
            }

            val currentUid = sender
            if (currentUid.isEmpty()) {
                Log.e("Firestore", "UID is null or empty. Aborting sendMessage.")
                return@launch
            }

            val messageId = UUID.randomUUID().toString()
            val hashMap = hashMapOf<String, Any>(
                "sender" to sender,
                "receiver" to receiver,
//                if (messageType== MediaType.AUDIO.name) "url" to url else "message" to "message",
//                if (messageType== MediaType.TEXT.name) "url" to "url" else "message" to message,
                if (message.isEmpty()) "url" to url else "message" to message,
                "time" to ChatUtils.getTime(),
                "statusSent" to MessageStatus.SENT.name,
                "messageType" to messageType,
                "messageId" to messageId,
            )
            if (duration?.isNotEmpty() == true) {
                hashMap["duration"] = duration
            }
            if (messageType == MediaType.REPLY.name) {
                hashMap["repliedMessage"] = repliedMessage.toString()
            }

            firestore.collection("Messages")
                .document(sender + "_" + receiver)
                .collection("chats")
                .document(messageId)
                .set(hashMap)
                .addOnCompleteListener { taskmessage ->
                    Log.i("TAG", "sendMessage message: $taskmessage")
                    this@ChatViewModel.message.value = ""
                    firestore.collection("Messages")
                        .document(receiver + "_" + sender)
                        .collection("chats")
                        .document(messageId)
                        .set(hashMap)
                        .addOnCompleteListener { _ -> }

                    // all work for recent list
                    val setHashap = hashMapOf<String, Any>(
                        "friendId" to receiver,
                        "time" to ChatUtils.getTime(),
                        "sender" to currentUid,
                        "message" to message,
                        "friendImage" to friendImage,
                        "name" to friendName,
                        "person" to "you",
                        "pinned" to pinned,
                        "archived" to archived,
                        "messageType" to messageType,
                    )
                    if (messageType == MediaType.REPLY.name) {
                        setHashap["repliedMessage"] = repliedMessage.toString()
                    }

                    firestore.collection("recentChats")
                        .document(currentUid)
                        .collection("chats")
                        .document(receiver)
                        .set(setHashap)

                    // Updating friend's recent chat
                    val chatDocRef = firestore.collection("recentChats")
                        .document(receiver)
                        .collection("chats")
                        .document(currentUid)

                    chatDocRef.get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            chatDocRef
                                .update(
                                    "message", message,
                                    "time", ChatUtils.getTime(),
                                    "person", name.value ?: "",
                                    "friendImage", imageUrl.value ?: "",
                                    "name", name.value ?: "",
                                    "messageType", messageType,
                                ).addOnSuccessListener {
                                    Log.d("Firestore", "Chat updated successfully")
                                }.addOnFailureListener { e ->
                                    Log.e("Firestore", "Error updating chat", e)
                                }
                        } else {
                            val setHashmap = hashMapOf<String, Any>(
                                "message" to message,
                                "time" to ChatUtils.getTime(),
                                "person" to name.value.toString(),
                                "friendImage" to imageUrl.value.toString(),
                                "name" to name.value.toString(),
                                "friendId" to currentUid,
                                "sender" to receiver,
                                "pinned" to "0",
                                "archived" to "0",
                                "messageType" to messageType
                            )
                            chatDocRef.set(setHashmap).addOnSuccessListener {
                                Log.d("Firestore", "New chat document created successfully")
                            }.addOnFailureListener { e ->
                                Log.e("Firestore", "Error creating chat document", e)
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Error checking document existence", e)
                    }

                    // firestore.collection("Users").document(receiver)
                    //     .addSnapshotListener { value, error ->
                    //         if (error != null) {
                    //             return@addSnapshotListener
                    //         }
                    //         Log.i("TAG", "sendMessage 1: $value       $error")
                    //         if (value != null && value.exists()) {
                    //             Log.i("TAG", "sendMessage: preference token " + Preferences.getStringPreference(MyApplication.appContext, FCM_TOKEN))
                    //             val token = value.getString("fcm_token")
                    //
                    //             if (message.isNotEmpty() && receiver.isNotEmpty()) {
                    //                 Log.i("TAG", "sendMessage fb console token: " + token)
                    //                 PushNotification(
                    //                     MessageData(
                    //                         token ?: "",
                    //                         data = mapOf(
                    //                             "title" to friendName,
                    //                             "message" to message,
                    //                             "receiver" to sender,
                    //                             "friendImage" to friendImage,
                    //                             "friendName" to friendName,
                    //                             "messageId" to messageId,
                    //                         )
                    //                     )
                    //                 ).also {
                    //                     // sendNotification(it)
                    //                 }
                    //             } else {
                    //                 Log.i("TAG", "sendMessage 3: " + value)
                    //             }
                    //         }
                    //
                    //         if (taskmessage.isSuccessful) {
                    //             this@ChatViewModel.message.value = ""
                    //         }
                    //     }
                }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("TAG", "sendMessage error: $e")
            return@launch
        }
    }

    fun startMessagesListener(friendId: String, userId: String) {
        // remove old listener if exists
        messagesListener?.remove()

        messagesListener = firestore.collection("Messages")
            .document("${userId}_${friendId}")
            .collection("chats")
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("MessageRepo", "Error fetching messages: ${exception.message}")
                    return@addSnapshotListener
                }

                val messagesList = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Messages::class.java)?.apply {
                        date = ChatUtils.getDay(time ?: "", "yyyy-MM-dd HH:mm:ss")
//                        if (sender != userId && statusSent != MessageStatus.SEEN.name) {
                            changeSentStatus(this, friendId, userId)
//                        }
                    }
                } ?: emptyList()
                _messagesFlow.value = messagesList
            }
    }
    private var messagesListener: ListenerRegistration? = null

    fun stopMessagesListener() {
        messagesListener?.remove()
        messagesListener = null
    }

    private fun changeSentStatus(message: Messages?, friendId: String, userId: String) {
        val hashMap = hashMapOf<String, Any>(
            "sender" to message?.sender.toString(),
            "receiver" to message?.receiver.toString(),
            "message" to message?.message.toString(),
            "time" to message?.time.toString(),
            "statusSent" to MessageStatus.SEEN.name,
        )
        firestore.collection("Messages")
            .document(friendId+"_"+userId)
            .collection("chats")
            .document(message?.messageId.toString())
            .update(hashMap).addOnCompleteListener {
                if (it.isSuccessful) {
//                    Log.i("TAG", "getMessages: " + "Updated success")
                } else {
                    Log.i("TAG", "getMessages: " + "Update failed")
                }
            }
    }
    private val _messagesFlow = MutableStateFlow<List<Messages>>(emptyList())
    val messagesFlow: StateFlow<List<Messages>> = _messagesFlow.asStateFlow()


    fun deleteMessage(groupId: String, message: Messages, listType: String) {
        val messageRef: DocumentReference
//        if (listType== toggleChatsType[0]){
            messageRef = firestore.collection("Messages")
                .document(message.sender+"_"+message.receiver)
                .collection("chats")
                .document(message.messageId ?: "")
//        }else{
//            messageRef = firestore.collection("groupMessages")
//                .document(groupId)
//                .collection("messages")
//                .document(message.messageId ?: "")
//        }

        messageRef.delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Message deleted successfully")
//                Toast.makeText(
//                    MyApplication.appContext?.applicationContext,
//                    "Message deleted successfully",
//                    Toast.LENGTH_SHORT
//                ).show()
                var messageRefOther = firestore.collection("Messages")
                    .document(message.receiver+"_"+message.sender)
                    .collection("chats")
                    .document(message.messageId ?: "")
                messageRefOther.delete()

            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to delete message", e)
                Toast.makeText(
                    HiHloApplication.appContext?.applicationContext,
                    "Failed to delete message",
                    Toast.LENGTH_SHORT
                ).show()
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
                        isChatDeleted.value = true
                        Toast.makeText(
                            HiHloApplication.appContext?.applicationContext,
                            "Chat deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        hitSaveRecentChatDataApi("Bearer $token", SaveRecentChatRequest(toUserId = receiver, message = " "))
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
    /*fun getPaidChatDetail(userid: String){
        firestore.collection("Users").document(userid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    paid.value = snapshot.getString("paid")
                    creator.value = snapshot.getString("creator")
                    Log.d("Firestore", "Updated Total Number: $paid $creator")
                }
            }
    }*/

    private val predefinedChatOfUserLiveData = SingleLiveEvent<Resources<PredefinedChatsResponse>>()

    fun getPredefinedChatOfUserLiveData(): LiveData<Resources<PredefinedChatsResponse>> {
        return predefinedChatOfUserLiveData
    }
    fun hitPredefinedChatsOfUserApi(token: String) {

        try {
            predefinedChatOfUserLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    predefinedChatOfUserLiveData.postValue(
                        Resources.success(
                            ApiRepository().getPredefinedChatsOfUserApi(token
                            )
                        )
                    )
                } catch (ex: Exception) {
                    predefinedChatOfUserLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



    private val deletePredefinedChatLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getDeletePredefinedChatLiveData(): LiveData<Resources<CommonResponse>> {
        return deletePredefinedChatLiveData
    }
    fun hitDeletePredefinedChatApi(token: String, id: String) {

        try {
            deletePredefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    deletePredefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().deletePreDefinedChatApi(token, id
                            )
                        )
                    )
                } catch (ex: Exception) {
                    deletePredefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val addPredefinedChatLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getAddPredefinedChatLiveData(): LiveData<Resources<CommonResponse>> {
        return addPredefinedChatLiveData
    }
    fun hitAddPredefinedChatApi(token: String, preDefinedChat: String) {

        try {
            addPredefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    addPredefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().addPreDefinedChatApi(token, preDefinedChat
                            )
                        )
                    )
                } catch (ex: Exception) {
                    addPredefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private val editPredefinedChatLiveData = SingleLiveEvent<Resources<CommonResponse>>()

    fun getEditPredefinedChatLiveData(): LiveData<Resources<CommonResponse>> {
        return editPredefinedChatLiveData
    }
    fun hitEditPredefinedChatApi(token: String, preDefinedChatId: String, preDefinedChat: String) {

        try {
            editPredefinedChatLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    editPredefinedChatLiveData.postValue(
                        Resources.success(
                            ApiRepository().editPreDefinedChatApi(token, preDefinedChatId, preDefinedChat
                            )
                        )
                    )
                } catch (ex: Exception) {
                    editPredefinedChatLiveData.postValue(Resources.error(ex.localizedMessage, null))

                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}