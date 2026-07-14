package com.kyant.backdrop.catalog.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToInt

const val CHAT_ATTACHMENT_MAX_BYTES = 25 * 1024 * 1024
const val CHAT_VIDEO_MAX_BYTES = 150 * 1024 * 1024
const val CHAT_VIDEO_MAX_DURATION_MS = 90_000L

val chatDocumentMimeTypes = arrayOf(
    "application/pdf",
    "text/plain",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

data class ChatPickedAttachment(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long?,
    val durationMs: Long? = null
)

@Composable
fun ChatAttachmentCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onRecordVoice: (() -> Unit)? = null,
    onPickDocument: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val cardShape = RoundedCornerShape(24.dp)
    val cardContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val borderColor = if (isGlassTheme) {
        Color.White.copy(alpha = 0.1f)
    } else {
        appearance.overlayBorderColor
    }
    val surface = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(24f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color(0xFF111B21).copy(alpha = 0.82f)) }
        )
    } else {
        Modifier.background(appearance.sheetColor, cardShape)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(surface)
            .clip(cardShape)
            .background(Color.Black.copy(alpha = if (isGlassTheme) 0.08f else 0f))
            .border(1.dp, borderColor, cardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(cardContentColor.copy(alpha = 0.18f))
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_gallery,
                title = "Photos",
                circleColor = Color(0xFF8B5CF6),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onPickImage
            )
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_video,
                title = "Videos",
                circleColor = Color(0xFFEF4444),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onPickVideo
            )
            onRecordVoice?.let {
                ChatAttachmentChoiceButton(
                    iconRes = R.drawable.ic_chat_audio,
                    title = "Voice",
                    circleColor = Color(0xFFF59E0B),
                    contentColor = cardContentColor,
                    modifier = Modifier.weight(1f),
                    onClick = it
                )
            }
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_document,
                title = "Docs",
                circleColor = Color(0xFF2563EB),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onPickDocument
            )
        }
    }
}

@Composable
private fun ChatAttachmentChoiceButton(
    iconRes: Int,
    title: String,
    circleColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

suspend fun Context.readChatAttachment(
    uri: Uri,
    fallbackFileName: String,
    fallbackMimeType: String
): ChatPickedAttachment = withContext(Dispatchers.IO) {
    val resolver = contentResolver
    val mimeType = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: fallbackMimeType
    val fileName = resolveAttachmentName(uri)?.let(::cleanAttachmentFileName)
        ?: cleanAttachmentFileName(fallbackFileName)
    val knownSize = resolveAttachmentSize(uri)
    if (knownSize != null && knownSize > maxChatAttachmentBytes(mimeType)) {
        throw IllegalArgumentException(chatAttachmentSizeMessage(mimeType))
    }
    val durationMs = if (mimeType.startsWith("video/")) {
        resolveVideoDurationMs(uri).also { duration ->
            if (duration != null && duration > CHAT_VIDEO_MAX_DURATION_MS) {
                throw IllegalArgumentException("Videos must be 90 seconds or less")
            }
        }
    } else {
        null
    }
    if (mimeType.startsWith("video/") && knownSize == null) {
        throw IllegalArgumentException("Could not read this video's size. Please choose another video.")
    }

    ChatPickedAttachment(
        uri = uri,
        fileName = fileName,
        mimeType = mimeType,
        fileSize = knownSize,
        durationMs = durationMs
    )
}

private fun Context.resolveAttachmentName(uri: Uri): String? {
    return runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }.getOrNull() ?: uri.lastPathSegment
}

private fun Context.resolveVideoDurationMs(uri: Uri): Long? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun Context.resolveAttachmentSize(uri: Uri): Long? {
    val sizeFromColumns = runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex).takeIf { it > 0L }
            } else {
                null
            }
        }
    }.getOrNull()

    if (sizeFromColumns != null) return sizeFromColumns

    val sizeFromAssetDescriptor = runCatching {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it > 0L }
        }
    }.getOrNull()

    if (sizeFromAssetDescriptor != null) return sizeFromAssetDescriptor

    return runCatching {
        contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.statSize.takeIf { it > 0L }
        }
    }.getOrNull()
}

fun maxChatAttachmentBytes(mimeType: String): Long {
    return if (mimeType.startsWith("video/")) {
        CHAT_VIDEO_MAX_BYTES.toLong()
    } else {
        CHAT_ATTACHMENT_MAX_BYTES.toLong()
    }
}

fun chatAttachmentSizeMessage(mimeType: String): String {
    return if (mimeType.startsWith("video/")) {
        "Videos must be under 150 MB"
    } else {
        "File must be under 25 MB"
    }
}

private fun cleanAttachmentFileName(name: String): String {
    return name
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { "attachment" }
}

fun String.isRemoteChatMediaUrl(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

fun Context.chatMediaCacheFile(
    mediaUrl: String,
    fileName: String?,
    mediaType: String
): File {
    val directory = File(filesDir, "chat_media/$mediaType").apply { mkdirs() }
    val extension = chatMediaExtension(fileName, mediaUrl, mediaType)
    return File(directory, "${sha256(mediaUrl).take(32)}.$extension")
}

private fun chatMediaExtension(
    fileName: String?,
    mediaUrl: String,
    mediaType: String
): String {
    val source = fileName?.takeIf { it.isNotBlank() }
        ?: Uri.decode(Uri.parse(mediaUrl).lastPathSegment.orEmpty())
    val extension = source
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
        .takeIf { it.length in 1..8 && it.all { ch -> ch.isLetterOrDigit() } }

    return extension ?: when (mediaType.lowercase(Locale.US)) {
        "image" -> "jpg"
        "video" -> "mp4"
        "audio" -> "m4a"
        else -> "bin"
    }
}

private fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

fun formatChatDownloadProgress(progress: Float): String {
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    return if (percent > 0) "Downloading $percent%" else "Downloading..."
}

fun formatChatLocalSaveProgress(progress: Float): String {
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    return if (percent > 0) "Saving locally $percent%" else "Saving locally..."
}

suspend fun Context.downloadChatMediaToCache(
    mediaUrl: String,
    targetFile: File,
    onProgress: suspend (Float) -> Unit
): File = withContext(Dispatchers.IO) {
    if (targetFile.exists() && targetFile.length() > 0L) {
        return@withContext targetFile
    }

    targetFile.parentFile?.mkdirs()
    val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
    val bufferSize = 256 * 1024
    val connection = (URL(mediaUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 120_000
        instanceFollowRedirects = true
    }

    try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Download failed ($responseCode)")
        }

        val totalBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connection.contentLengthLong
        } else {
            connection.contentLength.toLong()
        }
        var downloadedBytes = 0L
        var lastReportedProgress = 0f
        var lastProgressDispatchNs = 0L
        connection.inputStream.buffered(bufferSize).use { input ->
            tempFile.outputStream().buffered(bufferSize).use { output ->
                val buffer = ByteArray(bufferSize)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0L) {
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        val now = System.nanoTime()
                        val shouldDispatch = progress >= 1f ||
                            progress - lastReportedProgress >= 0.01f ||
                            now - lastProgressDispatchNs >= 150_000_000L
                        if (shouldDispatch) {
                            lastReportedProgress = progress
                            lastProgressDispatchNs = now
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
        }

        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        withContext(Dispatchers.Main) { onProgress(1f) }
        targetFile
    } finally {
        connection.disconnect()
        if (tempFile.exists() && !targetFile.exists()) {
            tempFile.delete()
        }
    }
}

fun Context.openChatDocument(
    mediaUrl: String,
    fileName: String?
) {
    val rawUri = Uri.parse(mediaUrl)
    val uri = if (rawUri.scheme.equals("file", ignoreCase = true)) {
        val file = File(rawUri.path.orEmpty())
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    } else {
        rawUri
    }
    val mimeType = resolveChatDocumentMimeType(uri, fileName)
    val typedIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        startActivity(Intent.createChooser(typedIntent, "Open attachment"))
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Context.resolveChatDocumentMimeType(
    uri: Uri,
    fileName: String?
): String {
    contentResolver.getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }
    val source = fileName?.takeIf { it.isNotBlank() }
        ?: Uri.decode(uri.lastPathSegment.orEmpty())
    val extension = source
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}

fun formatChatFileSize(fileSize: Int?): String? {
    val bytes = fileSize ?: return null
    if (bytes <= 0) return null
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }
}
