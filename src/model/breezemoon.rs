use serde::Deserialize;
use serde_json::Value;

use crate::utils::error::Error;

/// 清风明月内容
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct BreezemoonContent {
    /// 发布者用户名
    #[serde(rename = "breezemoonAuthorName")]
    pub authorName: String,
    /// 最后更新时间
    #[serde(rename = "breezemoonUpdated")]
    pub updated: String,
    /// 清风明月ID
    pub oId: String,
    /// 创建时间
    #[serde(rename = "breezemoonCreated")]
    pub created: String,
    /// 发布者头像URL
    #[serde(rename = "breezemoonAuthorThumbnailURL48")]
    pub thumbnailURL48: String,
    /// 发布时间
    pub timeAgo: String,
    /// 正文
    #[serde(rename = "breezemoonContent")]
    pub content: String,
    /// 创建时间
    #[serde(rename = "breezemoonCreateTime")]
    pub createTime: String,
    /// 发布城市（可能为空）
    #[serde(rename = "breezemoonCity")]
    pub city: String,
}

impl BreezemoonContent {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse BreezemoonContent: {}", e)))
    }
}
