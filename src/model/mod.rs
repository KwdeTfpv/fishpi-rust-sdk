pub mod article;
pub mod breezemoon;
pub mod chat;
pub mod chatroom;
pub mod emoji;
pub mod finger;
pub mod misc;
pub mod notice;
pub mod reaction;
pub mod redpacket;
pub mod user;

use crate::{
    model::user::{Metal, to_metal},
    utils::error::Error,
};
use serde::{Deserialize, Deserializer};

#[derive(Clone, Debug, Default, Deserialize)]
#[allow(non_snake_case)]
pub struct MuteItem {
    /// 解除禁言时间戳
    pub time: u64,
    /// 用户头像
    pub userAvatarURL: String,
    /// 用户名
    pub userName: String,
    /// 用户昵称
    pub userNickname: String,
}

impl MuteItem {
    pub fn from_value(data: &serde_json::Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse MuteItem: {}", e)))
    }
}

#[macro_export]
macro_rules! impl_str_enum {
    ($enum_name:ident { $($variant:ident => $str:expr),* $(,)? }) => {
        impl std::fmt::Display for $enum_name {
            fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                write!(f, "{}", self.as_str())
            }
        }

        impl std::str::FromStr for $enum_name {
            type Err = String;

            fn from_str(s: &str) -> Result<Self, Self::Err> {
                let candidate = s.split('/').next().unwrap_or(s);
                $(
                    if candidate == $str || candidate.eq_ignore_ascii_case($str) {
                        return Ok($enum_name::$variant);
                    }
                )*
                Err(format!("Unknown {}: {}", stringify!($enum_name), s))
            }
        }

        impl $enum_name {
            pub fn as_str(&self) -> &str {
                match self {
                    $($enum_name::$variant => $str,)*
                }
            }
        }
    };
}

pub fn bool_from_int<'de, D>(deserializer: D) -> Result<bool, D::Error>
where
    D: Deserializer<'de>,
{
    let value: i64 = Deserialize::deserialize(deserializer)?;
    Ok(value != 0)
}

pub fn bool_from_zero<'de, D>(deserializer: D) -> Result<bool, D::Error>
where
    D: Deserializer<'de>,
{
    let value: i64 = Deserialize::deserialize(deserializer)?;
    Ok(value == 0)
}

pub fn deserialize_sys_metal<'de, D>(deserializer: D) -> Result<Vec<Metal>, D::Error>
where
    D: Deserializer<'de>,
{
    let value: serde_json::Value = Deserialize::deserialize(deserializer)?;
    let mut metals = Vec::new();

    match value {
        // 兼容旧格式：["{\"list\":[...]}"]
        serde_json::Value::Array(arr) => {
            for item in arr {
                if let Some(s) = item.as_str()
                    && let Ok(m) = to_metal(s)
                {
                    metals.extend(m);
                }
            }
        }
        // 兼容旧格式："{"list":[...]}"
        serde_json::Value::String(s) => {
            if let Ok(m) = to_metal(&s) {
                metals.extend(m);
            }
        }
        // 新格式（对象数组）当前 SDK 不消费勋章细节，这里忽略以避免影响主流程解析
        _ => {}
    }

    Ok(metals)
}
