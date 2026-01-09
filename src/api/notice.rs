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
//! use crate::api::notice::{Notice, NoticeMsg};
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

use std::{collections::HashMap, sync::Arc};

use serde_json::Value;
use tokio::sync::{Mutex, mpsc};

use crate::{
    api::ws::{MessageHandler, WebSocketClient, WebSocketError},
    model::notice::{NoticeCount, NoticeItem, NoticeList, NoticeMsg, NoticeMsgType, NoticeType},
    utils::{error::Error, get},
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
pub struct NoticeHandler {
    emitter: Arc<Mutex<HashMap<NoticeEventType, Vec<NoticeListener>>>>,
}

impl Default for NoticeHandler {
    fn default() -> Self {
        Self {
            emitter: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

impl NoticeHandler {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn get_emitter(&self) -> Arc<Mutex<HashMap<NoticeEventType, Vec<NoticeListener>>>> {
        self.emitter.clone()
    }

    async fn emit_event(
        emitter: &Arc<Mutex<HashMap<NoticeEventType, Vec<NoticeListener>>>>,
        event_type: NoticeEventType,
        event: NoticeEventData,
    ) {
        let listeners: Vec<NoticeListener> = {
            let guard = emitter.lock().await;
            guard.get(&event_type).cloned().unwrap_or_default()
        };
        for listener in listeners {
            let event = event.clone();
            tokio::spawn(async move { listener(event) });
        }
    }
}

impl MessageHandler for NoticeHandler {
    fn handle_message(&self, text: String) {
        if let Ok(json) = serde_json::from_str::<Value>(&text) {
            let emitter = self.get_emitter();
            tokio::spawn(async move {
                match parse_notice_message(&json) {
                    Ok((event_type, event)) => {
                        Self::emit_event(&emitter, event_type, event).await;
                    }
                    Err(e) => {
                        eprintln!("Failed to parse chat message: {}", e);
                    }
                }
            });
        }
    }
}

/// 解析通知消息，返回(事件类型，事件数据)
#[allow(non_snake_case)]
fn parse_notice_message(data: &Value) -> Result<(NoticeEventType, NoticeEventData), Error> {
    let command = data["command"]
        .as_str()
        .ok_or_else(|| Error::Parse("Missing command field".to_string()))?;

    if NoticeMsgType::values().contains(&command) {
        let msg = NoticeMsg::from_value(data)?;
        Ok((NoticeEventType::Msg, NoticeEventData::Msg(msg)))
    } else {
        Err(Error::Parse(format!("Unsupported command: {}", command)))
    }
}

impl Clone for NoticeHandler {
    fn clone(&self) -> Self {
        Self {
            emitter: self.emitter.clone(),
        }
    }
}

/// 通知客户端
pub struct Notice {
    ws: Option<WebSocketClient>,
    handler: NoticeHandler,
    sender: Option<mpsc::UnboundedSender<String>>,
    api_key: String,
}

impl Notice {
    pub fn new(api_key: String) -> Self {
        Self {
            ws: None,
            handler: NoticeHandler::new(),
            sender: None,
            api_key,
        }
    }

    pub async fn connect(&mut self, reload: bool) -> Result<(), WebSocketError> {
        if self.ws.is_some() && !reload {
            return Ok(());
        }

        let url = format!("wss://{}/user-channel?apiKey={}", DOMAIN, self.api_key);
        let (tx_send, _) = mpsc::unbounded_channel::<String>();
        self.sender = Some(tx_send);

        let ws = WebSocketClient::connect(&url, self.handler.clone()).await?;
        self.ws = Some(ws);
        Ok(())
    }

    /// 重连
    pub async fn reconnect(&mut self) -> Result<(), WebSocketError> {
        self.connect(true).await
    }

    /// 移除监听
    pub async fn off(&self, event_type: NoticeEventType) {
        let mut listeners = self.handler.emitter.lock().await;
        listeners.remove(&event_type);
    }

    /// 监听通知消息事件
    pub async fn on_notice<F>(&self, listener: F)
    where
        F: Fn(NoticeMsg) + Send + Sync + 'static,
    {
        self.add_listener(NoticeEventType::Msg, move |event: NoticeEventData| {
            let NoticeEventData::Msg(msg) = event;
            listener(msg);
        }).await;
    }

    async fn add_listener<F>(&self, event: NoticeEventType, listener: F)
    where
        F: Fn(NoticeEventData) + Send + Sync + 'static,
    {
        let wrapped_listener: NoticeListener = Arc::new(listener);
        let mut emitter = self.handler.emitter.lock().await;
        emitter
            .entry(event)
            .or_insert_with(Vec::new)
            .push(wrapped_listener);
    }

    /// 断开连接
    pub fn disconnect(&mut self) {
        if let Some(ws) = &mut self.ws {
            ws.disconnect();
        }
        self.ws = None;
        self.sender = None;
    }

    /// 获取未读消息数
    ///
    /// 返回 [NoticeCount]
    pub async fn count(&self) -> Result<NoticeCount, Error> {
        let url = format!("notifications/unread/count?apiKey={}", self.api_key);
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
        let url = format!(
            "api/getNotifications?apiKey={}&type={}",
            self.api_key,
            notice_type.as_str()
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
        let url = format!(
            "notifications/make-read/{}?apiKey={}",
            notice_type.as_str(),
            self.api_key
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
        let url = format!("notifications/all-read?apiKey={}", self.api_key);
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
