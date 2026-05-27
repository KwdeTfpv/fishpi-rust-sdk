package dev.fishpi.mobile.feature.chat.mapper

import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.feature.chat.model.ChatMessageKind
import dev.fishpi.mobile.feature.chat.model.ChatMessageUiModel
import dev.fishpi.mobile.feature.chat.model.ChatQuoteUiModel
import dev.fishpi.mobile.feature.chat.model.ChatReactionUiModel
import dev.fishpi.mobile.feature.chat.model.ChatRedPacketUiModel

internal fun ChatRoomMessage.toChatMessageUiModel(
    currentUsername: String = "",
): ChatMessageUiModel = ChatMessageUiModel(
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
    kind = type.toChatMessageKind(),
    isMine = currentUsername.isNotBlank() && userName.equals(currentUsername, ignoreCase = true),
    isRevoked = revoked,
    reactions = reactionSummary.map {
        ChatReactionUiModel(
            value = it.value,
            emoji = it.emoji,
            count = it.count,
            selected = it.selected,
        )
    },
    currentUserReaction = currentUserReaction,
    quote = quote?.let {
        ChatQuoteUiModel(
            text = it.text,
            imageUrls = it.imageUrls,
        )
    },
    redPacket = redPacket?.let {
        ChatRedPacketUiModel(
            type = it.type,
            typeName = it.typeName,
            money = it.money,
            count = it.count,
            got = it.got,
            message = it.message,
            summary = it.summary,
            finished = it.finished,
            openable = it.openable,
            needGesture = it.needGesture,
            receivers = it.receivers,
            gesture = it.gesture,
        )
    },
)

internal fun List<ChatRoomMessage>.toChatMessageUiModels(
    currentUsername: String = "",
): List<ChatMessageUiModel> = map { it.toChatMessageUiModel(currentUsername) }

private fun String.toChatMessageKind(): ChatMessageKind = when (trim().lowercase()) {
    "sys", "system" -> ChatMessageKind.System
    "custom" -> ChatMessageKind.Custom
    else -> ChatMessageKind.Message
}
