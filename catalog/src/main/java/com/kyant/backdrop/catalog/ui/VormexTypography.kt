package com.kyant.backdrop.catalog.ui

import androidx.compose.foundation.text.BasicText as ComposeBasicText
import androidx.compose.foundation.text.BasicTextField as ComposeBasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.SettingsPreferences

data class VormexFontOption(
    val key: String,
    val label: String
)

val VormexFontOptions = listOf(
    VormexFontOption(SettingsPreferences.FONT_FAMILY_SYSTEM, "System"),
    VormexFontOption(SettingsPreferences.FONT_FAMILY_SANS, "Sans"),
    VormexFontOption(SettingsPreferences.FONT_FAMILY_SERIF, "Serif"),
    VormexFontOption(SettingsPreferences.FONT_FAMILY_MONO, "Mono"),
    VormexFontOption(SettingsPreferences.FONT_FAMILY_CURSIVE, "Cursive"),
    VormexFontOption(SettingsPreferences.FONT_FAMILY_KAUSHAN, "Kaushan")
)

private val KaushanFontFamily = FontFamily(Font(R.font.kaushan_script))

private val LocalVormexFontFamily = compositionLocalOf<FontFamily?> { null }

@Composable
fun ProvideVormexFontFamily(
    fontFamilyKey: String,
    content: @Composable () -> Unit
) {
    val fontFamily = remember(fontFamilyKey) { vormexFontFamilyForKey(fontFamilyKey) }
    val currentTypography = MaterialTheme.typography
    val typography = remember(fontFamily, currentTypography) {
        currentTypography.withVormexFontFamily(fontFamily)
    }

    CompositionLocalProvider(LocalVormexFontFamily provides fontFamily) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = typography,
            content = content
        )
    }
}

@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null
) {
    ComposeBasicText(
        text = text,
        modifier = modifier,
        style = style.withCurrentVormexFontFamily(),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = color,
        autoSize = autoSize
    )
}

@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null
) {
    ComposeBasicText(
        text = text,
        modifier = modifier,
        style = style.withCurrentVormexFontFamily(),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        color = color,
        autoSize = autoSize
    )
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    ComposeBasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.withCurrentVormexFontFamily(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}

@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    ComposeBasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.withCurrentVormexFontFamily(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}

private fun vormexFontFamilyForKey(key: String): FontFamily? =
    when (key) {
        SettingsPreferences.FONT_FAMILY_SANS -> FontFamily.SansSerif
        SettingsPreferences.FONT_FAMILY_SERIF -> FontFamily.Serif
        SettingsPreferences.FONT_FAMILY_MONO -> FontFamily.Monospace
        SettingsPreferences.FONT_FAMILY_CURSIVE -> FontFamily.Cursive
        SettingsPreferences.FONT_FAMILY_KAUSHAN -> KaushanFontFamily
        else -> null
    }

@Composable
private fun TextStyle.withCurrentVormexFontFamily(): TextStyle {
    val fontFamily = LocalVormexFontFamily.current ?: return this
    return withVormexFontFamily(fontFamily)
}

private fun TextStyle.withVormexFontFamily(fontFamily: FontFamily?): TextStyle {
    if (fontFamily == null || this.fontFamily != null) return this
    return copy(fontFamily = fontFamily)
}

private fun Typography.withVormexFontFamily(fontFamily: FontFamily?): Typography {
    if (fontFamily == null) return this
    return copy(
        displayLarge = displayLarge.withVormexFontFamily(fontFamily),
        displayMedium = displayMedium.withVormexFontFamily(fontFamily),
        displaySmall = displaySmall.withVormexFontFamily(fontFamily),
        headlineLarge = headlineLarge.withVormexFontFamily(fontFamily),
        headlineMedium = headlineMedium.withVormexFontFamily(fontFamily),
        headlineSmall = headlineSmall.withVormexFontFamily(fontFamily),
        titleLarge = titleLarge.withVormexFontFamily(fontFamily),
        titleMedium = titleMedium.withVormexFontFamily(fontFamily),
        titleSmall = titleSmall.withVormexFontFamily(fontFamily),
        bodyLarge = bodyLarge.withVormexFontFamily(fontFamily),
        bodyMedium = bodyMedium.withVormexFontFamily(fontFamily),
        bodySmall = bodySmall.withVormexFontFamily(fontFamily),
        labelLarge = labelLarge.withVormexFontFamily(fontFamily),
        labelMedium = labelMedium.withVormexFontFamily(fontFamily),
        labelSmall = labelSmall.withVormexFontFamily(fontFamily)
    )
}
