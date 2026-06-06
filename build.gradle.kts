// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.navigation.safeargs.kotlin) apply false
}