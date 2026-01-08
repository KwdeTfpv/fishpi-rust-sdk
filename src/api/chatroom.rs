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
//! - [`ChatRoomEventData`] - 聊天室事件数据枚举，包装所有消息类型（如连接成功、消息撤回、普通消息等）。
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
//! - [`ChatRoom::on`] - 添加事件监听器。
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
//! use crate::api::chatroom::{ChatRoom, ChatRoomEventData};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut chatroom = ChatRoom::new("your_api_key".to_string());
//!
//!     // 添加消息监听器
//!     chatroom.on("msg", |event: ChatRoomEventData| {
//!         match event {
//!             ChatRoomEventData::Msg(msg) => {
//!                 println!("Received message: {}", msg.content);
//!             }
//!             _ => {}
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
//!     let history = chatroom.history(1, crate::model::chatroom::ChatContentType::Html).await?;
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
//! 聊天室支持以下事件类型（通过 `on` 方法监听）：
//!
//! - `"open"` - 连接成功。
//! - `"close"` - 连接断开。
//! - `"error"` - 连接错误。
//! - `"online"` - 在线用户更新。
//! - `"discussChanged"` - 话题修改。
//! - `"revoke"` - 消息撤回。
//! - `"msg"` - 普通消息。
//! - `"barrager"` - 弹幕消息。
//! - `"redPacket"` - 红包消息。
//! - `"redPacketStatus"` - 红包状态。
//! - `"music"` - 音乐消息。
//! - `"weather"` - 天气消息。
//! - `"custom"` - 进出场消息。
//! - `"all"` - 所有事件（除了自身）。
use crate::api::ws::{MessageHandler, WebSocketClient, WebSocketError};
use crate::model::MuteItem;
use crate::model::chatroom::{
    BarragerCost, BarragerMsg, ChatContentType, ChatRoomMessageMode, ChatRoomMessageType,
    ChatRoomMsg, ClientType, CustomMsg, OnlineInfo, RevokeMsg,
};
use crate::model::redpacket::RedPacketStatusMsg;
use crate::utils::get_text;
use crate::utils::{delete, error::Error, get, post};
use serde_json::{Value, json};
use std::collections::HashMap;
use std::str::FromStr;
use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::sync::mpsc;
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
    /// 连接成功
    Open,
    /// 连接断开
    Close,
    /// 连接错误
    Error(String),
    /// 在线用户
    Online(Vec<OnlineInfo>),
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
}

/// 聊天室事件监听器类型
pub type ChatRoomListener = Box<dyn Fn(ChatRoomEventData) + Send + Sync + 'static>;

/// 聊天室消息处理器
pub struct ChatRoomHandler {
    emitter: Arc<Mutex<HashMap<String, Vec<ChatRoomListener>>>>,
}

impl Default for ChatRoomHandler {
    fn default() -> Self {
        Self {
            emitter: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

impl ChatRoomHandler {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn get_emitter(&self) -> Arc<Mutex<HashMap<String, Vec<ChatRoomListener>>>> {
        self.emitter.clone()
    }

    /// 发射事件
    async fn emit_event(
        emitter: &Arc<Mutex<HashMap<String, Vec<ChatRoomListener>>>>,
        event_type: &str,
        event: ChatRoomEventData,
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

impl MessageHandler for ChatRoomHandler {
    fn handle_message(&self, text: String) {
        if let Ok(json) = serde_json::from_str::<Value>(&text) {
            let emitter = self.get_emitter();
            tokio::spawn(async move {
                match parse_chatroom_message(&json) {
                    Ok((event_type, event)) => {
                        Self::emit_event(&emitter, &event_type, event).await;
                    }
                    Err(e) => {
                        eprintln!("解析聊天室消息失败: {}", e);
                    }
                }
            });
        }
    }
}

/// 解析聊天室消息，返回 (事件类型, 事件数据)
#[allow(non_snake_case)]
fn parse_chatroom_message(json: &Value) -> Result<(String, ChatRoomEventData), Error> {
    let type_str = json["type"]
        .as_str()
        .ok_or_else(|| Error::Parse("Missing type in message".to_string()))?;
    let r#type = ChatRoomMessageType::from_str(type_str)
        .map_err(|_| Error::Parse(format!("Unknown message type: {}", type_str)))?;

    match r#type {
        ChatRoomMessageType::Online => {
            if let Some(users) = json["users"].as_array() {
                let online_info: Vec<OnlineInfo> = users
                    .iter()
                    .filter_map(|u| {
                        Some(OnlineInfo {
                            homePage: u["homePage"].as_str()?.to_string(),
                            userAvatarURL: u["userAvatarURL"].as_str()?.to_string(),
                            userName: u["userName"].as_str()?.to_string(),
                        })
                    })
                    .collect();
                Ok((
                    ChatRoomMessageType::Online.to_string(),
                    ChatRoomEventData::Online(online_info),
                ))
            } else {
                Err(Error::Parse("Missing users in online message".to_string()))
            }
        }
        ChatRoomMessageType::DiscussChanged => {
            let new_discuss = json["newDiscuss"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing newDiscuss".to_string()))?
                .to_string();
            Ok((
                ChatRoomMessageType::DiscussChanged.to_string(),
                ChatRoomEventData::DiscussChanged(new_discuss),
            ))
        }
        ChatRoomMessageType::Revoke => {
            let o_id = json["oId"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing oId in revoke".to_string()))?
                .to_string();
            Ok((
                ChatRoomMessageType::Revoke.to_string(),
                ChatRoomEventData::Revoke(o_id),
            ))
        }
        ChatRoomMessageType::Msg => {
            let chat_msg = ChatRoomMsg::from_value(json)?;

            // 检查 content 是否为 music 或 weather
            if let Value::Object(ref obj) = chat_msg.content {
                if obj.get("msgType").and_then(|v| v.as_str()) == Some("music") {
                    Ok(("music".to_string(), ChatRoomEventData::Music(chat_msg)))
                } else if obj.get("msgType").and_then(|v| v.as_str()) == Some("weather") {
                    Ok(("weather".to_string(), ChatRoomEventData::Weather(chat_msg)))
                } else {
                    Ok((
                        ChatRoomMessageType::Msg.to_string(),
                        ChatRoomEventData::Msg(chat_msg),
                    ))
                }
            } else {
                Ok((
                    ChatRoomMessageType::Msg.to_string(),
                    ChatRoomEventData::Msg(chat_msg),
                ))
            }
        }
        ChatRoomMessageType::RedPacket => {
            let redpacket_msg = ChatRoomMsg::from_value(json)?;
            Ok((
                ChatRoomMessageType::RedPacket.to_string(),
                ChatRoomEventData::RedPacket(redpacket_msg),
            ))
        }
        ChatRoomMessageType::Barrager => {
            let barrager = BarragerMsg::from_value(json)?;
            Ok((
                ChatRoomMessageType::Barrager.to_string(),
                ChatRoomEventData::Barrager(barrager),
            ))
        }
        ChatRoomMessageType::Custom => {
            let message = json["message"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing message in custom".to_string()))?
                .to_string();
            Ok((
                ChatRoomMessageType::Custom.to_string(),
                ChatRoomEventData::Custom(CustomMsg { message }),
            ))
        }
        ChatRoomMessageType::RedPacketStatus => {
            let redpacket_status = RedPacketStatusMsg::from_value(json)?;
            Ok((
                ChatRoomMessageType::RedPacketStatus.to_string(),
                ChatRoomEventData::RedPacketStatus(redpacket_status),
            ))
        }
    }
}

impl Clone for ChatRoomHandler {
    fn clone(&self) -> Self {
        Self {
            emitter: self.emitter.clone(),
        }
    }
}

/// 聊天室客户端
pub struct ChatRoom {
    ws: Option<WebSocketClient>,
    handler: ChatRoomHandler,
    sender: Option<mpsc::UnboundedSender<String>>,
    api_key: String,
    discuss: Arc<Mutex<String>>,
    onlines: Arc<Mutex<Vec<OnlineInfo>>>,
    client: ClientType,
    version: String,
}

impl ChatRoom {
    pub fn new(api_key: String) -> Self {
        Self {
            ws: None,
            handler: ChatRoomHandler::new(),
            sender: None,
            api_key,
            discuss: Arc::new(Mutex::new(String::new())),
            onlines: Arc::new(Mutex::new(Vec::new())),
            client: ClientType::Rust,
            version: env!("CARGO_PKG_VERSION").to_string(),
        }
    }

    pub async fn get_node(&self) -> Result<ChatRoomNodeResponse, WebSocketError> {
        let url = format!("chat-room/node/get?apiKey={}", self.api_key);

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
            Err(_) => Ok(format!(
                "wss://fishpi.cn/chat-room-channel?apiKey={}",
                self.api_key
            )),
        }
    }

    /// 连接聊天室
    ///
    /// # 参数
    /// * `reload` - 是否重新连接
    pub async fn connect(&mut self, reload: bool) -> Result<(), WebSocketError> {
        if self.ws.is_some() && !reload {
            return Ok(());
        }

        let url = self.get_ws_url().await?;

        // 创建发送通道
        let (tx_send, _) = mpsc::unbounded_channel::<String>();
        self.sender = Some(tx_send);

        // 连接 WebSocket
        let ws = WebSocketClient::connect(&url, self.handler.clone()).await?;

        // 监听基础 WebSocket 事件并转换为聊天室事件
        let emitter = self.handler.get_emitter();
        ws.on_open({
            let emitter = emitter.clone();
            move || {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    ChatRoomHandler::emit_event(&emitter, "open", ChatRoomEventData::Open).await;
                });
            }
        })
        .await;

        ws.on_close({
            let emitter = emitter.clone();
            move |_reason| {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    ChatRoomHandler::emit_event(&emitter, "close", ChatRoomEventData::Close).await;
                });
            }
        })
        .await;

        ws.on_error({
            let emitter = emitter.clone();
            move |error| {
                let emitter = emitter.clone();
                tokio::spawn(async move {
                    ChatRoomHandler::emit_event(&emitter, "error", ChatRoomEventData::Error(error))
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
    /// # 参数
    /// * `event` - 事件类型
    /// * `listener` - 监听器函数 [ChatRoomEventData]
    pub async fn on<F>(&self, event: &str, listener: F)
    where
        F: Fn(ChatRoomEventData) + Send + Sync + 'static,
    {
        let discuss = Arc::clone(&self.discuss);
        let onlines = Arc::clone(&self.onlines);

        let wrapped_listener = move |event_data: ChatRoomEventData| {
            // 更新状态
            match &event_data {
                ChatRoomEventData::Online(new_onlines) => {
                    if let Ok(mut onlines_guard) = onlines.try_lock() {
                        *onlines_guard = new_onlines.clone();
                    }
                }
                ChatRoomEventData::DiscussChanged(new_discuss) => {
                    if let Ok(mut discuss_guard) = discuss.try_lock() {
                        *discuss_guard = new_discuss.clone();
                    }
                }
                _ => {}
            }
            // 用户监听器
            listener(event_data);
        };

        let mut emitter = self.handler.emitter.lock().await;
        emitter
            .entry(event.to_string())
            .or_insert_with(Vec::new)
            .push(Box::new(wrapped_listener));
    }

    /// 移除监听
    pub async fn off(&self, event: &str) {
        let mut emitter = self.handler.emitter.lock().await;
        emitter.remove(event);
    }

    /// 断开连接
    pub fn disconnect(&mut self) {
        if let Some(ws) = &self.ws {
            ws.disconnect();
        }
        self.ws = None;
        self.sender = None;
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
        let resp = get(&format!(
            "chat-room/more?page={}&type={}&apiKey={}",
            page,
            type_.as_str(),
            self.api_key
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
        let resp = get(&format!(
            "chat-room/getMessage?oId={}&mode={}&size={}&type={}&apiKey={}",
            o_id, mode, size, type_, self.api_key
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
        let resp = get(&format!("chat-room/barrager/get?apiKey={}", self.api_key)).await?;

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
