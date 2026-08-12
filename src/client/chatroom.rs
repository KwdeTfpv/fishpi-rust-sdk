use std::sync::Arc;

use serde_json::{Value, json};
use tokio::sync::Mutex;
use url::Url;

use crate::client::core::FishPi;
use crate::domain::chatroom::{
    BarragerCost, BarragerMsg, ChatContentType, ChatReactionMsg, ChatRoomEvent, ChatRoomEventKind,
    ChatRoomMessageMode, ChatRoomMsg, CustomMsg, MusicMsg, MuteItem, OnlineInfo,
};
use crate::domain::redpacket::RedPacketStatusMsg;
use crate::ids::MessageId;
use crate::utils::build_http_path;
use crate::utils::error::Error;
use crate::wire::chatroom::{
    WireBarragerMsg, WireChatReactionMsg, WireChatRoomMsg, WireChatRoomNode, WireMuteItem,
    WireOnlineInfo, payload_type,
};
use crate::wire::redpacket::WireRedPacketStatusMsg;
use crate::ws::{ParsedMessageHandler, RetryPolicy, WsConnection, WsLogHook, build_ws_url};

type ChatRoomHandler = ParsedMessageHandler<ChatRoomEventKind, ChatRoomEvent>;

fn deser<T: serde::de::DeserializeOwned>(v: &Value) -> Result<T, Error> {
    serde_json::from_value(v.clone()).map_err(|e| Error::decode(e.to_string()))
}

fn parse_chatroom_message(json: &Value) -> Result<(ChatRoomEventKind, ChatRoomEvent), Error> {
    let type_str = json["type"]
        .as_str()
        .ok_or_else(|| Error::decode("Missing type in message"))?;

    match type_str {
        "online" => {
            let users = json["users"]
                .as_array()
                .map(|arr| {
                    arr.iter()
                        .filter_map(|u| deser::<WireOnlineInfo>(u).ok())
                        .map(OnlineInfo::from)
                        .collect()
                })
                .unwrap_or_default();
            let discussing = json["discussing"]
                .as_str()
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty());
            let online_chat_cnt = json["onlineChatCnt"]
                .as_u64()
                .or_else(|| json["onlineCnt"].as_u64())
                .map(|n| n as usize);
            Ok((
                ChatRoomEventKind::Online,
                ChatRoomEvent::Online {
                    users,
                    discussing,
                    online_chat_cnt,
                },
            ))
        }
        "discussChanged" => {
            let d = json["newDiscuss"].as_str().unwrap_or_default().to_string();
            Ok((
                ChatRoomEventKind::DiscussChanged,
                ChatRoomEvent::DiscussChanged(d),
            ))
        }
        "revoke" => {
            let id = json["oId"].as_str().unwrap_or_default().to_string();
            Ok((ChatRoomEventKind::Revoke, ChatRoomEvent::Revoke(id)))
        }
        "msg" => {
            let wire: WireChatRoomMsg = deser(json)?;
            let kind = payload_type(&wire.content);
            let msg = ChatRoomMsg::from(wire);
            match kind.as_deref() {
                Some(t) if t.eq_ignore_ascii_case("music") => Ok((
                    ChatRoomEventKind::Music,
                    ChatRoomEvent::Music(MusicMsg::from_base(msg)),
                )),
                Some(t) if t.eq_ignore_ascii_case("weather") => {
                    Ok((ChatRoomEventKind::Weather, ChatRoomEvent::Weather(msg)))
                }
                _ => Ok((ChatRoomEventKind::Msg, ChatRoomEvent::Msg(msg))),
            }
        }
        "redPacket" => {
            let wire: WireChatRoomMsg = deser(json)?;
            Ok((
                ChatRoomEventKind::RedPacket,
                ChatRoomEvent::RedPacket(ChatRoomMsg::from(wire)),
            ))
        }
        "redPacketStatus" => {
            let wire: WireRedPacketStatusMsg = deser(json)?;
            Ok((
                ChatRoomEventKind::RedPacketStatus,
                ChatRoomEvent::RedPacketStatus(RedPacketStatusMsg::from(wire)),
            ))
        }
        "barrager" => {
            let wire: WireBarragerMsg = deser(json)?;
            Ok((
                ChatRoomEventKind::Barrager,
                ChatRoomEvent::Barrager(BarragerMsg::from(wire)),
            ))
        }
        "customMessage" => {
            let message = json["message"].as_str().unwrap_or_default().to_string();
            Ok((
                ChatRoomEventKind::Custom,
                ChatRoomEvent::Custom(CustomMsg { message }),
            ))
        }
        "chatreaction" => {
            let wire: WireChatReactionMsg = deser(json)?;
            Ok((
                ChatRoomEventKind::ChatReaction,
                ChatRoomEvent::ChatReaction(ChatReactionMsg::from(wire)),
            ))
        }
        other => Err(Error::decode(format!("Unknown message type: {}", other))),
    }
}

/// 聊天室长连接：接收消息、注册监听、缓存在线/话题、断开重连。
pub struct ChatRoomConnection {
    connection: WsConnection,
    handler: ChatRoomHandler,
    url: String,
    node_name: Option<String>,
    discuss: Arc<Mutex<String>>,
    onlines: Arc<Mutex<Vec<OnlineInfo>>>,
}

impl ChatRoomConnection {
    /// 监听在线用户更新（附带话题与在线数），并刷新缓存。
    pub async fn on_online<F>(&self, listener: F)
    where
        F: Fn(Vec<OnlineInfo>, Option<String>, Option<usize>) + Send + Sync + 'static,
    {
        let onlines = Arc::clone(&self.onlines);
        let discuss = Arc::clone(&self.discuss);
        self.add(ChatRoomEventKind::Online, move |e| {
            if let ChatRoomEvent::Online {
                users,
                discussing,
                online_chat_cnt,
            } = e
            {
                if !users.is_empty()
                    && let Ok(mut g) = onlines.try_lock()
                {
                    *g = users.clone();
                }
                if let Some(topic) = discussing.as_ref()
                    && let Ok(mut g) = discuss.try_lock()
                {
                    *g = topic.clone();
                }
                listener(users, discussing, online_chat_cnt);
            }
        })
        .await;
    }

    /// 监听话题变更，并刷新缓存。
    pub async fn on_discuss<F>(&self, listener: F)
    where
        F: Fn(String) + Send + Sync + 'static,
    {
        let discuss = Arc::clone(&self.discuss);
        self.add(ChatRoomEventKind::DiscussChanged, move |e| {
            if let ChatRoomEvent::DiscussChanged(topic) = e {
                if let Ok(mut g) = discuss.try_lock() {
                    *g = topic.clone();
                }
                listener(topic);
            }
        })
        .await;
    }

    /// 监听消息撤回。
    pub async fn on_revoke<F>(&self, listener: F)
    where
        F: Fn(String) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::Revoke, move |e| {
            if let ChatRoomEvent::Revoke(id) = e {
                listener(id);
            }
        })
        .await;
    }

    /// 监听普通消息。
    pub async fn on_msg<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::Msg, move |e| {
            if let ChatRoomEvent::Msg(m) = e {
                listener(m);
            }
        })
        .await;
    }

    /// 监听弹幕。
    pub async fn on_barrager<F>(&self, listener: F)
    where
        F: Fn(BarragerMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::Barrager, move |e| {
            if let ChatRoomEvent::Barrager(b) = e {
                listener(b);
            }
        })
        .await;
    }

    /// 监听红包消息。
    pub async fn on_redpacket<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::RedPacket, move |e| {
            if let ChatRoomEvent::RedPacket(m) = e {
                listener(m);
            }
        })
        .await;
    }

    /// 监听红包状态。
    pub async fn on_redpacket_status<F>(&self, listener: F)
    where
        F: Fn(RedPacketStatusMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::RedPacketStatus, move |e| {
            if let ChatRoomEvent::RedPacketStatus(s) = e {
                listener(s);
            }
        })
        .await;
    }

    /// 监听音乐消息。
    pub async fn on_music<F>(&self, listener: F)
    where
        F: Fn(MusicMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::Music, move |e| {
            if let ChatRoomEvent::Music(m) = e {
                listener(m);
            }
        })
        .await;
    }

    /// 监听天气消息。
    pub async fn on_weather<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::Weather, move |e| {
            if let ChatRoomEvent::Weather(m) = e {
                listener(m);
            }
        })
        .await;
    }

    /// 监听进出场消息。
    pub async fn on_custom<F>(&self, listener: F)
    where
        F: Fn(CustomMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::Custom, move |e| {
            if let ChatRoomEvent::Custom(c) = e {
                listener(c);
            }
        })
        .await;
    }

    /// 监听表态消息。
    pub async fn on_chat_reaction<F>(&self, listener: F)
    where
        F: Fn(ChatReactionMsg) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::ChatReaction, move |e| {
            if let ChatRoomEvent::ChatReaction(r) = e {
                listener(r);
            }
        })
        .await;
    }

    /// 监听全部事件。
    pub async fn on_all<F>(&self, listener: F)
    where
        F: Fn(ChatRoomEvent) + Send + Sync + 'static,
    {
        self.add(ChatRoomEventKind::All, listener).await;
    }

    async fn add<F>(&self, kind: ChatRoomEventKind, listener: F)
    where
        F: Fn(ChatRoomEvent) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(kind, listener)
            .await;
    }

    /// 移除某类监听。
    pub async fn off(&self, kind: ChatRoomEventKind) {
        self.handler.get_emitter().remove_listener(Some(kind)).await;
    }

    /// 当前缓存话题。
    pub async fn discuss(&self) -> String {
        self.discuss.lock().await.clone()
    }

    /// 当前缓存在线人数。
    pub async fn online_count(&self) -> usize {
        self.onlines.lock().await.len()
    }

    /// 所连聊天室节点的中文名（无则 None）。
    pub fn node_name(&self) -> Option<&str> {
        self.node_name.as_deref()
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

/// 聊天室域访问器。
pub struct ChatRoomApi(pub(crate) FishPi);

impl FishPi {
    /// 聊天室域。
    pub fn chatroom(&self) -> ChatRoomApi {
        ChatRoomApi(self.clone())
    }
}

impl ChatRoomApi {
    /// 解析聊天室节点，返回 (WS URL, 节点中文名)。
    async fn chatroom_ws_url(&self) -> Result<(String, Option<String>), Error> {
        let fallback = || {
            build_ws_url(
                self.0.domain(),
                "chat-room-channel",
                &[("apiKey", self.0.api_key().to_string())],
            )
            .map(|url| (url, None))
            .map_err(Error::from)
        };

        let raw = match self.0.get_raw(&self.0.with_key("chat-room/node/get")).await {
            Ok(v) => v,
            Err(_) => return fallback(),
        };
        let node: WireChatRoomNode = match serde_json::from_value(raw) {
            Ok(n) => n,
            Err(_) => return fallback(),
        };
        let node_name = Some(node.msg.trim().to_string()).filter(|s| !s.is_empty());
        match Url::parse(&node.data) {
            Ok(mut parsed) => {
                if parsed.path().is_empty() {
                    parsed.set_path("/");
                }
                Ok((parsed.to_string(), node_name))
            }
            Err(_) => fallback(),
        }
    }

    /// 建立聊天室长连接。
    pub async fn connect(&self) -> Result<ChatRoomConnection, Error> {
        let (url, node_name) = self.chatroom_ws_url().await?;
        let handler = ChatRoomHandler::new(
            parse_chatroom_message,
            Some(ChatRoomEventKind::All),
            "chatroom",
        );
        let mut connection = WsConnection::new();
        connection
            .connect(false, &url, handler.clone())
            .await
            .map_err(Error::from)?;
        Ok(ChatRoomConnection {
            connection,
            handler,
            url,
            node_name,
            discuss: Arc::new(Mutex::new(String::new())),
            onlines: Arc::new(Mutex::new(Vec::new())),
        })
    }

    /// 发送聊天室消息。
    pub async fn send(&self, content: impl Into<String>) -> Result<(), Error> {
        self.0
            .post_raw(
                "chat-room/send",
                &json!({ "content": content.into(), "client": self.0.client_tag() }),
            )
            .await?;
        Ok(())
    }

    /// 设置聊天室话题。
    pub async fn set_discuss(&self, discuss: impl Into<String>) -> Result<(), Error> {
        self.send(format!("[setdiscuss]{}[/setdiscuss]", discuss.into()))
            .await
    }

    /// 查询聊天室历史消息。
    pub async fn history(
        &self,
        page: u32,
        content_type: ChatContentType,
    ) -> Result<Vec<ChatRoomMsg>, Error> {
        let url = build_http_path(
            "chat-room/more",
            &[
                ("page", page.to_string()),
                ("type", content_type.as_str().to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let rsp = self.0.get_raw(&url).await?;
        Ok(collect_msgs(&rsp))
    }

    /// 获取指定消息附近的消息。
    pub async fn msg_around(
        &self,
        msg_id: impl Into<MessageId>,
        mode: ChatRoomMessageMode,
        size: u32,
        content_type: ChatContentType,
    ) -> Result<Vec<ChatRoomMsg>, Error> {
        let url = build_http_path(
            "chat-room/getMessage",
            &[
                ("oId", msg_id.into().into_string()),
                ("mode", mode.as_str().to_string()),
                ("size", size.to_string()),
                ("type", content_type.as_str().to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let rsp = self.0.get_raw(&url).await?;
        Ok(collect_msgs(&rsp))
    }

    /// 撤回聊天室消息，返回服务端提示。
    pub async fn revoke(&self, msg_id: impl Into<MessageId>) -> Result<String, Error> {
        let rsp = self
            .0
            .delete_raw(&format!("chat-room/revoke/{}", msg_id.into()), &json!({}))
            .await?;
        Ok(rsp["msg"].as_str().unwrap_or_default().to_string())
    }

    /// 发送弹幕，返回服务端提示。
    pub async fn barrager(
        &self,
        content: impl Into<String>,
        color: Option<&str>,
    ) -> Result<String, Error> {
        let color = color.unwrap_or("#ffffff");
        let tag = format!(
            "[barrager]{{\"color\":\"{}\",\"content\":\"{}\"}}[/barrager]",
            color,
            content.into()
        );
        let rsp = self
            .0
            .post_raw("chat-room/send", &json!({ "content": tag }))
            .await?;
        Ok(rsp["msg"].as_str().unwrap_or("弹幕发送成功").to_string())
    }

    /// 获取弹幕花费。
    pub async fn barrage_cost(&self) -> Result<BarragerCost, Error> {
        let rsp = self
            .0
            .get_raw(&self.0.with_key("chat-room/barrager/get"))
            .await?;
        let text = rsp["data"].as_str().unwrap_or("5积分").trim();
        let cost = text.trim_end_matches("积分").parse::<u32>().unwrap_or(0);
        Ok(BarragerCost {
            cost,
            unit: "积分".to_string(),
        })
    }

    /// 获取禁言成员列表（思过崖）。
    pub async fn mutes(&self) -> Result<Vec<MuteItem>, Error> {
        let rsp = self.0.get_raw("chat-room/si-guo-list").await?;
        Ok(rsp["data"]
            .as_array()
            .map(|arr| {
                arr.iter()
                    .filter_map(|v| serde_json::from_value::<WireMuteItem>(v.clone()).ok())
                    .map(MuteItem::from)
                    .collect()
            })
            .unwrap_or_default())
    }

    /// 获取消息原文（去掉 HTML 注释尾巴）。
    pub async fn raw_message(&self, msg_id: impl Into<MessageId>) -> Result<String, Error> {
        let text = self
            .0
            .get_text(&format!("cr/raw/{}", msg_id.into()))
            .await?;
        Ok(text.split("<!--").next().unwrap_or("").trim().to_string())
    }
}

fn collect_msgs(rsp: &Value) -> Vec<ChatRoomMsg> {
    rsp["data"]
        .as_array()
        .map(|arr| {
            arr.iter()
                .filter_map(|v| serde_json::from_value::<WireChatRoomMsg>(v.clone()).ok())
                .map(ChatRoomMsg::from)
                .collect()
        })
        .unwrap_or_default()
}
