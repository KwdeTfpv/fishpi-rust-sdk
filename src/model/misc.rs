use serde::{Deserialize, Deserializer, Serialize, Serializer};
use serde_json::Value;

use crate::utils::error::Error;

fn to_md5(input: &str) -> String {
    let hash = md5::compute(input.as_bytes());
    format!("{:x}", hash)
}

/// 登录账户信息
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct LoginData {
    #[serde(rename = "nameOrEmail")]
    pub username: String,
    #[serde(rename = "passwd")]
    pub password: String,
    #[serde(rename = "mfaCode")]
    pub mfa_code: Option<String>,
}

impl LoginData {
    pub fn new(username: &str, password: &str, mfa_code: Option<String>) -> Self {
        LoginData {
            username: username.to_string(),
            password: to_md5(password),
            mfa_code,
        }
    }

    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse LoginData: {}", e)))
    }

    pub fn to_value(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize LoginData: {}", e)))
    }
}

/// 预注册账户信息
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
#[derive(Default)]
pub struct PreRegisterInfo {
    /// 用户名
    #[serde(rename = "userName")]
    pub username: String,
    /// 手机号
    #[serde(rename = "userPhone")]
    pub phone: String,
    /// 邀请码
    pub invitecode: Option<String>,
    /// 验证码
    pub captcha: String,
}

impl PreRegisterInfo {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse PreRegisterInfo: {}", e)))
    }

    pub fn to_value(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize PreRegisterInfo: {}", e)))
    }
}

/// 注册账户信息
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
pub struct RegisterInfo {
    /// 用户角色
    #[serde(rename = "userAppRole")]
    pub role: String,
    /// 用户密码
    #[serde(rename = "userPassword", serialize_with = "serialize_md5")]
    pub passwd: String,
    /// 用户 Id
    #[serde(rename = "userId")]
    pub user_id: String,
    /// 邀请人用户名
    pub r: Option<String>,
}

impl RegisterInfo {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse RegisterInfo: {}", e)))
    }
}

impl Default for RegisterInfo {
    fn default() -> Self {
        Self {
            role: "0".to_string(),
            passwd: String::new(),
            user_id: String::new(),
            r: None,
        }
    }
}

/// 计算 MD5
fn serialize_md5<S>(passwd: &str, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    let hash = md5::compute(passwd.as_bytes());
    serializer.serialize_str(&format!("{:x}", hash))
}

/// 上传文件信息
#[derive(Clone, Debug, Serialize, Deserialize, Default)]
pub struct FileInfo {
    /// 文件名
    pub filename: String,
    /// 文件地址
    pub url: String,
}

impl FileInfo {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse FileInfo: {}", e)))
    }
}

impl std::fmt::Display for FileInfo {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "FileInfo{{filename={}, url={}}}",
            self.filename, self.url
        )
    }
}

/// 上传结果
#[derive(Clone, Debug, Default)]
pub struct UploadResult {
    /// 上传失败文件
    pub errs: Vec<String>,
    /// 上传成功文件
    pub success: Vec<FileInfo>,
}

impl UploadResult {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        let errs = data["errFiles"]
            .as_array()
            .unwrap_or(&vec![])
            .iter()
            .filter_map(|v| v.as_str().map(|s| s.to_string()))
            .collect();

        let success = if let Some(succ_map) = data["succMap"].as_object() {
            succ_map
                .iter()
                .map(|(filename, url)| FileInfo {
                    filename: filename.clone(),
                    url: url.as_str().unwrap_or("").to_string(),
                })
                .collect()
        } else {
            vec![]
        };

        Ok(UploadResult { errs, success })
    }
}

impl Serialize for UploadResult {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        use serde::ser::SerializeMap;

        let mut map = serializer.serialize_map(Some(2))?;
        map.serialize_entry("errFiles", &self.errs)?;
        let succ_map: std::collections::HashMap<String, String> = self
            .success
            .iter()
            .map(|f| (f.filename.clone(), f.url.clone()))
            .collect();
        map.serialize_entry("succMap", &succ_map)?;
        map.end()
    }
}

impl<'de> Deserialize<'de> for UploadResult {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        #[derive(Deserialize)]
        struct UploadResultHelper {
            #[serde(rename = "errFiles")]
            errs: Vec<String>,
            #[serde(rename = "succMap")]
            succ_map: std::collections::HashMap<String, String>,
        }

        let helper = UploadResultHelper::deserialize(deserializer)?;
        let success = helper
            .succ_map
            .into_iter()
            .map(|(filename, url)| FileInfo { filename, url })
            .collect();

        Ok(UploadResult {
            errs: helper.errs,
            success,
        })
    }
}

impl std::fmt::Display for UploadResult {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let errs_str = self.errs.join(",");
        write!(
            f,
            "UploadResult{{ errFiles={}, succFiles={:?} }}",
            errs_str, self.success
        )
    }
}

/// 最近注册用户信息
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
#[derive(Default)]
pub struct UserLite {
    /// 用户昵称
    #[serde(rename = "userNickname")]
    pub user_nickname: String,
    /// 用户名
    #[serde(rename = "userName")]
    pub user_name: String,
}

impl UserLite {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse UserLite: {}", e)))
    }
}

impl std::fmt::Display for UserLite {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "UserLite{{userNickname: {}, userName: {}}}",
            self.user_nickname, self.user_name
        )
    }
}

use chrono::{DateTime, Utc};

/// 用户 VIP 信息
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
#[derive(Default)]
pub struct UserVipInfo {
    pub joint_vip: bool,
    pub color: String,
    pub underline: bool,
    pub metal: bool,
    pub auto_checkin: u32,
    pub bold: bool,
    pub o_id: String,
    pub state: bool,
    pub user_id: String,
    pub lv_code: String,
    pub expires_at: u64,
    pub created_at: u64,
    pub updated_at: u64,
}

impl UserVipInfo {
    /// 是否为 VIP
    pub fn is_vip(&self) -> bool {
        self.state
    }

    /// 过期日期
    pub fn expires_date(&self) -> DateTime<Utc> {
        DateTime::from_timestamp_millis(self.expires_at as i64).unwrap_or_default()
    }

    /// 创建日期
    pub fn created_date(&self) -> DateTime<Utc> {
        DateTime::from_timestamp_millis(self.created_at as i64).unwrap_or_default()
    }

    /// 更新日期
    pub fn updated_date(&self) -> DateTime<Utc> {
        DateTime::from_timestamp_millis(self.updated_at as i64).unwrap_or_default()
    }

    /// VIP 名称
    pub fn vip_name(&self) -> String {
        self.lv_code
            .replace("_YEAR", "(包年)")
            .replace("_MONTH", "(包月)")
    }

    pub fn from_value(data: &Value) -> Result<Self, Error> {
        let joint_vip = data["jointVip"].as_bool().unwrap_or(false);
        let color = data["color"].as_str().unwrap_or("").to_string();
        let underline = data["underline"].as_bool().unwrap_or(false);
        let metal = data["metal"].as_bool().unwrap_or(false);
        let auto_checkin = data["autoCheckin"].as_u64().unwrap_or(0) as u32;
        let bold = data["bold"].as_bool().unwrap_or(false);
        let o_id = data["oId"].as_str().unwrap_or("").to_string();
        let state = data["state"].as_i64().unwrap_or(0) == 1;
        let user_id = data["userId"].as_str().unwrap_or("").to_string();
        let lv_code = data["lvCode"].as_str().unwrap_or("").to_string();
        let expires_at = data["expiresAt"].as_u64().unwrap_or(0);
        let created_at = data["createdAt"].as_u64().unwrap_or(0);
        let updated_at = data["updatedAt"].as_u64().unwrap_or(0);

        Ok(UserVipInfo {
            joint_vip,
            color,
            underline,
            metal,
            auto_checkin,
            bold,
            o_id,
            state,
            user_id,
            lv_code,
            expires_at,
            created_at,
            updated_at,
        })
    }
}

/// 举报数据类型
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum ReportDataType {
    /// 文章
    Article,
    /// 评论
    Comment,
    /// 用户
    User,
    /// 聊天消息
    Chatroom,
}

impl From<u8> for ReportDataType {
    fn from(value: u8) -> Self {
        match value {
            0 => Self::Article,
            1 => Self::Comment,
            2 => Self::User,
            _ => Self::Chatroom,
        }
    }
}

/// 举报类型
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum ReportType {
    /// 垃圾广告
    Advertise,
    /// 色情
    Porn,
    /// 违规
    Violate,
    /// 侵权
    Infringement,
    /// 人身攻击
    Attacks,
    /// 冒充他人账号
    Impersonate,
    /// 垃圾广告账号
    AdvertisingAccount,
    /// 违规泄露个人信息
    LeakPrivacy,
    /// 其它
    Other,
}

impl From<u8> for ReportType {
    fn from(value: u8) -> Self {
        match value {
            0 => Self::Advertise,
            1 => Self::Porn,
            2 => Self::Violate,
            3 => Self::Infringement,
            4 => Self::Attacks,
            5 => Self::Impersonate,
            6 => Self::AdvertisingAccount,
            7 => Self::LeakPrivacy,
            _ => Self::Other,
        }
    }
}

/// 举报数据
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
pub struct Report {
    /// 举报对象的 oId
    #[serde(rename = "reportDataId")]
    pub report_data_id: String,
    /// 举报数据的类型
    #[serde(rename = "reportDataType")]
    pub report_data_type: ReportDataType,
    /// 举报的类型
    #[serde(rename = "reportType")]
    pub report_type: ReportType,
    /// 举报的理由
    #[serde(rename = "reportMemo")]
    pub report_memo: String,
}

impl Report {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        let report_data_id = data["reportDataId"].as_str().unwrap_or("").to_string();
        let report_data_type =
            ReportDataType::from(data["reportDataType"].as_u64().unwrap_or(3) as u8);
        let report_type = ReportType::from(data["reportType"].as_u64().unwrap_or(0) as u8);
        let report_memo = data["reportMemo"].as_str().unwrap_or("").to_string();

        Ok(Report {
            report_data_id,
            report_data_type,
            report_type,
            report_memo,
        })
    }
}

impl Default for Report {
    fn default() -> Self {
        Self {
            report_data_id: String::new(),
            report_data_type: ReportDataType::Chatroom,
            report_type: ReportType::Advertise,
            report_memo: String::new(),
        }
    }
}

/// 服务器日志
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
#[derive(Default)]
pub struct Log {
    /// 操作时间
    pub key1: String,
    /// IP
    pub key2: String,
    /// 内容
    pub data: String,
    /// 是否公开
    #[serde(rename = "public")]
    pub is_public: bool,
    /// 操作类型
    pub key3: String,
    /// 唯一标识
    pub o_id: String,
    /// 类型
    #[serde(rename = "type")]
    pub type_: String,
}

impl Log {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse Log: {}", e)))
    }
}
