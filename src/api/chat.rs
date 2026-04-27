//! 私聊 API 模块
//!
//! 这个模块提供了与私聊相关的 API 操作，包括连接私聊 WebSocket、监听私聊事件、获取消息列表、历史消息、标记已读、撤回消息等功能。
//! 主要结构体是 `Chat`，用于管理私聊的 WebSocket 连接和事件监听。
//! 事件通过 `ChatEventData` 枚举表示，支持通知、数据、撤回等类型。
//!
//! # 主要组件
//!
//! - [`Chat`] - 私聊客户端结构体，负责连接、发送消息和管理监听器。
//! - [`ChatHandler`] - 私聊消息处理器，实现 `MessageHandler` trait，处理 WebSocket 消息并发射事件。
//! - [`ChatEventData`] - 私聊事件数据枚举，包装所有事件类型（如通知、数据、撤回等）。
//! - [`ChatEventType`] - 私聊事件类型枚举，用于标识事件种类。
//! - [`ChatListener`] - 私聊事件监听器类型别名，定义监听器函数的签名。
//!
//! # 方法列表
//!
//! - [`Chat::new`] - 创建新的私聊客户端实例。
//! - [`Chat::connect`] - 连接私聊 WebSocket。
//! - [`Chat::reconnect`] - 重连私聊 WebSocket。
//! - [`Chat::on_notice`] - 监听通知消息事件。
//! - [`Chat::on_data`] - 监听普通消息事件。
//! - [`Chat::on_revoke`] - 监听消息撤回事件。
//! - [`Chat::off`] - 移除事件监听器。
//! - [`Chat::disconnect`] - 断开连接。
//! - [`Chat::list`] - 获取有私聊用户列表第一条消息。
//! - [`Chat::history`] - 获取用户私聊历史消息。
//! - [`Chat::mark_as_read`] - 标记用户消息已读。
//! - [`Chat::unread`] - 获取未读消息。
//! - [`Chat::revoke`] - 撤回私聊消息。
//!
//! # 示例
//!
//! ```rust,no_run
//! use fishpi_sdk::api::chat::Chat;
//! use fishpi_sdk::model::chat::{ChatData, ChatNotice, ChatRevoke};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut chat = Chat::new("your_api_key".to_string());
//!
//!     // 监听通知消息（直接传递 ChatNotice，无需 match）
//!     chat.on_notice(|notice: ChatNotice| {
//!         println!("Notice: {}", notice.preview);
//!     }).await;
//!
//!     // 监听普通消息
//!     chat.on_data(|data: ChatData| {
//!         println!("Message: {}", data.content);
//!     }).await;
//!
//!     // 监听撤回消息
//!     chat.on_revoke(|revoke: ChatRevoke| {
//!         println!("Revoked: {}", revoke.data);
//!     }).await;
//!
//!     // 连接私聊
//!     chat.connect(false, Some("target_user".to_string())).await?;
//!
//!     // 获取历史消息
//!     let history = chat.history("target_user".to_string(), 1, 20, true).await?;
//!     for msg in history {
//!         println!("History: {}", msg.content);
//!     }
//!
//!     Ok(())
//! }
//! ```
//!
//! # 事件类型
//!
//! 私聊支持以下事件类型（通过特定 `on_*` 方法监听）：
//!
//! - `Notice` - 通知消息。
//! - `Data` - 普通消息。
//! - `Revoke` - 消息撤回。

use crate::{
    api::ws::{
        ParsedMessageHandler, RetryPolicy, WebSocketError, WsConnection, WsLogHook, build_ws_url,
    },
    model::chat::{ChatData, ChatMsgType, ChatNotice, ChatRevoke},
    utils::{build_http_path, error::Error, get},
};
use serde_json::Value;
use std::{str::FromStr, sync::Arc};

const DOMAIN: &str = "fishpi.cn";

#[derive(Clone, Debug)]
pub enum ChatEventData {
    Notice(ChatNotice),
    Data(ChatData),
    Revoke(ChatRevoke),
}

/// 私聊事件类型枚举
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ChatEventType {
    Notice,
    Data,
    Revoke,
}
pub type ChatListener = Arc<dyn Fn(ChatEventData) + Send + Sync + 'static>;

/// 消息处理器
pub type ChatHandler = ParsedMessageHandler<ChatEventType, ChatEventData>;

/// 解析私聊消息，返回(事件类型，事件数据)
#[allow(non_snake_case)]
fn parse_chat_message(json: &Value) -> Result<(ChatEventType, ChatEventData), Error> {
    let event_type = detect_chat_msg_type(json)?;
    let payload = json.get("data").filter(|v| !v.is_null()).unwrap_or(json);

    match event_type {
        ChatMsgType::Notice => {
            let notice = ChatNotice::from_value(payload).or_else(|_| ChatNotice::from_value(json))?;
            Ok((ChatEventType::Notice, ChatEventData::Notice(notice)))
        }
        ChatMsgType::Data => {
            let data = ChatData::from_value(payload).or_else(|_| ChatData::from_value(json))?;
            Ok((ChatEventType::Data, ChatEventData::Data(data)))
        }
        ChatMsgType::Revoke => {
            let revoke = ChatRevoke::from_value(payload).or_else(|_| ChatRevoke::from_value(json))?;
            Ok((ChatEventType::Revoke, ChatEventData::Revoke(revoke)))
        }
    }
}

fn detect_chat_msg_type(json: &Value) -> Result<ChatMsgType, Error> {
    let candidates = [
        json.get("type"),
        json.get("command"),
        json.get("data").and_then(|v| v.get("type")),
        json.get("data").and_then(|v| v.get("command")),
    ];

    for candidate in candidates {
        if let Some(raw) = candidate.and_then(|v| v.as_str())
            && let Ok(t) = ChatMsgType::from_str(raw)
        {
            return Ok(t);
        }
    }

    let payload = json.get("data").filter(|v| v.is_object()).unwrap_or(json);

    // 兼容部分节点的无 type/command 推送格式
    if payload.get("senderUserName").is_some() && payload.get("receiverUserName").is_some() {
        return Ok(ChatMsgType::Data);
    }
    if payload.get("userId").is_some() && payload.get("preview").is_some() {
        return Ok(ChatMsgType::Notice);
    }
    if payload.get("data").and_then(|v| v.as_str()).is_some() {
        return Ok(ChatMsgType::Revoke);
    }

    Err(Error::Parse("Missing type/command field".to_string()))
}

/// 私聊客户端
pub struct Chat {
    connection: WsConnection,
    handler: ChatHandler,
    api_key: String,
}

impl Chat {
    pub fn new(api_key: String) -> Self {
        Self {
            connection: WsConnection::new(),
            handler: ChatHandler::new(parse_chat_message, None, "chat"),
            api_key,
        }
    }

    fn ws_url(&self, user: Option<&str>) -> Result<String, WebSocketError> {
        let mut params = vec![("apiKey", self.api_key.clone())];
        let path = if let Some(user) = user {
            params.push(("toUser", user.to_string()));
            "chat-channel"
        } else {
            "user-channel"
        };

        build_ws_url(DOMAIN, path, &params)
    }

    pub async fn connect(
        &mut self,
        reload: bool,
        user: Option<String>,
    ) -> Result<(), WebSocketError> {
        let url = self.ws_url(user.as_deref())?;

        self.connection
            .connect(reload, &url, self.handler.clone())
            .await
    }

    /// 重连
    pub async fn reconnect(&mut self, user: Option<String>) -> Result<(), WebSocketError> {
        let url = self.ws_url(user.as_deref())?;

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

    /// 监听通知消息事件
    pub async fn on_notice<F>(&self, listener: F)
    where
        F: Fn(ChatNotice) + Send + Sync + 'static,
    {
        self.add_listener(ChatEventType::Notice, move |event: ChatEventData| {
            if let ChatEventData::Notice(notice) = event {
                listener(notice);
            }
        }).await;
    }

    /// 监听普通消息事件
    pub async fn on_data<F>(&self, listener: F)
    where
        F: Fn(ChatData) + Send + Sync + 'static,
    {
        self.add_listener(ChatEventType::Data, move |event: ChatEventData| {
            if let ChatEventData::Data(data) = event {
                listener(data);
            }
        }).await;
    }

    /// 监听消息撤回事件
    pub async fn on_revoke<F>(&self, listener: F)
    where
        F: Fn(ChatRevoke) + Send + Sync + 'static,
    {
        self.add_listener(ChatEventType::Revoke, move |event: ChatEventData| {
            if let ChatEventData::Revoke(revoke) = event {
                listener(revoke);
            }
        }).await;
    }

    async fn add_listener<F>(&self, event: ChatEventType, listener: F)
    where
        F: Fn(ChatEventData) + Send + Sync + 'static,
    {
        self.handler.get_emitter().add_listener(event, listener).await;
    }

    /// 移除监听
    pub async fn off(&self, event: ChatEventType) {
        self.handler
            .get_emitter()
            .remove_listener(Some(event))
            .await;
    }

    /// 断开连接
    pub fn disconnect(&mut self) {
        self.connection.disconnect();
    }

    /// 通过已连接的 chat-channel 发送私聊消息
    ///
    /// 该方法要求先调用 `connect(..., Some(to_user))` 建立目标会话连接。
    pub fn send_ws(&self, content: &str) -> Result<(), Error> {
        self.connection
            .send_text(content)
            .map_err(|e| Error::Api(format!("WS send failed: {}", e)))
    }

    /// 获取有私聊用户列表第一条消息
    ///
    /// 返回 私聊消息列表
    pub async fn list(&self) -> Result<Vec<ChatData>, Error> {
        let url = build_http_path("chat/get-list", &[("apiKey", self.api_key.clone())]);

        let resp = get(&url).await?;

        if let Some(code) = resp.get("code").and_then(|c| c.as_i64())
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let mut chat_list = Vec::new();
        if let Some(list) = resp["data"].as_array() {
            for item in list {
                let chat_data = ChatData::from_value(item)?;
                chat_list.push(chat_data);
            }
        }

        Ok(chat_list)
    }

    /// 获取用户私聊历史消息
    ///
    /// * `page` 页数
    /// * `size` 每页消息数量
    /// * `autoread` 是否自动标记为已读
    ///
    /// 返回 私聊消息列表
    pub async fn history(
        &self,
        user: String,
        page: u32,
        size: u32,
        autoread: bool,
    ) -> Result<Vec<ChatData>, Error> {
        let url = build_http_path(
            "chat/get-message",
            &[
                ("apiKey", self.api_key.clone()),
                ("page", page.to_string()),
                ("pageSize", size.to_string()),
                ("toUser", user.clone()),
            ],
        );
        let resp = get(&url).await?;
        if let Some(code) = resp.get("result").and_then(|c| c.as_i64())
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }
        let mut chat_list = Vec::new();
        if let Some(list) = resp["data"].as_array() {
            for item in list {
                let chat_data = ChatData::from_value(item)?;
                chat_list.push(chat_data);
            }
        }
        if autoread {
            self.mark_as_read(user).await?;
        }
        Ok(chat_list)
    }

    /// 标记用户消息已读
    ///
    /// - `user` 用户名
    ///
    /// 返回 执行结果
    pub async fn mark_as_read(&self, user: String) -> Result<bool, Error> {
        let to_user_url = build_http_path(
            "chat/mark-as-read",
            &[("toUser", user.clone()), ("apiKey", self.api_key.clone())],
        );
        let first = get(&to_user_url).await;
        match first {
            Ok(resp) => {
                if let Some(code) = resp.get("result").and_then(|c| c.as_i64()) {
                    if code == 0 {
                        return Ok(true);
                    }
                    let msg = resp["msg"].as_str().unwrap_or("API error").to_string();
                    let need_from_user_retry =
                        msg.contains("fromUserJSON") || msg.contains("Cannot invoke");
                    if !need_from_user_retry {
                        return Err(Error::Api(msg));
                    }

                    // Some backend nodes require fromUser for mark-as-read.
                    let from_user_url = build_http_path(
                        "chat/mark-as-read",
                        &[("fromUser", user), ("apiKey", self.api_key.clone())],
                    );
                    let resp = get(&from_user_url).await?;
                    if let Some(code) = resp.get("result").and_then(|c| c.as_i64())
                        && code != 0
                    {
                        return Err(Error::Api(
                            resp["msg"].as_str().unwrap_or("API error").to_string(),
                        ));
                    }
                    return Ok(true);
                }
                Ok(false)
            }
            Err(err) => {
                let err_text = err.to_string();
                if !(err_text.contains("fromUserJSON") || err_text.contains("Cannot invoke")) {
                    return Err(err);
                }

                // Some backend nodes require fromUser for mark-as-read.
                let from_user_url = build_http_path(
                    "chat/mark-as-read",
                    &[("fromUser", user), ("apiKey", self.api_key.clone())],
                );
                let resp = get(&from_user_url).await?;
                if let Some(code) = resp.get("result").and_then(|c| c.as_i64())
                    && code != 0
                {
                    return Err(Error::Api(
                        resp["msg"].as_str().unwrap_or("API error").to_string(),
                    ));
                }
                Ok(true)
            }
        }
    }

    /// 获取未读消息
    ///
    /// 返回 未读消息列表
    pub async fn unread(&self) -> Result<Vec<ChatData>, Error> {
        let url = build_http_path("chat/has-unread", &[("apiKey", self.api_key.clone())]);
        let resp = get(&url).await?;

        let unread_len = resp["result"].as_i64().unwrap_or(0);
        if unread_len == 0 {
            return Ok(Vec::new());
        }

        let chat_list = resp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(ChatData::from_value)
            .collect::<Result<Vec<_>, _>>()?;

        Ok(chat_list)
    }

    /// 撤回私聊消息
    ///
    /// - `msgId` 消息 ID
    ///
    /// 返回 执行结果
    pub async fn revoke(&self, msg_id: &str) -> Result<bool, Error> {
        let url = build_http_path(
            "chat/revoke",
            &[("apiKey", self.api_key.clone()), ("oId", msg_id.to_string())],
        );
        let resp = get(&url).await?;

        if let Some(code) = resp.get("result").and_then(|c| c.as_i64())
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        Ok(true)
    }
}

#[cfg(test)]
mod tests {
    use super::{ChatEventData, ChatEventType, parse_chat_message};
    use serde_json::json;

    #[test]
    fn parse_chat_notice_message() {
        let payload = json!({
            "type": "notice",
            "data": {
                "command": "notice",
                "userId": "u1",
                "preview": "hi",
                "senderAvatar": "a",
                "senderUserName": "bob"
            }
        });

        let (event_type, event) = parse_chat_message(&payload).expect("should parse");
        assert!(matches!(event_type, ChatEventType::Notice));
        match event {
            ChatEventData::Notice(n) => assert_eq!(n.preview, "hi"),
            _ => panic!("unexpected event variant"),
        }
    }

    #[test]
    fn parse_chat_invalid_type_fails() {
        let payload = json!({
            "type": "unknown",
            "data": {}
        });

        assert!(parse_chat_message(&payload).is_err());
    }

    #[test]
    fn parse_chat_notice_without_type_field() {
        let payload = json!({
            "command": "notice",
            "data": {
                "command": "notice",
                "userId": "u1",
                "preview": "hello",
                "senderAvatar": "a",
                "senderUserName": "alice"
            }
        });

        let (event_type, event) = parse_chat_message(&payload).expect("should parse");
        assert!(matches!(event_type, ChatEventType::Notice));
        match event {
            ChatEventData::Notice(n) => assert_eq!(n.preview, "hello"),
            _ => panic!("unexpected event variant"),
        }
    }
}
