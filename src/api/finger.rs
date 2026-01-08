use serde_json::{Value, json};

use crate::{
    model::{
        finger::{UserBag, UserBagType, UserIP},
        user::MetalBase,
    },
    utils::{ResponseResult, error::Error, post},
};

pub struct Finger {
    key: String,
}

impl Finger {
    pub fn new(key: String) -> Self {
        Self { key }
    }

    /// 上传摸鱼大闯关关卡数据
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    /// - `stage` 关卡数
    /// - `time` 通过此关时间（毫秒级时间戳），可选（默认为当前时间戳）
    ///
    /// 返回执行结果
    pub async fn add_mofish_score(
        &self,
        user_name: &str,
        stage: &str,
        time: Option<u64>,
    ) -> Result<ResponseResult, Error> {
        let url = "api/games/mofish/score".to_string();

        let time = time.unwrap_or_else(|| {
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64
        });

        let data = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
            "stage": stage,
            "time": time,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 查询用户最近登录的IP地址
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    ///
    /// 返回用户IP信息
    pub async fn query_latest_login_ip(&self, user_name: &str) -> Result<UserIP, Error> {
        let url = "user/query/latest-login-iP".to_string();

        let data = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
        });

        let rsp = post(&url, Some(data)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        UserIP::from_value(&rsp)
    }

    /// 添加勋章
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    /// - `metal` 勋章信息
    ///
    /// 返回执行结果
    pub async fn add_metal(
        &self,
        user_name: &str,
        metal: &MetalBase,
    ) -> Result<ResponseResult, Error> {
        let url = "user/edit/give-metal".to_string();

        let mut data = serde_json::to_value(metal)
            .map_err(|e| Error::Parse(format!("Failed to serialize MetalBase: {}", e)))?;
        data["goldFingerKey"] = Value::String(self.key.clone());
        data["userName"] = Value::String(user_name.to_string());
        data["attr"] = Value::String(metal.attr.to_string());

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 删除勋章
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    /// - `name` 勋章名称
    ///
    /// 返回执行结果
    pub async fn delete_metal(&self, user_name: &str, name: &str) -> Result<ResponseResult, Error> {
        let url = "user/edit/remove-metal".to_string();

        let data = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
            "name": name,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 通过用户ID删除勋章
    ///
    /// - `user_id` 用户在摸鱼派的用户ID
    /// - `name` 勋章名称
    ///
    /// 返回执行结果
    pub async fn delete_metal_by_user_id(
        &self,
        user_id: &str,
        name: &str,
    ) -> Result<ResponseResult, Error> {
        let url = "user/edit/remove-metal-by-user-id".to_string();

        let data = json!({
            "goldFingerKey": self.key,
            "userId": user_id,
            "name": name,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 查询用户背包
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    ///
    /// 返回用户背包信息
    pub async fn query_user_bag(&self, user_name: &str) -> Result<UserBag, Error> {
        let url = "user/query/items".to_string();

        let data_json = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
        });

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        UserBag::from_value(&rsp)
    }

    /// 调整用户背包
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    /// - `item` 物品名称
    /// - `sum` 物品数量
    ///
    /// 返回执行结果
    pub async fn edit_user_bag(
        &self,
        user_name: &str,
        item: UserBagType,
        sum: i32,
    ) -> Result<ResponseResult, Error> {
        let url = "user/edit/items".to_string();

        let data_json = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
            "item": item.to_string(),
            "sum": sum,
        });

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 调整用户积分
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    /// - `point` 积分数量
    /// - `memo` 备注
    ///
    /// 返回执行结果
    pub async fn edit_user_points(
        &self,
        user_name: &str,
        point: i32,
        memo: &str,
    ) -> Result<ResponseResult, Error> {
        let url = "user/edit/points".to_string();

        let data_json = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
            "point": point,
            "memo": memo,
        });

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 查询用户当前活跃度
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    ///
    /// 返回活跃度
    pub async fn get_liveness(&self, user_name: &str) -> Result<f64, Error> {
        let url = "user/liveness".to_string();

        let data_json = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
        });

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["liveness"].as_f64().unwrap_or(0.0))
    }

    /// 查询用户昨日活跃度奖励
    ///
    /// - `user_name` 用户在摸鱼派的用户名
    ///
    /// 返回奖励数量
    pub async fn get_yesterday_liveness_reward(&self, user_name: &str) -> Result<f64, Error> {
        let url = "activity/yesterday-liveness-reward-api".to_string();

        let data_json = json!({
            "goldFingerKey": self.key,
            "userName": user_name,
        });

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["sum"].as_f64().unwrap_or(0.0))
    }
}
