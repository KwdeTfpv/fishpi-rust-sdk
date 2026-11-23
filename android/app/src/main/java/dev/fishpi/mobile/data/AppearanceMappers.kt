package dev.fishpi.mobile.data

import org.json.JSONObject

internal fun JSONObject.toAppearanceShopItemList(): List<AppearanceShopItem> {
    val list = optJSONArray("list") ?: return emptyList()
    val items = mutableListOf<AppearanceShopItem>()
    for (index in 0 until list.length()) {
        val entry = list.optJSONObject(index) ?: continue
        items += entry.toAppearanceShopItem()
    }
    return items
}

internal fun JSONObject.toAppearanceShopItem(): AppearanceShopItem = AppearanceShopItem(
    id = optString("id"),
    kind = optString("kind"),
    styleId = optString("styleId"),
    name = optString("name"),
    version = optLong("version", 0L),
    price7 = optLong("price7", 0L),
    price30 = optLong("price30", 0L),
    enabled = optBoolean("enabled", false),
    revision = optLong("revision", 0L),
    vipIncluded = optBoolean("vipIncluded", false),
    bubbleStyleJson = optJSONObject("bubbleStyle")?.toString(),
)

internal fun JSONObject.toAppearanceInventory(): AppearanceInventory = AppearanceInventory(
    entitlements = buildList {
        val list = optJSONArray("list") ?: return@buildList
        for (index in 0 until list.length()) {
            val entry = list.optJSONObject(index) ?: continue
            add(AppearanceEntitlement(productId = entry.optString("productId"), expiresAt = entry.optLong("expiresAt", 0L)))
        }
    },
    mountId = optString("mountId"),
    serverNow = optLong("serverNow", 0L),
    balance = optLong("balance", 0L),
)

internal fun JSONObject.toAppearancePurchaseResult(): AppearancePurchaseResult = AppearancePurchaseResult(
    id = optString("id"),
    productId = optString("productId"),
    paidPoints = optLong("paidPoints", 0L),
    expiresAt = optLong("expiresAt", 0L),
)

internal fun JSONObject.toMembershipStatus(): MembershipStatus = MembershipStatus(
    active = optBoolean("active", false),
    levelCode = optString("levelCode"),
    expiresAt = optLong("expiresAt", 0L),
)
