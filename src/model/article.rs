use serde::{Deserialize, Deserializer, Serialize};
use serde_json::Value;

use crate::impl_str_enum;
use crate::model::user::Metal;
use crate::model::{bool_from_int, bool_from_zero, deserialize_sys_metal};
use crate::utils::error::Error;

/// 发帖信息
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticlePost {
    /// 帖子标题
    #[serde(rename = "articleTitle")]
    pub title: String,
    /// 帖子内容
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
    pub notifyFollowers: bool,
    /// 帖子类型，ArticleType
    #[serde(rename = "articleType")]
    pub type_: ArticleType,
    /// 是否在列表展示
    #[serde(rename = "articleShowInList")]
    pub showInList: u32,
    /// 打赏内容
    #[serde(rename = "articleRewardContent")]
    pub rewardContent: Option<String>,
    /// 打赏积分
    #[serde(rename = "articleRewardPoint")]
    pub rewardPoint: Option<String>,
    /// 是否匿名
    #[serde(rename = "articleAnonymous")]
    pub anonymous: Option<bool>,
    /// 提问悬赏积分
    #[serde(rename = "articleQnAOfferPoint")]
    pub offerPoint: Option<u32>,
}

impl ArticlePost {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticlePost: {}", e)))
    }

    pub fn to_json(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize ArticlePost: {}", e)))
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
    pub iconPath: String,
    /// 标签地址
    #[serde(rename = "tagURI")]
    pub uri: String,
    /// 标签自定义 CSS
    #[serde(rename = "tagCSS")]
    pub diyCSS: String,
    /// 反对数
    #[serde(rename = "tagBadCnt")]
    pub badCnt: u32,
    /// 标签回帖计数
    #[serde(rename = "tagCommentCount")]
    pub commentCnt: u32,
    /// 关注数
    #[serde(rename = "tagFollowerCount")]
    pub followerCnt: u32,
    /// 点赞数
    #[serde(rename = "tagGoodCnt")]
    pub goodCnt: u32,
    /// 引用计数
    #[serde(rename = "tagReferenceCount")]
    pub referenceCnt: u32,
    /// 标签相关链接计数
    #[serde(rename = "tagLinkCount")]
    pub linkCnt: u32,
    /// 标签 SEO 描述
    #[serde(rename = "tagSeoDesc")]
    pub seoDesc: String,
    /// 标签关键字
    #[serde(rename = "tagSeoKeywords")]
    pub seoKeywords: String,
    /// 标签 SEO 标题
    #[serde(rename = "tagSeoTitle")]
    pub seoTitle: String,
    /// 标签广告内容
    #[serde(rename = "tagAd")]
    pub tagAd: String,
    /// 是否展示广告
    #[serde(rename = "tagShowSideAd")]
    pub showSideAd: u32,
    /// 标签状态
    #[serde(rename = "tagStatus")]
    pub status: u32,
    /// 标签随机数
    #[serde(rename = "tagRandomDouble")]
    pub randomDouble: f64,
}

impl ArticleTag {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticleTag: {}", e)))
    }
}

/// 投票状态，点赞与否
#[derive(Clone, Debug)]
pub enum VoteStatus {
    /// 未投票
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

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleAuthor {
    /// 用户是否在线
    pub isOnline: bool,
    /// 用户在线时长
    pub onlineMinute: u32,
    /// 是否公开积分列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub pointStatus: bool,
    /// 是否公开关注者列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub followerStatus: bool,
    /// 用户完成新手指引步数
    pub guideStep: u32,
    /// 是否公开在线状态
    #[serde(deserialize_with = "bool_from_zero")]
    pub onlineStatus: bool,
    /// 当前连续签到起始日
    pub currentCheckinStreakStart: u32,
    /// 是否聊天室图片自动模糊
    #[serde(deserialize_with = "bool_from_int")] // == 1
    pub isAutoBlur: bool,
    /// 用户标签
    pub tags: String,
    /// 是否公开回帖列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub commentStatus: bool,
    /// 用户时区
    pub timezone: String,
    /// 用户个人主页
    pub homePage: String,
    /// 是否启用站外链接跳转页面
    #[serde(deserialize_with = "bool_from_int")] // == 1
    pub isEnableForwardPage: bool,
    /// 是否公开 UA 信息
    #[serde(deserialize_with = "bool_from_zero")]
    pub userUAStatus: bool,
    /// 自定义首页跳转地址
    pub userIndexRedirectURL: String,
    /// 最近发帖时间
    pub latestArticleTime: u32,
    /// 标签计数
    pub tagCount: u32,
    /// 昵称
    pub nickname: String,
    /// 回帖浏览模式
    pub listViewMode: u32,
    /// 最长连续签到
    pub longestCheckinStreak: u32,
    /// 用户头像类型
    pub avatarType: String,
    /// 用户确认邮件发送时间
    pub subMailSendTime: u32,
    /// 用户最后更新时间
    pub updateTime: u32,
    /// userSubMailStatus
    #[serde(deserialize_with = "bool_from_zero")]
    pub subMailStatus: bool,
    /// 是否加入积分排行
    #[serde(deserialize_with = "bool_from_zero")]
    pub isJoinPointRank: bool,
    /// 用户最后登录时间
    pub latestLoginTime: u32,
    /// 应用角色
    pub userAppRole: u32,
    /// 头像查看模式
    pub userAvatarViewMode: u32,
    /// 用户状态
    pub userStatus: u32,
    /// 用户上次最长连续签到日期
    pub longestCheckinStreakEnd: u32,
    /// 是否公开关注帖子列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub watchingArticleStatus: bool,
    /// 上次回帖时间
    pub latestCmtTime: u32,
    /// 用户省份
    pub province: String,
    /// 用户当前连续签到计数
    pub currentCheckinStreak: u32,
    /// 用户编号
    pub userNo: u32,
    /// 用户头像
    pub avatarURL: String,
    /// 是否公开关注标签列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub followingTagStatus: bool,
    /// 用户语言
    pub userLanguage: String,
    /// 是否加入消费排行
    #[serde(deserialize_with = "bool_from_zero")]
    pub isJoinUsedPointRank: bool,
    /// 上次签到日期
    pub currentCheckinStreakEnd: u32,
    /// 是否公开收藏帖子列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub followingArticleStatus: bool,
    /// 是否启用键盘快捷键
    #[serde(deserialize_with = "bool_from_zero")]
    pub keyboardShortcutsStatus: bool,
    /// 是否回帖后自动关注帖子
    #[serde(deserialize_with = "bool_from_zero")]
    pub replyWatchArticleStatus: bool,
    /// 回帖浏览模式
    pub commentViewMode: u32,
    /// 是否公开清风明月列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub breezemoonStatus: bool,
    /// 用户上次签到时间
    pub userCheckinTime: u32,
    /// 用户消费积分
    pub usedPoint: u32,
    /// 是否公开发帖列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub articleStatus: bool,
    /// 用户积分
    pub userPoint: u32,
    /// 用户回帖数
    pub commentCount: u32,
    /// 用户个性签名
    pub userIntro: String,
    /// 移动端主题
    pub userMobileSkin: String,
    /// 分页每页条目
    pub listPageSize: u32,
    /// 文章 Id
    pub oId: String,
    /// 用户名
    pub userName: String,
    /// 是否公开 IP 地理信息
    #[serde(deserialize_with = "bool_from_zero")]
    pub geoStatus: bool,
    /// 最长连续签到起始日
    pub longestCheckinStreakStart: u32,
    /// 用户主题
    pub userSkin: String,
    /// 是否启用 Web 通知
    #[serde(deserialize_with = "bool_from_zero")]
    pub notifyStatus: bool,
    /// 公开关注用户列表
    #[serde(deserialize_with = "bool_from_zero")]
    pub followingUserStatus: bool,
    /// 文章数
    pub articleCount: u32,
    /// 用户角色
    pub userRole: String,
    /// 徽章
    #[serde(deserialize_with = "deserialize_sys_metal")]
    pub sysMetal: Vec<Metal>,
}

impl ArticleAuthor {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticleAuthor: {}", e)))
    }
}

/// 评论作者
pub type CommentAuthor = ArticleAuthor;

#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleComment {
    /// 是否优评
    #[serde(rename = "commentNice")]
    pub isNice: bool,
    /// 评论创建时间字符串
    #[serde(rename = "commentCreateTimeStr")]
    pub createTimeStr: String,
    /// 评论作者 id
    #[serde(rename = "commentAuthorId")]
    pub authorId: String,
    /// 评论分数
    #[serde(deserialize_with = "deserialize_score")]
    pub score: String,
    /// 评论创建时间
    #[serde(rename = "commentCreateTime")]
    pub createTime: String,
    /// 评论作者头像
    #[serde(rename = "commentAuthorURL")]
    pub authorURL: String,
    /// 评论状态
    #[serde(deserialize_with = "deserialize_vote")]
    pub vote: VoteStatus,
    /// 评论引用数
    #[serde(rename = "commentRevisionCount")]
    pub revisionCount: u32,
    /// 评论经过时间
    #[serde(rename = "timeAgo")]
    pub timeAgo: String,
    /// 回复评论 id
    #[serde(rename = "commentOriginalCommentId")]
    pub replyId: String,
    /// 徽章
    #[serde(deserialize_with = "deserialize_sys_metal")]
    pub sysMetal: Vec<Metal>,
    /// 点赞数
    #[serde(rename = "commentGoodCnt")]
    pub goodCnt: u32,
    /// 评论是否可见
    #[serde(deserialize_with = "bool_from_zero")]
    pub visible: bool,
    /// 文章 id
    #[serde(rename = "commentOnArticleId")]
    pub articleId: String,
    /// 评论感谢数
    #[serde(rename = "rewardedCnt")]
    pub rewardedCnt: u32,
    /// 评论地址
    #[serde(rename = "commentSharpURL")]
    pub sharpURL: String,
    /// 是否匿名
    #[serde(deserialize_with = "bool_from_int")]
    pub isAnonymous: bool,
    /// 评论回复数
    #[serde(rename = "commentReplyCnt")]
    pub replyCnt: u32,
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
    pub thankCnt: u32,
    /// 评论点踩数
    #[serde(rename = "commentBadCnt")]
    pub badCnt: u32,
    /// 是否已感谢
    #[serde(rename = "rewarded")]
    pub rewarded: bool,
    /// 评论作者头像
    #[serde(rename = "commentAuthorThumbnailURL")]
    pub thumbnailURL: String,
    /// 评论音频地址
    #[serde(rename = "commentAudioURL")]
    pub audioURL: String,
    /// 评论是否采纳，1 表示采纳
    #[serde(rename = "commentQnAOffered")]
    pub offered: u32,
}

impl ArticleComment {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticleComment: {}", e)))
    }
}

/// 分页信息
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct Pagination {
    /// 总分页数
    #[serde(rename = "paginationPageCount")]
    pub count: u32,
    /// 建议分页页码
    #[serde(rename = "paginationPageNums")]
    pub pageNums: Vec<u32>,
}

impl Pagination {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse Pagination: {}", e)))
    }
}

/// 帖子类型
#[derive(Clone, Debug, Serialize, Deserialize)]
#[repr(u8)]
pub enum ArticleType {
    Normal = 0,
    Private = 1,
    Broadcast = 2,
    Thought = 3,
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
#[derive(Clone, Debug, Deserialize)]
#[allow(non_snake_case)]
pub struct ArticleDetail {
    /// 是否在列表展示
    #[serde(rename = "articleShowInList", deserialize_with = "bool_from_int")]
    pub showInList: bool,
    /// 文章创建时间
    #[serde(rename = "articleCreateTime")]
    pub createTime: String,
    /// 发布者Id
    #[serde(rename = "articleAuthorId")]
    pub authorId: String,
    /// 反对数
    #[serde(rename = "articleBadCnt")]
    pub badCnt: u32,
    /// 文章最后评论时间
    #[serde(rename = "articleLatestCmtTime")]
    pub latestCmtTime: String,
    /// 赞同数
    #[serde(rename = "articleGoodCnt")]
    pub goodCnt: u32,
    /// 悬赏积分
    #[serde(rename = "articleQnAOfferPoint")]
    pub offerPoint: u32,
    /// 文章缩略图
    #[serde(rename = "articleThumbnailURL")]
    pub thumbnailURL: String,
    /// 置顶序号
    #[serde(rename = "articleStickRemains")]
    pub stickRemains: u32,
    /// 发布时间简写
    #[serde(rename = "timeAgo")]
    pub timeAgo: String,
    /// 文章更新时间
    #[serde(rename = "articleUpdateTimeStr")]
    pub updateTimeStr: String,
    /// 作者用户名
    #[serde(rename = "articleAuthorName")]
    pub authorName: String,
    /// 文章类型
    #[serde(deserialize_with = "deserialize_type")]
    pub type_: ArticleType,
    /// 是否悬赏
    #[serde(rename = "offered")]
    pub offered: bool,
    /// 文章创建时间字符串
    #[serde(rename = "articleCreateTimeStr")]
    pub createTimeStr: String,
    /// 文章浏览数
    #[serde(rename = "articleViewCount")]
    pub viewCnt: u32,
    /// 作者头像缩略图
    #[serde(rename = "articleAuthorThumbnailURL20")]
    pub thumbnailURL20: String,
    /// 关注数
    #[serde(rename = "articleWatchCnt")]
    pub watchCnt: u32,
    /// 文章预览内容
    #[serde(rename = "articlePreviewContent")]
    pub previewContent: String,
    /// 文章标题
    #[serde(rename = "articleTitleEmoj")]
    pub titleEmoj: String,
    /// 文章标题（Unicode 的 Emoji）
    #[serde(rename = "articleTitleEmojUnicode")]
    pub titleEmojUnicode: String,
    /// 文章标题
    #[serde(rename = "articleTitle")]
    pub title: String,
    /// 作者头像缩略图
    #[serde(rename = "articleAuthorThumbnailURL48")]
    pub thumbnailURL48: String,
    /// 文章评论数
    #[serde(rename = "articleCommentCount")]
    pub commentCnt: u32,
    /// 收藏数
    #[serde(rename = "articleCollectCnt")]
    pub collectCnt: u32,
    /// 文章最后评论者
    #[serde(rename = "articleLatestCmterName")]
    pub latestCmterName: String,
    /// 文章标签
    #[serde(rename = "articleTags")]
    pub tags: String,
    /// 文章 id
    #[serde(rename = "oId")]
    pub oId: String,
    /// 最后评论时间简写
    #[serde(rename = "cmtTimeAgo")]
    pub cmtTimeAgo: String,
    /// 是否置顶
    #[serde(rename = "articleStick")]
    pub stick: u32,
    /// 文章标签信息
    #[serde(deserialize_with = "deserialize_tag_objs")]
    pub tagObjs: Vec<ArticleTag>,
    /// 文章最后评论时间
    #[serde(rename = "articleLatestCmtTimeStr")]
    pub latestCmtTimeStr: String,
    /// 是否匿名
    #[serde(rename = "articleAnonymous", deserialize_with = "bool_from_int")]
    pub anonymous: bool,
    /// 文章感谢数
    #[serde(rename = "articleThankCnt")]
    pub thankCnt: u32,
    /// 文章更新时间
    #[serde(rename = "articleUpdateTime")]
    pub updateTime: String,
    /// 文章状态
    #[serde(deserialize_with = "deserialize_status")]
    pub status: ArticleStatus,
    /// 文章点击数
    #[serde(rename = "articleHeat")]
    pub heat: u32,
    /// 文章是否优选
    #[serde(rename = "articlePerfect", deserialize_with = "bool_from_int")]
    pub perfect: bool,
    /// 作者头像缩略图
    #[serde(rename = "articleAuthorThumbnailURL210")]
    pub thumbnailURL210: String,
    /// 文章固定链接
    #[serde(rename = "articlePermalink")]
    pub permalink: String,
    /// 作者用户信息
    #[serde(deserialize_with = "deserialize_author")]
    pub author: ArticleAuthor,
    /// 文章感谢数
    #[serde(rename = "thankedCnt")]
    pub thankedCnt: u32,
    /// 文章匿名浏览量
    #[serde(rename = "articleAnonymousView")]
    pub anonymousView: u32,
    /// 文章浏览量简写
    #[serde(rename = "articleViewCntDisplayFormat")]
    pub viewCntFormat: String,
    /// 文章是否启用评论
    #[serde(rename = "articleCommentable")]
    pub commentable: bool,
    /// 是否已打赏
    #[serde(rename = "rewarded")]
    pub rewarded: bool,
    /// 打赏人数
    #[serde(rename = "rewardedCnt")]
    pub rewardedCnt: u32,
    /// 文章打赏积分
    #[serde(rename = "articleRewardPoint")]
    pub rewardPoint: u32,
    /// 是否已收藏
    #[serde(rename = "isFollowing")]
    pub isFollowing: bool,
    /// 是否已关注
    #[serde(rename = "isWatching")]
    pub isWatching: bool,
    /// 是否是我的文章
    #[serde(rename = "isMyArticle")]
    pub isMyArticle: bool,
    /// 是否已感谢
    #[serde(rename = "thanked")]
    pub thanked: bool,
    /// 编辑器类型
    #[serde(rename = "articleEditorType")]
    pub editorType: u32,
    /// 文章音频地址
    #[serde(rename = "articleAudioURL")]
    pub audioURL: String,
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
    pub img1URL: String,
    /// 文章点赞状态
    #[serde(deserialize_with = "deserialize_vote")]
    pub vote: VoteStatus,
    /// 文章随机数
    #[serde(rename = "articleRandomDouble")]
    pub randomDouble: f64,
    /// 作者签名
    #[serde(rename = "articleAuthorIntro")]
    pub authorIntro: String,
    /// 发布城市
    #[serde(rename = "articleCity")]
    pub city: String,
    /// 发布者 IP
    #[serde(rename = "articleIP")]
    pub IP: String,
    /// 作者首页地址
    #[serde(rename = "articleAuthorURL")]
    pub authorURL: String,
    /// 推送 Email 推送顺序
    #[serde(rename = "articlePushOrder")]
    pub pushOrder: u32,
    /// 打赏内容
    #[serde(rename = "articleRewardContent")]
    pub rewardContent: String,
    /// reddit分数
    #[serde(deserialize_with = "deserialize_reddit_score")]
    pub redditScore: String,
    /// 评论分页信息
    #[serde(deserialize_with = "deserialize_pagination")]
    pub pagination: Option<Pagination>,
    /// 评论是否可见
    #[serde(rename = "discussionViewable")]
    pub commentViewable: bool,
    /// 文章修改次数
    #[serde(rename = "articleRevisionCount")]
    pub revisionCount: u32,
    /// 文章的评论
    #[serde(deserialize_with = "deserialize_comments")]
    pub comments: Vec<ArticleComment>,
    /// 文章最佳评论
    #[serde(deserialize_with = "deserialize_comments")]
    pub niceComments: Vec<ArticleComment>,
}

impl ArticleDetail {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticleDetail: {}", e)))
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
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ArticleList: {}", e)))
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
    /// 优选，需包含标签
    Perfect,
}

impl_str_enum! {
    ArticleListType {
        Recent => "recent",
        Hot => "hot",
        Good => "good",
        Reply => "reply",
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
            ArticleListType::Perfect => "/perfect",
        }
    }

    pub fn values() -> Vec<Self> {
        vec![
            ArticleListType::Recent,
            ArticleListType::Hot,
            ArticleListType::Good,
            ArticleListType::Reply,
            ArticleListType::Perfect,
        ]
    }
}

/// 评论发布
#[derive(Clone, Debug, Serialize, Deserialize)]
#[allow(non_snake_case)]
pub struct CommentPost {
    /// 文章 Id
    pub articleId: String,
    /// 是否匿名评论
    #[serde(rename = "commentAnonymous")]
    pub isAnonymous: bool,
    /// 评论是否楼主可见
    #[serde(rename = "commentVisible")]
    pub isVisible: bool,
    /// 评论内容
    #[serde(rename = "commentContent")]
    pub content: String,
    /// 回复评论 Id
    #[serde(rename = "commentOriginalCommentId")]
    pub replyId: String,
}

impl CommentPost {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        serde_json::from_value(data.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse CommentPost: {}", e)))
    }

    pub fn to_value(&self) -> Result<Value, Error> {
        serde_json::to_value(self)
            .map_err(|e| Error::Parse(format!("Failed to serialize CommentPost: {}", e)))
    }
}
