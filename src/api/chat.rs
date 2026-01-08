//! 私聊 API 模块
//!
//! 这个模块提供了与私聊相关的 API 操作，包括连接私聊 WebSocket、监听私聊事件、获取消息列表、历史消息、标记已读、撤回消息等功能。
//! 主要结构体是 `Chat`，用于管理私聊的 WebSocket 连接和事件监听。
//! 事件通过 `ChatEventData` 枚举表示，支持多种消息类型（如通知、数据、撤回等）。
//!
//! # 主要组件
//!
//! - [`Chat`] - 私聊客户端结构体，负责连接、发送消息和管理监听器。
//! - [`ChatHandler`] - 私聊消息处理器，实现 `MessageHandler` trait，处理 WebSocket 消息并发射事件。
//! - [`ChatEventData`] - 私聊事件数据枚举，包装所有事件类型（如连接成功、消息接收、撤回等）。
//! - [`ChatListener`] - 私聊事件监听器类型别名，定义监听器函数的签名。
//!
//! # 方法列表
//!
//! - [`Chat::new`] - 创建新的私聊客户端实例。
//! - [`Chat::connect`] - 连接私聊 WebSocket。
//! - [`Chat::reconnect`] - 重连私聊 WebSocket。
//! - [`Chat::on`] - 添加事件监听器。
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
//! use crate::api::chat::{Chat, ChatEventData};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut chat = Chat::new("your_api_key".to_string());
//!
//!     // 添加消息监听器
//!     chat.on("data", |event: ChatEventData| {
//!         match event {
//!             ChatEventData::Data(data) => {
//!                 println!("Received message: {}", data.content);
//!             }
//!             _ => {}
//!         }
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
//!     // 撤回消息
//!     chat.revoke("message_id").await?;
//!
//!     Ok(())
//! }
//! ```
//!
//! # 事件类型
//!
//! 私聊支持以下事件类型（通过 `on` 方法监听）：
//!
//! - `"open"` - 连接成功。
//! - `"close"` - 连接断开。
//! - `"error"` - 连接错误。
//! - `"notice"` - 通知消息。
//! - `"data"` - 普通消息。
//! - `"revoke"` - 消息撤回。
//! - `"all"` - 所有事件（除了自身）。
use crate::{
    api::ws::{MessageHandler, WebSocketClient, WebSocketError},
    model::chat::{ChatData, ChatMsgType, ChatNotice, ChatRevoke},
    utils::{error::Error, get},
};
use serde_json::Value;
use std::{collections::HashMap, str::FromStr, sync::Arc};
use tokio::sync::{Mutex, mpsc};

const DOMAIN: &str = "fishpi.cn";

#[derive(Clone, Debug)]
pub enum ChatEventData {
    Open,
    Close,
    Error(String),
    Notice(ChatNotice),
    Data(ChatData),
    Revoke(ChatRevoke),
}

pub type ChatListener = Box<dyn Fn(ChatEventData) + Send + Sync + 'static>;

/// 消息处理器
pub struct ChatHandler {
    emitter: Arc<Mutex<HashMap<String, Vec<ChatListener>>>>,
}

impl Default for ChatHandler {
    fn default() -> Self {
        Self {
            emitter: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

impl ChatHandler {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn get_emitter(&self) -> Arc<Mutex<HashMap<String, Vec<ChatListener>>>> {
        self.emitter.clone()
    }

    async fn emit_event(
        emitter: &Arc<Mutex<HashMap<String, Vec<ChatListener>>>>,
        event_type: &str,
        event: ChatEventData,
    ) {
        let listeners = emitter.lock().await;
        if let Some(event_listeners) = listeners.get(event_type) {
            for listener in event_listeners {
                listener(event.clone());
            }
        }
        // 同时发送到 "all" 事件
        if event_type != "all"
            && let Some(all_listeners) = listeners.get("all")
        {
            for listener in all_listeners {
                listener(event.clone());
            }
        }
    }
}

impl MessageHandler for ChatHandler {
    fn handle_message(&self, text: String) {
        if let Ok(json) = serde_json::from_str::<Value>(&text) {
            let emitter = self.get_emitter();
            tokio::spawn(async move {
                match parse_chat_message(&json) {
                    Ok((event_type, event)) => {
                        Self::emit_event(&emitter, &event_type, event).await;
                    }
                    Err(e) => {
                        eprintln!("Failed to parse chat message: {}", e);
                    }
                }
            });
        }
    }
}

/// 解析私聊消息，返回(事件类型，事件数据)
#[allow(non_snake_case)]
fn parse_chat_message(json: &Value) -> Result<(String, ChatEventData), Error> {
    let type_str = json["type"]
        .as_str()
        .ok_or_else(|| Error::Parse("Missing type field".to_string()))?;
    let event_type = ChatMsgType::from_str(type_str)
        .map_err(|e| Error::Parse(format!("Invalid type field: {}", e)))?;

    match event_type {
        ChatMsgType::Notice => {
            let notice = ChatNotice::from_value(&json["data"])?;
            Ok((
                ChatMsgType::Notice.to_string(),
                ChatEventData::Notice(notice),
            ))
        }
        ChatMsgType::Data => {
            let data = ChatData::from_value(&json["data"])?;
            Ok((ChatMsgType::Data.to_string(), ChatEventData::Data(data)))
        }
        ChatMsgType::Revoke => {
            let revoke = ChatRevoke::from_value(&json["data"])?;
            Ok((
                ChatMsgType::Revoke.to_string(),
                ChatEventData::Revoke(revoke),
            ))
        }
    }
}

impl Clone for ChatHandler {
    fn clone(&self) -> Self {
        Self {
            emitter: self.emitter.clone(),
        }
    }
}

/// 私聊客户端
pub struct Chat {
    ws: Option<WebSocketClient>,
    handler: ChatHandler,
    sender: Option<mpsc::UnboundedSender<String>>,
    api_key: String,
}

impl Chat {
    pub fn new(api_key: String) -> Self {
        Self {
            ws: None,
            handler: ChatHandler::new(),
            sender: None,
            api_key,
        }
    }

    pub async fn connect(
        &mut self,
        reload: bool,
        user: Option<String>,
    ) -> Result<(), WebSocketError> {
        if self.ws.is_some() && !reload {
            return Ok(());
        }

        let url = if let Some(user) = user {
            format!(
                "wss://{}/chat-channel?apiKey={}&toUser={}",
                DOMAIN, self.api_key, user
            )
        } else {
            format!("wss://{}/user-channel?apiKey={}", DOMAIN, self.api_key)
        };

        let (tx_send, _) = mpsc::unbounded_channel::<String>();
        self.sender = Some(tx_send);

        let ws = WebSocketClient::connect(&url, self.handler.clone()).await?;

        let emitter = self.handler.get_emitter();
        ws.on_open({
            let emitter = emitter.clone();
            move || {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    ChatHandler::emit_event(&emitter, "open", ChatEventData::Open).await;
                });
            }
        })
        .await;

        ws.on_close({
            let emitter = emitter.clone();
            move |_reason| {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    ChatHandler::emit_event(&emitter, "close", ChatEventData::Close).await;
                });
            }
        })
        .await;

        ws.on_error({
            let emitter = emitter.clone();
            move |err| {
                let emitter = emitter.clone();
                let err_msg = err.to_string();
                tokio::spawn(async move {
                    ChatHandler::emit_event(&emitter, "error", ChatEventData::Error(err_msg)).await;
                });
            }
        })
        .await;

        self.ws = Some(ws);
        Ok(())
    }

    /// 重连
    pub async fn reconnect(&mut self, user: Option<String>) -> Result<(), WebSocketError> {
        self.connect(true, user).await
    }

    /// 监听事件
    ///
    /// #### 参数
    /// * `event_type` 事件类型
    /// * `listener` 监听器函数
    pub async fn on<F>(&self, event_type: &str, listener: F)
    where
        F: Fn(ChatEventData) + Send + Sync + 'static,
    {
        let mut emitter = self.handler.emitter.lock().await;
        let listeners = emitter
            .entry(event_type.to_string())
            .or_insert_with(Vec::new);
        listeners.push(Box::new(listener));
    }

    /// 移除监听
    pub async fn off(&self, event: &str) {
        let mut emitter = self.handler.emitter.lock().await;
        emitter.remove(event);
    }

    /// 断开连接
    pub fn disconnect(&mut self) {
        if let Some(ws) = &mut self.ws {
            ws.disconnect();
        }
        self.ws = None;
        self.sender = None;
    }

    /// 获取有私聊用户列表第一条消息
    ///
    /// 返回 私聊消息列表
    pub async fn list(&self) -> Result<Vec<ChatData>, Error> {
        let url = format!("chat/get-list?apiKey={}", self.api_key);

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
        // chat/get-message?apiKey=${this.apiKey}&toUser=${this.user}&page=${page}&pageSize=${size}
        let url = format!(
            "chat/get-message?apiKey={}&page={}&pageSize={}&toUser={}",
            self.api_key, page, size, user
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
        let url = format!("chat/mark-as-read?toUser={}&apiKey={}", user, self.api_key);

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

    /// 获取未读消息
    ///
    /// 返回 未读消息列表
    pub async fn unread(&self) -> Result<Vec<ChatData>, Error> {
        let url = format!("chat/has-unread?apiKey={}", self.api_key);
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
        let url = format!("chat/revoke?apiKey={}&oId={}", self.api_key, msg_id);
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
