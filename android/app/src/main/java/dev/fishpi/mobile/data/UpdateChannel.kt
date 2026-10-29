package dev.fishpi.mobile.data

enum class UpdateChannel(val key: String, val label: String) {
    Stable("stable", "稳定版"),
    Rc("rc", "候选版（RC）");

    companion object {
        fun fromKey(raw: String?): UpdateChannel =
            entries.firstOrNull { it.key == raw?.trim() } ?: Stable
    }
}
