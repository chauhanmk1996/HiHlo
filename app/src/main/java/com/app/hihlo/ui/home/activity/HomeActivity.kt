package com.app.hihlo.ui.home.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.request.transition.Transition
import com.app.hihlo.R
import com.app.hihlo.base.BaseActivity
import com.app.hihlo.databinding.ActivityHomeBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.navigation.findNavController
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.HomeNew.HomeNewFragment
import com.app.hihlo.ui.calling.CallStateHolder
import com.app.hihlo.ui.calling.view_model.CallStateViewModel
import com.app.hihlo.utils.RTVariable
import com.app.hihlo.utils.UserDataManager
import com.app.hihlo.utils.common.ScrollDirectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeActivity : BaseActivity<ActivityHomeBinding>(), ScrollDirectionListener {

    private lateinit var navController: NavController
    private val CAMERA_MIC_PERMISSION_REQUEST_CODE = 100
    private val OVERLAY_PERMISSION_REQ_CODE = 123
    var applyBottomBarPadding: Boolean = true
    var userImageUrl = ""
    val vm: CallStateViewModel by viewModels()

    // Optional: also animate the FAB + imgBtn together if you want
    override fun hideBottomElements() {
//        listOf(binding.bottomAppBar, binding.floatingbtn, binding.imgBtn).forEach { view ->
//            if (view.visibility == View.VISIBLE) {
//                view.animate()
//                    .translationY(view.height.toFloat())
//                    .alpha(0.85f)           // optional: slight fade
//                    .setDuration(180L)
//                    .setInterpolator(AccelerateInterpolator())
//                    .withEndAction { /* do NOT set invisible here */ }
//                    .start()
//            }
//        }
        hideNavigationView()
    }

    override fun showBottomElements() {
//        listOf(binding.bottomAppBar, binding.floatingbtn, binding.imgBtn).forEach { view ->
//            view.translationY = view.height.toFloat()
//            view.alpha = 0.85f
//
//            view.animate()
//                .translationY(0f)
//                .alpha(1f)
//                .setDuration(220L)
//                .setInterpolator(DecelerateInterpolator())
//                .start()
//        }
        showNavigationView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CallStateHolder.viewModel = vm
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        userImageUrl = Preferences.getCustomModelPreference<LoginResponse>(this, LOGIN_DATA)?.payload?.profileImage.toString()
        floatingButtonClick()
        UserDataManager.setHomeLoaded(this, false)
        navigationMenuClickListener()
//        setBottomBarPadding()
        setBottomNavigation()
        fragmentChangeCallback()
        handleActivityBackButton()
        requestCameraAndMicrophonePermissions()
        Handler(Looper.getMainLooper()).post {
            handleIntentNavigation(intent)
        }

    }

    fun setOnlineStatusVisibility(boolean: Boolean){
        if(boolean){
            binding.onlineStatusImage.isVisible = false
        }else{
            binding.onlineStatusImage.isVisible = true
        }
        Log.e("DELETE", "DELETE")
    }

    fun setOnlineStatus(onlineStatus: String) {
        when(onlineStatus){
            "1"->{
                binding.onlineStatusImage.setImageResource(R.drawable.online_status_green)
            }
            "2", "3"->{
                binding.onlineStatusImage.setImageResource(R.drawable.offline_status_red)
            }
            /*"3"->{
                binding.onlineStatusImage.setImageResource(R.drawable.busy_status)
            }*/
        }
    }

    fun updateProfileImage(imageUrl: String){
        userImageUrl = imageUrl
        setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = true)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentNavigation(intent)

    }

    private fun handleIntentNavigation(intent: Intent) {
        val target = intent.getStringExtra("target_fragment")
        Log.i("TAG", "handleIntentNavigation: "+target)
        when (target) {
            "message" -> {
                Log.i("TAG", "handleIntentNavigation chatid: "+intent.getStringExtra("chatId") ?: "")
                UserPreference.CHAT_PUSH_NOTIFICATION_ID = intent.getStringExtra("chat_id") ?: ""
                navController.navigate(R.id.chatListFragment)
            }
            "search" -> navController.navigate(R.id.searchNewFragment)
            "profile" -> navController.navigate(R.id.profileFragment)
            "reels" -> navController.navigate(R.id.reelsFragment)
            "home" -> navController.navigate(R.id.homeNewFragment)
        }
    }
    private fun handleActivityBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestinationId = navController.currentDestination?.id
                setOnlineStatusVisibility(false)
                when (currentDestinationId) {
                    R.id.homeNewFragment -> {
                        Log.e("CCCCC", "CCCCC")
                        finish()
                        showNavigationView()
                    }
                    R.id.searchNewFragment, R.id.chatListFragment -> {
                        popBackToHome()
                        showNavigationView()
                    }
                    R.id.reelsFragment -> {
                        if (UserPreference.navigatedToMyProfile){
//                            UserPreference.navigatedToMyProfile=false
                            navController.popBackStack()
                            showNavigationView()
//                            navigateToProfile(navController.currentDestination?.id, Preferences.getCustomModelPreference<LoginResponse>(this@HomeActivity, LOGIN_DATA)?.payload?.profileImage.toString())
                        }else{
                            popBackToHome()
                        }
                    }
                    R.id.profileFragment -> {
//                        popBackToHome()
                        navController.popBackStack()
                        showNavigationView()
                    }
                    R.id.openAdFragment -> {

                    }
                    else -> {
                        navController.popBackStack()
                        showNavigationView()
                    }
                }
            }
        })
    }

    private fun popBackToHome() {
        val popped = navController.popBackStack(R.id.homeNewFragment, false)
        if (!popped) {
            // HomeFragment not in back stack — navigate to it
            navController.navigate(R.id.homeNewFragment)
        }
        binding.bottomNavigationView.selectedItemId = R.id.home
        showBottomElements()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_home // Ensure this points to the correct layout resource
    }
    private fun floatingButtonClick() {
        binding.floatingbtn.setOnClickListener {
            // Simulate clicking the "Home" item in the BottomNavigationView
            binding.bottomNavigationView.selectedItemId = R.id.reel
        }
    }
    private fun clearBottomBarPadding() {
        binding.bottomAppBar.setPadding(0, 0, 0, 0)
        binding.bottomNavigationView.setPadding(0, 0, 0, 0)

        // Optional: remove listener only if you completely remove the bar
        // ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView, null)
    }

     fun setBottomBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Remove extra padding from BottomAppBar and its children
            binding.bottomAppBar.setPadding(0, 0, 0, 0)
            val offset = if (isGestureNavigation()) systemBars.bottom else 0
            binding.bottomNavigationView.setPadding(0, 0, 0, offset)
            WindowInsetsCompat.CONSUMED
        }
    }
     /*fun fullyResetFloatingButton() {
        val root = findViewById<ConstraintLayout>(R.id.home)
        val fab = findViewById<FloatingActionButton>(R.id.floatingbtn)
        val img = findViewById<AppCompatImageView>(R.id.imgBtn)

        // Temporarily remove both
        root.removeView(fab)
        root.removeView(img)

        // Re-add them after a slight delay to allow layout to settle
        Handler(Looper.getMainLooper()).postDelayed({
            root.addView(fab)
            root.addView(img)
        }, 100)
    }*/
     fun fullyResetFloatingButton() {
         val fab = findViewById<FloatingActionButton>(R.id.floatingbtn)
         val img = findViewById<AppCompatImageView>(R.id.imgBtn)

         if (fab != null && img != null) {
             fab.visibility = View.GONE
             img.visibility = View.GONE

             Handler(Looper.getMainLooper()).postDelayed({
                 fab.visibility = View.VISIBLE
                 img.visibility = View.VISIBLE
             }, 100)
         }
     }



    /*private fun setBottomBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppBar) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            val isKeyboardVisible = imeInsets.bottom > 0

            val bottomPadding = if (!isKeyboardVisible && isGestureNavigation()) {
                systemBarsInsets.bottom
            } else {
                0
            }

            // Apply bottom padding to BottomAppBar itself (not just the BottomNavigationView)
            binding.bottomAppBar.setPadding(0, 0, 0, bottomPadding)
            binding.bottomNavigationView.setPadding(0, 0, 0, 0) // Always 0 to avoid double padding

            insets
        }
    }*/

    /*private fun setBottomBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppBar) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            val isKeyboardVisible = imeInsets.bottom > 0

            if (!isKeyboardVisible && isGestureNavigation()) {
                binding.bottomAppBar.setPadding(0, 0, 0, systemBarsInsets.bottom+ CommonUtils.dpToPx(15))
                binding.bottomNavigationView.setPadding(0, 0, 0, 0) // Always 0 to avoid double padding
            } else {
                binding.bottomAppBar.setPadding(0, 0, 0, CommonUtils.dpToPx(-10))
                binding.bottomNavigationView.setPadding(0, 0, 0, 0) // Always 0 to avoid double padding
            }

            // Apply bottom padding to BottomAppBar itself (not just the BottomNavigationView)


            insets
        }
    }*/
    /*private fun setBottomBarPadding() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment

        if (currentFragment is ChatFragment) {
            // Skip setting insets in ChatFragment
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Remove extra padding from BottomAppBar and its children
            binding.bottomAppBar.setPadding(0, 0, 0, 0)
            val offset = if (isGestureNavigation()) systemBars.bottom else 0
            binding.bottomNavigationView.setPadding(0, 0, 0, offset)
            WindowInsetsCompat.CONSUMED
        }
    }*/




    fun isGestureNavigation(): Boolean {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        return resId > 0 && resources.getInteger(resId) == 2
    }
    private fun setBottomNavigation() {
        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.selectedItemId = R.id.home
    }



    fun setUserProfileImageWithStroke(context: Context, bottomNavView: BottomNavigationView, imageUrl: String, isSelected: Boolean = false) {
        if (imageUrl.isBlank()) {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.profile_icon)
            drawable?.setTint(ContextCompat.getColor(context, if (isSelected) R.color.theme else R.color.white)) // Apply your theme color here
            bottomNavView.menu.findItem(R.id.profile).icon = drawable
            return
        }

        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .placeholder(R.drawable.profile_icon)
            .error(R.drawable.profile_icon)
            .circleCrop()
            .into(object : CustomTarget<Bitmap>(80, 80) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val drawable = getCircularDrawableWithStroke(context, resource, isSelected)
                    bottomNavView.menu.findItem(R.id.profile).icon = drawable
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    fun getCircularDrawableWithStroke(context: Context, bitmap: Bitmap, isSelected: Boolean): Drawable {
        val size = bitmap.width
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val strokeColor = "#B90A66".toColorInt()
        val radius = size / 2f

        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = if (isSelected) strokeColor else Color.GRAY // Change stroke color
            style = Paint.Style.STROKE
            strokeWidth = if (isSelected) 6f else 0f  // Adjust stroke thickness
        }

        if (isSelected) {
            val halfStroke = borderPaint.strokeWidth / 2f
            canvas.drawCircle(radius, radius, radius - halfStroke, paint)
            canvas.drawCircle(radius, radius, radius - halfStroke, borderPaint)
        } else {
            canvas.drawCircle(radius, radius, radius, paint)
        }


        return BitmapDrawable(context.resources, output)
    }

    private fun navigationMenuClickListener() {
        binding.bottomNavigationView.itemIconTintList = null

        setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val currentDestId = navController.currentDestination?.id

            // Reset all to unselected first
            binding.bottomNavigationView.menu.findItem(R.id.home).icon = ContextCompat.getDrawable(this, R.drawable.home_icon)
            binding.bottomNavigationView.menu.findItem(R.id.chat).icon = ContextCompat.getDrawable(this, R.drawable.chat_icon)
            binding.bottomNavigationView.menu.findItem(R.id.search).icon = ContextCompat.getDrawable(this, R.drawable.search_icon)

            when (item.itemId) {
                R.id.home -> {
                    UserDataManager.setHomeLoaded(this, false)
                    UserDataManager.setGetBackToHome(this, false)
                    showNavigationView()
                    if (currentDestId != R.id.homeNewFragment) {
                        navController.navigate(R.id.homeNewFragment)
                    }else{
                        supportFragmentManager.setFragmentResult("home_click", Bundle())
                    }
                    binding.imgBtn.setImageResource(R.drawable.reel_icon_unselected)
                    binding.bottomNavigationView.menu.findItem(R.id.home).icon = ContextCompat.getDrawable(this, R.drawable.home_selected)
                    setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
                    true
                }
                R.id.chat -> {
                    UserDataManager.setGetBackToHome(this, true)
                    showNavigationView()
//                    if (currentDestId != R.id.chatListFragment) {
                        navController.navigate(R.id.chatListFragment)
//                    }
                    binding.imgBtn.setImageResource(R.drawable.reel_icon_unselected)
                    binding.bottomNavigationView.menu.findItem(R.id.chat).icon = ContextCompat.getDrawable(this, R.drawable.chat_selected)
                    setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
                    true
                }
                R.id.reel -> {
                    UserDataManager.setGetBackToHome(this, true)
                    showNavigationView()
//                    if (currentDestId != R.id.reelsFragment) {
                        navController.navigate(R.id.reelsFragment)
//                    }
                    binding.imgBtn.setImageResource(R.drawable.reel_icon_selected)
                    setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
                    true
                }
                R.id.search -> {
                    UserDataManager.setGetBackToHome(this, true)
                    showNavigationView()
                    if (currentDestId != R.id.searchFragment) {
                        navController.navigate(R.id.searchNewFragment)
                    }
                    binding.imgBtn.setImageResource(R.drawable.reel_icon_unselected)
                    binding.bottomNavigationView.menu.findItem(R.id.search).icon = ContextCompat.getDrawable(this, R.drawable.search_selected)
                    setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
                    true
                }
                R.id.profile -> {
                    UserDataManager.setGetBackToHome(this, true)
                    showNavigationView()
                    navigateToProfile(currentDestId, userImageUrl)
                    true
                }
                else -> false
            }
        }

    }

    private fun navigateToProfile(currentDestId: Int?, userImageUrl: String) {
//        if (currentDestId != R.id.profileFragment) {
            navController.navigate(R.id.profileFragment)
//        }
        binding.imgBtn.setImageResource(R.drawable.reel_icon_unselected)
        // Load profile image with stroke
        setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = true)
    }

    fun navigateToHome() {
        navController.navigate(R.id.homeNewFragment)
        binding.imgBtn.setImageResource(R.drawable.reel_icon_unselected)
        binding.bottomNavigationView.menu.findItem(R.id.home).icon = ContextCompat.getDrawable(this, R.drawable.home_selected)
        setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
    }


    private fun fragmentChangeCallback() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.profileFragment, R.id.chatListFragment, R.id.searchNewFragment -> {
                    showNavigationView()
                    setBottomBarPadding()
                }
                R.id.homeNewFragment -> {
                    binding.bottomNavigationView.menu.findItem(R.id.home).icon = ContextCompat.getDrawable(this, R.drawable.home_selected)
                    setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = false)
                    showNavigationView()
                    setBottomBarPadding()
                }
                R.id.reelsFragment -> {
//                    binding.imgBtn.setImageResource(R.drawable.reel_icon_selected)
                    showNavigationView()
                    setBottomBarPadding()
                    if (UserPreference.navigatedToMyProfile){
                        binding.bottomNavigationView.menu.findItem(R.id.home).icon = ContextCompat.getDrawable(this, R.drawable.home_icon)
                        setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = true)
                    }
                }
                R.id.chatFragment, R.id.newStoryFragment, R.id.predefinedChatFragment, R.id.addReelFragment, R.id.editProfileNewFragment, R.id.storyFragment, R.id.secondStoryFragment, R.id.becomeCreatorStatusFragment, R.id.benifitsOfCreatersFragment, R.id.rateUsFragment, R.id.openImageFragment, R.id.changePasswordFragment  -> {
                    clearBottomBarPadding()
                    hideNavigationView()

                }
                else -> {
                    setBottomBarPadding()
                    showNavigationView()
                }
            }
        }
    }

    fun showNavigationView() {
        binding.bottomAppBar.isVisible=true
        binding.floatingbtn.isVisible=true
        binding.imgBtn.isVisible=true
    }

    fun hideNavigationView() {
        binding.bottomAppBar.isVisible=false
        binding.floatingbtn.isVisible=false
        binding.imgBtn.isVisible=false
    }
    fun selectBottomNavTab(@IdRes itemId: Int) {
        if (binding.bottomNavigationView.isVisible){
            binding.bottomNavigationView.selectedItemId = itemId
        }
    }
    fun selectBottomNavTabIcon(@IdRes itemId: Int){
        if (binding.bottomNavigationView.isVisible){
            binding.bottomNavigationView.selectedItemId = itemId
        }
    }
    fun selectProfileTabIcon(){
        binding.bottomNavigationView.menu.findItem(R.id.home).icon = ContextCompat.getDrawable(this, R.drawable.home_icon)
        setUserProfileImageWithStroke(this, binding.bottomNavigationView, userImageUrl, isSelected = true)


    }
    private fun requestCameraAndMicrophonePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) and above
            val requiredPermissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )

            // Bluetooth for calls
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            val missingPermissions = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toTypedArray(),
                    CAMERA_MIC_PERMISSION_REQUEST_CODE
                )
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31–32)
            val requiredPermissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val missingPermissions = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toTypedArray(),
                    CAMERA_MIC_PERMISSION_REQUEST_CODE
                )
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6 (API 23) to Android 11 (API 30)
            val requiredPermissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
                // BLUETOOTH & BLUETOOTH_ADMIN are normal permissions below Android 12,
                // so no runtime request needed
            )

            val missingPermissions = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toTypedArray(),
                    CAMERA_MIC_PERMISSION_REQUEST_CODE
                )
            }
        }

        // 🔹 Overlay permission check (same as your code)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                // All permissions granted
            } else {
                Toast.makeText(this, "Permissions required for video call!", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // Overlay permission granted
            } else {
                Toast.makeText(this, "Permissions required for video call!", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
        }
    }



}