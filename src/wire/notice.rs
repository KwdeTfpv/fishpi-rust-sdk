use serde::{Deserialize, Deserializer};
use serde_json::Value;

use super::de::{bool_from_int_or_bool, string_lossy};

fn u64_lenient<'de, D>(deserializer: D) -> Result<u64, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value
        .as_u64()
        .or_else(|| value.as_str().and_then(|s| s.trim().parse::<u64>().ok()))
        .unwrap_or(0))
}

fn u32_lenient<'de, D>(deserializer: D) -> Result<u32, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value
        .as_u64()
        .or_else(|| value.as_str().and_then(|s| s.trim().parse::<u64>().ok()))
        .and_then(|n| u32::try_from(n).ok())
        .unwrap_or(0))
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeCount {
    #[serde(
        rename = "userNotifyStatus",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub notify_status: bool,
    #[serde(
        rename = "unreadNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub count: u64,
    #[serde(
        rename = "unreadReplyNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub reply: u64,
    #[serde(
        rename = "unreadPointNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub point: u64,
    #[serde(
        rename = "unreadAtNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub at: u64,
    #[serde(
        rename = "unreadBroadcastNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub broadcast: u64,
    #[serde(
        rename = "unreadSysAnnounceNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub sys_announce: u64,
    #[serde(
        rename = "unreadNewFollowerNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub new_follower: u64,
    #[serde(
        rename = "unreadFollowingNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub following: u64,
    #[serde(
        rename = "unreadCommentedNotificationCnt",
        default,
        deserialize_with = "u64_lenient"
    )]
    pub commented: u64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticePoint {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "dataId", default, deserialize_with = "string_lossy")]
    pub data_id: String,
    #[serde(rename = "userId", default, deserialize_with = "string_lossy")]
    pub user_id: String,
    #[serde(rename = "dataType", default, deserialize_with = "u32_lenient")]
    pub data_type: u32,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(
        rename = "hasRead",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub has_read: bool,
    #[serde(rename = "createTime", default, deserialize_with = "string_lossy")]
    pub create_time: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeComment {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(
        rename = "commentArticleTitle",
        default,
        deserialize_with = "string_lossy"
    )]
    pub title: String,
    #[serde(
        rename = "commentAuthorName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub author: String,
    #[serde(
        rename = "commentAuthorThumbnailURL",
        default,
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url: String,
    #[serde(
        rename = "commentArticleType",
        default,
        deserialize_with = "u32_lenient"
    )]
    pub type_: u32,
    #[serde(
        rename = "commentArticlePerfect",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub perfect: bool,
    #[serde(rename = "commentContent", default, deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(rename = "commentSharpURL", default, deserialize_with = "string_lossy")]
    pub sharp_url: String,
    #[serde(
        rename = "hasRead",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub has_read: bool,
    #[serde(
        rename = "commentCreateTime",
        default,
        deserialize_with = "string_lossy"
    )]
    pub create_time: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeReply {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(
        rename = "commentArticleTitle",
        default,
        deserialize_with = "string_lossy"
    )]
    pub title: String,
    #[serde(
        rename = "commentAuthorName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub author: String,
    #[serde(
        rename = "commentAuthorThumbnailURL",
        default,
        deserialize_with = "string_lossy"
    )]
    pub thumbnail_url: String,
    #[serde(
        rename = "commentArticleType",
        default,
        deserialize_with = "u32_lenient"
    )]
    pub type_: u32,
    #[serde(
        rename = "commentArticlePerfect",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub perfect: bool,
    #[serde(rename = "commentContent", default, deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(rename = "commentSharpURL", default, deserialize_with = "string_lossy")]
    pub sharp_url: String,
    #[serde(
        rename = "hasRead",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub has_read: bool,
    #[serde(
        rename = "commentCreateTime",
        default,
        deserialize_with = "string_lossy"
    )]
    pub create_time: String,
    #[serde(rename = "dataType", default, deserialize_with = "u32_lenient")]
    pub data_type: u32,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeAt {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "dataType", default, deserialize_with = "u32_lenient")]
    pub data_type: u32,
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(rename = "authorName", default, deserialize_with = "string_lossy")]
    pub author_name: String,
    #[serde(rename = "articleTitle", default, deserialize_with = "string_lossy")]
    pub article_title: String,
    #[serde(rename = "articleId", default, deserialize_with = "string_lossy")]
    pub article_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub url: String,
    #[serde(
        rename = "atInArticle",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub at_in_article: bool,
    #[serde(rename = "thumbnailURL", default, deserialize_with = "string_lossy")]
    pub avatar_url: String,
    #[serde(rename = "userAvatarURL", default, deserialize_with = "string_lossy")]
    pub user_avatar_url: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(
        rename = "hasRead",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub has_read: bool,
    #[serde(rename = "createTime", default, deserialize_with = "string_lossy")]
    pub create_time: String,
    #[serde(rename = "dataId", default, deserialize_with = "string_lossy")]
    pub data_id: String,
    #[serde(default, deserialize_with = "bool_from_int_or_bool")]
    pub deleted: bool,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeTag {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "tagTitle", default, deserialize_with = "string_lossy")]
    pub title: String,
    #[serde(rename = "tagIconPath", default, deserialize_with = "string_lossy")]
    pub icon_path: String,
    #[serde(rename = "tagURI", default, deserialize_with = "string_lossy")]
    pub uri: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeFollow {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub url: String,
    #[serde(rename = "dataType", default, deserialize_with = "u32_lenient")]
    pub data_type: u32,
    #[serde(rename = "articleTitle", default, deserialize_with = "string_lossy")]
    pub title: String,
    #[serde(rename = "authorName", default, deserialize_with = "string_lossy")]
    pub author: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(
        rename = "isComment",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub is_comment: bool,
    #[serde(rename = "thumbnailURL", default, deserialize_with = "string_lossy")]
    pub thumbnail_url: String,
    #[serde(
        rename = "articleCommentCount",
        default,
        deserialize_with = "u32_lenient"
    )]
    pub comment_count: u32,
    #[serde(
        rename = "articlePerfect",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub perfect: bool,
    #[serde(rename = "articleTagObjs", default)]
    pub tag_objs: Vec<WireNoticeTag>,
    #[serde(rename = "articleTags", default, deserialize_with = "string_lossy")]
    pub tags: String,
    #[serde(rename = "articleType", default, deserialize_with = "u32_lenient")]
    pub type_: u32,
    #[serde(
        rename = "hasRead",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub has_read: bool,
    #[serde(rename = "createTime", default, deserialize_with = "string_lossy")]
    pub create_time: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeSystem {
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub o_id: String,
    #[serde(rename = "userId", default, deserialize_with = "string_lossy")]
    pub user_id: String,
    #[serde(rename = "dataId", default, deserialize_with = "string_lossy")]
    pub data_id: String,
    #[serde(rename = "dataType", default, deserialize_with = "u32_lenient")]
    pub data_type: u32,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub content: String,
    #[serde(
        rename = "hasRead",
        default,
        deserialize_with = "bool_from_int_or_bool"
    )]
    pub has_read: bool,
    #[serde(rename = "createTime", default, deserialize_with = "string_lossy")]
    pub create_time: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireNoticeMsg {
    #[serde(default, deserialize_with = "string_lossy")]
    pub command: String,
    #[serde(rename = "userId", default, deserialize_with = "string_lossy")]
    pub user_id: String,
    #[serde(rename = "warnBroadcastText", default)]
    pub content: Option<String>,
    #[serde(default)]
    pub who: Option<String>,
}
