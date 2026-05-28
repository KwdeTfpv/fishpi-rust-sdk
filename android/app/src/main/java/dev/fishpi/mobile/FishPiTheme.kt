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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

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
    val palette: FishPiPalette,
    val rawJson: String,
)

internal data class FishPiThemeOption(
    val key: String,
    val label: String,
    val description: String,
    val palette: FishPiPalette,
    val uiStyle: FishPiUiStyle = FishPiUiStyle.Classic,
    val builtinPreset: FishPiThemePreset? = null,
    val rawJson: String? = null,
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

private val ThemeKeySanitizeRegex = Regex("[^a-z0-9_-]+")

internal val IslandFishPiPalette = FishPiPalette(
    background = Color(0xFF121212),
    chatBackground = Color(0xFF101010),
    wallpaperColors = listOf(
        Color(0xFF0B0B0B),
        Color(0xFF121212),
        Color(0xFF1A1A1A),
    ),
    surface = Color(0xFF1E1E1E),
    surfaceElevated = Color(0xFF252525),
    surfaceContainer = Color(0xFF2C2C2C),
    onSurface = Color(0xFFEDEDED),
    weakText = Color(0xFFB0B0B0),
    userName = Color(0xFFD6D6D6),
    clientText = Color(0xFFA8A8A8),
    clientBackground = Color(0xFF303030),
    timeText = Color(0xFF8E8E8E),
    outline = Color(0xFF3A3A3A),
    accent = Color(0xFFE0E0E0),
    quoteBackground = Color(0xFF2A2A2A),
    outgoingBubble = Color(0xFF3A3A3A),
    incomingBubble = Color(0xFF242424),
    linkText = Color(0xFFE0E0E0),
    quoteText = Color(0xFFC7C7C7),
    quoteLine = Color(0xFF5A5A5A),
    toolDefault = Color(0xFFD0D0D0),
    toolGallery = Color(0xFFD0D0D0),
    toolCamera = Color(0xFFD0D0D0),
    toolRedPacket = Color(0xFFFF5252),
)

internal val DeepBlueNeonFishPiPalette = FishPiPalette(
    background = Color(0xFFF3F8FF),
    chatBackground = Color(0xFFEDF5FF),
    wallpaperColors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF3F8FF),
        Color(0xFFEAF4FF),
    ),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF8FBFF),
    surfaceContainer = Color(0xFFE9F2FF),
    onSurface = Color(0xFF08233F),
    weakText = Color(0xFF5D7188),
    userName = Color(0xFF123E69),
    clientText = Color(0xFF526A82),
    clientBackground = Color(0xFFEAF2FA),
    timeText = Color(0xFF6B7D90),
    outline = Color(0xFFC9D8E8),
    accent = Color(0xFF08233F),
    quoteBackground = Color(0xFFF0F8FF),
    outgoingBubble = Color(0xFFE8FBE7),
    incomingBubble = Color(0xFFFFFFFF),
    linkText = Color(0xFF0B5C93),
    quoteText = Color(0xFF526A82),
    quoteLine = Color(0xFF7CFF52),
    toolDefault = Color(0xFF7CFF52),
    toolGallery = Color(0xFF42D94D),
    toolCamera = Color(0xFF0B5C93),
    toolRedPacket = Color(0xFFE53935),
)

internal val LocalFishPiPalette = compositionLocalOf { IslandFishPiPalette }
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
    val linkText: Color @Composable get() = LocalFishPiPalette.current.linkText
    val toolGallery: Color @Composable get() = LocalFishPiPalette.current.toolGallery
    val toolCamera: Color @Composable get() = LocalFishPiPalette.current.toolCamera
    val toolRedPacket: Color @Composable get() = LocalFishPiPalette.current.toolRedPacket
    val uiStyle: FishPiUiStyle @Composable get() = LocalFishPiUiStyle.current
}

internal fun builtinThemeOptions(): List<FishPiThemeOption> =
    listOf(FishPiThemePreset.DeepBlueNeon, FishPiThemePreset.Island).map { preset ->
        FishPiThemeOption(
            key = preset.key,
            label = preset.label,
            description = preset.themeDescription(),
            palette = preset.previewPalette(),
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
            palette = custom.palette,
            rawJson = custom.rawJson,
        )
    }

internal fun FishPiThemePreset.previewPalette(): FishPiPalette =
    when (this) {
        FishPiThemePreset.Island -> IslandFishPiPalette
        FishPiThemePreset.DeepBlueNeon -> DeepBlueNeonFishPiPalette
    }

internal fun FishPiThemePreset.uiStyle(): FishPiUiStyle =
    FishPiUiStyle.Classic

internal fun FishPiThemePreset.themeDescription(): String =
    when (this) {
        FishPiThemePreset.Island -> "通用深色背景，中性灰层级与高对比文字"
        FishPiThemePreset.DeepBlueNeon -> "深蓝主色，荧光绿强调，白色内容层"
    }

internal fun parseCustomFishPiTheme(rawJson: String): CustomFishPiTheme {
    val json = JSONObject(rawJson)
    val colors = json.optJSONObject("colors") ?: json
    val name = json.optString("name").ifBlank { json.optString("label").ifBlank { "导入主题" } }
    val base = IslandFishPiPalette
    val palette = FishPiPalette(
        background = colors.optThemeColor("background", base.background),
        chatBackground = colors.optThemeColor("chatBackground", base.chatBackground),
        wallpaperColors = json.optWallpaperColors(colors, base.wallpaperColors),
        wallpaperImageUri = json.optJSONObject("wallpaper")?.optString("image").orEmpty().ifBlank { null },
        surface = colors.optThemeColor("surface", base.surface),
        surfaceElevated = colors.optThemeColor("surfaceElevated", base.surfaceElevated),
        surfaceContainer = colors.optThemeColor("surfaceContainer", base.surfaceContainer),
        onSurface = colors.optThemeColor("onSurface", base.onSurface),
        weakText = colors.optThemeColor("weakText", base.weakText),
        userName = colors.optThemeColor("userName", base.userName),
        clientText = colors.optThemeColor("clientText", base.clientText),
        clientBackground = colors.optThemeColor("clientBackground", base.clientBackground),
        timeText = colors.optThemeColor("timeText", base.timeText),
        outline = colors.optThemeColor("outline", base.outline),
        accent = colors.optThemeColor("accent", base.accent),
        quoteBackground = colors.optThemeColor("quoteBackground", base.quoteBackground),
        outgoingBubble = colors.optThemeColor("outgoingBubble", base.outgoingBubble),
        incomingBubble = colors.optThemeColor("incomingBubble", base.incomingBubble),
        linkText = colors.optThemeColor("linkText", base.linkText),
        quoteText = colors.optThemeColor("quoteText", base.quoteText),
        quoteLine = colors.optThemeColor("quoteLine", base.quoteLine),
        toolDefault = colors.optThemeColor("toolDefault", base.toolDefault),
        toolGallery = colors.optThemeColor("toolGallery", base.toolGallery),
        toolCamera = colors.optThemeColor("toolCamera", base.toolCamera),
        toolRedPacket = colors.optThemeColor("toolRedPacket", base.toolRedPacket),
    )
    val explicitKey = json.optString("key").ifBlank { json.optString("id") }
    val key = "custom:" + explicitKey.ifBlank { "${name}-${rawJson.hashCode()}" }
        .lowercase()
        .replace(ThemeKeySanitizeRegex, "-")
        .trim('-')
    return CustomFishPiTheme(
        key = key,
        label = name,
        description = json.optString("description").ifBlank { "外部导入主题" },
        palette = palette,
        rawJson = rawJson,
    )
}

internal fun buildEditableThemeJson(
    label: String,
    description: String,
    palette: FishPiPalette,
): String {
    val colors = JSONObject()
        .put("background", palette.background.toThemeHex())
        .put("chatBackground", palette.chatBackground.toThemeHex())
        .put("surface", palette.surface.toThemeHex())
        .put("surfaceElevated", palette.surfaceElevated.toThemeHex())
        .put("surfaceContainer", palette.surfaceContainer.toThemeHex())
        .put("onSurface", palette.onSurface.toThemeHex())
        .put("weakText", palette.weakText.toThemeHex())
        .put("userName", palette.userName.toThemeHex())
        .put("clientText", palette.clientText.toThemeHex())
        .put("clientBackground", palette.clientBackground.toThemeHex())
        .put("timeText", palette.timeText.toThemeHex())
        .put("outline", palette.outline.toThemeHex())
        .put("accent", palette.accent.toThemeHex())
        .put("quoteBackground", palette.quoteBackground.toThemeHex())
        .put("outgoingBubble", palette.outgoingBubble.toThemeHex())
        .put("incomingBubble", palette.incomingBubble.toThemeHex())
        .put("linkText", palette.linkText.toThemeHex())
        .put("quoteText", palette.quoteText.toThemeHex())
        .put("quoteLine", palette.quoteLine.toThemeHex())
        .put("toolDefault", palette.toolDefault.toThemeHex())
        .put("toolGallery", palette.toolGallery.toThemeHex())
        .put("toolCamera", palette.toolCamera.toThemeHex())
        .put("toolRedPacket", palette.toolRedPacket.toThemeHex())
    val wallpaper = JSONObject()
        .put("colors", JSONArray().apply {
            palette.wallpaperColors.forEach { put(it.toThemeHex()) }
        })
    palette.wallpaperImageUri?.let { wallpaper.put("image", it) }
    return JSONObject()
        .put("name", label.ifBlank { "应用内主题" })
        .put("description", description.ifBlank { "应用内编辑主题" })
        .put("colors", colors)
        .put("wallpaper", wallpaper)
        .toString()
}

internal fun Color.toThemeHex(): String =
    "#%06X".format(0xFFFFFF and toArgb())

private fun JSONObject.optThemeColor(name: String, fallback: Color): Color {
    val value = optString(name).trim()
    if (value.isBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(value))
    }.getOrDefault(fallback)
}

private fun JSONObject.optWallpaperColors(colors: JSONObject, fallback: List<Color>): List<Color> {
    val wallpaper = optJSONObject("wallpaper")
    val array = wallpaper?.optJSONArray("colors") ?: colors.optJSONArray("wallpaper")
    if (array == null || array.length() == 0) return fallback
    val parsed = buildList {
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotBlank()) {
                runCatching { Color(android.graphics.Color.parseColor(value)) }
                    .onSuccess { add(it) }
            }
        }
    }
    return parsed.ifEmpty { fallback }
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

internal fun FishPiPalette.toM3ColorScheme(): androidx.compose.material3.ColorScheme {
    return if (isDarkPalette()) {
        darkColorScheme(
            primary = accent,
            onPrimary = M3Neutral100,
            primaryContainer = accent.copy(alpha = 0.2f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = M3Neutral100,
            tertiary = M3Tertiary40,
            onTertiary = M3Neutral100,
            error = FishPiErrorRed,
            onError = M3Neutral100,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceContainer,
            onSurfaceVariant = weakText,
            outline = outline,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceElevated,
            surfaceContainerHighest = surfaceElevated,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = M3Neutral100,
            primaryContainer = accent.copy(alpha = 0.12f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = M3Neutral100,
            tertiary = M3Tertiary40,
            onTertiary = M3Neutral100,
            error = FishPiErrorRed,
            onError = M3Neutral100,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceContainer,
            onSurfaceVariant = weakText,
            outline = outline,
            surfaceContainerLowest = M3Neutral100,
            surfaceContainerLow = M3Neutral96,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceElevated,
            surfaceContainerHighest = M3Neutral90,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// M3 + FishPiPalette bridged theme
// Provides both MaterialTheme and LocalFishPiPalette
// ─────────────────────────────────────────────────────────────

@Composable
internal fun FishPiM3BridgedTheme(
    palette: FishPiPalette,
    uiStyle: FishPiUiStyle = FishPiUiStyle.Classic,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val paletteIsDark = remember(palette) { palette.isDarkPalette() }
    val colorScheme = remember(palette, dynamicColor, context, paletteIsDark) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (paletteIsDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            palette.toM3ColorScheme()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalFishPiPalette provides palette,
        LocalFishPiUiStyle provides uiStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
