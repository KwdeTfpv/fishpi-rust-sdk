use crate::ids::{ArticleId, UserId};
use crate::wire::notice::{
    WireNoticeAt, WireNoticeComment, WireNoticeCount, WireNoticeFollow, WireNoticeMsg,
    WireNoticePoint, WireNoticeReply, WireNoticeSystem, WireNoticeTag,
};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum NoticeType {
    Point,
    Commented,
    Reply,
    At,
    Following,
    Broadcast,
    System,
}

impl NoticeType {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Point => "point",
            Self::Commented => "commented",
            Self::Reply => "reply",
            Self::At => "at",
            Self::Following => "following",
            Self::Broadcast => "broadcast",
            Self::System => "sys-announce",
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum NoticeMsgType {
    Refresh,
    WarnBroadcast,
}

impl NoticeMsgType {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Refresh => "refreshNotification",
            Self::WarnBroadcast => "warnBroadcast",
        }
    }

    pub fn values() -> [&'static str; 2] {
        ["refreshNotification", "warnBroadcast"]
    }

    pub fn from_command(command: &str) -> Option<Self> {
        match command {
            "refreshNotification" => Some(Self::Refresh),
            "warnBroadcast" => Some(Self::WarnBroadcast),
            _ => None,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeCount {
    /// 用户是否启用 Web 通知
    pub notify_status: bool,
    /// 未读通知数
    pub count: u64,
    /// 未读回复通知数
    pub reply: u64,
    /// 未读积分通知数
    pub point: u64,
    /// 未读 @ 通知数
    pub at: u64,
    /// 未读同城通知数
    pub broadcast: u64,
    /// 未读系统通知数
    pub sys_announce: u64,
    /// 未读关注者通知数
    pub new_follower: u64,
    /// 未读关注通知数
    pub following: u64,
    /// 未读评论通知数
    pub commented: u64,
}

impl From<WireNoticeCount> for NoticeCount {
    fn from(w: WireNoticeCount) -> Self {
        Self {
            notify_status: w.notify_status,
            count: w.count,
            reply: w.reply,
            point: w.point,
            at: w.at,
            broadcast: w.broadcast,
            sys_announce: w.sys_announce,
            new_follower: w.new_follower,
            following: w.following,
            commented: w.commented,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticePoint {
    /// 通知 ID
    pub id: String,
    /// 数据 ID
    pub data_id: String,
    /// 用户 ID
    pub user_id: UserId,
    /// 数据类型
    pub data_type: u32,
    /// 通知描述
    pub description: String,
    /// 是否已读
    pub has_read: bool,
    /// 创建时间
    pub create_time: String,
}

impl From<WireNoticePoint> for NoticePoint {
    fn from(w: WireNoticePoint) -> Self {
        Self {
            id: w.o_id,
            data_id: w.data_id,
            user_id: w.user_id.into(),
            data_type: w.data_type,
            description: w.description,
            has_read: w.has_read,
            create_time: w.create_time,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeComment {
    /// 通知 ID
    pub id: String,
    /// 文章标题
    pub title: String,
    /// 文章作者
    pub author: String,
    /// 作者头像
    pub thumbnail_url: String,
    /// 文章类型
    pub type_: u32,
    /// 是否精选
    pub perfect: bool,
    /// 评论内容
    pub content: String,
    /// 评论地址
    pub sharp_url: String,
    /// 是否已读
    pub has_read: bool,
    /// 评论时间
    pub create_time: String,
}

impl From<WireNoticeComment> for NoticeComment {
    fn from(w: WireNoticeComment) -> Self {
        Self {
            id: w.o_id,
            title: w.title,
            author: w.author,
            thumbnail_url: w.thumbnail_url,
            type_: w.type_,
            perfect: w.perfect,
            content: w.content,
            sharp_url: w.sharp_url,
            has_read: w.has_read,
            create_time: w.create_time,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeReply {
    /// 通知 ID
    pub id: String,
    /// 文章标题
    pub title: String,
    /// 文章作者
    pub author: String,
    /// 作者头像
    pub thumbnail_url: String,
    /// 文章类型
    pub type_: u32,
    /// 是否精选
    pub perfect: bool,
    /// 回复内容
    pub content: String,
    /// 回复地址
    pub sharp_url: String,
    /// 是否已读
    pub has_read: bool,
    /// 回复时间
    pub create_time: String,
    /// 数据类型
    pub data_type: u32,
}

impl From<WireNoticeReply> for NoticeReply {
    fn from(w: WireNoticeReply) -> Self {
        Self {
            id: w.o_id,
            title: w.title,
            author: w.author,
            thumbnail_url: w.thumbnail_url,
            type_: w.type_,
            perfect: w.perfect,
            content: w.content,
            sharp_url: w.sharp_url,
            has_read: w.has_read,
            create_time: w.create_time,
            data_type: w.data_type,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeAt {
    /// 通知 ID
    pub id: String,
    /// 数据类型
    pub data_type: u32,
    /// 用户名
    pub user_name: String,
    /// 文章 @ 的作者用户名
    pub author_name: String,
    /// 文章 @ 的文章标题
    pub article_title: String,
    /// 文章 @ 的文章 ID
    pub article_id: ArticleId,
    /// 文章 @ 的跳转地址
    pub url: String,
    /// 是否是文章内 @
    pub at_in_article: bool,
    /// 用户头像（点赞类消息）
    pub avatar_url: String,
    /// 用户头像（@ 消息/红包）
    pub user_avatar_url: String,
    /// 通知内容
    pub content: String,
    /// 通知描述
    pub description: String,
    /// 是否已读
    pub has_read: bool,
    /// 创建时间
    pub create_time: String,
    /// 数据 ID
    pub data_id: String,
    /// 是否已删除
    pub deleted: bool,
}

impl From<WireNoticeAt> for NoticeAt {
    fn from(w: WireNoticeAt) -> Self {
        Self {
            id: w.o_id,
            data_type: w.data_type,
            user_name: w.user_name,
            author_name: w.author_name,
            article_title: w.article_title,
            article_id: w.article_id.into(),
            url: w.url,
            at_in_article: w.at_in_article,
            avatar_url: w.avatar_url,
            user_avatar_url: w.user_avatar_url,
            content: w.content,
            description: w.description,
            has_read: w.has_read,
            create_time: w.create_time,
            data_id: w.data_id,
            deleted: w.deleted,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeTag {
    /// 标签 ID
    pub id: String,
    /// 标签名
    pub title: String,
    /// icon 地址
    pub icon_path: String,
    /// 标签地址
    pub uri: String,
}

impl From<WireNoticeTag> for NoticeTag {
    fn from(w: WireNoticeTag) -> Self {
        Self {
            id: w.o_id,
            title: w.title,
            icon_path: w.icon_path,
            uri: w.uri,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeFollow {
    /// 通知 ID
    pub id: String,
    /// 文章地址
    pub url: String,
    /// 数据类型
    pub data_type: u32,
    /// 文章标题
    pub title: String,
    /// 作者
    pub author: String,
    /// 通知内容
    pub content: String,
    /// 是否评论
    pub is_comment: bool,
    /// 作者头像
    pub thumbnail_url: String,
    /// 文章评论数
    pub comment_count: u32,
    /// 是否精选
    pub perfect: bool,
    /// 文章标签列表
    pub tag_objs: Vec<NoticeTag>,
    /// 文章标签
    pub tags: String,
    /// 文章类型
    pub type_: u32,
    /// 是否已读
    pub has_read: bool,
    /// 通知创建时间
    pub create_time: String,
}

impl From<WireNoticeFollow> for NoticeFollow {
    fn from(w: WireNoticeFollow) -> Self {
        Self {
            id: w.o_id,
            url: w.url,
            data_type: w.data_type,
            title: w.title,
            author: w.author,
            content: w.content,
            is_comment: w.is_comment,
            thumbnail_url: w.thumbnail_url,
            comment_count: w.comment_count,
            perfect: w.perfect,
            tag_objs: w.tag_objs.into_iter().map(NoticeTag::from).collect(),
            tags: w.tags,
            type_: w.type_,
            has_read: w.has_read,
            create_time: w.create_time,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeSystem {
    /// 通知 ID
    pub id: String,
    /// 用户 ID
    pub user_id: UserId,
    /// 数据 ID
    pub data_id: String,
    /// 数据类型
    pub data_type: u32,
    /// 消息描述
    pub description: String,
    /// 消息内容
    pub content: String,
    /// 是否已读
    pub has_read: bool,
    /// 创建日期
    pub create_time: String,
}

impl From<WireNoticeSystem> for NoticeSystem {
    fn from(w: WireNoticeSystem) -> Self {
        Self {
            id: w.o_id,
            user_id: w.user_id.into(),
            data_id: w.data_id,
            data_type: w.data_type,
            description: w.description,
            content: w.content,
            has_read: w.has_read,
            create_time: w.create_time,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct NoticeMsg {
    /// 通知类型
    pub command: String,
    /// 通知接收者用户 ID
    pub user_id: UserId,
    /// 全局公告内容，仅 `warnBroadcast` 有信息
    pub content: Option<String>,
    /// 全局公告发布者，仅 `warnBroadcast` 有信息
    pub who: Option<String>,
}

impl From<WireNoticeMsg> for NoticeMsg {
    fn from(w: WireNoticeMsg) -> Self {
        Self {
            command: w.command,
            user_id: w.user_id.into(),
            content: w.content,
            who: w.who,
        }
    }
}

#[derive(Clone, Debug)]
pub enum NoticeItem {
    /// 积分通知
    Point(NoticePoint),
    /// 评论/回帖通知
    Comment(NoticeComment),
    /// 回复通知
    Reply(NoticeReply),
    /// 提到我通知
    At(NoticeAt),
    /// 关注通知
    Follow(NoticeFollow),
    /// 系统通知
    System(NoticeSystem),
}

pub type NoticeList = Vec<NoticeItem>;
