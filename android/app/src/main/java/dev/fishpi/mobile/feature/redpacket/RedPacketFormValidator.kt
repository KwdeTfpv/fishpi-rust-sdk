package dev.fishpi.mobile.feature.redpacket

data class RedPacketFormValidationResult(
    val isValid: Boolean,
    val money: Int? = null,
    val count: Int? = null,
    val moneyError: String? = null,
    val countError: String? = null,
    val receiversError: String? = null,
) {
    val firstError: String?
        get() = moneyError ?: countError ?: receiversError
}

object RedPacketFormValidator {
    fun validate(form: RedPacketFormState): RedPacketFormValidationResult {
        val money = form.money.trim().toIntOrNull()
        val count = form.count.trim().toIntOrNull()
        val moneyError = when {
            money == null -> "请输入红包积分"
            money <= 0 -> "红包积分必须大于 0"
            form.type == RedPacketTypeRockPaperScissors && money < 256 -> "猜拳红包至少 256 积分"
            else -> null
        }
        val countError = when {
            count == null -> "请输入红包个数"
            count <= 0 -> "红包个数必须大于 0"
            else -> null
        }
        val receiversError = when {
            form.type == RedPacketTypeSpecify && form.receivers.trim().isBlank() -> "专属红包需要填写接收人"
            else -> null
        }
        return RedPacketFormValidationResult(
            isValid = moneyError == null && countError == null && receiversError == null,
            money = money,
            count = count,
            moneyError = moneyError,
            countError = countError,
            receiversError = receiversError,
        )
    }
}
