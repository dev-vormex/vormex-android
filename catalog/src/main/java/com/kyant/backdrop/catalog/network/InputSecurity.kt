package com.kyant.backdrop.catalog.network

import java.net.IDN
import java.net.URI
import java.util.Locale

object InputSecurity {
    private val controlChars = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")
    private val zeroWidthChars = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val activeMarkup = listOf(
        Regex("<\\s*/?\\s*(script|iframe|object|embed|link|meta|style|svg|math|form|input|button|video|audio|source|base)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bon[a-z]{3,}\\s*=", RegexOption.IGNORE_CASE),
        Regex("\\b(javascript|vbscript)\\s*:", RegexOption.IGNORE_CASE),
        Regex("\\bdata\\s*:\\s*text/html", RegexOption.IGNORE_CASE),
        Regex("\\bexpression\\s*\\(", RegexOption.IGNORE_CASE)
    )
    private val structuralInjection = listOf(
        Regex("\\bunion\\s+select\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(or|and)\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?", RegexOption.IGNORE_CASE),
        Regex(";\\s*(drop|delete|insert|update|alter|create|truncate)\\b", RegexOption.IGNORE_CASE),
        Regex("(--|/\\*|\\*/)"),
        Regex("[`$|;&<>]"),
        Regex("\\$\\s*\\(|\\|\\||&&")
    )
    private val promptInjection = listOf(
        Regex("\\b(ignore|disregard|forget|override)\\b.{0,80}\\b(previous|prior|above|system|developer|instruction|rules?)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(reveal|show|print|dump|exfiltrate|leak)\\b.{0,80}\\b(system|developer|hidden|secret|prompt|instructions?|messages?)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(jailbreak|prompt\\s*injection|developer\\s*mode|dan\\s*mode)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(new|updated)\\s+(system|developer)\\s+(prompt|message|instructions?)\\b", RegexOption.IGNORE_CASE),
        Regex("</?(system|developer|assistant|tool|function)\\b", RegexOption.IGNORE_CASE)
    )

    private val safeIdentifier = Regex("^[A-Za-z0-9@._:-]{1,160}$")
    private val safePaginationCursor = Regex("^[A-Za-z0-9._:-]{1,512}$")
    private val safeFileName = Regex("^[A-Za-z0-9._ ()@-]{1,180}$")
    private val emailPattern = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    private val allowedImageMimes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
    private val allowedVideoMimes = setOf("video/mp4", "video/webm", "video/quicktime")
    private val allowedAudioMimes = setOf("audio/mpeg", "audio/mp4", "audio/webm", "audio/wav", "audio/x-wav")
    private val allowedDocuments = setOf("application/pdf", "text/plain")

    class InputValidationException(message: String) : IllegalArgumentException(message)

    fun text(
        value: String?,
        label: String,
        maxLength: Int,
        allowBlank: Boolean = false,
        allowPromptInjection: Boolean = true
    ): String {
        val cleaned = (value ?: "").replace(zeroWidthChars, "").trim()
        requireValid(!controlChars.containsMatchIn(cleaned), "$label contains control characters")
        requireValid(allowBlank || cleaned.isNotBlank(), "$label is required")
        requireValid(cleaned.length <= maxLength, "$label must be $maxLength characters or less")
        requireValid(activeMarkup.none { it.containsMatchIn(cleaned) }, "$label contains unsafe HTML or script content")
        if (!allowPromptInjection) {
            requireValid(promptInjection.none { it.containsMatchIn(cleaned) }, "$label contains unsafe prompt instructions")
        }
        return cleaned
    }

    fun optionalText(value: String?, label: String, maxLength: Int): String? {
        val cleaned = text(value ?: return null, label, maxLength, allowBlank = true)
        return cleaned.ifBlank { null }
    }

    fun identifier(value: String?, label: String = "identifier"): String {
        val cleaned = text(value, label, 160, allowBlank = false)
        requireValid(safeIdentifier.matches(cleaned), "$label has invalid characters")
        requireValid(structuralInjection.none { it.containsMatchIn(cleaned) }, "$label contains unsafe injection markers")
        return cleaned
    }

    fun optionalIdentifier(value: String?, label: String = "identifier"): String? {
        return value?.takeIf { it.isNotBlank() }?.let { identifier(it, label) }
    }

    fun paginationCursor(value: String?, label: String = "cursor"): String {
        val cleaned = text(value, label, 512, allowBlank = false)
        requireValid(safePaginationCursor.matches(cleaned), "$label has invalid characters")
        return cleaned
    }

    fun optionalPaginationCursor(value: String?, label: String = "cursor"): String? {
        return value?.takeIf { it.isNotBlank() }?.let { paginationCursor(it, label) }
    }

    fun email(value: String?, label: String = "email"): String {
        val cleaned = text(value, label, 254).lowercase(Locale.US)
        requireValid(emailPattern.matches(cleaned), "$label must be a valid email address")
        return cleaned
    }

    fun boundedInt(value: Int, label: String, min: Int, max: Int): Int {
        requireValid(value in min..max, "$label must be between $min and $max")
        return value
    }

    fun boundedLong(value: Long, label: String, min: Long, max: Long): Long {
        requireValid(value in min..max, "$label must be between $min and $max")
        return value
    }

    fun enumValue(value: String, allowed: Set<String>, label: String): String {
        val cleaned = text(value, label, 60)
        val normalized = cleaned.uppercase(Locale.US)
        requireValid(normalized in allowed, "$label is invalid")
        return normalized
    }

    fun url(value: String?, label: String, allowBlank: Boolean = false): String {
        val cleaned = text(value, label, 2_000, allowBlank = allowBlank)
        if (allowBlank && cleaned.isBlank()) return cleaned
        val normalized = if ("://" in cleaned) cleaned else "https://$cleaned"
        val uri = try {
            URI(normalized)
        } catch (_: Exception) {
            throw InputValidationException("$label must be a valid URL")
        }
        val scheme = uri.scheme?.lowercase(Locale.US)
        requireValid(scheme == "http" || scheme == "https", "$label must use http or https")
        requireValid(uri.userInfo == null, "$label must not include credentials")
        val host = uri.host ?: throw InputValidationException("$label must include a host")
        requireValid(isSafeHost(host), "$label host is not allowed")
        return cleaned
    }

    fun prompt(value: String?, label: String, maxLength: Int): String {
        return text(value, label, maxLength, allowPromptInjection = false)
    }

    fun fileName(value: String?, fallback: String): String {
        val base = (value ?: fallback).substringAfterLast('/').substringAfterLast('\\').ifBlank { fallback }
        requireValid(!base.contains("..") && safeFileName.matches(base), "File name is unsafe")
        return base
    }

    fun uploadBytes(bytes: ByteArray, label: String, maxBytes: Int): ByteArray {
        requireValid(bytes.isNotEmpty(), "$label is empty")
        requireValid(bytes.size <= maxBytes, "$label exceeds the allowed size")
        return bytes
    }

    fun imageMime(mimeType: String): String = mimeType.lowercase(Locale.US).also {
        requireValid(it in allowedImageMimes, "Unsupported image type")
    }

    fun videoMime(mimeType: String): String = mimeType.lowercase(Locale.US).also {
        requireValid(it in allowedVideoMimes, "Unsupported video type")
    }

    fun chatMime(mimeType: String): String = mimeType.lowercase(Locale.US).also {
        requireValid(it in allowedImageMimes || it in allowedVideoMimes || it in allowedAudioMimes || it in allowedDocuments, "Unsupported attachment type")
    }

    fun voiceMime(mimeType: String): String = mimeType.lowercase(Locale.US).also {
        requireValid(it in allowedAudioMimes, "Unsupported audio type")
    }

    fun sanitizeList(values: List<String>, label: String, maxItems: Int, itemMaxLength: Int): List<String> {
        requireValid(values.size <= maxItems, "$label has too many items")
        return values
            .map { text(it, label, itemMaxLength, allowBlank = true) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }

    fun failure(message: String): Result<Nothing> = Result.failure(InputValidationException(message))

    private fun isSafeHost(host: String): Boolean {
        val asciiHost = try {
            IDN.toASCII(host).lowercase(Locale.US).trimEnd('.')
        } catch (_: Exception) {
            return false
        }
        if (
            asciiHost == "localhost" ||
            asciiHost.endsWith(".localhost") ||
            asciiHost.endsWith(".local") ||
            asciiHost.endsWith(".internal")
        ) return false

        val parts = asciiHost.split(".").map { it.toIntOrNull() }
        if (parts.size == 4 && parts.all { part -> part?.let { it in 0..255 } == true }) {
            val a = parts[0] ?: return false
            val b = parts[1] ?: return false
            return !(
                a == 0 ||
                    a == 10 ||
                    a == 127 ||
                    (a == 100 && b in 64..127) ||
                    (a == 169 && b == 254) ||
                    (a == 172 && b in 16..31) ||
                    (a == 192 && b == 168) ||
                    a >= 224
                )
        }
        return true
    }

    private fun requireValid(condition: Boolean, message: String) {
        if (!condition) throw InputValidationException(message)
    }
}
