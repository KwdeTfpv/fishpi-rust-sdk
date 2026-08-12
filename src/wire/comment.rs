use crate::wire::de::string_lossy;
use serde::Deserialize;

#[derive(Clone, Debug, Deserialize)]
pub struct WireCommentUpdate {
    #[serde(rename = "commentContent", default, deserialize_with = "string_lossy")]
    pub comment_content: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireCommentVote {
    #[serde(rename = "type", default)]
    pub vote_type: Option<i64>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireCommentRemove {
    #[serde(rename = "commentId", default, deserialize_with = "string_lossy")]
    pub comment_id: String,
}
