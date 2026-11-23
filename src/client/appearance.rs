//! 外观商城域：目录、权益、会员状态与购买。

use serde_json::json;

use crate::client::FishPi;
use crate::domain::appearance::{
    AppearanceItems, AppearancePurchase, AppearanceShop, MembershipStatus,
};
use crate::wire::appearance::{
    WireAppearanceItems, WireAppearancePurchase, WireAppearanceShop, WireMembershipStatus,
};

const APPEARANCE_SHOP_PATH: &str = "api/v3/appearance-shop?includeVipWeekly=1";
const APPEARANCE_ITEMS_PATH: &str = "api/v3/me/appearance-items";
const APPEARANCE_PURCHASES_PATH: &str = "api/v3/me/appearance-purchases";
const MEMBERSHIP_PATH: &str = "api/v3/me/membership";

/// 外观商城域访问器。
pub struct AppearanceApi(pub(crate) FishPi);

impl FishPi {
    /// 外观商城域。
    pub fn appearance(&self) -> AppearanceApi {
        AppearanceApi(self.clone())
    }
}

impl AppearanceApi {
    /// 读取外观商城目录（API v3）。`includeVipWeekly=1` 时附带会员周租条目；无访问令牌也能读取。
    pub async fn shop(
        &self,
        access_token: Option<&str>,
    ) -> Result<AppearanceShop, crate::client::V3Error> {
        let v3 = self.0.v3().await?;
        let wire: WireAppearanceShop = match access_token.map(str::trim).filter(|t| !t.is_empty()) {
            Some(token) => v3.get_json(APPEARANCE_SHOP_PATH, token).await?,
            None => v3.get_json_anon(APPEARANCE_SHOP_PATH).await?,
        };
        Ok(wire.into())
    }

    /// 读取本人外观权益与积分余额（API v3）。需要 Bearer token。
    pub async fn items(
        &self,
        access_token: &str,
    ) -> Result<AppearanceItems, crate::client::V3Error> {
        let wire: WireAppearanceItems = self
            .0
            .v3()
            .await?
            .get_json(APPEARANCE_ITEMS_PATH, access_token)
            .await?;
        Ok(wire.into())
    }

    /// 读取本人会员状态（API v3）。需要 Bearer token。
    pub async fn membership(
        &self,
        access_token: &str,
    ) -> Result<MembershipStatus, crate::client::V3Error> {
        let wire: WireMembershipStatus = self
            .0
            .v3()
            .await?
            .get_json(MEMBERSHIP_PATH, access_token)
            .await?;
        Ok(wire.into())
    }

    /// 购买或续期外观权益（API v3）。同一次逻辑购买的重试必须复用同一个 `idempotency_key`。
    pub async fn purchase(
        &self,
        access_token: &str,
        idempotency_key: &str,
        product_id: &str,
        days: u32,
        revision: u64,
    ) -> Result<AppearancePurchase, crate::client::V3Error> {
        let wire: WireAppearancePurchase = self
            .0
            .v3()
            .await?
            .post_json(
                APPEARANCE_PURCHASES_PATH,
                access_token,
                idempotency_key,
                &json!({
                    "productId": product_id,
                    "days": days,
                    "revision": revision,
                }),
            )
            .await?;
        Ok(wire.into())
    }
}
