package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.ui.ProvideVormexFontFamily

class IdentitySafetyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val themeMode by SettingsPreferences.themeMode(this@IdentitySafetyActivity)
                .collectAsState(initial = DefaultThemeModeKey)
            val accentPaletteKey by SettingsPreferences.accentPalette(this@IdentitySafetyActivity)
                .collectAsState(initial = "linkedin")
            val fontFamilyKey by SettingsPreferences.fontFamily(this@IdentitySafetyActivity)
                .collectAsState(initial = SettingsPreferences.FONT_FAMILY_SYSTEM)
            val appearance = rememberVormexAppearance(themeMode)
            val accentColor = vormexAccentColor(themeMode, accentPaletteKey)
            val colorScheme = identitySafetyColorScheme(appearance, accentColor)

            SideEffect {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !appearance.isDarkTheme
                    isAppearanceLightNavigationBars = !appearance.isDarkTheme
                }
            }

            ProvideVormexFontFamily(fontFamilyKey) {
                ProvideVormexAppearance(themeMode) {
                    MaterialTheme(colorScheme = colorScheme) {
                        IdentitySafetyScreen(
                            onNavigateBack = { finish() },
                            onNavigateHome = {
                                startActivity(
                                    Intent(this@IdentitySafetyActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                )
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun identitySafetyColorScheme(
    appearance: VormexAppearance,
    accentColor: Color
): ColorScheme {
    val background = appearance.backgroundColor.takeUnless { it.alpha == 0f } ?: appearance.sheetColor
    val error = if (appearance.isDarkTheme) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
    val errorContainer = if (appearance.isDarkTheme) Color(0xFF93000A) else Color(0xFFFFDAD6)
    val onErrorContainer = if (appearance.isDarkTheme) Color(0xFFFFDAD6) else Color(0xFF410002)
    val base = if (appearance.isDarkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    return base.copy(
        primary = accentColor,
        onPrimary = if (appearance.isDarkTheme) Color(0xFF07110B) else Color.White,
        primaryContainer = appearance.selectedColor,
        onPrimaryContainer = appearance.contentColor,
        secondary = accentColor,
        onSecondary = if (appearance.isDarkTheme) Color(0xFF07110B) else Color.White,
        background = background,
        onBackground = appearance.contentColor,
        surface = appearance.sheetColor,
        onSurface = appearance.contentColor,
        surfaceVariant = appearance.subtleColor,
        onSurfaceVariant = appearance.mutedContentColor,
        outline = appearance.inputBorderColor,
        outlineVariant = appearance.dividerColor,
        error = error,
        onError = Color.White,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer
    )
}
