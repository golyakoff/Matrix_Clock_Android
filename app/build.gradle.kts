import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Hilt runs on KSP, not kapt, and Kotlin is AGP 9's built-in one (no kotlin-android plugin): kapt's
// Java stub generation was the step that ran the ~4 GB dev box out of memory
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// The app version: bump both together with a new section of CHANGELOG.md. A beta is "X.Y.Z-betaN"
// (its tag vX.Y.Z-betaN is published as a pre-release, see .github/workflows/release.yml)
val appVersionCode = 36
val appVersionName = "2.1.0-beta2"

// APK name: tetris-clock-vX.Y.Z-<build type>.apk
base {
    archivesName = "tetris-clock-v$appVersionName"
}

android {
    namespace = "net.agolyakov.tetrisclockble"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.agolyakov.tetrisclockble"
        minSdk = 28     // Android 9
        targetSdk = 36  // Android 16
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.material3)
    implementation(libs.material3.window)
    implementation(libs.material.icons.extended)
    implementation(libs.bundles.networking)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.appcompat)
    ksp(libs.hilt.compiler)
    implementation(libs.nordic.ble)
    implementation(libs.nordic.ble.ktx)

    // Coil for rendering animated GIF previews of the splash animations
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}