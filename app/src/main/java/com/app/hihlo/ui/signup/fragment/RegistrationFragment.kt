package com.app.hihlo.ui.signup.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentRegistrationBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.login.response.Payload
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.LOGIN_TYPE
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.signup.view_model.SendMailOtpViewModel
import com.app.hihlo.ui.signup.model.SignUp
import com.app.hihlo.ui.signup.model.SocialSignUpRequest
import com.app.hihlo.ui.signup.view_model.SocialSignUpViewModel
import com.app.hihlo.utils.ChatUtils.getUidLoggedIn
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class RegistrationFragment : Fragment() {
    private var isUsernameAvailable: Int = 2
    private lateinit var binding: FragmentRegistrationBinding
    private var isPassHidden = true
    private var isCnfPassHidden = true
    private val viewModel: SendMailOtpViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    private val socialViewModel: SocialSignUpViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRegistrationBinding.inflate(layoutInflater)
        setUpGoogleSignUp()
        initViews()
        usernameTextWatcher()
        keyBoardClose()
        binding.etUserName.filters = arrayOf(usernameFilter)

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    fun keyBoardClose() {
        binding.scrollView.setOnTouchListener { _, motionEvent ->
            // Check if the touch event is outside an EditText
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                // Find the currently focused view
                val currentFocusedView = requireActivity().currentFocus
                if (currentFocusedView is EditText) {
                    // If it's an EditText, hide the keyboard when tapping outside of it
                    val imm =
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocusedView.windowToken, 0)
                }
            }
            false
        }
    }

    private val usernameFilter = InputFilter { source, _, _, _, _, _ ->
        val regex = Regex("[a-zA-Z0-9._]+")
        if (source.isEmpty()) {
            null
        } else if (source.matches(regex)) {
            null // allow valid chars
        } else {
            ""   // block invalid chars
        }
    }

    private fun setUpGoogleSignUp() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Get this from firebase console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun usernameTextWatcher() {
        binding.etUserName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    val lower = it.toString().lowercase()
                    if (it.toString() != lower) {
                        binding.etUserName.setText(lower)
                        binding.etUserName.setSelection(lower.length)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                socialViewModel.hitCheckUsernameDataApi(
                    binding.etUserName.text.toString()
                )
            }
        })
    }

    private fun initViews() {
        clickDontHaveAccount()
        setPasswordToggle()
        binding.apply {
            btnSignUp.setOnClickListener {
                checkValidation()
            }

            clGoogleLogin.setOnClickListener {
                signInWithGoogle()
            }
        }
    }

    private fun checkValidation() {
        val etName = binding.etName.text.toString()
        val etUserName = binding.etUserName.text.toString()
        val etEmail = binding.etEmail.text.toString()
        val etPhone = binding.etPhoneNumber.text.toString()
        val password = binding.etPassword.text.toString()
        val cnfPassword = binding.etCnfPassword.text.toString()
        val isValidUserName = etUserName.matches("^(?=.*\\d)[a-zA-Z0-9]+$".toRegex())
        if (etName.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter your name", Toast.LENGTH_SHORT).show()
        } else if (!etName.matches(Regex("^[a-zA-Z ]+$"))) {
            Toast.makeText(requireActivity(), "Name must contain only letters", Toast.LENGTH_SHORT)
                .show()
        } else if (etUserName.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter your user name", Toast.LENGTH_SHORT)
                .show()
        } else if (isUsernameAvailable != 1) {
            Toast.makeText(requireActivity(), "Please enter a valid user name.", Toast.LENGTH_SHORT)
                .show()
        } else if (etEmail.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter your email", Toast.LENGTH_SHORT).show()
        } else if (!CommonUtils.isValidEmail(etEmail)) {
            Toast.makeText(requireActivity(), "Please enter valid email", Toast.LENGTH_SHORT).show()
        } else if (etPhone.isEmpty()) {
            Toast.makeText(requireActivity(), "Please your phone number", Toast.LENGTH_SHORT).show()
        } else if (password.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter password", Toast.LENGTH_SHORT).show()
        } else if (cnfPassword.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter confirm password", Toast.LENGTH_SHORT)
                .show()
        } else if (cnfPassword.length < 8 || password.length < 8) {
            Toast.makeText(
                requireActivity(),
                "Please should be atleast 8 characters long!",
                Toast.LENGTH_SHORT
            ).show()
        } else if (!CommonUtils.isValidPassword(password)) {
            Toast.makeText(
                requireActivity(),
                "Password must contain contain 1 uppercase, 1 lowercase, and 1 special character",
                Toast.LENGTH_SHORT
            ).show()
        } else if (password != cnfPassword) {
            Toast.makeText(
                requireActivity(),
                "Password and confirm password not matched",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.e("TAG", "checkValidation: $etName $etUserName")
            viewModel.hitSendEmailOtp(etEmail, etUserName, "signup")
            initObserver(etName, etEmail, etUserName, etPhone, password, "signup")
        }
    }

    fun updateUserOnFirebase(payload: Payload?) {
        val dataHashMap = hashMapOf(
            "userId" to payload?.userId,
            "name" to payload?.fullName,
            "email" to payload?.email,
            "status" to "online",
            "mobileNumber" to "",
            "device_platform" to "android",
            "fcm_token" to Preferences.getStringPreference(requireContext(), FCM_TOKEN),
            "createdAt" to getUidLoggedIn(),
            "profilePicture" to ""
        )

        firestore.collection("Users").document(payload?.userId.toString()).set(dataHashMap)
            .addOnSuccessListener {
                Log.i("TAG", "firebase signup: " + it)
//            Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
//            startActivity(Intent(this, SignInActivity::class.java))
            }.addOnFailureListener { error ->
                Log.i("TAG", "firebase signup: " + error)
//            Toast.makeText(requireContext(), "Registration failed, try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initObserver(
        etName: String,
        etEmail: String,
        etUserName: String,
        etPhone: String,
        password: String,
        string: String,
    ) {
        viewModel.getLoginLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "interest list success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            val list = it.data.payload
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                            val data = SignUp(
                                name = etName,
                                email = etEmail,
                                username = etUserName,
                                phoneNumber = etPhone,
                                password = password,
                                confirmPassword = password,
                                deviceToken = Preferences.getStringPreference(
                                    requireActivity(),
                                    FCM_TOKEN
                                )
                            )
                            val bundle = Bundle()
                            bundle.putString("from", "register")
                            bundle.putParcelable("data", data)
                            bundle.putString("purpose", "signup")
                            findNavController().navigate(R.id.otpFragment, bundle)
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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

    private fun clickDontHaveAccount() {
        val fullText = "Already have an account ?  Sign In"
        val spannableString = SpannableString(fullText)
        val signUpClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().popBackStack()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color =
                    ContextCompat.getColor(requireActivity(), R.color.theme) // your link color
                ds.isUnderlineText = false
            }
        }

        val signUpStart = fullText.indexOf("Sign In")
        val signUpEnd = signUpStart + "Sign In".length

        spannableString.setSpan(
            signUpClick,
            signUpStart,
            signUpEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvDontHaveAccount.text = spannableString
        binding.tvDontHaveAccount.movementMethod = LinkMovementMethod.getInstance()
        binding.tvDontHaveAccount.highlightColor = Color.TRANSPARENT


    }

    private fun setPasswordToggle() {
        //for new Password
        binding.etPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        binding.passwordToggle.setOnClickListener {
            isPassHidden = if (isPassHidden) {
                binding.passwordToggle.setImageResource(R.drawable.open_eye_2)
                binding.etPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                binding.passwordToggle.setImageResource(R.drawable.close_eye_2)
                binding.etPassword.transformationMethod =
                    CommonUtils.DotPasswordTransformationMethod
                true
            }
            binding.etPassword.setSelection(binding.etPassword.text.toString().length)
        }


        //for confirm new password
        binding.etCnfPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        binding.cnfPasswordToggle.setOnClickListener {
            isCnfPassHidden = if (isCnfPassHidden) {
                binding.cnfPasswordToggle.setImageResource(R.drawable.open_eye_2)
                binding.etCnfPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                binding.cnfPasswordToggle.setImageResource(R.drawable.close_eye_2)
                binding.etCnfPassword.transformationMethod =
                    CommonUtils.DotPasswordTransformationMethod
                true
            }
            binding.etCnfPassword.setSelection(binding.etCnfPassword.text.toString().length)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CommonUtils.touchHideKeyBoard(view, requireActivity())
        socialViewModel.getCheckUsernameLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "check username success: ${Gson().toJson(it)}")
                    if (it.data?.status == 0 || it.data?.status == 1) {
                        if (it.data.code == 200) {
                            binding.apply {
                                isUsernameAvailable = it.data.payload.usernameAvailable
                                if (it.data.payload.usernameAvailable == 2 || it.data.payload.usernameAvailable == 3) {
//                                    usernameWarning.isVisible=true
//                                    usernameWarning.text = it.data.message
                                    usernameCheck.setImageResource(R.drawable.cancel_red)

                                } else {
//                                    usernameWarning.isVisible=false
                                    usernameCheck.setImageResource(R.drawable.checked_green)
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT)
                            .show()
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

    override fun onPause() {
        super.onPause()
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
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
            about = null,

            profileImage = this.profileImage,
            profile_image = this.profileImage,

            isCreator = this.isCreator,
            role = if (this.isCreator == 1) "Creator" else "User",

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

    private fun signInWithGoogle() {
        googleSignInClient.signOut()
        ProcessDialog.showDialog(requireActivity(),true)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Log.e("LoginActivity", "Sign-in canceled: "+data)
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Log.e("LoginActivity", "Sign-in canceled: "+task)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    // ProcessDialog.dismissDialog(true)
                    Log.w("LoginActivity", "Sign-in canceled")
                    // Dismiss your dialog or take fallback action here
                }
            } catch (e: ApiException) {
                ProcessDialog.dismissDialog(true)
                Log.w("LoginActivity", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val socialId = firebaseAuth.currentUser?.uid
                    val email = firebaseAuth.currentUser?.email
                    val name = firebaseAuth.currentUser?.displayName
                    val photoUrl = firebaseAuth.currentUser?.photoUrl
                    hitSocialApi(name,email,photoUrl,socialId)
                    Log.e("TAG", "firebaseAuthWithGoogle: $socialId \n $email $name $photoUrl", )
                } else {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(requireActivity(), "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun hitSocialApi(name: String?, email: String?, photoUrl: Uri?, socialId: String?) {
        val model = SocialSignUpRequest(
            name = name,
            email = email,
            social_id = socialId,
            social_type = "G",
            profile_image = photoUrl.toString(),
            deviceToken =Preferences.getStringPreference(requireContext(), FCM_TOKEN),
            deviceType = "A"
        )
        socialViewModel.hitSocialSignUpUser(model)
        socialViewModel.getSocialSignUpLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "interest list success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            it.data.payload
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                            Preferences.setStringPreference(requireContext(), LOGIN_TYPE, "G")
                            Preferences.setCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA, it.data)
                            CommonUtils.hideKeyboard(requireActivity())
                            Log.i("TAG", "setObserver: "+ Preferences.getStringPreference(requireContext(), FCM_TOKEN))

                            updateUserOnFirebase(it.data.payload)
                            if(it.data.payload?.city.isNullOrBlank()){
                                val bundle = Bundle()
                                val userDetails = it.data.payload?.toUserDetailsX()
                                bundle.putString("from","social")
                                bundle.putParcelable("userDetail",userDetails)
                                findNavController().navigate(R.id.editProfileNewFragment,bundle)
                            }else{
                                startActivity(Intent(requireActivity(), HomeActivity::class.java))
                                requireActivity().finish()
                            }
                            /*startActivity(Intent(requireActivity(), HomeActivity::class.java))
                            requireActivity().finish()*/
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    //ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

    }
}