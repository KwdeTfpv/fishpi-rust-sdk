package dev.fishpi.mobile.feature.redpacket

fun redPacketGestureResultText(
    who: List<RedPacketGotUiModel>,
    senderName: String,
    senderGestureLabel: String,
    selfUsername: String,
    isSelfSender: Boolean,
): String {
    val senderLabel = senderName.ifBlank { "发红包的人" }
    val gestureText = senderGestureLabel.takeIf { it.isNotBlank() && it != "未知" }
        ?.let { "出$it" }
        ?: "出拳"
    if (who.isEmpty()) return "$senderLabel $gestureText，等待大家出手气"

    fun receiverName(record: RedPacketGotUiModel): String =
        record.userName.ifBlank { "对方" }

    if (isSelfSender) {
        val latest = who.lastOrNull()
        if (latest != null && who.size == 1) {
            return when {
                latest.userMoney > 0L -> "你$gestureText，${receiverName(latest)} 赢了 ${latest.userMoney} 积分，你输了"
                latest.userMoney < 0L -> "你$gestureText，${receiverName(latest)} 输了 ${kotlin.math.abs(latest.userMoney)} 积分，你赢了"
                else -> "你$gestureText，和 ${receiverName(latest)} 打平"
            }
        }
        return senderSummaryText(who = who, senderLabel = "你", gestureText = gestureText)
    }

    val selfRecord = who.firstOrNull { item ->
        item.userName.equals(selfUsername, ignoreCase = true)
    }
    if (selfRecord != null) {
        return when {
            selfRecord.userMoney > 0L -> "$senderLabel $gestureText，输了 ${selfRecord.userMoney} 积分，你赢了"
            selfRecord.userMoney < 0L -> "$senderLabel $gestureText，赢了 ${kotlin.math.abs(selfRecord.userMoney)} 积分，你输了"
            else -> "$senderLabel $gestureText，双方打平"
        }
    }

    val latest = who.lastOrNull()
    if (latest != null && who.size == 1) {
        return when {
            latest.userMoney > 0L -> "$senderLabel $gestureText，${receiverName(latest)} 赢了 ${latest.userMoney} 积分"
            latest.userMoney < 0L -> "$senderLabel $gestureText，${receiverName(latest)} 输了 ${kotlin.math.abs(latest.userMoney)} 积分"
            else -> "$senderLabel $gestureText，和 ${receiverName(latest)} 打平"
        }
    }
    return senderSummaryText(who = who, senderLabel = senderLabel, gestureText = gestureText)
}

private fun senderSummaryText(
    who: List<RedPacketGotUiModel>,
    senderLabel: String,
    gestureText: String,
): String {
    val senderLost = who.filter { it.userMoney > 0L }.sumOf { it.userMoney }
    val senderWon = who.filter { it.userMoney < 0L }.sumOf { kotlin.math.abs(it.userMoney) }
    val tied = who.count { it.userMoney == 0L }
    return when {
        senderLost > 0L && senderWon > 0L -> "$senderLabel $gestureText，赢了 $senderWon 积分，输了 $senderLost 积分"
        senderLost > 0L -> "$senderLabel $gestureText，输了 $senderLost 积分"
        senderWon > 0L -> "$senderLabel $gestureText，赢了 $senderWon 积分"
        tied > 0 -> "$senderLabel $gestureText，${tied} 次打平"
        else -> "$senderLabel $gestureText，等待大家出手气"
    }
}

fun redPacketNormalResultText(
    who: List<RedPacketGotUiModel>,
    selfUsername: String,
): String {
    val selfRecord = who.firstOrNull { it.userName.equals(selfUsername, ignoreCase = true) }
    return when {
        selfRecord != null -> "你抢到 ${selfRecord.userMoney} 积分"
        who.isEmpty() -> "暂时还没有领取记录"
        else -> "已领取 ${who.size} 条记录"
    }
}
