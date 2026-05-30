use crate::{impl_str_enum, utils::error::Error};
use serde::Deserialize;
use serde_json::Value;

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ChatData {
    #[serde(default, rename = "toId")]
    pub to_id: String,
    #[serde(default)]
    pub preview: String,
    #[serde(default)]
    pub user_session: String,
    #[serde(default, rename = "senderAvatar")]
    pub sender_avatar: String,
    #[serde(default)]
    pub markdown: String,
    #[serde(default, rename = "receiverAvatar")]
    pub receiver_avatar: String,
    #[serde(default)]
    pub oId: String,
    #[serde(default)]
    pub time: String,
    #[serde(default, rename = "fromId")]
    pub from_id: String,
    #[serde(default, rename = "senderUserName")]
    pub sender_user_name: String,
    #[serde(default)]
    pub content: String,
    #[serde(default, rename = "receiverUserName")]
    pub receiver_user_name: String,
}
impl ChatData {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ChatData: {}", e)))
    }
}

#[derive(Clone, Debug, Deserialize)]
pub struct ChatNotice {
    pub command: String,
    #[serde(rename = "userId")]
    pub user_id: String,
    pub preview: String,
    #[serde(rename = "senderAvatar")]
    pub sender_avatar: String,
    #[serde(rename = "senderUserName")]
    pub sender_user_name: String,
}

impl ChatNotice {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ChatNotice: {}", e)))
    }
}

#[derive(Clone, Debug, Deserialize)]
pub struct ChatRevoke {
    pub data: String,
}

impl ChatRevoke {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ChatRevoke: {}", e)))
    }
}

pub enum ChatMsgType {
    Notice,
    Data,
    Revoke,
}

impl_str_enum!(ChatMsgType {
    Notice => "notice",
    Data => "data",
    Revoke => "revoke"
});
