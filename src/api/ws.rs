//! WebSocket API 模块
//!
//! 这个模块提供了基础的 WebSocket 客户端功能，包括连接、监听事件、处理消息等。
//! 主要结构体是 `WebSocketClient`，用于管理 WebSocket 连接和事件监听。
//! 事件通过 `WsBaseEvent` 枚举表示，支持连接、断开和错误事件。
//!
//! # 主要组件
//!
//! - [`WebSocketClient`] - WebSocket 客户端结构体，负责连接和管理监听器。
//! - [`MessageHandler`] - WebSocket 消息处理器 trait，用于处理接收到的文本消息。
//! - [`WsBaseEvent`] - WebSocket 基础事件枚举，包装连接、断开和错误事件。
//! - [`EventListener`] - 事件监听器类型别名，定义监听器函数的签名。
//! - [`WebSocketError`] - WebSocket 错误类型，用于连接和操作错误。
//!
//! # 方法列表
//!
//! - [`WebSocketClient::connect`] - 创建并连接 WebSocket。
//! - [`WebSocketClient::add_listener`] - 添加事件监听器。
//! - [`WebSocketClient::on_open`] - 监听连接成功事件。
//! - [`WebSocketClient::on_close`] - 监听连接断开事件。
//! - [`WebSocketClient::on_error`] - 监听连接错误事件。
//! - [`WebSocketClient::remove_listener`] - 移除事件监听器。
//! - [`WebSocketClient::disconnect`] - 断开连接。
//!
//! # 示例
//!
//! ```rust,no_run
//! use crate::api::ws::{WebSocketClient, MessageHandler, WsBaseEvent};
//!
//! struct MyHandler;
//!
//! impl MessageHandler for MyHandler {
//!     fn handle_message(&self, msg: String) {
//!         println!("Received: {}", msg);
//!     }
//! }
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let handler = MyHandler;
//!
//!     // 连接 WebSocket
//!     let ws = WebSocketClient::connect("ws://example.com", handler).await?;
//!
//!     // 添加事件监听器
//!     ws.on_open(|| {
//!         println!("Connected!");
//!     }).await;
//!
//!     ws.on_close(|reason| {
//!         println!("Disconnected: {:?}", reason);
//!     }).await;
//!
//!     ws.on_error(|error| {
//!         println!("Error: {}", error);
//!     }).await;
//!
//!     // 断开连接
//!     ws.disconnect();
//!
//!     Ok(())
//! }
//! ```
//!
//! # 注意事项
//!
//! - 连接前需要有效的 WebSocket URL。
//! - 监听器函数必须是 `Send + Sync + 'static`，以支持异步环境。
//! - `MessageHandler` 实现必须处理文本消息，其他消息类型被忽略。
//! - 断开连接后，客户端会自动清理资源。
//! - 事件监听器支持 "open"、"close"、"error" 和 "all" 事件。
//! - 错误处理使用 `WebSocketError`，连接失败或操作错误。

use futures_util::StreamExt;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::Mutex;
use tokio_tungstenite::{connect_async, tungstenite::protocol::Message};
use tokio_util::sync::CancellationToken;

/// WebSocket 错误类型
#[derive(Debug, thiserror::Error)]
pub enum WebSocketError {
    #[error("连接失败: {0}")]
    ConnectionFailed(String),
    #[error("其他错误: {0}")]
    Other(String),
}

/// 基础 WebSocket 事件
#[derive(Debug, Clone)]
pub enum WsBaseEvent {
    Open,
    Close(Option<String>),
    Error(String),
}

/// WebSocket 事件类型枚举
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum WsEventType {
    Open,
    Close,
    Error,
    All,
}

/// 事件监听器类型
pub type EventListener = Arc<dyn Fn(WsBaseEvent) + Send + Sync + 'static>;

/// WebSocket 消息处理器 trait
pub trait MessageHandler: Send + Sync {
    /// 处理接收到的文本消息
    fn handle_message(&self, msg: String);
}

/// 基础 WebSocket 客户端
pub struct WebSocketClient {
    listeners: Arc<Mutex<HashMap<WsEventType, Vec<EventListener>>>>,
    cancel_token: CancellationToken,
    _handle: tokio::task::JoinHandle<()>,
}

impl WebSocketClient {
    /// 创建并连接 WebSocket
    pub async fn connect<H>(url: &str, message_handler: H) -> Result<Self, WebSocketError>
    where
        H: MessageHandler + 'static,
    {
        let listeners = Arc::new(Mutex::new(HashMap::<WsEventType, Vec<EventListener>>::new()));
        let cancel_token = CancellationToken::new();

        let (ws_stream, _) = connect_async(url)
            .await
            .map_err(|e| WebSocketError::ConnectionFailed(e.to_string()))?;

        let (_write, mut read) = ws_stream.split();

        let listeners_clone = listeners.clone();
        let cancel = cancel_token.clone();

        // 主接收任务
        let handle = tokio::spawn(async move {
            tokio::select! {
                _ = cancel.cancelled() => {}
                _ = async {
                    // 发送 Open 事件
                    Self::emit_event(&listeners_clone, &WsEventType::Open, WsBaseEvent::Open).await;

                    while let Some(msg) = read.next().await {
                        match msg {
                            Ok(Message::Text(text)) => {
                                message_handler.handle_message(text.to_string());
                            }
                            Ok(Message::Close(frame)) => {
                                let reason = frame.map(|f| f.reason.to_string());
                                Self::emit_event(&listeners_clone, &WsEventType::Close, WsBaseEvent::Close(reason)).await;
                                break;
                            }
                            Err(e) => {
                                Self::emit_event(&listeners_clone, &WsEventType::Error, WsBaseEvent::Error(e.to_string())).await;
                                break;
                            }
                            _ => {}
                        }
                    }
                } => {}
            }
        });

        Ok(Self {
            listeners,
            cancel_token,
            _handle: handle,
        })
    }

    /// 添加事件监听器
    pub async fn add_listener<F>(&self, event: WsEventType, listener: F)
    where
        F: Fn(WsBaseEvent) + Send + Sync + 'static,
    {
        let mut listeners = self.listeners.lock().await;
        listeners
            .entry(event)
            .or_insert_with(Vec::new)
            .push(Arc::new(listener));
    }

    /// 监听 open 事件
    pub async fn on_open<F>(&self, listener: F)
    where
        F: Fn() + Send + Sync + 'static,
    {
        self.add_listener(WsEventType::Open, move |_| listener())
            .await;
    }

    /// 监听 close 事件
    pub async fn on_close<F>(&self, listener: F)
    where
        F: Fn(Option<String>) + Send + Sync + 'static,
    {
        self.add_listener(WsEventType::Close, move |event| {
            if let WsBaseEvent::Close(reason) = event {
                listener(reason);
            }
        })
        .await;
    }

    /// 监听 error 事件
    pub async fn on_error<F>(&self, listener: F)
    where
        F: Fn(String) + Send + Sync + 'static,
    {
        self.add_listener(WsEventType::Error, move |event| {
            if let WsBaseEvent::Error(error) = event {
                listener(error);
            }
        })
        .await;
    }

    /// 移除监听器
    pub async fn remove_listener(&self, event: Option<WsEventType>) {
        let mut listeners = self.listeners.lock().await;
        match event {
            Some(e) => {
                listeners.remove(&e);
            }
            None => {
                listeners.clear();
            }
        }
    }

    /// 断开连接
    pub fn disconnect(&self) {
        self.cancel_token.cancel();
    }

    /// 内部方法：发射事件
    async fn emit_event(
        listeners: &Arc<Mutex<HashMap<WsEventType, Vec<EventListener>>>>,
        event: &WsEventType,
        data: WsBaseEvent,
    ) {
        let event_listeners: Vec<EventListener> = {
            let listeners_guard = listeners.lock().await;
            listeners_guard.get(event).cloned().unwrap_or_default()
        };
        for listener in event_listeners {
            let data = data.clone();
            tokio::spawn(async move { listener(data) });
        }
        // 同时发送到 "all" 事件
        if *event != WsEventType::All {
            let all_listeners: Vec<EventListener> = {
                let listeners_guard = listeners.lock().await;
                listeners_guard
                    .get(&WsEventType::All)
                    .cloned()
                    .unwrap_or_default()
            };
            for listener in all_listeners {
                let data = data.clone();
                tokio::spawn(async move { listener(data) });
            }
        }
    }
}

impl Drop for WebSocketClient {
    fn drop(&mut self) {
        self.cancel_token.cancel();
    }
}
