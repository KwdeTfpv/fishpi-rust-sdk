//! 清风明月响应的 wire 层类型。

use serde::Deserialize;

use super::de::string_lossy;

/// 清风明月内容。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireBreezemoon {
    #[serde(
        rename = "breezemoonAuthorName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub author_name: String,
    #[serde(
        rename = "breezemoonUpdated",
        default,
        deserialize_with = "string_lossy"
    )]
    pub updated: String,
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(
        rename = "breezemoonCreated",
        default,
        deserialize_with = "string_lossy"
    )]
    pub created: String,
    #[serde(
        rename = "breezemoonAuthorThumbnailURL48",
        default,
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url48: String,
    #[serde(rename = "timeAgo", default, deserialize_with = "string_lossy")]
    pub time_ago: String,
    #[serde(
        rename = "breezemoonContent",
        default,
        deserialize_with = "string_lossy"
    )]
    pub content: String,
    #[serde(
        rename = "breezemoonCreateTime",
        default,
        deserialize_with = "string_lossy"
    )]
    pub create_time: String,
    #[serde(rename = "breezemoonCity", default, deserialize_with = "string_lossy")]
    pub city: String,
}

/// 清风明月列表响应的 `data`。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireBreezemoonList {
    #[serde(default)]
    pub breezemoons: Vec<WireBreezemoon>,
}
