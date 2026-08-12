use reqwest::Method;
use serde::Serialize;
use serde_json::{Value, json};

use crate::client::core::FishPi;
use crate::domain::article::{
    ArticleDetail, ArticleDraftDetail, ArticleDraftSummary, ArticleList, ArticleListType,
    ArticleType, Pagination,
};
use crate::ids::ArticleId;
use crate::utils::build_http_path;
use crate::utils::error::Error;
use crate::wire::article::{
    WireArticleDetail, WireArticleDraftEnvelope, WireArticleDraftList,
    WireArticleDraftSummaryEnvelope, WireArticleList, WirePagination,
};
use crate::ws::{ParsedMessageHandler, RetryPolicy, WsConnection, WsLogHook, build_ws_url};
use std::sync::Arc;

type ArticleHandler = ParsedMessageHandler<ArticleEventKind, Value>;

/// 文章频道事件类别（监听器键）。当前只有一类原始推送。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum ArticleEventKind {
    Message,
}

fn parse_article_message(json: &Value) -> Result<(ArticleEventKind, Value), Error> {
    Ok((ArticleEventKind::Message, json.clone()))
}

/// 发帖信息。
#[derive(Clone, Debug, Serialize)]
pub struct ArticlePost {
    #[serde(rename = "articleTitle")]
    pub title: String,
    #[serde(rename = "articleContent")]
    pub content: String,
    #[serde(rename = "articleTags")]
    pub tags: String,
    #[serde(rename = "articleCommentable")]
    pub commentable: bool,
    #[serde(rename = "articleNotifyFollowers")]
    pub notify_followers: bool,
    #[serde(rename = "articleType")]
    pub type_: ArticleType,
    #[serde(rename = "articleShowInList")]
    pub show_in_list: u32,
    #[serde(
        rename = "articleRewardContent",
        skip_serializing_if = "Option::is_none"
    )]
    pub reward_content: Option<String>,
    #[serde(rename = "articleRewardPoint", skip_serializing_if = "Option::is_none")]
    pub reward_point: Option<String>,
    #[serde(rename = "articleAnonymous", skip_serializing_if = "Option::is_none")]
    pub anonymous: Option<bool>,
    #[serde(
        rename = "articleQnAOfferPoint",
        skip_serializing_if = "Option::is_none"
    )]
    pub offer_point: Option<u32>,
    #[serde(rename = "isGoodArticle", skip_serializing_if = "Option::is_none")]
    pub is_good_article: Option<String>,
}

impl ArticlePost {
    pub fn new(
        title: impl Into<String>,
        content: impl Into<String>,
        tags: impl Into<String>,
    ) -> Self {
        Self {
            title: title.into(),
            content: content.into(),
            tags: tags.into(),
            commentable: true,
            notify_followers: false,
            type_: ArticleType::Normal,
            show_in_list: 1,
            reward_content: None,
            reward_point: None,
            anonymous: None,
            offer_point: None,
            is_good_article: None,
        }
    }
}

/// 文章草稿保存参数。
#[derive(Clone, Debug, Serialize, Default)]
pub struct ArticleDraftSave {
    #[serde(rename = "articleDraftId", skip_serializing_if = "Option::is_none")]
    pub draft_id: Option<String>,
    #[serde(rename = "articleTitle")]
    pub title: String,
    #[serde(rename = "articleContent")]
    pub content: String,
    #[serde(rename = "articleDraftThoughtContent")]
    pub thought_content: String,
    #[serde(rename = "articleTags")]
    pub tags: String,
    #[serde(rename = "articleType")]
    pub article_type: u32,
    #[serde(rename = "columnId")]
    pub column_id: String,
    #[serde(rename = "columnTitle")]
    pub column_title: String,
    #[serde(rename = "chapterNo")]
    pub chapter_no: String,
    #[serde(rename = "articleRewardContent")]
    pub reward_content: String,
    #[serde(rename = "articleRewardPoint")]
    pub reward_point: String,
    #[serde(
        rename = "articleQnAOfferPoint",
        skip_serializing_if = "Option::is_none"
    )]
    pub qna_offer_point: Option<u32>,
    #[serde(rename = "articleCommentable")]
    pub commentable: bool,
    #[serde(rename = "articleAnonymous")]
    pub anonymous: bool,
    #[serde(rename = "articleNotifyFollowers")]
    pub notify_followers: bool,
    #[serde(rename = "articleShowInList")]
    pub show_in_list: u32,
    #[serde(rename = "articleStatement")]
    pub statement: u32,
}

/// 文章域访问器。
pub struct ArticleApi(pub(crate) FishPi);

impl FishPi {
    /// 文章域。
    pub fn article(&self) -> ArticleApi {
        ArticleApi(self.clone())
    }
}

impl ArticleApi {
    /// 发布文章，返回文章 Id。
    pub async fn post(&self, data: &ArticlePost) -> Result<ArticleId, Error> {
        let rsp = self.0.post_raw("article", data).await?;
        take_article_id(&rsp)
    }

    /// 更新文章，返回文章 Id。
    pub async fn update(
        &self,
        id: impl Into<ArticleId>,
        data: &ArticlePost,
    ) -> Result<ArticleId, Error> {
        let id = id.into();
        let rsp = self.0.post_raw(&format!("article/{}", id), data).await?;
        take_article_id(&rsp)
    }

    /// 查询文章列表。
    pub async fn list(
        &self,
        type_: ArticleListType,
        page: u32,
        size: u32,
        tag: Option<&str>,
    ) -> Result<ArticleList, Error> {
        let path = match (tag.map(str::trim).filter(|v| !v.is_empty()), type_) {
            (Some(tag), ArticleListType::Long) => format!("api/articles/tag/{}", tag),
            (Some(tag), _) => format!("api/articles/tag/{}{}", tag, type_.to_code()),
            (None, ArticleListType::Perfect) => {
                return Err(Error::business(
                    -1,
                    "优选帖子列表需要指定标签，请使用 /api/articles/tag/<标签URI>/perfect",
                ));
            }
            (None, _) => format!("api/articles/recent{}", type_.to_code()),
        };
        let url = build_http_path(
            &path,
            &[
                ("p", page.to_string()),
                ("size", size.to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let wire: WireArticleList = self.0.get_data(&url).await?;
        Ok(wire.into())
    }

    /// 查询指定用户的文章列表。
    pub async fn list_by_user(
        &self,
        user: impl Into<String>,
        page: u32,
        size: u32,
    ) -> Result<ArticleList, Error> {
        let user = user.into();
        let url = build_http_path(
            &format!("api/user/{}/articles", user),
            &[
                ("p", page.to_string()),
                ("size", size.to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let wire: WireArticleList = self.0.get_data(&url).await?;
        Ok(wire.into())
    }

    /// 获取文章详情，`p` 为评论页码。
    pub async fn detail(&self, id: impl Into<ArticleId>, p: u32) -> Result<ArticleDetail, Error> {
        let id = id.into();
        let url = build_http_path(
            &format!("api/article/{}", id),
            &[
                ("p", p.to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let rsp = self.0.get_raw(&url).await?;
        let data = &rsp["data"];
        let wire: WireArticleDetail = serde_json::from_value(data["article"].clone())
            .map_err(|e| Error::decode(e.to_string()))?;
        let mut detail: ArticleDetail = wire.into();
        let wire_pagination: WirePagination =
            serde_json::from_value(data["pagination"].clone()).unwrap_or_default();
        detail.pagination = Some(Pagination::from(wire_pagination));
        Ok(detail)
    }

    /// 获取文章 Markdown 原文。
    pub async fn markdown_source(&self, id: impl Into<ArticleId>) -> Result<String, Error> {
        let id = id.into();
        self.0
            .get_text(&self.0.with_key(&format!("api/article/md/{}", id)))
            .await
    }
}

fn take_article_id(rsp: &Value) -> Result<ArticleId, Error> {
    rsp["articleId"]
        .as_str()
        .map(ArticleId::from)
        .ok_or_else(|| Error::decode("Missing articleId in response"))
}

impl ArticleApi {
    /// 查询当前用户的文章草稿列表。
    pub async fn list_drafts(&self) -> Result<Vec<ArticleDraftSummary>, Error> {
        let url = build_http_path(
            "api/article-drafts",
            &[("apiKey", self.0.api_key().to_string())],
        );
        let wire: WireArticleDraftList = self.0.get_data(&url).await?;
        Ok(wire
            .drafts
            .into_iter()
            .map(ArticleDraftSummary::from)
            .collect())
    }

    /// 新建或更新当前用户的文章草稿。
    pub async fn save_draft(&self, data: &ArticleDraftSave) -> Result<ArticleDraftSummary, Error> {
        let wire: WireArticleDraftSummaryEnvelope =
            self.0.post_data("api/article-drafts", data).await?;
        Ok(wire.draft.into())
    }

    /// 查询指定文章草稿详情。
    pub async fn draft_detail(&self, id: impl Into<String>) -> Result<ArticleDraftDetail, Error> {
        let id = id.into();
        let url = build_http_path(
            &format!("api/article-drafts/{}", id),
            &[("apiKey", self.0.api_key().to_string())],
        );
        let wire: WireArticleDraftEnvelope = self.0.get_data(&url).await?;
        Ok(wire.draft.into())
    }

    /// 删除指定文章草稿，返回被删除草稿 Id。
    pub async fn delete_draft(&self, id: impl Into<String>) -> Result<String, Error> {
        let id = id.into();
        let url = build_http_path(
            &format!("api/article-drafts/{}", id),
            &[("apiKey", self.0.api_key().to_string())],
        );
        let rsp = self.0.send_raw(Method::DELETE, &url, None).await?;
        if rsp.get("code").and_then(Value::as_i64).unwrap_or(-1) != 0 {
            return Err(Error::business(
                rsp.get("code").and_then(Value::as_i64).unwrap_or(-1),
                rsp["msg"].as_str().unwrap_or("API error"),
            ));
        }
        Ok(rsp["data"]["id"].as_str().unwrap_or(&id).to_string())
    }

    /// 点赞/点踩文章，返回是否点赞。
    pub async fn vote(&self, id: impl Into<ArticleId>, like: bool) -> Result<bool, Error> {
        let id = id.into();
        let url = format!("vote/{}/article", if like { "up" } else { "down" });
        let rsp = self
            .0
            .post_raw(&url, &json!({ "dataId": id.as_str() }))
            .await?;
        Ok(rsp.get("type").and_then(Value::as_i64) == Some(-1))
    }

    /// 感谢文章。
    pub async fn thank(&self, id: impl Into<ArticleId>) -> Result<(), Error> {
        let url = build_http_path("article/thank", &[("articleId", id.into().into_string())]);
        self.0.post_raw(&url, &json!({})).await?;
        Ok(())
    }

    /// 收藏文章。
    pub async fn follow(&self, id: impl Into<ArticleId>) -> Result<(), Error> {
        let id = id.into();
        self.0
            .post_raw("follow/article", &json!({ "followingId": id.as_str() }))
            .await?;
        Ok(())
    }

    /// 取消收藏文章。
    pub async fn unfollow(&self, id: impl Into<ArticleId>) -> Result<(), Error> {
        let id = id.into();
        self.0
            .post_raw("unfollow/article", &json!({ "followingId": id.as_str() }))
            .await?;
        Ok(())
    }

    /// 关注/取消关注文章。
    pub async fn watch(&self, following_id: impl Into<ArticleId>) -> Result<(), Error> {
        let following_id = following_id.into();
        self.0
            .post_raw(
                "follow/article-watch",
                &json!({ "followingId": following_id.as_str() }),
            )
            .await?;
        Ok(())
    }

    /// 打赏文章。
    pub async fn reward(&self, id: impl Into<ArticleId>) -> Result<(), Error> {
        let url = build_http_path("article/reward", &[("articleId", id.into().into_string())]);
        self.0.post_raw(&url, &json!({})).await?;
        Ok(())
    }

    /// 获取文章在线人数。
    pub async fn heat(&self, id: impl Into<ArticleId>) -> Result<u32, Error> {
        let id = id.into();
        let rsp = self
            .0
            .post_raw(&format!("api/article/heat/{}", id), &json!({}))
            .await?;
        let heat = rsp
            .get("articleHeat")
            .or_else(|| rsp.get("data").and_then(|d| d.get("articleHeat")))
            .and_then(Value::as_u64)
            .ok_or_else(|| Error::decode("Missing heat data in response"))?;
        Ok(heat as u32)
    }

    /// 建立文章频道长连接（在线人数、评论等实时推送），返回连接句柄。
    pub async fn connect(
        &self,
        id: impl Into<ArticleId>,
        article_type: ArticleType,
    ) -> Result<ArticleConnection, Error> {
        let url = build_ws_url(
            self.0.domain(),
            "article-channel",
            &[
                ("articleId", id.into().into_string()),
                ("articleType", (article_type as u8).to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        )
        .map_err(Error::from)?;
        let handler = ArticleHandler::new(parse_article_message, None, "article");
        let mut connection = WsConnection::new();
        connection
            .connect(false, &url, handler.clone())
            .await
            .map_err(Error::from)?;
        Ok(ArticleConnection {
            connection,
            handler,
            url,
        })
    }
}

/// 文章频道长连接：监听原始推送、断开重连。
pub struct ArticleConnection {
    connection: WsConnection,
    handler: ArticleHandler,
    url: String,
}

impl ArticleConnection {
    /// 监听文章频道推送（原始 JSON）。
    pub async fn on_message<F>(&self, listener: F)
    where
        F: Fn(Value) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(ArticleEventKind::Message, listener)
            .await;
    }

    /// 重连。
    pub async fn reconnect(&mut self) -> Result<(), Error> {
        self.connection
            .reconnect(&self.url, self.handler.clone())
            .await
            .map_err(Error::from)
    }

    /// 设置重连策略。
    pub fn set_retry_policy(&mut self, policy: RetryPolicy) {
        self.connection.set_retry_policy(policy);
    }

    /// 设置日志钩子。
    pub fn set_log_hook<F>(&mut self, hook: F)
    where
        F: Fn(&str) + Send + Sync + 'static,
    {
        let hook = Arc::new(hook) as WsLogHook;
        self.connection.set_log_hook_arc(hook.clone());
        self.handler.set_log_hook_arc(hook);
    }

    /// 断开连接。
    pub fn disconnect(&mut self) {
        self.connection.disconnect();
    }
}
