package com.kyant.backdrop.catalog.network

import java.util.Base64
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeTokenPolicyTest {
    private fun tokenWithExpiry(expiresAtSeconds: Long): String {
        val payload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"exp\":$expiresAtSeconds}".toByteArray())
        return "header.$payload.signature"
    }

    @Test
    fun healthyAccessTokenIsReusedForRealtime() {
        val now = 1_700_000_000_000L
        val token = tokenWithExpiry(now / 1000L + 3600L)

        assertFalse(ApiClient.isJwtExpiringSoon(token, nowMillis = now, refreshSkewMillis = 60_000L))
    }

    @Test
    fun expiringOrMalformedAccessTokenRequestsRefresh() {
        val now = 1_700_000_000_000L
        val token = tokenWithExpiry(now / 1000L + 30L)

        assertTrue(ApiClient.isJwtExpiringSoon(token, nowMillis = now, refreshSkewMillis = 60_000L))
        assertTrue(ApiClient.isJwtExpiringSoon("not-a-jwt", nowMillis = now))
    }
}
