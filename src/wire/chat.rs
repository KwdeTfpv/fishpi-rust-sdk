use crate::wire::de::string_lossy;
use serde::Deserialize;

/// 私聊消息原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireChatData {
    #[serde(rename = "toId", default, deserialize_with = "string_lossy")]
    pub to_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub preview: String,
    #[serde(rename = "user_session", default, deserialize_with = "string_lossy")]
    pub user_session: String,
    #[serde(rename = "senderAvatar", default, deserialize_with = "string_lossy")]
    pub sender_avatar: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub markdown: String,
    #[serde(rename = "receiverAvatar", default, deserialize_with = "string_lossy")]
    pub receiver_avatar: String,
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub time: String,
    #[serde(rename = "fromId", default, deserialize_with = "string_lossy")]
    pub from_id: String,
    #[serde(rename = "senderUserName", default, deserialize_with = "string_lossy")]
    pub sender_user_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(
        rename = "receiverUserName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub receiver_user_name: String,
}

/// 系统通知原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireChatNotice {
    #[serde(default, deserialize_with = "string_lossy")]
    pub command: String,
    #[serde(rename = "userId", default, deserialize_with = "string_lossy")]
    pub user_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub preview: String,
    #[serde(rename = "senderAvatar", default, deserialize_with = "string_lossy")]
    pub sender_avatar: String,
    #[serde(rename = "senderUserName", default, deserialize_with = "string_lossy")]
    pub sender_user_name: String,
}
