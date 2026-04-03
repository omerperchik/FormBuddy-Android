plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.formbuddy.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.formbuddy.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export for migration validation
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.generateKotlin", "true")
        }

        // ABI splits: ship only the architectures users actually have (~60% APK reduction for native libs)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // Vector drawables compat for pre-API 24
        vectorDrawables.useSupportLibrary = true
    }

    // Per-ABI APK splits (smallest possible APK per device)
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            // Faster debug builds
            isMinifyEnabled = false
            isShrinkResources = false
            // Enable StrictMode in debug
            buildConfigField("boolean", "STRICT_MODE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "STRICT_MODE", "false")

            // Enable R8 full mode for maximum shrinking
            packaging {
                resources {
                    excludes += setOf(
                        "META-INF/LICENSE*",
                        "META-INF/NOTICE*",
                        "META-INF/DEPENDENCIES",
                        "META-INF/*.kotlin_module",
                        "META-INF/versions/**",
                        "DebugProbesKt.bin",
                        "kotlin-tooling-metadata.json",
                        "**/*.proto",
                        "**/*.properties"
                    )
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            // Enable Compose compiler metrics for performance debugging
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Lint performance
    lint {
        abortOnError = false
        checkDependencies = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room + SQLCipher
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher)

    // Security
    implementation(libs.security.crypto)
    implementation(libs.androidx.biometric)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation("com.google.guava:guava:33.0.0-android")

    // ML Kit
    implementation(libs.mlkit.text.recognition)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.config)

    // Billing
    implementation(libs.play.billing)

    // Generative AI (Gemini Nano)
    implementation(libs.generative.ai)

    // Image loading
    implementation(libs.coil.compose)

    // Animations
    implementation(libs.lottie.compose)

    // JSON - Moshi is faster than Gson but keeping Gson for compat
    implementation(libs.gson)

    // PDF
    implementation(libs.pdfbox.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Baseline Profiles (startup optimization)
    implementation(libs.androidx.profileinstaller)
}
