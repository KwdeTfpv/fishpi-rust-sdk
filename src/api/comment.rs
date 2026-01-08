//! 评论 API 模块
//!
//! 这个模块提供了与评论相关的 API 操作，包括发布评论、更新评论、点赞评论、感谢评论、删除评论等功能。
//! 主要结构体是 `Comment`，用于管理评论的 API 请求。
//!
//! # 主要组件
//!
//! - [`Comment`] - 评论客户端结构体，负责所有评论相关的 API 调用。
//!
//! # 方法列表
//!
//! - [`Comment::new`] - 创建新的评论客户端实例。
//! - [`Comment::send`] - 发布评论。
//! - [`Comment::update`] - 更新评论。
//! - [`Comment::vote`] - 评论点赞。
//! - [`Comment::thank`] - 评论感谢。
//! - [`Comment::remove`] - 删除评论。
//!
//! # 示例
//!
//! ```rust,no_run
//! use crate::api::comment::Comment;
//! use crate::model::article::CommentPost;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let comment = Comment::new("your_api_key".to_string());
//!
//!     // 发布评论
//!     let data = CommentPost {
//!         article_id: "article_id".to_string(),
//!         content: "This is a comment.".to_string(),
//!         reply_id: None,
//!     };
//!     let result = comment.send(&data).await?;
//!     println!("Sent: {}", result.success);
//!
//!     // 更新评论
//!     let updated_content = comment.update("comment_id", &data).await?;
//!     println!("Updated content: {}", updated_content);
//!
//!     // 点赞评论
//!     let voted = comment.vote("comment_id", true).await?;
//!     println!("Voted: {}", voted);
//!
//!     Ok(())
//! }
//! ```
use serde_json::{Value, json};

use crate::{
    model::article::CommentPost,
    utils::{ResponseResult, error::Error, post, put},
};

pub struct Comment {
    api_key: String,
}

impl Comment {
    pub fn new(api_key: String) -> Self {
        Self { api_key }
    }

    /// 发布评论
    ///
    /// - `data` 评论信息
    ///
    /// 返回执行结果
    pub async fn send(&self, data: &CommentPost) -> Result<ResponseResult, Error> {
        let url = "comment".to_string();

        let mut data_json = data.to_value()?;
        data_json["apiKey"] = Value::String(self.api_key.clone());

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 更新评论
    ///
    /// - `id` 评论 Id
    /// - `data` 评论信息
    ///
    /// 返回评论内容 HTML
    pub async fn update(&self, id: &str, data: &CommentPost) -> Result<String, Error> {
        let url = format!("comment/{}", id);

        let mut data_json = data.to_value()?;
        data_json["apiKey"] = Value::String(self.api_key.clone());

        let rsp = put(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["commentContent"].as_str().unwrap_or("").to_string())
    }

    /// 评论点赞
    ///
    /// - `id` 评论 Id
    /// - `like` 点赞类型，true 为点赞，false 为点踩
    ///
    /// 返回评论点赞状态，true 为点赞，false 为点踩
    pub async fn vote(&self, id: &str, like: bool) -> Result<bool, Error> {
        let action = if like { "up" } else { "down" };
        let url = format!("vote/{}/comment", action);

        let data_json = json!({
            "dataId": id,
            "apiKey": self.api_key,
        });

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["type"].as_i64().unwrap_or(-1) == 0)
    }

    /// 评论感谢
    ///
    /// - `id` 评论 Id
    ///
    /// 返回执行结果
    pub async fn thank(&self, id: &str) -> Result<ResponseResult, Error> {
        let url = "comment/thank".to_string();

        let data_json = json!({
            "apiKey": self.api_key,
            "commentId": id,
        });

        let rsp = post(&url, Some(data_json)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 删除评论
    ///
    /// - `id` 评论 Id
    ///
    /// 返回删除的评论 Id
    pub async fn remove(&self, id: &str) -> Result<String, Error> {
        let url = format!("comment/{}/remove", id);

        let data_json = json!({
            "apiKey": self.api_key,
        });

        let rsp = post(&url, Some(data_json)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["commentId"].as_str().unwrap_or("").to_string())
    }
}
