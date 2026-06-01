package dev.fishpi.mobile

import android.app.Activity
import android.os.Build
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class FishPiThemePreset(val label: String, val key: String) {
    Island("夜间模式", "island"),
    DeepBlueNeon("深蓝荧光", "deep-blue-neon"),
    ;

    companion object {
        fun fromKey(key: String, fallback: FishPiThemePreset): FishPiThemePreset =
            entries.firstOrNull { it.key == key } ?: fallback
    }
}

internal enum class FishPiUiStyle {
    Classic,
}

internal data class CustomFishPiTheme(
    val key: String,
    val label: String,
    val description: String,
    val tokens: FishPiThemeTokens,
    val palette: FishPiPalette,
    val rawJson: String,
    val previewImageUris: Map<String, String> = emptyMap(),
)

internal data class FishPiThemeOption(
    val key: String,
    val label: String,
    val description: String,
    val tokens: FishPiThemeTokens,
    val palette: FishPiPalette,
    val uiStyle: FishPiUiStyle = FishPiUiStyle.Classic,
    val builtinPreset: FishPiThemePreset? = null,
    val rawJson: String? = null,
    val previewImageUris: Map<String, String> = emptyMap(),
)

internal data class FishPiPalette(
    val background: Color,
    val chatBackground: Color,
    val wallpaperColors: List<Color>,
    val wallpaperImageUri: String? = null,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceContainer: Color,
    val onSurface: Color,
    val weakText: Color,
    val userName: Color,
    val clientText: Color,
    val clientBackground: Color,
    val timeText: Color,
    val outline: Color,
    val accent: Color,
    val quoteBackground: Color,
    val outgoingBubble: Color,
    val incomingBubble: Color,
    val linkText: Color = accent,
    val quoteText: Color = weakText,
    val quoteLine: Color = weakText,
    val toolDefault: Color = accent,
    val toolGallery: Color = Color(0xFF16A34A),
    val toolCamera: Color = Color(0xFF2563EB),
    val toolRedPacket: Color = Color(0xFFE53935),
)

internal enum class FishPiThemeColorScheme {
    Light,
    Dark,
}

internal data class FishPiColorTokens(
    val base100: Color,
    val base200: Color,
    val base300: Color,
    val baseContent: Color,
    val primary: Color,
    val primaryContent: Color,
    val secondary: Color,
    val secondaryContent: Color,
    val accent: Color,
    val accentContent: Color,
    val neutral: Color,
    val neutralContent: Color,
    val info: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val messageOutgoing: Color,
)

internal data class FishPiRadiusTokens(
    val selector: Float,
    val field: Float,
    val box: Float,
)

internal data class FishPiSpacingTokens(
    val page: Float,
    val section: Float,
    val item: Float,
    val control: Float,
)

internal data class FishPiBorderTokens(
    val width: Float,
    val opacity: Float,
)

internal data class FishPiDepthTokens(
    val level: Float,
)

internal data class FishPiThemeTokens(
    val colorScheme: FishPiThemeColorScheme,
    val colors: FishPiColorTokens,
    val radius: FishPiRadiusTokens,
    val spacing: FishPiSpacingTokens,
    val border: FishPiBorderTokens,
    val depth: FishPiDepthTokens,
)

private val ThemeKeySanitizeRegex = Regex("[^a-z0-9_-]+")

internal val IslandFishPiThemeTokens = FishPiThemeTokens(
    colorScheme = FishPiThemeColorScheme.Dark,
    colors = FishPiColorTokens(
        base100 = Color(0xFF121212),
        base200 = Color(0xFF1E1E1E),
        base300 = Color(0xFF2C2C2C),
        baseContent = Color(0xFFEDEDED),
        primary = Color(0xFFE0E0E0),
        primaryContent = Color(0xFF101010),
        secondary = Color(0xFFD6D6D6),
        secondaryContent = Color(0xFF101010),
        accent = Color(0xFFB6B6B6),
        accentContent = Color(0xFF101010),
        neutral = Color(0xFFB0B0B0),
        neutralContent = Color(0xFF101010),
        info = Color(0xFFD0D0D0),
        success = Color(0xFF42D974),
        warning = Color(0xFFE4C36A),
        error = Color(0xFFFF5252),
        messageOutgoing = Color(0xFF252525),
    ),
    radius = FishPiRadiusTokens(selector = 999f, field = 18f, box = 12f),
    spacing = FishPiSpacingTokens(page = 14f, section = 12f, item = 8f, control = 10f),
    border = FishPiBorderTokens(width = 1f, opacity = 0.22f),
    depth = FishPiDepthTokens(level = 0.08f),
)

internal val DeepBlueNeonFishPiThemeTokens = FishPiThemeTokens(
    colorScheme = FishPiThemeColorScheme.Light,
    colors = FishPiColorTokens(
        base100 = Color(0xFFF3F8FF),
        base200 = Color(0xFFFFFFFF),
        base300 = Color(0xFFE9F2FF),
        baseContent = Color(0xFF08233F),
        primary = Color(0xFF08233F),
        primaryContent = Color(0xFFFFFFFF),
        secondary = Color(0xFF0B5C93),
        secondaryContent = Color(0xFFFFFFFF),
        accent = Color(0xFF7CFF52),
        accentContent = Color(0xFF08233F),
        neutral = Color(0xFF5D7188),
        neutralContent = Color(0xFFFFFFFF),
        info = Color(0xFF0B5C93),
        success = Color(0xFF42D94D),
        warning = Color(0xFFEAB308),
        error = Color(0xFFE53935),
        messageOutgoing = Color(0xFFEAF4FF),
    ),
    radius = FishPiRadiusTokens(selector = 999f, field = 18f, box = 12f),
    spacing = FishPiSpacingTokens(page = 14f, section = 12f, item = 8f, control = 10f),
    border = FishPiBorderTokens(width = 1f, opacity = 0.20f),
    depth = FishPiDepthTokens(level = 0.12f),
)

internal val IslandFishPiPalette = IslandFishPiThemeTokens.toPalette()
internal val DeepBlueNeonFishPiPalette = DeepBlueNeonFishPiThemeTokens.toPalette()

internal val LocalFishPiPalette = compositionLocalOf { IslandFishPiPalette }
internal val LocalFishPiThemeTokens = compositionLocalOf { IslandFishPiThemeTokens }
internal val LocalFishPiUiStyle = compositionLocalOf { FishPiUiStyle.Classic }

internal object FishPiTheme {
    val background: Color @Composable get() = LocalFishPiPalette.current.background
    val chatBackground: Color @Composable get() = LocalFishPiPalette.current.chatBackground
    val surface: Color @Composable get() = LocalFishPiPalette.current.surface
    val surfaceElevated: Color @Composable get() = LocalFishPiPalette.current.surfaceElevated
    val surfaceContainer: Color @Composable get() = LocalFishPiPalette.current.surfaceContainer
    val onSurface: Color @Composable get() = LocalFishPiPalette.current.onSurface
    val weakText: Color @Composable get() = LocalFishPiPalette.current.weakText
    val outline: Color @Composable get() = LocalFishPiPalette.current.outline
    val accent: Color @Composable get() = LocalFishPiPalette.current.accent
    val success: Color @Composable get() = LocalFishPiThemeTokens.current.colors.success
    val warning: Color @Composable get() = LocalFishPiThemeTokens.current.colors.warning
    val error: Color @Composable get() = LocalFishPiThemeTokens.current.colors.error
    val linkText: Color @Composable get() = LocalFishPiPalette.current.linkText
    val toolGallery: Color @Composable get() = LocalFishPiPalette.current.toolGallery
    val toolCamera: Color @Composable get() = LocalFishPiPalette.current.toolCamera
    val toolRedPacket: Color @Composable get() = LocalFishPiPalette.current.toolRedPacket
    val radiusSelector: Dp @Composable get() = LocalFishPiThemeTokens.current.radius.selector.dp
    val radiusField: Dp @Composable get() = LocalFishPiThemeTokens.current.radius.field.dp
    val radiusBox: Dp @Composable get() = LocalFishPiThemeTokens.current.radius.box.dp
    val spacingPage: Dp @Composable get() = LocalFishPiThemeTokens.current.spacing.page.dp
    val spacingSection: Dp @Composable get() = LocalFishPiThemeTokens.current.spacing.section.dp
    val spacingItem: Dp @Composable get() = LocalFishPiThemeTokens.current.spacing.item.dp
    val spacingControl: Dp @Composable get() = LocalFishPiThemeTokens.current.spacing.control.dp
    val borderWidth: Dp @Composable get() = LocalFishPiThemeTokens.current.border.width.dp
    val depth: Float @Composable get() = LocalFishPiThemeTokens.current.depth.level
    val uiStyle: FishPiUiStyle @Composable get() = LocalFishPiUiStyle.current
}

internal fun FishPiThemeTokens.toPalette(wallpaperImageUri: String? = null): FishPiPalette {
    val c = colors
    val dark = colorScheme == FishPiThemeColorScheme.Dark
    val incoming = if (dark) c.base200 else c.base200
    return FishPiPalette(
        background = c.base100,
        chatBackground = if (dark) c.base100 else c.base100,
        wallpaperColors = listOf(c.base100, c.base200, c.base300),
        wallpaperImageUri = wallpaperImageUri,
        surface = c.base200,
        surfaceElevated = c.base200,
        surfaceContainer = c.base300,
        onSurface = c.baseContent,
        weakText = c.neutral,
        userName = c.secondary,
        clientText = c.neutral,
        clientBackground = c.base300,
        timeText = c.neutral.copy(alpha = 0.78f),
        outline = c.neutral.copy(alpha = border.opacity.coerceIn(0f, 1f)),
        accent = c.primary,
        quoteBackground = c.base300.copy(alpha = if (dark) 0.82f else 0.72f),
        outgoingBubble = c.messageOutgoing,
        incomingBubble = incoming,
        linkText = c.secondary,
        quoteText = c.neutral,
        quoteLine = c.accent,
        toolDefault = c.accent,
        toolGallery = c.success,
        toolCamera = c.info,
        toolRedPacket = c.error,
    )
}

internal fun builtinThemeOptions(): List<FishPiThemeOption> =
    listOf(FishPiThemePreset.DeepBlueNeon, FishPiThemePreset.Island).map { preset ->
        val tokens = preset.themeTokens()
        FishPiThemeOption(
            key = preset.key,
            label = preset.label,
            description = preset.themeDescription(),
            tokens = tokens,
            palette = tokens.toPalette(),
            uiStyle = preset.uiStyle(),
            builtinPreset = preset,
        )
    }

internal fun buildThemeOptions(
    builtinOptions: List<FishPiThemeOption>,
    importedThemes: List<CustomFishPiTheme>,
): List<FishPiThemeOption> =
    builtinOptions + importedThemes.map { custom ->
        FishPiThemeOption(
            key = custom.key,
            label = custom.label,
            description = custom.description,
            tokens = custom.tokens,
            palette = custom.palette,
            rawJson = custom.rawJson,
            previewImageUris = custom.previewImageUris,
        )
    }

internal fun FishPiThemePreset.themeTokens(): FishPiThemeTokens =
    when (this) {
        FishPiThemePreset.Island -> IslandFishPiThemeTokens
        FishPiThemePreset.DeepBlueNeon -> DeepBlueNeonFishPiThemeTokens
    }

internal fun FishPiThemePreset.previewPalette(): FishPiPalette =
    themeTokens().toPalette()

internal fun FishPiThemePreset.uiStyle(): FishPiUiStyle =
    FishPiUiStyle.Classic

internal fun FishPiThemePreset.themeDescription(): String =
    when (this) {
        FishPiThemePreset.Island -> "通用深色背景，中性灰层级与高对比文字"
        FishPiThemePreset.DeepBlueNeon -> "深蓝主色，荧光绿强调，白色内容层"
    }

internal fun parseCustomFishPiTheme(rawJson: String): CustomFishPiTheme {
    val json = JSONObject(rawJson)
    val colors = json.optJSONObject("colors") ?: JSONObject()
    val name = json.optString("name").ifBlank { json.optString("label").ifBlank { "导入主题" } }
    val base = IslandFishPiThemeTokens
    val radius = json.optJSONObject("radius") ?: JSONObject()
    val spacing = json.optJSONObject("spacing") ?: JSONObject()
    val border = json.optJSONObject("border") ?: JSONObject()
    val depth = json.optJSONObject("depth") ?: JSONObject()
    val schemeValue = json.optString("colorScheme")
        .ifBlank { json.optString("prefersdark") }
        .lowercase()
    val colorScheme = when (schemeValue) {
            "light" -> FishPiThemeColorScheme.Light
            "dark" -> FishPiThemeColorScheme.Dark
            else -> base.colorScheme
        }
    val base100 = colors.optThemeColor("base100", "base-100", base.colors.base100)
    val base200 = colors.optThemeColor("base200", "base-200", base.colors.base200)
    val base300 = colors.optThemeColor("base300", "base-300", base.colors.base300)
    val baseContent = colors.optThemeColor("baseContent", "base-content", base.colors.baseContent)
    val primary = colors.optThemeColor("primary", "primary", base.colors.primary)
    val primaryContent = colors.optThemeColor("primaryContent", "primary-content", base.colors.primaryContent)
    val secondary = colors.optThemeColor("secondary", "secondary", base.colors.secondary)
    val secondaryContent = colors.optThemeColor("secondaryContent", "secondary-content", base.colors.secondaryContent)
    val accent = colors.optThemeColor("accent", "accent", base.colors.accent)
    val accentContent = colors.optThemeColor("accentContent", "accent-content", base.colors.accentContent)
    val neutral = colors.optThemeColor("neutral", "neutral", base.colors.neutral)
    val neutralContent = colors.optThemeColor("neutralContent", "neutral-content", base.colors.neutralContent)
    val info = colors.optThemeColor("info", "info", base.colors.info)
    val success = colors.optThemeColor("success", "success", base.colors.success)
    val warning = colors.optThemeColor("warning", "warning", base.colors.warning)
    val error = colors.optThemeColor("error", "error", base.colors.error)
    val tokens = FishPiThemeTokens(
        colorScheme = colorScheme,
        colors = FishPiColorTokens(
            base100 = base100,
            base200 = base200,
            base300 = base300,
            baseContent = baseContent,
            primary = primary,
            primaryContent = primaryContent,
            secondary = secondary,
            secondaryContent = secondaryContent,
            accent = accent,
            accentContent = accentContent,
            neutral = neutral,
            neutralContent = neutralContent,
            info = info,
            success = success,
            warning = warning,
            error = error,
            messageOutgoing = colors.optThemeColor(
                "messageOutgoing",
                "message-outgoing",
                defaultMessageOutgoingColor(primary, base200, colorScheme),
            ),
        ),
        radius = FishPiRadiusTokens(
            selector = radius.optThemeFloat("selector", "radius-selector", base.radius.selector, 0f, 40f),
            field = radius.optThemeFloat("field", "radius-field", base.radius.field, 0f, 40f),
            box = radius.optThemeFloat("box", "radius-box", base.radius.box, 0f, 40f),
        ),
        spacing = FishPiSpacingTokens(
            page = spacing.optThemeFloat("page", "page", base.spacing.page, 8f, 28f),
            section = spacing.optThemeFloat("section", "section", base.spacing.section, 6f, 28f),
            item = spacing.optThemeFloat("item", "item", base.spacing.item, 4f, 20f),
            control = spacing.optThemeFloat("control", "control", base.spacing.control, 4f, 20f),
        ),
        border = FishPiBorderTokens(
            width = border.optThemeFloat("width", "border", base.border.width, 0f, 3f),
            opacity = border.optThemeFloat("opacity", "opacity", base.border.opacity, 0f, 1f),
        ),
        depth = FishPiDepthTokens(
            level = depth.optThemeFloat("level", "depth", base.depth.level, 0f, 1f),
        ),
    )
    val wallpaperImageUri = json.optJSONObject("wallpaper")?.optString("image").orEmpty().ifBlank { null }
    val palette = tokens.toPalette(wallpaperImageUri)
    val previewImageUris = json.optJSONObject("previews")?.let { previews ->
        buildMap {
            listOf("chat", "chatroom", "home", "article", "profile").forEach { key ->
                previews.optString(key).trim().takeIf { it.isNotBlank() }?.let { put(key, it) }
            }
        }
    }.orEmpty()
    val explicitKey = json.optString("key").ifBlank { json.optString("id") }
    val key = "custom:" + explicitKey.ifBlank { "${name}-${rawJson.hashCode()}" }
        .lowercase()
        .replace(ThemeKeySanitizeRegex, "-")
        .trim('-')
    return CustomFishPiTheme(
        key = key,
        label = name,
        description = json.optString("description").ifBlank { "外部导入主题" },
        tokens = tokens,
        palette = palette,
        rawJson = rawJson,
        previewImageUris = previewImageUris,
    )
}

internal fun buildEditableThemeJson(
    label: String,
    description: String,
    tokens: FishPiThemeTokens,
): String {
    val c = tokens.colors
    val colors = JSONObject()
        .put("base-100", c.base100.toThemeHex())
        .put("base-200", c.base200.toThemeHex())
        .put("base-300", c.base300.toThemeHex())
        .put("base-content", c.baseContent.toThemeHex())
        .put("primary", c.primary.toThemeHex())
        .put("primary-content", c.primaryContent.toThemeHex())
        .put("secondary", c.secondary.toThemeHex())
        .put("secondary-content", c.secondaryContent.toThemeHex())
        .put("accent", c.accent.toThemeHex())
        .put("accent-content", c.accentContent.toThemeHex())
        .put("neutral", c.neutral.toThemeHex())
        .put("neutral-content", c.neutralContent.toThemeHex())
        .put("info", c.info.toThemeHex())
        .put("success", c.success.toThemeHex())
        .put("warning", c.warning.toThemeHex())
        .put("error", c.error.toThemeHex())
        .put("message-outgoing", c.messageOutgoing.toThemeHex())
    return JSONObject()
        .put("schema", 1)
        .put("previewTemplate", "fishpi-mobile-v1")
        .put("name", label.ifBlank { "应用内主题" })
        .put("description", description.ifBlank { "应用内编辑主题" })
        .put("colorScheme", tokens.colorScheme.name.lowercase())
        .put("colors", colors)
        .put("radius", JSONObject()
            .put("radius-selector", tokens.radius.selector)
            .put("radius-field", tokens.radius.field)
            .put("radius-box", tokens.radius.box))
        .put("spacing", JSONObject()
            .put("page", tokens.spacing.page)
            .put("section", tokens.spacing.section)
            .put("item", tokens.spacing.item)
            .put("control", tokens.spacing.control))
        .put("border", JSONObject()
            .put("border", tokens.border.width)
            .put("opacity", tokens.border.opacity))
        .put("depth", JSONObject().put("depth", tokens.depth.level))
        .toString()
}

internal fun Color.toThemeHex(): String =
    "#%06X".format(0xFFFFFF and toArgb())

private fun JSONObject.optThemeColor(primaryName: String, aliasName: String, fallback: Color): Color {
    val value = optString(primaryName).ifBlank { optString(aliasName) }.trim()
    if (value.isBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(value))
    }.getOrDefault(fallback)
}

private fun defaultMessageOutgoingColor(
    primary: Color,
    surface: Color,
    colorScheme: FishPiThemeColorScheme,
): Color {
    val alpha = if (colorScheme == FishPiThemeColorScheme.Dark) 0.20f else 0.10f
    return primary.copy(alpha = alpha).compositeOver(surface)
}

private fun JSONObject.optThemeFloat(
    primaryName: String,
    aliasName: String,
    fallback: Float,
    min: Float,
    max: Float,
): Float {
    val value = when {
        has(primaryName) -> optDouble(primaryName, fallback.toDouble())
        has(aliasName) -> optDouble(aliasName, fallback.toDouble())
        else -> fallback.toDouble()
    }
    return value.toFloat().coerceIn(min, max)
}

// ─────────────────────────────────────────────────────────────
// Material 3 Color Schemes
// ─────────────────────────────────────────────────────────────

internal val FishPiLightColorScheme = lightColorScheme(
    primary = M3Primary40,
    onPrimary = M3Neutral100,
    primaryContainer = M3Primary90,
    onPrimaryContainer = M3Primary10,
    secondary = M3Secondary40,
    onSecondary = M3Neutral100,
    secondaryContainer = M3Secondary90,
    onSecondaryContainer = M3Secondary10,
    tertiary = M3Tertiary40,
    onTertiary = M3Neutral100,
    tertiaryContainer = M3Tertiary90,
    onTertiaryContainer = M3Tertiary10,
    error = M3Error40,
    onError = M3Neutral100,
    errorContainer = M3Error90,
    onErrorContainer = M3Error10,
    background = M3Neutral99,
    onBackground = M3Neutral10,
    surface = M3Neutral99,
    onSurface = M3Neutral10,
    surfaceVariant = M3NeutralVariant90,
    onSurfaceVariant = M3NeutralVariant30,
    outline = M3NeutralVariant50,
    outlineVariant = M3NeutralVariant80,
    inverseSurface = M3Neutral20,
    inverseOnSurface = M3Neutral95,
    inversePrimary = M3Primary80,
    surfaceTint = M3Primary40,
    surfaceDim = M3Neutral87,
    surfaceBright = M3Neutral98,
    surfaceContainerLowest = M3Neutral100,
    surfaceContainerLow = M3Neutral96,
    surfaceContainer = M3Neutral94,
    surfaceContainerHigh = M3Neutral92,
    surfaceContainerHighest = M3Neutral90,
)

internal val FishPiDarkColorScheme = darkColorScheme(
    primary = M3Primary80,
    onPrimary = M3Primary20,
    primaryContainer = M3Primary30,
    onPrimaryContainer = M3Primary90,
    secondary = M3Secondary80,
    onSecondary = M3Secondary20,
    secondaryContainer = M3Secondary30,
    onSecondaryContainer = M3Secondary90,
    tertiary = M3Tertiary80,
    onTertiary = M3Tertiary20,
    tertiaryContainer = M3Tertiary30,
    onTertiaryContainer = M3Tertiary90,
    error = M3Error80,
    onError = M3Error20,
    errorContainer = FishPiErrorRed,
    onErrorContainer = M3Error90,
    background = M3Neutral6,
    onBackground = M3Neutral90,
    surface = M3Neutral6,
    onSurface = M3Neutral90,
    surfaceVariant = M3NeutralVariant30,
    onSurfaceVariant = M3NeutralVariant80,
    outline = M3NeutralVariant60,
    outlineVariant = M3NeutralVariant30,
    inverseSurface = M3Neutral90,
    inverseOnSurface = M3Neutral20,
    inversePrimary = M3Primary40,
    surfaceTint = M3Primary80,
    surfaceDim = M3Neutral6,
    surfaceBright = M3Neutral24,
    surfaceContainerLowest = M3Neutral4,
    surfaceContainerLow = M3Neutral10,
    surfaceContainer = M3Neutral12,
    surfaceContainerHigh = M3Neutral17,
    surfaceContainerHighest = M3Neutral22,
)

// ─────────────────────────────────────────────────────────────
// Material 3 Theme Composable
// Supports: Light / Dark / Dynamic Color (Android 12+ Material You)
// ─────────────────────────────────────────────────────────────

@Composable
internal fun FishPiM3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> FishPiDarkColorScheme
        else -> FishPiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

// ─────────────────────────────────────────────────────────────
// Bridge: FishPiPalette → M3 ColorScheme
// ─────────────────────────────────────────────────────────────

private fun FishPiPalette.isDarkPalette(): Boolean {
    val lum = background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f
    return lum < 0.5f
}

internal fun FishPiThemeTokens.toM3ColorScheme(palette: FishPiPalette): androidx.compose.material3.ColorScheme {
    val c = colors
    val isDark = colorScheme == FishPiThemeColorScheme.Dark || palette.isDarkPalette()
    return if (isDark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.primaryContent,
            primaryContainer = c.base300,
            onPrimaryContainer = c.baseContent,
            secondary = c.secondary,
            onSecondary = c.secondaryContent,
            secondaryContainer = c.base300,
            onSecondaryContainer = c.baseContent,
            tertiary = c.accent,
            onTertiary = c.accentContent,
            tertiaryContainer = c.base300,
            onTertiaryContainer = c.baseContent,
            error = c.error,
            onError = M3Neutral100,
            errorContainer = c.error.copy(alpha = 0.22f),
            onErrorContainer = c.error,
            background = palette.background,
            onBackground = c.baseContent,
            surface = palette.surface,
            onSurface = c.baseContent,
            surfaceVariant = palette.surfaceContainer,
            onSurfaceVariant = c.neutral,
            outline = palette.outline,
            outlineVariant = palette.outline.copy(alpha = 0.62f),
            inverseSurface = c.primary,
            inverseOnSurface = c.primaryContent,
            inversePrimary = c.primaryContent,
            surfaceTint = c.primary,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = c.base200,
            surfaceContainer = c.base300,
            surfaceContainerHigh = c.base300,
            surfaceContainerHighest = c.base300,
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.primaryContent,
            primaryContainer = c.base300,
            onPrimaryContainer = c.baseContent,
            secondary = c.secondary,
            onSecondary = c.secondaryContent,
            secondaryContainer = c.base300,
            onSecondaryContainer = c.baseContent,
            tertiary = c.accent,
            onTertiary = c.accentContent,
            tertiaryContainer = c.base300,
            onTertiaryContainer = c.baseContent,
            error = c.error,
            onError = M3Neutral100,
            errorContainer = c.error.copy(alpha = 0.14f),
            onErrorContainer = c.error,
            background = palette.background,
            onBackground = c.baseContent,
            surface = palette.surface,
            onSurface = c.baseContent,
            surfaceVariant = palette.surfaceContainer,
            onSurfaceVariant = c.neutral,
            outline = palette.outline,
            outlineVariant = palette.outline.copy(alpha = 0.62f),
            inverseSurface = c.baseContent,
            inverseOnSurface = c.base100,
            inversePrimary = c.primaryContent,
            surfaceTint = c.primary,
            surfaceContainerLowest = c.base200,
            surfaceContainerLow = c.base200,
            surfaceContainer = c.base300,
            surfaceContainerHigh = c.base200,
            surfaceContainerHighest = c.base300,
        )
    }
}

internal fun FishPiPalette.toM3ColorScheme(): androidx.compose.material3.ColorScheme {
    val tokens = if (isDarkPalette()) IslandFishPiThemeTokens else DeepBlueNeonFishPiThemeTokens
    return tokens.toM3ColorScheme(this)
}

// ─────────────────────────────────────────────────────────────
// M3 + FishPiPalette bridged theme
// Provides both MaterialTheme and LocalFishPiPalette
// ─────────────────────────────────────────────────────────────

@Composable
internal fun FishPiM3BridgedTheme(
    palette: FishPiPalette,
    tokens: FishPiThemeTokens = IslandFishPiThemeTokens,
    uiStyle: FishPiUiStyle = FishPiUiStyle.Classic,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val paletteIsDark = remember(palette) { palette.isDarkPalette() }
    val colorScheme = remember(palette, tokens, dynamicColor, context, paletteIsDark) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (paletteIsDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            tokens.toM3ColorScheme(palette)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalFishPiPalette provides palette,
        LocalFishPiThemeTokens provides tokens,
        LocalFishPiUiStyle provides uiStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
