package com.kyant.backdrop.catalog.linkedin

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kyant.backdrop.catalog.R

data class DefaultPostVideo(
    val id: String,
    val title: String,
    val description: String,
    @param:RawRes val rawResId: Int,
    val backgroundColor: Color? = null
)

val defaultPostVideos = listOf(
    DefaultPostVideo(
        id = "businessman_rocket",
        title = "Rocket Lift",
        description = "Momentum and launches",
        rawResId = R.raw.post_businessman_rocket
    ),
    DefaultPostVideo(
        id = "rainbow_twist",
        title = "Rainbow Twist",
        description = "Bright quick energy",
        rawResId = R.raw.post_rainbow_twist
    ),
    DefaultPostVideo(
        id = "hoverboard",
        title = "Hoverboard",
        description = "Playful movement",
        rawResId = R.raw.post_hoverboard
    ),
    DefaultPostVideo(
        id = "loudspeaker",
        title = "Loudspeaker",
        description = "Announcements",
        rawResId = R.raw.post_loudspeaker,
        backgroundColor = Color(0xFFFFF4D8)
    ),
    DefaultPostVideo(
        id = "eye_searching",
        title = "Eye Search",
        description = "Discovery and curiosity",
        rawResId = R.raw.post_eye_searching
    ),
    DefaultPostVideo(
        id = "sushi",
        title = "Sushi",
        description = "Small fun moments",
        rawResId = R.raw.post_sushi
    ),
    DefaultPostVideo(
        id = "spooky_onion",
        title = "Spooky Onion",
        description = "Quirky reactions",
        rawResId = R.raw.post_spooky_onion
    ),
    DefaultPostVideo(
        id = "spooky_onion_alt",
        title = "Spooky Onion 2",
        description = "Alternate onion mood",
        rawResId = R.raw.post_spooky_onion_alt
    ),
    DefaultPostVideo(
        id = "lost_in_space",
        title = "Lost in Space",
        description = "Empty-state space drift",
        rawResId = R.raw.post_404_lost_in_space
    ),
    DefaultPostVideo(
        id = "error_not_found",
        title = "Not Found",
        description = "404-style status moment",
        rawResId = R.raw.post_404_error_not_found
    ),
    DefaultPostVideo(
        id = "all_the_data",
        title = "All the Data",
        description = "Analytics and insights",
        rawResId = R.raw.post_all_the_data_concept
    ),
    DefaultPostVideo(
        id = "baloons",
        title = "Balloons",
        description = "Light celebration lift",
        rawResId = R.raw.post_baloons
    ),
    DefaultPostVideo(
        id = "be_bold",
        title = "Be Bold",
        description = "Confident statement",
        rawResId = R.raw.post_be_bold,
        backgroundColor = Color(0xFF101827)
    ),
    DefaultPostVideo(
        id = "quiz_bump",
        title = "Quiz Bump",
        description = "Questions and polls",
        rawResId = R.raw.post_quiz_bump
    ),
    DefaultPostVideo(
        id = "litarebello_hi",
        title = "Litarebello Hi",
        description = "Friendly hello",
        rawResId = R.raw.post_litarebello_hi
    ),
    DefaultPostVideo(
        id = "palapala_loading",
        title = "Palapala Loading",
        description = "Waiting and progress",
        rawResId = R.raw.post_palapala_loading
    )
)

fun findDefaultPostVideo(id: String?): DefaultPostVideo? {
    if (id.isNullOrBlank()) return null
    return defaultPostVideos.firstOrNull { it.id == id }
}

@Composable
fun DefaultPostVideoPlayer(
    defaultVideoId: String?,
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false,
    accentColor: Color = Color(0xFF4B70E2),
    height: Dp? = 220.dp,
    contentScale: ContentScale = ContentScale.Fit
) {
    val video = remember(defaultVideoId) { findDefaultPostVideo(defaultVideoId) } ?: return
    DefaultPostVideoPlayer(
        video = video,
        modifier = modifier,
        reduceAnimations = reduceAnimations,
        accentColor = accentColor,
        height = height,
        contentScale = contentScale
    )
}

@Composable
fun DefaultPostVideoPlayer(
    video: DefaultPostVideo,
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false,
    accentColor: Color = Color(0xFF4B70E2),
    height: Dp? = 220.dp,
    contentScale: ContentScale = ContentScale.Fit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(video.rawResId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceAnimations
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compositionBounds = composition?.bounds
        val resolvedHeight = height ?: compositionBounds
            ?.takeIf { it.width() > 0 && it.height() > 0 }
            ?.let { bounds -> maxWidth * (bounds.height().toFloat() / bounds.width().toFloat()) }
            ?: 220.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(resolvedHeight)
                .then(
                    video.backgroundColor?.let { Modifier.background(it) } ?: Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { if (reduceAnimations) 0f else progress },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    enableMergePaths = true,
                    clipToCompositionBounds = true
                )
            }
        }
    }
}
