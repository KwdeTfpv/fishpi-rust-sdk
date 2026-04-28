//! 通知 API 模块
//!
//! 这个模块提供了与通知相关的 API 操作，包括连接通知 WebSocket、监听通知事件、获取未读消息数、查询消息列表、标记已读等功能。
//! 主要结构体是 `Notice`，用于管理通知的 WebSocket 连接和事件监听。
//! 事件通过 `NoticeEventData` 枚举表示，支持通知消息类型。
//!
//! # 主要组件
//!
//! - [`Notice`] - 通知客户端结构体，负责连接、监听和管理通知。
//! - [`NoticeHandler`] - 通知消息处理器，实现 `MessageHandler` trait，处理 WebSocket 消息并发射事件。
//! - [`NoticeEventData`] - 通知事件数据枚举，包装通知消息。
//! - [`NoticeListener`] - 通知事件监听器类型别名，定义监听器函数的签名。
//!
//! # 方法列表
//!
//! - [`Notice::new`] - 创建新的通知客户端实例。
//! - [`Notice::connect`] - 连接通知 WebSocket。
//! - [`Notice::reconnect`] - 重连通知 WebSocket。
//! - [`Notice::on_notice`] - 监听通知消息事件。
//! - [`Notice::off`] - 移除事件监听器。
//! - [`Notice::disconnect`] - 断开连接。
//! - [`Notice::count`] - 获取未读消息数。
//! - [`Notice::list`] - 获取消息列表。
//! - [`Notice::make_read`] - 已读指定类型消息。
//! - [`Notice::read_all`] - 已读所有消息。
//!
//! # 示例
//!
//! ```rust,no_run
//! use fishpi_sdk::api::notice::Notice;
//! use fishpi_sdk::model::notice::NoticeMsg;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut notice = Notice::new("your_api_key".to_string());
//!
//!     // 监听通知消息（直接传递 NoticeMsg，无需 match）
//!     notice.on_notice(|msg: NoticeMsg| {
//!         println!("Received message: {}", msg.content.unwrap_or_default());
//!     }).await;
//!
//!     // 连接通知
//!     notice.connect(false).await?;
//!
//!     // 获取未读消息数
//!     let count = notice.count().await?;
//!     println!("Unread count: {:?}", count);
//!
//!     Ok(())
//! }
//! ```
//!
//! # 事件类型
//!
//! 通知支持以下事件类型（通过特定 `on_*` 方法监听）：
//!
//! - `Msg` - 通知消息接收。

use std::sync::Arc;

use serde_json::Value;

use crate::{
    api::ws::{
        ParsedMessageHandler, RetryPolicy, WebSocketError, WsConnection, WsLogHook, build_ws_url,
    },
    model::notice::{NoticeCount, NoticeItem, NoticeList, NoticeMsg, NoticeMsgType, NoticeType},
    utils::{build_http_path, error::Error, get},
};

const DOMAIN: &str = "fishpi.cn";

/// 通知项联合类型
#[derive(Clone, Debug)]
pub enum NoticeEventData {
    Msg(NoticeMsg),
}

/// 通知事件类型枚举
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum NoticeEventType {
    Msg,
}

pub type NoticeListener = Arc<dyn Fn(NoticeEventData) + Send + Sync + 'static>;

/// 消息处理器
pub type NoticeHandler = ParsedMessageHandler<NoticeEventType, NoticeEventData>;

/// 解析通知消息，返回(事件类型，事件数据)
#[allow(non_snake_case)]
fn parse_notice_message(data: &Value) -> Result<(NoticeEventType, NoticeEventData), Error> {
    let command = data
        .get("command")
        .and_then(|v| v.as_str())
        .or_else(|| {
            data.get("data")
                .and_then(|v| v.get("command"))
                .and_then(|v| v.as_str())
        })
        .ok_or_else(|| Error::Parse("Missing command field".to_string()))?;

    if NoticeMsgType::values().contains(&command) {
        let msg = NoticeMsg::from_value(data).or_else(|_| {
            data.get("data")
                .ok_or_else(|| Error::Parse("Missing data field".to_string()))
                .and_then(NoticeMsg::from_value)
        })?;
        Ok((NoticeEventType::Msg, NoticeEventData::Msg(msg)))
    } else {
        Err(Error::Parse(format!("Unsupported command: {}", command)))
    }
}

/// 通知客户端
pub struct Notice {
    connection: WsConnection,
    handler: NoticeHandler,
    api_key: String,
}

impl Notice {
    pub fn new(api_key: String) -> Self {
        Self {
            connection: WsConnection::new(),
            handler: NoticeHandler::new(parse_notice_message, None, "notice"),
            api_key,
        }
    }

    fn ws_url(&self) -> Result<String, WebSocketError> {
        build_ws_url(DOMAIN, "user-channel", &[("apiKey", self.api_key.clone())])
    }

    pub async fn connect(&mut self, reload: bool) -> Result<(), WebSocketError> {
        let url = self.ws_url()?;
        self.connection
            .connect(reload, &url, self.handler.clone())
            .await
    }

    /// 重连
    pub async fn reconnect(&mut self) -> Result<(), WebSocketError> {
        let url = self.ws_url()?;
        self.connection.reconnect(&url, self.handler.clone()).await
    }

    pub fn set_reconnect_policy(&mut self, policy: RetryPolicy) {
        self.connection.set_retry_policy(policy);
    }

    pub fn on_ws_log<F>(&mut self, hook: F)
    where
        F: Fn(&str) + Send + Sync + 'static,
    {
        let hook = Arc::new(hook) as WsLogHook;
        self.connection.set_log_hook_arc(hook.clone());
        self.handler.set_log_hook_arc(hook);
    }

    /// 移除监听
    pub async fn off(&self, event_type: NoticeEventType) {
        self.handler
            .get_emitter()
            .remove_listener(Some(event_type))
            .await;
    }

    /// 监听通知消息事件
    pub async fn on_notice<F>(&self, listener: F)
    where
        F: Fn(NoticeMsg) + Send + Sync + 'static,
    {
        self.add_listener(NoticeEventType::Msg, move |event: NoticeEventData| {
            let NoticeEventData::Msg(msg) = event;
            listener(msg);
        })
        .await;
    }

    async fn add_listener<F>(&self, event: NoticeEventType, listener: F)
    where
        F: Fn(NoticeEventData) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(event, listener)
            .await;
    }

    /// 断开连接
    pub fn disconnect(&mut self) {
        self.connection.disconnect();
    }

    /// 获取未读消息数
    ///
    /// 返回 [NoticeCount]
    pub async fn count(&self) -> Result<NoticeCount, Error> {
        let url = build_http_path(
            "notifications/unread/count",
            &[("apiKey", self.api_key.clone())],
        );
        let resp = get(&url).await?;
        let count = NoticeCount::from_value(&resp)?;

        Ok(count)
    }

    /// 获取消息列表
    ///
    /// * `type` 消息类型
    ///
    /// 返回消息列表
    pub async fn list(&self, notice_type: NoticeType) -> Result<NoticeList, Error> {
        let url = build_http_path(
            "api/getNotifications",
            &[
                ("apiKey", self.api_key.clone()),
                ("type", notice_type.as_str().to_string()),
            ],
        );
        let resp = get(&url).await?;

        let data_array = resp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?;
        let list: Vec<NoticeItem> = data_array
            .iter()
            .map(|item| NoticeItem::from_value(item, &notice_type))
            .collect::<Result<Vec<_>, _>>()?;
        Ok(list)
    }

    /// 已读指定类型消息
    ///
    /// - `type` 消息类型
    ///
    /// 返回执行结果
    pub async fn make_read(&self, notice_type: NoticeType) -> Result<bool, Error> {
        let url = build_http_path(
            &format!("notifications/make-read/{}", notice_type.as_str()),
            &[("apiKey", self.api_key.clone())],
        );
        let resp = get(&url).await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("Api error").to_string(),
            ));
        }

        Ok(true)
    }

    /// 已读所有消息
    pub async fn read_all(&self) -> Result<bool, Error> {
        let url = build_http_path(
            "notifications/all-read",
            &[("apiKey", self.api_key.clone())],
        );
        let resp = get(&url).await?;
        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("Api error").to_string(),
            ));
        }
        Ok(true)
    }
}

#[cfg(test)]
mod tests {
    use super::{NoticeEventData, NoticeEventType, parse_notice_message};
    use serde_json::json;

    #[test]
    fn parse_notice_warn_broadcast() {
        let payload = json!({
            "command": "warnBroadcast",
            "userId": "u1",
            "warnBroadcastText": "hello",
            "who": "system"
        });

        let (event_type, event) = parse_notice_message(&payload).expect("should parse");
        assert!(matches!(event_type, NoticeEventType::Msg));
        match event {
            NoticeEventData::Msg(msg) => assert_eq!(msg.content.as_deref(), Some("hello")),
        }
    }

    #[test]
    fn parse_notice_unsupported_command_fails() {
        let payload = json!({
            "command": "unknown",
            "userId": "u1"
        });

        assert!(parse_notice_message(&payload).is_err());
    }
}
