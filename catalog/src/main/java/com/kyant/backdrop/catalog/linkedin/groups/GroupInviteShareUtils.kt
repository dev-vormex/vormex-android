package com.kyant.backdrop.catalog.linkedin.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

fun copyGroupInviteLink(context: Context, inviteUrl: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Group invite link", inviteUrl))
    Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()
}

fun launchGroupInviteShareSheet(
    context: Context,
    groupName: String,
    inviteUrl: String
): Boolean {
    if (inviteUrl.isBlank()) return false
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Join $groupName on Vormex: $inviteUrl")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share group invite"))
    return true
}

fun generateGroupInviteQrBitmap(value: String, sizePx: Int = 512): Bitmap {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        val offset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    }
}
