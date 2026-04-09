package com.kyant.backdrop.catalog.network

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for Google Sign-In using Credential Manager
 */
object GoogleAuthHelper {
    // Web Client ID from Google Cloud Console (NOT the Android Client ID)
    // The Credential Manager requires the Web Client ID for server-side verification
    private const val WEB_CLIENT_ID = "562328294412-3qt2hj14q8c43nhjimqevhdopecvp04b.apps.googleusercontent.com"
    
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
            
            // Configure Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Allow any Google account
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false) // Let user choose account
                .build()
            
            // Create credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            // Get credential - requires Activity context
            val result: GetCredentialResponse = credentialManager.getCredential(
                context = activity,
                request = request
            )
            
            // Extract Google ID token
            val credential = result.credential
            
            when (credential.type) {
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken
                    val email = googleCredential.id
                    val displayName = googleCredential.displayName
                    GoogleSignInResult.Success(idToken, email, displayName)
                }
                else -> {
                    GoogleSignInResult.Error("Unexpected credential type: ${credential.type}")
                }
            }
        } catch (e: GetCredentialException) {
            // Handle specific error types
            when {
                e.type == "android.credentials.GetCredentialException.TYPE_USER_CANCELED" ||
                e.message?.contains("canceled", ignoreCase = true) == true -> {
                    GoogleSignInResult.Cancelled
                }
                e.message?.contains("No credentials available", ignoreCase = true) == true -> {
                    GoogleSignInResult.Error("No Google accounts found. Please add a Google account to your device.")
                }
                else -> {
                    GoogleSignInResult.Error(e.message ?: "Google Sign-In failed")
                }
            }
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.message ?: "Unknown error during Google Sign-In")
        }
    }
}
