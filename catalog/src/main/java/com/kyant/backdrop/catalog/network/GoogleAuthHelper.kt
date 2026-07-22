package com.kyant.backdrop.catalog.network

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for Google Sign-In using Credential Manager
 */
object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    /**
     * Result of Google Sign-In attempt
     */
    sealed class GoogleSignInResult {
        data class Success(val idToken: String, val email: String, val displayName: String?) : GoogleSignInResult()
        data class Error(val message: String) : GoogleSignInResult()
        object Cancelled : GoogleSignInResult()
    }
    
    /**
     * Initiates Google Sign-In flow using Credential Manager
     * IMPORTANT: Must pass an Activity context, not Application context
     * Returns the ID token that should be sent to the backend
     */
    suspend fun signIn(activity: Activity): GoogleSignInResult = withContext(Dispatchers.Main) {
        try {
            if (!hasValidatedInternet(activity)) {
                return@withContext GoogleSignInResult.Error(
                    "No internet connection. Turn on Wi-Fi or mobile data and try Google Sign-In again."
                )
            }

            val credentialManager = CredentialManager.create(activity)
            val serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim()
            val androidClientId = BuildConfig.GOOGLE_ANDROID_CLIENT_ID.trim()
            val buildConfigWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            val googleServicesWebClientId = activity.getString(R.string.default_web_client_id).trim()
            val googleClientId = serverClientId.ifBlank {
                buildConfigWebClientId
            }.ifBlank {
                googleServicesWebClientId
            }
            if (googleClientId.isBlank()) {
                return@withContext GoogleSignInResult.Error("Google Sign-In is not configured. Missing server client ID.")
            }
            Log.d(
                TAG,
                "Starting Google sign-in for ${activity.packageName}. " +
                    "Server client ${redactedClientId(googleClientId)}, " +
                    "Android OAuth client ${redactedClientId(androidClientId)}, " +
                    "google-services default ${redactedClientId(googleServicesWebClientId)}."
            )
            
            // This flow starts from an explicit "Continue with Google" button. Google's
            // button-specific option is important on a fresh install: unlike the generic
            // bottom-sheet option, it can still show the account chooser when global
            // Credential Manager sign-in prompts have not been initialized for this app.
            val googleIdOption = GetSignInWithGoogleOption.Builder(googleClientId)
                .build()
            val result = getCredential(activity, credentialManager, googleIdOption)
                ?: return@withContext GoogleSignInResult.Error(
                    "Google could not return an account for this app. Check the Google OAuth setup for com.vormex.android."
                )
            
            // Extract Google ID token
            val credential = result.credential
            Log.d(TAG, "Credential Manager returned credential type: ${credential.type}")
            
            when {
                isGoogleIdTokenCredentialType(credential.type) -> {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken
                    val email = googleCredential.id
                    val displayName = googleCredential.displayName
                    Log.d(TAG, "Google ID token credential parsed successfully.")
                    GoogleSignInResult.Success(idToken, email, displayName)
                }
                else -> {
                    Log.w(TAG, "Unexpected credential type: ${credential.type}")
                    GoogleSignInResult.Error("Unexpected credential type: ${credential.type}")
                }
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential available.", e)
            GoogleSignInResult.Error("No usable Google account found. Please add or re-authenticate a Google account on this device.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager sign-in failed: ${e.type}", e)
            // Handle specific error types
            when {
                e.type == "android.credentials.GetCredentialException.TYPE_USER_CANCELED" ||
                e.message?.contains("canceled", ignoreCase = true) == true -> {
                    GoogleSignInResult.Cancelled
                }
                e.message?.contains("No credentials available", ignoreCase = true) == true -> {
                    GoogleSignInResult.Error("No usable Google account found. Please add or re-authenticate a Google account on this device.")
                }
                else -> {
                    GoogleSignInResult.Error(e.message ?: "Google Sign-In failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Google sign-in error.", e)
            GoogleSignInResult.Error(e.message ?: "Unknown error during Google Sign-In")
        }
    }

    internal fun isGoogleIdTokenCredentialType(type: String): Boolean =
        type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
            type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL

    private fun redactedClientId(clientId: String): String {
        if (clientId.isBlank()) return "not configured"
        val suffix = clientId.substringAfterLast('-', missingDelimiterValue = clientId)
            .substringBefore(".apps.googleusercontent.com")
        return if (suffix.length <= 8) {
            "ending $suffix"
        } else {
            "ending ${suffix.takeLast(8)}"
        }
    }

    private fun hasValidatedInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun getCredential(
        activity: Activity,
        credentialManager: CredentialManager,
        credentialOption: CredentialOption
    ): GetCredentialResponse? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(credentialOption)
            .build()
        return try {
            credentialManager.getCredential(
                context = activity,
                request = request
            )
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential for ${credentialOption::class.java.simpleName}.", e)
            null
        }
    }
}
