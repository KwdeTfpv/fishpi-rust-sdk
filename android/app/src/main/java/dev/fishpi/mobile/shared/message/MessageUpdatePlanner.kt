package dev.fishpi.mobile.shared.message

internal enum class MessageUpdateKind { Full, AppearanceOnly, ReactionsOnly, AvatarOnly }

internal object MessageUpdatePlanner {

    fun plan(previous: ChatListItem?, next: ChatListItem, sameContext: Boolean): MessageUpdateKind {
        if (previous == null || !sameContext) return MessageUpdateKind.Full
        if (previous.stableNativeKey() != next.stableNativeKey()) return MessageUpdateKind.Full
        if (previous.message.appearance != next.message.appearance) {
            if (previous.message.content != next.message.content) return MessageUpdateKind.Full
            if (previous.message.contentKind != next.message.contentKind) return MessageUpdateKind.Full
            val sameOtherFields =
                previous.copy(message = previous.message.copy(appearance = null)) ==
                    next.copy(message = next.message.copy(appearance = null))
            return if (sameOtherFields) MessageUpdateKind.AppearanceOnly else MessageUpdateKind.Full
        }
        if (previous.message.reactionSummary != next.message.reactionSummary) {
            val sameOtherFields =
                previous.copy(message = previous.message.copy(reactionSummary = emptyList())) ==
                    next.copy(message = next.message.copy(reactionSummary = emptyList()))
            return if (sameOtherFields) MessageUpdateKind.ReactionsOnly else MessageUpdateKind.Full
        }
        if (previous.message.userAvatarURL != next.message.userAvatarURL) {
            val sameOtherFields =
                previous.copy(message = previous.message.copy(userAvatarURL = "")) ==
                    next.copy(message = next.message.copy(userAvatarURL = ""))
            return if (sameOtherFields) MessageUpdateKind.AvatarOnly else MessageUpdateKind.Full
        }
        return MessageUpdateKind.Full
    }
}

internal fun ChatListItem.stableNativeKey(): String =
    echoKey.ifBlank {
        message.oId.ifBlank { "${message.time}:${message.userName}:${message.content.hashCode()}" }
    }
