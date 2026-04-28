//! 聊天室 API 模块
//!
//! 这个模块提供了与聊天室相关的 API 操作，包括连接聊天室、发送消息、监听事件、获取历史消息、撤回消息、发送弹幕等功能。
//! 主要结构体是 `ChatRoom`，用于管理聊天室的 WebSocket 连接和事件监听。
//! 事件通过 `ChatRoomEventData` 枚举表示，支持多种消息类型（如普通消息、弹幕、红包等）。
//!
//! # 主要组件
//!
//! - [`ChatRoom`] - 聊天室客户端结构体，负责连接、发送消息和管理监听器。
//! - [`ChatRoomHandler`] - 聊天室消息处理器，实现 `MessageHandler` trait，处理 WebSocket 消息并发射事件。
//! - [`ChatRoomEventData`] - 聊天室事件数据枚举，包装所有消息类型（如在线用户、话题修改、普通消息等）。
//! - [`ChatRoomListener`] - 聊天室事件监听器类型别名，定义监听器函数的签名。
//! - [`ChatRoomNodeResponse`] 和 [`ChatRoomAvailableNode`] - 聊天室节点相关结构体，用于获取可用节点。
//!
//! # 方法列表
//!
//! - [`ChatRoom::new`] - 创建新的聊天室客户端实例。
//! - [`ChatRoom::get_node`] - 获取聊天室节点信息。
//! - [`ChatRoom::get_ws_url`] - 获取 WebSocket URL。
//! - [`ChatRoom::connect`] - 连接聊天室。
//! - [`ChatRoom::reconnect`] - 重连聊天室。
//! - [`ChatRoom::on_online`] - 监听在线用户更新事件。
//! - [`ChatRoom::on_discuss`] - 监听话题变更事件。
//! - [`ChatRoom::on_revoke`] - 监听消息撤回事件。
//! - [`ChatRoom::on_msg`] - 监听普通消息事件。
//! - [`ChatRoom::on_barrager`] - 监听弹幕消息事件。
//! - [`ChatRoom::on_redpacket`] - 监听红包消息事件。
//! - [`ChatRoom::on_redpacketstatus`] - 监听红包状态事件。
//! - [`ChatRoom::on_music`] - 监听音乐消息事件。
//! - [`ChatRoom::on_weather`] - 监听天气消息事件。
//! - [`ChatRoom::on_custom`] - 监听进出场消息事件。
//! - [`ChatRoom::off`] - 移除事件监听器。
//! - [`ChatRoom::disconnect`] - 断开连接。
//! - [`ChatRoom::send`] - 发送消息。
//! - [`ChatRoom::get_discuss`] - 获取当前话题。
//! - [`ChatRoom::set_discuss`] - 设置当前话题。
//! - [`ChatRoom::get_online_count`] - 获取在线人数。
//! - [`ChatRoom::set_api_key`] - 设置 API 密钥。
//! - [`ChatRoom::set_client_type`] - 设置客户端类型。
//! - [`ChatRoom::history`] - 查询历史消息。
//! - [`ChatRoom::get_msg_around`] - 获取指定消息附近的聊天室消息。
//! - [`ChatRoom::revoke`] - 撤回消息。
//! - [`ChatRoom::barrager`] - 发送弹幕。
//! - [`ChatRoom::barrage_cost`] - 获取弹幕花费。
//! - [`ChatRoom::mutes`] - 获取禁言成员列表。
//! - [`ChatRoom::get_raw_message`] - 获取消息原文。
//!
//! # 示例
//!
//! ```rust,no_run
//! use fishpi_sdk::api::chatroom::ChatRoom;
//! use fishpi_sdk::model::chatroom::{BarragerMsg, ChatContentType, ChatRoomMsg, OnlineInfo};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut chatroom = ChatRoom::new("your_api_key".to_string());
//!
//!     // 监听普通消息（直接传递 ChatRoomMsg，无需 match）
//!     chatroom.on_msg(|msg: ChatRoomMsg| {
//!         println!("Received message: {}", msg.content);
//!     }).await;
//!
//!     // 监听弹幕消息
//!     chatroom.on_barrager(|barrager: BarragerMsg| {
//!         println!("Barrage: {}", barrager.barragerContent);
//!     }).await;
//!
//!     // 监听在线用户更新
//!     chatroom.on_online(|users: Vec<OnlineInfo>, discussing, online_cnt| {
//!         println!("Online users: {}", online_cnt.unwrap_or(users.len()));
//!         if let Some(topic) = discussing {
//!             println!("Topic: {}", topic);
//!         }
//!     }).await;
//!
//!     // 连接聊天室
//!     chatroom.connect(false).await?;
//!
//!     // 发送消息
//!     chatroom.send("Hello, world!".to_string()).await?;
//!
//!     // 获取历史消息
//!     let history = chatroom.history(1, ChatContentType::Html).await?;
//!     for msg in history {
//!         println!("History: {}", msg.content);
//!     }
//!
//!     Ok(())
//! }
//! ```
//!
//! # 事件类型 [ChatRoomEventType]
//!
//! 聊天室支持以下事件类型（通过特定 `on_*` 方法监听）：
//!
//! - `Online` - 在线用户更新。
//! - `DiscussChanged` - 话题修改。
//! - `Revoke` - 消息撤回。
//! - `Msg` - 普通消息。
//! - `Barrager` - 弹幕消息。
//! - `RedPacket` - 红包消息。
//! - `RedPacketStatus` - 红包状态。
//! - `Music` - 音乐消息。
//! - `Weather` - 天气消息。
//! - `Custom` - 进出场消息。
//! - `All` - 所有事件（除了自身）。

use crate::api::ws::{
    ParsedMessageHandler, RetryPolicy, WebSocketError, WsConnection, WsLogHook, build_ws_url,
};
use crate::model::MuteItem;
use crate::model::chatroom::{
    BarragerCost, BarragerMsg, ChatContentType, ChatReactionMsg, ChatRoomMessageMode,
    ChatRoomMessageType, ChatRoomMsg, ClientType, CustomMsg, OnlineInfo, RevokeMsg,
};
use crate::model::reaction::ReactionMutationResult;
use crate::model::redpacket::RedPacketStatusMsg;
use crate::utils::get_text;
use crate::utils::{build_http_path, delete, error::Error, get, post};
use serde_json::{Value, json};
use std::str::FromStr;
use std::sync::Arc;
use tokio::sync::Mutex;
use url::Url;

#[derive(Debug, Clone, serde::Deserialize)]
#[allow(non_snake_case)]
pub struct ChatRoomNodeResponse {
    pub msg: String,
    pub code: i32,
    pub data: String,
    pub apiKey: String,
    pub avaliable: Vec<ChatRoomAvailableNode>,
}

#[derive(Debug, Clone, serde::Deserialize)]
pub struct ChatRoomAvailableNode {
    pub node: String,
    pub name: String,
    pub weight: u32,
    pub online: u32,
}

/// 聊天室事件数据（包装所有消息类型）
#[derive(Debug, Clone)]
pub enum ChatRoomEventData {
    /// 在线用户
    Online {
        users: Vec<OnlineInfo>,
        discussing: Option<String>,
        online_chat_cnt: Option<usize>,
    },
    /// 话题修改
    DiscussChanged(String),
    /// 消息撤回
    Revoke(String),
    /// 普通消息
    Msg(ChatRoomMsg),
    /// 弹幕消息
    Barrager(BarragerMsg),
    /// 红包消息
    RedPacket(ChatRoomMsg<Value>),
    /// 红包状态
    RedPacketStatus(RedPacketStatusMsg),
    /// 音乐消息
    Music(ChatRoomMsg<Value>),
    /// 天气消息
    Weather(ChatRoomMsg<Value>),
    /// 进出场消息
    Custom(CustomMsg),
    /// 聊天室表态/反应
    ChatReaction(ChatReactionMsg),
}

/// 聊天室事件类型枚举
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ChatRoomEventType {
    /// 在线用户更新
    Online,
    /// 话题修改
    DiscussChanged,
    /// 消息撤回
    Revoke,
    /// 普通消息
    Msg,
    /// 弹幕消息
    Barrager,
    /// 红包消息
    RedPacket,
    /// 红包状态
    RedPacketStatus,
    /// 音乐消息
    Music,
    /// 天气消息
    Weather,
    /// 进出场消息
    Custom,
    /// 聊天室表态/反应
    ChatReaction,
    /// 所有事件（除了自身）
    All,
}

/// 聊天室事件监听器类型
pub type ChatRoomListener = Arc<dyn Fn(ChatRoomEventData) + Send + Sync + 'static>;

/// 聊天室消息处理器
pub type ChatRoomHandler = ParsedMessageHandler<ChatRoomEventType, ChatRoomEventData>;

/// 解析聊天室消息，返回 (事件类型, 事件数据)
#[allow(non_snake_case)]
fn parse_chatroom_message(json: &Value) -> Result<(ChatRoomEventType, ChatRoomEventData), Error> {
    let type_str = json["type"]
        .as_str()
        .ok_or_else(|| Error::Parse("Missing type in message".to_string()))?;
    let r#type = ChatRoomMessageType::from_str(type_str)
        .map_err(|_| Error::Parse(format!("Unknown message type: {}", type_str)))?;

    match r#type {
        ChatRoomMessageType::Online => {
            let online_info: Vec<OnlineInfo> = json["users"]
                .as_array()
                .map(|users| {
                    users
                        .iter()
                        .filter_map(|u| {
                            Some(OnlineInfo {
                                homePage: u["homePage"].as_str()?.to_string(),
                                userAvatarURL: u["userAvatarURL"].as_str()?.to_string(),
                                userName: u["userName"].as_str()?.to_string(),
                            })
                        })
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
                ChatRoomEventType::Online,
                ChatRoomEventData::Online {
                    users: online_info,
                    discussing,
                    online_chat_cnt,
                },
            ))
        }
        ChatRoomMessageType::DiscussChanged => {
            let new_discuss = json["newDiscuss"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing newDiscuss".to_string()))?
                .to_string();
            Ok((
                ChatRoomEventType::DiscussChanged,
                ChatRoomEventData::DiscussChanged(new_discuss),
            ))
        }
        ChatRoomMessageType::Revoke => {
            let o_id = json["oId"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing oId in revoke".to_string()))?
                .to_string();
            Ok((ChatRoomEventType::Revoke, ChatRoomEventData::Revoke(o_id)))
        }
        ChatRoomMessageType::Msg => {
            let chat_msg = ChatRoomMsg::from_value(json)?;

            // 检查 content 是否为 music 或 weather
            if let Value::Object(ref obj) = chat_msg.content {
                if obj.get("msgType").and_then(|v| v.as_str()) == Some("music") {
                    Ok((ChatRoomEventType::Music, ChatRoomEventData::Music(chat_msg)))
                } else if obj.get("msgType").and_then(|v| v.as_str()) == Some("weather") {
                    Ok((
                        ChatRoomEventType::Weather,
                        ChatRoomEventData::Weather(chat_msg),
                    ))
                } else {
                    Ok((ChatRoomEventType::Msg, ChatRoomEventData::Msg(chat_msg)))
                }
            } else {
                Ok((ChatRoomEventType::Msg, ChatRoomEventData::Msg(chat_msg)))
            }
        }
        ChatRoomMessageType::RedPacket => {
            let redpacket_msg = ChatRoomMsg::from_value(json)?;
            Ok((
                ChatRoomEventType::RedPacket,
                ChatRoomEventData::RedPacket(redpacket_msg),
            ))
        }
        ChatRoomMessageType::Barrager => {
            let barrager = BarragerMsg::from_value(json)?;
            Ok((
                ChatRoomEventType::Barrager,
                ChatRoomEventData::Barrager(barrager),
            ))
        }
        ChatRoomMessageType::Custom => {
            let message = json["message"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing message in custom".to_string()))?
                .to_string();
            Ok((
                ChatRoomEventType::Custom,
                ChatRoomEventData::Custom(CustomMsg { message }),
            ))
        }
        ChatRoomMessageType::RedPacketStatus => {
            let redpacket_status = RedPacketStatusMsg::from_value(json)?;
            Ok((
                ChatRoomEventType::RedPacketStatus,
                ChatRoomEventData::RedPacketStatus(redpacket_status),
            ))
        }
        ChatRoomMessageType::ChatReaction => Ok((
            ChatRoomEventType::ChatReaction,
            ChatRoomEventData::ChatReaction(ChatReactionMsg::from_value(json)?),
        )),
    }
}

/// 聊天室客户端
pub struct ChatRoom {
    connection: WsConnection,
    handler: ChatRoomHandler,
    api_key: String,
    discuss: Arc<Mutex<String>>,
    onlines: Arc<Mutex<Vec<OnlineInfo>>>,
    client: ClientType,
    version: String,
}

impl ChatRoom {
    pub fn new(api_key: String) -> Self {
        Self {
            connection: WsConnection::new(),
            handler: ChatRoomHandler::new(
                parse_chatroom_message,
                Some(ChatRoomEventType::All),
                "chatroom",
            ),
            api_key,
            discuss: Arc::new(Mutex::new(String::new())),
            onlines: Arc::new(Mutex::new(Vec::new())),
            client: ClientType::Rust,
            version: env!("CARGO_PKG_VERSION").to_string(),
        }
    }

    pub async fn get_node(&self) -> Result<ChatRoomNodeResponse, WebSocketError> {
        let url = build_http_path("chat-room/node/get", &[("apiKey", self.api_key.clone())]);

        let response: Value = get(&url)
            .await
            .map_err(|e| WebSocketError::Other(format!("请求失败：{}", e)))?;

        let code = response["code"].as_i64().unwrap_or(-1) as i32;
        if code != 0 {
            let msg = response["msg"].as_str().unwrap_or("未知错误");
            return Err(WebSocketError::Other(format!("获取节点失败：{}", msg)));
        }

        let node_response: ChatRoomNodeResponse = serde_json::from_value(response)
            .map_err(|e| WebSocketError::Other(format!("解析节点信息失败：{}", e)))?;
        Ok(node_response)
    }

    /// 获取 WebSocket URL
    pub async fn get_ws_url(&self) -> Result<String, WebSocketError> {
        match self.get_node().await {
            Ok(node_response) => {
                let mut parsed = Url::parse(&node_response.data)
                    .map_err(|e| WebSocketError::Other(format!("URL parse error: {}", e)))?;
                if parsed.path() == "" {
                    parsed.set_path("/");
                }
                Ok(parsed.to_string())
            }
            Err(_) => build_ws_url(
                "fishpi.cn",
                "chat-room-channel",
                &[("apiKey", self.api_key.clone())],
            ),
        }
    }

    /// 连接聊天室
    ///
    /// # 参数
    /// * `reload` - 是否重新连接
    pub async fn connect(&mut self, reload: bool) -> Result<(), WebSocketError> {
        let url = self.get_ws_url().await?;
        self.connection
            .connect(reload, &url, self.handler.clone())
            .await
    }

    /// 重连
    pub async fn reconnect(&mut self) -> Result<(), WebSocketError> {
        let url = self.get_ws_url().await?;
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

    /// 监听在线用户更新事件（同时可获取 discussing / onlineChatCnt）
    pub async fn on_online<F>(&self, listener: F)
    where
        F: Fn(Vec<OnlineInfo>, Option<String>, Option<usize>) + Send + Sync + 'static,
    {
        let onlines = Arc::clone(&self.onlines);
        let discuss = Arc::clone(&self.discuss);
        let wrapped_listener: ChatRoomListener = Arc::new(move |event: ChatRoomEventData| {
            if let ChatRoomEventData::Online {
                users,
                discussing,
                online_chat_cnt,
            } = event
            {
                // 更新状态
                if !users.is_empty()
                    && let Ok(mut onlines_guard) = onlines.try_lock()
                {
                    *onlines_guard = users.clone();
                }
                if let Some(topic) = discussing.as_ref()
                    && let Ok(mut discuss_guard) = discuss.try_lock()
                {
                    *discuss_guard = topic.to_string();
                }
                listener(users, discussing, online_chat_cnt);
            }
        });
        self.handler
            .get_emitter()
            .add_listener(ChatRoomEventType::Online, move |event| {
                wrapped_listener(event)
            })
            .await;
    }

    /// 监听话题变更事件
    pub async fn on_discuss<F>(&self, listener: F)
    where
        F: Fn(String) + Send + Sync + 'static,
    {
        let discuss = Arc::clone(&self.discuss);
        let wrapped_listener: ChatRoomListener = Arc::new(move |event: ChatRoomEventData| {
            if let ChatRoomEventData::DiscussChanged(topic) = event {
                // 更新状态
                if let Ok(mut discuss_guard) = discuss.try_lock() {
                    *discuss_guard = topic.clone();
                }
                listener(topic);
            }
        });
        self.handler
            .get_emitter()
            .add_listener(ChatRoomEventType::DiscussChanged, move |event| {
                wrapped_listener(event)
            })
            .await;
    }

    /// 监听消息撤回事件
    pub async fn on_revoke<F>(&self, listener: F)
    where
        F: Fn(String) + Send + Sync + 'static,
    {
        self.add_listener(
            ChatRoomEventType::Revoke,
            move |event: ChatRoomEventData| {
                if let ChatRoomEventData::Revoke(msg_id) = event {
                    listener(msg_id);
                }
            },
        )
        .await;
    }

    /// 监听普通消息事件
    pub async fn on_msg<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg) + Send + Sync + 'static,
    {
        self.add_listener(ChatRoomEventType::Msg, move |event: ChatRoomEventData| {
            if let ChatRoomEventData::Msg(msg) = event {
                listener(msg);
            }
        })
        .await;
    }

    /// 监听弹幕消息事件
    pub async fn on_barrager<F>(&self, listener: F)
    where
        F: Fn(BarragerMsg) + Send + Sync + 'static,
    {
        self.add_listener(
            ChatRoomEventType::Barrager,
            move |event: ChatRoomEventData| {
                if let ChatRoomEventData::Barrager(barrager) = event {
                    listener(barrager);
                }
            },
        )
        .await;
    }

    /// 监听红包消息事件
    pub async fn on_redpacket<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg<Value>) + Send + Sync + 'static,
    {
        self.add_listener(
            ChatRoomEventType::RedPacket,
            move |event: ChatRoomEventData| {
                if let ChatRoomEventData::RedPacket(red_packet) = event {
                    listener(red_packet);
                }
            },
        )
        .await;
    }

    /// 监听红包状态事件
    pub async fn on_redpacketstatus<F>(&self, listener: F)
    where
        F: Fn(RedPacketStatusMsg) + Send + Sync + 'static,
    {
        self.add_listener(
            ChatRoomEventType::RedPacketStatus,
            move |event: ChatRoomEventData| {
                if let ChatRoomEventData::RedPacketStatus(status) = event {
                    listener(status);
                }
            },
        )
        .await;
    }

    /// 监听音乐消息事件
    pub async fn on_music<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg<Value>) + Send + Sync + 'static,
    {
        self.add_listener(ChatRoomEventType::Music, move |event: ChatRoomEventData| {
            if let ChatRoomEventData::Music(music) = event {
                listener(music);
            }
        })
        .await;
    }

    /// 监听天气消息事件
    pub async fn on_weather<F>(&self, listener: F)
    where
        F: Fn(ChatRoomMsg<Value>) + Send + Sync + 'static,
    {
        self.add_listener(
            ChatRoomEventType::Weather,
            move |event: ChatRoomEventData| {
                if let ChatRoomEventData::Weather(weather) = event {
                    listener(weather);
                }
            },
        )
        .await;
    }

    /// 监听进出场消息事件
    pub async fn on_custom<F>(&self, listener: F)
    where
        F: Fn(CustomMsg) + Send + Sync + 'static,
    {
        self.add_listener(
            ChatRoomEventType::Custom,
            move |event: ChatRoomEventData| {
                if let ChatRoomEventData::Custom(custom) = event {
                    listener(custom);
                }
            },
        )
        .await;
    }

    pub async fn on_all<F>(&self, listener: F)
    where
        F: Fn(ChatRoomEventData) + Send + Sync + 'static,
    {
        self.add_listener(ChatRoomEventType::All, listener).await;
    }

    async fn add_listener<F>(&self, event: ChatRoomEventType, listener: F)
    where
        F: Fn(ChatRoomEventData) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(event, listener)
            .await;
    }

    /// 移除监听
    pub async fn off(&self, event: ChatRoomEventType) {
        self.handler
            .get_emitter()
            .remove_listener(Some(event))
            .await;
    }

    /// 断开连接
    pub fn disconnect(&mut self) {
        self.connection.disconnect();
    }

    /// 发送消息
    ///
    /// # 参数
    /// * `msg` - 消息内容
    pub async fn send(&self, msg: String) -> Result<(), Error> {
        let client = format!("{}/{}", self.client.as_str(), self.version);

        let data = json!({
            "content": msg,
            "client": client,
            "apiKey": self.api_key,
        });

        let resp = post("chat-room/send", Some(data)).await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("发送失败").to_string(),
            ));
        }

        Ok(())
    }

    /// 当前话题
    pub async fn get_discuss(&self) -> String {
        let discuss_guard = self.discuss.lock().await;
        discuss_guard.clone()
    }

    /// 设置当前话题
    ///
    /// # 参数
    /// * `discuss` - 新话题
    pub async fn set_discuss(&self, discuss: String) {
        self.send(format!("[setdiscuss]{}[/setdiscuss]", discuss))
            .await
            .ok();
    }

    /// 当前在线人数
    pub async fn get_online_count(&self) -> usize {
        let onlines_guard = self.onlines.lock().await;
        onlines_guard.len()
    }

    /// 重新设置apiKey
    pub fn set_api_key(&mut self, api_key: String) {
        self.api_key = api_key;
    }

    /// 设置客户端类型
    ///
    /// #### 参数
    /// * `client` - 客户端类型 [ClientType]
    /// * `version` - 版本号
    pub fn set_client_type(&mut self, client: ClientType, version: Option<String>) {
        self.client = client;
        self.version = version.unwrap_or_else(|| "last".to_string());
    }

    /// 查询聊天室历史消息
    ///
    /// #参数
    /// `page` - 页码
    /// `type_` - 内容类型 [ChatContentType]
    pub async fn history(
        &self,
        page: u32,
        type_: ChatContentType,
    ) -> Result<Vec<ChatRoomMsg>, Error> {
        let resp = get(&build_http_path(
            "chat-room/more",
            &[
                ("page", page.to_string()),
                ("type", type_.as_str().to_string()),
                ("apiKey", self.api_key.clone()),
            ],
        ))
        .await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("Api error").to_string(),
            ));
        }

        let messages: Vec<ChatRoomMsg> = resp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(ChatRoomMsg::from_value)
            .collect::<Result<Vec<_>, _>>()?;
        Ok(messages)
    }

    /// 获取指定消息附近的聊天室消息
    ///
    /// # 参数
    /// * `o_id` - 消息 Id
    /// * `mode` - 获取模式，context 上下文模式，after 之后模式 [ChatRoomMessageMode]
    /// * `size` - 获取消息数量，默认 25，最大 100
    /// * `type_` - 获取消息类型，默认 HTML [ChatContentType]
    /// * 返回 [ChatRoomMsg] 消息列表
    pub async fn get_msg_around(
        &self,
        o_id: &str,
        mode: ChatRoomMessageMode,
        size: u32,
        type_: ChatContentType,
    ) -> Result<Vec<ChatRoomMsg>, Error> {
        let resp = get(&build_http_path(
            "chat-room/getMessage",
            &[
                ("oId", o_id.to_string()),
                ("mode", mode.to_string()),
                ("size", size.to_string()),
                ("type", type_.as_str().to_string()),
                ("apiKey", self.api_key.clone()),
            ],
        ))
        .await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("Api error").to_string(),
            ));
        }

        let messages: Vec<ChatRoomMsg> = resp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(ChatRoomMsg::from_value)
            .collect::<Result<Vec<_>, _>>()?;

        Ok(messages)
    }

    /// 撤回消息
    ///
    /// #### 参数
    /// * `o_id` - 消息 ID
    /// #### 返回 [RevokeMsg]
    pub async fn revoke(&self, o_id: &str) -> Result<RevokeMsg, Error> {
        let data = json!({
            "apiKey": self.api_key,
        });
        let resp = delete(&format!("chat-room/revoke/{}", o_id), Some(data)).await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("Api error").to_string(),
            ));
        }

        Ok(RevokeMsg {
            msg: resp["msg"].as_str().unwrap_or("").to_string(),
        })
    }

    /// 给聊天室消息添加/切换/取消 emoji reaction。
    ///
    /// 再次发送相同 value 表示取消；发送不同 value 表示切换。
    pub async fn reaction(&self, o_id: &str, value: &str) -> Result<ReactionMutationResult, Error> {
        crate::api::reaction::Reaction::new(self.api_key.clone())
            .chat_room(o_id, value)
            .await
    }

    /// 发送弹幕
    ///
    /// #### 参数
    /// * `msg` - 弹幕内容
    /// * `color` - 颜色（可选）
    pub async fn barrager(&self, msg: String, color: Option<String>) -> Result<String, Error> {
        let color = color.unwrap_or("#ffffff".to_string());

        let data = json!({
            "content": format!("[barrager]{{\"color\":\"{}\",\"content\":\"{}\"}}[/barrager]",color, msg),
            "apiKey": self.api_key,
        });

        let resp = post("chat-room/send", Some(data)).await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("弹幕发送失败").to_string(),
            ));
        }

        Ok(resp["msg"].as_str().unwrap_or("弹幕发送成功").to_string())
    }

    /// 获取弹幕花费
    /// #### 返回 [BarragerCost]
    pub async fn barrage_cost(&self) -> Result<BarragerCost, Error> {
        let resp = get(&build_http_path(
            "chat-room/barrager/get",
            &[("apiKey", self.api_key.clone())],
        ))
        .await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"]
                    .as_str()
                    .unwrap_or("获取弹幕花费失败")
                    .to_string(),
            ));
        }

        Ok(BarragerCost::from_value(&resp["data"]))
    }

    /// 获取禁言中成员列表（思过崖）
    ///
    /// 返回禁言中成员列表 [MuteItem]
    pub async fn mutes(&self) -> Result<Vec<MuteItem>, Error> {
        let resp = get("chat-room/si-guo-list").await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"]
                    .as_str()
                    .unwrap_or("获取禁言成员列表失败")
                    .to_string(),
            ));
        }

        let messages: Vec<MuteItem> = resp["data"]
            .as_array()
            .ok_or_else(|| Error::Api("Data is not an array".to_string()))?
            .iter()
            .map(MuteItem::from_value)
            .collect::<Result<Vec<_>, _>>()?;

        Ok(messages)
    }

    /// 获取消息原文（比如 Markdown）
    ///
    /// #### 参数
    /// * `o_id` - 消息 ID
    pub async fn get_raw_message(&self, o_id: &str) -> Result<String, Error> {
        let resp = get_text(&format!("cr/raw/{}", o_id,)).await?;

        let raw_message = resp.split("<!--").next().unwrap_or("").trim().to_string();

        Ok(raw_message)
    }
}

#[cfg(test)]
mod tests {
    use super::{ChatRoomEventData, ChatRoomEventType, parse_chatroom_message};
    use serde_json::json;

    #[test]
    fn parse_chatroom_custom_message() {
        let payload = json!({
            "type": "customMessage",
            "message": "user joined"
        });

        let (event_type, event) = parse_chatroom_message(&payload).expect("should parse");
        assert!(matches!(event_type, ChatRoomEventType::Custom));
        match event {
            ChatRoomEventData::Custom(msg) => assert_eq!(msg.message, "user joined"),
            _ => panic!("unexpected event variant"),
        }
    }

    #[test]
    fn parse_chatroom_reaction_message() {
        let payload = json!({
            "type": "chatreaction",
            "oId": "reaction-id",
            "data": {
                "target": "message-id"
            }
        });

        let (event_type, event) = parse_chatroom_message(&payload).expect("should parse");
        assert!(matches!(event_type, ChatRoomEventType::ChatReaction));
        match event {
            ChatRoomEventData::ChatReaction(reaction) => {
                assert_eq!(reaction.raw["type"], "chatreaction");
                assert_eq!(reaction.oId, "reaction-id");
            }
            _ => panic!("unexpected event variant"),
        }
    }

    #[test]
    fn parse_chatroom_unknown_type_fails() {
        let payload = json!({
            "type": "nope"
        });
        assert!(parse_chatroom_message(&payload).is_err());
    }
}
