use crate::ids::UserId;
use crate::wire::finger::{WireUserBag, WireUserIP};

/// 用户最近登录 IP。
#[derive(Clone, Debug)]
pub struct UserIp {
    pub latest_login_ip: String,
    pub user_id: UserId,
}

impl From<WireUserIP> for UserIp {
    fn from(wire: WireUserIP) -> Self {
        Self {
            latest_login_ip: wire.latest_login_ip,
            user_id: wire.user_id.into(),
        }
    }
}

/// 用户背包。
#[derive(Clone, Debug)]
pub struct UserBag {
    /// 免签卡
    pub checkin1day: u32,
    /// 两日免签卡
    pub checkin2days: u32,
    /// 补签卡
    pub patch_checkin_card: u32,
    /// 勋章领取券
    pub metal_ticket: u32,
}

impl From<WireUserBag> for UserBag {
    fn from(wire: WireUserBag) -> Self {
        Self {
            checkin1day: wire.checkin1day,
            checkin2days: wire.checkin2days,
            patch_checkin_card: wire.patch_checkin_card,
            metal_ticket: wire.metal_ticket,
        }
    }
}

/// 背包物品类型。
#[derive(Clone, Copy, Debug)]
pub enum BagItem {
    Checkin1day,
    Checkin2days,
    PatchCheckinCard,
    MetalTicket,
}

impl BagItem {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Checkin1day => "checkin1day",
            Self::Checkin2days => "checkin2days",
            Self::PatchCheckinCard => "patchCheckinCard",
            Self::MetalTicket => "metalTicket",
        }
    }
}

/// 发放勋章入参。
#[derive(Clone, Debug)]
pub struct MetalGift {
    /// attr 字符串，形如 `url=...&backcolor=...&fontcolor=...`
    pub attr: String,
    pub name: String,
    pub description: String,
    pub data: String,
}
