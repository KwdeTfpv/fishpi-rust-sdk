package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatRoomMessage

internal fun ChatRoomMessage.toMessageUiModel(currentUsername: String = ""): MessageUiModel =
    MessageUiModel(
        id = oId,
        authorUsername = userName,
        authorDisplayName = displayName,
        authorAvatarUrl = userAvatarURL,
        content = content,
        markdown = md,
        html = contentHtml,
        imageUrls = imageUrls,
        linkUrls = linkUrls,
        time = time,
        client = client,
        kind = type.toMessageKind(),
        isMine = currentUsername.isNotBlank() && userName.equals(currentUsername, ignoreCase = true),
        isRevoked = revoked,
        reactions = reactionSummary.map {
            MessageReactionUiModel(
                value = it.value,
                emoji = it.emoji,
                count = it.count,
                selected = it.selected,
            )
        },
        redPacket = redPacket?.let {
            MessageRedPacketUiModel(
                type = it.type,
                typeName = it.typeName,
                money = it.money,
                count = it.count,
                got = it.got,
                message = it.message,
                finished = it.finished,
                openable = it.openable,
                needGesture = it.needGesture,
            )
        },
        legacyChatRoomMessage = this,
    )

internal fun ChatListItem.toMessageUiModel(currentUsername: String = ""): MessageUiModel =
    message.toMessageUiModel(currentUsername)

private fun String.toMessageKind(): MessageKind = when (trim().lowercase()) {
    "sys", "system" -> MessageKind.System
    "custom" -> MessageKind.Custom
    else -> MessageKind.Message
}



