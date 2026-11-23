use std::sync::Arc;
use std::time::Duration;

use futures_util::{SinkExt, StreamExt};
use serde_json::{Value, json};
use tokio_tungstenite::connect_async;
use tokio_tungstenite::tungstenite::protocol::Message;
use tokio_tungstenite::{MaybeTlsStream, WebSocketStream};
use tokio_util::sync::CancellationToken;

use crate::domain::chatroom::ChatAppearance;
use crate::ws::EventBus;
use crate::wire::chatroom::WireChatAppearance;

use super::v3::{V3Client, V3Error, V3RealtimeTicket};

const PING_PERIOD: Duration = Duration::from_secs(25);
const RECONNECT_MIN_DELAY: Duration = Duration::from_secs(1);
const RECONNECT_MAX_DELAY: Duration = Duration::from_secs(60);
const EXPIRY_FALLBACK: Duration = Duration::from_secs(50 * 60);

type WsStream = WebSocketStream<MaybeTlsStream<tokio::net::TcpStream>>;

/// V3 聊天室实时事件类别。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum V3ChatEventKind {
    Status,
    MessageCreated,
    MessageUpdated,
    MessageDeleted,
    SyncRequired,
    AuthExpired,
}

/// V3 聊天室实时事件：`text` 承载 Status/AuthExpired 的说明文字。
#[derive(Clone, Debug)]
pub struct V3ChatEvent {
    pub kind: V3ChatEventKind,
    pub message_id: String,
    pub revision: Option<String>,
    pub appearance: Option<ChatAppearance>,
    pub text: String,
}

impl V3ChatEvent {
    fn status(text: impl Into<String>) -> Self {
        Self {
            kind: V3ChatEventKind::Status,
            message_id: String::new(),
            revision: None,
            appearance: None,
            text: text.into(),
        }
    }

    fn auth_expired(text: impl Into<String>) -> Self {
        Self {
            kind: V3ChatEventKind::AuthExpired,
            message_id: String::new(),
            revision: None,
            appearance: None,
            text: text.into(),
        }
    }
}

/// 聊天室外观实时订阅连接，断线自动取新票重连。
pub struct V3ChatRealtime {
    events: EventBus<V3ChatEventKind, V3ChatEvent>,
    cancel: CancellationToken,
}

impl V3ChatRealtime {
    /// 注册全类别事件监听；监听器在订阅连接所在运行时上被调用。
    pub async fn on_event<F>(&self, listener: F)
    where
        F: Fn(V3ChatEvent) + Send + Sync + 'static,
    {
        let shared = Arc::new(listener);
        for kind in [
            V3ChatEventKind::Status,
            V3ChatEventKind::MessageCreated,
            V3ChatEventKind::MessageUpdated,
            V3ChatEventKind::MessageDeleted,
            V3ChatEventKind::SyncRequired,
            V3ChatEventKind::AuthExpired,
        ] {
            let handler = Arc::clone(&shared);
            self.events.add_listener(kind, move |event| handler(event)).await;
        }
    }

    pub fn disconnect(&self) {
        self.cancel.cancel();
    }
}

impl Drop for V3ChatRealtime {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

impl V3Client {
    /// 建立聊天室外观实时订阅。令牌失效时发出 AuthExpired 事件并停止，
    /// 其余断线在托管循环内取新票重连。
    pub async fn connect_chat_realtime(&self, access_token: &str) -> Result<V3ChatRealtime, V3Error> {
        let token = access_token.trim().to_string();
        if token.is_empty() {
            return Err(V3Error::unauthorized("缺少 V3 访问令牌"));
        }
        let ticket = self.chat_realtime_ticket(&token).await?;
        let url = realtime_ws_url(self.base_url(), &ticket.path);
        let (stream, _) = connect_async(url)
            .await
            .map_err(|err| V3Error::transport(format!("V3 实时订阅连接失败: {err}")))?;
        let events = EventBus::new();
        let cancel = CancellationToken::new();
        let v3 = self.clone();
        let task_cancel = cancel.clone();
        let task_events = events.clone();
        tokio::spawn(async move {
            realtime_loop(v3, token, Some((ticket, stream)), task_cancel, task_events).await;
        });
        Ok(V3ChatRealtime { events, cancel })
    }
}

async fn realtime_loop(
    v3: V3Client,
    access_token: String,
    mut pending: Option<(V3RealtimeTicket, WsStream)>,
    cancel: CancellationToken,
    events: EventBus<V3ChatEventKind, V3ChatEvent>,
) {
    let mut backoff = RECONNECT_MIN_DELAY;
    while !cancel.is_cancelled() {
        let (ticket, stream) = match pending.take() {
            Some(pair) => pair,
            None => {
                let ticket = match v3.chat_realtime_ticket(&access_token).await {
                    Ok(ticket) => ticket,
                    Err(err) => {
                        events
                            .emit(
                                &V3ChatEventKind::Status,
                                V3ChatEvent::status(format!("V3 实时订阅取票失败: {err}")),
                                None,
                            )
                            .await;
                        if err.requires_reauth() {
                            events
                                .emit(&V3ChatEventKind::AuthExpired, V3ChatEvent::auth_expired(err.to_string()), None)
                                .await;
                            return;
                        }
                        if !sleep_cancelable(&cancel, backoff).await {
                            return;
                        }
                        backoff = (backoff * 2).min(RECONNECT_MAX_DELAY);
                        continue;
                    }
                };
                let url = realtime_ws_url(v3.base_url(), &ticket.path);
                match connect_async(url).await {
                    Ok((stream, _)) => {
                        backoff = RECONNECT_MIN_DELAY;
                        (ticket, stream)
                    }
                    Err(err) => {
                        events
                            .emit(
                                &V3ChatEventKind::Status,
                                V3ChatEvent::status(format!("V3 实时订阅连接失败: {err}")),
                                None,
                            )
                            .await;
                        if !sleep_cancelable(&cancel, backoff).await {
                            return;
                        }
                        backoff = (backoff * 2).min(RECONNECT_MAX_DELAY);
                        continue;
                    }
                }
            }
        };

        let (mut write, mut read) = stream.split();
        let mut sent_auth = false;
        let mut authenticated = false;
        let mut ping = tokio::time::interval_at(tokio::time::Instant::now() + PING_PERIOD, PING_PERIOD);
        let expiry = tokio::time::sleep_until(tokio::time::Instant::now() + EXPIRY_FALLBACK);
        tokio::pin!(expiry);

        loop {
            tokio::select! {
                _ = cancel.cancelled() => {
                    let _ = write.send(Message::Close(None)).await;
                    return;
                }
                _ = &mut expiry => {
                    events
                        .emit(
                            &V3ChatEventKind::Status,
                            V3ChatEvent::status("V3 实时订阅授权到期，重新取票连接"),
                            None,
                        )
                        .await;
                    break;
                }
                _ = ping.tick() => {
                    if authenticated {
                        let frame = json!({"type": "ping"}).to_string();
                        if write.send(Message::Text(frame.into())).await.is_err() {
                            break;
                        }
                    }
                }
                frame = read.next() => {
                    let Some(Ok(message)) = frame else { break };
                    let Message::Text(text) = message else { continue };
                    let Ok(value) = serde_json::from_str::<Value>(text.as_str()) else {
                        continue;
                    };
                    match value["type"].as_str().unwrap_or("") {
                        "authentication.required" => {
                            if !sent_auth {
                                sent_auth = true;
                                let frame = json!({"type": "authenticate", "ticket": ticket.ticket}).to_string();
                                if write.send(Message::Text(frame.into())).await.is_err() {
                                    break;
                                }
                            }
                        }
                        "ready" => {
                            authenticated = true;
                            backoff = RECONNECT_MIN_DELAY;
                            if let Some(expires_at) = value["data"]["expiresAt"].as_u64() {
                                let deadline = std::time::Instant::now() + Duration::from_millis(expires_at);
                                expiry.as_mut().reset(tokio::time::Instant::from_std(deadline));
                            }
                            events
                                .emit(
                                    &V3ChatEventKind::Status,
                                    V3ChatEvent::status("V3 聊天室订阅已就绪"),
                                    None,
                                )
                                .await;
                        }
                        other => {
                            if let Some(kind) = message_kind(other) {
                                if let Some(event) = message_event(kind, &value) {
                                    events.emit(&kind, event, None).await;
                                }
                            }
                        }
                    }
                }
            }
        }

        events
            .emit(
                &V3ChatEventKind::Status,
                V3ChatEvent::status("V3 实时订阅连接中断，准备重连"),
                None,
            )
            .await;
        if !sleep_cancelable(&cancel, backoff).await {
            return;
        }
        backoff = (backoff * 2).min(RECONNECT_MAX_DELAY);
    }
}

async fn sleep_cancelable(cancel: &CancellationToken, delay: Duration) -> bool {
    tokio::select! {
        _ = cancel.cancelled() => false,
        _ = tokio::time::sleep(delay) => true,
    }
}

/// 由站点根地址与订阅路径拼接 WS 地址（无查询参数）。
fn realtime_ws_url(base: &str, path: &str) -> String {
    let host = base
        .trim_start_matches("https://")
        .trim_start_matches("http://");
    let path = if path.starts_with('/') {
        path.to_string()
    } else {
        format!("/{path}")
    };
    format!("wss://{host}{path}")
}

/// 把实时帧类型映射为事件类别；握手、心跳与其余广播类型返回 None。
fn message_kind(type_str: &str) -> Option<V3ChatEventKind> {
    match type_str {
        "chat.message.created" => Some(V3ChatEventKind::MessageCreated),
        "chat.message.updated" => Some(V3ChatEventKind::MessageUpdated),
        "chat.message.deleted" => Some(V3ChatEventKind::MessageDeleted),
        "chat.sync_required" => Some(V3ChatEventKind::SyncRequired),
        _ => None,
    }
}

/// 解析消息事件帧；缺少 messageId 的事件按协议丢弃，appearance 解析失败按无快照处理。
fn message_event(kind: V3ChatEventKind, value: &Value) -> Option<V3ChatEvent> {
    let data = &value["data"];
    let message_id = data["messageId"].as_str()?.trim().to_string();
    if message_id.is_empty() {
        return None;
    }
    let revision = data["revision"].as_str().map(str::to_string);
    let appearance = data["message"]["appearance"]
        .as_object()
        .and_then(|object| {
            serde_json::from_value::<WireChatAppearance>(Value::Object(object.clone())).ok()
        })
        .map(ChatAppearance::from);
    Some(V3ChatEvent {
        kind,
        message_id,
        revision,
        appearance,
        text: String::new(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn realtime_ws_url_has_no_query() {
        assert_eq!(
            realtime_ws_url("https://fishpi.cn", "/api/v3/realtime"),
            "wss://fishpi.cn/api/v3/realtime"
        );
        assert_eq!(
            realtime_ws_url("https://fishpi.cn", "api/v3/realtime"),
            "wss://fishpi.cn/api/v3/realtime"
        );
    }

    #[test]
    fn message_created_frame_with_appearance_is_parsed() {
        let frame = json!({
            "type": "chat.message.created",
            "data": {
                "messageId": "1795142651997",
                "revision": "116328",
                "message": {
                    "appearance": {
                        "bubbleId": "paipai-orange",
                        "frameId": "paipai-orange",
                        "version": 1
                    }
                }
            }
        });
        let event = message_event(V3ChatEventKind::MessageCreated, &frame).unwrap();
        assert_eq!(event.message_id, "1795142651997");
        assert_eq!(event.revision.as_deref(), Some("116328"));
        let appearance = event.appearance.unwrap();
        assert_eq!(appearance.bubble_id, "paipai-orange");
        assert_eq!(appearance.frame_id, "paipai-orange");
    }

    #[test]
    fn frame_without_message_payload_yields_no_appearance() {
        let frame = json!({
            "type": "chat.message.created",
            "data": {"messageId": "1", "revision": "2"}
        });
        let event = message_event(V3ChatEventKind::MessageCreated, &frame).unwrap();
        assert!(event.appearance.is_none());
    }

    #[test]
    fn malformed_appearance_is_treated_as_missing() {
        let frame = json!({
            "type": "chat.message.updated",
            "data": {
                "messageId": "3",
                "message": {"appearance": {"frameId": "none", "version": 1}}
            }
        });
        let event = message_event(V3ChatEventKind::MessageUpdated, &frame).unwrap();
        assert!(event.appearance.is_none());
    }

    #[test]
    fn unknown_and_handshake_types_map_to_no_message_event() {
        assert!(message_kind("pong").is_none());
        assert!(message_kind("ready").is_none());
        assert!(message_kind("authentication.required").is_none());
        assert!(message_kind("chat.topic_changed").is_none());
        assert!(message_kind("chat.presence.changed").is_none());
        assert_eq!(message_kind("chat.message.deleted"), Some(V3ChatEventKind::MessageDeleted));
        assert_eq!(message_kind("chat.sync_required"), Some(V3ChatEventKind::SyncRequired));
    }

    #[test]
    fn missing_message_id_frame_is_dropped() {
        let frame = json!({"type": "chat.message.updated", "data": {"revision": "9"}});
        assert!(message_event(V3ChatEventKind::MessageUpdated, &frame).is_none());
    }
}
