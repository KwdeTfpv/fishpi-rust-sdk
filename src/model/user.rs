use serde::Serialize;

/// 更新用户信息参数
#[derive(Clone, Serialize)]
pub struct UserProfileUpdate {
    /// 用户昵称
    #[serde(rename = "nickName")]
    pub nickname: Option<String>,
    ///  用户标签，多个标签用逗号分隔
    #[serde(rename = "userTag")]
    pub tags: Option<String>,
    /// 个人主页 URL
    #[serde(rename = "userUrl")]
    pub url: Option<String>,
    /// 个人简介
    #[serde(rename = "userIntro")]
    pub intro: Option<String>,
    /// MBTI 性格类型（例如：ENFP）
    pub mbti: Option<String>,
}

impl From<&UserProfileUpdate> for UserProfileUpdate {
    fn from(value: &UserProfileUpdate) -> Self {
        value.clone()
    }
}
