package dev.fishpi.mobile.feature.redpacket

data class RedPacketState(
    val composerOpen: Boolean = false,
    val form: RedPacketFormState = RedPacketFormState(),
    val gestureTargetMessageId: String? = null,
    val selectedGesture: Int? = null,
    val result: RedPacketResultUiModel? = null,
)
