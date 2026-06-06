package com.app.hihlo.utils
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.hihlo.R
import com.app.hihlo.model.staticModel.NotificationsTypeModel
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.NOTIFICATION_TOGGLE
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.preferences.UserPreference.AGORA_TOKEN
import com.app.hihlo.preferences.UserPreference.CALLER_USER_IMAGE
import com.app.hihlo.preferences.UserPreference.CALL_TYPE
import com.app.hihlo.preferences.UserPreference.CALL_USER_NAME
import com.app.hihlo.preferences.UserPreference.CHANNEL_NAME
import com.app.hihlo.preferences.UserPreference.CITY_COUNTRY
import com.app.hihlo.preferences.UserPreference.OTHER_USER_ID
import com.app.hihlo.preferences.UserPreference.USER_NAME
import com.app.hihlo.preferences.UserPreference.U_ID
import com.app.hihlo.ui.calling.activity.OldIncomingCallActivity
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.AgoraUtils.isAppOnForeground
import com.app.hihlo.utils.AgoraUtils.isForegroundServiceRunning
import com.app.hihlo.utils.CommonUtils.launchActivityWithBundle
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import java.util.Random

class MyFirebaseService : FirebaseMessagingService() {
    private var callStatusType: String? = ""
    private var notificationType: String? = ""
    private var callType: String? = ""


    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        Log.i("TAG", "onNewToken: $newToken")
        Preferences.setStringPreference(applicationContext, FCM_TOKEN, newToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: ""
        val message = data["body"] ?: remoteMessage.notification?.body ?: ""
        callStatusType = data["type"] ?: remoteMessage.notification?.body
        notificationType = data["notificationType"]
        callType = data["callType"]

        Log.i("TAG", "onMessageReceived: $title - $message")
        Log.i("TAG", "onMessageReceived: ${Gson().toJson(remoteMessage)}")
        if (callStatusType=="incoming_call"){
            val agoraToken = data["agoraToken"] ?: ""
            val channelName = data["channelName"] ?: ""
            val callerId = data["callerId"] ?: ""
            val callerName = data["callerName"] ?: ""
            val uId = data["uid"] ?: ""
            val callType = data["callType"] ?: ""
            val callerImage = data["callerAvatar"] ?: ""
            val callUserName = data["callerUserName"]?:""
            val callCountry = data["callerCountry"]
            val callCity = data["callerCity"]
            Log.e("MyFirebaseService", "inviteForVideoCall: agoraToken: $agoraToken, channelName: $channelName, callerId: $callerId, callerName: $callerName, uId: $uId, callType: $callType, callerImage: $callerImage, callUserName: $callUserName, callCity: $callCity, callCountry: $callCountry")
//            sendNotification(message, title, agoraToken, channelName, callerId, callerName, callerImage)
            inviteForVideoCall(agoraToken, channelName, callerId, callerName, uId, callType, callerImage,callUserName,"$callCity, $callCountry")
//            inviteForAudioCall(agoraToken, channelName, callerId, callerName, uId)

        }else if (callStatusType=="CALL_ENDED"){
            Log.i("TAG", "onMessageReceived: "+"callended")
            NotificationManagerCompat.from(applicationContext).cancelAll()
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                if (isForegroundServiceRunning(applicationContext, OnClearFromRecentService::class.java) && !isAppOnForeground(applicationContext)) {
                    val intent = Intent("app_background_action")
                    sendBroadcast(intent)
                } else if (AppStateLiveData.instance.getIsForeground().value != null && !AppStateLiveData.instance.getIsForeground().value!!) {
                    val intent = Intent("app_background_action")
                    sendBroadcast(intent)
                }
                else {
                    callEnd()
                }
            }
        }
        else{

        }
        var notificationData = Preferences.getCustomModelPreference<NotificationsTypeModel>(this, NOTIFICATION_TOGGLE) ?: NotificationsTypeModel()
        Log.i("TAG", "onMessageReceived: "+notificationData)
        if(notificationData.general == true && (notificationType=="like"||notificationType=="comment"||notificationType=="login")){
            showNotificationNormal(title, message, data)
        }else if(notificationData.general == true && notificationType=="message"){
            if(UserPreference.isChatFragmentOpen=="0"){
                showNotificationNormal(title, message, data)
            }
        }
        else if (notificationData.following == true &&notificationType=="follow"){
            showNotificationNormal(title, message, data)
        }else if(notificationData.audioCall == true && notificationType=="display" && callType=="audio"){
            showNotificationNormal(title, message, data)
        } else if(notificationData.videoCall == true && notificationType=="display" && callType=="video"){
            showNotificationNormal(title, message, data)
        }else{
//            showNotificationNormal(title, message, data)
        }
    }
    private fun callEnd() {
        val callEnd = Intent("callEnd")
        callEnd.`package` = packageName
        applicationContext.sendBroadcast(callEnd)
    }
    private fun inviteForVideoCall(
        agoraToken: String,
        callingChannelName: String,
        callerId: String,
        callerName: String,
        uId: String,
        callType: String,
        profileImage: String,
        callUserName: String,
        cityCountry: String,
    ) {
        val bundle = Bundle()
        bundle.putString(AGORA_TOKEN, agoraToken)
        bundle.putString(CHANNEL_NAME, callingChannelName)
//        bundle.putString(CALL_USER_IMAGE, callBody.senderId.image)
        bundle.putString(CALL_USER_NAME, callerName)
        bundle.putString(CALLER_USER_IMAGE, profileImage)
        bundle.putString(U_ID, uId)
        bundle.putString(CALL_TYPE, callType)
        bundle.putString(OTHER_USER_ID, callerId)
        bundle.putString(USER_NAME,callUserName)
        bundle.putString(CITY_COUNTRY,cityCountry)
        launchActivityWithBundle(OldIncomingCallActivity::class.java, bundle)
    }

    private fun showNotificationNormal(title: String, message: String, data: Map<String, String>) {
        val channelId = "default_channel_id"
        val channelName =  data["channelName"] ?: "Default Channel"

        var notificationId  = 456

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0+ requires a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Default notification channel"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val agoraToken = data["agoraToken"] ?: ""
        val callerId = data["callerId"] ?: ""
        val callerName = data["callerName"] ?: ""
        val uId = data["uid"] ?: ""
        val callType = data["callType"] ?: ""
        val callerImage = data["callerAvatar"] ?: ""
        val callUserName = data["callerUserName"]?:""
        val callCountry = data["callerCountry"]
        val callCity = data["callerCity"]
        val callStatusType = data["type"]
        Log.e("TAG", "showNotificationNormal: statys $callStatusType", )
      //  val channelName = data["channelName"] ?: ""


        Log.e("MyFirebaseService", "inviteForVideoCall: agoraToken: $agoraToken, channelName: $channelName, callerId: $callerId, callerName: $callerName, uId: $uId, callType: $callType, callerImage: $callerImage, callUserName: $callUserName, callCity: $callCity, callCountry: $callCountry")
        var intentNew = Intent()
        if(callStatusType=="incoming_call"){
//            intentNew = Intent(this, SingleVideoCallActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                putExtra("target_fragment", callStatusType)
//                putExtra("chat_id", data["chatId"] ?: 0)
//                putExtra(AGORA_TOKEN, agoraToken)
//                putExtra(CHANNEL_NAME, channelName)
//                putExtra(CALL_USER_NAME, callerName)
//                putExtra(CALLER_USER_IMAGE, callerImage)
//                putExtra(U_ID, uId)
//                putExtra(CALL_TYPE, callType)
//                putExtra(OTHER_USER_ID, callerId)
//                putExtra(USER_NAME,callUserName)
//                putExtra(CITY_COUNTRY,"$callCity, $callCountry")
//            }
//            notificationManager.cancel(notificationId)
        }else{
            intentNew = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("target_fragment", callStatusType)
                putExtra("chat_id", data["chatId"] ?: 0)
            }
            notificationManager.cancel(notificationId)

        }



        val pendingIntent = PendingIntent.getActivity(
            this, 0, intentNew,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Replace with your icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
    private fun getRequestCode(): Int {
        val rnd = Random()
        return 100 + rnd.nextInt(900000)
    }

}




