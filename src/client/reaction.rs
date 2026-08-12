use crate::client::core::FishPi;
use crate::domain::reaction::ReactionMutationResult;
use crate::ids::{ArticleId, CommentId, MessageId};
use crate::utils::error::Error;
use crate::wire::reaction::WireReactionMutationResult;
use serde_json::json;

/// 表态域访问器。
pub struct ReactionApi(pub(crate) FishPi);

impl FishPi {
    /// 表态域。
    pub fn reaction(&self) -> ReactionApi {
        ReactionApi(self.clone())
    }
}

impl ReactionApi {
    pub async fn article(
        &self,
        article_id: impl Into<ArticleId>,
        value: impl Into<String>,
    ) -> Result<ReactionMutationResult, Error> {
        let article_id = article_id.into();
        let value = value.into();
        let wire: WireReactionMutationResult = self
            .0
            .post_data(
                "article/reaction",
                &json!({
                    "articleId": article_id.as_str(),
                    "groupType": "emoji",
                    "value": value,
                }),
            )
            .await?;
        Ok(wire.into())
    }

    pub async fn comment(
        &self,
        comment_id: impl Into<CommentId>,
        value: impl Into<String>,
    ) -> Result<ReactionMutationResult, Error> {
        let comment_id = comment_id.into();
        let value = value.into();
        let wire: WireReactionMutationResult = self
            .0
            .post_data(
                "comment/reaction",
                &json!({
                    "commentId": comment_id.as_str(),
                    "groupType": "emoji",
                    "value": value,
                }),
            )
            .await?;
        Ok(wire.into())
    }

    pub async fn chat_room(
        &self,
        o_id: impl Into<MessageId>,
        value: impl Into<String>,
    ) -> Result<ReactionMutationResult, Error> {
        let o_id = o_id.into();
        let value = value.into();
        let wire: WireReactionMutationResult = self
            .0
            .post_data(
                "chat-room/reaction",
                &json!({
                    "oId": o_id.as_str(),
                    "groupType": "emoji",
                    "value": value,
                }),
            )
            .await?;
        Ok(wire.into())
    }
}
