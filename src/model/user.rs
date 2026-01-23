use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::utils::error::Error;

#[derive(Clone, Serialize, Deserialize, Default)]
#[allow(non_snake_case)]
pub struct UserInfo {
    /// 用户id
    #[serde(rename = "oId")]
    oId: String,
    /// 用户编号
    userNo: String,
    /// 用户名
    userName: String,
    /// 用户昵称
    userNickname: String,
    /// 首页地址
    #[serde(rename = "userURL")]
    URL: String,
    /// 所在城市
    #[serde(rename = "userCity")]
    city: String,
    /// 签名
    #[serde(rename = "userIntro")]
    intro: String,
    /// 是否在线
    #[serde(rename = "userOnlineFlag")]
    online: bool,
    /// 用户积分
    #[serde(rename = "userPoint")]
    points: i32,
    /// 用户组
    #[serde(rename = "userRole")]
    role: String,
    /// 角色
    #[serde(rename = "userAppRole")]
    appRole: UserAppRole,
    /// 头像地址
    #[serde(rename = "userAvatarURL")]
    avatar: String,
    /// 用户卡片背景
    cardBg: String,
    /// 用户关注数
    #[serde(rename = "followingUserCount")]
    following: i32,
    /// 用户粉丝数
    #[serde(rename = "followerCount")]
    follower: i32,
    /// 在线时长(分钟)
    #[serde(rename = "onlineMinute")]
    onlineMinutes: i32,
    // / 是否已经关注，未登录则为 `hide`
    // canFollow: String,
    // / 用户所有勋章列表，包含未佩戴
    // ownedMetal: Vec<Metal>,
    /// 用户勋章列表
    sysMetal: Vec<Metal>,
    // / MBTI 性格类型
    // mbti: String,
}

impl UserInfo {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        let mut data = data.clone();

        if let Some(sys_metal_str) = data["sysMetal"].as_str() {
            let metals = to_metal(sys_metal_str).map_err(|e| Error::Parse(e.to_string()))?;
            data["sysMetal"] =
                serde_json::to_value(metals).map_err(|e| Error::Parse(e.to_string()))?;
        }

        if let Some(owned_metal_str) = data["ownedMetal"].as_str() {
            let metals = to_metal(owned_metal_str).map_err(|e| Error::Parse(e.to_string()))?;
            data["ownedMetal"] =
                serde_json::to_value(metals).map_err(|e| Error::Parse(e.to_string()))?;
        }

        serde_json::from_value(data)
            .map_err(|e| Error::Parse(format!("Failed to parse UserInfo: {}", e)))
    }
}

/// 更新用户信息参数
#[derive(Clone, Serialize, Deserialize)]
#[allow(non_snake_case)]
pub struct UpdateUserInfoParams {
    /// 用户昵称
    pub nickName: Option<String>,
    ///  用户标签，多个标签用逗号分隔
    pub userTag: Option<String>,
    /// 个人主页 URL
    pub userUrl: Option<String>,
    /// 个人简介
    pub userIntro: Option<String>,
    /// MBTI 性格类型（例如：ENFP）
    pub mbti: Option<String>,
}

#[derive(Clone, Serialize, Deserialize, Default)]
#[repr(u8)]
#[serde(try_from = "String")]
enum UserAppRole {
    /// 黑客
    #[default]
    Hack = 0,
    /// 画家
    Artist = 1,
}

#[derive(Clone, Serialize, Deserialize, Default, Debug)]
pub struct MetalBase {
    pub attr: MetalAttrOrString,
    pub name: String,
    pub description: String,
    pub data: String,
}

#[derive(Clone, Serialize, Deserialize, Default, Debug)]
pub struct MetalAttr {
    /// 徽标图地址
    url: String,
    /// 背景色
    backcolor: String,
    /// 文字颜色
    fontcolor: String,
    /// 版本号
    ver: f32,
    /// 缩放比例
    scale: f32,
}

#[derive(Clone, Serialize, Deserialize, Debug)]
pub enum MetalAttrOrString {
    Attr(MetalAttr),
    Str(String),
}

impl std::fmt::Display for MetalAttrOrString {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            MetalAttrOrString::Attr(attr) => {
                write!(
                    f,
                    "ver={}&scale={}&backcolor={}&fontcolor={}&url={}",
                    attr.ver, attr.scale, attr.backcolor, attr.fontcolor, attr.url
                )
            }
            MetalAttrOrString::Str(s) => write!(f, "{}", s),
        }
    }
}

#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct Metal {
    /// 徽章基本信息
    base: MetalBase,
    /// 完整徽章地址（含文字）
    url: String,
    /// 徽章地址（不含文字）
    icon: String,
    /// 是否佩戴
    enable: bool,
}

#[derive(Clone, Deserialize)]
#[allow(non_snake_case)]
pub struct AtUser {
    /// 用户名
    pub userName: String,
    /// 用户头像
    pub userAvatarURL: String,
    /// 全小写用户名
    pub userNameLowerCase: String,
}

impl AtUser {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse AtUser: {}", e)))
    }
}

impl From<u8> for UserAppRole {
    fn from(value: u8) -> Self {
        match value {
            0 => UserAppRole::Hack,
            1 => UserAppRole::Artist,
            _ => UserAppRole::Hack,
        }
    }
}

impl From<UserAppRole> for u8 {
    fn from(value: UserAppRole) -> Self {
        match value {
            UserAppRole::Hack => 0,
            UserAppRole::Artist => 1,
        }
    }
}

impl TryFrom<String> for UserAppRole {
    type Error = String;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        let num: u8 = value.parse().map_err(|_| "Invalid number".to_string())?;
        Ok(UserAppRole::from(num))
    }
}

impl UserInfo {
    pub fn name(&self) -> &str {
        if self.userNickname.is_empty() {
            &self.userName
        } else {
            &self.userNickname
        }
    }
}

impl Default for MetalAttrOrString {
    fn default() -> Self {
        MetalAttrOrString::Attr(MetalAttr::default())
    }
}

/// 共同trait
#[allow(dead_code)]
trait MetalCommon {
    fn attr(&self) -> &MetalAttrOrString;
    fn name(&self) -> &str;
    fn description(&self) -> &str;
    fn data(&self) -> &str;
    fn to_url(&self, include_text: bool) -> String {
        let domain = "fishpi.cn";
        let attr_str = match self.attr() {
            MetalAttrOrString::Attr(attr) => {
                format!(
                    "ver={}&scale={}&backcolor={}&fontcolor={}",
                    attr.ver, attr.scale, attr.backcolor, attr.fontcolor
                )
            }
            MetalAttrOrString::Str(s) => s.clone(),
        };
        let text_str = if include_text {
            self.name().to_string()
        } else {
            "".to_string()
        };
        format!("`https://{}/gen?txt={}&{}", domain, text_str, attr_str)
    }
}

impl MetalCommon for MetalBase {
    fn attr(&self) -> &MetalAttrOrString {
        &self.attr
    }
    fn name(&self) -> &str {
        &self.name
    }
    fn description(&self) -> &str {
        &self.description
    }
    fn data(&self) -> &str {
        &self.data
    }
}

#[allow(dead_code)]
impl MetalBase {
    fn new(metal: Option<&Self>) -> Self {
        metal.cloned().unwrap_or_default()
    }
}

impl Default for Metal {
    fn default() -> Self {
        Self {
            base: MetalBase::default(),
            url: String::default(),
            icon: String::default(),
            enable: true,
        }
    }
}

impl MetalCommon for Metal {
    fn attr(&self) -> &MetalAttrOrString {
        self.base.attr()
    }
    fn name(&self) -> &str {
        self.base.name()
    }
    fn description(&self) -> &str {
        self.base.description()
    }
    fn data(&self) -> &str {
        self.base.data()
    }
}

#[allow(dead_code)]
impl Metal {
    fn new(metal: Option<&Self>) -> Self {
        let base = MetalBase::new(metal.map(|m| &m.base));
        Self {
            base,
            ..Default::default()
        }
    }
}

pub fn to_metal(sys_metal: &str) -> Result<Vec<Metal>, Box<dyn std::error::Error>> {
    let parsed: Value = serde_json::from_str(sys_metal)?;
    let list = parsed["list"].as_array().ok_or("no list in sysMetal")?;
    let mut metals = Vec::new();
    for item in list {
        let attr_str = item["attr"].as_str().unwrap_or("");
        let base = MetalBase {
            attr: analyze_metal_attr(attr_str),
            name: item["name"].as_str().unwrap_or("").to_string(),
            description: item["description"].as_str().unwrap_or("").to_string(),
            data: item["data"].as_str().unwrap_or("").to_string(),
        };
        let url = base.to_url(true);
        let icon = base.to_url(false);
        let enable = item["enabled"].as_bool().unwrap_or(true);
        metals.push(Metal {
            base,
            url,
            icon,
            enable,
        });
    }
    Ok(metals)
}

pub fn analyze_metal_attr(attr_str: &str) -> MetalAttrOrString {
    if attr_str.is_empty() {
        return MetalAttrOrString::Str("".to_string());
    }
    let mut url = String::new();
    let mut backcolor = String::new();
    let mut fontcolor = String::new();
    let mut ver = 1.0;
    let mut scale = 0.79;
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
        MetalAttrOrString::Str(attr_str.to_string())
    } else {
        MetalAttrOrString::Attr(MetalAttr {
            url,
            backcolor,
            fontcolor,
            ver,
            scale,
        })
    }
}

#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct UserPoint {
    #[serde(rename = "userPoint")]
    pub point: u32,
    #[serde(rename = "userName")]
    pub name: String,
}

impl UserPoint {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data["data"].clone())
            .map_err(|e| Error::Parse(format!("Failed to parse UserPoint: {}", e)))
    }
}