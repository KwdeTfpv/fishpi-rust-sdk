use crate::impl_str_enum;
use crate::model::reaction::ReactionSummaryItem;
use crate::model::user::{Metal, to_metal};
use crate::utils::error::Error;
use serde::{Deserialize, Deserializer};
use serde_json::Value;
use std::str::FromStr;

#[derive(Clone, Debug)]
pub enum ClientType {
    /// 网页端
    Web,
    /// PC 端
    PC,
    /// 移动端聊天室
    Mobile,
    /// Windows 客户端
    Windows,
    /// macOS 客户端
    MacOs,
    /// Linux 客户端
    Linux,
    /// iOS 客户端
    Ios,
    /// Android 客户端
    Android,
    /// IDEA 插件
    Idea,
    /// Chrome 插件
    Chrome,
    /// Edge 插件
    Edge,
    /// VSCode 插件
    VSCode,
    /// Python 客户端
    Python,
    /// Golang 客户端
    Golang,
    /// Rust 客户端
    Rust,
    /// Harmony App
    Harmony,
    /// CLI 工具
    Cli,
    /// 鸽机器人
    Bird,
    /// 小冰机器人
    IceNet,
    /// 凌机器人
    ElvesOnline,
    /// 其他插件
    Other,
}

pub enum ChatContentType {
    Markdown,
    Html,
}

/// chatroom get 接口获取 oId 的相关消息类型
#[repr(u8)]
pub enum ChatRoomMessageMode {
    ///前后消息
    Context = 0,
    /// 前面的消息
    Before = 1,
    /// 后面的消息
    After = 2,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ChatRoomMessageType {
    /// 在线用户
    Online,
    /// 话题修改
    DiscussChanged,
    /// 消息撤回
    Revoke,
    /// 消息
    Msg,
    /// 红包
    RedPacket,
    /// 红包状态
    RedPacketStatus,
    /// 弹幕
    Barrager,
    /// 进出场消息
    Custom,
    /// 聊天室表态/反应
    ChatReaction,
}

#[derive(Clone, Debug)]
pub struct CustomMsg {
    pub message: String,
}
pub struct DiscussMsg;

#[derive(Clone, Debug)]
pub struct RevokeMsg {
    pub msg: String,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[allow(non_snake_case)]
pub struct ChatReactionMsg {
    /// 目标聊天室消息 ID。
    #[serde(default)]
    pub oId: String,
    /// 目标类型。
    #[serde(default, rename = "targetType")]
    pub target_type: String,
    /// Reaction 分组。
    #[serde(default, rename = "groupType")]
    pub group_type: String,
    /// 该聊天消息最新的表情汇总。
    #[serde(default)]
    pub summary: Vec<ReactionSummaryItem>,
    /// 本次触发操作的用户 id。
    #[serde(default, rename = "actorUserId")]
    pub actor_user_id: String,
    /// 该用户本次操作后最终选中的表情值。
    #[serde(default, rename = "actorReaction")]
    pub actor_reaction: String,
    /// 保留原始数据，方便服务端字段扩展时客户端不丢信息。
    #[serde(skip)]
    pub raw: Value,
}

impl ChatReactionMsg {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        let mut msg: Self = serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ChatReactionMsg: {}", e)))?;
        msg.raw = value.clone();
        Ok(msg)
    }
}

#[derive(Clone, Debug)]
pub struct BarragerCost {
    pub cost: u32,
    pub unit: String,
}

// /// 聊天天气消息
// pub struct WeatherMessage {
//     city: String,
//     description: String,
//     data: WeatherData,
// }

/// 聊天天气消息详情
pub struct WeatherData {
    pub date: String,
    pub code: WeatherCode,
    pub min: String,
    pub max: String,
}

/// 消息来源
pub struct ChatRoomSource {
    pub client: String,
    pub version: String,
}

#[derive(Clone, Debug)]
pub enum WeatherCode {
    ClearDay,
    ClearNight,
    Cloudy,
    Dust,
    Fog,
    HeavyHaze,
    HeavyRain,
    HeavySnow,
    LightHaze,
    LightRain,
    LightSnow,
    ModerateHaze,
    ModerateRain,
    ModerateSnow,
    PartlyCloudyDay,
    PartlyCloudyNight,
    Sand,
    StormRain,
    StormSnow,
    Wind,
}

/// 聊天消息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct ChatRoomMsg {
    pub r#type: ChatRoomMessageType,
    pub oId: String,
    pub time: String,
    pub user_o_id: String,
    pub user_name: String,
    pub user_nickname: String,
    pub user_avatar_url: String,
    pub sys_metal: Vec<Metal>,
    pub content: Value,
    pub md: String,
    pub client: String,
    pub via: ClientType,
    pub reaction_summary: Vec<ReactionSummaryItem>,
    pub current_user_reaction: String,
}

/// 聊天室音乐消息
#[derive(Clone, Debug)]
pub struct MusicMsg {
    pub base: ChatRoomMsg,
    pub cover_url: String,
    pub source: String,
    pub title: String,
    pub from: String,
}

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct BarragerMsg {
    /// 用户名
    #[serde(rename = "userName")]
    pub user_name: String,
    /// 用户昵称
    #[serde(rename = "userNickname")]
    pub user_nickname: String,
    /// 弹幕消息
    #[serde(rename = "barragerContent")]
    pub barrager_content: String,
    /// 弹幕颜色
    #[serde(rename = "barragerColor")]
    pub barrager_color: String,
    /// 用户头像地址
    #[serde(rename = "userAvatarURL")]
    pub user_avatar_url: String,
    /// 头像地址20x20
    #[serde(rename = "userAvatarURL20")]
    pub user_avatar_url20: String,
    /// 头像地址48x48
    #[serde(rename = "userAvatarURL48")]
    pub user_avatar_url48: String,
    /// 头像地址100x100
    #[serde(rename = "userAvatarURL210")]
    pub user_avatar_url210: String,
}

/// 在线用户信息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct OnlineInfo {
    /// 用户首页
    pub home_page: String,
    /// 用户头像
    pub user_avatar_url: String,
    /// 用户名
    pub user_name: String,
}

impl_str_enum!(ClientType {
    Web => "Web",
    PC => "PC",
    Mobile => "Mobile",
    Windows => "Windows",
    MacOs => "macOS",
    Linux => "Linux",
    Ios => "iOS",
    Android => "Android",
    Idea => "IDEA",
    Chrome => "Chrome",
    Edge => "Edge",
    VSCode => "VSCode",
    Python => "Python",
    Golang => "Golang",
    Rust => "Rust",
    Harmony => "Harmony",
    Cli => "CLI",
    Bird => "Bird",
    IceNet => "IceNet",
    ElvesOnline => "ElvesOnline",
    Other => "Other",
});

impl_str_enum!(ChatContentType {
    Markdown => "Markdown",
    Html => "Html",
});

impl_str_enum!(ChatRoomMessageType {
    Online => "online",
    DiscussChanged => "discussChanged",
    Revoke => "revoke",
    Msg => "msg",
    RedPacket => "redPacket",
    RedPacketStatus => "redPacketStatus",
    Barrager => "barrager",
    Custom => "customMessage",
    ChatReaction => "chatreaction",
});

impl_str_enum!(WeatherCode {
    ClearDay => "CLEAR_DAY",
    ClearNight => "CLEAR_NIGHT",
    Cloudy => "CLOUDY",
    Dust => "DUST",
    Fog => "FOG",
    HeavyHaze => "HEAVY_HAZE",
    HeavyRain => "HEAVY_RAIN",
    HeavySnow => "HEAVY_SNOW",
    LightHaze => "LIGHT_HAZE",
    LightRain => "LIGHT_RAIN",
    LightSnow => "LIGHT_SNOW",
    ModerateHaze => "MODERATE_HAZE",
    ModerateRain => "MODERATE_RAIN",
    ModerateSnow => "MODERATE_SNOW",
    PartlyCloudyDay => "PARTLY_CLOUDY_DAY",
    PartlyCloudyNight => "PARTLY_CLOUDY_NIGHT",
    Sand => "SAND",
    StormRain => "STORM_RAIN",
    StormSnow => "STORM_SNOW",
    Wind => "WIND",
});

impl_str_enum!(ChatRoomMessageMode{
    Context => "0",
    Before => "1",
    After => "2",
});

impl Default for ChatRoomSource {
    fn default() -> Self {
        Self {
            client: "Other".to_string(),
            version: "latest".to_string(),
        }
    }
}

impl ChatRoomMsg {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ChatRoomMsg: {}", e)))
    }

    pub fn name(&self) -> &str {
        if self.user_nickname.is_empty() {
            &self.user_name
        } else {
            &self.user_nickname
        }
    }
}

impl MusicMsg {
    pub fn from_chatroom_msg(base: ChatRoomMsg) -> Result<Self, Error> {
        let payload = base
            .content
            .as_object()
            .ok_or_else(|| Error::Parse("Missing music payload".to_string()))?;

        Ok(Self {
            cover_url: payload
                .get("coverURL")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            source: payload
                .get("source")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            title: payload
                .get("title")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            from: payload
                .get("from")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            base,
        })
    }
}

impl BarragerMsg {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse BarragerMsg: {}", e)))
    }
}

fn parse_message_payload(value: &Value) -> Option<Value> {
    match value {
        Value::Object(_) => Some(value.clone()),
        Value::String(content) => serde_json::from_str::<Value>(content)
            .ok()
            .filter(|v| v.is_object()),
        _ => None,
    }
}

fn message_payload_type(value: &Value) -> Option<&str> {
    value
        .get("msgType")
        .or_else(|| value.get("type"))
        .and_then(|v| v.as_str())
}

fn message_payload_type_is(value: &Value, expected: &str) -> bool {
    message_payload_type(value).is_some_and(|actual| actual.eq_ignore_ascii_case(expected))
}

fn parse_chatroom_payload(content: &Value, md: Option<&Value>) -> (ChatRoomMessageType, Value) {
    if let Some(data) = parse_message_payload(content) {
        if message_payload_type_is(&data, "redPacket") {
            return (ChatRoomMessageType::RedPacket, data);
        }
        if message_payload_type_is(&data, "music") || message_payload_type_is(&data, "weather") {
            return (ChatRoomMessageType::Msg, data);
        }
    }

    if let Some(data) = md.and_then(parse_message_payload)
        && (message_payload_type_is(&data, "music") || message_payload_type_is(&data, "weather"))
    {
        return (ChatRoomMessageType::Msg, data);
    }

    match content {
        Value::String(content) => (ChatRoomMessageType::Msg, Value::String(content.clone())),
        _ => (ChatRoomMessageType::Msg, content.clone()),
    }
}

impl<'de> Deserialize<'de> for ChatRoomMsg {
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
            sysMetal: Option<Value>,
            content: Value,
            md: Option<Value>,
            client: Option<String>,
            #[serde(default)]
            reactionSummary: Option<Vec<ReactionSummaryItem>>,
            #[serde(default)]
            currentUserReaction: Option<String>,
        }

        let raw = Raw::deserialize(deserializer)?;

        let (r#type, content) = parse_chatroom_payload(&raw.content, raw.md.as_ref());

        let client = raw.client.unwrap_or_default();
        let client = if client.trim().is_empty() {
            ClientType::Other.as_str().to_string()
        } else {
            client
        };
        let via = ClientType::from_str(&client).unwrap_or(ClientType::Other);

        let sys_metal = raw
            .sysMetal
            .as_ref()
            .and_then(|v| v.as_str())
            .map(|s| to_metal(s))
            .unwrap_or(Ok(vec![]))
            .unwrap_or(vec![]);

        Ok(ChatRoomMsg {
            r#type,
            oId: raw.oId,
            time: raw.time,
            user_o_id: raw.userOId.to_string(),
            user_name: raw.userName,
            user_nickname: raw.userNickname,
            user_avatar_url: raw.userAvatarURL,
            sys_metal,
            content,
            md: raw
                .md
                .map(|value| match value {
                    Value::String(text) => text,
                    other => other.to_string(),
                })
                .unwrap_or_default(),
            client,
            via,
            reaction_summary: raw.reactionSummary.unwrap_or_default(),
            current_user_reaction: raw.currentUserReaction.unwrap_or_default(),
        })
    }
}

impl BarragerCost {
    pub fn from_value(value: &Value) -> Self {
        let content = value.as_str().unwrap_or("5积分").trim();
        let cost = content.trim_end_matches("积分").parse::<u32>().unwrap_or(0);
        Self {
            cost,
            unit: "积分".to_string(),
        }
    }
}
