package dev.fishpi.mobile.feature.redpacket

import dev.fishpi.mobile.data.RedPacketGot
import dev.fishpi.mobile.data.RedPacketOpenResult
import dev.fishpi.mobile.data.RedPacketPreview

fun RedPacketPreview.toRedPacketUiModel(): RedPacketPreviewUiModel =
    RedPacketPreviewUiModel(
        type = type,
        typeName = typeName,
        money = money,
        count = count,
        got = got,
        message = message,
        summary = summary,
        finished = finished,
        openable = openable,
        needGesture = needGesture,
        receivers = receivers,
        gesture = gesture,
    )

fun RedPacketGot.toRedPacketUiModel(): RedPacketGotUiModel =
    RedPacketGotUiModel(
        userId = userId,
        userName = userName,
        avatar = avatar,
        userMoney = userMoney,
        time = time,
    )

fun RedPacketGotUiModel.toLegacyRedPacketGot(): RedPacketGot =
    RedPacketGot(
        userId = userId,
        userName = userName,
        avatar = avatar,
        userMoney = userMoney,
        time = time,
    )

fun RedPacketOpenResult.toRedPacketResultUiModel(
    senderName: String,
    senderAvatar: String,
    packetMessage: String,
    selfUsername: String,
    finished: Boolean,
): RedPacketResultUiModel =
    RedPacketResultUiModel(
        message = message,
        count = count,
        got = got,
        gesture = gesture,
        who = who.map { it.toRedPacketUiModel() },
        senderName = this.senderName.ifBlank { senderName },
        senderAvatar = this.senderAvatar.ifBlank { senderAvatar },
        packetMessage = packetMessage,
        selfUsername = selfUsername,
        finished = finished,
    )
