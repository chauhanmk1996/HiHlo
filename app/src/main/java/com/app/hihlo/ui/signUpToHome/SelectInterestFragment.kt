package com.app.hihlo.ui.signUpToHome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentSelectIntrestBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.interest_list.response.Interests
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.profile.view_model.EditProfileViewModel
import com.app.hihlo.ui.signup.fragment.SelectInterestAdapter
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status

class SelectInterestFragment : Fragment(), SelectInterestAdapter.OnInterestSelectedListener {
    private lateinit var binding: FragmentSelectIntrestBinding
    private lateinit var selectInterestAdapter: SelectInterestAdapter
    private val viewModel: EditProfileViewModel by viewModels()
    private val registerViewModel: RegisterationViewModel by viewModels()
    private var selectedInterest:String?=null
    private var selectedPosition:Int?=-1
    var selectedId:String?=null
    var signUpData: SignUp?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            signUpData = it.getParcelable("data")
            Log.e("TAG", "onCreate: $signUpData", )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       binding = FragmentSelectIntrestBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }
    private fun initViews(){
        loadRcv()
        binding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }
            clNext.setOnClickListener {
                if (selectedInterest.isNullOrEmpty()){
                    Toast.makeText(requireActivity(), "Please select your interest", Toast.LENGTH_SHORT).show()
                }else{
                    val data = SignUp(
                        name = signUpData?.name,
                        username = signUpData?.username,
                        email = signUpData?.email,
                        phoneNumber = signUpData?.phoneNumber,
                        gender_id = signUpData?.gender_id,
                        dob = signUpData?.dob,
                        password = signUpData?.password,
                        deviceType = signUpData?.deviceType,
                        deviceToken = signUpData?.deviceToken,
                        confirmPassword = signUpData?.password,
                        city = signUpData?.city,
                        interest_id = selectedId.toString()
                    )
                    hitRegisterApi(data)
                    Log.e("TAG", "initViews:ddd $data", )
                    Toast.makeText(requireActivity(), "Logged in successfully", Toast.LENGTH_SHORT).show()
                }
            }

        }
        Log.e("TAG", "initViews: $selectedPosition", )
    }

    private fun hitRegisterApi(data: SignUp) {
        registerViewModel.hitRegisterUser(model = data)
        registerViewModel.getRegisterLiveData().observe(viewLifecycleOwner) {
            Log.e("two", "getCityApi: called this", )
            when (it.status) {
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    if (it.data?.status == 1 && it.data.code == 200) {
                        val list = it.data.payload
                        Log.e("TAG", "hitRegisterApi: $list", )
                        Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                        Preferences.setCustomModelPreference<LoginResponse>(requireContext(),
                            LOGIN_DATA, it.data)
                        CommonUtils.hideKeyboard(requireActivity())
                        Log.i("TAG", "setObserver: "+ Preferences.getStringPreference(requireContext(),
                            FCM_TOKEN
                        ))
                        if(it.data.payload?.city.isNullOrBlank() || it.data.payload.profileImage.isNullOrEmpty()){
                            val bundle = Bundle()
                            val userDetails = list?.toUserDetailsX()
                            bundle.putString("from","normal")
                            bundle.putParcelable("userDetail",userDetails)
                            findNavController().navigate(R.id.editProfileNewFragment,bundle)
                        }else{
                            startActivity(Intent(requireActivity(), HomeActivity::class.java))
                            requireActivity().finish()
                        }
                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun Payload.toUserDetailsX(): UserDetailsX {
        return UserDetailsX(
            id = this.userId,
            name = if (this.name?.isNotEmpty() == true && this.name != "") this.name else this.fullName,
            username = this.username,
            email = this.email,
            phone = this.phone,
            dob = this.dob,
            city = this.city,
            country = this.country,
            about = null,   // Payload में नहीं है

            profileImage = this.profileImage,
            profile_image = this.profileImage, // अगर दोनों चाहिए तो same assign कर दो

            isCreator = this.isCreator,
            role = if (this.isCreator == 1) "Creator" else "User", // optional mapping

            // बाकी fields Payload में नहीं हैं तो default null/0 रहेंगे
            followers_count = null,
            following_count = null,
            gender = null,
            interest_name = null,
            is_verified = null,
            posts_count = null,
            blockStatus = null,
            reels_count = null,
            is_following = null,
            is_seen = null,
            user_live_status = null,
            creatorStatus = null,
            isStoryUploaded = null,
            is_story_uploaded = null,
            story = null,
            myStory = null,
            notificationSettings = null
        )
    }



    private fun loadRcv() {
        binding.rcvInterest.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL, false
        )
        selectInterestAdapter = SelectInterestAdapter(requireActivity(), this)
        binding.rcvInterest.adapter = selectInterestAdapter
        selectInterestAdapter.selectedPosition = selectedPosition!!
        getInterest()

    }

    private fun getInterest() {
        // Check cache first
        val cachedList = viewModel.interestCache.value
        if (!cachedList.isNullOrEmpty()) {
            selectInterestAdapter.setData(cachedList)
            return // ✅ use cached data, skip API call
        }

        // If no cache, call API and observe result
        viewModel.hitInterestListDataApi()

        viewModel.getInterestListLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    if (it.data?.status == 1 && it.data.code == 200) {
                        val list = it.data.payload.rows
                        selectInterestAdapter.setData(list)
                        viewModel.catcheInterstList(list) // ✅ store in cache
                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    override fun onInterestSelect(city: Interests, position:Int) {
        selectedInterest = city.name
        selectedPosition = position
        selectedId = city.id.toString()
        Log.e("TAG", "onInterestSelect: ${city.name} ${city.id}", )
    }
}