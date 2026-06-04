package dev.fishpi.mobile.feature.privatechat.mapper

import dev.fishpi.mobile.data.PrivateChatSession
import dev.fishpi.mobile.feature.privatechat.model.PrivateSessionUiModel

internal const val FileTransferPeer = "FileTransfer"
private const val FileTransferAvatarUrl =
    "https://file.fishpi.cn/2022/06/e1541bfe4138c144285f11ea858b6bf6-ba777366.jpeg"

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
    sortedWith(
        compareByDescending<PrivateChatSession> { it.peer.isFileTransferPeer() }
            .thenByDescending { it.sort }
            .thenByDescending { it.time },
    )

internal fun List<PrivateChatSession>.withFileTransferSession(): List<PrivateChatSession> {
    if (any { it.peer.isFileTransferPeer() }) return this
    return listOf(
        PrivateChatSession(
            peer = FileTransferPeer,
            preview = "跨端传输文本/文件",
            time = "",
            avatar = FileTransferAvatarUrl,
            unread = 0,
            sort = Long.MAX_VALUE,
        ),
    ) + this
}

internal fun String.isFileTransferPeer(): Boolean =
    equals(FileTransferPeer, ignoreCase = true)
