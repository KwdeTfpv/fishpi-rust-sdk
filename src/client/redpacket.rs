use crate::client::core::FishPi;
use crate::domain::redpacket::{GestureType, RedPacketInfo, RedPacketType};
use crate::utils::error::Error;
use crate::wire::redpacket::WireRedPacketInfo;
use serde::Serialize;
use serde_json::json;

#[derive(Clone, Debug, Serialize)]
pub struct RedPacketSend {
    #[serde(rename = "type")]
    pub r#type: String,
    pub money: u32,
    pub count: u32,
    #[serde(rename = "msg")]
    pub message: String,
    #[serde(rename = "recivers")]
    pub receivers: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub gesture: Option<u8>,
}

impl RedPacketSend {
    pub fn new(r#type: RedPacketType, money: u32, count: u32, message: String) -> Self {
        Self {
            r#type: r#type.as_str().to_string(),
            money,
            count,
            message,
            receivers: Vec::new(),
            gesture: None,
        }
    }

    pub fn with_receivers(mut self, receivers: Vec<String>) -> Self {
        self.receivers = receivers;
        self
    }

    pub fn with_gesture(mut self, gesture: GestureType) -> Self {
        self.gesture = Some(gesture as u8);
        self
    }
}

/// 红包域访问器。
pub struct RedPacketApi(pub(crate) FishPi);

impl FishPi {
    /// 红包域。
    pub fn redpacket(&self) -> RedPacketApi {
        RedPacketApi(self.clone())
    }
}

impl RedPacketApi {
    pub async fn open(
        &self,
        o_id: impl Into<String>,
        gesture: Option<GestureType>,
    ) -> Result<RedPacketInfo, Error> {
        let o_id = o_id.into();
        let wire: WireRedPacketInfo = self
            .0
            .post_data(
                "chat-room/red-packet/open",
                &json!({
                    "oId": o_id,
                    "gesture": gesture.map(|g| g as u8),
                }),
            )
            .await?;
        Ok(wire.into())
    }

    pub async fn send(&self, redpacket: RedPacketSend) -> Result<(), Error> {
        let data = serde_json::to_value(&redpacket).map_err(|e| Error::decode(e.to_string()))?;

        let msg = format!("[redpacket]{}[/redpacket]", data);

        self.0.chatroom().send(msg).await?;
        Ok(())
    }
}
