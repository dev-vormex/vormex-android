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
val googleWebClientId = secret("VORMEX_GOOGLE_WEB_CLIENT_ID")
    ?: "562328294412-3qt2hj14q8c43nhjimqevhdopecvp04b.apps.googleusercontent.com"
// Physical devices and emulators can reach the host machine through adb reverse on 127.0.0.1.
// If you prefer the emulator-only host bridge, override with VORMEX_DEBUG_* = http://10.0.2.2:5000.
val localDebugApiBaseUrl = "http://127.0.0.1:5000/api"
val localDebugSocketBaseUrl = "http://127.0.0.1:5000"
val debugApiBaseUrl = secret("VORMEX_DEBUG_API_BASE_URL") ?: localDebugApiBaseUrl
val debugSocketBaseUrl = secret("VORMEX_DEBUG_SOCKET_BASE_URL") ?: localDebugSocketBaseUrl
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
        versionCode = 6
        versionName = "1.0.4"
        androidResources.localeFilters += arrayOf("en")
        buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
        buildConfigField("String", "SOCKET_BASE_URL", "\"$releaseSocketBaseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        // Release must not allow HTTP; debug needs cleartext for local dev (e.g. adb reverse to localhost).
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            buildConfigField("String", "SOCKET_BASE_URL", "\"$debugSocketBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
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
    implementation(libs.androidx.splashscreen)
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
