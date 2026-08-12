//! 用户域方法。

use serde_json::{Value, json};

use super::core::FishPi;
use crate::domain::user::{AtUser, UserInfo, UserLite, UserPoint, UserVipInfo};
use crate::ids::UserName;
use crate::model::user::UserProfileUpdate;
use crate::utils::error::Error;
use crate::wire::user::{
    WireAtUser, WireLiveness, WireUserInfo, WireUserLite, WireUserPoint, WireUserVipInfo,
};

/// 用户域访问器。
pub struct UserApi(pub(crate) FishPi);

impl FishPi {
    /// 用户域。
    pub fn user(&self) -> UserApi {
        UserApi(self.clone())
    }
}

impl UserApi {
    /// 登录账户信息。
    pub async fn info(&self) -> Result<UserInfo, Error> {
        let wire: WireUserInfo = self.0.get_data(&self.0.with_key("api/user")).await?;
        Ok(wire.into())
    }

    /// 获取其他用户信息。
    pub async fn profile(&self, username: impl Into<UserName>) -> Result<UserInfo, Error> {
        let username = username.into();
        let path = self.0.with_key(&format!("user/{username}"));
        let value = self.0.get_raw(&path).await?;
        let wire: WireUserInfo =
            serde_json::from_value(value).map_err(|e| Error::decode(e.to_string()))?;
        Ok(wire.into())
    }

    /// 用户积分。
    pub async fn points(&self, username: impl Into<UserName>) -> Result<UserPoint, Error> {
        let username = username.into();
        let wire: WireUserPoint = self.0.get_data(&format!("user/{username}/point")).await?;
        Ok(wire.into())
    }

    /// 常用表情列表。
    pub async fn emotions(&self) -> Result<Vec<String>, Error> {
        let items: Vec<Value> = self.0.get_data(&self.0.with_key("users/emotions")).await?;
        Ok(items
            .into_iter()
            .filter_map(|v| {
                v.as_object()
                    .and_then(|obj| obj.values().next())
                    .and_then(Value::as_str)
                    .map(str::to_string)
            })
            .collect())
    }

    /// 当前活跃度（请至少 10 分钟一次）。
    pub async fn liveness(&self) -> Result<f64, Error> {
        let wire: WireLiveness = {
            let value = self.0.get_raw(&self.0.with_key("user/liveness")).await?;
            serde_json::from_value(value).map_err(|e| Error::decode(e.to_string()))?
        };
        Ok(wire.liveness.max(0.0))
    }

    /// 是否已签到。
    pub async fn is_checkin(&self) -> Result<bool, Error> {
        let value = self.0.get_raw(&self.0.with_key("user/checkedIn")).await?;
        Ok(value
            .get("checkedIn")
            .and_then(Value::as_bool)
            .unwrap_or(false))
    }

    /// 是否已领取昨日活跃奖励。
    pub async fn is_collected_liveness(&self) -> Result<bool, Error> {
        let value = self
            .0
            .get_raw(&self.0.with_key("api/activity/is-collected-liveness"))
            .await?;
        value
            .get("isCollectedYesterdayLivenessReward")
            .and_then(Value::as_bool)
            .ok_or_else(|| Error::decode("Missing isCollectedYesterdayLivenessReward"))
    }

    /// 领取昨日活跃度奖励，返回获得积分。
    pub async fn reward_liveness(&self) -> Result<i32, Error> {
        let value = self
            .0
            .get_raw(&self.0.with_key("activity/yesterday-liveness-reward-api"))
            .await?;
        value
            .get("sum")
            .and_then(Value::as_i64)
            .map(|n| n as i32)
            .ok_or_else(|| Error::decode("Missing liveness reward sum"))
    }

    /// 转账。
    pub async fn transfer(
        &self,
        username: impl Into<UserName>,
        amount: u32,
        memo: impl Into<String>,
    ) -> Result<(), Error> {
        let body = json!({
            "userName": username.into().as_str(),
            "amount": amount,
            "memo": memo.into(),
            "apiKey": self.0.api_key().as_str(),
        });
        self.0.post_raw("point/transfer", &body).await?;
        Ok(())
    }

    /// 关注用户。
    pub async fn follow(&self, following_id: impl Into<UserName>) -> Result<(), Error> {
        let body = json!({
            "followingId": following_id.into().as_str(),
            "apiKey": self.0.api_key().as_str(),
        });
        self.0.post_raw("follow/user", &body).await?;
        Ok(())
    }

    /// 取消关注用户。
    pub async fn unfollow(&self, following_id: impl Into<UserName>) -> Result<(), Error> {
        let body = json!({
            "followingId": following_id.into().as_str(),
            "apiKey": self.0.api_key().as_str(),
        });
        self.0.post_raw("unfollow/user", &body).await?;
        Ok(())
    }

    /// 获取用户勋章原始列表。
    pub async fn medals(&self, username: impl Into<UserName>) -> Result<Value, Error> {
        let body = json!({
            "apiKey": self.0.api_key().as_str(),
            "userName": username.into().as_str(),
        });
        let value = self.0.post_raw("api/medal/user/list", &body).await?;
        Ok(value
            .get("data")
            .cloned()
            .unwrap_or(Value::Array(Vec::new())))
    }

    /// 修改头像。
    pub async fn update_avatar(&self, avatar_url: impl Into<String>) -> Result<(), Error> {
        let body = json!({
            "userAvatarURL": avatar_url.into(),
            "apiKey": self.0.api_key().as_str(),
        });
        self.0.post_raw("api/settings/avatar", &body).await?;
        Ok(())
    }

    /// 修改用户资料。
    pub async fn update_profile(&self, params: impl Into<UserProfileUpdate>) -> Result<(), Error> {
        let params = params.into();
        let body = json!({
            "userNickname": params.nickname,
            "userURL": params.url,
            "userIntro": params.intro,
            "userTag": params.tags,
            "mbti": params.mbti,
            "apiKey": self.0.api_key().as_str(),
        });
        self.0.post_raw("api/settings/profiles", &body).await?;
        Ok(())
    }

    /// 用户名联想。
    pub async fn names(&self, name: impl Into<String>) -> Result<Vec<AtUser>, Error> {
        let body = json!({ "name": name.into() });
        let wire: Vec<WireAtUser> = self.0.post_data("users/names", &body).await?;
        Ok(wire.into_iter().map(AtUser::from).collect())
    }

    /// 最近注册的 20 个用户。
    pub async fn recent_register(&self) -> Result<Vec<UserLite>, Error> {
        let wire: Vec<WireUserLite> = self.0.get_data("api/user/recentReg").await?;
        Ok(wire.into_iter().map(UserLite::from).collect())
    }

    /// 用户 VIP 信息。
    pub async fn vip_info(&self, user_id: impl AsRef<str>) -> Result<UserVipInfo, Error> {
        let path = format!("api/membership/{}", user_id.as_ref());
        let value = self.0.get_raw(&path).await?;
        let assembled = assemble_vip(&value);
        let wire: WireUserVipInfo =
            serde_json::from_value(assembled).map_err(|e| Error::decode(e.to_string()))?;
        Ok(wire.into())
    }
}

/// 组装 `configJson` 与外层字段。
fn assemble_vip(rsp: &Value) -> Value {
    let data = match rsp.get("data").and_then(Value::as_object) {
        Some(d) => d,
        None => return json!({}),
    };

    let config_json_str = data
        .get("configJson")
        .and_then(Value::as_str)
        .unwrap_or("null");
    let mut assembled: Value = serde_json::from_str(config_json_str).unwrap_or_else(|_| json!({}));

    if let Some(map) = assembled.as_object_mut() {
        map.insert(
            "state".to_string(),
            data.get("state").cloned().unwrap_or(Value::Null),
        );
        if data.get("state").and_then(Value::as_i64).unwrap_or(0) == 1 {
            for key in [
                "oId",
                "userId",
                "lvCode",
                "expiresAt",
                "createdAt",
                "updatedAt",
            ] {
                map.insert(
                    key.to_string(),
                    data.get(key).cloned().unwrap_or(Value::Null),
                );
            }
        }
    }
    assembled
}
