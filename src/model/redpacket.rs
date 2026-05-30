use std::str::FromStr;

use serde::{Deserialize, Deserializer, Serialize, Serializer};
use serde_json::Value;

use crate::{impl_str_enum, utils::error::Error};

/// 猜拳类型
#[derive(Clone, Copy, Debug)]
#[repr(u8)]
pub enum GestureType {
    /// 石
    Rock = 0,
    /// 剪刀
    Scissors = 1,
    /// 布
    Paper = 2,
}

/// 红包类型
#[derive(Clone, Debug)]
pub enum RedPacketType {
    /// 拼手气
    Random,
    /// 平分
    Average,
    /// 专属
    Specify,
    /// 心跳
    Heartbeat,
    /// 猜拳
    RockPaperScissors,
}

/// 红包数据
#[derive(Clone, Debug, Serialize)]
pub struct RedPacket {
    /// 红包类型
    #[serde(rename = "type", serialize_with = "serialize_redpacket_type")]
    pub r#type: RedPacketType,
    /// 红包积分
    pub money: u32,
    /// 红包个数
    pub count: u32,
    /// 祝福语
    #[serde(rename = "msg")]
    pub message: String,
    /// 接收者, 专属红包有效
    #[serde(rename = "recivers")]
    pub receivers: Vec<String>,
    /// 出拳, 猜拳红包有效
    #[serde(serialize_with = "serialize_gesture")]
    pub gesture: Option<GestureType>,
}

/// 红包领取者信息
#[derive(Clone, Debug, Deserialize)]
pub struct RedPacketGot {
    /// 用户ID
    #[serde(rename = "userId")]
    pub user_id: String,
    /// 用户名
    #[serde(rename = "userName")]
    pub user_name: String,
    /// 用户头像
    pub avatar: String,
    /// 领取到的积分
    #[serde(
        rename = "userMoney",
        alias = "money",
        default,
        deserialize_with = "deserialize_u32"
    )]
    pub user_money: u32,
    /// 领取时间
    pub time: String,
}

/// 红包历史信息
#[derive(Clone, Debug, Deserialize)]
pub struct RedPacketMessage {
    /// 消息类型，固定为redPacket
    #[serde(rename = "msgType")]
    pub msg_type: String,
    /// 红包数
    #[serde(deserialize_with = "deserialize_u32")]
    pub count: u32,
    /// 领取数
    #[serde(deserialize_with = "deserialize_u32")]
    pub got: u32,
    /// 内含积分
    #[serde(deserialize_with = "deserialize_u32")]
    pub money: u32,
    /// 祝福语
    #[serde(rename = "msg")]
    pub message: String,
    /// 发送者ID
    #[serde(rename = "senderId")]
    pub sender_id: String,
    /// 出拳，猜拳红包有效
    #[serde(
        rename = "GestureType",
        alias = "gesture",
        default,
        deserialize_with = "deserialize_optional_gesture"
    )]
    pub gesture: Option<GestureType>,
    /// 接收者，专属红包有效
    #[serde(
        rename = "recivers",
        alias = "receivers",
        default,
        deserialize_with = "deserialize_string_vec"
    )]
    pub receivers: Vec<String>,
    /// 已领取者列表
    #[serde(default, deserialize_with = "deserialize_got_vec")]
    pub who: Vec<RedPacketGot>,
}

/// 红包基本信息
#[derive(Clone, Debug, Deserialize)]
pub struct RedPacketBase {
    /// 数量
    #[serde(deserialize_with = "deserialize_u32")]
    pub count: u32,
    /// 猜拳类型
    #[serde(default, deserialize_with = "deserialize_optional_gesture")]
    pub gesture: Option<GestureType>,
    /// 领取数
    #[serde(deserialize_with = "deserialize_u32")]
    pub got: u32,
    /// 祝福语
    #[serde(rename = "msg")]
    pub message: String,
    /// 发送者用户名
    #[serde(rename = "userName")]
    pub user_name: String,
    /// 用户头像
    #[serde(rename = "userAvatarURL")]
    pub user_avatar_url: String,
}

/// 红包信息
#[derive(Clone, Debug, Deserialize)]
pub struct RedPacketInfo {
    pub info: RedPacketBase,
    #[serde(
        rename = "recivers",
        alias = "receivers",
        default,
        deserialize_with = "deserialize_string_vec"
    )]
    pub receivers: Vec<String>,
    #[serde(default, deserialize_with = "deserialize_got_vec")]
    pub who: Vec<RedPacketGot>,
}

fn parse_string_list(data: &Value, primary_key: &str, fallback_key: &str) -> Vec<String> {
    data.get(primary_key)
        .or_else(|| data.get(fallback_key))
        .map(value_to_string_vec)
        .unwrap_or_default()
}

fn serialize_redpacket_type<S>(value: &RedPacketType, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    serializer.serialize_str(value.as_str())
}

fn serialize_gesture<S>(value: &Option<GestureType>, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    match value {
        Some(gesture) => serializer.serialize_some(&(*gesture as u8)),
        None => serializer.serialize_none(),
    }
}

fn deserialize_u32<'de, D>(deserializer: D) -> Result<u32, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    value_to_u32(&value).ok_or_else(|| serde::de::Error::custom("Expected u32 or numeric string"))
}

fn value_to_u32(value: &Value) -> Option<u32> {
    value
        .as_u64()
        .or_else(|| {
            value
                .as_i64()
                .and_then(|n| if n >= 0 { Some(n as u64) } else { None })
        })
        .or_else(|| value.as_str().and_then(|s| s.trim().parse::<u64>().ok()))
        .and_then(|n| u32::try_from(n).ok())
}

fn deserialize_optional_gesture<'de, D>(deserializer: D) -> Result<Option<GestureType>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<Value>::deserialize(deserializer)?;
    let Some(value) = value else {
        return Ok(None);
    };

    let parsed = if let Some(index) = value.as_u64() {
        match index {
            0 => Some(GestureType::Rock),
            1 => Some(GestureType::Scissors),
            2 => Some(GestureType::Paper),
            _ => {
                return Err(serde::de::Error::custom("Invalid gesture index"));
            }
        }
    } else if let Some(text) = value.as_str() {
        match text {
            "0" => Some(GestureType::Rock),
            "1" => Some(GestureType::Scissors),
            "2" => Some(GestureType::Paper),
            _ => Some(
                GestureType::from_str(text)
                    .map_err(|_| serde::de::Error::custom("Invalid gesture"))?,
            ),
        }
    } else {
        return Err(serde::de::Error::custom("Invalid gesture value"));
    };

    Ok(parsed)
}

fn parse_gesture(
    data: &Value,
    primary_key: &str,
    fallback_key: &str,
    err_ctx: &str,
) -> Result<Option<GestureType>, Error> {
    let gesture = data.get(primary_key).or_else(|| data.get(fallback_key));

    if let Some(gesture_index) = gesture.and_then(|v| v.as_u64()) {
        return match gesture_index {
            0 => Ok(Some(GestureType::Rock)),
            1 => Ok(Some(GestureType::Scissors)),
            2 => Ok(Some(GestureType::Paper)),
            _ => Err(Error::Parse(format!("Invalid gesture in {}", err_ctx))),
        };
    }

    let gesture = gesture.and_then(|v| v.as_str());

    match gesture {
        Some("0") => Ok(Some(GestureType::Rock)),
        Some("1") => Ok(Some(GestureType::Scissors)),
        Some("2") => Ok(Some(GestureType::Paper)),
        Some(gesture_str) => GestureType::from_str(gesture_str)
            .map(Some)
            .map_err(|_| Error::Parse(format!("Invalid gesture in {}", err_ctx))),
        None => Ok(None),
    }
}

fn value_from_maybe_json_string(value: Value) -> Value {
    match value {
        Value::String(text) => {
            let trimmed = text.trim();
            if trimmed.starts_with('[') || trimmed.starts_with('{') {
                serde_json::from_str(trimmed).unwrap_or(Value::String(text))
            } else {
                Value::String(text)
            }
        }
        value => value,
    }
}

fn value_to_string_vec(value: &Value) -> Vec<String> {
    let normalized = value_from_maybe_json_string(value.clone());
    match normalized {
        Value::Array(items) => items
            .into_iter()
            .filter_map(|item| item.as_str().map(ToString::to_string))
            .collect(),
        Value::String(item) if item.trim().is_empty() => Vec::new(),
        Value::String(item) => vec![item],
        Value::Null => Vec::new(),
        _ => Vec::new(),
    }
}

/// 红包状态信息
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct RedPacketStatusMsg {
    #[serde(rename = "oId")]
    pub oId: String,
    #[serde(deserialize_with = "deserialize_u32")]
    pub count: u32,
    #[serde(deserialize_with = "deserialize_u32")]
    pub got: u32,
    #[serde(rename = "whoGive")]
    pub who_give: String,
    #[serde(
        rename = "whoGot",
        default,
        deserialize_with = "deserialize_string_vec"
    )]
    pub who_got: Vec<String>,
    #[serde(rename = "userAvatarURL20")]
    pub user_avatar_url20: String,
    #[serde(rename = "userAvatarURL48")]
    pub user_avatar_url48: String,
    #[serde(rename = "userAvatarURL210")]
    pub user_avatar_url210: String,
}

impl RedPacketStatusMsg {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse RedPacketStatusMsg: {}", e)))
    }
}

fn deserialize_string_vec<'de, D>(deserializer: D) -> Result<Vec<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<Value>::deserialize(deserializer)?;
    Ok(value.map(|v| value_to_string_vec(&v)).unwrap_or_default())
}

fn deserialize_got_vec<'de, D>(deserializer: D) -> Result<Vec<RedPacketGot>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<Value>::deserialize(deserializer)?;
    let Some(value) = value else {
        return Ok(Vec::new());
    };

    match value_from_maybe_json_string(value) {
        Value::Array(items) => items
            .into_iter()
            .map(serde_json::from_value)
            .collect::<Result<Vec<_>, _>>()
            .map_err(serde::de::Error::custom),
        Value::Null => Ok(Vec::new()),
        _ => Err(serde::de::Error::custom("Expected red packet user list")),
    }
}

impl Default for RedPacket {
    fn default() -> Self {
        RedPacket {
            r#type: RedPacketType::Random,
            money: 32,
            count: 1,
            message: "摸鱼者, 事竟成!".to_string(),
            receivers: Vec::new(),
            gesture: None,
        }
    }
}

impl RedPacket {
    pub fn to_value(&self) -> Value {
        serde_json::to_value(self).expect("RedPacket serialization should not fail")
    }

    pub fn from_value(data: &Value) -> Result<Self, Error> {
        Ok(RedPacket {
            r#type: RedPacketType::from_str(
                data["type"]
                    .as_str()
                    .ok_or_else(|| Error::Parse("Missing type in RedPacket".to_string()))?,
            )
            .map_err(|_| Error::Parse("Invalid type in RedPacket".to_string()))?,
            money: data["money"]
                .as_u64()
                .ok_or_else(|| Error::Parse("Missing or invalid money in RedPacket".to_string()))?
                as u32,
            count: data["count"]
                .as_u64()
                .ok_or_else(|| Error::Parse("Missing or invalid count in RedPacket".to_string()))?
                as u32,
            message: data["msg"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing msg in RedPacket".to_string()))?
                .to_string(),
            receivers: parse_string_list(data, "recivers", "receivers"),
            gesture: parse_gesture(data, "gesture", "GestureType", "RedPacket")?,
        })
    }
}

impl From<&RedPacket> for RedPacket {
    fn from(value: &RedPacket) -> Self {
        value.clone()
    }
}

impl RedPacketMessage {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse RedPacketMessage: {}", e)))
    }
}

impl RedPacketBase {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse RedPacketBase: {}", e)))
    }
}

impl RedPacketInfo {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse RedPacketInfo: {}", e)))
    }
}

impl_str_enum!(GestureType {
    Rock => "石头",
    Scissors => "剪刀",
    Paper => "布",
});

impl_str_enum!(RedPacketType {
    Random => "random",
    Average => "average",
    Specify => "specify",
    Heartbeat => "heartbeat",
    RockPaperScissors => "rockPaperScissors",
});
