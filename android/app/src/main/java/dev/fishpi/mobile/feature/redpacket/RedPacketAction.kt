package dev.fishpi.mobile.feature.redpacket

sealed interface RedPacketAction {
    data object SendClicked : RedPacketAction
    data object OpenClicked : RedPacketAction
    data object Dismiss : RedPacketAction
    data object DismissResult : RedPacketAction
    data class GesturePicked(val value: Int) : RedPacketAction
    data class TypeChanged(val value: String) : RedPacketAction
    data class MoneyChanged(val value: String) : RedPacketAction
    data class CountChanged(val value: String) : RedPacketAction
    data class MessageChanged(val value: String) : RedPacketAction
    data class ReceiversChanged(val value: String) : RedPacketAction
}
