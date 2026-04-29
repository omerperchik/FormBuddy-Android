plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
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

    // Optional cloud-Gemini API key. Read from gradle.properties or env so the key
    // never ends up in source control. Empty value triggers the heuristic-only path.
    val geminiKey = (project.findProperty("GEMINI_API_KEY") as String?)
        ?: System.getenv("GEMINI_API_KEY")
        ?: ""

    // Release signing — env-driven so the keystore + passwords stay out of source.
    // Set FORMBUDDY_KEYSTORE / FORMBUDDY_KEYSTORE_PASSWORD / FORMBUDDY_KEY_ALIAS
    // / FORMBUDDY_KEY_PASSWORD (or matching gradle.properties) to enable signing.
    // Without them, the release build is unsigned (debug keystore is still used
    // when building locally for testing).
    val keystoreFile = (project.findProperty("FORMBUDDY_KEYSTORE") as String?)
        ?: System.getenv("FORMBUDDY_KEYSTORE")
    val keystorePassword = (project.findProperty("FORMBUDDY_KEYSTORE_PASSWORD") as String?)
        ?: System.getenv("FORMBUDDY_KEYSTORE_PASSWORD")
    val keyAlias = (project.findProperty("FORMBUDDY_KEY_ALIAS") as String?)
        ?: System.getenv("FORMBUDDY_KEY_ALIAS")
    val keyPassword = (project.findProperty("FORMBUDDY_KEY_PASSWORD") as String?)
        ?: System.getenv("FORMBUDDY_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    // Bundle config: per-language + per-density + per-ABI splits so Play
    // delivers the smallest possible APK to each device.
    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    buildTypes {
        debug {
            // Faster debug builds
            isMinifyEnabled = false
            isShrinkResources = false
            // Enable StrictMode in debug
            buildConfigField("boolean", "STRICT_MODE", "true")
            buildConfigField("String", "GEMINI_API_KEY", "\"${geminiKey}\"")
            // Don't upload Crashlytics mappings on debug
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "STRICT_MODE", "false")
            buildConfigField("String", "GEMINI_API_KEY", "\"${geminiKey}\"")
            // Sign release builds when a keystore has been provided.
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }

        }
    }

    // Global Java-resource packaging rules. Drive + Auth deps ship duplicate
    // META-INF entries; the AGP merger can't pick a winner without help.
    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "META-INF/{AL2.0,LGPL2.1}",
                "META-INF/io.netty.versions.properties",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
            // Native-lib pickFirst so duplicated SQLCipher / mlkit libs don't fail packaging.
            pickFirsts += setOf(
                "lib/arm64-v8a/libsqlcipher.so",
                "lib/armeabi-v7a/libsqlcipher.so",
                "lib/x86_64/libsqlcipher.so",
                "lib/x86/libsqlcipher.so"
            )
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
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation("com.google.guava:guava:33.0.0-android")

    // ML Kit
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.document.scanner)

    // WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Play In-App Review + Integrity
    implementation(libs.play.review)
    implementation(libs.play.integrity)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Billing
    implementation(libs.play.billing)

    // Generative AI (Gemini Nano)
    implementation(libs.generative.ai)
    // On-device AICore SDK (real Gemini Nano on Pixel 8+/S24+).
    implementation(libs.aicore)

    // QR generation (share-out + referral).
    implementation(libs.zxing.core)

    // Drive auto-export (Pro tier).
    implementation(libs.google.signin)
    implementation(libs.google.api.client.android)
    implementation(libs.google.drive.api)

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
