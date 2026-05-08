use crate::utils::error::Error;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;

/// 表情包分组。
#[derive(Clone, Debug, Default, Serialize, PartialEq)]
#[allow(non_snake_case)]
pub struct EmojiGroup {
    /// 分组 ID。
    pub oId: String,
    /// 分组名称。
    pub name: String,
    /// 排序值。
    pub sort: i64,
    /// 是否为“全部”等默认分组。
    pub isDefault: bool,
    /// 分组内表情数量。
    pub emojiCnt: u64,
    /// 兼容服务端新增字段。
    #[serde(flatten)]
    pub extra: HashMap<String, Value>,
}

impl EmojiGroup {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        let object = value.as_object().ok_or_else(|| {
            Error::Parse("Failed to parse EmojiGroup: expected object".to_string())
        })?;

        Ok(Self {
            oId: pick_string(value, &["oId", "groupId", "id", "fileId"]),
            name: pick_string(value, &["name", "groupName"]),
            sort: pick_i64(value, &["sort"]),
            isDefault: pick_bool(value, &["isDefault", "default", "isAll"]),
            emojiCnt: pick_u64(value, &["emojiCnt", "count", "emojiCount"]),
            extra: object.clone().into_iter().collect(),
        })
    }
}

impl<'de> Deserialize<'de> for EmojiGroup {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = Value::deserialize(deserializer)?;
        EmojiGroup::from_value(&value).map_err(serde::de::Error::custom)
    }
}

/// 表情包条目。
#[derive(Clone, Debug, Default, Serialize, PartialEq)]
#[allow(non_snake_case)]
pub struct EmojiItem {
    /// 条目 ID。
    pub oId: String,
    /// 所属分组 ID。
    pub groupId: String,
    /// 表情名称。
    pub name: String,
    /// 表情图片 URL。
    pub url: String,
    /// 排序值。
    pub sort: i64,
    /// 兼容服务端新增字段。
    #[serde(flatten)]
    pub extra: HashMap<String, Value>,
}

impl EmojiItem {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        let object = value.as_object().ok_or_else(|| {
            Error::Parse("Failed to parse EmojiItem: expected object".to_string())
        })?;

        Ok(Self {
            oId: pick_string(value, &["oId", "emojiId", "id", "fileId"]),
            groupId: pick_string(value, &["groupId", "gid"]),
            name: pick_string(value, &["name", "fileName"]),
            url: pick_string(
                value,
                &[
                    "url", "emojiURL", "emojiUrl", "imageURL", "imageUrl", "src", "fileURL",
                    "fileUrl",
                ],
            ),
            sort: pick_i64(value, &["sort"]),
            extra: object.clone().into_iter().collect(),
        })
    }
}

impl<'de> Deserialize<'de> for EmojiItem {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = Value::deserialize(deserializer)?;
        EmojiItem::from_value(&value).map_err(serde::de::Error::custom)
    }
}

fn pick_string(value: &Value, keys: &[&str]) -> String {
    keys.iter()
        .find_map(|key| value.get(key).and_then(Value::as_str))
        .unwrap_or_default()
        .to_string()
}

fn pick_i64(value: &Value, keys: &[&str]) -> i64 {
    keys.iter()
        .find_map(|key| {
            let value = value.get(key)?;
            value
                .as_i64()
                .or_else(|| value.as_u64().and_then(|v| i64::try_from(v).ok()))
                .or_else(|| value.as_str()?.parse::<i64>().ok())
        })
        .unwrap_or_default()
}

fn pick_u64(value: &Value, keys: &[&str]) -> u64 {
    keys.iter()
        .find_map(|key| {
            let value = value.get(key)?;
            value
                .as_u64()
                .or_else(|| value.as_i64().and_then(|v| u64::try_from(v).ok()))
                .or_else(|| value.as_str()?.parse::<u64>().ok())
        })
        .unwrap_or_default()
}

fn pick_bool(value: &Value, keys: &[&str]) -> bool {
    keys.iter()
        .find_map(|key| {
            let value = value.get(key)?;
            value.as_bool().or_else(|| {
                value.as_i64().map(|v| v != 0).or_else(|| {
                    value.as_str().map(|v| {
                        matches!(v.trim().to_ascii_lowercase().as_str(), "true" | "1" | "yes")
                    })
                })
            })
        })
        .unwrap_or(false)
}

#[cfg(test)]
mod tests {
    use super::EmojiItem;
    use serde_json::json;

    #[test]
    fn emoji_item_allows_duplicate_id_sources() {
        let item = EmojiItem::from_value(&json!({
            "oId": "item-o-id",
            "fileId": "file-id",
            "id": "plain-id",
            "url": "https://file.fishpi.cn/a.gif",
            "name": "动图",
        }))
        .expect("emoji item should parse");

        assert_eq!(item.oId, "item-o-id");
        assert_eq!(item.url, "https://file.fishpi.cn/a.gif");
    }
}
