plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

fun secret(name: String): String? = providers.gradleProperty(name).orNull ?: System.getenv(name)

val releaseStoreFilePath = secret("VORMEX_RELEASE_STORE_FILE")
val releaseStorePassword = secret("VORMEX_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = secret("VORMEX_RELEASE_KEY_ALIAS")
val releaseKeyPassword = secret("VORMEX_RELEASE_KEY_PASSWORD")
val releaseApiBaseUrl = secret("VORMEX_RELEASE_API_BASE_URL").orEmpty()
val releaseSocketBaseUrl = secret("VORMEX_RELEASE_SOCKET_BASE_URL").orEmpty()
// Credential Manager asks for the server client ID: this is the OAuth client
// whose audience the backend verifies. The Android OAuth client must still be
// registered in the same Google project for com.vormex.android and the app's
// signing fingerprints, but it is not passed as the ID-token audience.
val googleServerClientId = secret("VORMEX_GOOGLE_SERVER_CLIENT_ID")
    ?: secret("VORMEX_GOOGLE_WEB_CLIENT_ID")
    ?: secret("GOOGLE_CLIENT_ID_WEB")
    ?: "562328294412-3qt2hj14q8c43nhjimqevhdopecvp04b.apps.googleusercontent.com"
val googleWebClientId = googleServerClientId
val googleAndroidClientId = secret("VORMEX_GOOGLE_ANDROID_CLIENT_ID")
    ?: secret("GOOGLE_CLIENT_ID_ANDROID")
    ?: "562328294412-sfil52dp4f7mapttri74hs9t445ierpd.apps.googleusercontent.com"
// Debug builds use the local backend by default. For physical devices, run
// adb reverse tcp:5000 tcp:5000 so the phone's 127.0.0.1 points at this machine.
// Keep the Render URLs here for quick hosted-backend overrides when needed.
val renderDebugApiBaseUrl = "https://vormex-backend.onrender.com/api"
val renderDebugSocketBaseUrl = "https://vormex-backend.onrender.com"
val localDebugApiBaseUrl = "http://127.0.0.1:5000/api"
val localDebugSocketBaseUrl = "http://127.0.0.1:5000"
val debugApiBaseUrl = secret("VORMEX_DEBUG_API_BASE_URL") ?: localDebugApiBaseUrl
val debugSocketBaseUrl = secret("VORMEX_DEBUG_SOCKET_BASE_URL") ?: localDebugSocketBaseUrl
val adMobApplicationId = secret("VORMEX_ADMOB_APPLICATION_ID")
    ?: "ca-app-pub-3940256099942544~3347511713"
val adMobNativeFeedAdUnitId = secret("VORMEX_ADMOB_NATIVE_FEED_AD_UNIT_ID")
    ?: "ca-app-pub-3940256099942544/2247696110"
val adMobNativeReelsAdUnitId = secret("VORMEX_ADMOB_NATIVE_REELS_AD_UNIT_ID")
    ?: "ca-app-pub-3940256099942544/2247696110"
val adsEnabled = secret("VORMEX_ADS_ENABLED")?.toBooleanStrictOrNull() ?: true
val debugAdMobApplicationId = "ca-app-pub-3940256099942544~3347511713"
val debugAdMobNativeAdUnitId = "ca-app-pub-3940256099942544/2247696110"
val debugUseRealAds = secret("VORMEX_DEBUG_USE_REAL_ADS")?.toBooleanStrictOrNull() ?: false
val debugEffectiveAdMobApplicationId = if (debugUseRealAds) adMobApplicationId else debugAdMobApplicationId
val debugEffectiveNativeFeedAdUnitId = if (debugUseRealAds) adMobNativeFeedAdUnitId else debugAdMobNativeAdUnitId
val debugEffectiveNativeReelsAdUnitId = if (debugUseRealAds) adMobNativeReelsAdUnitId else debugAdMobNativeAdUnitId
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.kyant.backdrop.catalog"
    compileSdk {
        version = release(36)
    }
    buildToolsVersion = "36.1.0"

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    defaultConfig {
        applicationId = "com.vormex.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.6"
        androidResources.localeFilters += arrayOf("en")
        buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
        buildConfigField("String", "SOCKET_BASE_URL", "\"$releaseSocketBaseUrl\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", "\"$googleAndroidClientId\"")
        buildConfigField("String", "ADMOB_NATIVE_FEED_AD_UNIT_ID", "\"$adMobNativeFeedAdUnitId\"")
        buildConfigField("String", "ADMOB_NATIVE_REELS_AD_UNIT_ID", "\"$adMobNativeReelsAdUnitId\"")
        buildConfigField("Boolean", "ADS_ENABLED", adsEnabled.toString())
        // Release must not allow HTTP; debug needs cleartext for local dev (e.g. adb reverse to localhost).
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        manifestPlaceholders["adMobApplicationId"] = adMobApplicationId
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            buildConfigField("String", "SOCKET_BASE_URL", "\"$debugSocketBaseUrl\"")
            buildConfigField("String", "ADMOB_NATIVE_FEED_AD_UNIT_ID", "\"$debugEffectiveNativeFeedAdUnitId\"")
            buildConfigField("String", "ADMOB_NATIVE_REELS_AD_UNIT_ID", "\"$debugEffectiveNativeReelsAdUnitId\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            manifestPlaceholders["adMobApplicationId"] = debugEffectiveAdMobApplicationId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            vcsInfo.include = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    packaging {
        resources {
            excludes += arrayOf(
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "kotlin/**",
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt"
            )
        }
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    lint {
        checkReleaseBuilds = true
        abortOnError = true
    }
}

gradle.taskGraph.whenReady {
    val isReleaseTask = allTasks.any { task ->
        task.path.startsWith(":catalog:") && task.name.contains("Release")
    }
    if (isReleaseTask && (releaseApiBaseUrl.isBlank() || releaseSocketBaseUrl.isBlank())) {
        throw GradleException(
            "Release builds require VORMEX_RELEASE_API_BASE_URL and VORMEX_RELEASE_SOCKET_BASE_URL."
        )
    }
    if (isReleaseTask && !hasReleaseSigning) {
        throw GradleException(
            "Release builds require VORMEX_RELEASE_STORE_FILE, VORMEX_RELEASE_STORE_PASSWORD, " +
                "VORMEX_RELEASE_KEY_ALIAS, and VORMEX_RELEASE_KEY_PASSWORD."
        )
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xlambdas=class"
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material.ripple)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kyant.shapes)
    implementation(project(":backdrop"))
    
    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Network
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // DataStore for token storage
    implementation(libs.androidx.datastore.preferences)
    
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    
    // Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    
    // Google Play Services Location
    implementation(libs.google.play.services.location)

    // Ads
    implementation(libs.google.play.services.ads)
    implementation(libs.google.user.messaging.platform)

    // Payments
    implementation(libs.google.play.billing)

    // Free/open map rendering for Nearby
    implementation(libs.osmdroid.android)
    
    // Image Loading
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-gif:2.6.0")

    // Lottie (e.g. streak fire on profile)
    implementation(libs.lottie.compose)
    implementation(libs.zxing.core)

    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.2")
    implementation("com.google.android.gms:play-services-tasks:17.2.1")

    // Room cache
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Media3 (ExoPlayer) for video playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource)
    implementation(libs.media3.database)
    implementation(libs.media3.ui)

    // Socket.IO for real-time chat
    implementation(libs.socketio.client) {
        exclude(group = "org.json", module = "json")
    }
    
    // Firebase (Push Notifications)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // Gemini Nano / on-device GenAI
    implementation(libs.mlkit.genai.prompt)

    testImplementation("junit:junit:4.13.2")
}
