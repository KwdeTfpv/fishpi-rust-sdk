//! 文章 API 模块
//!
//! 这个模块提供了与文章相关的 API 操作，包括发布、更新、查询、点赞、感谢、收藏、关注、打赏、获取在线人数和 WebSocket 监听等功能。
//! 主要结构体是 `Article`，用于管理文章相关的 HTTP 请求和 WebSocket 连接。
//! 事件通过 `ArticleListener` 回调处理，支持实时消息监听。
//!
//! # 主要组件
//!
//! - [`Article`] - 文章客户端结构体，负责所有文章相关的 API 调用和 WebSocket 连接。
//! - [`ArticleMessageHandler`] - 文章消息处理器，实现 `MessageHandler` trait，处理 WebSocket 消息并调用回调。
//! - [`ArticleListener`] - 文章监听器类型别名，定义监听器函数的签名，用于处理接收到的消息。
//!
//! # 方法列表
//!
//! - [`Article::new`] - 创建新的文章客户端实例。
//! - [`Article::post_article`] - 发布新文章。
//! - [`Article::update_article`] - 更新现有文章。
//! - [`Article::list`] - 查询文章列表（支持类型、标签、分页）。
//! - [`Article::list_by_user`] - 查询指定用户的文章列表。
//! - [`Article::detail`] - 获取文章详情（包括评论分页）。
//! - [`Article::vote`] - 点赞或点踩文章。
//! - [`Article::thank`] - 感谢文章。
//! - [`Article::follow`] - 收藏或取消收藏文章。
//! - [`Article::watch`] - 关注或取消关注文章。
//! - [`Article::reward`] - 打赏文章。
//! - [`Article::heat`] - 获取文章在线人数。
//! - [`Article::add_listener`] - 添加文章 WebSocket 监听器。
//!
//! # 示例
//!
//! ```rust,no_run
//! use crate::api::article::{Article, ArticlePost, ArticleListener};
//! use serde_json::Value;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let article = Article::new("your_api_key".to_string());
//!
//!     // 发布文章
//!     let data = ArticlePost {
//!         title: "Test Title".to_string(),
//!         content: "Test Content".to_string(),
//!         tags: "test".to_string(),
//!         commentable: true,
//!         notify_followers: false,
//!         type_: 0,
//!         show_in_list: 1,
//!         reward_content: None,
//!         reward_point: None,
//!         anonymous: None,
//!         offer_point: None,
//!     };
//!     let article_id = article.post_article(&data).await?;
//!     println!("Published article ID: {}", article_id);
//!
//!     // 获取文章详情
//!     let detail = article.detail(&article_id, 1).await?;
//!     println!("Article title: {}", detail.title);
//!
//!     // 添加 WebSocket 监听器
//!     let callback: ArticleListener = Box::new(|msg: Value| {
//!         println!("Received message: {:?}", msg);
//!     });
//!     let ws_client = article.add_listener(&article_id, 0, callback).await?;
//!
//!     // 点赞文章
//!     let voted = article.vote(&article_id, true).await?;
//!     println!("Voted: {}", voted);
//!
//!     Ok(())
//! }
//! ```
use serde_json::{Value, json};

use crate::{
    api::ws::{MessageHandler, WebSocketClient},
    model::article::{ArticleDetail, ArticleList, ArticleListType, ArticlePost, Pagination},
    utils::{ResponseResult, error::Error, get, post},
};

/// 文章监听器类型
pub type ArticleListener = Box<dyn Fn(Value) + Send + Sync + 'static>;

/// 文章消息处理器
pub struct ArticleMessageHandler {
    callback: ArticleListener,
}

impl ArticleMessageHandler {
    pub fn new(callback: ArticleListener) -> Self {
        Self { callback }
    }
}

impl MessageHandler for ArticleMessageHandler {
    fn handle_message(&self, msg: String) {
        if let Ok(json) = serde_json::from_str::<Value>(&msg) {
            (self.callback)(json);
        } else {
            (self.callback)(Value::String(msg));
        }
    }
}

pub struct Article {
    api_key: String,
}

impl Article {
    pub fn new(api_key: String) -> Self {
        Self { api_key }
    }

    /// 发布文章
    ///
    /// * `data` 文章信息 [ArticlePost]
    ///
    /// 返回文章 Id
    pub async fn post_article(&self, data: &ArticlePost) -> Result<String, Error> {
        let url = "article".to_string();

        let mut data_json = data.to_json()?;
        data_json["apiKey"] = Value::String(self.api_key.clone());

        let resp = post(&url, Some(data_json)).await?;

        if resp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let article_id = resp["articleId"]
            .as_str()
            .ok_or_else(|| Error::Api("Missing articleId in response".to_string()))?
            .to_string();

        Ok(article_id)
    }

    /// 更新文章
    ///
    /// * `id` 文章 Id
    /// * `data` 文章信息 [ArticlePost]
    ///
    /// 返回文章 Id
    pub async fn update_article(&self, id: &str, data: &ArticlePost) -> Result<String, Error> {
        let url = format!("article/{}", id);

        let mut data_json = data.to_json()?;
        data_json["apiKey"] = Value::String(self.api_key.clone());

        let resp = post(&url, Some(data_json)).await?;

        if resp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let article_id = resp["articleId"]
            .as_str()
            .ok_or_else(|| Error::Api("Missing articleId in response".to_string()))?
            .to_string();

        Ok(article_id)
    }

    /// 查询文章列表
    ///
    /// * `type` 查询类型，来自 [ArticleListType]
    /// * `tag` 指定查询标签，可选
    /// * `page` 页码
    /// * `size` 每页数量
    ///
    /// 返回文章列表
    pub async fn list(
        &self,
        type_: ArticleListType,
        page: u32,
        size: u32,
        tag: Option<&str>,
    ) -> Result<ArticleList, Error> {
        let base = if let Some(tag) = tag {
            format!("tag/{}", tag)
        } else {
            "recent".to_string()
        };

        let url = format!(
            "api/articles/{}{}?p={}&size={}&apiKey={}",
            base,
            type_.to_code(),
            page,
            size,
            self.api_key
        );

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        ArticleList::from_value(&rsp["data"])
    }

    /// 查询文章列表
    ///
    /// - `user` 指定用户
    /// - `page` 页码
    /// - `size` 每页数量
    ///
    /// 返回文章列表
    pub async fn list_by_user(
        &self,
        user: &str,
        page: u32,
        size: u32,
    ) -> Result<ArticleList, Error> {
        let url = format!(
            "api/articles/user/{}?p={}&size={}&apiKey={}",
            user, page, size, self.api_key
        );

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        ArticleList::from_value(&rsp["data"])
    }

    /// 获取文章详情
    ///
    /// - `id` 文章id
    /// - `p` 评论页码
    ///
    /// 返回文章详情 [ArticleDetail]
    pub async fn detail(&self, id: &str, p: u32) -> Result<ArticleDetail, Error> {
        let url = format!("api/article/{}?p={}&apiKey={}", id, p, self.api_key);

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let data = &rsp["data"];
        let mut article_detail = ArticleDetail::from_value(&data["article"])?;
        article_detail.pagination = Some(Pagination::from_value(&data["pagination"])?);

        Ok(article_detail)
    }

    /// 点赞/取消点赞文章
    ///
    /// - `id` 文章id
    /// - `like` 点赞类型，true 为点赞，false 为点踩
    ///
    /// 返回文章点赞状态，true 为点赞，false 为点踩
    pub async fn vote(&self, id: &str, like: bool) -> Result<bool, Error> {
        let url = format!("vote/{}/article", if like { "up" } else { "down" });

        let data = json!({
            "dataId": id,
            "apiKey": self.api_key,
        });

        let rsp = post(&url, Some(data)).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(rsp["type"].as_i64().unwrap_or(0) == 0)
    }

    /// 感谢文章
    ///
    /// - `id` 文章id
    ///
    /// 返回执行结果
    pub async fn thank(&self, id: &str) -> Result<ResponseResult, Error> {
        let url = "article/thank".to_string();

        let data = json!({
            "apiKey": self.api_key,
            "articleId": id,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 收藏/取消收藏文章
    ///
    /// - `id` 文章id
    ///
    /// 返回执行结果
    pub async fn follow(&self, id: &str) -> Result<ResponseResult, Error> {
        let url = "follow/article".to_string();

        let data = json!({
            "apiKey": self.api_key,
            "followingId": id,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 关注/取消关注文章
    ///
    /// - `followingId` 文章id
    ///
    /// 返回执行结果
    pub async fn watch(&self, following_id: &str) -> Result<ResponseResult, Error> {
        let url = "follow/article-watch".to_string();

        let data = json!({
            "apiKey": self.api_key,
            "followingId": following_id,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 打赏文章
    ///
    /// - `id` 文章id
    ///
    /// 返回执行结果
    pub async fn reward(&self, id: &str) -> Result<ResponseResult, Error> {
        let url = format!("article/reward?articleId={}", id);

        let data = json!({
            "apiKey": self.api_key,
        });

        let rsp = post(&url, Some(data)).await?;

        ResponseResult::from_value(&rsp)
    }

    /// 获取文章在线人数
    ///
    /// - `id` 文章id
    ///
    /// 返回在线人数
    pub async fn heat(&self, id: &str) -> Result<u32, Error> {
        let url = format!("api/article/heat/{}?apiKey={}", id, self.api_key);

        let rsp = get(&url).await?;

        if rsp.get("code").and_then(|c| c.as_i64()).unwrap_or(-1) != 0 {
            return Err(Error::Api(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let heat = rsp["articleHeat"]
            .as_u64()
            .ok_or_else(|| Error::Api("Missing heat data in response".to_string()))?
            as u32;

        Ok(heat)
    }

    /// 添加文章监听器
    ///
    /// - `id` 文章id
    /// - `type_` 文章类型
    /// - `callback` 监听回调
    ///
    /// 返回 WebSocketClient
    pub async fn add_listener(
        &self,
        id: &str,
        type_: u32,
        callback: ArticleListener,
    ) -> Result<WebSocketClient, Error> {
        let url = format!(
            "wss://fishpi.cn/article-channel?apiKey={}&articleId={}&articleType={}",
            self.api_key, id, type_
        );

        let handler = ArticleMessageHandler::new(callback);
        let ws = WebSocketClient::connect(&url, handler)
            .await
            .map_err(|e| Error::Api(format!("WebSocket connection failed: {}", e)))?;

        // 添加默认 close 和 error 监听器
        ws.on_close(|reason| {
            println!("WebSocket is closed: {:?}", reason);
        })
        .await;

        ws.on_error(|error| {
            println!("WebSocket error: {}", error);
        })
        .await;

        Ok(ws)
    }
}
