package dev.fishpi.mobile.feature.privatechat.model

internal data class PrivateSessionUiModel(
    val peer: String,
    val preview: String,
    val time: String,
    val avatar: String,
    val unread: Long,
    val sort: Long = 0,
)
