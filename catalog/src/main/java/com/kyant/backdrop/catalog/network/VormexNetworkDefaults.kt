package com.kyant.backdrop.catalog.network

import android.os.Build
import com.google.firebase.appcheck.FirebaseAppCheck
import com.kyant.backdrop.catalog.BuildConfig
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val APP_CHECK_HEADER = "X-Firebase-AppCheck"
private const val CLIENT_HEADER = "X-Vormex-Client"
private const val APP_VERSION_HEADER = "X-Vormex-App-Version"
private const val APP_BUILD_HEADER = "X-Vormex-App-Build"

private val vormexUserAgent: String
    get() = "VormexAndroid/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID}; Android ${Build.VERSION.SDK_INT})"

internal fun HttpMessageBuilder.applyVormexClientHeaders() {
    headers.set(HttpHeaders.UserAgent, vormexUserAgent)
    headers.set(CLIENT_HEADER, "android")
    headers.set(APP_VERSION_HEADER, BuildConfig.VERSION_NAME)
    headers.set(APP_BUILD_HEADER, BuildConfig.VERSION_CODE.toString())
}

internal fun HttpClientConfig<*>.installVormexAppCheckInterceptor() {
    install("VormexAppCheck") {
        requestPipeline.intercept(HttpRequestPipeline.State) {
            if (context.headers[APP_CHECK_HEADER].isNullOrBlank()) {
                getAppCheckTokenOrNull()?.takeIf { it.isNotBlank() }?.let { token ->
                    context.headers.set(APP_CHECK_HEADER, token)
                }
            }
            proceed()
        }
    }
}

private suspend fun getAppCheckTokenOrNull(): String? = suspendCancellableCoroutine { continuation ->
    try {
        FirebaseAppCheck.getInstance()
            .getAppCheckToken(false)
            .addOnSuccessListener { token ->
                if (continuation.isActive) {
                    continuation.resume(token.token)
                }
            }
            .addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
            .addOnCanceledListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
    } catch (_: Exception) {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }
}
