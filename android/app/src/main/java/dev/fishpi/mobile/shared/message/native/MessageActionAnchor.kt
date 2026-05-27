package dev.fishpi.mobile.shared.message.native

import android.graphics.Rect
import dev.fishpi.mobile.data.ChatRoomMessage

internal data class MessageActionAnchor(
    val message: ChatRoomMessage,
    val rectInWindow: Rect,
    val isMine: Boolean,
)
