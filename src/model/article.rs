use serde::{Deserialize, Deserializer, Serialize, Serializer};
use serde_json::Value;

use crate::impl_str_enum;
use crate::model::user::Metal;
use crate::model::{bool_from_int, bool_from_zero, deserialize_sys_metal};
use crate::utils::error::Error;

fn normalize_float_numbers(value: &mut Value) {
    match value {
        Value::Array(arr) => {
            for item in arr {
                normalize_float_numbers(item);
            }
        }
        Value::Object(map) => {
            for v in map.values_mut() {
                normalize_float_numbers(v);
            }
        }
        Value::Number(num) => {
            if num.as_u64().is_none()
                && num.as_i64().is_none()
                && let Some(f) = num.as_f64()
                && f.is_finite()
            {
                let n = if f <= 0.0 { 0 } else { f.trunc() as u64 };
                *value = Value::Number(serde_json::Number::from(n));
            }
        }
        _ => {}
    }
}

fn parse_with_float_fallback<T>(data: &Value, type_name: &str) -> Result<T, Error>
where
    T: for<'de> Deserialize<'de>,
{
    match serde_json::from_value::<T>(data.clone()) {
        Ok(v) => Ok(v),
        Err(first_err) => {
            let mut normalized = data.clone();
            normalize_float_numbers(&mut normalized);
            serde_json::from_value::<T>(normalized).map_err(|second_err| {
                Error::Parse(format!(
                    "Failed to parse {}: {} (fallback after float-normalize also failed: {})",
                    type_name, first_err, second_err
                ))
            })
        }
    }
}

fn non_negative_u64<'de, D>(deserializer: D) -> Result<u64, D::Error>
where
    D: Deserializer<'de>,
{
    let value: Value = Deserialize::deserialize(deserializer)?;
    match value {
        Value::Number(number) => {
            if let Some(value) = number.as_u64() {
                Ok(value)
            } else if let Some(value) = number.as_i64() {
                Ok(value.max(0) as u64)
            } else if let Some(value) = number.as_f64() {
                Ok(if value.is_finite() && value > 0.0 {
                    value.trunc() as u64
                } else {
                    0
                })
            } else {
                Ok(0)
            }
        }
        Value::String(text) => Ok(text.trim().parse::<i64>().unwrap_or(0).max(0) as u64),
        Value::Null => Ok(0),
        other => Err(serde::de::Error::custom(format!(
            "expected non-negative integer-compatible value, got {other}"
        ))),
    }
}

/// 发帖信息
#[derive(Clone, Debug, Serialize)]
#[allow(non_snake_case)]
pub struct ArticlePost {
    /// 帖子标题
    #[serde(rename = "articleTitle")]
    pub title: String,
    /// 帖子内容（Markdown 源文本）
    ///
    /// 服务端会将该 Markdown 渲染为 HTML，并在帖子详情里通过
    /// `articleContent` 返回渲染结果。
    #[serde(rename = "articleContent")]
    pub content: String,
    /// 帖子标签
    #[serde(rename = "articleTags")]
    pub tags: String,
    /// 是否允许评论
    #[serde(rename = "articleCommentable")]
    pub commentable: bool,
    /// 是否通知帖子关注者
    #[serde(rename = "articleNotifyFollowers")]
    pub notify_followers: bool,
    /// 帖子类型，ArticleType
    #[serde(rename = "articleType")]
    pub type_: ArticleType,
    /// 是否在列表展示
    #[serde(rename = "articleShowInList")]
    pub show_in_list: u32,
    /// 打赏内容
    #[serde(rename = "articleRewardContent")]
    pub reward_content: Option<String>,
    /// 打赏积分
    #[serde(rename = "articleRewardPoint")]
    pub reward_point: Option<String>,
    /// 是否匿名
    #[serde(rename = "articleAnonymous")]
    pub anonymous: Option<bool>,
    /// 提问悬赏积分
    #[serde(rename = "articleQnAOfferPoint")]
    pub offer_point: Option<u32>,
    /// 是否作为好帖领取奖励，传入 yes 时服务端按好帖处理。
    #[serde(rename = "isGoodArticle", skip_serializing_if = "Option::is_none")]
    pub is_good_article: Option<String>,
}

impl ArticlePost {
    pub fn to_json(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize ArticlePost: {}", e)))
    }
}

impl From<&ArticlePost> for ArticlePost {
    fn from(value: &ArticlePost) -> Self {
        value.clone()
    }
}

/// 文章草稿保存参数。
#[derive(Clone, Debug, Serialize, Deserialize, Default)]
#[allow(non_snake_case)]
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
    #[serde(rename = "articleQnAOfferPoint")]
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

impl ArticleDraftSave {
    pub fn to_json(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize ArticleDraftSave: {}", e)))
    }
}

impl From<&ArticleDraftSave> for ArticleDraftSave {
    fn from(value: &ArticleDraftSave) -> Self {
        value.clone()
    }
}

/// 文章草稿列表项。
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleDraftSummary {
    pub oId: String,
    #[serde(rename = "articleDraftTitle")]
    pub title: String,
    #[serde(rename = "articleDraftSummary", default)]
    pub summary: String,
    #[serde(rename = "articleDraftTags", default)]
    pub tags: String,
    #[serde(rename = "articleDraftType", default)]
    pub type_: u32,
    #[serde(rename = "articleDraftColumnId", default)]
    pub column_id: String,
    #[serde(rename = "articleDraftColumnTitle", default)]
    pub column_title: String,
    #[serde(rename = "articleDraftChapterNo", default)]
    pub chapter_no: String,
    #[serde(rename = "articleDraftUpdatedTime", default)]
    pub updated_time: u64,
}

impl ArticleDraftSummary {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "ArticleDraftSummary")
    }
}

/// 文章草稿详情。
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleDraftDetail {
    pub oId: String,
    #[serde(rename = "articleDraftTitle")]
    pub title: String,
    #[serde(rename = "articleDraftContent", default)]
    pub content: String,
    #[serde(rename = "articleDraftThoughtContent", default)]
    pub thought_content: String,
    #[serde(rename = "articleDraftTags", default)]
    pub tags: String,
    #[serde(rename = "articleDraftType", default)]
    pub type_: u32,
    #[serde(rename = "articleDraftColumnId", default)]
    pub column_id: String,
    #[serde(rename = "articleDraftColumnTitle", default)]
    pub column_title: String,
    #[serde(rename = "articleDraftChapterNo", default)]
    pub chapter_no: String,
    #[serde(rename = "articleDraftRewardContent", default)]
    pub reward_content: String,
    #[serde(rename = "articleDraftRewardPoint", default)]
    pub reward_point: String,
    #[serde(rename = "articleDraftQnAOfferPoint", default)]
    pub qna_offer_point: u32,
    #[serde(rename = "articleDraftCommentable", default)]
    pub commentable: bool,
    #[serde(rename = "articleDraftAnonymous", default)]
    pub anonymous: bool,
    #[serde(rename = "articleDraftNotifyFollowers", default)]
    pub notify_followers: bool,
    #[serde(rename = "articleDraftShowInList", default)]
    pub show_in_list: u32,
    #[serde(rename = "articleDraftStatement", default)]
    pub statement: u32,
    #[serde(rename = "articleDraftUpdatedTime", default)]
    pub updated_time: u64,
}

impl ArticleDraftDetail {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "ArticleDraftDetail")
    }
}

/// 文章标签
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleTag {
    /// 标签 id
    pub oId: String,
    /// 标签名
    #[serde(rename = "tagTitle")]
    pub title: String,
    /// 标签描述
    #[serde(rename = "tagDescription")]
    pub description: String,
    /// icon 地址
    #[serde(rename = "tagIconPath")]
    pub icon_path: String,
    /// 标签地址
    #[serde(rename = "tagURI")]
    pub uri: String,
    /// 标签自定义 CSS
    #[serde(rename = "tagCSS")]
    pub diy_css: String,
    /// 反对数
    #[serde(rename = "tagBadCnt")]
    pub bad_count: u64,
    /// 标签回帖计数
    #[serde(rename = "tagCommentCount")]
    pub comment_count: u32,
    /// 关注数
    #[serde(rename = "tagFollowerCount")]
    pub follower_count: u32,
    /// 点赞数
    #[serde(rename = "tagGoodCnt")]
    pub good_count: u64,
    /// 引用计数
    #[serde(rename = "tagReferenceCount")]
    pub reference_count: u32,
    /// 标签相关链接计数
    #[serde(rename = "tagLinkCount")]
    pub link_count: u32,
    /// 标签 SEO 描述
    #[serde(rename = "tagSeoDesc")]
    pub seo_desc: String,
    /// 标签关键字
    #[serde(rename = "tagSeoKeywords")]
    pub seo_keywords: String,
    /// 标签 SEO 标题
    #[serde(rename = "tagSeoTitle")]
    pub seo_title: String,
    /// 标签广告内容
    #[serde(rename = "tagAd")]
    pub tag_ad: String,
    /// 是否展示广告
    #[serde(rename = "tagShowSideAd")]
    pub show_side_ad: u32,
    /// 标签状态
    #[serde(rename = "tagStatus")]
    pub status: u32,
    /// 标签随机数
    #[serde(rename = "tagRandomDouble")]
    pub random_double: f64,
}

impl ArticleTag {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticleTag: {}", e)))
    }
}

/// 投票状态，点赞与否
#[derive(Clone, Debug, Default)]
pub enum VoteStatus {
    /// 未投票
    #[default]
    Normal,
    /// 点赞
    Up,
    /// 点踩
    Down,
}

impl VoteStatus {
    pub fn from_index(index: usize) -> Self {
        match index {
            1 => VoteStatus::Up,
            2 => VoteStatus::Down,
            _ => VoteStatus::Normal,
        }
    }
}

/// 文章状态
#[derive(Clone, Debug)]
pub enum ArticleStatus {
    /// 正常
    Normal,

    /// 封禁
    Ban,

    /// 锁定
    Lock,
}

impl ArticleStatus {
    pub fn from_index(index: usize) -> Self {
        match index {
            0 => ArticleStatus::Normal,
            1 => ArticleStatus::Ban,
            _ => ArticleStatus::Lock, // 默认值
        }
    }
}

impl Default for ArticleStatus {
    fn default() -> Self {
        Self::Normal
    }
}

pub fn deserialize_score<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    let value: u64 = Deserialize::deserialize(deserializer)?;
    Ok(value.to_string())
}

pub fn deserialize_vote<'de, D>(deserializer: D) -> Result<VoteStatus, D::Error>
where
    D: Deserializer<'de>,
{
    let value: i64 = Deserialize::deserialize(deserializer)?;
    Ok(VoteStatus::from_index((value + 1) as usize))
}

pub fn deserialize_status<'de, D>(deserializer: D) -> Result<ArticleStatus, D::Error>
where
    D: Deserializer<'de>,
{
    let value: u64 = Deserialize::deserialize(deserializer)?;
    Ok(ArticleStatus::from_index(value as usize))
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default)]
#[serde(rename_all = "camelCase")]
#[allow(non_snake_case)]
pub struct ArticleAuthor {
    /// 用户是否在线
    pub is_online: bool,
    /// 用户在线时长
    #[serde(deserialize_with = "non_negative_u64")]
    pub online_minute: u64,
    /// 是否公开积分列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub point_status: bool,
    /// 是否公开关注者列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub follower_status: bool,
    /// 用户完成新手指引步数
    pub guide_step: u64,
    /// 是否公开在线状态
    #[serde(deserialize_with = "bool_from_zero")]
    pub online_status: bool,
    /// 当前连续签到起始日
    pub current_checkin_streak_start: u64,
    /// 是否聊天室图片自动模糊
    #[serde(deserialize_with = "bool_from_int")] // == 1
    pub is_auto_blur: bool,
    /// 用户标签
    pub tags: String,
    /// 是否公开回帖列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub comment_status: bool,
    /// 用户时区
    pub timezone: String,
    /// 用户个人主页
    pub home_page: String,
    /// 是否启用站外链接跳转页面
    #[serde(deserialize_with = "bool_from_int")] // == 1
    pub is_enable_forward_page: bool,
    /// 是否公开 UA 信息
    #[serde(rename = "userUAStatus")]
    #[serde(deserialize_with = "bool_from_zero")]
    pub user_ua_status: bool,
    /// 自定义首页跳转地址
    #[serde(rename = "userIndexRedirectURL")]
    pub user_index_redirect_url: String,
    /// 最近发帖时间
    pub latest_article_time: u64,
    /// 标签计数
    pub tag_count: u64,
    /// 昵称
    pub nickname: String,
    /// 回帖浏览模式
    pub list_view_mode: u64,
    /// 最长连续签到
    pub longest_checkin_streak: u64,
    /// 用户头像类型
    pub avatar_type: String,
    /// 用户确认邮件发送时间
    pub sub_mail_send_time: u64,
    /// 用户最后更新时间
    pub update_time: u64,
    /// userSubMailStatus
    #[serde(deserialize_with = "bool_from_zero")]
    pub sub_mail_status: bool,
    /// 是否加入积分排行
    #[serde(deserialize_with = "bool_from_zero")]
    pub is_join_point_rank: bool,
    /// 用户最后登录时间
    pub latest_login_time: u64,
    /// 应用角色
    pub user_app_role: u64,
    /// 头像查看模式
    pub user_avatar_view_mode: u64,
    /// 用户状态
    pub user_status: u64,
    /// 用户上次最长连续签到日期
    pub longest_checkin_streak_end: u64,
    /// 是否公开关注帖子列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub watching_article_status: bool,
    /// 上次回帖时间
    pub latest_comment_time: u64,
    /// 用户省份
    pub province: String,
    /// 用户当前连续签到计数
    pub current_checkin_streak: u64,
    /// 用户编号
    pub user_no: u64,
    /// 用户头像
    #[serde(rename = "avatarURL")]
    pub avatar_url: String,
    /// 是否公开关注标签列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub following_tag_status: bool,
    /// 用户语言
    pub user_language: String,
    /// 是否加入消费排行
    #[serde(deserialize_with = "bool_from_zero")]
    pub is_join_used_point_rank: bool,
    /// 上次签到日期
    pub current_checkin_streak_end: u64,
    /// 是否公开收藏帖子列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub following_article_status: bool,
    /// 是否启用键盘快捷键
    #[serde(deserialize_with = "bool_from_zero")]
    pub keyboard_shortcuts_status: bool,
    /// 是否回帖后自动关注帖子
    #[serde(deserialize_with = "bool_from_zero")]
    pub reply_watch_article_status: bool,
    /// 回帖浏览模式
    pub comment_view_mode: u64,
    /// 是否公开清风明月列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub breezemoon_status: bool,
    /// 用户上次签到时间
    pub user_checkin_time: u64,
    /// 用户消费积分
    pub used_point: u64,
    /// 是否公开发帖列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub article_status: bool,
    /// 用户积分
    #[serde(deserialize_with = "non_negative_u64")]
    pub user_point: u64,
    /// 用户回帖数
    pub comment_count: u64,
    /// 用户个性签名
    pub user_intro: String,
    /// 移动端主题
    pub user_mobile_skin: String,
    /// 分页每页条目
    pub list_page_size: u64,
    /// 文章 Id
    pub oId: String,
    /// 用户名
    #[serde(rename = "userName")]
    pub user_name: String,
    /// 是否公开 IP 地理信息
    #[serde(deserialize_with = "bool_from_zero")]
    pub geo_status: bool,
    /// 最长连续签到起始日
    pub longest_checkin_streak_start: u64,
    /// 用户主题
    pub user_skin: String,
    /// 是否启用 Web 通知
    #[serde(deserialize_with = "bool_from_zero")]
    pub notify_status: bool,
    /// 公开关注用户列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub following_user_status: bool,
    /// 文章数
    pub article_count: u64,
    /// 用户角色
    pub user_role: String,
    /// 徽章
    #[serde(rename = "sysMetal", deserialize_with = "deserialize_sys_metal")]
    pub sys_metal: Vec<Metal>,
}

impl ArticleAuthor {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "ArticleAuthor")
    }
}

/// 评论作者
pub type CommentAuthor = ArticleAuthor;

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default)]
#[allow(non_snake_case)]
pub struct ArticleComment {
    /// 是否优评
    #[serde(rename = "commentNice")]
    pub is_nice: bool,
    /// 评论创建时间字符串
    #[serde(rename = "commentCreateTimeStr")]
    pub create_time_str: String,
    /// 评论作者 id
    #[serde(rename = "commentAuthorId")]
    pub author_id: String,
    /// 评论分数
    #[serde(deserialize_with = "deserialize_score")]
    pub score: String,
    /// 评论创建时间
    #[serde(rename = "commentCreateTime")]
    pub create_time: String,
    /// 评论作者头像
    #[serde(rename = "commentAuthorURL")]
    pub author_url: String,
    /// 评论状态
    #[serde(deserialize_with = "deserialize_vote")]
    pub vote: VoteStatus,
    /// 评论引用数
    #[serde(rename = "commentRevisionCount")]
    pub revision_count: u64,
    /// 评论经过时间
    #[serde(rename = "timeAgo")]
    pub time_ago: String,
    /// 回复评论 id
    #[serde(rename = "commentOriginalCommentId")]
    pub reply_id: String,
    /// 徽章
    #[serde(rename = "sysMetal", deserialize_with = "deserialize_sys_metal")]
    pub sys_metal: Vec<Metal>,
    /// 点赞数
    #[serde(rename = "commentGoodCnt")]
    pub good_count: u64,
    /// 评论是否可见
    #[serde(deserialize_with = "bool_from_zero")]
    pub visible: bool,
    /// 文章 id
    #[serde(rename = "commentOnArticleId")]
    pub article_id: String,
    /// 评论感谢数
    #[serde(rename = "rewardedCnt")]
    pub rewarded_count: u64,
    /// 评论地址
    #[serde(rename = "commentSharpURL")]
    pub sharp_url: String,
    /// 是否匿名
    #[serde(deserialize_with = "bool_from_int")]
    pub is_anonymous: bool,
    /// 评论回复数
    #[serde(rename = "commentReplyCnt")]
    pub reply_count: u64,
    /// 评论 id
    #[serde(rename = "oId")]
    pub oId: String,
    /// 评论内容
    #[serde(rename = "commentContent")]
    pub content: String,
    /// 评论状态
    #[serde(deserialize_with = "deserialize_status")]
    pub status: ArticleStatus,
    /// 评论作者
    pub commenter: CommentAuthor,
    /// 评论作者用户名
    #[serde(rename = "commentAuthorName")]
    pub author: String,
    /// 评论感谢数
    #[serde(rename = "commentThankCnt")]
    pub thank_count: u64,
    /// 评论点踩数
    #[serde(rename = "commentBadCnt")]
    pub bad_count: u64,
    /// 是否已感谢
    #[serde(rename = "rewarded")]
    pub rewarded: bool,
    /// 评论作者头像
    #[serde(rename = "commentAuthorThumbnailURL")]
    pub thumbnail_url: String,
    /// 评论音频地址
    #[serde(rename = "commentAudioURL")]
    pub audio_url: String,
    /// 评论是否采纳，1 表示采纳
    #[serde(rename = "commentQnAOffered")]
    pub offered: u64,
}

impl ArticleComment {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "ArticleComment")
    }
}

/// 分页信息
#[derive(Clone, Debug, Default, Deserialize)]
#[allow(non_snake_case)]
pub struct Pagination {
    /// 总分页数
    #[serde(rename = "paginationPageCount")]
    pub count: u32,
    /// 建议分页页码
    #[serde(rename = "paginationPageNums")]
    pub page_nums: Vec<u32>,
}

impl Pagination {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "Pagination")
    }
}

/// 帖子类型
#[derive(Clone, Debug)]
#[repr(u8)]
#[derive(Default)]
pub enum ArticleType {
    Normal = 0,
    Private = 1,
    Broadcast = 2,
    Thought = 3,
    #[default]
    Unknown = 4,
    Question = 5,
}

impl ArticleType {
    pub fn from_index(index: usize) -> Self {
        match index {
            0 => ArticleType::Normal,
            1 => ArticleType::Private,
            2 => ArticleType::Broadcast,
            3 => ArticleType::Thought,
            5 => ArticleType::Question,
            _ => ArticleType::Unknown,
        }
    }
}

impl Serialize for ArticleType {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_u8(match self {
            ArticleType::Normal => 0,
            ArticleType::Private => 1,
            ArticleType::Broadcast => 2,
            ArticleType::Thought => 3,
            ArticleType::Unknown => 4,
            ArticleType::Question => 5,
        })
    }
}

impl<'de> Deserialize<'de> for ArticleType {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let value = Value::deserialize(deserializer)?;
        Ok(match value {
            Value::Number(n) => ArticleType::from_index(n.as_u64().unwrap_or(4) as usize),
            Value::String(s) => match s.as_str() {
                "Normal" | "normal" => ArticleType::Normal,
                "Private" | "private" => ArticleType::Private,
                "Broadcast" | "broadcast" => ArticleType::Broadcast,
                "Thought" | "thought" => ArticleType::Thought,
                "Question" | "question" => ArticleType::Question,
                _ => ArticleType::Unknown,
            },
            _ => ArticleType::Unknown,
        })
    }
}

fn default_article_type() -> ArticleType {
    ArticleType::Unknown
}

pub fn deserialize_type<'de, D>(deserializer: D) -> Result<ArticleType, D::Error>
where
    D: Deserializer<'de>,
{
    let value: u64 = Deserialize::deserialize(deserializer)?;
    Ok(ArticleType::from_index(value as usize))
}

pub fn deserialize_reddit_score<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    let value: u64 = Deserialize::deserialize(deserializer)?;
    Ok(value.to_string())
}

pub fn deserialize_tag_objs<'de, D>(deserializer: D) -> Result<Vec<ArticleTag>, D::Error>
where
    D: Deserializer<'de>,
{
    let arr: Vec<Value> = Deserialize::deserialize(deserializer)?;
    arr.into_iter()
        .map(|v| ArticleTag::from_value(&v))
        .collect::<Result<Vec<_>, _>>()
        .map_err(serde::de::Error::custom)
}

pub fn deserialize_author<'de, D>(deserializer: D) -> Result<ArticleAuthor, D::Error>
where
    D: Deserializer<'de>,
{
    let value: Value = Deserialize::deserialize(deserializer)?;
    ArticleAuthor::from_value(&value).map_err(serde::de::Error::custom)
}

pub fn deserialize_pagination<'de, D>(deserializer: D) -> Result<Option<Pagination>, D::Error>
where
    D: Deserializer<'de>,
{
    let value: Option<Value> = Deserialize::deserialize(deserializer)?;
    match value {
        Some(v) => Pagination::from_value(&v)
            .map(Some)
            .map_err(serde::de::Error::custom),
        None => Ok(None),
    }
}

pub fn deserialize_comments<'de, D>(deserializer: D) -> Result<Vec<ArticleComment>, D::Error>
where
    D: Deserializer<'de>,
{
    let arr: Vec<Value> = Deserialize::deserialize(deserializer)?;
    arr.into_iter()
        .map(|v| ArticleComment::from_value(&v))
        .collect::<Result<Vec<_>, _>>()
        .map_err(serde::de::Error::custom)
}

/// 文章详情
#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default)]
#[allow(non_snake_case)]
pub struct ArticleDetail {
    /// 是否在列表展示
    #[serde(rename = "articleShowInList", deserialize_with = "bool_from_int")]
    pub show_in_list: bool,
    /// 文章创建时间
    #[serde(rename = "articleCreateTime")]
    pub create_time: String,
    /// 发布者Id
    #[serde(rename = "articleAuthorId")]
    pub author_id: String,
    /// 反对数
    #[serde(rename = "articleBadCnt")]
    pub bad_count: u32,
    /// 文章最后评论时间
    #[serde(rename = "articleLatestCmtTime")]
    pub latest_comment_time: String,
    /// 赞同数
    #[serde(rename = "articleGoodCnt")]
    pub good_count: u32,
    /// 悬赏积分
    #[serde(rename = "articleQnAOfferPoint")]
    pub offer_point: u64,
    /// 文章缩略图
    #[serde(rename = "articleThumbnailURL")]
    pub thumbnail_url: String,
    /// 置顶序号
    #[serde(rename = "articleStickRemains")]
    pub stick_remains: u64,
    /// 发布时间简写
    #[serde(rename = "timeAgo")]
    pub time_ago: String,
    /// 文章更新时间
    #[serde(rename = "articleUpdateTimeStr")]
    pub update_time_str: String,
    /// 作者用户名
    #[serde(rename = "articleAuthorName")]
    pub author_name: String,
    /// 文章类型
    #[serde(
        rename = "articleType",
        default = "default_article_type",
        deserialize_with = "deserialize_type"
    )]
    pub type_: ArticleType,
    /// 是否悬赏
    #[serde(rename = "offered")]
    pub offered: bool,
    /// 文章创建时间字符串
    #[serde(rename = "articleCreateTimeStr")]
    pub create_time_str: String,
    /// 文章浏览数
    #[serde(rename = "articleViewCount")]
    pub view_count: u64,
    /// 作者头像缩略图
    #[serde(rename = "articleAuthorThumbnailURL20")]
    pub thumbnail_url20: String,
    /// 关注数
    #[serde(rename = "articleWatchCnt")]
    pub watch_count: u64,
    /// 文章预览内容
    #[serde(rename = "articlePreviewContent")]
    pub preview_content: String,
    /// 文章标题
    #[serde(rename = "articleTitleEmoj")]
    pub title_emoji: String,
    /// 文章标题（Unicode 的 Emoji）
    #[serde(rename = "articleTitleEmojUnicode")]
    pub title_emoji_unicode: String,
    /// 文章标题
    #[serde(rename = "articleTitle")]
    pub title: String,
    /// 作者头像缩略图
    #[serde(rename = "articleAuthorThumbnailURL48")]
    pub thumbnail_url48: String,
    /// 文章评论数
    #[serde(rename = "articleCommentCount")]
    pub comment_count: u64,
    /// 收藏数
    #[serde(rename = "articleCollectCnt")]
    pub collect_count: u64,
    /// 文章最后评论者
    #[serde(rename = "articleLatestCmterName")]
    pub latest_commenter_name: String,
    /// 文章标签
    #[serde(rename = "articleTags")]
    pub tags: String,
    /// 文章 id
    #[serde(rename = "oId")]
    pub oId: String,
    /// 最后评论时间简写
    #[serde(rename = "cmtTimeAgo")]
    pub comment_time_ago: String,
    /// 是否置顶
    #[serde(rename = "articleStick")]
    pub stick: u64,
    /// 文章标签信息
    #[serde(
        rename = "articleTagObjs",
        default,
        deserialize_with = "deserialize_tag_objs"
    )]
    pub tag_objs: Vec<ArticleTag>,
    /// 文章最后评论时间
    #[serde(rename = "articleLatestCmtTimeStr")]
    pub latest_comment_time_str: String,
    /// 是否匿名
    #[serde(rename = "articleAnonymous", deserialize_with = "bool_from_int")]
    pub anonymous: bool,
    /// 文章感谢数
    #[serde(rename = "articleThankCnt")]
    pub thank_count: u64,
    /// 文章更新时间
    #[serde(rename = "articleUpdateTime")]
    pub update_time: String,
    /// 文章状态
    #[serde(rename = "articleStatus", deserialize_with = "deserialize_status")]
    pub status: ArticleStatus,
    /// 文章点击数
    #[serde(rename = "articleHeat")]
    pub heat: u64,
    /// 文章是否优选
    #[serde(rename = "articlePerfect", deserialize_with = "bool_from_int")]
    pub perfect: bool,
    /// 作者头像缩略图
    #[serde(rename = "articleAuthorThumbnailURL210")]
    pub thumbnail_url210: String,
    /// 文章固定链接
    #[serde(rename = "articlePermalink")]
    pub permalink: String,
    /// 作者用户信息
    #[serde(
        rename = "articleAuthor",
        default,
        deserialize_with = "deserialize_author"
    )]
    pub author: ArticleAuthor,
    /// 文章感谢数
    #[serde(rename = "thankedCnt", default, deserialize_with = "non_negative_u64")]
    pub thanked_count: u64,
    /// 文章匿名浏览量
    #[serde(
        rename = "articleAnonymousView",
        default,
        deserialize_with = "non_negative_u64"
    )]
    pub anonymous_view: u64,
    /// 文章浏览量简写
    #[serde(rename = "articleViewCntDisplayFormat")]
    pub view_count_format: String,
    /// 文章是否启用评论
    #[serde(rename = "articleCommentable")]
    pub commentable: bool,
    /// 是否已打赏
    #[serde(rename = "rewarded")]
    pub rewarded: bool,
    /// 打赏人数
    #[serde(rename = "rewardedCnt")]
    pub rewarded_count: u64,
    /// 文章打赏积分
    #[serde(rename = "articleRewardPoint")]
    pub reward_point: u64,
    /// 是否已收藏
    #[serde(rename = "isFollowing")]
    pub is_following: bool,
    /// 是否已关注
    #[serde(rename = "isWatching")]
    pub is_watching: bool,
    /// 是否是我的文章
    #[serde(rename = "isMyArticle")]
    pub is_my_article: bool,
    /// 是否已感谢
    #[serde(rename = "thanked")]
    pub thanked: bool,
    /// 编辑器类型
    #[serde(rename = "articleEditorType")]
    pub editor_type: u64,
    /// 文章音频地址
    #[serde(rename = "articleAudioURL")]
    pub audio_url: String,
    /// 文章目录 HTML
    #[serde(rename = "articleToC")]
    pub table: String,
    /// 文章内容 HTML
    #[serde(rename = "articleContent")]
    pub content: String,
    /// 文章内容 Markdown
    #[serde(rename = "articleOriginalContent")]
    pub source: String,
    /// 文章缩略图
    #[serde(rename = "articleImg1URL")]
    pub img1_url: String,
    /// 文章点赞状态
    #[serde(rename = "articleVote", deserialize_with = "deserialize_vote")]
    pub vote: VoteStatus,
    /// 文章随机数
    #[serde(rename = "articleRandomDouble")]
    pub random_double: f64,
    /// 作者签名
    #[serde(rename = "articleAuthorIntro")]
    pub author_intro: String,
    /// 发布城市
    #[serde(rename = "articleCity")]
    pub city: String,
    /// 发布者 IP
    #[serde(rename = "articleIP")]
    pub ip: String,
    /// 作者首页地址
    #[serde(rename = "articleAuthorURL")]
    pub author_url: String,
    /// 推送 Email 推送顺序
    #[serde(rename = "articlePushOrder")]
    pub push_order: u64,
    /// 打赏内容
    #[serde(rename = "articleRewardContent")]
    pub reward_content: String,
    /// reddit分数
    #[serde(deserialize_with = "deserialize_reddit_score")]
    pub reddit_score: String,
    /// 评论分页信息
    #[serde(default, deserialize_with = "deserialize_pagination")]
    pub pagination: Option<Pagination>,
    /// 评论是否可见
    #[serde(rename = "discussionViewable")]
    pub comment_viewable: bool,
    /// 文章修改次数
    #[serde(rename = "articleRevisionCount")]
    pub revision_count: u64,
    /// 文章的评论
    #[serde(
        rename = "articleComments",
        default,
        deserialize_with = "deserialize_comments"
    )]
    pub comments: Vec<ArticleComment>,
    /// 文章最佳评论
    #[serde(
        rename = "articleNiceComments",
        default,
        deserialize_with = "deserialize_comments"
    )]
    pub nice_comments: Vec<ArticleComment>,
}

impl ArticleDetail {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "ArticleDetail")
    }

    /// 返回帖子 Markdown 源文本。
    ///
    /// 优先使用 `articleOriginalContent`，若为空则回退到 `articleContent`。
    /// 这样在部分接口没有返回原始内容时仍可得到可展示文本。
    pub fn markdown_content(&self) -> &str {
        if self.source.trim().is_empty() {
            &self.content
        } else {
            &self.source
        }
    }

    /// 返回帖子 HTML 内容（服务端渲染结果）。
    pub fn html_content(&self) -> &str {
        &self.content
    }

    /// 返回推荐用于展示的正文：
    /// - 若有 Markdown 原文，返回 Markdown；
    /// - 否则返回 HTML。
    pub fn display_content(&self) -> &str {
        self.markdown_content()
    }

    /// 是否存在 Markdown 原文。
    pub fn has_markdown_source(&self) -> bool {
        !self.source.trim().is_empty()
    }
}

pub fn deserialize_articles<'de, D>(deserializer: D) -> Result<Vec<ArticleDetail>, D::Error>
where
    D: Deserializer<'de>,
{
    let arr: Vec<Value> = Deserialize::deserialize(deserializer)?;
    arr.into_iter()
        .map(|v| ArticleDetail::from_value(&v))
        .collect::<Result<Vec<_>, _>>()
        .map_err(serde::de::Error::custom)
}

/// 文章列表
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleList {
    /// 文章列表
    #[serde(rename = "articles", deserialize_with = "deserialize_articles")]
    pub list: Vec<ArticleDetail>,
    /// 分页信息
    pub pagination: Pagination,
    /// 标签信息，仅查询标签下文章列表有效
    pub tag: Option<ArticleTag>,
}

impl ArticleList {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        parse_with_float_fallback(data, "ArticleList")
    }
}

/// 帖子列表查询类型
#[derive(Clone, Debug)]
pub enum ArticleListType {
    /// 最近
    Recent,
    /// 热门
    Hot,
    /// 点赞
    Good,
    /// 最近回复
    Reply,
    /// 最新长篇
    Long,
    /// 优选，需包含标签
    Perfect,
}

impl_str_enum! {
    ArticleListType {
        Recent => "recent",
        Hot => "hot",
        Good => "good",
        Reply => "reply",
        Long => "long",
        Perfect => "perfect",
    }
}

impl ArticleListType {
    pub fn to_code(&self) -> &'static str {
        match self {
            ArticleListType::Recent => "",
            ArticleListType::Hot => "/hot",
            ArticleListType::Good => "/good",
            ArticleListType::Reply => "/reply",
            ArticleListType::Long => "/long",
            ArticleListType::Perfect => "/perfect",
        }
    }

    pub fn values() -> Vec<Self> {
        vec![
            ArticleListType::Recent,
            ArticleListType::Hot,
            ArticleListType::Good,
            ArticleListType::Reply,
            ArticleListType::Long,
            ArticleListType::Perfect,
        ]
    }
}

/// 评论发布
#[derive(Clone, Debug, Serialize)]
#[allow(non_snake_case)]
pub struct CommentPost {
    /// 文章 Id
    #[serde(rename = "articleId")]
    pub article_id: String,
    /// 是否匿名评论
    #[serde(rename = "commentAnonymous")]
    pub anonymous: bool,
    /// 评论是否楼主可见
    #[serde(rename = "commentVisible")]
    pub visible: bool,
    /// 评论内容
    #[serde(rename = "commentContent")]
    pub content: String,
    /// 回复评论 Id
    #[serde(rename = "commentOriginalCommentId")]
    pub reply_id: String,
}

impl CommentPost {
    pub fn new(article_id: impl Into<String>, content: impl Into<String>) -> Self {
        Self {
            article_id: article_id.into(),
            anonymous: false,
            visible: false,
            content: content.into(),
            reply_id: String::new(),
        }
    }

    pub fn to_value(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize CommentPost: {}", e)))
    }
}

impl From<&CommentPost> for CommentPost {
    fn from(value: &CommentPost) -> Self {
        value.clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn article_author_clamps_negative_online_minute() {
        let author = ArticleAuthor::from_value(&json!({
            "onlineMinute": -27231
        }))
        .expect("negative onlineMinute should not fail ArticleAuthor parsing");

        assert_eq!(author.online_minute, 0);
    }
}
