use serde::Deserialize;
use serde_json::Value;

use crate::model::article::ArticleTag;
use crate::model::bool_from_int;
use crate::{impl_str_enum, utils::error::Error};

/// 数据类型
#[derive(Debug, Clone)]
#[repr(u8)]
pub enum Notice {
    /// 文章
    Article = 0,
    /// 评论
    Comment = 1,
    /// @
    At = 2,
    /// 被评论
    Commented = 3,
    /// 关注者
    FollowingUser = 4,
    /// 积分 - 充值
    PointCharge = 5,
    /// 积分 - 转账
    PointTransfer = 6,
    /// 积分 - 文章打赏
    PointArticleReward = 7,
    /// 积分 - 评论感谢
    PointCommentThank = 8,
    /// 同城广播
    Broadcast = 9,
    /// 积分 - 交易
    PointExchange = 10,
    /// 积分 - 滥用扣除
    AbusePointDeduct = 11,
    /// 积分 - 文章被感谢
    PointArticleThank = 12,
    /// 回复
    Reply = 13,
    /// 使用邀请码
    InvitecodeUsed = 14,
    /// 系统公告 - 文章
    SysAnnounceArticle = 15,
    /// 系统公告 - 新用户
    SysAnnounceNewUser = 16,
    /// 新的关注者
    NewFollower = 17,
    /// 邀请链接
    InvitationLinkUsed = 18,
    /// 系统通知 - 角色变化
    SysAnnounceRoleChanged = 19,
    /// 关注的文章更新
    FollowingArticleUpdate = 20,
    /// 关注的文章评论
    FollowingArticleComment = 21,
    /// 积分 - 文章优选
    PointPerfectArticle = 22,
    /// 文章新的被关注者
    ArticleNewFollower = 23,
    /// 文章新的关注者
    ArticleNewWatcher = 24,
    /// 评论点赞
    CommentVoteUp = 25,
    /// 评论点踩
    CommentVoteDown = 26,
    /// 文章被点赞
    ArticleVoteUp = 27,
    /// 文章被点踩
    ArticleVoteDown = 28,
    /// 积分 - 评论被接受
    PointCommentAccept = 33,
    /// 积分 - 举报处理
    PointReportHandled = 36,
    /// 聊天室 @
    ChatRoomAt = 38,
    /// 专属红包提醒
    RedPacket = 39,
}

/// 通知类型
#[derive(Debug, Clone)]
pub enum NoticeType {
    Point,
    Commented,
    Reply,
    At,
    Following,
    Broadcast,
    System,
}

impl_str_enum!(NoticeType {
    Point => "point",
    Commented => "commented",
    Reply => "reply",
    At => "at",
    Following => "following",
    Broadcast => "broadcast",
    System => "sys-announce",
});

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticeCount {
    /// 用户是否启用 Web 通知
    #[serde(rename = "userNotifyStatus", deserialize_with = "bool_from_int")]
    pub notifyStatus: bool,
    /// 未读通知数
    #[serde(rename = "unreadNotificationCnt")]
    pub count: u64,
    /// 未读回复通知数
    #[serde(rename = "unreadReplyNotificationCnt")]
    pub reply: u64,
    /// 未读积分通知数
    #[serde(rename = "unreadPointNotificationCnt")]
    pub point: u64,
    /// 未读 @ 通知数
    #[serde(rename = "unreadAtNotificationCnt")]
    pub at: u64,
    /// 未读同城通知数
    #[serde(rename = "unreadBroadcastNotificationCnt")]
    pub broadcast: u64,
    /// 未读系统通知数
    #[serde(rename = "unreadSysAnnounceNotificationCnt")]
    pub sysAnnounce: u64,
    /// 未读关注者通知数
    #[serde(rename = "unreadNewFollowerNotificationCnt")]
    pub newFollower: u64,
    /// 未读关注通知数
    #[serde(rename = "unreadFollowingNotificationCnt")]
    pub following: u64,
    /// 未读评论通知数
    #[serde(rename = "unreadCommentedNotificationCnt")]
    pub commented: u64,
}

impl NoticeCount {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticeCount: {}", e)))
    }
}

/// 积分通知
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticePoint {
    /// 通知 ID
    pub oId: String,
    /// 数据ID
    pub dataId: String,
    /// 用户ID
    pub userId: String,
    /// 数据类型
    pub dataType: u32,
    /// 通知描述
    pub description: String,
    /// 是否已读
    pub hasRead: bool,
    /// 创建时间
    pub createTime: String,
}

impl NoticePoint {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticePoint: {}", e)))
    }
}

/// 评论/回帖通知
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticeComment {
    /// 通知 id
    pub oId: String,
    /// 文章标题
    #[serde(rename = "commentArticleTitle")]
    pub title: String,
    /// 文章作者
    #[serde(rename = "commentAuthorName")]
    pub author: String,
    /// 作者头像
    #[serde(rename = "commentAuthorThumbnailURL")]
    pub thumbnailURL: String,
    /// 文章类型
    #[serde(rename = "commentArticleType")]
    pub type_: u32,
    /// 是否精选
    #[serde(rename = "commentArticlePerfect", deserialize_with = "bool_from_int")]
    pub perfect: bool,
    /// 评论内容
    #[serde(rename = "commentContent")]
    pub content: String,
    /// 评论地址
    #[serde(rename = "commentSharpURL")]
    pub sharpURL: String,
    /// 是否已读
    pub hasRead: bool,
    /// 评论时间
    pub createTime: String,
}

impl NoticeComment {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticeComment: {}", e)))
    }
}

/// 提到我通知
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticeAt {
    /// 通知 id
    pub oId: String,
    /// 数据类型
    pub dataType: u32,
    /// 用户名
    pub userName: String,
    /// 用户头像
    #[serde(rename = "userAvatarURL")]
    pub avatarURL: String,
    /// 通知内容
    pub content: String,
    /// 是否已读
    pub hasRead: bool,
    /// 创建时间
    pub createTime: String,
}

impl NoticeAt {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticeAt: {}", e)))
    }
}

/// 关注通知
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticeFollow {
    /// 通知 Id
    pub oId: String,
    /// 文章地址
    pub url: String,
    /// 数据类型
    pub dataType: u32,
    /// 文章标题
    #[serde(rename = "articleTitle")]
    pub title: String,
    /// 作者
    #[serde(rename = "authorName")]
    pub author: String,
    /// 通知内容
    pub content: String,
    /// 是否评论
    pub isComment: bool,
    /// 作者头像
    pub thumbnailURL: String,
    /// 文章评论数
    #[serde(rename = "articleCommentCount")]
    pub commentCnt: u32,
    /// 是否精选
    #[serde(rename = "articlePerfect", deserialize_with = "bool_from_int")]
    pub perfect: bool,
    /// 文章标签列表
    #[serde(rename = "articleTagObjs")]
    pub tagObjs: Vec<ArticleTag>,
    /// 文章标签
    #[serde(rename = "articleTags")]
    pub tags: String,
    /// 文章类型
    #[serde(rename = "articleType")]
    pub type_: u32,
    /// 是否已读
    pub hasRead: bool,
    /// 通知创建时间
    pub createTime: String,
}

impl NoticeFollow {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticeFollow: {}", e)))
    }
}

/// 系统通知数据
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticeSystem {
    /// 消息的 oId
    pub oId: String,
    /// 用户 Id
    pub userId: String,
    /// 数据 Id
    pub dataId: String,
    /// 数据类型
    pub dataType: u32,
    /// 消息描述
    pub description: String,
    /// 是否已读
    pub hasRead: bool,
    /// 创建日期
    pub createTime: String,
}

impl NoticeSystem {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticeSystem: {}", e)))
    }
}

/// 通知消息类型
#[derive(Debug, Clone)]
pub enum NoticeMsgType {
    /// 刷新通知数，需调用 Notice.count 获取明细
    Refresh,
    /// 全局公告
    WarnBroadcast,
}

impl NoticeMsgType {
    pub fn values() -> Vec<&'static str> {
        vec!["refreshNotification", "warnBroadcast"]
    }
}

impl_str_enum!(NoticeMsgType {
    Refresh => "refreshNotification",
    WarnBroadcast => "warnBroadcast",
});

/// 通知消息
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct NoticeMsg {
    /// 通知类型
    pub command: String,
    /// 通知接收者用户Id
    pub userId: String,
    /// 全局公告内容，仅 `warnBroadcast` 有信息
    #[serde(rename = "warnBroadcastText")]
    pub content: Option<String>,
    /// 全局公告发布者，仅 `warnBroadcast` 有信息
    pub who: Option<String>,
}

impl NoticeMsg {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse NoticeMsg: {}", e)))
    }
}

/// 通知项联合类型
#[derive(Clone, Debug)]
pub enum NoticeItem {
    /// 积分通知
    Point(NoticePoint),
    /// 评论/回帖通知
    Comment(NoticeComment),
    /// 提到我通知
    At(NoticeAt),
    /// 关注通知
    Follow(NoticeFollow),
    /// 系统通知数据
    System(NoticeSystem),
}

pub type NoticeList = Vec<NoticeItem>;

impl NoticeItem {
    pub fn from_value(data: &Value, notice_type: &NoticeType) -> Result<Self, Error> {
        match notice_type {
            NoticeType::Point => Ok(NoticeItem::Point(NoticePoint::from_value(data)?)),
            NoticeType::Commented => Ok(NoticeItem::Comment(NoticeComment::from_value(data)?)),
            NoticeType::At => Ok(NoticeItem::At(NoticeAt::from_value(data)?)),
            NoticeType::Following => Ok(NoticeItem::Follow(NoticeFollow::from_value(data)?)),
            NoticeType::System => Ok(NoticeItem::System(NoticeSystem::from_value(data)?)),
            _ => Err(Error::Parse("Unsupported notice type".to_string())),
        }
    }
}
