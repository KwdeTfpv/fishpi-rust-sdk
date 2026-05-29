package dev.fishpi.mobile

import android.content.Context
import android.net.Uri
import dev.fishpi.mobile.utils.uriDisplayName
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

private const val THEME_PACKAGE_SCHEMA = 1
private const val THEME_PREVIEW_TEMPLATE = "fishpi-mobile-v1"
private val ThemePackageImageExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "svg")

internal fun importFishPiThemePackage(context: Context, uriString: String): CustomFishPiTheme {
    val uri = Uri.parse(uriString)
    val displayName = context.uriDisplayName(uri).orEmpty()
    require(displayName.lowercase(Locale.US).endsWith(".fpt")) {
        "请选择 FishPi 主题包（.fpt）"
    }

    val tempFile = File.createTempFile("fishpi-theme-", ".fpt", File(context.cacheDir, "theme_imports").apply { mkdirs() })
    try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取主题包" }
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        return importFishPiThemePackageFile(context, tempFile)
    } finally {
        tempFile.delete()
    }
}

internal fun deleteFishPiThemePackageFiles(context: Context, themeKey: String) {
    val themesRoot = File(context.filesDir, "themes").canonicalFile
    val themeDir = File(themesRoot, themeKey.removePrefix("custom:")).canonicalFile
    if (themeDir.path.startsWith(themesRoot.path)) {
        themeDir.deleteRecursively()
    }
}

private fun importFishPiThemePackageFile(context: Context, packageFile: File): CustomFishPiTheme {
    ZipFile(packageFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            if (!entry.isDirectory) require(isSafeThemePackagePath(entry.name)) { "主题包包含非法路径：${entry.name}" }
        }
        val themeEntry = zip.getEntry("theme.json") ?: error("主题包缺少 theme.json")
        val rawThemeJson = zip.getInputStream(themeEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(rawThemeJson)
        require(json.optInt("schema", -1) == THEME_PACKAGE_SCHEMA) { "主题 schema 需要为 1" }
        require(json.optString("previewTemplate") == THEME_PREVIEW_TEMPLATE) {
            "主题预览模板需要为 $THEME_PREVIEW_TEMPLATE"
        }

        val initialTheme = parseCustomFishPiTheme(json.toString())
        val themeDir = File(context.filesDir, "themes/${initialTheme.key.removePrefix("custom:")}").canonicalFile
        val themesRoot = File(context.filesDir, "themes").canonicalFile.apply { mkdirs() }
        require(themeDir.path.startsWith(themesRoot.path)) { "主题目录不合法" }
        themeDir.deleteRecursively()
        themeDir.mkdirs()

        resolvePackagedWallpaper(zip, json, themeDir)
        resolvePackagedPreviews(zip, json, themeDir)

        return parseCustomFishPiTheme(json.toString())
    }
}

private fun resolvePackagedWallpaper(zip: ZipFile, json: JSONObject, themeDir: File) {
    val wallpaper = json.optJSONObject("wallpaper") ?: return
    val image = wallpaper.optString("image").trim()
    if (image.isBlank() || image.isExternalThemeResource()) return
    val copied = copyThemePackageImage(zip, image, File(themeDir, "assets"))
    wallpaper.put("image", Uri.fromFile(copied).toString())
}

private fun resolvePackagedPreviews(zip: ZipFile, json: JSONObject, themeDir: File) {
    val previews = JSONObject()
    listOf("chat", "chatroom", "home", "article", "profile").forEach { key ->
        val path = "previews/$key.png"
        if (zip.getEntry(path) != null) {
            val copied = copyThemePackageImage(zip, path, File(themeDir, "previews"))
            previews.put(key, Uri.fromFile(copied).toString())
        }
    }
    if (previews.length() > 0) {
        json.put("previews", previews)
    }
}

private fun copyThemePackageImage(zip: ZipFile, path: String, targetDir: File): File {
    val normalized = path.normalizeThemePackagePath()
    require(isSafeThemePackagePath(normalized)) { "主题包包含非法路径：$path" }
    val entry = zip.getEntry(normalized) ?: error("主题包缺少资源：$normalized")
    require(!entry.isDirectory) { "主题资源不是文件：$normalized" }
    val extension = normalized.substringAfterLast('.', "").lowercase(Locale.US)
    require(extension in ThemePackageImageExtensions) { "不支持的主题图片格式：$extension" }
    targetDir.mkdirs()
    val target = File(targetDir, File(normalized).name).canonicalFile
    require(target.path.startsWith(targetDir.canonicalPath)) { "主题资源路径不合法" }
    zip.getInputStream(entry).use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    }
    return target
}

private fun String.isExternalThemeResource(): Boolean =
    startsWith("https://", ignoreCase = true) ||
        startsWith("http://", ignoreCase = true) ||
        startsWith("file://", ignoreCase = true) ||
        startsWith("content://", ignoreCase = true)

private fun String.normalizeThemePackagePath(): String =
    replace('\\', '/').trim().trimStart('/')

private fun isSafeThemePackagePath(path: String): Boolean {
    val normalized = path.normalizeThemePackagePath()
    return normalized.isNotBlank() &&
        !normalized.startsWith("/") &&
        !normalized.startsWith("../") &&
        !normalized.contains("/../") &&
        !normalized.contains('\u0000')
}
