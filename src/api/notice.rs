//! 通知 API 模块
//!
//! 这个模块提供了与通知相关的 API 操作，包括连接通知 WebSocket、监听通知事件、获取未读消息数、查询消息列表、标记已读等功能。
//! 主要结构体是 `Notice`，用于管理通知的 WebSocket 连接和事件监听。
//! 事件通过 `NoticeEventData` 枚举表示，支持多种通知类型（如消息、系统通知等）。
//!
//! # 主要组件
//!
//! - [`Notice`] - 通知客户端结构体，负责连接、监听和管理通知。
//! - [`NoticeHandler`] - 通知消息处理器，实现 `MessageHandler` trait，处理 WebSocket 消息并发射事件。
//! - [`NoticeEventData`] - 通知事件数据枚举，包装所有事件类型（如连接成功、消息接收等）。
//! - [`NoticeListener`] - 通知事件监听器类型别名，定义监听器函数的签名。
//!
//! # 方法列表
//!
//! - [`Notice::new`] - 创建新的通知客户端实例。
//! - [`Notice::connect`] - 连接通知 WebSocket。
//! - [`Notice::reconnect`] - 重连通知 WebSocket。
//! - [`Notice::on`] - 添加事件监听器。
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
//! use crate::api::notice::{Notice, NoticeEventData, NoticeType};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut notice = Notice::new("your_api_key".to_string());
//!
//!     // 添加消息监听器
//!     notice.on("msg", |event: NoticeEventData| {
//!         match event {
//!             NoticeEventData::Msg(msg) => {
//!                 println!("Received message: {}", msg.content);
//!             }
//!             _ => {}
//!         }
//!     }).await;
//!
//!     // 连接通知
//!     notice.connect(false).await?;
//!
//!     // 获取未读消息数
//!     let count = notice.count().await?;
//!     println!("Unread count: {:?}", count);
//!
//!     // 获取消息列表
//!     let list = notice.list(NoticeType::SysAnnounce).await?;
//!     for item in list {
//!         println!("Message: {}", item.content);
//!     }
//!
//!     Ok(())
//! }
//! ```
//!
//! # 事件类型
//!
//! 通知支持以下事件类型（通过 `on` 方法监听）：
//!
//! - `"open"` - 连接成功。
//! - `"close"` - 连接断开。
//! - `"error"` - 连接错误。
//! - `"msg"` - 消息接收（包括系统公告、评论、点赞等）。
//! - `"all"` - 所有事件（除了自身）。
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
    Open,
    Close,
    Error(String),
    Msg(NoticeMsg),
}

pub type NoticeListener = Box<dyn Fn(NoticeEventData) + Send + Sync + 'static>;

/// 消息处理器
pub struct NoticeHandler {
    emitter: Arc<Mutex<HashMap<String, Vec<NoticeListener>>>>,
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

    pub fn get_emitter(&self) -> Arc<Mutex<HashMap<String, Vec<NoticeListener>>>> {
        self.emitter.clone()
    }

    async fn emit_event(
        emitter: &Arc<Mutex<HashMap<String, Vec<NoticeListener>>>>,
        event_type: &str,
        event: NoticeEventData,
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

impl MessageHandler for NoticeHandler {
    fn handle_message(&self, text: String) {
        if let Ok(json) = serde_json::from_str::<Value>(&text) {
            let emitter = self.get_emitter();
            tokio::spawn(async move {
                match parse_notice_message(&json) {
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

/// 解析通知消息，返回(事件类型，事件数据)
#[allow(non_snake_case)]
fn parse_notice_message(data: &Value) -> Result<(String, NoticeEventData), Error> {
    let command = data["command"]
        .as_str()
        .ok_or_else(|| Error::Parse("Missing command field".to_string()))?;

    if NoticeMsgType::values().contains(&command) {
        let msg = NoticeMsg::from_value(data)?;
        Ok((command.to_string(), NoticeEventData::Msg(msg)))
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

        let emitter = self.handler.get_emitter();
        ws.on_open({
            let emitter = emitter.clone();
            move || {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    NoticeHandler::emit_event(&emitter, "open", NoticeEventData::Open).await;
                });
            }
        })
        .await;

        ws.on_close({
            let emitter = emitter.clone();
            move |_reason| {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    NoticeHandler::emit_event(&emitter, "close", NoticeEventData::Close).await;
                });
            }
        })
        .await;

        ws.on_error({
            let emitter = emitter.clone();
            move |error| {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    NoticeHandler::emit_event(&emitter, "error", NoticeEventData::Error(error))
                        .await;
                });
            }
        })
        .await;

        self.ws = Some(ws);

        Ok(())
    }

    /// 重连
    pub async fn reconnect(&mut self) -> Result<(), WebSocketError> {
        self.connect(true).await
    }

    /// 监听事件
    ///
    /// #### 参数
    /// * `event_type` 事件类型
    /// * `listener` 监听器函数
    pub async fn on<F>(&self, event_type: &str, listener: F)
    where
        F: Fn(NoticeEventData) + Send + Sync + 'static,
    {
        let mut listeners = self.handler.emitter.lock().await;
        let entry = listeners
            .entry(event_type.to_string())
            .or_insert_with(Vec::new);
        entry.push(Box::new(listener));
    }

    /// 移除监听
    pub async fn off(&self, event_type: &str) {
        let mut listeners = self.handler.emitter.lock().await;
        listeners.remove(event_type);
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
