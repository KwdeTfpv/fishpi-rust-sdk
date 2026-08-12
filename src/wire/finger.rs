use crate::wire::de::string_lossy;
use serde::Deserialize;

/// 用户最近登录 IP。
#[derive(Clone, Debug, Deserialize, Default)]
#[serde(default)]
pub struct WireUserIP {
    #[serde(rename = "latestLoginIP", deserialize_with = "string_lossy")]
    pub latest_login_ip: String,
    #[serde(rename = "userId", deserialize_with = "string_lossy")]
    pub user_id: String,
}

/// 用户背包。
#[derive(Clone, Debug, Deserialize, Default)]
#[serde(default)]
pub struct WireUserBag {
    pub checkin1day: u32,
    pub checkin2days: u32,
    #[serde(rename = "patchCheckinCard")]
    pub patch_checkin_card: u32,
    #[serde(rename = "metalTicket")]
    pub metal_ticket: u32,
}
