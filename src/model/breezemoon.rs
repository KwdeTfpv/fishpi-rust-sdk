use serde::{Deserialize, Deserializer};
use serde_json::Value;

use crate::utils::error::Error;

/// 清风明月内容
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct BreezemoonContent {
    /// 发布者用户名
    #[serde(rename = "breezemoonAuthorName")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub authorName: String,
    /// 最后更新时间
    #[serde(rename = "breezemoonUpdated")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub updated: String,
    /// 清风明月ID
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub oId: String,
    /// 创建时间
    #[serde(rename = "breezemoonCreated")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub created: String,
    /// 发布者头像URL
    #[serde(rename = "breezemoonAuthorThumbnailURL48")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub thumbnailURL48: String,
    /// 发布时间
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub timeAgo: String,
    /// 正文
    #[serde(rename = "breezemoonContent")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub content: String,
    /// 创建时间
    #[serde(rename = "breezemoonCreateTime")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub createTime: String,
    /// 发布城市（可能为空）
    #[serde(rename = "breezemoonCity")]
    #[serde(default, deserialize_with = "de_string_lossy")]
    pub city: String,
}

impl BreezemoonContent {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse BreezemoonContent: {}", e)))
    }
}

fn de_string_lossy<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    let v = Value::deserialize(deserializer)?;
    Ok(match v {
        Value::String(s) => s,
        Value::Number(n) => n.to_string(),
        Value::Bool(b) => b.to_string(),
        Value::Null => String::new(),
        other => other.to_string(),
    })
}
