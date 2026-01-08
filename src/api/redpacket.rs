//! 红包 API 模块
//!
//! 这个模块提供了与红包相关的 API 操作，包括打开红包、发送红包等功能。
//! 主要结构体是 `Redpacket`，用于管理红包的发送和接收。
//! 红包支持普通红包、猜拳红包等类型。
//!
//! # 主要组件
//!
//! - [`Redpacket`] - 红包客户端结构体，负责打开和发送红包。
//!
//! # 方法列表
//!
//! - [`Redpacket::new`] - 创建新的红包客户端实例。
//! - [`Redpacket::open`] - 打开一个红包。
//! - [`Redpacket::send`] - 发送一个红包。
//!
//! # 示例
//!
//! ```rust,no_run
//! use crate::api::redpacket::Redpacket;
//! use crate::model::redpacket::{RedPacket, RedPacketType, GestureType};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let redpacket = Redpacket::new("your_api_key".to_string());
//!
//!     // 发送红包
//!     let rp = RedPacket {
//!         r#type: RedPacketType::Random,
//!         money: 32,
//!         count: 5,
//!         msg: "古德古德".to_string(),
//!         recivers: None,
//!         gesture: Some(GestureType::Rock),
//!     };
//!     redpacket.send(&rp).await?;
//!
//!     // 打开红包
//!     let info = redpacket.open("redpacket_id", Some(GestureType::Paper)).await?;
//!     println!("Opened redpacket: {:?}", info);
//!
//!     Ok(())
//! }
//! ```
use serde_json::json;

use crate::api::chatroom::ChatRoom;
use crate::model::redpacket::{GestureType, RedPacket, RedPacketInfo};
use crate::utils::error::Error;
use crate::utils::post;

pub struct Redpacket {
    api_key: String,
    chatroom: ChatRoom,
}

impl Redpacket {
    pub fn new(api_key: String) -> Self {
        Self {
            api_key: api_key.clone(),
            chatroom: ChatRoom::new(api_key),
        }
    }

    /// 打开一个红包
    ///
    /// * `oId` 红包消息 Id
    /// * `gesture` 猜拳类型 [GestureType]
    ///
    /// [RedPacketInfo]返回红包信息
    pub async fn open(
        &self,
        oid: &str,
        gesture: Option<GestureType>,
    ) -> Result<RedPacketInfo, Error> {
        let url = "chat-room/red-packet/open".to_string();

        let data = json!({
            "oId": oid,
            "gesture": gesture.map(|g| g as u8),
            "apiKey": self.api_key
        });

        let resp = post(&url, Some(data)).await?;

        if let Some(code) = resp.get("code").and_then(|c| c.as_i64())
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"].as_str().unwrap_or("API error").to_string(),
            ));
        }

        let red_packet_info: RedPacketInfo = RedPacketInfo::from_value(&resp)?;
        Ok(red_packet_info)
    }

    /// 发送一个红包
    ///
    /// #### 参数
    /// * `redpacket` 红包对象 [RedPacket]
    pub async fn send(&self, redpacket: &RedPacket) -> Result<(), Error> {
        let data = json!({
            "type": redpacket.r#type.as_str(),
            "money": redpacket.money,
            "count": redpacket.count,
            "msg": redpacket.msg,
            "recivers": redpacket.recivers,
            "gesture": redpacket.gesture.clone().map(|g| g as u8),
            "apiKey": self.api_key
        });

        self.chatroom
            .send(format!("[redpacket]{}[/redpacket]", data))
            .await?;
        Ok(())
    }
}
