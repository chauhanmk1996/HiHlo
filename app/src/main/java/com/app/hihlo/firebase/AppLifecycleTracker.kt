package com.app.hihlo.firebase

import android.app.Activity
import android.app.Application
import android.os.Bundle

object AppLifecycleTracker : Application.ActivityLifecycleCallbacks {
    private var activityCount = 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun isAppInForeground(): Boolean = activityCount > 0

    override fun onActivityStarted(activity: Activity) {
        activityCount++
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
    }

    // Unused methods
    override fun onActivityCreated(a: Activity, s: Bundle?) {}
    override fun onActivityResumed(a: Activity) {}
    override fun onActivityPaused(a: Activity) {}
    override fun onActivitySaveInstanceState(a: Activity, s: Bundle) {}
    override fun onActivityDestroyed(a: Activity) {}
}
