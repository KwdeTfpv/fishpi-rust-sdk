//! 清风明月 API 模块
//!
//! 这个模块提供了与清风明月相关的 API 操作，包括获取清风明月列表、发送清风明月等功能。
//! 主要结构体是 `BreezeMoon`，用于管理清风明月的 API 请求。
//!
//! # 主要组件
//!
//! - [`BreezeMoon`] - 清风明月客户端结构体，负责所有清风明月相关的 API 调用。
//!
//! # 方法列表
//!
//! - [`BreezeMoon::new`] - 创建新的清风明月客户端实例。
//! - [`BreezeMoon::list`] - 获取清风明月列表。
//! - [`BreezeMoon::send`] - 发送清风明月。
//!
//! # 示例
//!
//! ```rust,no_run
//! use crate::api::breezemoon::BreezeMoon;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let breezemoon = BreezeMoon::new("your_api_key".to_string());
//!
//!     // 获取清风明月列表
//!     let list = breezemoon.list(1, 20, None).await?;
//!     for item in list {
//!         println!("Content: {}", item.content);
//!     }
//!
//!     // 发送清风明月
//!     let result = breezemoon.send("Hello, world!").await?;
//!     println!("Sent: {}", result.success);
//!
//!     Ok(())
//! }
//! ```
use serde_json::json;

use crate::{
    model::breezemoon::BreezemoonContent,
    utils::{ResponseResult, error::Error, get, post},
};

pub struct BreezeMoon {
    api_key: String,
}

impl BreezeMoon {
    pub fn new(api_key: String) -> Self {
        Self { api_key }
    }

    /// 获取清风明月列表
    ///
    /// - `page` 消息页码
    /// - `size` 每页个数
    /// - `user` 用户名，可选（指定用户时查询该用户的清风明月）
    ///
    /// 返回清风明月内容列表 [BreezemoonContent]
    pub async fn list(
        &self,
        page: u32,
        size: u32,
        user: Option<&str>,
    ) -> Result<Vec<BreezemoonContent>, Error> {
        let base = if let Some(user) = user {
            format!("user/{}/", user)
        } else {
            "".to_string()
        };
        let url = format!(
            "api/{}breezemoons?p={}&size={}&apiKey={}",
            base, page, size, self.api_key
        );

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let data = rsp.get("data").unwrap_or(&rsp);
        let breezemoons = data["breezemoons"]
            .as_array()
            .ok_or_else(|| Error::Api("breezemoons is not an array".to_string()))?
            .iter()
            .map(BreezemoonContent::from_value)
            .collect::<Result<Vec<_>, _>>()?;

        Ok(breezemoons)
    }

    /// 发送清风明月
    ///
    /// - `content` 内容
    ///
    /// 返回执行结果
    pub async fn send(&self, content: &str) -> Result<ResponseResult, Error> {
        let url = "breezemoon".to_string();

        let data_json = json!({
            "apiKey": self.api_key,
            "breezemoonContent": content,
        });

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }
}
