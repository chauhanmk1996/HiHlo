package com.app.hihlo.ui.splash.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.hihlo.R
import com.app.hihlo.databinding.ActivityMainBinding
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.preferences.IS_LOGIN
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.signup.activity.SignupFlowActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fixNavigationBarForDarkMode()
        delayTime()
    }

    private fun delayTime() {
        lifecycleScope.launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                Log.e(
                    "TAG",
                    "delayTime: ${
                        Preferences.getCustomModelPreference<LoginResponse>(
                            this@MainActivity,
                            LOGIN_DATA
                        )?.payload
                    }",
                )
                if (Preferences.getStringPreference(this@MainActivity, IS_LOGIN) == "2") {
                    if (Preferences.getCustomModelPreference<LoginResponse>(
                            this@MainActivity,
                            LOGIN_DATA
                        )?.payload?.city.isNullOrBlank()
                        || Preferences.getCustomModelPreference<LoginResponse>(
                            this@MainActivity,
                            LOGIN_DATA
                        )?.payload?.profileImage.isNullOrEmpty()
                    ) {
                        startActivity(Intent(this@MainActivity, SignupFlowActivity::class.java))
                    } else {
                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    }
                } else {
                    startActivity(Intent(this@MainActivity, SignupFlowActivity::class.java))
                }
                finish()
            }
        }
    }

    private fun fixNavigationBarForDarkMode() {
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            flags = if (isDark) {
                // Remove light nav bar flag so icons become white
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                // Add light nav bar flag so icons become dark
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            decorView.systemUiVisibility = flags
        }
    }
}