package dev.fishpi.mobile.feature.chat.barrage

data class ChatBarrageUiModel(
    val id: String,
    val author: String,
    val avatarUrl: String,
    val content: String,
    val color: String,
    val createdAtMs: Long,
)

data class ChatBarrageComposerState(
    val open: Boolean = false,
    val input: String = "",
    val costLabel: String = "发送弹幕会消耗积分",
    val isLoadingCost: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
)
