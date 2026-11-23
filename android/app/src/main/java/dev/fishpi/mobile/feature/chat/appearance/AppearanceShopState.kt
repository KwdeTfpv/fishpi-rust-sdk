package dev.fishpi.mobile.feature.chat.appearance

import dev.fishpi.mobile.data.AppearanceInventory
import dev.fishpi.mobile.data.AppearancePurchaseResult
import dev.fishpi.mobile.data.AppearanceShopItem
import dev.fishpi.mobile.data.MembershipStatus

enum class AppearanceShopPhase {
    Loading,
    Ready,
    Failed,
}

data class AppearancePurchaseUiState(
    val productId: String,
    val days: Int,
    val revision: Long,
    val idempotencyKey: String,
    val submitting: Boolean = false,
    val unknownResult: Boolean = false,
    val error: String? = null,
    val success: AppearancePurchaseResult? = null,
    val needsCaptcha: Boolean = false,
)

data class AppearanceShopUiState(
    val phase: AppearanceShopPhase = AppearanceShopPhase.Loading,
    val items: List<AppearanceShopItem> = emptyList(),
    val inventory: AppearanceInventory? = null,
    val inventoryError: String? = null,
    val membership: MembershipStatus? = null,
    val membershipError: String? = null,
    val error: String? = null,
    val purchase: AppearancePurchaseUiState? = null,
) {
    val isLoading: Boolean get() = phase == AppearanceShopPhase.Loading
    val balance: Long? get() = inventory?.balance
    val serverNow: Long get() = inventory?.serverNow ?: 0L
}
