package com.app.hihlo.ui.home.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.hihlo.model.common_response.CommonResponse
import com.app.hihlo.enum.MediaType
import com.app.hihlo.model.chat.MessageStatus
import com.app.hihlo.model.save_recent_chat.request.SaveRecentChatRequest
import com.app.hihlo.model.save_recent_chat.response.SaveRecentChatResponse
import com.app.hihlo.model.story_seen.request.StorySeen
import com.app.hihlo.network_call.repository.ApiRepository
import com.app.hihlo.utils.ChatUtils
import com.app.hihlo.utils.network_utils.Resources
import com.app.hihlo.utils.network_utils.SingleLiveEvent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class NewStoryViewModel :ViewModel() {
    private val seenStoryLiveData = SingleLiveEvent<Resources<CommonResponse>>()
    val firestore = FirebaseFirestore.getInstance()


    fun seenStoryLiveData(): LiveData<Resources<CommonResponse>> {
        return seenStoryLiveData
    }
    fun hitSeenStoryDataApi(token: String, request: StorySeen) {

        try {
            seenStoryLiveData.postValue(Resources.loading(null))
            viewModelScope.launch {
                try {
                    seenStoryLiveData.postValue(
                        Resources.success(
                            ApiRepository().storySeenApi(token, request
                            )
                        )
                    )
                } catch (ex: Exception) {
                    seenStoryLiveData.postValue(Resources.error(ex.localizedMessage, null))

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
    ) = viewModelScope.launch(Dispatchers.IO) {
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

            firestore.collection("Messages")
                .document(sender + "_" + receiver)
                .collection("chats")
                .document(messageId)
                .set(hashMap)
                .addOnCompleteListener { taskmessage ->
                    Log.i("TAG", "sendMessage message: $taskmessage")

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

                    // Updating my recent chat
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
                                    "person", friendName,
                                    "friendImage", friendImage,
                                    "name", friendName,
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
                                "person" to friendName,
                                "friendImage" to friendImage,
                                "name" to friendName,
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

}