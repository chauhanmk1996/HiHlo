package com.app.hihlo.ui.signup.fragment

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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentSigninBinding
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.login.response.Payload
import com.app.hihlo.preferences.FCM_TOKEN
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.LOGIN_TYPE
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.signup.model.SocialSignUpRequest
import com.app.hihlo.ui.signup.view_model.SigninViewModel
import com.app.hihlo.utils.ChatUtils.getUidLoggedIn
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.logD
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SigninFragment : BaseFragment<FragmentSigninBinding>() {
    private var isPassHidden = true
    private val viewModel: SigninViewModel by viewModels()
    lateinit var firestore : FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var firebaseAuth: FirebaseAuth

    override fun getLayoutId(): Int {
        return R.layout.fragment_signin
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        CommonUtils.touchHideKeyBoard(view, requireActivity())
    }

    override fun initView(savedInstanceState: Bundle?) {
        setObserver()
        setUpGoogleSignUp()
        clickTermsConditions()
        clickDoNotHaveAccount()
        setPasswordToggle()
        onClick()
    }

    private fun setObserver() {
        viewModel.getLoginLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Login success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                            Preferences.setCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA,
                                it.data
                            )
                            CommonUtils.hideKeyboard(requireActivity())
                            Log.i(
                                "TAG",
                                "setObserver: " + Preferences.getStringPreference(
                                    requireContext(),
                                    FCM_TOKEN
                                )
                            )
                            updateUserOnFirebase(it.data.payload)

                            if (it.data.payload?.city.isNullOrBlank() || it.data.payload.profileImage.isNullOrEmpty()) {
                                val bundle = Bundle()
                                val userDetails = it.data.payload?.toUserDetailsX()
                                bundle.putString("from", "normal")
                                bundle.putParcelable("userDetail", userDetails)
                                findNavController().navigate(R.id.editProfileNewFragment, bundle)
                            } else {
                                startActivity(Intent(requireActivity(), HomeActivity::class.java))
                                requireActivity().finish()
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
                    ProcessDialog.showDialog(requireContext(), true)
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
        viewModel.validationMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.getSocialLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Login success: ${Gson().toJson(it)}")
                    if (it.data?.status == 1) {
                        if (it.data.code == 200) {
                            Preferences.setStringPreference(requireContext(), IS_LOGIN, "2")
                            Preferences.setStringPreference(requireContext(), LOGIN_TYPE, "G")
                            Preferences.setCustomModelPreference<LoginResponse>(
                                requireContext(),
                                LOGIN_DATA,
                                it.data
                            )
                            CommonUtils.hideKeyboard(requireActivity())
                            Log.i(
                                "TAG",
                                "setObserver: " + Preferences.getStringPreference(
                                    requireContext(),
                                    FCM_TOKEN
                                )
                            )
                            updateUserOnFirebase(it.data.payload)
                            if (it.data.payload?.city.isNullOrBlank() || it.data.payload.profileImage.isNullOrEmpty()) {
                                val bundle = Bundle()
                                val userDetails = it.data.payload?.toUserDetailsX()
                                bundle.putString("from", "social")
                                bundle.putParcelable("userDetail", userDetails)
                                findNavController().navigate(R.id.editProfileNewFragment, bundle)
                            } else {
                                startActivity(Intent(requireActivity(), HomeActivity::class.java))
                                requireActivity().finish()
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
                    ProcessDialog.showDialog(requireContext(), true)
                }

                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
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

    private fun clickTermsConditions() {
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
        binding.tvTermsConditions.text = spannableString
        binding.tvTermsConditions.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTermsConditions.highlightColor = Color.TRANSPARENT
    }

    private fun clickDoNotHaveAccount() {
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
        binding.tvDontHaveAccount.text = spannableString
        binding.tvDontHaveAccount.movementMethod = LinkMovementMethod.getInstance()
        binding.tvDontHaveAccount.highlightColor = Color.TRANSPARENT
    }

    private fun setPasswordToggle() {
        binding.passwordToggle.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            ), PorterDuff.Mode.SRC_IN
        )
        binding.password.transformationMethod = CommonUtils.DotPasswordTransformationMethod
        binding.passwordToggle.setOnClickListener {
            isPassHidden = if (isPassHidden) {
                binding.passwordToggle.setImageResource(R.drawable.open_eye_2)
                binding.password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                false
            } else {
                binding.passwordToggle.setImageResource(R.drawable.close_eye_2)
                binding.password.transformationMethod = CommonUtils.DotPasswordTransformationMethod
                true
            }
            binding.password.setSelection(binding.password.text.toString().length)
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
                logD("Google Login Success: $it")
            }.addOnFailureListener { error ->
                logD("Google Login Error: $error")
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

    private fun onClick() {
        binding.apply {
            tvForgotPassword.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("from", "login")
                findNavController().navigate(R.id.emailFragment, bundle)
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

    private fun hitSocialLoginApi(currentUser: FirebaseUser?, ) {
        val model = SocialSignUpRequest(
            name = currentUser?.displayName,
            email = currentUser?.email,
            profile_image = currentUser?.photoUrl.toString(),
            social_id = currentUser?.uid,
            social_type = "G",
            deviceToken = Preferences.getStringPreference(requireContext(), FCM_TOKEN),
            deviceType = "A"
        )
        logD("HitSocialLoginApi: $model")
        viewModel.hitSocialApi(model)
    }
}