package dev.fishpi.mobile.feature.redpacket

data class RedPacketFormState(
    val type: String = RedPacketTypeRandom,
    val money: String = "32",
    val count: String = "1",
    val message: String = DefaultRedPacketMessage,
    val receivers: String = "",
    val gesture: Int = RedPacketGestureRock,
    val balance: Long? = null,
    val isSending: Boolean = false,
    val moneyError: String? = null,
    val countError: String? = null,
    val receiversError: String? = null,
)

const val RedPacketTypeRandom = "random"
const val RedPacketTypeAverage = "average"
const val RedPacketTypeSpecify = "specify"
const val RedPacketTypeHeartbeat = "heartbeat"
const val RedPacketTypeRockPaperScissors = "rockPaperScissors"

const val RedPacketGestureRock = 0
const val RedPacketGestureScissors = 1
const val RedPacketGesturePaper = 2

const val DefaultRedPacketMessage = "摸鱼者，事竟成"
const val DefaultGestureRedPacketMessage = "剪刀石头布!"
const val DefaultSpecifyRedPacketMessage = "看看是不是给你的"

fun defaultRedPacketMessage(type: String): String =
    when (type) {
        RedPacketTypeRockPaperScissors -> DefaultGestureRedPacketMessage
        RedPacketTypeSpecify -> DefaultSpecifyRedPacketMessage
        else -> DefaultRedPacketMessage
    }
