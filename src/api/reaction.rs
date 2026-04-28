//! Emoji Reaction API 模块。
//!
//! 该模块提供帖子、评论、聊天室消息的贴 emoji 能力。再次发送相同 value 表示取消，发送不同 value 表示切换。

use crate::model::reaction::ReactionMutationResult;
use crate::utils::{error::Error, post};
use serde_json::{Value, json};

pub struct Reaction {
    api_key: String,
}

impl Reaction {
    pub fn new(api_key: String) -> Self {
        Self { api_key }
    }

    /// 给帖子添加/切换/取消 emoji reaction。
    pub async fn article(
        &self,
        article_id: &str,
        value: &str,
    ) -> Result<ReactionMutationResult, Error> {
        self.post_reaction(
            "article/reaction",
            json!({
                "apiKey": self.api_key,
                "articleId": article_id,
                "groupType": "emoji",
                "value": value,
            }),
        )
        .await
    }

    /// 给评论添加/切换/取消 emoji reaction。
    pub async fn comment(
        &self,
        comment_id: &str,
        value: &str,
    ) -> Result<ReactionMutationResult, Error> {
        self.post_reaction(
            "comment/reaction",
            json!({
                "apiKey": self.api_key,
                "commentId": comment_id,
                "groupType": "emoji",
                "value": value,
            }),
        )
        .await
    }

    /// 给聊天室消息添加/切换/取消 emoji reaction。
    pub async fn chat_room(
        &self,
        o_id: &str,
        value: &str,
    ) -> Result<ReactionMutationResult, Error> {
        self.post_reaction(
            "chat-room/reaction",
            json!({
                "apiKey": self.api_key,
                "oId": o_id,
                "groupType": "emoji",
                "value": value,
            }),
        )
        .await
    }

    async fn post_reaction(
        &self,
        path: &str,
        data: Value,
    ) -> Result<ReactionMutationResult, Error> {
        let resp = post(path, Some(data)).await?;

        if let Some(code) = resp["code"].as_i64()
            && code != 0
        {
            return Err(Error::Api(
                resp["msg"]
                    .as_str()
                    .unwrap_or("Reaction API error")
                    .to_string(),
            ));
        }

        ReactionMutationResult::from_value(&resp["data"])
    }
}
