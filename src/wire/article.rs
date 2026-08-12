//! 文章相关响应的 wire 层类型。

use serde::{Deserialize, Deserializer};
use serde_json::Value;

use super::de::{bool_from_int_or_bool, f64_lenient, string_lossy};
use super::user::{WireMetal, metals};

fn u64_lenient<'de, D>(deserializer: D) -> Result<u64, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(match value {
        Value::Number(n) => n
            .as_u64()
            .or_else(|| n.as_i64().map(|v| v.max(0) as u64))
            .or_else(|| {
                n.as_f64().map(|f| {
                    if f.is_finite() && f > 0.0 {
                        f.trunc() as u64
                    } else {
                        0
                    }
                })
            })
            .unwrap_or(0),
        Value::String(s) => s.trim().parse::<i64>().unwrap_or(0).max(0) as u64,
        _ => 0,
    })
}

/// 宽松解析 `u32` 数组。
fn u32_vec_lenient<'de, D>(deserializer: D) -> Result<Vec<u32>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<Value>::deserialize(deserializer)?;
    let Some(Value::Array(items)) = value else {
        return Ok(Vec::new());
    };
    Ok(items
        .into_iter()
        .filter_map(|v| match v {
            Value::Number(n) => n
                .as_u64()
                .or_else(|| n.as_f64().map(|f| f.max(0.0).trunc() as u64)),
            Value::String(s) => s.trim().parse::<u64>().ok(),
            _ => None,
        })
        .map(|n| n as u32)
        .collect())
}

fn bool_from_zero<'de, D>(deserializer: D) -> Result<bool, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(match value {
        Value::Number(n) => n.as_i64().map(|v| v == 0).unwrap_or(true),
        Value::Bool(b) => !b,
        Value::Null => true,
        _ => false,
    })
}

fn opt_vote_index<'de, D>(deserializer: D) -> Result<Option<i64>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(match value {
        Value::Number(n) => n.as_i64().or_else(|| n.as_f64().map(|f| f.trunc() as i64)),
        Value::String(s) => s.trim().parse::<i64>().ok(),
        _ => None,
    })
}

fn default_type_index() -> i64 {
    4
}

fn article_type_index<'de, D>(deserializer: D) -> Result<i64, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(match value {
        Value::Number(n) => n
            .as_i64()
            .or_else(|| n.as_f64().map(|f| f.trunc() as i64))
            .unwrap_or(4),
        Value::String(s) => match s.trim() {
            "Normal" | "normal" => 0,
            "Private" | "private" => 1,
            "Broadcast" | "broadcast" => 2,
            "Thought" | "thought" => 3,
            "Question" | "question" => 5,
            other => other.parse::<i64>().unwrap_or(4),
        },
        _ => 4,
    })
}

/// 文章标签。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleTag {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "tagTitle", default, deserialize_with = "string_lossy")]
    pub title: String,
    #[serde(rename = "tagDescription", default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(rename = "tagIconPath", default, deserialize_with = "string_lossy")]
    pub icon_path: String,
    #[serde(rename = "tagURI", default, deserialize_with = "string_lossy")]
    pub uri: String,
    #[serde(rename = "tagCSS", default, deserialize_with = "string_lossy")]
    pub diy_css: String,
    #[serde(rename = "tagBadCnt", default, deserialize_with = "u64_lenient")]
    pub bad_count: u64,
    #[serde(rename = "tagCommentCount", default, deserialize_with = "u64_lenient")]
    pub comment_count: u64,
    #[serde(rename = "tagFollowerCount", default, deserialize_with = "u64_lenient")]
    pub follower_count: u64,
    #[serde(rename = "tagGoodCnt", default, deserialize_with = "u64_lenient")]
    pub good_count: u64,
    #[serde(
        rename = "tagReferenceCount",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub reference_count: u64,
    #[serde(rename = "tagLinkCount", default, deserialize_with = "u64_lenient")]
    pub link_count: u64,
    #[serde(rename = "tagSeoDesc", default, deserialize_with = "string_lossy")]
    pub seo_desc: String,
    #[serde(rename = "tagSeoKeywords", default, deserialize_with = "string_lossy")]
    pub seo_keywords: String,
    #[serde(rename = "tagSeoTitle", default, deserialize_with = "string_lossy")]
    pub seo_title: String,
    #[serde(rename = "tagAd", default, deserialize_with = "string_lossy")]
    pub tag_ad: String,
    #[serde(rename = "tagShowSideAd", default, deserialize_with = "u64_lenient")]
    pub show_side_ad: u64,
    #[serde(rename = "tagStatus", default, deserialize_with = "u64_lenient")]
    pub status: u64,
    #[serde(rename = "tagRandomDouble", default, deserialize_with = "f64_lenient")]
    pub random_double: f64,
}

/// 分页信息。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WirePagination {
    #[serde(
        rename = "paginationPageCount",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub count: u64,
    #[serde(
        rename = "paginationPageNums",
        default,
        deserialize_with = "u32_vec_lenient"
    )]
    pub page_nums: Vec<u32>,
}

/// 文章/评论作者。
#[derive(Clone, Debug, Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
pub struct WireArticleAuthor {
    #[serde(rename = "oId", deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "userName", deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(deserialize_with = "string_lossy")]
    pub nickname: String,
    #[serde(rename = "avatarURL", deserialize_with = "string_lossy")]
    pub avatar_url: String,
    #[serde(deserialize_with = "bool_from_int_or_bool")]
    pub is_online: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub online_minute: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub point_status: bool,
    #[serde(deserialize_with = "bool_from_zero")]
    pub follower_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub guide_step: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub online_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub current_checkin_streak_start: u64,
    #[serde(deserialize_with = "bool_from_int_or_bool")]
    pub is_auto_blur: bool,
    #[serde(deserialize_with = "string_lossy")]
    pub tags: String,
    #[serde(deserialize_with = "bool_from_zero")]
    pub comment_status: bool,
    #[serde(deserialize_with = "string_lossy")]
    pub timezone: String,
    #[serde(deserialize_with = "string_lossy")]
    pub home_page: String,
    #[serde(deserialize_with = "bool_from_int_or_bool")]
    pub is_enable_forward_page: bool,
    #[serde(rename = "userUAStatus", deserialize_with = "bool_from_zero")]
    pub user_ua_status: bool,
    #[serde(rename = "userIndexRedirectURL", deserialize_with = "string_lossy")]
    pub user_index_redirect_url: String,
    #[serde(deserialize_with = "u64_lenient")]
    pub latest_article_time: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub tag_count: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub list_view_mode: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub longest_checkin_streak: u64,
    #[serde(deserialize_with = "string_lossy")]
    pub avatar_type: String,
    #[serde(deserialize_with = "u64_lenient")]
    pub sub_mail_send_time: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub update_time: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub sub_mail_status: bool,
    #[serde(deserialize_with = "bool_from_zero")]
    pub is_join_point_rank: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub latest_login_time: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub user_app_role: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub user_avatar_view_mode: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub user_status: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub longest_checkin_streak_end: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub watching_article_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub latest_comment_time: u64,
    #[serde(deserialize_with = "string_lossy")]
    pub province: String,
    #[serde(deserialize_with = "u64_lenient")]
    pub current_checkin_streak: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub user_no: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub following_tag_status: bool,
    #[serde(deserialize_with = "string_lossy")]
    pub user_language: String,
    #[serde(deserialize_with = "bool_from_zero")]
    pub is_join_used_point_rank: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub current_checkin_streak_end: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub following_article_status: bool,
    #[serde(deserialize_with = "bool_from_zero")]
    pub keyboard_shortcuts_status: bool,
    #[serde(deserialize_with = "bool_from_zero")]
    pub reply_watch_article_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub comment_view_mode: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub breezemoon_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub user_checkin_time: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub used_point: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub article_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub user_point: u64,
    #[serde(deserialize_with = "u64_lenient")]
    pub comment_count: u64,
    #[serde(deserialize_with = "string_lossy")]
    pub user_intro: String,
    #[serde(deserialize_with = "string_lossy")]
    pub user_mobile_skin: String,
    #[serde(deserialize_with = "u64_lenient")]
    pub list_page_size: u64,
    #[serde(deserialize_with = "bool_from_zero")]
    pub geo_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub longest_checkin_streak_start: u64,
    #[serde(deserialize_with = "string_lossy")]
    pub user_skin: String,
    #[serde(deserialize_with = "bool_from_zero")]
    pub notify_status: bool,
    #[serde(deserialize_with = "bool_from_zero")]
    pub following_user_status: bool,
    #[serde(deserialize_with = "u64_lenient")]
    pub article_count: u64,
    #[serde(deserialize_with = "string_lossy")]
    pub user_role: String,
    #[serde(rename = "sysMetal", deserialize_with = "metals")]
    pub sys_metal: Vec<WireMetal>,
}

/// 文章评论。
#[derive(Clone, Debug, Deserialize, Default)]
#[serde(default)]
pub struct WireArticleComment {
    #[serde(rename = "commentNice", deserialize_with = "bool_from_int_or_bool")]
    pub is_nice: bool,
    #[serde(rename = "commentCreateTimeStr", deserialize_with = "string_lossy")]
    pub create_time_str: String,
    #[serde(rename = "commentAuthorId", deserialize_with = "string_lossy")]
    pub author_id: String,
    #[serde(default, deserialize_with = "u64_lenient")]
    pub score: u64,
    #[serde(rename = "commentCreateTime", deserialize_with = "string_lossy")]
    pub create_time: String,
    #[serde(rename = "commentAuthorURL", deserialize_with = "string_lossy")]
    pub author_url: String,
    #[serde(rename = "commentVote", deserialize_with = "opt_vote_index")]
    pub vote: Option<i64>,
    #[serde(rename = "commentRevisionCount", deserialize_with = "u64_lenient")]
    pub revision_count: u64,
    #[serde(rename = "timeAgo", deserialize_with = "string_lossy")]
    pub time_ago: String,
    #[serde(rename = "commentOriginalCommentId", deserialize_with = "string_lossy")]
    pub reply_id: String,
    #[serde(rename = "sysMetal", deserialize_with = "metals")]
    pub sys_metal: Vec<WireMetal>,
    #[serde(rename = "commentGoodCnt", deserialize_with = "u64_lenient")]
    pub good_count: u64,
    #[serde(rename = "commentVisible", deserialize_with = "bool_from_zero")]
    pub visible: bool,
    #[serde(rename = "commentOnArticleId", deserialize_with = "string_lossy")]
    pub article_id: String,
    #[serde(rename = "rewardedCnt", deserialize_with = "u64_lenient")]
    pub rewarded_count: u64,
    #[serde(rename = "commentSharpURL", deserialize_with = "string_lossy")]
    pub sharp_url: String,
    #[serde(
        rename = "commentAnonymous",
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub is_anonymous: bool,
    #[serde(rename = "commentReplyCnt", deserialize_with = "u64_lenient")]
    pub reply_count: u64,
    #[serde(rename = "oId", deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "commentContent", deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(default, deserialize_with = "u64_lenient")]
    pub status: u64,
    pub commenter: WireArticleAuthor,
    #[serde(rename = "commentAuthorName", deserialize_with = "string_lossy")]
    pub author: String,
    #[serde(rename = "commentThankCnt", deserialize_with = "u64_lenient")]
    pub thank_count: u64,
    #[serde(rename = "commentBadCnt", deserialize_with = "u64_lenient")]
    pub bad_count: u64,
    #[serde(rename = "rewarded", deserialize_with = "bool_from_int_or_bool")]
    pub rewarded: bool,
    #[serde(
        rename = "commentAuthorThumbnailURL",
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url: String,
    #[serde(rename = "commentAudioURL", deserialize_with = "string_lossy")]
    pub audio_url: String,
    #[serde(rename = "commentQnAOffered", deserialize_with = "u64_lenient")]
    pub offered: u64,
}

/// 文章详情。
#[derive(Clone, Debug, Deserialize, Default)]
#[serde(default)]
pub struct WireArticleDetail {
    #[serde(
        rename = "articleShowInList",
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub show_in_list: bool,
    #[serde(rename = "articleCreateTime", deserialize_with = "string_lossy")]
    pub create_time: String,
    #[serde(rename = "articleAuthorId", deserialize_with = "string_lossy")]
    pub author_id: String,
    #[serde(rename = "articleBadCnt", deserialize_with = "u64_lenient")]
    pub bad_count: u64,
    #[serde(rename = "articleLatestCmtTime", deserialize_with = "string_lossy")]
    pub latest_comment_time: String,
    #[serde(rename = "articleGoodCnt", deserialize_with = "u64_lenient")]
    pub good_count: u64,
    #[serde(rename = "articleQnAOfferPoint", deserialize_with = "u64_lenient")]
    pub offer_point: u64,
    #[serde(rename = "articleThumbnailURL", deserialize_with = "string_lossy")]
    pub thumbnail_url: String,
    #[serde(rename = "articleStickRemains", deserialize_with = "u64_lenient")]
    pub stick_remains: u64,
    #[serde(rename = "timeAgo", deserialize_with = "string_lossy")]
    pub time_ago: String,
    #[serde(rename = "articleUpdateTimeStr", deserialize_with = "string_lossy")]
    pub update_time_str: String,
    #[serde(rename = "articleAuthorName", deserialize_with = "string_lossy")]
    pub author_name: String,
    #[serde(
        rename = "articleType",
        default = "default_type_index",
        deserialize_with = "article_type_index"
    )]
    pub type_index: i64,
    #[serde(rename = "offered", deserialize_with = "bool_from_int_or_bool")]
    pub offered: bool,
    #[serde(rename = "articleCreateTimeStr", deserialize_with = "string_lossy")]
    pub create_time_str: String,
    #[serde(rename = "articleViewCount", deserialize_with = "u64_lenient")]
    pub view_count: u64,
    #[serde(
        rename = "articleAuthorThumbnailURL20",
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url20: String,
    #[serde(rename = "articleWatchCnt", deserialize_with = "u64_lenient")]
    pub watch_count: u64,
    #[serde(rename = "articlePreviewContent", deserialize_with = "string_lossy")]
    pub preview_content: String,
    #[serde(rename = "articleTitleEmoj", deserialize_with = "string_lossy")]
    pub title_emoji: String,
    #[serde(rename = "articleTitleEmojUnicode", deserialize_with = "string_lossy")]
    pub title_emoji_unicode: String,
    #[serde(rename = "articleTitle", deserialize_with = "string_lossy")]
    pub title: String,
    #[serde(
        rename = "articleAuthorThumbnailURL48",
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url48: String,
    #[serde(rename = "articleCommentCount", deserialize_with = "u64_lenient")]
    pub comment_count: u64,
    #[serde(rename = "articleCollectCnt", deserialize_with = "u64_lenient")]
    pub collect_count: u64,
    #[serde(rename = "articleLatestCmterName", deserialize_with = "string_lossy")]
    pub latest_commenter_name: String,
    #[serde(rename = "articleTags", deserialize_with = "string_lossy")]
    pub tags: String,
    #[serde(rename = "oId", deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "cmtTimeAgo", deserialize_with = "string_lossy")]
    pub comment_time_ago: String,
    #[serde(rename = "articleStick", deserialize_with = "u64_lenient")]
    pub stick: u64,
    #[serde(rename = "articleTagObjs", default)]
    pub tag_objs: Vec<WireArticleTag>,
    #[serde(rename = "articleLatestCmtTimeStr", deserialize_with = "string_lossy")]
    pub latest_comment_time_str: String,
    #[serde(
        rename = "articleAnonymous",
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub anonymous: bool,
    #[serde(rename = "articleThankCnt", deserialize_with = "u64_lenient")]
    pub thank_count: u64,
    #[serde(rename = "articleUpdateTime", deserialize_with = "string_lossy")]
    pub update_time: String,
    #[serde(rename = "articleStatus", default, deserialize_with = "u64_lenient")]
    pub status: u64,
    #[serde(rename = "articleHeat", deserialize_with = "u64_lenient")]
    pub heat: u64,
    #[serde(rename = "articlePerfect", deserialize_with = "bool_from_int_or_bool")]
    pub perfect: bool,
    #[serde(
        rename = "articleAuthorThumbnailURL210",
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url210: String,
    #[serde(rename = "articlePermalink", deserialize_with = "string_lossy")]
    pub permalink: String,
    #[serde(rename = "articleAuthor", default)]
    pub author: WireArticleAuthor,
    #[serde(rename = "thankedCnt", default, deserialize_with = "u64_lenient")]
    pub thanked_count: u64,
    #[serde(
        rename = "articleAnonymousView",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub anonymous_view: u64,
    #[serde(
        rename = "articleViewCntDisplayFormat",
        deserialize_with = "string_lossy"
    )]
    pub view_count_format: String,
    #[serde(
        rename = "articleCommentable",
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub commentable: bool,
    #[serde(rename = "rewarded", deserialize_with = "bool_from_int_or_bool")]
    pub rewarded: bool,
    #[serde(rename = "rewardedCnt", deserialize_with = "u64_lenient")]
    pub rewarded_count: u64,
    #[serde(rename = "articleRewardPoint", deserialize_with = "u64_lenient")]
    pub reward_point: u64,
    #[serde(rename = "isFollowing", deserialize_with = "bool_from_int_or_bool")]
    pub is_following: bool,
    #[serde(rename = "isWatching", deserialize_with = "bool_from_int_or_bool")]
    pub is_watching: bool,
    #[serde(rename = "isMyArticle", deserialize_with = "bool_from_int_or_bool")]
    pub is_my_article: bool,
    #[serde(rename = "thanked", deserialize_with = "bool_from_int_or_bool")]
    pub thanked: bool,
    #[serde(rename = "articleEditorType", deserialize_with = "u64_lenient")]
    pub editor_type: u64,
    #[serde(rename = "articleAudioURL", deserialize_with = "string_lossy")]
    pub audio_url: String,
    #[serde(rename = "articleToC", deserialize_with = "string_lossy")]
    pub table: String,
    #[serde(rename = "articleContent", deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(rename = "articleOriginalContent", deserialize_with = "string_lossy")]
    pub source: String,
    #[serde(rename = "articleImg1URL", deserialize_with = "string_lossy")]
    pub img1_url: String,
    #[serde(rename = "articleVote", deserialize_with = "opt_vote_index")]
    pub vote: Option<i64>,
    #[serde(
        rename = "articleRandomDouble",
        default,
        deserialize_with = "f64_lenient"
    )]
    pub random_double: f64,
    #[serde(rename = "articleAuthorIntro", deserialize_with = "string_lossy")]
    pub author_intro: String,
    #[serde(rename = "articleCity", deserialize_with = "string_lossy")]
    pub city: String,
    #[serde(rename = "articleIP", deserialize_with = "string_lossy")]
    pub ip: String,
    #[serde(rename = "articleAuthorURL", deserialize_with = "string_lossy")]
    pub author_url: String,
    #[serde(rename = "articlePushOrder", deserialize_with = "u64_lenient")]
    pub push_order: u64,
    #[serde(rename = "articleRewardContent", deserialize_with = "string_lossy")]
    pub reward_content: String,
    #[serde(rename = "reddit_score", default, deserialize_with = "u64_lenient")]
    pub reddit_score: u64,
    #[serde(
        rename = "discussionViewable",
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub comment_viewable: bool,
    #[serde(rename = "articleRevisionCount", deserialize_with = "u64_lenient")]
    pub revision_count: u64,
    #[serde(rename = "articleComments", default)]
    pub comments: Vec<WireArticleComment>,
    #[serde(rename = "articleNiceComments", default)]
    pub nice_comments: Vec<WireArticleComment>,
}

/// 文章列表响应的 `data`。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleList {
    #[serde(default)]
    pub articles: Vec<WireArticleDetail>,
    #[serde(default)]
    pub pagination: WirePagination,
    #[serde(default)]
    pub tag: Option<WireArticleTag>,
}

/// 文章草稿列表项。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleDraftSummary {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(
        rename = "articleDraftTitle",
        default,
        deserialize_with = "string_lossy"
    )]
    pub title: String,
    #[serde(
        rename = "articleDraftSummary",
        default,
        deserialize_with = "string_lossy"
    )]
    pub summary: String,
    #[serde(
        rename = "articleDraftTags",
        default,
        deserialize_with = "string_lossy"
    )]
    pub tags: String,
    #[serde(rename = "articleDraftType", default, deserialize_with = "u64_lenient")]
    pub type_: u64,
    #[serde(
        rename = "articleDraftColumnId",
        default,
        deserialize_with = "string_lossy"
    )]
    pub column_id: String,
    #[serde(
        rename = "articleDraftColumnTitle",
        default,
        deserialize_with = "string_lossy"
    )]
    pub column_title: String,
    #[serde(
        rename = "articleDraftChapterNo",
        default,
        deserialize_with = "string_lossy"
    )]
    pub chapter_no: String,
    #[serde(
        rename = "articleDraftUpdatedTime",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub updated_time: u64,
    #[serde(
        rename = "articleDraftCreatedTime",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub created_time: u64,
    #[serde(
        rename = "articleDraftUserId",
        default,
        deserialize_with = "string_lossy"
    )]
    pub user_id: String,
    #[serde(
        rename = "articleDraftRewardPoint",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub reward_point: u64,
    #[serde(
        rename = "articleDraftQnAOfferPoint",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub qna_offer_point: u64,
    #[serde(
        rename = "articleDraftShowInList",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub show_in_list: u64,
    #[serde(
        rename = "articleDraftStatement",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub statement: u64,
    #[serde(
        rename = "articleDraftCommentable",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub commentable: bool,
    #[serde(
        rename = "articleDraftAnonymous",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub anonymous: bool,
    #[serde(
        rename = "articleDraftNotifyFollowers",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub notify_followers: bool,
}

/// 文章草稿详情。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleDraftDetail {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(
        rename = "articleDraftTitle",
        default,
        deserialize_with = "string_lossy"
    )]
    pub title: String,
    #[serde(
        rename = "articleDraftContent",
        default,
        deserialize_with = "string_lossy"
    )]
    pub content: String,
    #[serde(
        rename = "articleDraftThoughtContent",
        default,
        deserialize_with = "string_lossy"
    )]
    pub thought_content: String,
    #[serde(
        rename = "articleDraftTags",
        default,
        deserialize_with = "string_lossy"
    )]
    pub tags: String,
    #[serde(rename = "articleDraftType", default, deserialize_with = "u64_lenient")]
    pub type_: u64,
    #[serde(
        rename = "articleDraftColumnId",
        default,
        deserialize_with = "string_lossy"
    )]
    pub column_id: String,
    #[serde(
        rename = "articleDraftColumnTitle",
        default,
        deserialize_with = "string_lossy"
    )]
    pub column_title: String,
    #[serde(
        rename = "articleDraftChapterNo",
        default,
        deserialize_with = "string_lossy"
    )]
    pub chapter_no: String,
    #[serde(
        rename = "articleDraftRewardContent",
        default,
        deserialize_with = "string_lossy"
    )]
    pub reward_content: String,
    #[serde(
        rename = "articleDraftRewardPoint",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub reward_point: u64,
    #[serde(
        rename = "articleDraftQnAOfferPoint",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub qna_offer_point: u64,
    #[serde(
        rename = "articleDraftCommentable",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub commentable: bool,
    #[serde(
        rename = "articleDraftAnonymous",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub anonymous: bool,
    #[serde(
        rename = "articleDraftNotifyFollowers",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub notify_followers: bool,
    #[serde(
        rename = "articleDraftShowInList",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub show_in_list: u64,
    #[serde(
        rename = "articleDraftStatement",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub statement: u64,
    #[serde(
        rename = "articleDraftUpdatedTime",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub updated_time: u64,
    #[serde(
        rename = "articleDraftCreatedTime",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub created_time: u64,
    #[serde(
        rename = "articleDraftSummary",
        default,
        deserialize_with = "string_lossy"
    )]
    pub summary: String,
    #[serde(
        rename = "articleDraftUserId",
        default,
        deserialize_with = "string_lossy"
    )]
    pub user_id: String,
}

/// 草稿列表响应的 `data`。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleDraftList {
    #[serde(default)]
    pub drafts: Vec<WireArticleDraftSummary>,
}

/// 单条草稿响应的 `data`。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleDraftEnvelope {
    #[serde(default)]
    pub draft: WireArticleDraftDetail,
}

/// 保存草稿响应的 `data`。
#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireArticleDraftSummaryEnvelope {
    #[serde(default)]
    pub draft: WireArticleDraftSummary,
}
