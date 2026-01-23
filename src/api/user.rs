//! # 用户 API 模块
//!
//! 这个模块提供了与用户相关的 API 操作，包括获取用户信息、查询表情、活跃度、签到、转账、关注、修改头像和用户信息等功能。
//! 主要结构体是 `User`，用于管理用户的 API 请求。
//!
//! ## 主要组件
//!
//! - [`User`] - 用户客户端结构体，负责所有用户相关的 API 调用。
//!
//! ## 方法列表
//!
//! - [`User::new`] - 创建新的用户客户端实例。
//! - [`User::get_token`] - 获取当前 API token。
//! - [`User::set_token`] - 重新设置请求 token。
//! - [`User::is_logined`] - 检查用户是否已登录（API key 是否为空）。
//! - [`User::info`] - 返回登录账户信息。
//! - [`User::emotions`] - 查询登录用户常用表情。
//! - [`User::liveness`] - 查询登录用户当前活跃度。
//! - [`User::is_checkin`] - 检查用户是否已经签到。
//! - [`User::is_collected_liveness`] - 检查用户是否领取昨日活跃奖励。
//! - [`User::reward_liveness`] - 领取昨日活跃度奖励。
//! - [`User::transfer`] - 转账。
//! - [`User::follow`] - 关注用户。
//! - [`User::unfollow`] - 取消关注用户。
//! - [`User::update_avatar`] - 修改用户头像。
//! - [`User::update_user_info`] - 修改用户信息。
//! - [`User::get_user`] - 获取其他用户信息。
//! - [`User::report`] - 举报。
//! - [`User::upload`] - 上传文件。
//!
//! ## 示例
//!
//! ```rust,no_run
//! use fishpi_sdk::{FishPi, api::user::User};
//! use fishpi_sdk::model::user::UpdateUserInfoParams;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     // 登录获取用户客户端
//!     let user = FishPi::login(&login_data).await?;
//!
//!     // 获取用户信息
//!     let info = user.info().await?;
//!     println!("User name: {}", info.user_name);
//!
//!     // 查询表情
//!     let emotions = user.emotions().await?;
//!     println!("Emotions: {:?}", emotions);
//!
//!     // 转账
//!     user.transfer("target_user", 100, "Gift").await?;
//!
//!     // 修改用户信息
//!     let params = UpdateUserInfoParams {
//!         nickName: Some("New Name".to_string()),
//!         userUrl: Some("https://example.com".to_string()),
//!         userIntro: Some("New intro".to_string()),
//!         userTag: Some("tag".to_string()),
//!     };
//!     user.update_user_info(params).await?;
//!
//!     Ok(())
//! }
//! ```
use crate::api::article::Article;
use crate::api::breezemoon::BreezeMoon;
use crate::api::chat::Chat;
use crate::api::chatroom::ChatRoom;
use crate::api::comment::Comment;
use crate::api::notice::Notice;
use crate::api::redpacket::Redpacket;
use crate::model::misc::{Report, UploadResult};
use crate::model::user::{UpdateUserInfoParams, UserInfo, UserPoint};
use crate::utils::error::Error;
use crate::utils::{ResponseResult, get, post, upload_files};
use serde_json::{Value, json};

pub struct User {
    api_key: String,
    pub chatroom: ChatRoom,
    pub chat: Chat,
    pub breezemoon: BreezeMoon,
    pub article: Article,
    pub notice: Notice,
    pub redpacket: Redpacket,
    pub comment: Comment,
}

impl User {
    pub fn new(api_key: String) -> Self {
        Self {
            api_key: api_key.clone(),
            chatroom: ChatRoom::new(api_key.clone()),
            chat: Chat::new(api_key.clone()),
            breezemoon: BreezeMoon::new(api_key.clone()),
            article: Article::new(api_key.clone()),
            notice: Notice::new(api_key.clone()),
            redpacket: Redpacket::new(api_key.clone()),
            comment: Comment::new(api_key.clone()),
        }
    }

    pub fn get_token(&self) -> &str {
        &self.api_key
    }

    /// 重新设置请求token
    pub fn set_token(&mut self, token: String) {
        self.api_key = token;
    }

    pub fn is_logined(&self) -> bool {
        !self.api_key.is_empty()
    }

    /// 返回登录账户信息，需要先登录或设置有效api_key
    pub async fn info(&self) -> Result<UserInfo, Error> {
        let mut resp = get(&format!("api/user?apiKey={}", &self.api_key)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let data_value = if let Some(data_str) = resp["data"].as_str() {
            serde_json::from_str(data_str).map_err(|e| Error::Api(e.to_string()))?
        } else {
            resp["data"].take()
        };

        UserInfo::from_value(&data_value)
    }

    /// 查询登录用户常用表情
    pub async fn emotions(&self) -> Result<Vec<String>, Error> {
        let mut resp = get(&format!("users/emotions?apiKey={}", &self.api_key)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let data: Vec<Value> =
            serde_json::from_value(resp["data"].take()).map_err(|e| Error::Parse(format!("Failed to parse emotions: {}", e)))?;
        let emotions: Vec<String> = data
            .into_iter()
            .filter_map(|v| {
                v.as_object()
                    .and_then(|obj| obj.values().next())
                    .and_then(|val| val.as_str())
                    .map(|s| s.to_string())
            })
            .collect();
        Ok(emotions)
    }

    /// 查询登录用户当前活跃度，请求频率请控制在 30 ~ 60 秒一次
    pub async fn liveness(&self) -> Result<u32, Error> {
        let resp = get(&format!("user/liveness?apiKey={}", &self.api_key)).await?;

        let liveness = resp["liveness"].as_u64().unwrap_or(0) as u32;
        Ok(liveness)
    }

    /// 检查用户是否已经签到
    pub async fn is_checkin(&self) -> Result<bool, Error> {
        let resp = get(&format!("user/isCheckin?apiKey={}", &self.api_key)).await?;

        let is_checkin: bool = resp["isCheckin"].as_bool().unwrap_or(false);
        Ok(is_checkin)
    }

    /// 检查用户是否领取昨日活跃奖励
    pub async fn is_collected_liveness(&self) -> Result<bool, Error> {
        let resp = get(&format!(
            "api/activity/is-collected-liveness?apiKey={}",
            &self.api_key
        ))
        .await?;

        let is_rewarded: bool = resp["isLivenessRewarded"].as_bool().unwrap_or(false);
        Ok(is_rewarded)
    }

    /// 领取昨日活跃度奖励
    pub async fn reward_liveness(&self) -> Result<u32, Error> {
        let resp = get(&format!(
            "activity/yesterday-liveness-reward-api?apiKey={}",
            &self.api_key
        ))
        .await?;

        let success: u32 = resp["sum"].as_u64().unwrap_or(0) as u32;
        Ok(success)
    }

    /// 转账
    pub async fn transfer(&self, username: &str, amount: u32, memo: &str) -> Result<bool, Error> {
        let data = json!({
            "username": username,
            "amount": amount,
            "memo": memo,
            "apiKey": self.api_key,
        });

        let resp = post("point/transfer", Some(data)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(true)
    }

    /// 关注用户
    pub async fn follow(&self, following_id: &str) -> Result<bool, Error> {
        let data = json!({
            "followingId": following_id,
            "apiKey": self.api_key,
        });

        let resp = post("follow/user", Some(data)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(true)
    }

    /// 取消关注用户
    pub async fn unfollow(&self, following_id: &str) -> Result<bool, Error> {
        let data = json!({
            "followingId": following_id,
            "apiKey": self.api_key,
        });

        let resp = post("unfollow/user", Some(data)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(true)
    }

    /// 修改用户头像
    pub async fn update_avatar(&self, avatar_url: &str) -> Result<bool, Error> {
        let data = json!({
            "userAvatarURL": avatar_url,
            "apiKey": self.api_key
        });

        let resp = post("api/settings/avatar", Some(data)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(true)
    }

    /// 修改用户信息
    ///
    /// #### 参数
    /// * `params` 用户信息参数 [UpdateUserInfoParams]
    pub async fn update_user_info(&self, params: UpdateUserInfoParams) -> Result<bool, Error> {
        let data = json!({
            "userNickname": params.nickName,
            "userURL": params.userUrl,
            "userIntro": params.userIntro,
            "userTag": params.userTag,
            "apiKey": self.api_key,
        });

        let resp = post("api/settings/profiles", Some(data)).await?;

        if resp["code"] != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(true)
    }

    /// 获取用户信息
    ///
    /// - `username` 用户名
    ///
    /// 返回用户信息
    pub async fn get_user(&self, username: &str) -> Result<UserInfo, Error> {
        let url = format!("user/{}?apiKey={}", username, self.api_key);

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(0) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        UserInfo::from_value(&rsp)
    }

    /// 举报
    ///
    /// - `data` 举报数据 [Report]
    ///
    /// 返回举报结果
    pub async fn report(&self, data: &Report) -> Result<ResponseResult, Error> {
        let url = "report".to_string();

        let mut data_json = serde_json::to_value(data)
            .map_err(|e| Error::Parse(format!("Failed to serialize Report: {}", e)))?;
        data_json["apiKey"] = Value::String(self.api_key.clone());

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 上传文件
    ///
    /// - `files` 文件路径列表
    ///
    /// 返回上传结果
    pub async fn upload(&self, files: Vec<String>) -> Result<UploadResult, Error> {
        // 检查文件是否存在
        for file in &files {
            if !std::path::Path::new(file).exists() {
                return Err(Error::Api(format!("File not exist: {}", file)));
            }
        }

        let url = "upload".to_string();
        let rsp = upload_files(&url, files, &self.api_key).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        UploadResult::from_value(&rsp["data"])
    }

    /// 获取用户积分
    /// 
    /// - `username` 用户名
    ///   返回用户积分信息 [UserPoint]
    pub async fn get_points(&self, username: &str) -> Result<UserPoint, Error> {
        let resp = get(&format!("user/{}/point", username)).await?;

        if resp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        UserPoint::from_value(&resp)
    }
}
