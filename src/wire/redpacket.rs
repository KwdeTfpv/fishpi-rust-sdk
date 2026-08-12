use crate::wire::de::string_lossy;
use serde::{Deserialize, Deserializer};
use serde_json::Value;

#[derive(Clone, Debug, Deserialize)]
pub struct WireRedPacketInfo {
    pub info: WireRedPacketBase,
    #[serde(
        rename = "recivers",
        alias = "receivers",
        default,
        deserialize_with = "deserialize_string_vec"
    )]
    pub receivers: Vec<String>,
    #[serde(default, deserialize_with = "deserialize_got_vec")]
    pub who: Vec<WireRedPacketGot>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireRedPacketBase {
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub count: u32,
    #[serde(default, deserialize_with = "deserialize_optional_gesture")]
    pub gesture: Option<u8>,
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub got: u32,
    #[serde(rename = "msg", default, deserialize_with = "string_lossy")]
    pub message: String,
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(rename = "userAvatarURL", default, deserialize_with = "string_lossy")]
    pub user_avatar_url: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireRedPacketGot {
    #[serde(rename = "userId", default, deserialize_with = "string_lossy")]
    pub user_id: String,
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub avatar: String,
    #[serde(
        rename = "userMoney",
        alias = "money",
        default,
        deserialize_with = "deserialize_i32"
    )]
    pub user_money: i32,
    #[serde(default, deserialize_with = "string_lossy")]
    pub time: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireRedPacketMessage {
    #[serde(rename = "msgType", default, deserialize_with = "string_lossy")]
    pub msg_type: String,
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub count: u32,
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub got: u32,
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub money: u32,
    #[serde(rename = "msg", default, deserialize_with = "string_lossy")]
    pub message: String,
    #[serde(rename = "senderId", default, deserialize_with = "string_lossy")]
    pub sender_id: String,
    #[serde(
        rename = "GestureType",
        alias = "gesture",
        default,
        deserialize_with = "deserialize_optional_gesture"
    )]
    pub gesture: Option<u8>,
    #[serde(
        rename = "recivers",
        alias = "receivers",
        default,
        deserialize_with = "deserialize_string_vec"
    )]
    pub receivers: Vec<String>,
    #[serde(default, deserialize_with = "deserialize_got_vec")]
    pub who: Vec<WireRedPacketGot>,
}

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct WireRedPacketStatusMsg {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub oId: String,
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub count: u32,
    #[serde(default, deserialize_with = "deserialize_u32")]
    pub got: u32,
    #[serde(rename = "whoGive", default, deserialize_with = "string_lossy")]
    pub who_give: String,
    #[serde(
        rename = "whoGot",
        default,
        deserialize_with = "deserialize_string_vec"
    )]
    pub who_got: Vec<String>,
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

fn deserialize_u32<'de, D>(deserializer: D) -> Result<u32, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value_to_u32(&value).unwrap_or(0))
}

fn deserialize_i32<'de, D>(deserializer: D) -> Result<i32, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value_to_i32(&value).unwrap_or(0))
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

fn value_to_i32(value: &Value) -> Option<i32> {
    value
        .as_i64()
        .or_else(|| value.as_str().and_then(|s| s.trim().parse::<i64>().ok()))
        .and_then(|n| i32::try_from(n).ok())
}

fn deserialize_optional_gesture<'de, D>(deserializer: D) -> Result<Option<u8>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<Value>::deserialize(deserializer)?;
    let Some(value) = value else {
        return Ok(None);
    };

    let parsed = if let Some(index) = value.as_u64() {
        match index {
            0..=2 => Some(index as u8),
            _ => None,
        }
    } else if let Some(text) = value.as_str() {
        match text {
            "0" => Some(0),
            "1" => Some(1),
            "2" => Some(2),
            _ => None,
        }
    } else {
        None
    };

    Ok(parsed)
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

fn deserialize_string_vec<'de, D>(deserializer: D) -> Result<Vec<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<Value>::deserialize(deserializer)?;
    Ok(value.map(|v| value_to_string_vec(&v)).unwrap_or_default())
}

fn deserialize_got_vec<'de, D>(deserializer: D) -> Result<Vec<WireRedPacketGot>, D::Error>
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
        _ => Ok(Vec::new()),
    }
}
