package com.kyant.backdrop.catalog.network

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
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
            val credentialManager = CredentialManager.create(activity)
            val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
                .ifBlank { activity.getString(R.string.default_web_client_id).trim() }
            if (webClientId.isBlank()) {
                return@withContext GoogleSignInResult.Error("Google Sign-In is not configured.")
            }
            Log.d(TAG, "Starting Google sign-in with configured web client.")
            
            // This is launched from an explicit "Continue with Google" button, so use the
            // Credential Manager button flow rather than the passive bottom-sheet flow.
            val googleSignInOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()
            
            // Create credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleSignInOption)
                .build()
            
            // Get credential - requires Activity context
            val result: GetCredentialResponse = credentialManager.getCredential(
                context = activity,
                request = request
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
}
