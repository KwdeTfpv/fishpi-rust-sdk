package dev.fishpi.mobile.feature.redpacket

data class RedPacketPreviewUiModel(
    val type: String,
    val typeName: String,
    val money: Long,
    val count: Long,
    val got: Long,
    val message: String,
    val summary: String,
    val finished: Boolean,
    val openable: Boolean,
    val needGesture: Boolean,
    val receivers: List<String> = emptyList(),
    val gesture: Int? = null,
)

data class RedPacketGotUiModel(
    val userId: String = "",
    val userName: String,
    val avatar: String,
    val userMoney: Long,
    val time: String,
)

data class RedPacketResultUiModel(
    val message: String,
    val count: Long,
    val got: Long,
    val gesture: Int? = null,
    val who: List<RedPacketGotUiModel>,
    val senderName: String,
    val senderAvatar: String,
    val packetMessage: String,
    val selfUsername: String,
    val finished: Boolean,
)

data class RedPacketGestureUiModel(
    val value: Int,
    val label: String,
    val emoji: String,
)

val RedPacketGestureOptions = listOf(
    RedPacketGestureUiModel(RedPacketGestureRock, "石头", "✊"),
    RedPacketGestureUiModel(RedPacketGestureScissors, "剪刀", "✌"),
    RedPacketGestureUiModel(RedPacketGesturePaper, "布", "✋"),
)

fun redPacketGestureLabel(value: Int): String =
    RedPacketGestureOptions.firstOrNull { it.value == value }?.label ?: "未知"

fun redPacketGestureEmoji(value: Int): String =
    RedPacketGestureOptions.firstOrNull { it.value == value }?.emoji ?: "🧧"
