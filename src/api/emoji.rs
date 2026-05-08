//! 表情包 V2 API 模块。
//!
//! 该模块负责管理自定义表情包分组和表情项。这里的“表情包”与
//! [`crate::api::reaction`] 中的“给内容贴 emoji reaction”是两套不同能力。

use crate::model::emoji::{EmojiGroup, EmojiItem};
use crate::utils::{build_http_path, error::Error, get_with_body, post};
use serde_json::{Value, json};

pub struct Emoji {
    api_key: String,
}

impl Emoji {
    pub fn new(api_key: String) -> Self {
        Self { api_key }
    }

    /// 获取表情分组列表。
    pub async fn groups(&self) -> Result<Vec<EmojiGroup>, Error> {
        let resp = get_with_body("api/emoji/groups", Some(self.api_key_body())).await?;
        ensure_success(&resp, "Emoji groups API error")?;
        parse_groups(&resp)
    }

    /// 获取指定分组内的表情。
    pub async fn group_emojis(&self, group_id: &str) -> Result<Vec<EmojiItem>, Error> {
        let path = build_http_path(
            "api/emoji/group/emojis",
            &[("groupId", group_id.to_string())],
        );
        let resp = get_with_body(&path, Some(self.api_key_body())).await?;
        ensure_success(&resp, "Emoji group items API error")?;
        parse_items(&resp)
    }

    /// 上传 URL 到“全部”分组。
    pub async fn upload_url(&self, url: &str) -> Result<EmojiItem, Error> {
        let resp = post(
            "api/emoji/upload",
            Some(json!({
                "apiKey": self.api_key,
                "url": url,
            })),
        )
        .await?;
        ensure_success(&resp, "Emoji upload API error")?;
        parse_item(&resp)
    }

    /// 创建分组。
    pub async fn create_group(&self, name: &str, sort: i64) -> Result<EmojiGroup, Error> {
        let resp = post(
            "api/emoji/group/create",
            Some(json!({
                "apiKey": self.api_key,
                "name": name,
                "sort": sort,
            })),
        )
        .await?;
        ensure_success(&resp, "Emoji create group API error")?;
        parse_group(&resp)
    }

    /// 更新分组。
    pub async fn update_group(
        &self,
        group_id: &str,
        name: &str,
        sort: i64,
    ) -> Result<EmojiGroup, Error> {
        let resp = post(
            "api/emoji/group/update",
            Some(json!({
                "apiKey": self.api_key,
                "groupId": group_id,
                "name": name,
                "sort": sort,
            })),
        )
        .await?;
        ensure_success(&resp, "Emoji update group API error")?;
        parse_group(&resp)
    }

    /// 删除分组。
    pub async fn delete_group(&self, group_id: &str) -> Result<(), Error> {
        let resp = post(
            "api/emoji/group/delete",
            Some(json!({
                "apiKey": self.api_key,
                "groupId": group_id,
            })),
        )
        .await?;
        ensure_success(&resp, "Emoji delete group API error")
    }

    /// 分组添加已有表情。
    pub async fn add_emoji(
        &self,
        group_id: &str,
        emoji_id: &str,
        sort: i64,
        name: Option<&str>,
    ) -> Result<EmojiItem, Error> {
        let mut data = json!({
            "apiKey": self.api_key,
            "groupId": group_id,
            "emojiId": emoji_id,
            "sort": sort,
        });
        insert_optional_name(&mut data, name);

        let resp = post("api/emoji/group/add-emoji", Some(data)).await?;
        ensure_success(&resp, "Emoji add item API error")?;
        parse_item(&resp)
    }

    /// 分组添加 URL 表情。
    pub async fn add_url_emoji(
        &self,
        group_id: &str,
        url: &str,
        sort: i64,
        name: Option<&str>,
    ) -> Result<EmojiItem, Error> {
        let mut data = json!({
            "apiKey": self.api_key,
            "groupId": group_id,
            "url": url,
            "sort": sort,
        });
        insert_optional_name(&mut data, name);

        let resp = post("api/emoji/group/add-url-emoji", Some(data)).await?;
        ensure_success(&resp, "Emoji add url item API error")?;
        parse_item(&resp)
    }

    /// 从分组移除表情。若 groupId 为“全部”，服务端会同时从所有分组移除。
    pub async fn remove_emoji(&self, group_id: &str, emoji_id: &str) -> Result<(), Error> {
        let resp = post(
            "api/emoji/group/remove-emoji",
            Some(json!({
                "apiKey": self.api_key,
                "groupId": group_id,
                "emojiId": emoji_id,
            })),
        )
        .await?;
        ensure_success(&resp, "Emoji remove item API error")
    }

    /// 更新表情项（重命名/排序）。
    pub async fn update_emoji(
        &self,
        o_id: &str,
        group_id: &str,
        name: &str,
        sort: i64,
    ) -> Result<EmojiItem, Error> {
        let resp = post(
            "api/emoji/emoji/update",
            Some(json!({
                "apiKey": self.api_key,
                "oId": o_id,
                "groupId": group_id,
                "name": name,
                "sort": sort,
            })),
        )
        .await?;
        ensure_success(&resp, "Emoji update item API error")?;
        parse_item(&resp)
    }

    /// 迁移旧表情到 V2。
    pub async fn migrate(&self) -> Result<Value, Error> {
        let resp = post("api/emoji/emoji/migrate", Some(self.api_key_body())).await?;
        ensure_success(&resp, "Emoji migrate API error")?;
        Ok(resp)
    }

    fn api_key_body(&self) -> Value {
        json!({ "apiKey": self.api_key })
    }
}

fn insert_optional_name(data: &mut Value, name: Option<&str>) {
    if let Some(name) = name
        && !name.is_empty()
        && let Some(object) = data.as_object_mut()
    {
        object.insert("name".to_string(), json!(name));
    }
}

fn ensure_success(resp: &Value, fallback: &str) -> Result<(), Error> {
    if let Some(code) = resp.get("code").and_then(|c| c.as_i64())
        && code != 0
    {
        return Err(Error::Api(
            resp.get("msg")
                .and_then(|m| m.as_str())
                .unwrap_or(fallback)
                .to_string(),
        ));
    }

    Ok(())
}

fn parse_groups(resp: &Value) -> Result<Vec<EmojiGroup>, Error> {
    let values = array_value(resp, &["groups", "list", "items"])?;
    values.iter().map(EmojiGroup::from_value).collect()
}

fn parse_items(resp: &Value) -> Result<Vec<EmojiItem>, Error> {
    let values = array_value(resp, &["emojis", "items", "list"])?;
    values.iter().map(EmojiItem::from_value).collect()
}

fn parse_group(resp: &Value) -> Result<EmojiGroup, Error> {
    let value = object_value(resp, &["group", "item"])?;
    EmojiGroup::from_value(value)
}

fn parse_item(resp: &Value) -> Result<EmojiItem, Error> {
    let value = object_value(resp, &["emoji", "item"])?;
    EmojiItem::from_value(value)
}

fn array_value<'a>(resp: &'a Value, keys: &[&str]) -> Result<&'a Vec<Value>, Error> {
    if let Some(array) = resp.as_array() {
        return Ok(array);
    }

    let data = resp.get("data").unwrap_or(resp);
    if let Some(array) = data.as_array() {
        return Ok(array);
    }

    for container in [data, resp] {
        for key in keys {
            if let Some(array) = container.get(key).and_then(|v| v.as_array()) {
                return Ok(array);
            }
        }
    }

    Err(Error::Parse(
        "Failed to parse emoji list response".to_string(),
    ))
}

fn object_value<'a>(resp: &'a Value, keys: &[&str]) -> Result<&'a Value, Error> {
    let data = resp.get("data").unwrap_or(resp);

    for container in [data, resp] {
        for key in keys {
            if let Some(value) = container.get(key) {
                return Ok(value);
            }
        }
    }

    if data.is_object() && !looks_like_envelope(data) {
        return Ok(data);
    }

    if let Some(array) = data.as_array()
        && let Some(first) = array.first()
    {
        return Ok(first);
    }

    Err(Error::Parse(
        "Failed to parse emoji object response".to_string(),
    ))
}

fn looks_like_envelope(value: &Value) -> bool {
    value.get("code").is_some() || value.get("msg").is_some()
}
