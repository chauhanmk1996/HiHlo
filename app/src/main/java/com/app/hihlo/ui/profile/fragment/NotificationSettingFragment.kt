package com.app.hihlo.ui.profile.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.databinding.FragmentNotificationSettingBinding
import com.app.hihlo.model.get_notification_setting.response.GetNotificationSettingResponse
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.set_notification.SetNotificationRequest
import com.app.hihlo.model.staticModel.NotificationsTypeModel
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.NOTIFICATION_TOGGLE
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.view_model.NotificationSettingsViewModel
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class NotificationSettingFragment : Fragment() {
    private val viewModel: NotificationSettingsViewModel by viewModels()
    private lateinit var binding:FragmentNotificationSettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotificationSettingBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }
    private fun initViews(){
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        setTogglesListener()
//        val token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString()
//        viewModel.hitGetPushNotificationApi(token)
//        setTogglesListener()
    }
    private fun setTogglesListener() {
        Log.i("TAG", "setTogglesListener: "+Preferences.getCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE))
        binding.apply {
            var notificationData = Preferences.getCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE) ?: NotificationsTypeModel()
            switchGeneral.isChecked = notificationData.general == true
            switchAudioCall.isChecked = notificationData.audioCall == true
            switchVideo.isChecked = notificationData.videoCall == true
            switchPayments.isChecked = notificationData?.payments == true
            switchFollowers.isChecked = notificationData?.followers == true
            switchFollowing.isChecked = notificationData?.following == true

            switchGeneral.setOnCheckedChangeListener { _, isChecked ->
                notificationData.general = isChecked
                Preferences.setCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE, notificationData)
//                hitSetPushNotificationsApi(SetNotificationRequest(generalNotification = isChecked.toInt()))
                if (isChecked) {

                } else {

                }
            }
            switchAudioCall.setOnCheckedChangeListener { _, isChecked ->
                notificationData.audioCall = isChecked
                Preferences.setCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE, notificationData)
//                hitSetPushNotificationsApi(SetNotificationRequest(audioCall = isChecked.toInt()))
                if (isChecked) {

                } else {

                }
            }
            switchVideo.setOnCheckedChangeListener { _, isChecked ->
                notificationData.videoCall = isChecked
                Preferences.setCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE, notificationData)
//                hitSetPushNotificationsApi(SetNotificationRequest(videoCall = isChecked.toInt()))
                if (isChecked) {

                } else {

                }
            }
            switchPayments.setOnCheckedChangeListener { _, isChecked ->
                notificationData.payments = isChecked
                Preferences.setCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE, notificationData)
//                hitSetPushNotificationsApi(SetNotificationRequest(payments = isChecked.toInt()))
                if (isChecked) {

                } else {

                }
            }
            switchFollowers.setOnCheckedChangeListener { _, isChecked ->
                notificationData.followers = isChecked
                Preferences.setCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE, notificationData)
//                hitSetPushNotificationsApi(SetNotificationRequest(follow = isChecked.toInt()))
                if (isChecked) {

                } else {

                }
            }
            switchFollowing.setOnCheckedChangeListener { _, isChecked ->
                notificationData.following = isChecked
                Preferences.setCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE, notificationData)
//                hitSetPushNotificationsApi(SetNotificationRequest(following = isChecked.toInt()))
                if (isChecked) {

                } else {

                }
            }
        }
    }
    private fun setObserver() {
        viewModel.getPushNotificationLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
//                            setTogglesListener(it.data.payload)
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.pushNotificationLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
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
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

    }
    fun hitSetPushNotificationsApi(request: SetNotificationRequest){
        val token = "Bearer "+Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken.toString()
        viewModel.hitSetPushNotificationApi(token, request = request)
    }
    fun Boolean.toInt() = if (this) 1 else 2


//    do not delete this function
    private fun setCallVisibilityToOthers(notificationData: GetNotificationSettingResponse.Payload) {
        Log.i("TAG", "setTogglesListener: "+Preferences.getCustomModelPreference<NotificationsTypeModel>(requireContext(), NOTIFICATION_TOGGLE))
        binding.apply {
            switchGeneral.isChecked = notificationData.generalNotification == "1"
            switchAudioCall.isChecked = notificationData.audioCall == "1"

            switchGeneral.setOnCheckedChangeListener { _, isChecked ->
                notificationData.generalNotification = if (isChecked) "1" else "2"
                hitSetPushNotificationsApi(SetNotificationRequest(generalNotification = isChecked.toInt()))
            }
            switchAudioCall.setOnCheckedChangeListener { _, isChecked ->
                notificationData.audioCall = if (isChecked) "1" else "2"
                hitSetPushNotificationsApi(SetNotificationRequest(audioCall = isChecked.toInt()))
            }
        }
    }


}