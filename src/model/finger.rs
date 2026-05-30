use serde::Deserialize;
use serde_json::Value;

use crate::{impl_str_enum, utils::error::Error};

/// 摸鱼大闯关信息
#[derive(Debug, Clone, Deserialize)]
pub struct MoFishGame {
    #[serde(rename = "userName")]
    pub user_name: String,
    pub stage: String,
    pub time: u64,
}

impl MoFishGame {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse MoFishGame: {}", e)))
    }
}

/// 用户IP信息
#[derive(Debug, Clone, Deserialize)]
pub struct UserIP {
    #[serde(rename = "latestLoginIP")]
    pub latest_login_ip: String,
    #[serde(rename = "userId")]
    pub user_id: String,
}

impl UserIP {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse UserIP: {}", e)))
    }
}

/// 用户背包物品类型
#[derive(Clone, Debug, Deserialize)]
pub enum UserBagType {
    #[serde(rename = "checkin1day")]
    Checkin1day,
    #[serde(rename = "checkin2days")]
    Checkin2days,
    #[serde(rename = "patchCheckinCard")]
    PatchCheckinCard,
    #[serde(rename = "metalTicket")]
    MetalTicket,
}

impl_str_enum!(UserBagType{
    Checkin1day => "checkin1day",
    Checkin2days => "checkin2days",
    PatchCheckinCard => "patchCheckinCard",
    MetalTicket => "metalTicket",
});

/// 用户背包信息
#[derive(Default, Clone, Debug, Deserialize)]
#[serde(default)]
pub struct UserBag {
    /// 免签卡
    pub checkin1day: u32,
    /// 两日免签卡
    pub checkin2days: u32,
    /// 补签卡
    #[serde(rename = "patchCheckinCard")]
    pub patch_checkin_card: u32,
    /// 摸鱼派一周年纪念勋章领取券
    #[serde(rename = "metalTicket")]
    pub metal_ticket: u32,
}

impl UserBag {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse UserBag: {}", e)))
    }
}
