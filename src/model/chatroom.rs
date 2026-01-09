use crate::impl_str_enum;
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
pub struct ChatRoomMsg<T = Value> {
    pub r#type: ChatRoomMessageType,
    pub oId: String,
    pub time: String,
    pub userOId: String,
    pub userName: String,
    pub userNickname: String,
    pub userAvatarURL: String,
    pub sysMetal: Vec<Metal>,
    pub content: T,
    pub md: String,
    pub client: String,
    pub via: ClientType,
}

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct BarragerMsg {
    /// 用户名
    pub userName: String,
    /// 用户昵称
    pub userNickname: String,
    /// 弹幕消息
    pub barragerContent: String,
    /// 弹幕颜色
    pub barragerColor: String,
    /// 用户头像地址
    pub userAvatarURL: String,
    /// 头像地址20x20
    pub userAvatarURL200: String,
    /// 头像地址48x48
    pub userAvatarURL48: String,
    /// 头像地址100x100
    pub userAvatarURL210: String,
}

/// 在线用户信息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct OnlineInfo {
    /// 用户首页
    pub homePage: String,
    /// 用户头像
    pub userAvatarURL: String,
    /// 用户名
    pub userName: String,
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
        if self.userNickname.is_empty() {
            &self.userName
        } else {
            &self.userNickname
        }
    }
}

impl BarragerMsg {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse BarragerMsg: {}", e)))
    }
}

fn parse_content(content: &str) -> (ChatRoomMessageType, Value) {
    if let Ok(data) = serde_json::from_str::<Value>(content) {
        if let Some(msg_type_str) = data["msgType"].as_str() {
            match msg_type_str {
                "redPacket" => (ChatRoomMessageType::RedPacket, data),
                "music" => (ChatRoomMessageType::Msg, data),
                "weather" => (ChatRoomMessageType::Msg, data),
                _ => (ChatRoomMessageType::Msg, Value::String(content.to_string())),
            }
        } else {
            (ChatRoomMessageType::Msg, Value::String(content.to_string()))
        }
    } else {
        (ChatRoomMessageType::Msg, Value::String(content.to_string()))
    }
}

impl<'de> Deserialize<'de> for ChatRoomMsg<Value> {
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
            content: String,
            md: Option<String>,
            client: Option<String>,
        }

        let raw = Raw::deserialize(deserializer)?;

        let (r#type, content) = parse_content(&raw.content);

        let via = raw
            .client
            .as_ref()
            .and_then(|s| ClientType::from_str(s).ok())
            .unwrap_or(ClientType::Other);
        let client = raw.client.unwrap_or(ClientType::Rust.as_str().to_string());

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
            userOId: raw.userOId.to_string(),
            userName: raw.userName,
            userNickname: raw.userNickname,
            userAvatarURL: raw.userAvatarURL,
            sysMetal: sys_metal,
            content,
            md: raw.md.unwrap_or("".to_string()),
            client,
            via,
        })
    }
}

impl BarragerCost {
    pub fn from_value(value: &Value) -> Self {
        let content = value
            .get("data")
            .and_then(|v| v.as_str())
            .unwrap_or("5积分");
        let parts: Vec<&str> = content
            .split(|c: char| !c.is_alphanumeric())
            .filter(|s| !s.is_empty())
            .collect();

        Self {
            cost: parts[0].parse::<u32>().unwrap_or(0),
            unit: parts[1].to_string(),
        }
    }
}
