package com.app.hihlo.base

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.app.hihlo.BR
import com.app.hihlo.Global
import com.app.hihlo.R
import com.app.hihlo.utils.*
import com.permissionx.guolindev.PermissionX

abstract class BaseNewActivity<T : ViewDataBinding, V : BaseViewModel>(
    @LayoutRes private val layoutId: Int,
) : AppCompatActivity() {

    private lateinit var mViewDataBinding: T
    private var doubleBackToExitPressedOnce: Boolean = false
    lateinit var sharedPreference: SharedPreferenceUtil
    private var progressDialog: ProgressDialog? = null

    fun getViewDataBinding(): T {
        return mViewDataBinding
    }

    abstract fun initViews(viewBinding: T)
    abstract fun getBindingVariable(): Int
    abstract fun getViewModel(): V

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        mViewDataBinding = DataBindingUtil.setContentView(this, layoutId)
        mViewDataBinding.setVariable(BR.viewModel, getViewModel())
        mViewDataBinding.lifecycleOwner = this

        sharedPreference = SharedPreferenceUtil.getInstance(this)
        Global.setActivity(this)
        initViews(mViewDataBinding)
    }

    fun applyWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        Global.setActivity(this)
    }

    fun showProgress(visible: Boolean) {
        if (visible) {
            progressDialog?.dismiss()
            progressDialog = ProgressDialog(this)
            progressDialog?.setCancelable(false)
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
        }
    }

    fun showToast(message: String?) {
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val layout: View = layoutInflater.inflate(R.layout.long_toast, parent, false)

        val text = layout.findViewById<TextView>(R.id.tv_toast)
        text.text = message

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    fun showLongToast(message: String) {
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val layout: View = layoutInflater.inflate(R.layout.long_toast, parent, false)

        val text = layout.findViewById<TextView>(R.id.tv_toast)
        text.text = message

        Toast(this).apply {
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }

    fun onBackPressedMethod() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackClick()
                }
            })
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                    onBackClick()
                }
            } else {
                onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        onBackClick()
                    }
                })
            }
        }
    }

    fun onBackClick() {
        if (doubleBackToExitPressedOnce) {
            doubleBackToExitPressedOnce = false
            finishAffinity()
        } else {
            showToast(getString(R.string.please_click_back_again_to_exit))
            doubleBackToExitPressedOnce = true
            Handler(Looper.myLooper()!!).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 3000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        showProgress(false)
    }

    fun enableDeviceGPS(context: Context) {
        val alertDialog = AlertDialog.Builder(context).setMessage(getString(R.string.gps_enable))
            .setPositiveButton(getString(R.string.setting)) { dialog, which ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                showToast(getString(R.string.need_location_permission))
            }
        alertDialog.show()
    }

    fun fullScreenLayout() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionX.init(this).permissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            ).onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.our_apps_core_functionality_are_based_on_these_permissions),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }.request { allGranted, grantedList, deniedList ->
            }
        }
    }

    fun checkCameraStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED
            }

            Build.VERSION.SDK_INT == Build.VERSION_CODES.S -> {
                // Android 12 (API 31)
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            }

            else -> {
                // Android 11 and below
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun checkMainPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkDeviceLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun provideLocationPermission() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.our_apps_core_functionality_are_based_on_these_permissions),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .request { allGranted, grantedList, deniedList ->
            }
    }
}