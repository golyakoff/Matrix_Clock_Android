import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    alias(libs.plugins.parcelize)
}

android {
    namespace = "net.agolyakov.tetrisclockble"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.agolyakov.tetrisclockble"
        minSdk = 28     // Android 9
        targetSdk = 36  // Android 16
        versionCode = 29
        versionName = "1.10.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        outputs.all {
            val variantName = name
            val apkOutput = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            apkOutput.outputFileName =
                "tetris-clock-${variantName}-v${versionName}.apk"
        }
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
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
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
    kapt(libs.hilt.compiler)
    implementation(libs.nordic.ble)

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