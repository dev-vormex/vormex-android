package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.backdrop.catalog.chat.formatChatFileSize
import com.kyant.backdrop.catalog.chat.openChatDocument
import com.kyant.backdrop.catalog.linkedin.FullScreenVideoPlayer
import com.kyant.backdrop.catalog.linkedin.VideoPlayer
import com.kyant.backdrop.catalog.network.models.GroupMessage

data class GroupFullscreenMedia(
    val url: String,
    val type: String,
    val title: String? = null
)

@Composable
fun GroupChatMediaMessageContent(
    message: GroupMessage,
    contentColor: Color,
    accentColor: Color,
    onOpenMedia: (GroupFullscreenMedia) -> Unit
) {
    val context = LocalContext.current
    val mediaUrl = message.mediaUrl ?: return
    val mediaType = message.contentType.lowercase()
    val isUploading = message.isPendingUpload()

    when (mediaType) {
        "image" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(contentColor.copy(alpha = 0.08f))
                    .clickable {
                        onOpenMedia(
                            GroupFullscreenMedia(
                                url = mediaUrl,
                                type = "image",
                                title = message.fileName ?: "Image"
                            )
                        )
                    }
            ) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = message.fileName ?: "Group image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isUploading) {
                    GroupMediaUploadingOverlay()
                }
            }
        }

        "video" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .clickable(enabled = !isUploading) {
                        onOpenMedia(
                            GroupFullscreenMedia(
                                url = mediaUrl,
                                type = "video",
                                title = message.fileName ?: "Video"
                            )
                        )
                    }
            ) {
                if (isUploading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.dp
                        )
                        BasicText(
                            "Uploading video...",
                            style = TextStyle(Color.White, 12.sp, FontWeight.SemiBold)
                        )
                    }
                } else {
                    VideoPlayer(
                        videoUrl = mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = false,
                        showControls = false,
                        contentColor = Color.White,
                        onFullScreenClick = {
                            onOpenMedia(
                                GroupFullscreenMedia(
                                    url = mediaUrl,
                                    type = "video",
                                    title = message.fileName ?: "Video"
                                )
                            )
                        }
                    )
                    GroupMediaStatusChip(
                        text = "Open",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
            }
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.08f))
                    .clickable(enabled = !isUploading) {
                        context.openChatDocument(
                            mediaUrl = mediaUrl,
                            fileName = message.fileName ?: message.content
                        )
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "PDF",
                        style = TextStyle(accentColor, 10.sp, FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        message.fileName?.ifBlank { "Attachment" } ?: "Attachment",
                        style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        listOfNotNull(
                            formatChatFileSize(message.fileSize),
                            if (isUploading) "Uploading..." else "Tap to open"
                        ).joinToString(" • "),
                        style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                BasicText(
                    if (isUploading) "Wait" else "Open",
                    style = TextStyle(accentColor, 12.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
fun GroupFullscreenMediaViewer(
    media: GroupFullscreenMedia,
    onDismiss: () -> Unit
) {
    when (media.type.lowercase()) {
        "video" -> FullScreenVideoPlayer(
            videoUrl = media.url,
            onDismiss = onDismiss
        )

        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = media.url,
                    contentDescription = media.title ?: "Group media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "✕",
                        style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun GroupChatAttachmentError(
    message: String,
    contentColor: Color,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFB3261E).copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            message,
            style = TextStyle(contentColor, 12.sp),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            "Dismiss",
            style = TextStyle(contentColor, 12.sp, FontWeight.SemiBold),
            modifier = Modifier.clickable(onClick = onDismiss)
        )
    }
}

@Composable
private fun GroupMediaStatusChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun GroupMediaUploadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            BasicText(
                "Uploading...",
                style = TextStyle(Color.White, 12.sp, FontWeight.SemiBold)
            )
        }
    }
}

private fun GroupMessage.isPendingUpload(): Boolean {
    return id.startsWith("pending-")
}
