use std::sync::Arc;

use serde_json::Value;

use crate::client::core::FishPi;
use crate::domain::chat::{ChatData, ChatEvent, ChatEventKind, ChatNotice, ChatRevoke};
use crate::ids::MessageId;
use crate::utils::build_http_path;
use crate::utils::error::Error;
use crate::wire::chat::{WireChatData, WireChatNotice};
use crate::ws::{ParsedMessageHandler, RetryPolicy, WsConnection, WsLogHook, build_ws_url};

type ChatHandler = ParsedMessageHandler<ChatEventKind, ChatEvent>;

fn parse_chat_message(json: &Value) -> Result<(ChatEventKind, ChatEvent), Error> {
    let kind = detect_kind(json)?;
    let payload = json.get("data").filter(|v| !v.is_null()).unwrap_or(json);
    match kind {
        ChatEventKind::Notice => {
            let wire: WireChatNotice = deser(payload).or_else(|_| deser(json))?;
            Ok((kind, ChatEvent::Notice(ChatNotice::from(wire))))
        }
        ChatEventKind::Data => {
            let wire: WireChatData = deser(payload).or_else(|_| deser(json))?;
            Ok((kind, ChatEvent::Data(ChatData::from(wire))))
        }
        ChatEventKind::Revoke => {
            let id = payload
                .get("data")
                .and_then(Value::as_str)
                .or_else(|| json.get("data").and_then(Value::as_str))
                .unwrap_or_default()
                .to_string();
            Ok((kind, ChatEvent::Revoke(ChatRevoke { id })))
        }
    }
}

fn deser<T: serde::de::DeserializeOwned>(v: &Value) -> Result<T, Error> {
    serde_json::from_value(v.clone()).map_err(|e| Error::decode(e.to_string()))
}

fn detect_kind(json: &Value) -> Result<ChatEventKind, Error> {
    let candidates = [
        json.get("type"),
        json.get("command"),
        json.get("data").and_then(|v| v.get("type")),
        json.get("data").and_then(|v| v.get("command")),
    ];
    for c in candidates {
        match c.and_then(Value::as_str) {
            Some("notice") => return Ok(ChatEventKind::Notice),
            Some("data") => return Ok(ChatEventKind::Data),
            Some("revoke") => return Ok(ChatEventKind::Revoke),
            _ => {}
        }
    }
    let payload = json.get("data").filter(|v| v.is_object()).unwrap_or(json);
    if payload.get("senderUserName").is_some() && payload.get("receiverUserName").is_some() {
        return Ok(ChatEventKind::Data);
    }
    if payload.get("userId").is_some() && payload.get("preview").is_some() {
        return Ok(ChatEventKind::Notice);
    }
    if payload.get("data").and_then(Value::as_str).is_some() {
        return Ok(ChatEventKind::Revoke);
    }
    Err(Error::decode("Missing type/command field"))
}

/// 私聊长连接：收发消息、注册监听、断开重连。
pub struct ChatConnection {
    connection: WsConnection,
    handler: ChatHandler,
    url: String,
}

impl ChatConnection {
    /// 监听私聊消息。
    pub async fn on_data<F>(&self, listener: F)
    where
        F: Fn(ChatData) + Send + Sync + 'static,
    {
        self.add(ChatEventKind::Data, move |e| {
            if let ChatEvent::Data(d) = e {
                listener(d);
            }
        })
        .await;
    }

    /// 监听系统通知。
    pub async fn on_notice<F>(&self, listener: F)
    where
        F: Fn(ChatNotice) + Send + Sync + 'static,
    {
        self.add(ChatEventKind::Notice, move |e| {
            if let ChatEvent::Notice(n) = e {
                listener(n);
            }
        })
        .await;
    }

    /// 监听撤回事件。
    pub async fn on_revoke<F>(&self, listener: F)
    where
        F: Fn(ChatRevoke) + Send + Sync + 'static,
    {
        self.add(ChatEventKind::Revoke, move |e| {
            if let ChatEvent::Revoke(r) = e {
                listener(r);
            }
        })
        .await;
    }

    async fn add<F>(&self, kind: ChatEventKind, listener: F)
    where
        F: Fn(ChatEvent) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(kind, listener)
            .await;
    }

    /// 移除某类监听。
    pub async fn off(&self, kind: ChatEventKind) {
        self.handler.get_emitter().remove_listener(Some(kind)).await;
    }

    /// 经当前连接发送私聊消息。
    pub fn send(&self, content: &str) -> Result<(), Error> {
        self.connection.send_text(content).map_err(Error::from)
    }

    /// 重连。
    pub async fn reconnect(&mut self) -> Result<(), Error> {
        self.connection
            .reconnect(&self.url, self.handler.clone())
            .await
            .map_err(Error::from)
    }

    /// 设置重连策略。
    pub fn set_retry_policy(&mut self, policy: RetryPolicy) {
        self.connection.set_retry_policy(policy);
    }

    /// 设置日志钩子。
    pub fn set_log_hook<F>(&mut self, hook: F)
    where
        F: Fn(&str) + Send + Sync + 'static,
    {
        let hook = Arc::new(hook) as WsLogHook;
        self.connection.set_log_hook_arc(hook.clone());
        self.handler.set_log_hook_arc(hook);
    }

    /// 断开连接。
    pub fn disconnect(&mut self) {
        self.connection.disconnect();
    }
}

/// 私聊域访问器。
pub struct ChatApi(pub(crate) FishPi);

impl FishPi {
    /// 私聊域。
    pub fn chat(&self) -> ChatApi {
        ChatApi(self.clone())
    }
}

impl ChatApi {
    /// 建立私聊长连接。`user` 为 `Some` 连私聊频道，`None` 连系统通知频道。
    pub async fn connect(&self, user: Option<&str>) -> Result<ChatConnection, Error> {
        let mut params = vec![("apiKey", self.0.api_key().to_string())];
        let path = match user {
            Some(u) => {
                params.push(("toUser", u.to_string()));
                "chat-channel"
            }
            None => "user-channel",
        };
        let url = build_ws_url(self.0.domain(), path, &params).map_err(Error::from)?;
        let handler = ChatHandler::new(parse_chat_message, None, "chat");
        let mut connection = WsConnection::new();
        connection
            .connect(false, &url, handler.clone())
            .await
            .map_err(Error::from)?;
        Ok(ChatConnection {
            connection,
            handler,
            url,
        })
    }

    /// 私聊会话列表（每个会话最新一条）。
    pub async fn list(&self) -> Result<Vec<ChatData>, Error> {
        let rsp = self.0.get_raw(&self.0.with_key("chat/get-list")).await?;
        Ok(collect_chat_data(&rsp))
    }

    /// 与指定用户的历史消息。
    pub async fn history(
        &self,
        user: impl Into<String>,
        page: u32,
        size: u32,
        autoread: bool,
    ) -> Result<Vec<ChatData>, Error> {
        let user = user.into();
        let url = build_http_path(
            "chat/get-message",
            &[
                ("apiKey", self.0.api_key().to_string()),
                ("page", page.to_string()),
                ("pageSize", size.to_string()),
                ("toUser", user.clone()),
            ],
        );
        let rsp = self.0.get_raw(&url).await?;
        if !result_ok(&rsp) {
            return Err(Error::decode(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }
        let list = collect_chat_data(&rsp);
        if autoread {
            self.mark_as_read(user).await?;
        }
        Ok(list)
    }

    /// 标记与某用户的私聊为已读。
    pub async fn mark_as_read(&self, user: impl Into<String>) -> Result<bool, Error> {
        let user = user.into();
        let by_to = build_http_path(
            "chat/mark-as-read",
            &[
                ("toUser", user.clone()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        match self.0.get_raw(&by_to).await {
            Ok(rsp) if result_ok(&rsp) => Ok(true),
            other => {
                let text = match &other {
                    Ok(rsp) => rsp["msg"].as_str().unwrap_or("").to_string(),
                    Err(e) => e.to_string(),
                };
                if !(text.contains("fromUserJSON") || text.contains("Cannot invoke")) {
                    other?;
                    return Ok(false);
                }
                let by_from = build_http_path(
                    "chat/mark-as-read",
                    &[("fromUser", user), ("apiKey", self.0.api_key().to_string())],
                );
                let rsp = self.0.get_raw(&by_from).await?;
                if !result_ok(&rsp) {
                    return Err(Error::decode(
                        rsp["msg"].as_str().unwrap_or("API error").to_string(),
                    ));
                }
                Ok(true)
            }
        }
    }

    /// 未读私聊消息。
    pub async fn unread(&self) -> Result<Vec<ChatData>, Error> {
        let rsp = self.0.get_raw(&self.0.with_key("chat/has-unread")).await?;
        if rsp["result"].as_i64().unwrap_or(0) == 0 {
            return Ok(Vec::new());
        }
        Ok(collect_chat_data(&rsp))
    }

    /// 撤回私聊消息。
    pub async fn revoke(&self, msg_id: impl Into<MessageId>) -> Result<bool, Error> {
        let url = build_http_path(
            "chat/revoke",
            &[
                ("apiKey", self.0.api_key().to_string()),
                ("oId", msg_id.into().into_string()),
            ],
        );
        let rsp = self.0.get_raw(&url).await?;
        if !result_ok(&rsp) {
            return Err(Error::decode(
                rsp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }
        Ok(true)
    }
}

fn result_ok(rsp: &Value) -> bool {
    rsp.get("result").and_then(Value::as_i64).unwrap_or(0) == 0
}

fn collect_chat_data(rsp: &Value) -> Vec<ChatData> {
    rsp["data"]
        .as_array()
        .map(|list| {
            list.iter()
                .filter_map(|v| serde_json::from_value::<WireChatData>(v.clone()).ok())
                .map(ChatData::from)
                .collect()
        })
        .unwrap_or_default()
}
