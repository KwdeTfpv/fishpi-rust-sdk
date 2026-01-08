pub mod article;
pub mod breezemoon;
pub mod chat;
pub mod chatroom;
pub mod finger;
pub mod misc;
pub mod notice;
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
                match candidate {
                    $($str => Ok($enum_name::$variant),)*
                    _ => Err(format!("Unknown {}: {}", stringify!($enum_name), s)),
                }
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
    let strs: Vec<String> = Deserialize::deserialize(deserializer)?;
    let mut metals = Vec::new();
    for s in strs {
        match to_metal(&s) {
            Ok(m) => metals.extend(m),
            Err(e) => {
                println!("Failed to parse sysMetal: {}", e);
            }
        }
    }
    Ok(metals)
}
