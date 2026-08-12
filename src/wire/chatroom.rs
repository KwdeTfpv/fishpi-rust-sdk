use crate::wire::de::string_lossy;
use crate::wire::reaction::WireReactionSummaryItem;
use crate::wire::user::{WireMetal, metals};
use serde::{Deserialize, Deserializer};
use serde_json::Value;
use std::str::FromStr;

/// 禁言成员原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireMuteItem {
    #[serde(default)]
    pub time: u64,
    #[serde(rename = "userAvatarURL", default, deserialize_with = "string_lossy")]
    pub user_avatar_url: String,
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(rename = "userNickname", default, deserialize_with = "string_lossy")]
    pub user_nickname: String,
}

/// 在线用户原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireOnlineInfo {
    #[serde(rename = "homePage", default, deserialize_with = "string_lossy")]
    pub home_page: String,
    #[serde(rename = "userAvatarURL", default, deserialize_with = "string_lossy")]
    pub user_avatar_url: String,
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
}

/// 弹幕消息原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireBarragerMsg {
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(rename = "userNickname", default, deserialize_with = "string_lossy")]
    pub user_nickname: String,
    #[serde(rename = "barragerContent", default, deserialize_with = "string_lossy")]
    pub barrager_content: String,
    #[serde(rename = "barragerColor", default, deserialize_with = "string_lossy")]
    pub barrager_color: String,
    #[serde(rename = "userAvatarURL", default, deserialize_with = "string_lossy")]
    pub user_avatar_url: String,
    #[serde(rename = "userAvatarURL20", default, deserialize_with = "string_lossy")]
    pub user_avatar_url20: String,
    #[serde(rename = "userAvatarURL48", default, deserialize_with = "string_lossy")]
    pub user_avatar_url48: String,
    #[serde(
        rename = "userAvatarURL210",
        default,
        deserialize_with = "string_lossy"
    )]
    pub user_avatar_url210: String,
}

/// 聊天室表态消息原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireChatReactionMsg {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "targetType", default, deserialize_with = "string_lossy")]
    pub target_type: String,
    #[serde(rename = "groupType", default, deserialize_with = "string_lossy")]
    pub group_type: String,
    #[serde(default)]
    pub summary: Vec<WireReactionSummaryItem>,
    #[serde(rename = "actorUserId", default, deserialize_with = "string_lossy")]
    pub actor_user_id: String,
    #[serde(rename = "actorReaction", default, deserialize_with = "string_lossy")]
    pub actor_reaction: String,
}

/// 聊天室节点响应原始形状。
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct WireChatRoomNode {
    #[serde(default, deserialize_with = "string_lossy")]
    pub msg: String,
    #[serde(default)]
    pub data: String,
    #[serde(rename = "apiKey", default, deserialize_with = "string_lossy")]
    pub apiKey: String,
    #[serde(default)]
    pub avaliable: Vec<WireChatRoomAvailableNode>,
}

/// 可用节点原始形状。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireChatRoomAvailableNode {
    #[serde(default, deserialize_with = "string_lossy")]
    pub node: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub name: String,
    #[serde(default)]
    pub weight: u32,
    #[serde(default)]
    pub online: u32,
}

/// 聊天室消息原始形状。`content` 已归一化为对象或原文字符串。
#[derive(Clone, Debug)]
pub struct WireChatRoomMsg {
    pub o_id: String,
    pub time: String,
    pub user_o_id: String,
    pub user_name: String,
    pub user_nickname: String,
    pub user_avatar_url: String,
    pub sys_metal: Vec<WireMetal>,
    pub content: Value,
    pub md: String,
    pub client: String,
    pub reaction_summary: Vec<WireReactionSummaryItem>,
    pub current_user_reaction: String,
}

fn payload_object(value: &Value) -> Option<Value> {
    match value {
        Value::Object(_) => Some(value.clone()),
        Value::String(s) => serde_json::from_str::<Value>(s)
            .ok()
            .filter(Value::is_object),
        _ => None,
    }
}

/// content 内嵌类型标记（`msgType`/`type`）。
pub fn payload_type(value: &Value) -> Option<String> {
    payload_object(value)
        .as_ref()
        .and_then(|v| v.get("msgType").or_else(|| v.get("type")))
        .and_then(Value::as_str)
        .map(str::to_string)
}

impl<'de> Deserialize<'de> for WireChatRoomMsg {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        #[derive(Deserialize)]
        #[allow(non_snake_case)]
        struct Raw {
            oId: String,
            time: String,
            userOId: Value,
            userName: String,
            userNickname: String,
            userAvatarURL: String,
            #[serde(default, deserialize_with = "metals")]
            sysMetal: Vec<WireMetal>,
            content: Value,
            md: Option<Value>,
            client: Option<String>,
            #[serde(default)]
            reactionSummary: Option<Vec<WireReactionSummaryItem>>,
            #[serde(default)]
            currentUserReaction: Option<String>,
        }

        let raw = Raw::deserialize(deserializer)?;
        let content = payload_object(&raw.content)
            .or_else(|| raw.md.as_ref().and_then(payload_object))
            .unwrap_or(raw.content);
        let md = raw
            .md
            .map(|v| match v {
                Value::String(s) => s,
                other => other.to_string(),
            })
            .unwrap_or_default();

        Ok(WireChatRoomMsg {
            o_id: raw.oId,
            time: raw.time,
            user_o_id: raw.userOId.to_string(),
            user_name: raw.userName,
            user_nickname: raw.userNickname,
            user_avatar_url: raw.userAvatarURL,
            sys_metal: raw.sysMetal,
            content,
            md,
            client: raw.client.unwrap_or_default(),
            reaction_summary: raw.reactionSummary.unwrap_or_default(),
            current_user_reaction: raw.currentUserReaction.unwrap_or_default(),
        })
    }
}

impl FromStr for WireChatRoomMsg {
    type Err = serde_json::Error;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        serde_json::from_str(s)
    }
}
