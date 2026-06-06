import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.navigation.safeargs.kotlin)
}

android {
    namespace = "com.app.hihlo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.hihlo"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // resValue("string", "server_client_id", project.findProperty("SERVER_CLIENT_ID")?.toString() ?: "")
        // resValue("string", "google_map_key", project.findProperty("GOOGLE_MAP_KEY")?.toString() ?: "")
        // resValue("string", "facebook_app_id", project.findProperty("FACEBOOK_APP_ID")?.toString() ?: "")
        // resValue("string", "fb_login_protocol_scheme", project.findProperty("FACEBOOK_LOGIN_PROTOCOL_SCHEME")?.toString() ?: "")
        // resValue("string", "facebook_client_token", project.findProperty("FACEBOOK_CLIENT_TOKEN")?.toString() ?: "")
    }

    buildTypes {
        debug {
            // buildConfigField("String", "BASE_URL_API", "\"https://backend.kuberfinancial.com.au/\"")
        }

        release {
            // buildConfigField("String", "BASE_URL_API", "\"https://www.kuberfinancial.com.au/\"")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    lint {
        disable += setOf(
            "IconLauncherShape",
            "LogNotTimber",
            "MergeRootFrame",
            "TypographyEllipsis"
        )

        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Constraint Layout
    implementation(libs.constraint.layout)

    //Glide
    implementation(libs.glide)

    //Coil for SVG Image
    implementation(libs.bundles.coil.all)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase.all)

    //Fragment Ktx
    implementation(libs.fragment.ktx)

    //Viewmodel LiveData
    implementation(libs.bundles.lifecycle.all)

    //Permission
    implementation(libs.android.permission)

    //Rx-Java
    implementation(libs.bundles.rx.java.all)

    //Retrofit
    implementation(libs.bundles.retrofit.all)

    //Okhttp
    implementation(libs.bundles.okhttp.all)

    //Gson
    implementation(libs.gson)

    //Swipe Refresh Layout
    implementation(libs.androidx.swiperefreshlayout)

    //Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //Vide Trimmer
    implementation(libs.video.trimmer)

    //DataSource
    implementation(libs.androidx.media3.datasource)

    //Agora
    implementation(libs.full.sdk)

    //AmazonAws
    implementation(libs.amazon.aws)
    implementation(libs.amazon.aws.core)

    //ExoPlayer
    implementation(libs.bundles.exoplayer.all)

    //Play Service Auth
    implementation(libs.play.services.auth)

    //Crop
    implementation(libs.ucrop)

    //SeekBar
    implementation(libs.waveform.seekbar)

    //Razorpay
    implementation(libs.razorpay.checkout)

    //Scroll Picker
    implementation(libs.scrollpicker)

    //Photo Editor
    implementation(libs.photoeditor)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    //coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

configurations.all {
    resolutionStrategy {
        force("com.razorpay:checkout:1.6.40")
    }
}