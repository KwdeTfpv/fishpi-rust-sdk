package dev.fishpi.mobile.feature.chat.appearance

import dev.fishpi.mobile.data.AppearanceInventory
import dev.fishpi.mobile.data.AppearanceShopItem
import dev.fishpi.mobile.shared.message.native.ChatAppearanceCatalog
import dev.fishpi.mobile.shared.message.native.ChatAppearanceSupport
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class AppearanceProductKind(val wire: String) {
    Bubble("bubble"),
    Frame("frame"),
}

enum class AppearanceCardAction {
    None,
    Equip,
    Purchase,
}

data class AppearanceProductCard(
    val styleId: String,
    val kind: AppearanceProductKind,
    val name: String,
    val productId: String?,
    val price7: Long?,
    val price30: Long?,
    val revision: Long,
    val enabled: Boolean,
    val bubbleStyleJson: String?,
    val freeAccess: Boolean,
    val membershipAccess: Boolean,
    val vipIncluded: Boolean,
    val entitlementExpiresAt: Long?,
    val serverNow: Long,
    val supported: Boolean,
    val equipped: Boolean,
    val action: AppearanceCardAction,
    val statusText: String,
    val actionText: String?,
) {
    val selectable: Boolean get() = action == AppearanceCardAction.Equip && !equipped
    val purchasable: Boolean
        get() = action == AppearanceCardAction.Purchase && productId != null && enabled
}

private val expiryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

private fun formatExpiry(epochMillis: Long): String =
    expiryFormatter.format(Instant.ofEpochMilli(epochMillis))

private data class StaticEntry(
    val styleId: String,
    val name: String,
    val free: Boolean,
    val membershipOnly: Boolean,
)

private fun staticEntries(kind: AppearanceProductKind): List<StaticEntry> {
    if (kind == AppearanceProductKind.Bubble) {
        return ChatAppearanceCatalog.bubbles.map { option ->
            StaticEntry(
                styleId = option.id,
                name = option.label,
                free = option.access == "免费",
                membershipOnly = option.access == "会员",
            )
        }
    }
    return ChatAppearanceCatalog.frames.map { option ->
        StaticEntry(
            styleId = option.id,
            name = option.label,
            free = option.access == "免费" || option.access == "默认",
            membershipOnly = option.access == "会员",
        )
    }
}

internal fun buildAppearanceProductCards(
    kind: AppearanceProductKind,
    shopItems: List<AppearanceShopItem>,
    inventory: AppearanceInventory?,
    membershipActive: Boolean?,
    equippedStyleId: String?,
): List<AppearanceProductCard> {
    val statics = staticEntries(kind)
    val shopByStyleId = shopItems
        .filter { it.kind == kind.wire && it.styleId.isNotBlank() }
        .associateBy { it.styleId }
    val entitlementByProductId = inventory?.entitlements?.associate { it.productId to it.expiresAt }
    val serverNow = inventory?.serverNow ?: 0L
    val equipped = equippedStyleId?.let {
        if (kind == AppearanceProductKind.Bubble) {
            ChatAppearanceSupport.canonicalBubbleId(it)
        } else {
            ChatAppearanceSupport.canonicalFrameId(it)
        }
    }

    val styleIds = (statics.map { it.styleId } + shopByStyleId.keys).distinct()
    return styleIds.map { styleId ->
        val static = statics.firstOrNull { it.styleId == styleId }
        val shop = shopByStyleId[styleId]
        val supported = if (kind == AppearanceProductKind.Bubble) {
            ChatAppearanceSupport.supportsBubble(styleId)
        } else {
            ChatAppearanceSupport.supportsFrame(styleId)
        }
        val card = AppearanceProductCard(
            styleId = styleId,
            kind = kind,
            name = shop?.name?.takeIf { it.isNotBlank() } ?: static?.name ?: styleId,
            productId = shop?.id,
            price7 = shop?.price7,
            price30 = shop?.price30,
            revision = shop?.revision ?: 0L,
            enabled = shop?.enabled ?: false,
            bubbleStyleJson = shop?.bubbleStyleJson,
            freeAccess = static?.free == true && shop == null,
            membershipAccess = static?.membershipOnly == true,
            vipIncluded = shop?.vipIncluded == true,
            entitlementExpiresAt = shop?.id?.let { entitlementByProductId?.get(it) },
            serverNow = serverNow,
            supported = supported,
            equipped = styleId == equipped,
            action = AppearanceCardAction.None,
            statusText = "",
            actionText = null,
        )
        card.copy(
            statusText = appearanceCardStatusText(card, membershipActive),
            action = appearanceCardAction(card, membershipActive),
        )
    }
}

internal fun appearanceCardActionLabel(card: AppearanceProductCard): String = when (card.action) {
    AppearanceCardAction.None -> if (card.equipped) "当前已装备" else ""
    AppearanceCardAction.Equip -> "装备"
    AppearanceCardAction.Purchase -> if (card.entitlementExpiresAt != null) "续期购买" else "选择期限购买"
}

private fun appearanceCardStatusText(
    card: AppearanceProductCard,
    membershipActive: Boolean?,
): String {
    if (!card.supported) return "当前版本暂不支持预览/装备"
    if (card.equipped) return "已装备"
    if (card.freeAccess) return "免费"
    if (membershipActive == true && (card.membershipAccess || card.vipIncluded)) return "会员可直接使用"
    if (card.productId != null) {
        if (!card.enabled) return "已下架"
        if (hasValidEntitlement(card)) return "已购 · 有效期至 ${formatExpiry(card.entitlementExpiresAt ?: 0L)}"
        if (hasExpiredEntitlement(card)) {
            return if (renewable(card)) "已到期 · 可续期" else "已到期 · 不可续期"
        }
        if (hasPrice(card)) return "在售"
    }
    if (card.membershipAccess && membershipActive == null) return "会员状态暂不可用"
    if (card.membershipAccess) return "需要会员"
    return "暂不可用"
}

private fun appearanceCardAction(
    card: AppearanceProductCard,
    membershipActive: Boolean?,
): AppearanceCardAction {
    if (!card.supported || card.equipped) return AppearanceCardAction.None
    if (card.freeAccess) return AppearanceCardAction.Equip
    if (membershipActive == true && (card.membershipAccess || card.vipIncluded)) return AppearanceCardAction.Equip
    if (hasValidEntitlement(card)) return AppearanceCardAction.Equip
    if (card.productId != null && !card.enabled) return AppearanceCardAction.None
    if (renewable(card) || (card.productId != null && card.enabled && hasPrice(card))) {
        return AppearanceCardAction.Purchase
    }
    return AppearanceCardAction.None
}

private fun hasValidEntitlement(card: AppearanceProductCard): Boolean {
    val expiresAt = card.entitlementExpiresAt ?: return false
    if (expiresAt <= 0L) return false
    return card.serverNow <= 0L || expiresAt >= card.serverNow
}

private fun hasExpiredEntitlement(card: AppearanceProductCard): Boolean {
    val expiresAt = card.entitlementExpiresAt ?: return false
    return expiresAt > 0L && card.serverNow > 0L && expiresAt < card.serverNow
}

private fun hasPrice(card: AppearanceProductCard): Boolean =
    (card.price7 ?: 0L) > 0L || (card.price30 ?: 0L) > 0L

private fun renewable(card: AppearanceProductCard): Boolean =
    card.productId != null && card.enabled && hasPrice(card) && hasExpiredEntitlement(card)
