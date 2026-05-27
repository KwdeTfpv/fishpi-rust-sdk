package dev.fishpi.mobile.plugin

internal data class PluginHeader(
    val name: String,
    val author: String = "",
    val version: String = "0.0.1",
    val scenes: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
)

internal object PluginHeaderParser {
    private val META_RE = Regex("""^//\s*@(\w+)\s+(.+)$""", setOf(RegexOption.MULTILINE))

    fun parse(script: String): PluginHeader? {
        val end = script.indexOf("==/FishPiPlugin==")
        val start = script.indexOf("==FishPiPlugin==")
        if (start < 0 || end < 0) return null
        val block = script.substring(start, end)
        val meta = META_RE.findAll(block).associate { it.groupValues[1] to it.groupValues[2].trim() }
        return PluginHeader(
            name = meta["name"] ?: return null,
            author = meta["author"] ?: "",
            version = meta["version"] ?: "0.0.1",
            scenes = meta["scenes"]?.split(",")?.map { it.trim() } ?: emptyList(),
            permissions = meta["permissions"]?.split(",")?.map { it.trim() } ?: emptyList(),
        )
    }
}
