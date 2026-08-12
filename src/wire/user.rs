//! 用户相关响应的 wire 层类型。

use serde::{Deserialize, Deserializer};
use serde_json::Value;

use super::de::{bool_from_int_or_bool, f64_lenient};

/// 勋章原始项。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireMetal {
    /// 原始 attr 串
    #[serde(default)]
    pub attr: String,
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub description: String,
    #[serde(default)]
    pub data: String,
    /// 是否佩戴
    #[serde(default = "default_true")]
    pub enabled: bool,
}

fn default_true() -> bool {
    true
}

/// 解析 `sysMetal`/`ownedMetal` 为 `Vec<WireMetal>`。
pub fn metals<'de, D>(deserializer: D) -> Result<Vec<WireMetal>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(parse_metals(value))
}

fn parse_metals(value: Value) -> Vec<WireMetal> {
    let mut out = Vec::new();
    collect_metals(value, &mut out);
    out
}

/// 递归收集勋章。
fn collect_metals(value: Value, out: &mut Vec<WireMetal>) {
    match value {
        Value::String(s) => {
            let trimmed = s.trim();
            if let Ok(v) = serde_json::from_str::<Value>(trimmed) {
                collect_metals(v, out);
            }
        }
        Value::Array(items) => {
            for item in items {
                collect_metals(item, out);
            }
        }
        Value::Object(mut map) => {
            if let Some(list) = map.remove("list") {
                collect_metals(list, out);
            } else if let Ok(metal) = serde_json::from_value::<WireMetal>(Value::Object(map)) {
                out.push(metal);
            }
        }
        _ => {}
    }
}

/// 用户信息响应。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireUserInfo {
    #[serde(rename = "oId", default)]
    pub o_id: String,
    #[serde(rename = "userNo", default)]
    pub user_no: String,
    #[serde(rename = "userName", default)]
    pub user_name: String,
    #[serde(rename = "userNickname", default)]
    pub user_nickname: String,
    #[serde(rename = "userURL", default)]
    pub user_url: String,
    #[serde(rename = "userCity", default)]
    pub user_city: String,
    #[serde(rename = "userIntro", default)]
    pub user_intro: String,
    #[serde(rename = "userOnlineFlag", default)]
    pub user_online_flag: bool,
    #[serde(rename = "userPoint", default)]
    pub user_point: i32,
    #[serde(rename = "userRole", default)]
    pub user_role: String,
    #[serde(rename = "userAppRole", default)]
    pub user_app_role: String,
    #[serde(rename = "userAvatarURL", default)]
    pub user_avatar_url: String,
    #[serde(rename = "cardBg", default)]
    pub card_bg: String,
    #[serde(rename = "followingUserCount", default)]
    pub following_user_count: i32,
    #[serde(rename = "followerCount", default)]
    pub follower_count: i32,
    #[serde(rename = "onlineMinute", default)]
    pub online_minute: i32,
    #[serde(rename = "canFollow", default)]
    pub can_follow: String,
    #[serde(rename = "sysMetal", default, deserialize_with = "metals")]
    pub sys_metal: Vec<WireMetal>,
}

/// 用户名联想项。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireAtUser {
    #[serde(rename = "userName", default)]
    pub user_name: String,
    #[serde(rename = "userAvatarURL", default)]
    pub user_avatar_url: String,
    #[serde(rename = "userNameLowerCase", default)]
    pub user_name_lower_case: String,
}

/// 用户积分。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireUserPoint {
    #[serde(rename = "userPoint", default)]
    pub user_point: u32,
    #[serde(rename = "userName", default)]
    pub user_name: String,
}

/// 最近注册用户。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireUserLite {
    #[serde(rename = "userNickname", default)]
    pub user_nickname: String,
    #[serde(rename = "userName", default)]
    pub user_name: String,
}

/// VIP 信息。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireUserVipInfo {
    #[serde(rename = "jointVip", default)]
    pub joint_vip: bool,
    #[serde(default)]
    pub color: String,
    #[serde(default)]
    pub underline: bool,
    #[serde(default)]
    pub metal: bool,
    #[serde(rename = "autoCheckin", default)]
    pub auto_checkin: u32,
    #[serde(default)]
    pub bold: bool,
    #[serde(rename = "oId", default)]
    pub o_id: String,
    #[serde(default, deserialize_with = "bool_from_int_or_bool")]
    pub state: bool,
    #[serde(rename = "userId", default)]
    pub user_id: String,
    #[serde(rename = "lvCode", default)]
    pub lv_code: String,
    #[serde(rename = "expiresAt", default)]
    pub expires_at: u64,
    #[serde(rename = "createdAt", default)]
    pub created_at: u64,
    #[serde(rename = "updatedAt", default)]
    pub updated_at: u64,
}

/// 活跃度响应。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireLiveness {
    #[serde(default, deserialize_with = "f64_lenient")]
    pub liveness: f64,
}
