package com.kyant.backdrop.catalog.linkedin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kyant.backdrop.catalog.R

@Composable
fun LikeHeartsEffect(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.stream_of_hearts))
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
        }
    }

    if (visible && composition != null) {
        key(trigger) {
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                isPlaying = true
            )
            LaunchedEffect(progress) {
                if (progress >= 0.99f) {
                    visible = false
                }
            }
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = modifier,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                clipToCompositionBounds = false
            )
        }
    }
}
