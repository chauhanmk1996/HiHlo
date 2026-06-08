package com.app.hihlo.ui.signUpToHome

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentSigninBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.utils.ChatUtils
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.logD
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.startScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.getValue

class SigninFragment :
    BaseNewFragment<FragmentSigninBinding, SignUpToHomeViewModel>(R.layout.fragment_signin) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var isPassHidden = true
    lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onInitDataBinding(viewBinding: FragmentSigninBinding) {
        mViewModel.mEmailIdLiveData.value = ""
        mViewModel.mPasswordLiveData.value = ""
        mViewModel.mDeviceTokenLiveData.value =
            Preferences.getStringPreference(requireContext(), FCM_TOKEN)
        setObserver()
        setUpGoogleSignUp()
        clickTermsConditions(viewBinding)
        clickDoNotHaveAccount(viewBinding)
        setPasswordToggle(viewBinding)
        onClick(viewBinding)
    }

    private fun setObserver() {
        mViewModel.loginResponse.observe(this) {
            if (it?.peekContent() != null) {
                it.peekContent().payload?.let { data ->
                    Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                    Preferences.setCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA,
                        it.peekContent()
                    )
                    CommonUtils.hideKeyboard(requireActivity())
                    updateUserOnFirebase(it.peekContent().payload)

                    if (data.city.isNullOrBlank() || data.profileImage.isNullOrEmpty()) {
                        val bundle = Bundle()
                        val userDetails = data.toUserDetailsX()
                        bundle.putString("from", "normal")
                        bundle.putParcelable("userDetail", userDetails)
                        findNavController().navigate(R.id.editProfileNewFragment, bundle)
                    } else {
                        startScreen(HomeActivity())
                        requireActivity().finish()
                    }
                }
                mViewModel.loginResponse.value = null
            }
        }

        mViewModel.socialLoginResponse.observe(this) {
            if (it?.peekContent() != null) {
                it.peekContent().payload?.let { data ->
                    Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                    Preferences.setCustomModelPreference<LoginResponse>(
                        requireContext(),
                        LOGIN_DATA,
                        it.peekContent()
                    )
                    CommonUtils.hideKeyboard(requireActivity())
                    updateUserOnFirebase(it.peekContent().payload)

                    if (data.city.isNullOrBlank() || data.profileImage.isNullOrEmpty()) {
                        val bundle = Bundle()
                        val userDetails = data.toUserDetailsX()
                        bundle.putString("from", "normal")
                        bundle.putParcelable("userDetail", userDetails)
                        findNavController().navigate(R.id.editProfileNewFragment, bundle)
                    } else {
                        startScreen(HomeActivity())
                        requireActivity().finish()
                    }
                }
                mViewModel.socialLoginResponse.value = null
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
                logD("Google Login Success: $it")
            }.addOnFailureListener { error ->
                logD("Google Login Error: $error")
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
            role = if (this.isCreator == 1) "Creator" else "User", // optional mapping

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

    private fun setUpGoogleSignUp() {
        firestore = FirebaseFirestore.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Get this from firebase console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun clickTermsConditions(viewBinding: FragmentSigninBinding) {
        val fullText = "I agree to Terms & Conditions and Privacy Policy of the App"
        val spannableString = SpannableString(fullText)
        val termsClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val bundle = Bundle()
                bundle.putString("screen", "termsCondition")
                findNavController().navigate(R.id.termsConditionsFragment, bundle)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color =
                    ContextCompat.getColor(requireActivity(), R.color.theme) // your link color
                ds.isUnderlineText = false
            }
        }

        val privacyClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val bundle = Bundle()
                bundle.putString("screen", "privacy")
                findNavController().navigate(R.id.termsConditionsFragment, bundle)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireActivity(), R.color.theme)
                ds.isUnderlineText = false
            }
        }
        val termsStart = fullText.indexOf("Terms & Conditions")
        val termsEnd = termsStart + "Terms & Conditions".length
        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + " Privacy Policy".length

        spannableString.setSpan(
            termsClickable,
            termsStart,
            termsEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            privacyClickable,
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        viewBinding.tvTermsConditions.text = spannableString
        viewBinding.tvTermsConditions.movementMethod = LinkMovementMethod.getInstance()
        viewBinding.tvTermsConditions.highlightColor = Color.TRANSPARENT
    }

    private fun clickDoNotHaveAccount(viewBinding: FragmentSigninBinding) {
        val fullText = "Don’t have an account ?  Sign Up"
        val spannableString = SpannableString(fullText)
        val signUpClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.registrationFragment)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireActivity(), R.color.theme)
                ds.isUnderlineText = false
            }
        }

        val signUpStart = fullText.indexOf("Sign Up")
        val signUpEnd = signUpStart + "Sign Up".length

        spannableString.setSpan(
            signUpClick,
            signUpStart,
            signUpEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        viewBinding.tvDoNotHaveAccount.text = spannableString
        viewBinding.tvDoNotHaveAccount.movementMethod = LinkMovementMethod.getInstance()
        viewBinding.tvDoNotHaveAccount.highlightColor = Color.TRANSPARENT
    }

    private fun setPasswordToggle(viewBinding: FragmentSigninBinding) {
        viewBinding.ivPasswordToggle.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            ), PorterDuff.Mode.SRC_IN
        )
        viewBinding.etPassword.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        viewBinding.ivPasswordToggle.setOnClickListener {
            isPassHidden = if (isPassHidden) {
                viewBinding.ivPasswordToggle.setImageResource(R.drawable.open_eye_2)
                viewBinding.etPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                viewBinding.ivPasswordToggle.setImageResource(R.drawable.close_eye_2)
                viewBinding.etPassword.transformationMethod =
                    CommonUtils.DotPasswordTransformationMethod
                true
            }
            viewBinding.etPassword.setSelection(viewBinding.etPassword.text.toString().length)
        }
    }

    private fun onClick(viewBinding: FragmentSigninBinding) {
        viewBinding.btnLogin.setOnClickListener {
            mViewModel.loginApi()
        }

        viewBinding.tvForgotPassword.setOnClickListener {
             val bundle = Bundle()
             bundle.putString("from", "login")
             findNavController().navigate(R.id.forgotPasswordFragment, bundle)
        }

        viewBinding.clGoogleLogin.setOnClickListener {
            signInWithGoogle()
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
            Log.e("GoogleSignIn", "resultCode = $resultCode, data = $data")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account?.idToken != null) {
                    logD("GoogleSignIn:: IdToken = ${account.idToken ?: ""}")
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    logD("GoogleSignIn:: Google Sign-in failed")
                    showToast("Google Sign-in failed")
                }
            } catch (e: ApiException) {
                logD("GoogleSignIn:: ${e.message ?: e.localizedMessage ?: ""}")
                showToast(e.message ?: e.localizedMessage ?: "")
                ProcessDialog.dismissDialog(true)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    hitSocialLoginApi(firebaseAuth.currentUser)
                } else {
                    showToast("Authentication Failed")
                }
            }
    }

    private fun hitSocialLoginApi(currentUser: FirebaseUser?) {
        val socialLoginRequest = SocialLoginRequest(
            name = currentUser?.displayName,
            email = currentUser?.email,
            profile_image = currentUser?.photoUrl.toString(),
            social_id = currentUser?.uid,
            social_type = "G",
            deviceToken = Preferences.getStringPreference(requireContext(), FCM_TOKEN),
            deviceType = "A"
        )
        mViewModel.socialLoginApi(socialLoginRequest)
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}