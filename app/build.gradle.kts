plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
}

android {
    namespace = "com.app.hihlo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.hihlo"
        minSdk = 29
        targetSdk = 36
        versionCode = 8
        versionName = "1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        lint {
            baseline = file("lint-baseline.xml")
        }
    }

    buildTypes {
        /*release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }*/
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        dataBinding = true
    }
    android {
        packaging {
            resources {
                excludes += setOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/DEPENDENCIES",
                    "META-INF/gradle/incremental.annotation.processors"
                )
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a") // remove x86 unless needed
            isUniversalApk = true // don't build a fat APK
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.media3.datasource)

    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.androidx.swiperefreshlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    //glide
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation("io.coil-kt:coil:2.7.0")

    //Hilt - Dagger
    implementation(libs.hilt.android.compiler)
    implementation(libs.hilt.android)



    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")


    implementation("com.amazonaws:aws-android-sdk-s3:2.72.0")
    implementation("com.amazonaws:aws-android-sdk-core:2.72.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-transformer:1.3.1")

    implementation(libs.android.video.trimmer)

    implementation(libs.full.sdk)

    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("com.github.yalantis:ucrop:2.2.10")

    //    wave form library
    implementation("com.github.massoudss:waveformSeekBar:5.0.2")

    implementation("com.razorpay:checkout:1.6.40")

    //wheel picker
    implementation("com.github.tomeees:scrollpicker:1.7.5")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.burhanrashid52:photoeditor:3.1.0")

    //Google SignIn
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.identity)

}
configurations.all {
    resolutionStrategy {
        force("com.razorpay:checkout:1.6.40")
    }
}