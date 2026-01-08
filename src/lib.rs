//! # 摸鱼派 Rust SDK
//!
//! 这是一个用于与摸鱼派社区 API 交互的 Rust SDK，提供用户管理、文章、聊天室、私聊、通知、清风明月、红包、评论、举报、日志、文件上传等功能的异步客户端。
//!
//! ## 主要组件
//!
//! - [`FishPi`] - 主要接口结构体，包含所有 API 操作。
//! - [`api`] - API 客户端模块，包含各个子模块（如用户、文章等）。
//! - [`model`] - 数据模型模块，定义请求和响应的数据结构。
//! - [`utils`] - 工具模块，提供 HTTP 请求、错误处理等辅助功能。
//!
//! ## 功能特性
//!
//! - **异步支持**: 使用 `tokio` 提供异步 API 调用。
//! - **类型安全**: 使用 Serde 进行序列化/反序列化，确保数据类型安全。
//! - **错误处理**: 统一的错误类型和处理机制。
//! - **文件上传**: 支持多文件上传。
//!
//! ## 示例
//!
//! ```rust,no_run
//! use fishpi_sdk::FishPi;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     // 创建实例
//!     let mut fishpi = FishPi::new("your_api_key".to_string());
//!
//!     // 登录
//!     let token = fishpi.login(&login_data).await?;
//!
//!     // 获取用户信息
//!     let user_info = fishpi.get_user("username").await?;
//!
//!     // 发送评论
//!     let result = fishpi.comment.send(&comment_data).await?;
//!
//!     Ok(())
//! }
//! ```
pub mod api;
pub mod model;
pub mod utils;

use serde_json::{Value, json};

use crate::{
    api::{
        article::Article, breezemoon::BreezeMoon, chat::Chat, chatroom::ChatRoom, comment::Comment,
        notice::Notice, redpacket::Redpacket, user::User,
    },
    model::{
        misc::{
            Log, LoginData, PreRegisterInfo, RegisterInfo, Report, UploadResult, UserLite,
            UserVipInfo,
        },
        user::{AtUser, UserInfo},
    },
    utils::{ResponseResult, error::Error, get, post, upload_files},
};

/// 摸鱼派 Rust SDK 接口
pub struct FishPi {
    api_key: String,
    pub user: User,
    pub chatroom: ChatRoom,
    pub chat: Chat,
    pub breezemoon: BreezeMoon,
    pub article: Article,
    pub notice: Notice,
    pub redpacket: Redpacket,
    pub comment: Comment,
}

impl FishPi {
    pub fn new(api_key: String) -> Self {
        Self {
            api_key: api_key.clone(),
            user: User::new(api_key.clone()),
            chatroom: ChatRoom::new(api_key.clone()),
            chat: Chat::new(api_key.clone()),
            breezemoon: BreezeMoon::new(api_key.clone()),
            article: Article::new(api_key.clone()),
            notice: Notice::new(api_key.clone()),
            redpacket: Redpacket::new(api_key.clone()),
            comment: Comment::new(api_key.clone()),
        }
    }

    pub fn get_api_key(&self) -> &str {
        &self.api_key
    }

    pub fn set_api_key(&mut self, api_key: String) {
        self.api_key = api_key.clone();
        self.user = User::new(api_key.clone());
        self.chatroom = ChatRoom::new(api_key.clone());
        self.chat = Chat::new(api_key.clone());
        self.breezemoon = BreezeMoon::new(api_key.clone());
        self.article = Article::new(api_key.clone());
        self.notice = Notice::new(api_key.clone());
        self.redpacket = Redpacket::new(api_key.clone());
        self.comment = Comment::new(api_key.clone());
    }

    pub fn is_logined(&self) -> bool {
        !self.api_key.is_empty()
    }

    /// 登录
    ///
    /// - `data` 登录账密
    ///
    /// 返回用户的 Token
    pub async fn login(&mut self, data: &LoginData) -> Result<String, Error> {
        let url = "api/getKey".to_string();

        let data_json = data.to_value()?;

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let token = rsp["Key"].as_str().unwrap_or("").trim().to_string();
        self.set_api_key(token.clone());

        Ok(token)
    }

    /// 预注册
    ///
    /// - `data` 预注册数据
    ///
    /// 返回预注册结果
    pub async fn pre_register(&self, data: &PreRegisterInfo) -> Result<ResponseResult, Error> {
        let url = "register".to_string();

        let data_json = serde_json::to_value(data)
            .map_err(|e| Error::Parse(format!("Failed to serialize PreRegisterInfo: {}", e)))?;

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 验证手机验证码
    ///
    /// - `code` 验证码
    ///
    /// 返回用户 ID
    pub async fn verify(&self, code: &str) -> Result<String, Error> {
        let url = format!("verify?code={}", code);

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["userId"].as_str().unwrap_or("").to_string())
    }

    /// 注册
    ///
    /// - `data` 注册数据 [RegisterInfo]
    ///
    /// 返回注册结果
    pub async fn register(&self, data: &RegisterInfo) -> Result<ResponseResult, Error> {
        let url = if let Some(r) = &data.r {
            format!("register2?r={}", r)
        } else {
            "register2".to_string()
        };

        let data_json = serde_json::to_value(data)
            .map_err(|e| Error::Parse(format!("Failed to serialize RegisterInfo: {}", e)))?;

        let rsp = post(&url, Some(data_json)).await?;

        if let Some(code) = rsp.get("code").and_then(|c| c.as_i64())
            && code != 0
        {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        ResponseResult::from_value(&rsp)
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

    /// 获取用户名联想
    ///
    /// - `name` 用户名
    ///
    /// 返回用户名联想列表
    pub async fn names(&self, name: &str) -> Result<Vec<AtUser>, Error> {
        let url = "users/names".to_string();

        let data_json = json!({
            "name": name,
        });

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(0) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let at_users = rsp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(AtUser::from_value)
            .collect::<Result<Vec<AtUser>, _>>()?;

        Ok(at_users)
    }

    /// 获取最近注册的 20 个用户
    ///
    /// 返回用户列表
    pub async fn recent_register(&self) -> Result<Vec<UserLite>, Error> {
        let url = "api/user/recentReg".to_string();

        let rsp = get(&url).await?;

        let user_lites = rsp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(UserLite::from_value)
            .collect::<Result<Vec<UserLite>, _>>()?;

        Ok(user_lites)
    }

    /// 获取用户VIP信息
    ///
    /// - `user_id` 用户ID
    ///
    /// 返回用户VIP信息
    pub async fn vip_info(&self, user_id: &str) -> Result<UserVipInfo, Error> {
        let url = format!("api/membership/{}", user_id);

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(0) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let data_obj = rsp["data"]
            .as_object()
            .ok_or_else(|| Error::Api("Data is not an object".to_string()))?;

        let config_json_str = data_obj
            .get("configJson")
            .and_then(|v| v.as_str())
            .unwrap_or("null");
        let mut data: Value = serde_json::from_str(config_json_str).unwrap_or_else(|_| json!({}));

        if let Some(data_map) = data.as_object_mut() {
            data_map.insert(
                "state".to_string(),
                data_obj.get("state").cloned().unwrap_or(Value::Null),
            );
            if data_obj.get("state").and_then(|v| v.as_i64()).unwrap_or(0) == 1 {
                data_map.insert(
                    "oId".to_string(),
                    data_obj.get("oId").cloned().unwrap_or(Value::Null),
                );
                data_map.insert(
                    "userId".to_string(),
                    data_obj.get("userId").cloned().unwrap_or(Value::Null),
                );
                data_map.insert(
                    "lvCode".to_string(),
                    data_obj.get("lvCode").cloned().unwrap_or(Value::Null),
                );
                data_map.insert(
                    "expiresAt".to_string(),
                    data_obj.get("expiresAt").cloned().unwrap_or(Value::Null),
                );
                data_map.insert(
                    "createdAt".to_string(),
                    data_obj.get("createdAt").cloned().unwrap_or(Value::Null),
                );
                data_map.insert(
                    "updatedAt".to_string(),
                    data_obj.get("updatedAt").cloned().unwrap_or(Value::Null),
                );
            }
        }

        UserVipInfo::from_value(&data)
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

    /// 获取操作日志
    ///
    /// - `page` 页码
    /// - `page_size` 每页数量
    ///
    /// 返回日志列表
    pub async fn log(&self, page: u32, page_size: u32) -> Result<Vec<Log>, Error> {
        let url = format!("logs/more?page={}&pageSize={}", page, page_size);

        let rsp = get(&url).await?;

        let logs = rsp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(Log::from_value)
            .collect::<Result<Vec<Log>, _>>()?;

        Ok(logs)
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
}
