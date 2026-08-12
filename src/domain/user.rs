//! 用户相关的干净对外类型。

use chrono::{DateTime, Utc};

use crate::ids::{UserId, UserName};
use crate::wire::user::{
    WireAtUser, WireMetal, WireUserInfo, WireUserLite, WireUserPoint, WireUserVipInfo,
};

const DOMAIN: &str = "fishpi.cn";

/// 用户 App 角色。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Default)]
pub enum UserAppRole {
    /// 黑客
    #[default]
    Hack,
    /// 画家
    Artist,
}

impl From<&str> for UserAppRole {
    fn from(value: &str) -> Self {
        match value.trim() {
            "1" => UserAppRole::Artist,
            _ => UserAppRole::Hack,
        }
    }
}

/// 勋章 attr。
#[derive(Clone, Debug)]
pub enum MetalAttr {
    Attr {
        url: String,
        backcolor: String,
        fontcolor: String,
        ver: f32,
        scale: f32,
    },
    Raw(String),
}

/// 一枚勋章。
#[derive(Clone, Debug)]
pub struct Metal {
    pub attr: MetalAttr,
    pub name: String,
    pub description: String,
    pub data: String,
    /// 完整徽章地址（含文字）
    pub url: String,
    /// 徽章地址（不含文字）
    pub icon: String,
    /// 是否佩戴
    pub enabled: bool,
}

fn analyze_metal_attr(attr_str: &str) -> MetalAttr {
    if attr_str.is_empty() {
        return MetalAttr::Raw(String::new());
    }
    let mut url = String::new();
    let mut backcolor = String::new();
    let mut fontcolor = String::new();
    let mut ver = 1.0_f32;
    let mut scale = 0.79_f32;
    for pair in attr_str.split('&') {
        let mut parts = pair.split('=');
        if let (Some(key), Some(value)) = (parts.next(), parts.next()) {
            match key {
                "url" => url = value.to_string(),
                "backcolor" => backcolor = value.to_string(),
                "fontcolor" => fontcolor = value.to_string(),
                "ver" => ver = value.parse().unwrap_or(1.0),
                "scale" => scale = value.parse().unwrap_or(0.79),
                _ => {}
            }
        }
    }
    if url.is_empty() && backcolor.is_empty() && fontcolor.is_empty() {
        MetalAttr::Raw(attr_str.to_string())
    } else {
        MetalAttr::Attr {
            url,
            backcolor,
            fontcolor,
            ver,
            scale,
        }
    }
}

fn metal_url(attr: &MetalAttr, name: &str, include_text: bool) -> String {
    let attr_str = match attr {
        MetalAttr::Attr {
            backcolor,
            fontcolor,
            ver,
            scale,
            ..
        } => format!("ver={ver}&scale={scale}&backcolor={backcolor}&fontcolor={fontcolor}"),
        MetalAttr::Raw(s) => s.clone(),
    };
    let text = if include_text { name } else { "" };
    format!("https://{DOMAIN}/gen?txt={text}&{attr_str}")
}

impl From<WireMetal> for Metal {
    fn from(w: WireMetal) -> Self {
        let attr = analyze_metal_attr(&w.attr);
        let url = metal_url(&attr, &w.name, true);
        let icon = metal_url(&attr, &w.name, false);
        Metal {
            attr,
            name: w.name,
            description: w.description,
            data: w.data,
            url,
            icon,
            enabled: w.enabled,
        }
    }
}

/// 用户信息。
#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct UserInfo {
    /// 用户 ID（`oId`）
    pub id: UserId,
    /// 用户编号
    pub user_no: String,
    /// 用户名
    pub username: UserName,
    /// 用户昵称
    pub nickname: String,
    /// 首页地址
    pub url: String,
    /// 所在城市
    pub city: String,
    /// 签名
    pub intro: String,
    /// 是否在线
    pub online: bool,
    /// 用户积分
    pub points: i32,
    /// 用户组
    pub role: String,
    /// App 角色
    pub app_role: UserAppRole,
    /// 头像地址
    pub avatar: String,
    /// 卡片背景
    pub card_bg: String,
    /// 关注数
    pub following: i32,
    /// 粉丝数
    pub follower: i32,
    /// 在线时长（分钟）
    pub online_minutes: i32,
    /// 是否可关注（`no`=已关注，未登录可能 `hide`）
    pub can_follow: String,
    /// 佩戴/持有的勋章
    pub metals: Vec<Metal>,
}

impl UserInfo {
    /// 展示名：优先昵称，空则用户名。
    pub fn display_name(&self) -> &str {
        if self.nickname.is_empty() {
            self.username.as_str()
        } else {
            &self.nickname
        }
    }
}

impl From<WireUserInfo> for UserInfo {
    fn from(w: WireUserInfo) -> Self {
        UserInfo {
            id: UserId::new(w.o_id),
            user_no: w.user_no,
            username: UserName::new(w.user_name),
            nickname: w.user_nickname,
            url: w.user_url,
            city: w.user_city,
            intro: w.user_intro,
            online: w.user_online_flag,
            points: w.user_point,
            role: w.user_role,
            app_role: UserAppRole::from(w.user_app_role.as_str()),
            avatar: w.user_avatar_url,
            card_bg: w.card_bg,
            following: w.following_user_count,
            follower: w.follower_count,
            online_minutes: w.online_minute,
            can_follow: w.can_follow,
            metals: w.sys_metal.into_iter().map(Metal::from).collect(),
        }
    }
}

/// 用户名联想项。
#[derive(Clone, Debug)]
pub struct AtUser {
    pub username: UserName,
    pub avatar: String,
    pub username_lower: String,
}

impl From<WireAtUser> for AtUser {
    fn from(w: WireAtUser) -> Self {
        AtUser {
            username: UserName::new(w.user_name),
            avatar: w.user_avatar_url,
            username_lower: w.user_name_lower_case,
        }
    }
}

/// 用户积分。
#[derive(Clone, Debug)]
pub struct UserPoint {
    pub username: UserName,
    pub point: u32,
}

impl From<WireUserPoint> for UserPoint {
    fn from(w: WireUserPoint) -> Self {
        UserPoint {
            username: UserName::new(w.user_name),
            point: w.user_point,
        }
    }
}

/// 最近注册用户。
#[derive(Clone, Debug)]
pub struct UserLite {
    pub username: UserName,
    pub nickname: String,
}

impl From<WireUserLite> for UserLite {
    fn from(w: WireUserLite) -> Self {
        UserLite {
            username: UserName::new(w.user_name),
            nickname: w.user_nickname,
        }
    }
}

/// 用户 VIP 信息。
#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct UserVipInfo {
    pub joint_vip: bool,
    pub color: String,
    pub underline: bool,
    pub metal: bool,
    pub auto_checkin: u32,
    pub bold: bool,
    pub o_id: UserId,
    pub state: bool,
    pub user_id: UserId,
    pub lv_code: String,
    pub expires_at: u64,
    pub created_at: u64,
    pub updated_at: u64,
}

impl UserVipInfo {
    /// 是否为 VIP。
    pub fn is_vip(&self) -> bool {
        self.state
    }

    /// 过期日期。
    pub fn expires_date(&self) -> DateTime<Utc> {
        DateTime::from_timestamp_millis(self.expires_at as i64).unwrap_or_default()
    }

    /// 创建日期。
    pub fn created_date(&self) -> DateTime<Utc> {
        DateTime::from_timestamp_millis(self.created_at as i64).unwrap_or_default()
    }

    /// 更新日期。
    pub fn updated_date(&self) -> DateTime<Utc> {
        DateTime::from_timestamp_millis(self.updated_at as i64).unwrap_or_default()
    }

    /// VIP 名称。
    pub fn vip_name(&self) -> String {
        self.lv_code
            .replace("_YEAR", "(包年)")
            .replace("_MONTH", "(包月)")
    }
}

impl From<WireUserVipInfo> for UserVipInfo {
    fn from(w: WireUserVipInfo) -> Self {
        UserVipInfo {
            joint_vip: w.joint_vip,
            color: w.color,
            underline: w.underline,
            metal: w.metal,
            auto_checkin: w.auto_checkin,
            bold: w.bold,
            o_id: w.o_id.into(),
            state: w.state,
            user_id: w.user_id.into(),
            lv_code: w.lv_code,
            expires_at: w.expires_at,
            created_at: w.created_at,
            updated_at: w.updated_at,
        }
    }
}
