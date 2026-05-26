package com.kyant.backdrop.catalog.deeplink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.NotificationDeepLink
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object VormexDeepLinks {
    const val ACTION_POST_LINK = "post_link"
    const val ACTION_REEL_LINK = "reel_link"
    const val ACTION_GROUP_INVITE_LINK = "group_invite_link"
    const val ACTION_GITHUB_INTEGRATION = "github_integration"

    private val WEB_HOSTS = setOf(
        "vormex.in",
        "www.vormex.in",
        "vormex.com",
        "www.vormex.com"
    )

    fun postUrl(postId: String): String = "https://vormex.in/post/${Uri.encode(postId)}"
    fun groupInviteUrl(inviteCode: String): String = "https://vormex.in/groups/invite/${Uri.encode(inviteCode)}"
    fun reelUrl(
        reelId: String,
        commentId: String? = null,
        parentCommentId: String? = null
    ): String {
        val base = "https://vormex.in/reels/${Uri.encode(reelId)}"
        val query = buildList {
            commentId?.takeIf { it.isNotBlank() }?.let { add("commentId=${Uri.encode(it)}") }
            parentCommentId?.takeIf { it.isNotBlank() }?.let { add("parentCommentId=${Uri.encode(it)}") }
        }.joinToString("&")
        return if (query.isBlank()) base else "$base?$query"
    }

    fun parse(uri: Uri): NotificationDeepLink? {
        extractGitHubStatus(uri)?.let { status ->
            return NotificationDeepLink(
                action = ACTION_GITHUB_INTEGRATION,
                githubStatus = status,
                githubMessage = uri.getQueryParameter("message")
            )
        }
        extractPostId(uri)?.let { postId ->
            return NotificationDeepLink(
                action = ACTION_POST_LINK,
                postId = postId
            )
        }
        extractGroupInviteCode(uri)?.let { inviteCode ->
            return NotificationDeepLink(
                action = ACTION_GROUP_INVITE_LINK,
                groupInviteCode = inviteCode
            )
        }
        val reelId = extractReelId(uri) ?: return null
        return NotificationDeepLink(
            action = ACTION_REEL_LINK,
            reelId = reelId,
            commentId = uri.getQueryParameter("commentId"),
            parentCommentId = uri.getQueryParameter("parentCommentId")
        )
    }

    fun extractPostId(uri: Uri): String? {
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)

        if (scheme == "vormex") {
            return when (host) {
                "post", "posts" -> firstNonBlank(
                    uri.pathSegments.firstOrNull(),
                    uri.getQueryParameter("id"),
                    uri.getQueryParameter("postId")
                )
                else -> null
            }
        }

        if (scheme != "https" && scheme != "http") return null
        if (host !in WEB_HOSTS) return null

        val segments = uri.pathSegments
        val first = segments.firstOrNull()?.lowercase(Locale.US)
        if ((first == "post" || first == "posts") && segments.size >= 2) {
            return segments[1].takeIf { it.isNotBlank() }
        }

        return if (first == "post" || first == "posts") {
            firstNonBlank(uri.getQueryParameter("id"), uri.getQueryParameter("postId"))
        } else {
            null
        }
    }

    fun extractReelId(uri: Uri): String? {
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)

        if (scheme == "vormex") {
            return when (host) {
                "reel", "reels" -> firstNonBlank(
                    uri.pathSegments.firstOrNull(),
                    uri.getQueryParameter("id"),
                    uri.getQueryParameter("reelId")
                )
                else -> null
            }
        }

        if (scheme != "https" && scheme != "http") return null
        if (host !in WEB_HOSTS) return null

        val segments = uri.pathSegments
        val first = segments.firstOrNull()?.lowercase(Locale.US)
        if ((first == "reel" || first == "reels") && segments.size >= 2) {
            return segments[1].takeIf { it.isNotBlank() }
        }

        return if (first == "reel" || first == "reels") {
            firstNonBlank(uri.getQueryParameter("id"), uri.getQueryParameter("reelId"))
        } else {
            null
        }
    }

    fun extractGroupInviteCode(uri: Uri): String? {
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)

        if (scheme == "vormex") {
            return when (host) {
                "group-invite", "groups-invite" -> firstNonBlank(
                    uri.pathSegments.firstOrNull(),
                    uri.getQueryParameter("code"),
                    uri.getQueryParameter("inviteCode")
                )
                else -> null
            }
        }

        if (scheme != "https" && scheme != "http") return null
        if (host !in WEB_HOSTS) return null

        val segments = uri.pathSegments
        if (segments.size >= 3 &&
            segments[0].equals("groups", ignoreCase = true) &&
            segments[1].equals("invite", ignoreCase = true)
        ) {
            return segments[2].takeIf { it.isNotBlank() }
        }

        return null
    }

    fun extractGroupInviteCode(rawUrl: String): String? {
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)
        val segments = uri.rawPath
            ?.split("/")
            ?.filter { it.isNotBlank() }
            ?.map(::decodeUrlPart)
            .orEmpty()
        val query = parseRawQuery(uri.rawQuery)

        if (scheme == "vormex") {
            return when (host) {
                "group-invite", "groups-invite" -> firstNonBlank(
                    segments.firstOrNull(),
                    query["code"],
                    query["inviteCode"]
                )
                else -> null
            }
        }

        if (scheme != "https" && scheme != "http") return null
        if (host !in WEB_HOSTS) return null

        return if (segments.size >= 3 &&
            segments[0].equals("groups", ignoreCase = true) &&
            segments[1].equals("invite", ignoreCase = true)
        ) {
            segments[2].takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    private fun extractGitHubStatus(uri: Uri): String? {
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)
        if (scheme != "https" && scheme != "http") return null
        if (host !in WEB_HOSTS) return null

        val first = uri.pathSegments.firstOrNull()?.lowercase(Locale.US)
        if (first != "profile") return null

        return uri.getQueryParameter("github")
            ?.takeIf { it == "connected" || it == "error" }
    }

    fun openPostUrl(context: Context, postUrl: String): Boolean {
        val uri = runCatching { Uri.parse(postUrl) }.getOrNull() ?: return false
        return openPostUri(context, uri)
    }

    fun openPostUri(context: Context, uri: Uri): Boolean {
        if (extractPostId(uri).isNullOrBlank()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
        return true
    }

    fun openReelUrl(context: Context, reelUrl: String): Boolean {
        val uri = runCatching { Uri.parse(reelUrl) }.getOrNull() ?: return false
        return openReelUri(context, uri)
    }

    fun openReelUri(context: Context, uri: Uri): Boolean {
        if (extractReelId(uri).isNullOrBlank()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
        return true
    }

    private fun parseRawQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery
            .split("&")
            .mapNotNull { pair ->
                val index = pair.indexOf("=")
                when {
                    index <= 0 -> null
                    else -> decodeUrlPart(pair.substring(0, index)) to decodeUrlPart(pair.substring(index + 1))
                }
            }
            .toMap()
    }

    private fun decodeUrlPart(value: String): String {
        return runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }.getOrDefault(value)
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
