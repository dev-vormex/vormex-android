package com.kyant.backdrop.catalog.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.data.OnboardingPreferences
import com.kyant.backdrop.catalog.network.RecommendationApiService
import com.kyant.backdrop.catalog.network.models.RecommendationPreferencesPatch
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val iconResId: Int
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Welcome to Vormex",
        description = "Connect with professionals, share your journey, and build meaningful relationships.",
        iconResId = R.drawable.ic_network
    ),
    OnboardingPage(
        title = "Share Your Story",
        description = "Create posts, share updates, and engage with a community that cares about your growth.",
        iconResId = R.drawable.ic_post
    ),
    OnboardingPage(
        title = "Discover Opportunities",
        description = "Find jobs, events, and connections that align with your professional goals.",
        iconResId = R.drawable.ic_search
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isLightTheme = !isSystemInDarkTheme()
    val backdrop = rememberLayerBackdrop()
    var personalizedRecommendationsEnabled by rememberSaveable { mutableStateOf(true) }
    var activityRecommendationsEnabled by rememberSaveable { mutableStateOf(true) }

    fun completeWithPreferences() {
        coroutineScope.launch {
            RecommendationApiService.updatePreferences(
                context,
                RecommendationPreferencesPatch(
                    personalizedRecommendationsEnabled = personalizedRecommendationsEnabled,
                    activityRecommendationsEnabled = activityRecommendationsEnabled
                )
            )
            onComplete()
        }
    }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { onboardingPages.size }
    )
    
    val textColor = if (isLightTheme) Color.Black else Color.White
    val secondaryTextColor = if (isLightTheme) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLightTheme) Color.White else Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button at top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < onboardingPages.size - 1) {
                    Box(
                        modifier = Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(16.dp) },
                                effects = {
                                    vibrancy()
                                    blur(4f.dp.toPx())
                                    lens(8f.dp.toPx(), 16f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.3f))
                                }
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    coroutineScope.launch {
                                        completeWithPreferences()
                                    }
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            text = "Skip",
                            style = TextStyle(
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Pager for onboarding pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    backdrop = backdrop,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }

            if (pagerState.currentPage == onboardingPages.lastIndex) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecommendationOnboardingToggle(
                        title = "Personalize my recommendations",
                        description = "Use my public profile and Vormex activity to rank posts, people and opportunities.",
                        checked = personalizedRecommendationsEnabled,
                        onCheckedChange = { personalizedRecommendationsEnabled = it },
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                    RecommendationOnboardingToggle(
                        title = "Show my public activity in recommendations",
                        description = "Eligible public reactions or comments may be named. Private saves, views and messages are never named.",
                        checked = activityRecommendationsEnabled,
                        onCheckedChange = { activityRecommendationsEnabled = it },
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    val animatedSize by animateFloatAsState(
                        targetValue = if (isSelected) 12f else 8f,
                        animationSpec = tween(durationMillis = 300),
                        label = "dotSize"
                    )
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.5f,
                        animationSpec = tween(durationMillis = 300),
                        label = "dotAlpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(animatedSize.dp)
                            .clip(CircleShape)
                            .background(textColor.copy(alpha = animatedAlpha))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val isLastPage = pagerState.currentPage == onboardingPages.size - 1
                
                LiquidButton(
                    onClick = {
                        coroutineScope.launch {
                            if (isLastPage) {
                                completeWithPreferences()
                            } else {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    surfaceColor = if (isLightTheme) {
                        Color.White.copy(alpha = 0.5f)
                    } else {
                        Color.Black.copy(alpha = 0.3f)
                    }
                ) {
                    BasicText(
                        text = if (isLastPage) "Get Started" else "Next",
                        style = TextStyle(
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecommendationOnboardingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(title, style = TextStyle(textColor, 14.sp, fontWeight = FontWeight.SemiBold))
            BasicText(description, style = TextStyle(secondaryTextColor, 11.sp, lineHeight = 15.sp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    backdrop: com.kyant.backdrop.Backdrop,
    textColor: Color,
    secondaryTextColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glass card with icon
        Box(
            modifier = Modifier
                .size(160.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(40.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.4f))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(page.iconResId),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = textColor
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title in glass container
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.3f))
                    }
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            BasicText(
                text = page.title,
                style = TextStyle(
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        BasicText(
            text = page.description,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
