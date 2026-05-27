package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.shared.message.RepeatStackInfo


internal data class MessageRepeatStackUiModel(
    val count: Int,
    val participantUsernames: List<String>,
    val participantAvatars: List<String>,
    val legacyRepeatStack: RepeatStackInfo? = null,
)

internal fun RepeatStackInfo.toMessageRepeatStackUiModel(): MessageRepeatStackUiModel =
    MessageRepeatStackUiModel(
        count = count,
        participantUsernames = participantUsernames,
        participantAvatars = participantAvatars,
        legacyRepeatStack = this,
    )



