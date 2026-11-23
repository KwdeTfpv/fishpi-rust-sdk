//! Domain 层：外观商城对外类型。

use serde_json::Value;

use crate::wire::appearance::{
    WireAppearanceItems, WireAppearancePurchase, WireAppearanceShop, WireMembershipStatus,
};

/// 商城目录条目。
#[derive(Clone, Debug, PartialEq)]
pub struct AppearanceShopItem {
    /// 商城商品 ID，与装备用的 styleId 可能不同。
    pub id: String,
    /// 条目类别：`bubble` / `frame` / 其他。
    pub kind: String,
    /// 装备到聊天外观时使用的样式 ID。
    pub style_id: String,
    pub name: String,
    pub version: u64,
    /// 7 天价格（积分），目录保证 ≥1。
    pub price7: u64,
    /// 30 天价格（积分），0 表示不提供该期限。
    pub price30: u64,
    pub enabled: bool,
    pub revision: u64,
    /// 会员可直接使用（`kind != mount` 时出现）。
    pub vip_included: bool,
    /// 自定义气泡样式（`kind == bubble` 时出现）。
    pub bubble_style: Option<Value>,
}

/// 商城目录。
#[derive(Clone, Debug, Default, PartialEq)]
pub struct AppearanceShop {
    pub items: Vec<AppearanceShopItem>,
}

/// 本人外观权益条目。
#[derive(Clone, Debug, PartialEq)]
pub struct AppearanceEntitlement {
    pub product_id: String,
    /// 到期时间（毫秒时间戳）。
    pub expires_at: u64,
}

/// 本人外观权益与积分余额。
#[derive(Clone, Debug, Default, PartialEq)]
pub struct AppearanceItems {
    pub entitlements: Vec<AppearanceEntitlement>,
    pub mount_id: String,
    /// 服务器当前时间（毫秒时间戳），用于校准到期判断。
    pub server_now: u64,
    /// 积分余额，可能为负。
    pub balance: i64,
}

/// 外观购买结果。
#[derive(Clone, Debug, PartialEq)]
pub struct AppearancePurchase {
    /// 购买流水号。
    pub id: String,
    pub product_id: String,
    /// 实扣积分。
    pub paid_points: i64,
    /// 购买后的权益到期时间（毫秒时间戳）。
    pub expires_at: u64,
}

/// 本人会员状态。
#[derive(Clone, Debug, Default, PartialEq)]
pub struct MembershipStatus {
    pub active: bool,
    /// 当前档位代码，无有效会员时为空。
    pub level_code: String,
    /// 到期时间（毫秒时间戳），永久会员为 0。
    pub expires_at: u64,
}

impl From<WireAppearanceShop> for AppearanceShop {
    fn from(wire: WireAppearanceShop) -> Self {
        Self {
            items: wire
                .list
                .into_iter()
                .map(|item| AppearanceShopItem {
                    id: item.id,
                    kind: item.kind,
                    style_id: item.style_id,
                    name: item.name,
                    version: item.version,
                    price7: item.price7,
                    price30: item.price30,
                    enabled: item.enabled,
                    revision: item.revision,
                    vip_included: item.vip_included.unwrap_or(false),
                    bubble_style: item.bubble_style,
                })
                .collect(),
        }
    }
}

impl From<WireAppearanceItems> for AppearanceItems {
    fn from(wire: WireAppearanceItems) -> Self {
        Self {
            entitlements: wire
                .list
                .into_iter()
                .map(|entry| AppearanceEntitlement {
                    product_id: entry.product_id,
                    expires_at: entry.expires_at,
                })
                .collect(),
            mount_id: wire.mount_id,
            server_now: wire.server_now,
            balance: wire.balance,
        }
    }
}

impl From<WireAppearancePurchase> for AppearancePurchase {
    fn from(wire: WireAppearancePurchase) -> Self {
        Self {
            id: wire.id,
            product_id: wire.product_id,
            paid_points: wire.paid_points,
            expires_at: wire.expires_at,
        }
    }
}

impl From<WireMembershipStatus> for MembershipStatus {
    fn from(wire: WireMembershipStatus) -> Self {
        Self {
            active: wire.active,
            level_code: wire.level_code,
            expires_at: wire.expires_at,
        }
    }
}
