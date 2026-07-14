package com.kyant.backdrop.catalog.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream

object MediaReadSafety {
    const val MaxPostImageBytes = 10L * 1024L * 1024L
    const val MaxPostVideoBytes = 100L * 1024L * 1024L
    const val MaxStoryMediaBytes = 50L * 1024L * 1024L

    fun readMediaBytes(
        context: Context,
        uri: Uri,
        fallbackFileName: String,
        maxBytes: Long,
        label: String
    ): Pair<ByteArray, String> {
        validateKnownSize(context.resolveOpenableSize(uri), maxBytes, label)
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            readBoundedBytes(stream, maxBytes, label)
        } ?: throw IllegalArgumentException("Could not open $label")
        return bytes to (context.resolveOpenableName(uri) ?: uri.lastPathSegment ?: fallbackFileName)
    }

    fun validateKnownSize(knownSize: Long?, maxBytes: Long, label: String) {
        if (knownSize != null && knownSize > maxBytes) {
            throw IllegalArgumentException(sizeError(label, maxBytes))
        }
    }

    fun readBoundedBytes(input: InputStream, maxBytes: Long, label: String): ByteArray {
        require(maxBytes > 0) { "maxBytes must be positive" }
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        var total = 0L

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                throw IllegalArgumentException(sizeError(label, maxBytes))
            }
            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    fun sizeError(label: String, maxBytes: Long): String {
        return "$label must be ${formatBytes(maxBytes)} or less"
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024L * 1024L)
        return if (mb > 0) "$mb MB" else "$bytes bytes"
    }

    private fun Context.resolveOpenableSize(uri: Uri): Long? {
        return runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                    cursor.getLong(index)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun Context.resolveOpenableName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
