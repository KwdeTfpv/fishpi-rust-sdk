//! Wire 层：外观商城相关响应原始形状。

use serde::Deserialize;
use serde_json::Value;

/// 商城目录条目原始形状。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireAppearanceShopItem {
    #[serde(default)]
    pub id: String,
    #[serde(default)]
    pub kind: String,
    #[serde(default)]
    pub style_id: String,
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub version: u64,
    #[serde(default)]
    pub price7: u64,
    #[serde(default)]
    pub price30: u64,
    #[serde(default)]
    pub enabled: bool,
    #[serde(default)]
    pub revision: u64,
    #[serde(default)]
    pub vip_included: Option<bool>,
    #[serde(default)]
    pub bubble_style: Option<Value>,
}

/// 商城目录原始形状。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireAppearanceShop {
    #[serde(default)]
    pub list: Vec<WireAppearanceShopItem>,
}

/// 本人外观权益条目原始形状。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireAppearanceEntitlement {
    #[serde(default)]
    pub product_id: String,
    #[serde(default)]
    pub expires_at: u64,
}

/// 本人外观权益原始形状。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireAppearanceItems {
    #[serde(default)]
    pub list: Vec<WireAppearanceEntitlement>,
    #[serde(default)]
    pub mount_id: String,
    #[serde(default)]
    pub server_now: u64,
    #[serde(default)]
    pub balance: i64,
}

/// 外观购买结果原始形状。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireAppearancePurchase {
    #[serde(default)]
    pub id: String,
    #[serde(default)]
    pub product_id: String,
    #[serde(default)]
    pub paid_points: i64,
    #[serde(default)]
    pub expires_at: u64,
}

/// 本人会员状态原始形状。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireMembershipStatus {
    #[serde(default)]
    pub active: bool,
    #[serde(default)]
    pub level_code: String,
    #[serde(default)]
    pub expires_at: u64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shop_item_decodes_catalog_fields() {
        let item: WireAppearanceShopItem = serde_json::from_value(serde_json::json!({
            "id": "bubble-sakura-moon",
            "kind": "bubble",
            "styleId": "sakura-moon",
            "name": "月弧夜樱",
            "version": 1,
            "price7": 200,
            "price30": 0,
            "enabled": true,
            "revision": 3,
            "vipIncluded": true
        }))
        .unwrap();
        assert_eq!(item.id, "bubble-sakura-moon");
        assert_eq!(item.kind, "bubble");
        assert_eq!(item.style_id, "sakura-moon");
        assert_eq!(item.name, "月弧夜樱");
        assert_eq!(item.version, 1);
        assert_eq!(item.price7, 200);
        assert_eq!(item.price30, 0);
        assert!(item.enabled);
        assert_eq!(item.revision, 3);
        assert_eq!(item.vip_included, Some(true));
        assert!(item.bubble_style.is_none());
    }

    #[test]
    fn shop_item_decodes_custom_bubble_style() {
        let style = serde_json::json!({ "background": "linear-gradient(red, blue)" });
        let item: WireAppearanceShopItem = serde_json::from_value(serde_json::json!({
            "id": "bubble-hexiaotun",
            "kind": "bubble",
            "styleId": "bubble-hexiaotun",
            "name": "荷叶午后",
            "version": 2,
            "price7": 60,
            "price30": 0,
            "enabled": true,
            "revision": 5,
            "bubbleStyle": style
        }))
        .unwrap();
        assert_eq!(item.vip_included, None);
        assert_eq!(item.bubble_style, Some(style));
    }

    #[test]
    fn appearance_items_decode_entitlements_balance_and_server_now() {
        let items: WireAppearanceItems = serde_json::from_value(serde_json::json!({
            "list": [
                { "productId": "frame-tide", "expiresAt": 1767225600000u64 }
            ],
            "mountId": "",
            "serverNow": 1767000000000u64,
            "balance": -5
        }))
        .unwrap();
        assert_eq!(items.list.len(), 1);
        assert_eq!(items.list[0].product_id, "frame-tide");
        assert_eq!(items.list[0].expires_at, 1767225600000);
        assert_eq!(items.mount_id, "");
        assert_eq!(items.server_now, 1767000000000);
        assert_eq!(items.balance, -5);
    }

    #[test]
    fn purchase_result_decodes_paid_points_and_expiry() {
        let purchase: WireAppearancePurchase = serde_json::from_value(serde_json::json!({
            "id": "1234567890123456789",
            "productId": "frame-tide",
            "paidPoints": 200,
            "expiresAt": 1767225600000u64
        }))
        .unwrap();
        assert_eq!(purchase.id, "1234567890123456789");
        assert_eq!(purchase.product_id, "frame-tide");
        assert_eq!(purchase.paid_points, 200);
        assert_eq!(purchase.expires_at, 1767225600000);
    }

    #[test]
    fn membership_status_decodes_active_flag() {
        let status: WireMembershipStatus =
            serde_json::from_value(serde_json::json!({ "active": true, "levelCode": "vip" }))
                .unwrap();
        assert!(status.active);
        assert_eq!(status.level_code, "vip");
        assert_eq!(status.expires_at, 0);
    }
}
