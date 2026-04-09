package com.kyant.backdrop.catalog.onboarding

import androidx.compose.runtime.Composable
import com.kyant.backdrop.catalog.NotificationDeepLink
import com.kyant.backdrop.catalog.linkedin.LinkedInContent

/**
 * Home screen - displays the main LinkedIn-style content.
 * This is shown after onboarding is completed.
 */
@Composable
fun HomeScreen(
    deepLink: NotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    LinkedInContent(
        deepLink = deepLink,
        onDeepLinkConsumed = onDeepLinkConsumed
    )
}
