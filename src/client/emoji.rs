use crate::client::core::FishPi;
use crate::domain::emoji::{EmojiGroup, EmojiItem};
use crate::utils::error::Error;
use crate::wire::emoji::{WireEmojiGroup, WireEmojiItem};
use serde_json::json;

/// 表情域访问器。
pub struct EmojiApi(pub(crate) FishPi);

impl FishPi {
    /// 表情域。
    pub fn emoji(&self) -> EmojiApi {
        EmojiApi(self.clone())
    }
}

impl EmojiApi {
    pub async fn groups(&self) -> Result<Vec<EmojiGroup>, Error> {
        let wire: Vec<WireEmojiGroup> = self.0.get_data_with_key("api/emoji/groups").await?;
        Ok(wire.into_iter().map(EmojiGroup::from).collect())
    }

    pub async fn group_items(&self, group_id: impl Into<String>) -> Result<Vec<EmojiItem>, Error> {
        let group_id = group_id.into();
        let path = format!("api/emoji/group/emojis?groupId={}", group_id);
        let wire: Vec<WireEmojiItem> = self.0.get_data_with_key(&path).await?;
        Ok(wire.into_iter().map(EmojiItem::from).collect())
    }

    /// 上传 URL 到「全部」分组。
    pub async fn upload_url(&self, url: impl Into<String>) -> Result<(), Error> {
        let url = url.into();
        self.0
            .post_raw("api/emoji/upload", &json!({ "url": url }))
            .await?;
        Ok(())
    }

    /// 创建分组。
    pub async fn create_group(&self, name: impl Into<String>, sort: i64) -> Result<(), Error> {
        let name = name.into();
        self.0
            .post_raw(
                "api/emoji/group/create",
                &json!({ "name": name, "sort": sort }),
            )
            .await?;
        Ok(())
    }

    /// 更新分组。
    pub async fn update_group(
        &self,
        group_id: impl Into<String>,
        name: impl Into<String>,
        sort: i64,
    ) -> Result<(), Error> {
        let group_id = group_id.into();
        let name = name.into();
        self.0
            .post_raw(
                "api/emoji/group/update",
                &json!({ "groupId": group_id, "name": name, "sort": sort }),
            )
            .await?;
        Ok(())
    }

    pub async fn delete_group(&self, group_id: impl Into<String>) -> Result<(), Error> {
        let group_id = group_id.into();
        self.0
            .post_raw("api/emoji/group/delete", &json!({ "groupId": group_id }))
            .await?;
        Ok(())
    }

    /// 分组添加已有表情。
    pub async fn add(
        &self,
        group_id: impl Into<String>,
        emoji_id: impl Into<String>,
        sort: i64,
        name: Option<&str>,
    ) -> Result<(), Error> {
        let group_id = group_id.into();
        let emoji_id = emoji_id.into();
        let mut data = json!({ "groupId": group_id, "emojiId": emoji_id, "sort": sort });
        if let Some(n) = name {
            data["name"] = json!(n);
        }
        self.0.post_raw("api/emoji/group/add-emoji", &data).await?;
        Ok(())
    }

    /// 分组添加 URL 表情。
    pub async fn add_url(
        &self,
        group_id: impl Into<String>,
        url: impl Into<String>,
        sort: i64,
        name: Option<&str>,
    ) -> Result<(), Error> {
        let group_id = group_id.into();
        let url = url.into();
        let mut data = json!({ "groupId": group_id, "url": url, "sort": sort });
        if let Some(n) = name {
            data["name"] = json!(n);
        }
        self.0
            .post_raw("api/emoji/group/add-url-emoji", &data)
            .await?;
        Ok(())
    }

    pub async fn remove(
        &self,
        group_id: impl Into<String>,
        emoji_id: impl Into<String>,
    ) -> Result<(), Error> {
        let group_id = group_id.into();
        let emoji_id = emoji_id.into();
        self.0
            .post_raw(
                "api/emoji/group/remove-emoji",
                &json!({ "groupId": group_id, "emojiId": emoji_id }),
            )
            .await?;
        Ok(())
    }

    /// 更新表情项。
    pub async fn update(
        &self,
        o_id: impl Into<String>,
        group_id: impl Into<String>,
        name: impl Into<String>,
        sort: i64,
    ) -> Result<(), Error> {
        let o_id = o_id.into();
        let group_id = group_id.into();
        let name = name.into();
        self.0
            .post_raw(
                "api/emoji/emoji/update",
                &json!({ "oId": o_id, "groupId": group_id, "name": name, "sort": sort }),
            )
            .await?;
        Ok(())
    }
}
