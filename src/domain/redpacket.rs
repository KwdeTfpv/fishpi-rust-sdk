use crate::ids::UserId;
use crate::wire::redpacket::{
    WireRedPacketBase, WireRedPacketGot, WireRedPacketInfo, WireRedPacketMessage,
    WireRedPacketStatusMsg,
};

#[derive(Clone, Copy, Debug)]
#[repr(u8)]
pub enum GestureType {
    Rock = 0,
    Scissors = 1,
    Paper = 2,
}

#[derive(Clone, Debug)]
pub enum RedPacketType {
    Random,
    Average,
    Specify,
    Heartbeat,
    RockPaperScissors,
}

impl RedPacketType {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Random => "random",
            Self::Average => "average",
            Self::Specify => "specify",
            Self::Heartbeat => "heartbeat",
            Self::RockPaperScissors => "rockPaperScissors",
        }
    }
}

#[derive(Clone, Debug)]
pub struct RedPacketInfo {
    pub info: RedPacketBase,
    pub receivers: Vec<String>,
    pub who: Vec<RedPacketGot>,
}

impl From<WireRedPacketInfo> for RedPacketInfo {
    fn from(wire: WireRedPacketInfo) -> Self {
        Self {
            info: wire.info.into(),
            receivers: wire.receivers,
            who: wire.who.into_iter().map(Into::into).collect(),
        }
    }
}

#[derive(Clone, Debug)]
pub struct RedPacketBase {
    pub count: u32,
    pub gesture: Option<GestureType>,
    pub got: u32,
    pub message: String,
    pub user_name: String,
    pub user_avatar_url: String,
}

impl From<WireRedPacketBase> for RedPacketBase {
    fn from(wire: WireRedPacketBase) -> Self {
        Self {
            count: wire.count,
            gesture: wire.gesture.and_then(|g| match g {
                0 => Some(GestureType::Rock),
                1 => Some(GestureType::Scissors),
                2 => Some(GestureType::Paper),
                _ => None,
            }),
            got: wire.got,
            message: wire.message,
            user_name: wire.user_name,
            user_avatar_url: wire.user_avatar_url,
        }
    }
}

#[derive(Clone, Debug)]
pub struct RedPacketGot {
    pub user_id: UserId,
    pub user_name: String,
    pub avatar: String,
    pub user_money: i32,
    pub time: String,
}

impl From<WireRedPacketGot> for RedPacketGot {
    fn from(wire: WireRedPacketGot) -> Self {
        Self {
            user_id: wire.user_id.into(),
            user_name: wire.user_name,
            avatar: wire.avatar,
            user_money: wire.user_money,
            time: wire.time,
        }
    }
}

#[derive(Clone, Debug)]
pub struct RedPacketMessage {
    pub msg_type: String,
    pub count: u32,
    pub got: u32,
    pub money: u32,
    pub message: String,
    pub sender_id: String,
    pub gesture: Option<GestureType>,
    pub receivers: Vec<String>,
    pub who: Vec<RedPacketGot>,
}

impl From<WireRedPacketMessage> for RedPacketMessage {
    fn from(wire: WireRedPacketMessage) -> Self {
        Self {
            msg_type: wire.msg_type,
            count: wire.count,
            got: wire.got,
            money: wire.money,
            message: wire.message,
            sender_id: wire.sender_id,
            gesture: wire.gesture.and_then(|g| match g {
                0 => Some(GestureType::Rock),
                1 => Some(GestureType::Scissors),
                2 => Some(GestureType::Paper),
                _ => None,
            }),
            receivers: wire.receivers,
            who: wire.who.into_iter().map(Into::into).collect(),
        }
    }
}

#[derive(Clone, Debug)]
pub struct RedPacketStatusMsg {
    pub o_id: UserId,
    pub count: u32,
    pub got: u32,
    pub who_give: String,
    pub who_got: Vec<String>,
    pub user_avatar_url20: String,
    pub user_avatar_url48: String,
    pub user_avatar_url210: String,
}

impl From<WireRedPacketStatusMsg> for RedPacketStatusMsg {
    fn from(wire: WireRedPacketStatusMsg) -> Self {
        Self {
            o_id: wire.oId.into(),
            count: wire.count,
            got: wire.got,
            who_give: wire.who_give,
            who_got: wire.who_got,
            user_avatar_url20: wire.user_avatar_url20,
            user_avatar_url48: wire.user_avatar_url48,
            user_avatar_url210: wire.user_avatar_url210,
        }
    }
}
