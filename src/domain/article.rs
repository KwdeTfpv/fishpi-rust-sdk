//! 文章相关的干净对外类型。

use serde::{Serialize, Serializer};

use crate::domain::user::Metal;
use crate::ids::{ArticleId, UserId};
use crate::impl_str_enum;
use crate::wire::article::{
    WireArticleAuthor, WireArticleComment, WireArticleDetail, WireArticleDraftDetail,
    WireArticleDraftSummary, WireArticleList, WireArticleTag, WirePagination,
};

/// 帖子类型。
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
#[repr(u8)]
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
    pub fn from_index(index: i64) -> Self {
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
        serializer.serialize_u8(*self as u8)
    }
}

/// 帖子列表查询类型。
#[derive(Clone, Copy, Debug)]
pub enum ArticleListType {
    Recent,
    Hot,
    Good,
    Reply,
    Long,
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

/// 投票状态。
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum VoteStatus {
    #[default]
    Normal,
    Up,
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

    fn from_wire(value: Option<i64>) -> Self {
        match value {
            Some(v) => VoteStatus::from_index((v + 1).max(0) as usize),
            None => VoteStatus::Normal,
        }
    }
}

/// 文章状态。
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum ArticleStatus {
    #[default]
    Normal,
    Ban,
    Lock,
}

impl ArticleStatus {
    pub fn from_index(index: usize) -> Self {
        match index {
            0 => ArticleStatus::Normal,
            1 => ArticleStatus::Ban,
            _ => ArticleStatus::Lock,
        }
    }
}

/// 文章标签。
#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ArticleTag {
    pub id: String,
    pub title: String,
    pub description: String,
    pub icon_path: String,
    pub uri: String,
    pub diy_css: String,
    pub bad_count: u64,
    pub comment_count: u32,
    pub follower_count: u32,
    pub good_count: u64,
    pub reference_count: u32,
    pub link_count: u32,
    pub seo_desc: String,
    pub seo_keywords: String,
    pub seo_title: String,
    pub tag_ad: String,
    pub show_side_ad: u32,
    pub status: u32,
    pub random_double: f64,
}

impl From<WireArticleTag> for ArticleTag {
    fn from(w: WireArticleTag) -> Self {
        ArticleTag {
            id: w.o_id,
            title: w.title,
            description: w.description,
            icon_path: w.icon_path,
            uri: w.uri,
            diy_css: w.diy_css,
            bad_count: w.bad_count,
            comment_count: w.comment_count as u32,
            follower_count: w.follower_count as u32,
            good_count: w.good_count,
            reference_count: w.reference_count as u32,
            link_count: w.link_count as u32,
            seo_desc: w.seo_desc,
            seo_keywords: w.seo_keywords,
            seo_title: w.seo_title,
            tag_ad: w.tag_ad,
            show_side_ad: w.show_side_ad as u32,
            status: w.status as u32,
            random_double: w.random_double,
        }
    }
}

/// 分页信息。
#[derive(Clone, Debug, Default)]
pub struct Pagination {
    pub count: u32,
    pub page_nums: Vec<u32>,
}

impl From<WirePagination> for Pagination {
    fn from(w: WirePagination) -> Self {
        Pagination {
            count: w.count as u32,
            page_nums: w.page_nums,
        }
    }
}

/// 文章/评论作者。
#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ArticleAuthor {
    pub id: String,
    pub user_name: String,
    pub nickname: String,
    pub avatar_url: String,
    pub is_online: bool,
    pub online_minute: u64,
    pub point_status: bool,
    pub follower_status: bool,
    pub guide_step: u64,
    pub online_status: bool,
    pub current_checkin_streak_start: u64,
    pub is_auto_blur: bool,
    pub tags: String,
    pub comment_status: bool,
    pub timezone: String,
    pub home_page: String,
    pub is_enable_forward_page: bool,
    pub user_ua_status: bool,
    pub user_index_redirect_url: String,
    pub latest_article_time: u64,
    pub tag_count: u64,
    pub nickname_list_view_mode: u64,
    pub longest_checkin_streak: u64,
    pub avatar_type: String,
    pub sub_mail_send_time: u64,
    pub update_time: u64,
    pub sub_mail_status: bool,
    pub is_join_point_rank: bool,
    pub latest_login_time: u64,
    pub user_app_role: u64,
    pub user_avatar_view_mode: u64,
    pub user_status: u64,
    pub longest_checkin_streak_end: u64,
    pub watching_article_status: bool,
    pub latest_comment_time: u64,
    pub province: String,
    pub current_checkin_streak: u64,
    pub user_no: u64,
    pub following_tag_status: bool,
    pub user_language: String,
    pub is_join_used_point_rank: bool,
    pub current_checkin_streak_end: u64,
    pub following_article_status: bool,
    pub keyboard_shortcuts_status: bool,
    pub reply_watch_article_status: bool,
    pub comment_view_mode: u64,
    pub breezemoon_status: bool,
    pub user_checkin_time: u64,
    pub used_point: u64,
    pub article_status: bool,
    pub user_point: u64,
    pub comment_count: u64,
    pub user_intro: String,
    pub user_mobile_skin: String,
    pub list_page_size: u64,
    pub geo_status: bool,
    pub longest_checkin_streak_start: u64,
    pub user_skin: String,
    pub notify_status: bool,
    pub following_user_status: bool,
    pub article_count: u64,
    pub user_role: String,
    pub sys_metal: Vec<Metal>,
}

impl From<WireArticleAuthor> for ArticleAuthor {
    fn from(w: WireArticleAuthor) -> Self {
        ArticleAuthor {
            id: w.o_id,
            user_name: w.user_name,
            nickname: w.nickname,
            avatar_url: w.avatar_url,
            is_online: w.is_online,
            online_minute: w.online_minute,
            point_status: w.point_status,
            follower_status: w.follower_status,
            guide_step: w.guide_step,
            online_status: w.online_status,
            current_checkin_streak_start: w.current_checkin_streak_start,
            is_auto_blur: w.is_auto_blur,
            tags: w.tags,
            comment_status: w.comment_status,
            timezone: w.timezone,
            home_page: w.home_page,
            is_enable_forward_page: w.is_enable_forward_page,
            user_ua_status: w.user_ua_status,
            user_index_redirect_url: w.user_index_redirect_url,
            latest_article_time: w.latest_article_time,
            tag_count: w.tag_count,
            nickname_list_view_mode: w.list_view_mode,
            longest_checkin_streak: w.longest_checkin_streak,
            avatar_type: w.avatar_type,
            sub_mail_send_time: w.sub_mail_send_time,
            update_time: w.update_time,
            sub_mail_status: w.sub_mail_status,
            is_join_point_rank: w.is_join_point_rank,
            latest_login_time: w.latest_login_time,
            user_app_role: w.user_app_role,
            user_avatar_view_mode: w.user_avatar_view_mode,
            user_status: w.user_status,
            longest_checkin_streak_end: w.longest_checkin_streak_end,
            watching_article_status: w.watching_article_status,
            latest_comment_time: w.latest_comment_time,
            province: w.province,
            current_checkin_streak: w.current_checkin_streak,
            user_no: w.user_no,
            following_tag_status: w.following_tag_status,
            user_language: w.user_language,
            is_join_used_point_rank: w.is_join_used_point_rank,
            current_checkin_streak_end: w.current_checkin_streak_end,
            following_article_status: w.following_article_status,
            keyboard_shortcuts_status: w.keyboard_shortcuts_status,
            reply_watch_article_status: w.reply_watch_article_status,
            comment_view_mode: w.comment_view_mode,
            breezemoon_status: w.breezemoon_status,
            user_checkin_time: w.user_checkin_time,
            used_point: w.used_point,
            article_status: w.article_status,
            user_point: w.user_point,
            comment_count: w.comment_count,
            user_intro: w.user_intro,
            user_mobile_skin: w.user_mobile_skin,
            list_page_size: w.list_page_size,
            geo_status: w.geo_status,
            longest_checkin_streak_start: w.longest_checkin_streak_start,
            user_skin: w.user_skin,
            notify_status: w.notify_status,
            following_user_status: w.following_user_status,
            article_count: w.article_count,
            user_role: w.user_role,
            sys_metal: w.sys_metal.into_iter().map(Metal::from).collect(),
        }
    }
}

/// 评论作者。
pub type CommentAuthor = ArticleAuthor;

/// 文章评论。
#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ArticleComment {
    pub id: String,
    pub is_nice: bool,
    pub create_time_str: String,
    pub author_id: UserId,
    pub score: String,
    pub create_time: String,
    pub author_url: String,
    pub vote: VoteStatus,
    pub revision_count: u64,
    pub time_ago: String,
    pub reply_id: String,
    pub sys_metal: Vec<Metal>,
    pub good_count: u64,
    pub visible: bool,
    pub article_id: ArticleId,
    pub rewarded_count: u64,
    pub sharp_url: String,
    pub is_anonymous: bool,
    pub reply_count: u64,
    pub content: String,
    pub status: ArticleStatus,
    pub commenter: CommentAuthor,
    pub author: String,
    pub thank_count: u64,
    pub bad_count: u64,
    pub rewarded: bool,
    pub thumbnail_url: String,
    pub audio_url: String,
    pub offered: u64,
}

impl From<WireArticleComment> for ArticleComment {
    fn from(w: WireArticleComment) -> Self {
        ArticleComment {
            id: w.o_id,
            is_nice: w.is_nice,
            create_time_str: w.create_time_str,
            author_id: w.author_id.into(),
            score: w.score.to_string(),
            create_time: w.create_time,
            author_url: w.author_url,
            vote: VoteStatus::from_wire(w.vote),
            revision_count: w.revision_count,
            time_ago: w.time_ago,
            reply_id: w.reply_id,
            sys_metal: w.sys_metal.into_iter().map(Metal::from).collect(),
            good_count: w.good_count,
            visible: w.visible,
            article_id: w.article_id.into(),
            rewarded_count: w.rewarded_count,
            sharp_url: w.sharp_url,
            is_anonymous: w.is_anonymous,
            reply_count: w.reply_count,
            content: w.content,
            status: ArticleStatus::from_index(w.status as usize),
            commenter: w.commenter.into(),
            author: w.author,
            thank_count: w.thank_count,
            bad_count: w.bad_count,
            rewarded: w.rewarded,
            thumbnail_url: w.thumbnail_url,
            audio_url: w.audio_url,
            offered: w.offered,
        }
    }
}

/// 文章详情。
#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ArticleDetail {
    pub id: String,
    pub title: String,
    pub title_emoji: String,
    pub title_emoji_unicode: String,
    pub type_: ArticleType,
    pub status: ArticleStatus,
    pub vote: VoteStatus,
    pub author_id: UserId,
    pub author_name: String,
    pub author_intro: String,
    pub author_url: String,
    pub author: ArticleAuthor,
    pub thumbnail_url: String,
    pub thumbnail_url20: String,
    pub thumbnail_url48: String,
    pub thumbnail_url210: String,
    pub content: String,
    pub source: String,
    pub preview_content: String,
    pub table: String,
    pub tags: String,
    pub tag_objs: Vec<ArticleTag>,
    pub permalink: String,
    pub img1_url: String,
    pub audio_url: String,
    pub city: String,
    pub ip: String,
    pub create_time: String,
    pub create_time_str: String,
    pub update_time: String,
    pub update_time_str: String,
    pub time_ago: String,
    pub comment_time_ago: String,
    pub latest_comment_time: String,
    pub latest_comment_time_str: String,
    pub latest_commenter_name: String,
    pub show_in_list: bool,
    pub anonymous: bool,
    pub offered: bool,
    pub offer_point: u64,
    pub perfect: bool,
    pub commentable: bool,
    pub comment_viewable: bool,
    pub rewarded: bool,
    pub rewarded_count: u64,
    pub reward_point: u64,
    pub reward_content: String,
    pub thanked: bool,
    pub thank_count: u64,
    pub thanked_count: u64,
    pub is_following: bool,
    pub is_watching: bool,
    pub is_my_article: bool,
    pub bad_count: u64,
    pub good_count: u64,
    pub view_count: u64,
    pub view_count_format: String,
    pub watch_count: u64,
    pub collect_count: u64,
    pub comment_count: u64,
    pub anonymous_view: u64,
    pub heat: u64,
    pub stick: u64,
    pub stick_remains: u64,
    pub editor_type: u64,
    pub push_order: u64,
    pub reddit_score: String,
    pub revision_count: u64,
    pub random_double: f64,
    pub pagination: Option<Pagination>,
    pub comments: Vec<ArticleComment>,
    pub nice_comments: Vec<ArticleComment>,
}

impl ArticleDetail {
    /// 帖子 Markdown 源文本。
    pub fn markdown_content(&self) -> &str {
        if self.source.trim().is_empty() {
            &self.content
        } else {
            &self.source
        }
    }

    /// 帖子 HTML 内容。
    pub fn html_content(&self) -> &str {
        &self.content
    }

    /// 推荐展示正文。
    pub fn display_content(&self) -> &str {
        self.markdown_content()
    }

    /// 是否存在 Markdown 原文。
    pub fn has_markdown_source(&self) -> bool {
        !self.source.trim().is_empty()
    }
}

impl From<WireArticleDetail> for ArticleDetail {
    fn from(w: WireArticleDetail) -> Self {
        ArticleDetail {
            id: w.o_id,
            title: w.title,
            title_emoji: w.title_emoji,
            title_emoji_unicode: w.title_emoji_unicode,
            type_: ArticleType::from_index(w.type_index),
            status: ArticleStatus::from_index(w.status as usize),
            vote: VoteStatus::from_wire(w.vote),
            author_id: w.author_id.into(),
            author_name: w.author_name,
            author_intro: w.author_intro,
            author_url: w.author_url,
            author: w.author.into(),
            thumbnail_url: w.thumbnail_url,
            thumbnail_url20: w.thumbnail_url20,
            thumbnail_url48: w.thumbnail_url48,
            thumbnail_url210: w.thumbnail_url210,
            content: w.content,
            source: w.source,
            preview_content: w.preview_content,
            table: w.table,
            tags: w.tags,
            tag_objs: w.tag_objs.into_iter().map(ArticleTag::from).collect(),
            permalink: w.permalink,
            img1_url: w.img1_url,
            audio_url: w.audio_url,
            city: w.city,
            ip: w.ip,
            create_time: w.create_time,
            create_time_str: w.create_time_str,
            update_time: w.update_time,
            update_time_str: w.update_time_str,
            time_ago: w.time_ago,
            comment_time_ago: w.comment_time_ago,
            latest_comment_time: w.latest_comment_time,
            latest_comment_time_str: w.latest_comment_time_str,
            latest_commenter_name: w.latest_commenter_name,
            show_in_list: w.show_in_list,
            anonymous: w.anonymous,
            offered: w.offered,
            offer_point: w.offer_point,
            perfect: w.perfect,
            commentable: w.commentable,
            comment_viewable: w.comment_viewable,
            rewarded: w.rewarded,
            rewarded_count: w.rewarded_count,
            reward_point: w.reward_point,
            reward_content: w.reward_content,
            thanked: w.thanked,
            thank_count: w.thank_count,
            thanked_count: w.thanked_count,
            is_following: w.is_following,
            is_watching: w.is_watching,
            is_my_article: w.is_my_article,
            bad_count: w.bad_count,
            good_count: w.good_count,
            view_count: w.view_count,
            view_count_format: w.view_count_format,
            watch_count: w.watch_count,
            collect_count: w.collect_count,
            comment_count: w.comment_count,
            anonymous_view: w.anonymous_view,
            heat: w.heat,
            stick: w.stick,
            stick_remains: w.stick_remains,
            editor_type: w.editor_type,
            push_order: w.push_order,
            reddit_score: w.reddit_score.to_string(),
            revision_count: w.revision_count,
            random_double: w.random_double,
            pagination: None,
            comments: w.comments.into_iter().map(ArticleComment::from).collect(),
            nice_comments: w
                .nice_comments
                .into_iter()
                .map(ArticleComment::from)
                .collect(),
        }
    }
}

/// 文章列表。
#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ArticleList {
    pub list: Vec<ArticleDetail>,
    pub pagination: Pagination,
    pub tag: Option<ArticleTag>,
}

impl From<WireArticleList> for ArticleList {
    fn from(w: WireArticleList) -> Self {
        ArticleList {
            list: w.articles.into_iter().map(ArticleDetail::from).collect(),
            pagination: w.pagination.into(),
            tag: w.tag.map(ArticleTag::from),
        }
    }
}

/// 文章草稿列表项。
#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ArticleDraftSummary {
    pub id: String,
    pub title: String,
    pub summary: String,
    pub tags: String,
    pub type_: u32,
    pub column_id: String,
    pub column_title: String,
    pub chapter_no: String,
    pub updated_time: u64,
    pub created_time: u64,
    pub user_id: UserId,
    pub reward_point: u32,
    pub qna_offer_point: u32,
    pub show_in_list: u32,
    pub statement: u32,
    pub commentable: bool,
    pub anonymous: bool,
    pub notify_followers: bool,
}

impl From<WireArticleDraftSummary> for ArticleDraftSummary {
    fn from(w: WireArticleDraftSummary) -> Self {
        ArticleDraftSummary {
            id: w.o_id,
            title: w.title,
            summary: w.summary,
            tags: w.tags,
            type_: w.type_ as u32,
            column_id: w.column_id,
            column_title: w.column_title,
            chapter_no: w.chapter_no,
            updated_time: w.updated_time,
            created_time: w.created_time,
            user_id: w.user_id.into(),
            reward_point: w.reward_point as u32,
            qna_offer_point: w.qna_offer_point as u32,
            show_in_list: w.show_in_list as u32,
            statement: w.statement as u32,
            commentable: w.commentable,
            anonymous: w.anonymous,
            notify_followers: w.notify_followers,
        }
    }
}

/// 文章草稿详情。
#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ArticleDraftDetail {
    pub id: String,
    pub title: String,
    pub content: String,
    pub thought_content: String,
    pub tags: String,
    pub type_: u32,
    pub column_id: String,
    pub column_title: String,
    pub chapter_no: String,
    pub reward_content: String,
    pub reward_point: u32,
    pub qna_offer_point: u32,
    pub commentable: bool,
    pub anonymous: bool,
    pub notify_followers: bool,
    pub show_in_list: u32,
    pub statement: u32,
    pub updated_time: u64,
    pub created_time: u64,
    pub summary: String,
    pub user_id: UserId,
}

impl From<WireArticleDraftDetail> for ArticleDraftDetail {
    fn from(w: WireArticleDraftDetail) -> Self {
        ArticleDraftDetail {
            id: w.o_id,
            title: w.title,
            content: w.content,
            thought_content: w.thought_content,
            tags: w.tags,
            type_: w.type_ as u32,
            column_id: w.column_id,
            column_title: w.column_title,
            chapter_no: w.chapter_no,
            reward_content: w.reward_content,
            reward_point: w.reward_point as u32,
            qna_offer_point: w.qna_offer_point as u32,
            commentable: w.commentable,
            anonymous: w.anonymous,
            notify_followers: w.notify_followers,
            show_in_list: w.show_in_list as u32,
            statement: w.statement as u32,
            updated_time: w.updated_time,
            created_time: w.created_time,
            summary: w.summary,
            user_id: w.user_id.into(),
        }
    }
}
