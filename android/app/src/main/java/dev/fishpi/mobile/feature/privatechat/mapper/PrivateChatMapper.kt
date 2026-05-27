package dev.fishpi.mobile.feature.privatechat.mapper

import dev.fishpi.mobile.data.PrivateChatSession
import dev.fishpi.mobile.feature.privatechat.model.PrivateSessionUiModel

internal fun PrivateChatSession.toPrivateSessionUiModel(): PrivateSessionUiModel =
    PrivateSessionUiModel(
        peer = peer,
        preview = preview,
        time = time,
        avatar = avatar,
        unread = unread,
        sort = sort,
    )

internal fun PrivateSessionUiModel.toPrivateChatSession(): PrivateChatSession =
    PrivateChatSession(
        peer = peer,
        preview = preview,
        time = time,
        avatar = avatar,
        unread = unread,
        sort = sort,
    )

internal fun List<PrivateChatSession>.sortedByLatest(): List<PrivateChatSession> =
    sortedWith(compareByDescending<PrivateChatSession> { it.sort }.thenByDescending { it.time })
