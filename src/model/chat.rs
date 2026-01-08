use crate::{impl_str_enum, utils::error::Error};
use serde::Deserialize;
use serde_json::Value;

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ChatData {
    pub toId: String,
    pub preview: String,
    pub user_session: String,
    pub senderAvatar: String,
    pub markdown: String,
    pub receiverAvatar: String,
    pub oId: String,
    pub time: String,
    pub fromId: String,
    pub senderUserName: String,
    pub content: String,
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
