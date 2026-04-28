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
//! use fishpi_sdk::api::ws::{MessageHandler, WebSocketClient};
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

use futures_util::{SinkExt, StreamExt};
use serde_json::Value;
use std::collections::HashMap;
use std::hash::Hash;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Mutex;
use tokio::time::sleep;
use tokio_tungstenite::{connect_async, tungstenite::protocol::Message};
use tokio_util::sync::CancellationToken;
use url::Url;

use crate::utils::error::Error;

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
pub type TypedListener<D> = Arc<dyn Fn(D) + Send + Sync + 'static>;
pub type WsLogHook = Arc<dyn Fn(&str) + Send + Sync + 'static>;

/// 自动重连策略
#[derive(Clone, Debug)]
pub struct RetryPolicy {
    /// 最大尝试次数（包含首次）
    pub max_attempts: u32,
    /// 首次重试延迟
    pub initial_delay: Duration,
    /// 最大重试延迟
    pub max_delay: Duration,
    /// 退避倍率（每次失败后 delay *= backoff_factor）
    pub backoff_factor: f64,
}

impl Default for RetryPolicy {
    fn default() -> Self {
        Self {
            max_attempts: 3,
            initial_delay: Duration::from_millis(400),
            max_delay: Duration::from_secs(8),
            backoff_factor: 2.0,
        }
    }
}

/// 通用事件总线
#[derive(Clone)]
pub struct EventBus<E, D>
where
    E: Eq + Hash + Clone,
{
    listeners: Arc<Mutex<HashMap<E, Vec<TypedListener<D>>>>>,
}

impl<E, D> Default for EventBus<E, D>
where
    E: Eq + Hash + Clone,
{
    fn default() -> Self {
        Self {
            listeners: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

impl<E, D> EventBus<E, D>
where
    E: Eq + Hash + Clone + Send + Sync + 'static,
    D: Clone + Send + 'static,
{
    pub fn new() -> Self {
        Self::default()
    }

    pub async fn add_listener<F>(&self, event: E, listener: F)
    where
        F: Fn(D) + Send + Sync + 'static,
    {
        let mut listeners = self.listeners.lock().await;
        listeners
            .entry(event)
            .or_insert_with(Vec::new)
            .push(Arc::new(listener));
    }

    pub async fn remove_listener(&self, event: Option<E>) {
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

    pub async fn emit(&self, event: &E, data: D, all_event: Option<&E>) {
        let event_listeners: Vec<TypedListener<D>> = {
            let listeners_guard = self.listeners.lock().await;
            listeners_guard.get(event).cloned().unwrap_or_default()
        };

        for listener in event_listeners {
            let data = data.clone();
            tokio::spawn(async move { listener(data) });
        }

        if let Some(all) = all_event {
            if all == event {
                return;
            }

            let all_listeners: Vec<TypedListener<D>> = {
                let listeners_guard = self.listeners.lock().await;
                listeners_guard.get(all).cloned().unwrap_or_default()
            };

            for listener in all_listeners {
                let data = data.clone();
                tokio::spawn(async move { listener(data) });
            }
        }
    }
}

/// WebSocket 消息处理器 trait
pub trait MessageHandler: Send + Sync {
    /// 处理接收到的文本消息
    fn handle_message(&self, msg: String);
}

/// 基础 WebSocket 客户端
pub struct WebSocketClient {
    listeners: EventBus<WsEventType, WsBaseEvent>,
    cancel_token: CancellationToken,
    outbound_tx: tokio::sync::mpsc::UnboundedSender<Message>,
    _handle: tokio::task::JoinHandle<()>,
}

/// 构造带查询参数的 WebSocket URL，自动进行 query 编码
pub fn build_ws_url(
    domain: &str,
    path: &str,
    params: &[(&str, String)],
) -> Result<String, WebSocketError> {
    let mut url = Url::parse(&format!(
        "wss://{}/{}",
        domain,
        path.trim_start_matches('/')
    ))
    .map_err(|e| WebSocketError::Other(format!("invalid ws url: {}", e)))?;

    {
        let mut query = url.query_pairs_mut();
        for (k, v) in params {
            query.append_pair(k, v);
        }
    }

    Ok(url.to_string())
}

impl WebSocketClient {
    /// 创建并连接 WebSocket
    pub async fn connect<H>(url: &str, message_handler: H) -> Result<Self, WebSocketError>
    where
        H: MessageHandler + 'static,
    {
        let listeners = EventBus::<WsEventType, WsBaseEvent>::new();
        let cancel_token = CancellationToken::new();

        let (ws_stream, _) = connect_async(url)
            .await
            .map_err(|e| WebSocketError::ConnectionFailed(e.to_string()))?;

        let (mut write, mut read) = ws_stream.split();
        let (outbound_tx, mut outbound_rx) = tokio::sync::mpsc::unbounded_channel::<Message>();

        let listeners_clone = listeners.clone();
        let cancel = cancel_token.clone();

        // 主接收任务
        let handle = tokio::spawn(async move {
            tokio::select! {
                _ = cancel.cancelled() => {}
                _ = async {
                    // 发送 Open 事件
                    listeners_clone
                        .emit(&WsEventType::Open, WsBaseEvent::Open, Some(&WsEventType::All))
                        .await;

                    loop {
                        tokio::select! {
                            _ = cancel.cancelled() => {
                                break;
                            }
                            outbound = outbound_rx.recv() => {
                                match outbound {
                                    Some(msg) => {
                                        if let Err(e) = write.send(msg).await {
                                            listeners_clone
                                                .emit(
                                                    &WsEventType::Error,
                                                    WsBaseEvent::Error(e.to_string()),
                                                    Some(&WsEventType::All),
                                                )
                                                .await;
                                            break;
                                        }
                                    }
                                    None => break,
                                }
                            }
                            incoming = read.next() => {
                                match incoming {
                                    Some(Ok(Message::Text(text))) => {
                                        message_handler.handle_message(text.to_string());
                                    }
                                    Some(Ok(Message::Close(frame))) => {
                                        let reason = frame.map(|f| f.reason.to_string());
                                        listeners_clone
                                            .emit(
                                                &WsEventType::Close,
                                                WsBaseEvent::Close(reason),
                                                Some(&WsEventType::All),
                                            )
                                            .await;
                                        break;
                                    }
                                    Some(Err(e)) => {
                                        listeners_clone
                                            .emit(
                                                &WsEventType::Error,
                                                WsBaseEvent::Error(e.to_string()),
                                                Some(&WsEventType::All),
                                            )
                                            .await;
                                        break;
                                    }
                                    _ => {}
                                }
                            }
                        }
                    }
                } => {}
            }
        });

        Ok(Self {
            listeners,
            cancel_token,
            outbound_tx,
            _handle: handle,
        })
    }

    /// 创建带自动重连的 WebSocket 连接。
    pub async fn connect_managed<H>(
        url: String,
        message_handler: H,
        retry_policy: RetryPolicy,
        log_hook: Option<WsLogHook>,
    ) -> Result<Self, WebSocketError>
    where
        H: MessageHandler + Clone + 'static,
    {
        let listeners = EventBus::<WsEventType, WsBaseEvent>::new();
        let cancel_token = CancellationToken::new();
        let (outbound_tx, mut outbound_rx) = tokio::sync::mpsc::unbounded_channel::<Message>();

        let (initial_stream, _) = connect_async(&url)
            .await
            .map_err(|e| WebSocketError::ConnectionFailed(e.to_string()))?;

        let listeners_for_initial = listeners.clone();
        let cancel = cancel_token.clone();
        let handle = tokio::spawn(async move {
            let mut attempt: u32 = 0;
            let mut delay = retry_policy.initial_delay;
            let mut pending_stream = Some(initial_stream);

            loop {
                if cancel.is_cancelled() {
                    break;
                }

                let connect_result = if let Some(ws_stream) = pending_stream.take() {
                    Ok(ws_stream)
                } else {
                    connect_async(&url).await.map(|(stream, _)| stream)
                };

                match connect_result {
                    Ok(ws_stream) => {
                        attempt = 0;
                        delay = retry_policy.initial_delay;
                        listeners_for_initial
                            .emit(
                                &WsEventType::Open,
                                WsBaseEvent::Open,
                                Some(&WsEventType::All),
                            )
                            .await;

                        let (mut write, mut read) = ws_stream.split();
                        let mut disconnected_reason: Option<String> = None;

                        loop {
                            tokio::select! {
                                _ = cancel.cancelled() => {
                                    return;
                                }
                                outbound = outbound_rx.recv() => {
                                    match outbound {
                                        Some(msg) => {
                                            if let Err(e) = write.send(msg).await {
                                                disconnected_reason = Some(e.to_string());
                                                listeners_for_initial
                                                    .emit(
                                                        &WsEventType::Error,
                                                        WsBaseEvent::Error(e.to_string()),
                                                        Some(&WsEventType::All),
                                                    )
                                                    .await;
                                                break;
                                            }
                                        }
                                        None => return,
                                    }
                                }
                                incoming = read.next() => {
                                    match incoming {
                                        Some(Ok(Message::Text(text))) => {
                                            message_handler.handle_message(text.to_string());
                                        }
                                        Some(Ok(Message::Close(frame))) => {
                                            let reason = frame.map(|f| f.reason.to_string());
                                            disconnected_reason = reason.clone();
                                            listeners_for_initial
                                                .emit(
                                                    &WsEventType::Close,
                                                    WsBaseEvent::Close(reason),
                                                    Some(&WsEventType::All),
                                                )
                                                .await;
                                            break;
                                        }
                                        Some(Ok(_)) => {}
                                        Some(Err(e)) => {
                                            disconnected_reason = Some(e.to_string());
                                            listeners_for_initial
                                                .emit(
                                                    &WsEventType::Error,
                                                    WsBaseEvent::Error(e.to_string()),
                                                    Some(&WsEventType::All),
                                                )
                                                .await;
                                            break;
                                        }
                                        None => {
                                            listeners_for_initial
                                                .emit(
                                                    &WsEventType::Close,
                                                    WsBaseEvent::Close(Some("stream ended".to_string())),
                                                    Some(&WsEventType::All),
                                                )
                                                .await;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if let Some(hook) = &log_hook {
                            hook(&format!(
                                "WebSocket disconnected: {}",
                                disconnected_reason.unwrap_or_else(|| "stream ended".to_string())
                            ));
                        }
                    }
                    Err(err) => {
                        attempt = attempt.saturating_add(1);
                        let message = err.to_string();
                        listeners_for_initial
                            .emit(
                                &WsEventType::Error,
                                WsBaseEvent::Error(message.clone()),
                                Some(&WsEventType::All),
                            )
                            .await;
                        if let Some(hook) = &log_hook {
                            hook(&format!("WebSocket reconnect failed: {}", message));
                        }
                    }
                }

                if retry_policy.max_attempts > 0 && attempt >= retry_policy.max_attempts {
                    attempt = 0;
                }

                if let Some(hook) = &log_hook {
                    hook(&format!("WebSocket reconnecting in {:?}", delay));
                }

                tokio::select! {
                    _ = cancel.cancelled() => break,
                    _ = sleep(delay) => {}
                }

                let next = (delay.as_secs_f64() * retry_policy.backoff_factor)
                    .max(retry_policy.initial_delay.as_secs_f64());
                delay = Duration::from_secs_f64(next.min(retry_policy.max_delay.as_secs_f64()));
            }
        });

        Ok(Self {
            listeners,
            cancel_token,
            outbound_tx,
            _handle: handle,
        })
    }

    /// 添加事件监听器
    pub async fn add_listener<F>(&self, event: WsEventType, listener: F)
    where
        F: Fn(WsBaseEvent) + Send + Sync + 'static,
    {
        self.listeners.add_listener(event, listener).await;
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
        self.listeners.remove_listener(event).await;
    }

    /// 断开连接
    pub fn disconnect(&self) {
        self.cancel_token.cancel();
    }

    /// 发送文本消息
    pub fn send_text(&self, text: &str) -> Result<(), WebSocketError> {
        self.outbound_tx
            .send(Message::Text(text.to_string().into()))
            .map_err(|e| WebSocketError::Other(format!("send message failed: {}", e)))
    }
}

impl Drop for WebSocketClient {
    fn drop(&mut self) {
        self.cancel_token.cancel();
    }
}

/// 通用 WebSocket 连接生命周期管理器
#[derive(Default)]
pub struct WsConnection {
    client: Option<WebSocketClient>,
    retry_policy: RetryPolicy,
    log_hook: Option<WsLogHook>,
}

impl WsConnection {
    pub fn new() -> Self {
        Self {
            client: None,
            retry_policy: RetryPolicy::default(),
            log_hook: None,
        }
    }

    pub fn is_connected(&self) -> bool {
        self.client.is_some()
    }

    pub fn set_retry_policy(&mut self, policy: RetryPolicy) {
        self.retry_policy = policy;
    }

    pub fn set_log_hook<F>(&mut self, hook: F)
    where
        F: Fn(&str) + Send + Sync + 'static,
    {
        self.log_hook = Some(Arc::new(hook));
    }

    pub fn set_log_hook_arc(&mut self, hook: WsLogHook) {
        self.log_hook = Some(hook);
    }

    fn log(&self, message: &str) {
        if let Some(hook) = &self.log_hook {
            hook(message);
        }
    }

    pub async fn connect<H>(
        &mut self,
        reload: bool,
        url: &str,
        message_handler: H,
    ) -> Result<(), WebSocketError>
    where
        H: MessageHandler + Clone + 'static,
    {
        if self.client.is_some() {
            if !reload {
                return Ok(());
            }
            self.disconnect();
        }

        let ws = WebSocketClient::connect_managed(
            url.to_string(),
            message_handler,
            self.retry_policy.clone(),
            self.log_hook.clone(),
        )
        .await?;
        self.client = Some(ws);
        Ok(())
    }

    pub async fn reconnect<H>(
        &mut self,
        url: &str,
        message_handler: H,
    ) -> Result<(), WebSocketError>
    where
        H: MessageHandler + Clone + 'static,
    {
        self.disconnect();

        let attempts = self.retry_policy.max_attempts.max(1);
        let mut delay = self.retry_policy.initial_delay;
        let mut last_err: Option<WebSocketError> = None;

        for attempt in 1..=attempts {
            match WebSocketClient::connect_managed(
                url.to_string(),
                message_handler.clone(),
                self.retry_policy.clone(),
                self.log_hook.clone(),
            )
            .await
            {
                Ok(ws) => {
                    self.client = Some(ws);
                    self.log(&format!(
                        "WebSocket reconnected on attempt {}/{}",
                        attempt, attempts
                    ));
                    return Ok(());
                }
                Err(err) => {
                    last_err = Some(err);
                    if attempt >= attempts {
                        break;
                    }

                    self.log(&format!(
                        "WebSocket reconnect attempt {}/{} failed, retrying in {:?}",
                        attempt, attempts, delay
                    ));
                    sleep(delay).await;

                    let next = (delay.as_secs_f64() * self.retry_policy.backoff_factor)
                        .max(self.retry_policy.initial_delay.as_secs_f64());
                    delay = Duration::from_secs_f64(
                        next.min(self.retry_policy.max_delay.as_secs_f64()),
                    );
                }
            }
        }

        Err(last_err.unwrap_or_else(|| WebSocketError::Other("reconnect failed".to_string())))
    }

    pub fn disconnect(&mut self) {
        if let Some(ws) = self.client.take() {
            ws.disconnect();
        }
    }

    pub fn send_text(&self, text: &str) -> Result<(), WebSocketError> {
        match &self.client {
            Some(ws) => ws.send_text(text),
            None => Err(WebSocketError::Other(
                "websocket is not connected".to_string(),
            )),
        }
    }
}

/// 通用“解析后分发”消息处理器
#[derive(Clone)]
pub struct ParsedMessageHandler<E, D>
where
    E: Eq + Hash + Clone + Send + Sync + 'static,
    D: Clone + Send + 'static,
{
    emitter: EventBus<E, D>,
    log_hook: Option<WsLogHook>,
    parser: fn(&Value) -> Result<(E, D), Error>,
    all_event: Option<E>,
    error_context: &'static str,
}

impl<E, D> ParsedMessageHandler<E, D>
where
    E: Eq + Hash + Clone + Send + Sync + 'static,
    D: Clone + Send + 'static,
{
    pub fn new(
        parser: fn(&Value) -> Result<(E, D), Error>,
        all_event: Option<E>,
        error_context: &'static str,
    ) -> Self {
        Self {
            emitter: EventBus::new(),
            log_hook: None,
            parser,
            all_event,
            error_context,
        }
    }

    pub fn get_emitter(&self) -> EventBus<E, D> {
        self.emitter.clone()
    }

    pub fn set_log_hook_arc(&mut self, hook: WsLogHook) {
        self.log_hook = Some(hook);
    }
}

impl<E, D> MessageHandler for ParsedMessageHandler<E, D>
where
    E: Eq + Hash + Clone + Send + Sync + 'static,
    D: Clone + Send + 'static,
{
    fn handle_message(&self, text: String) {
        if let Ok(json) = serde_json::from_str::<Value>(&text) {
            let emitter = self.get_emitter();
            let log_hook = self.log_hook.clone();
            let parser = self.parser;
            let all_event = self.all_event.clone();
            let context = self.error_context;

            tokio::spawn(async move {
                match parser(&json) {
                    Ok((event_type, event)) => {
                        emitter.emit(&event_type, event, all_event.as_ref()).await;
                    }
                    Err(e) => {
                        if let Some(hook) = log_hook {
                            hook(&format!("Failed to parse {} message: {}", context, e));
                        }
                    }
                }
            });
        }
    }
}

#[cfg(test)]
mod tests {
    use super::{EventBus, RetryPolicy, WsEventType, build_ws_url};
    use tokio::sync::mpsc;
    use tokio::time::{Duration, timeout};

    #[test]
    fn retry_policy_defaults_are_reasonable() {
        let p = RetryPolicy::default();
        assert_eq!(p.max_attempts, 3);
        assert_eq!(p.initial_delay, Duration::from_millis(400));
        assert_eq!(p.max_delay, Duration::from_secs(8));
        assert!((p.backoff_factor - 2.0).abs() < f64::EPSILON);
    }

    #[tokio::test]
    async fn event_bus_emits_target_and_all() {
        let bus = EventBus::<WsEventType, String>::new();
        let (tx, mut rx) = mpsc::unbounded_channel::<String>();

        let tx1 = tx.clone();
        bus.add_listener(WsEventType::Open, move |msg| {
            let _ = tx1.send(format!("open:{msg}"));
        })
        .await;

        let tx2 = tx.clone();
        bus.add_listener(WsEventType::All, move |msg| {
            let _ = tx2.send(format!("all:{msg}"));
        })
        .await;

        bus.emit(
            &WsEventType::Open,
            "hello".to_string(),
            Some(&WsEventType::All),
        )
        .await;

        let first = timeout(Duration::from_secs(1), rx.recv())
            .await
            .expect("first recv timeout")
            .expect("first message missing");
        let second = timeout(Duration::from_secs(1), rx.recv())
            .await
            .expect("second recv timeout")
            .expect("second message missing");

        let got = [first, second];
        assert!(got.iter().any(|s| s == "open:hello"));
        assert!(got.iter().any(|s| s == "all:hello"));
    }

    #[test]
    fn build_ws_url_encodes_query_params() {
        let url = build_ws_url(
            "fishpi.cn",
            "chat-channel",
            &[
                ("apiKey", "token a+b".to_string()),
                ("toUser", "alice/bob".to_string()),
            ],
        )
        .expect("url build should succeed");

        assert!(url.starts_with("wss://fishpi.cn/chat-channel?"));
        assert!(url.contains("apiKey=token+a%2Bb"));
        assert!(url.contains("toUser=alice%2Fbob"));
    }
}
