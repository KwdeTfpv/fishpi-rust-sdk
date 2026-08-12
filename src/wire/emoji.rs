use crate::wire::de::string_lossy;
use serde::Deserialize;

/// 表情分组。服务端 `type==1` 表示「全部」默认分组。
#[derive(Clone, Debug, Deserialize)]
pub struct WireEmojiGroup {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub name: String,
    #[serde(default)]
    pub sort: i64,
    #[serde(rename = "type", default)]
    pub type_: i64,
}

/// 分组内表情项。
#[derive(Clone, Debug, Deserialize)]
pub struct WireEmojiItem {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "emojiId", default, deserialize_with = "string_lossy")]
    pub emoji_id: String,
    #[serde(rename = "groupId", default, deserialize_with = "string_lossy")]
    pub group_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub url: String,
    #[serde(default)]
    pub sort: i64,
}
