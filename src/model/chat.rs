use crate::{impl_str_enum, utils::error::Error};
use serde::Deserialize;
use serde_json::Value;

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ChatData {
    #[serde(default)]
    pub toId: String,
    #[serde(default)]
    pub preview: String,
    #[serde(default)]
    pub user_session: String,
    #[serde(default)]
    pub senderAvatar: String,
    #[serde(default)]
    pub markdown: String,
    #[serde(default)]
    pub receiverAvatar: String,
    #[serde(default)]
    pub oId: String,
    #[serde(default)]
    pub time: String,
    #[serde(default)]
    pub fromId: String,
    #[serde(default)]
    pub senderUserName: String,
    #[serde(default)]
    pub content: String,
    #[serde(default)]
    pub receiverUserName: String,
}
impl ChatData {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ChatData: {}", e)))
    }
}

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ChatNotice {
    pub command: String,
    pub userId: String,
    pub preview: String,
    pub senderAvatar: String,
    pub senderUserName: String,
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
