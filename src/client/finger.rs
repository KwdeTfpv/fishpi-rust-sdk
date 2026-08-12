use crate::client::core::FishPi;
use crate::domain::finger::{BagItem, MetalGift, UserBag, UserIp};
use crate::utils::error::Error;
use crate::wire::finger::{WireUserBag, WireUserIP};
use serde_json::{Value, json};

/// 摸金手指域访问器，持有金手指密钥。
pub struct FingerApi {
    client: FishPi,
    gold_key: String,
}

impl FishPi {
    /// 摸金手指域（管理员），`gold_key` 为金手指密钥。
    pub fn finger(&self, gold_key: impl Into<String>) -> FingerApi {
        FingerApi {
            client: self.clone(),
            gold_key: gold_key.into(),
        }
    }
}

impl FingerApi {
    /// 上传摸鱼大闯关关卡数据。`time` 为 `None` 时用当前毫秒时间戳。
    pub async fn add_mofish_score(
        &self,
        user_name: impl Into<String>,
        stage: impl Into<String>,
        time: Option<u64>,
    ) -> Result<(), Error> {
        let time = time.unwrap_or_else(|| {
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_millis() as u64)
                .unwrap_or(0)
        });
        self.client
            .post_raw(
                "api/games/mofish/score",
                &json!({
                    "goldFingerKey": self.gold_key,
                    "userName": user_name.into(),
                    "stage": stage.into(),
                    "time": time,
                }),
            )
            .await?;
        Ok(())
    }

    /// 查询用户最近登录 IP。
    pub async fn latest_login_ip(&self, user_name: impl Into<String>) -> Result<UserIp, Error> {
        let wire: WireUserIP = self
            .client
            .post_data(
                "user/query/latest-login-iP",
                &json!({ "goldFingerKey": self.gold_key, "userName": user_name.into() }),
            )
            .await?;
        Ok(wire.into())
    }

    /// 发放勋章。
    pub async fn add_metal(
        &self,
        user_name: impl Into<String>,
        metal: &MetalGift,
    ) -> Result<(), Error> {
        self.client
            .post_raw(
                "user/edit/give-metal",
                &json!({
                    "goldFingerKey": self.gold_key,
                    "userName": user_name.into(),
                    "attr": metal.attr,
                    "name": metal.name,
                    "description": metal.description,
                    "data": metal.data,
                }),
            )
            .await?;
        Ok(())
    }

    /// 删除勋章。
    pub async fn delete_metal(
        &self,
        user_name: impl Into<String>,
        name: impl Into<String>,
    ) -> Result<(), Error> {
        self.client
            .post_raw(
                "user/edit/remove-metal",
                &json!({
                    "goldFingerKey": self.gold_key,
                    "userName": user_name.into(),
                    "name": name.into(),
                }),
            )
            .await?;
        Ok(())
    }

    /// 通过用户 ID 删除勋章。
    pub async fn delete_metal_by_user_id(
        &self,
        user_id: impl Into<String>,
        name: impl Into<String>,
    ) -> Result<(), Error> {
        self.client
            .post_raw(
                "user/edit/remove-metal-by-user-id",
                &json!({
                    "goldFingerKey": self.gold_key,
                    "userId": user_id.into(),
                    "name": name.into(),
                }),
            )
            .await?;
        Ok(())
    }

    /// 查询用户背包。
    pub async fn user_bag(&self, user_name: impl Into<String>) -> Result<UserBag, Error> {
        let wire: WireUserBag = self
            .client
            .post_data(
                "user/query/items",
                &json!({ "goldFingerKey": self.gold_key, "userName": user_name.into() }),
            )
            .await?;
        Ok(wire.into())
    }

    /// 调整用户背包。
    pub async fn edit_user_bag(
        &self,
        user_name: impl Into<String>,
        item: BagItem,
        sum: i32,
    ) -> Result<(), Error> {
        self.client
            .post_raw(
                "user/edit/items",
                &json!({
                    "goldFingerKey": self.gold_key,
                    "userName": user_name.into(),
                    "item": item.as_str(),
                    "sum": sum,
                }),
            )
            .await?;
        Ok(())
    }

    /// 调整用户积分。
    pub async fn edit_user_points(
        &self,
        user_name: impl Into<String>,
        point: i32,
        memo: impl Into<String>,
    ) -> Result<(), Error> {
        self.client
            .post_raw(
                "user/edit/points",
                &json!({
                    "goldFingerKey": self.gold_key,
                    "userName": user_name.into(),
                    "point": point,
                    "memo": memo.into(),
                }),
            )
            .await?;
        Ok(())
    }

    /// 查询用户当前活跃度。
    pub async fn liveness(&self, user_name: impl Into<String>) -> Result<f64, Error> {
        let rsp: Value = self
            .client
            .post_raw(
                "user/liveness",
                &json!({ "goldFingerKey": self.gold_key, "userName": user_name.into() }),
            )
            .await?;
        Ok(rsp["liveness"].as_f64().unwrap_or(0.0))
    }

    /// 查询用户昨日活跃度奖励。
    pub async fn yesterday_liveness_reward(
        &self,
        user_name: impl Into<String>,
    ) -> Result<f64, Error> {
        let rsp: Value = self
            .client
            .post_raw(
                "activity/yesterday-liveness-reward-api",
                &json!({ "goldFingerKey": self.gold_key, "userName": user_name.into() }),
            )
            .await?;
        Ok(rsp["sum"].as_f64().unwrap_or(0.0))
    }
}
