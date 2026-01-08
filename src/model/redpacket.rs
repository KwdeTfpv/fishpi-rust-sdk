use std::str::FromStr;

use serde_json::Value;

use crate::{impl_str_enum, utils::error::Error};

/// 猜拳类型
#[derive(Clone, Debug)]
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
#[derive(Clone, Debug)]
pub struct RedPacket {
    /// 红包类型
    pub r#type: RedPacketType,
    /// 红包积分
    pub money: u32,
    /// 红包个数
    pub count: u32,
    /// 祝福语
    pub msg: String,
    /// 接收者, 专属红包有效
    pub recivers: Vec<String>,
    /// 出拳, 猜拳红包有效
    pub gesture: Option<GestureType>,
}

/// 红包领取者信息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct RedPacketGot {
    /// 用户ID
    pub userId: String,
    /// 用户名
    pub userName: String,
    /// 用户头像
    pub avatar: String,
    /// 领取到的积分
    pub userMoney: u32,
    /// 领取时间
    pub time: String,
}

/// 红包历史信息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct RedPacketMessage {
    /// 消息类型，固定为redPacket
    pub msgType: String,
    /// 红包数
    pub count: u32,
    /// 领取数
    pub got: u32,
    /// 内含积分
    pub money: u32,
    /// 祝福语
    pub msg: String,
    /// 发送者ID
    pub senderId: String,
    /// 出拳，猜拳红包有效
    pub GestureType: Option<GestureType>,
    /// 接收者，专属红包有效
    pub recivers: Vec<String>,
    /// 已领取者列表
    pub who: Vec<RedPacketGot>,
}

/// 红包基本信息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct RedPacketBase {
    /// 数量
    pub count: u32,
    /// 猜拳类型
    pub gesture: Option<GestureType>,
    /// 领取数
    pub got: u32,
    /// 祝福语
    pub msg: String,
    /// 发送者用户名
    pub userName: String,
    /// 用户头像
    pub userAvatarURL: String,
}

/// 红包信息
#[derive(Clone, Debug)]
pub struct RedPacketInfo {
    pub info: RedPacketBase,
    pub recivers: Vec<String>,
    pub who: Vec<RedPacketGot>,
}

/// 红包状态信息
#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct RedPacketStatusMsg {
    pub oId: String,
    pub count: u32,
    pub got: u32,
    pub whoGive: String,
    pub whoGot: Vec<String>,
    pub avatarURL20: String,
    pub avatarURL48: String,
    pub avatarURL210: String,
}

impl RedPacketStatusMsg {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        Ok(RedPacketStatusMsg {
            oId: data["oId"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing oId in RedPacketStatusMsg".to_string()))?
                .to_string(),
            count: data["count"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid count in RedPacketStatusMsg".to_string())
            })? as u32,
            got: data["got"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid got in RedPacketStatusMsg".to_string())
            })? as u32,
            whoGive: data["whoGive"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing whoGive in RedPacketStatusMsg".to_string()))?
                .to_string(),
            whoGot: if let Some(who_got_array) = data["whoGot"].as_array() {
                who_got_array
                    .iter()
                    .filter_map(|v| v.as_str().map(|s| s.to_string()))
                    .collect()
            } else {
                data["whoGot"]
                    .as_str()
                    .map(|s| vec![s.to_string()])
                    .unwrap_or_default()
            },
            avatarURL20: data["userAvatarURL20"]
                .as_str()
                .ok_or_else(|| {
                    Error::Parse("Missing userAvatarURL20 in RedPacketStatusMsg".to_string())
                })?
                .to_string(),
            avatarURL48: data["userAvatarURL48"]
                .as_str()
                .ok_or_else(|| {
                    Error::Parse("Missing userAvatarURL48 in RedPacketStatusMsg".to_string())
                })?
                .to_string(),
            avatarURL210: data["userAvatarURL210"]
                .as_str()
                .ok_or_else(|| {
                    Error::Parse("Missing userAvatarURL210 in RedPacketStatusMsg".to_string())
                })?
                .to_string(),
        })
    }
}

impl Default for RedPacket {
    fn default() -> Self {
        RedPacket {
            r#type: RedPacketType::Random,
            money: 32,
            count: 1,
            msg: "摸鱼者, 事竟成!".to_string(),
            recivers: Vec::new(),
            gesture: None,
        }
    }
}

impl RedPacket {
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
            msg: data["msg"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing msg in RedPacket".to_string()))?
                .to_string(),
            recivers: if let Some(recivers_array) = data["recivers"].as_array() {
                recivers_array
                    .iter()
                    .filter_map(|v| v.as_str().map(|s| s.to_string()))
                    .collect()
            } else {
                Vec::new()
            },
            gesture: if let Some(gesture_str) = data["gesture"].as_str() {
                Some(
                    GestureType::from_str(gesture_str)
                        .map_err(|_| Error::Parse("Invalid gesture in RedPacket".to_string()))?,
                )
            } else {
                None
            },
        })
    }
}

impl RedPacketMessage {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        Ok(RedPacketMessage {
            msgType: data["msgType"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing msgType in RedPacketMessage".to_string()))?
                .to_string(),
            count: data["count"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid count in RedPacketMessage".to_string())
            })? as u32,
            got: data["got"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid got in RedPacketMessage".to_string())
            })? as u32,
            money: data["money"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid money in RedPacketMessage".to_string())
            })? as u32,
            msg: data["msg"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing msg in RedPacketMessage".to_string()))?
                .to_string(),
            senderId: data["senderId"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing senderId in RedPacketMessage".to_string()))?
                .to_string(),
            GestureType: if let Some(gesture_str) = data["gesture"].as_str() {
                Some(
                    GestureType::from_str(gesture_str).map_err(|_| {
                        Error::Parse("Invalid gesture in RedPacketMessage".to_string())
                    })?,
                )
            } else {
                None
            },
            recivers: if let Some(recivers_array) = data["recivers"].as_array() {
                recivers_array
                    .iter()
                    .filter_map(|v| v.as_str().map(|s| s.to_string()))
                    .collect()
            } else {
                Vec::new()
            },
            who: if let Some(who_array) = data["who"].as_array() {
                let mut got_list = Vec::new();
                for item in who_array {
                    got_list.push(RedPacketGot {
                        userId: item["userId"]
                            .as_str()
                            .ok_or_else(|| Error::Parse("Missing userId in who".to_string()))?
                            .to_string(),
                        userName: item["userName"]
                            .as_str()
                            .ok_or_else(|| Error::Parse("Missing userName in who".to_string()))?
                            .to_string(),
                        avatar: item["avatar"]
                            .as_str()
                            .ok_or_else(|| Error::Parse("Missing avatar in who".to_string()))?
                            .to_string(),
                        userMoney: item["userMoney"].as_u64().ok_or_else(|| {
                            Error::Parse("Missing or invalid userMoney in who".to_string())
                        })? as u32,
                        time: item["time"]
                            .as_str()
                            .ok_or_else(|| Error::Parse("Missing time in who".to_string()))?
                            .to_string(),
                    });
                }
                got_list
            } else {
                Vec::new()
            },
        })
    }
}

impl RedPacketBase {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        Ok(RedPacketBase {
            count: data["count"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid count in RedPacketBase".to_string())
            })? as u32,
            gesture: if let Some(gesture_str) = data["gesture"].as_str() {
                Some(
                    GestureType::from_str(gesture_str).map_err(|_| {
                        Error::Parse("Invalid gesture in RedPacketBase".to_string())
                    })?,
                )
            } else {
                None
            },
            got: data["got"].as_u64().ok_or_else(|| {
                Error::Parse("Missing or invalid got in RedPacketBase".to_string())
            })? as u32,
            msg: data["msg"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing msg in RedPacketBase".to_string()))?
                .to_string(),
            userName: data["userName"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing userName in RedPacketBase".to_string()))?
                .to_string(),
            userAvatarURL: data["userAvatarURL"]
                .as_str()
                .ok_or_else(|| Error::Parse("Missing userAvatarURL in RedPacketBase".to_string()))?
                .to_string(),
        })
    }
}

impl RedPacketInfo {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        let info_data = &data["info"];
        let info = RedPacketBase::from_value(info_data)?;

        let recivers = if let Some(recivers_array) = data["recivers"].as_array() {
            recivers_array
                .iter()
                .filter_map(|v| v.as_str().map(|s| s.to_string()))
                .collect()
        } else {
            Vec::new()
        };

        let who = if let Some(who_array) = data["who"].as_array() {
            let mut got_list = Vec::new();
            for item in who_array {
                got_list.push(RedPacketGot {
                    userId: item["userId"]
                        .as_str()
                        .ok_or_else(|| Error::Parse("Missing userId in who".to_string()))?
                        .to_string(),
                    userName: item["userName"]
                        .as_str()
                        .ok_or_else(|| Error::Parse("Missing userName in who".to_string()))?
                        .to_string(),
                    avatar: item["avatar"]
                        .as_str()
                        .ok_or_else(|| Error::Parse("Missing avatar in who".to_string()))?
                        .to_string(),
                    userMoney: item["userMoney"].as_u64().ok_or_else(|| {
                        Error::Parse("Missing or invalid userMoney in who".to_string())
                    })? as u32,
                    time: item["time"]
                        .as_str()
                        .ok_or_else(|| Error::Parse("Missing time in who".to_string()))?
                        .to_string(),
                });
            }
            got_list
        } else {
            Vec::new()
        };

        Ok(RedPacketInfo {
            info,
            recivers,
            who,
        })
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
