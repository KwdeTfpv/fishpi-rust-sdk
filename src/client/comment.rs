use crate::client::core::FishPi;
use crate::domain::comment::CommentPost;
use crate::ids::CommentId;
use crate::utils::error::Error;
use crate::wire::comment::{WireCommentRemove, WireCommentUpdate, WireCommentVote};
use serde_json::json;

/// 评论域访问器。
pub struct CommentApi(pub(crate) FishPi);

impl FishPi {
    /// 评论域。
    pub fn comment(&self) -> CommentApi {
        CommentApi(self.clone())
    }
}

impl CommentApi {
    pub async fn post(&self, data: CommentPost) -> Result<(), Error> {
        self.0.post_raw("comment", &data).await?;
        Ok(())
    }

    pub async fn update(
        &self,
        id: impl Into<CommentId>,
        data: CommentPost,
    ) -> Result<String, Error> {
        let id = id.into();
        let value = self.0.put_raw(&format!("comment/{}", id), &data).await?;
        let wire: WireCommentUpdate =
            serde_json::from_value(value).map_err(|e| Error::decode(e.to_string()))?;
        Ok(wire.comment_content)
    }

    pub async fn vote(&self, id: impl Into<CommentId>, like: bool) -> Result<bool, Error> {
        let id = id.into();
        let action = if like { "up" } else { "down" };
        let value = self
            .0
            .post_raw(
                &format!("vote/{}/comment", action),
                &json!({ "dataId": id.as_str() }),
            )
            .await?;
        let wire: WireCommentVote =
            serde_json::from_value(value).map_err(|e| Error::decode(e.to_string()))?;
        let previous_vote = wire.vote_type.unwrap_or(-1);
        Ok(like && previous_vote != 0)
    }

    pub async fn thank(&self, id: impl Into<CommentId>) -> Result<(), Error> {
        let id = id.into();
        self.0
            .post_raw("comment/thank", &json!({ "commentId": id.as_str() }))
            .await?;
        Ok(())
    }

    pub async fn remove(&self, id: impl Into<CommentId>) -> Result<String, Error> {
        let id = id.into();
        let value = self
            .0
            .post_raw(&format!("comment/{}/remove", id), &json!({}))
            .await?;
        let wire: WireCommentRemove =
            serde_json::from_value(value).map_err(|e| Error::decode(e.to_string()))?;
        Ok(wire.comment_id)
    }
}
