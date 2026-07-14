package com.kyant.backdrop.catalog.network

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthHelperTest {
    @Test
    fun `accepts both Google ID token credential variants`() {
        assertTrue(
            GoogleAuthHelper.isGoogleIdTokenCredentialType(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            )
        )
        assertTrue(
            GoogleAuthHelper.isGoogleIdTokenCredentialType(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
            )
        )
        assertFalse(GoogleAuthHelper.isGoogleIdTokenCredentialType("password"))
    }
}
