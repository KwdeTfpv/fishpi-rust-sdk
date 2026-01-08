//! # 摸鱼派 Rust SDK
//!
//! 这是一个用于与摸鱼派社区 API 交互的 Rust SDK，提供用户管理、文章、聊天室、私聊、通知、清风明月、红包、评论、举报、日志、文件上传等功能的异步客户端。
//!
//! ## 主要组件
//!
//! - [`FishPi`] - 静态客户端，提供不需要认证的操作（如登录、注册、验证）。
//! - [`Client`] - 认证客户端，持有 API 密钥和各个子模块实例，提供需要认证的完整 API 操作。
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
//! use fishpi_sdk::{FishPi, Client};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     // 登录获取用户实例
//!     let user = FishPi::login(&login_data).await?;
//!
//!     // 创建认证客户端
//!     let client = Client::new(user.get_api_key().to_string());
//!
//!     // 获取用户信息
//!     let user_info = client.get_user("username").await?;
//!
//!     // 发送评论
//!     let result = client.comment.send(&comment_data).await?;
//!
//!     Ok(())
//! }
//! ```
pub mod api;
pub mod model;
pub mod utils;

use serde_json::{Value, json};

use crate::{
    api::user::User,
    model::{
        misc::{
            Log, LoginData, PreRegisterInfo, RegisterInfo,UserLite,UserVipInfo,
        },
        user::AtUser,
    },
    utils::{ResponseResult, error::Error, get, post},
};

/// 摸鱼派 Rust SDK 接口
pub struct FishPi;

impl FishPi {

    /// 登录
    ///
    /// - `data` 登录账密
    ///
    /// 返回用户实例
    pub async fn login(data: &LoginData) -> Result<User, Error> {
        let url = "api/getKey".to_string();

        let data_json = data.to_value()?;

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let token = rsp["Key"].as_str().unwrap_or("").trim().to_string();

        Ok(User::new(token))
    }

    /// 预注册
    ///
    /// - `data` 预注册数据
    ///
    /// 返回预注册结果
    pub async fn pre_register(data: &PreRegisterInfo) -> Result<ResponseResult, Error> {
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
    pub async fn verify(code: &str) -> Result<String, Error> {
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
    pub async fn register(data: &RegisterInfo) -> Result<ResponseResult, Error> {
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

    /// 获取用户名联想
    ///
    /// - `name` 用户名
    ///
    /// 返回用户名联想列表
    pub async fn names(name: &str) -> Result<Vec<AtUser>, Error> {
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
    pub async fn recent_register() -> Result<Vec<UserLite>, Error> {
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
    pub async fn vip_info(user_id: &str) -> Result<UserVipInfo, Error> {
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

    /// 获取操作日志
    ///
    /// - `page` 页码
    /// - `page_size` 每页数量
    ///
    /// 返回日志列表
    pub async fn log(page: u32, page_size: u32) -> Result<Vec<Log>, Error> {
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

}
