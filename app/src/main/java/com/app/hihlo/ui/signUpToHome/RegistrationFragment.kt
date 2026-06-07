package com.app.hihlo.ui.signUpToHome

import android.content.Intent
import android.graphics.Color
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
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentRegistrationBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.LOGIN_TYPE
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.ChatUtils
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.getLength
import com.app.hihlo.utils.logD
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.startScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.getValue

class RegistrationFragment :
    BaseNewFragment<FragmentRegistrationBinding, SignUpToHomeViewModel>(R.layout.fragment_registration) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var isUsernameAvailable: Int = 2
    private var isPassHidden = true
    private var isConfirmPassHidden = true
    private lateinit var firebaseAuth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private val usernameFilter = InputFilter { source, _, _, _, _, _ ->
        val regex = Regex("[a-zA-Z0-9._]+")
        if (source.isEmpty()) {
            null
        } else if (source.matches(regex)) {
            null
        } else {
            ""
        }
    }

    override fun onInitDataBinding(viewBinding: FragmentRegistrationBinding) {
        mViewModel.mDeviceTokenLiveData.value =
            Preferences.getStringPreference(requireContext(), FCM_TOKEN)
        setUpGoogleSignUp()
        observer(viewBinding)
        userNameTextWatcher(viewBinding)
        clickDoNotHaveAccount(viewBinding)
        setPasswordToggle(viewBinding)
        okClick(viewBinding)
        viewBinding.etUserName.filters = arrayOf(usernameFilter)
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

    private fun observer(viewBinding: FragmentRegistrationBinding) {
        mViewModel.checkUserNameResponse.observe(this) {
            if (it?.peekContent() != null) {
                viewBinding.apply {
                    isUsernameAvailable = it.peekContent().payload.usernameAvailable
                    if (isUsernameAvailable == 2 || isUsernameAvailable == 3) {
                        ivUserNameCheck.setImageResource(R.drawable.cancel_red)
                    } else {
                        ivUserNameCheck.setImageResource(R.drawable.checked_green)
                    }
                }
                mViewModel.checkUserNameResponse.value = null
            }
        }

        mViewModel.signUpResponse.observe(this) {
            if (it?.peekContent() != null) {
                val signUpRequest = SignUpRequest(
                    name = mViewModel.mNameLiveData.value ?: "",
                    email = mViewModel.mEmailIdLiveData.value ?: "",
                    username = mViewModel.mUserNameLiveData.value ?: "",
                    phoneNumber = mViewModel.mMobileNumberLiveData.value ?: "",
                    password = mViewModel.mPasswordLiveData.value ?: "",
                    confirmPassword = mViewModel.mConfirmPasswordLiveData.value ?: "",
                    deviceToken = Preferences.getStringPreference(
                        requireActivity(),
                        FCM_TOKEN
                    )
                )
                showToast(it.peekContent().message ?: "")
                val bundle = Bundle()
                bundle.putString("from", "register")
                bundle.putParcelable("data", signUpRequest)
                bundle.putString("purpose", "signup")
                findNavController().navigate(R.id.otpFragment, bundle)
                mViewModel.signUpResponse.value = null
            }
        }

        mViewModel.socialSignUpResponse.observe(this) {
            if (it?.peekContent() != null) {
                showToast(it.peekContent().message ?: "")
                Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                Preferences.setStringPreference(requireContext(), LOGIN_TYPE, "G")
                Preferences.setCustomModelPreference<LoginResponse>(
                    requireContext(),
                    LOGIN_DATA,
                    it.peekContent()
                )
                updateUserOnFirebase(it.peekContent().payload)
                if (it.peekContent().payload?.city.isNullOrBlank()) {
                    val bundle = Bundle()
                    val userDetails = it.peekContent().payload?.toUserDetailsX()
                    bundle.putString("from", "social")
                    bundle.putParcelable("userDetail", userDetails)
                    findNavController().navigate(R.id.editProfileNewFragment, bundle)
                } else {
                    startScreen(HomeActivity())
                    requireActivity().finish()
                }
                mViewModel.socialSignUpResponse.value = null
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val name = firebaseAuth.currentUser?.displayName
                    val email = firebaseAuth.currentUser?.email
                    val socialId = firebaseAuth.currentUser?.uid
                    val photoUrl = firebaseAuth.currentUser?.photoUrl

                    val socialSignUpRequest = SocialSignUpRequest(
                        name = name,
                        email = email,
                        social_id = socialId,
                        social_type = "G",
                        profile_image = photoUrl.toString(),
                        deviceToken = Preferences.getStringPreference(requireContext(), FCM_TOKEN),
                        deviceType = "A"
                    )
                    mViewModel.socialSignUpApi(socialSignUpRequest)
                } else {
                    ProcessDialog.dismissDialog(true)
                    showToast("Authentication Failed")
                }
            }
    }

    fun updateUserOnFirebase(payload: LoginPayload?) {
        val dataHashMap = hashMapOf(
            "userId" to payload?.userId,
            "name" to payload?.fullName,
            "email" to payload?.email,
            "status" to "online",
            "mobileNumber" to "",
            "device_platform" to "android",
            "fcm_token" to Preferences.getStringPreference(requireContext(), FCM_TOKEN),
            "createdAt" to ChatUtils.getUidLoggedIn(),
            "profilePicture" to ""
        )

        firestore.collection("Users").document(payload?.userId.toString()).set(dataHashMap)
            .addOnSuccessListener {
                logD("Firebase SignUp Success = $it")
            }.addOnFailureListener { error ->
                logD("Firebase SignUp Error = $error")
            }
    }

    fun LoginPayload.toUserDetailsX(): UserDetailsX {
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

    private fun userNameTextWatcher(viewBinding: FragmentRegistrationBinding) {
        viewBinding.etUserName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    val lower = it.toString().lowercase()
                    if (it.toString() != lower) {
                        viewBinding.etUserName.setText(lower)
                        viewBinding.etUserName.setSelection(lower.length)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                mViewModel.checkUsernameApi()
            }
        })
    }

    private fun clickDoNotHaveAccount(viewBinding: FragmentRegistrationBinding) {
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
        viewBinding.tvAlreadyHaveAnAccount.text = spannableString
        viewBinding.tvAlreadyHaveAnAccount.movementMethod = LinkMovementMethod.getInstance()
        viewBinding.tvAlreadyHaveAnAccount.highlightColor = Color.TRANSPARENT
    }

    private fun setPasswordToggle(viewBinding: FragmentRegistrationBinding) {
        viewBinding.apply {
            etPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
            ivPasswordToggle.setOnClickListener {
                isPassHidden = if (isPassHidden) {
                    ivPasswordToggle.setImageResource(R.drawable.open_eye_2)
                    etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    false
                } else {
                    ivPasswordToggle.setImageResource(R.drawable.close_eye_2)
                    etPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
                    true
                }
                etPassword.setSelection(etPassword.getLength())
            }

            etConfirmPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
            ivConfirmPasswordToggle.setOnClickListener {
                isConfirmPassHidden = if (isConfirmPassHidden) {
                    ivConfirmPasswordToggle.setImageResource(R.drawable.open_eye_2)
                    etConfirmPassword.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()
                    false
                } else {
                    ivConfirmPasswordToggle.setImageResource(R.drawable.close_eye_2)
                    etConfirmPassword.transformationMethod =
                        CommonUtils.DotPasswordTransformationMethod
                    true
                }
                etConfirmPassword.setSelection(etConfirmPassword.getLength())
            }
        }
    }

    private fun okClick(viewBinding: FragmentRegistrationBinding) {
        viewBinding.apply {
            btnSignUp.setOnClickListener {
                mViewModel.signUpApi()
            }

            clGoogleLogin.setOnClickListener {
                signInWithGoogle()
            }
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut()
        ProcessDialog.showDialog(requireActivity(), true)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    showToast("Sign-in canceled")
                }
            } catch (e: ApiException) {
                ProcessDialog.dismissDialog(true)
                showToast("Google sign in failed")
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

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}