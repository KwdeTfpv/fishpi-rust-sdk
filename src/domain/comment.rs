use crate::ids::ArticleId;
use serde::Serialize;

/// 发布评论入参。
#[derive(Clone, Debug, Serialize)]
pub struct CommentPost {
    #[serde(rename = "articleId")]
    pub article_id: ArticleId,
    #[serde(rename = "commentAnonymous")]
    pub anonymous: bool,
    #[serde(rename = "commentVisible")]
    pub visible: bool,
    #[serde(rename = "commentContent")]
    pub content: String,
    #[serde(rename = "commentOriginalCommentId")]
    pub reply_id: String,
}

impl CommentPost {
    pub fn new(article_id: impl Into<ArticleId>, content: impl Into<String>) -> Self {
        Self {
            article_id: article_id.into(),
            anonymous: false,
            visible: false,
            content: content.into(),
            reply_id: String::new(),
        }
    }

    pub fn with_anonymous(mut self, anonymous: bool) -> Self {
        self.anonymous = anonymous;
        self
    }

    pub fn with_visible(mut self, visible: bool) -> Self {
        self.visible = visible;
        self
    }

    pub fn with_reply_id(mut self, reply_id: impl Into<String>) -> Self {
        self.reply_id = reply_id.into();
        self
    }
}
