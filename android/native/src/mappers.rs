use crate::media::{extract_image_urls, extract_link_urls, strip_html_tags};
use fishpi_sdk::api::chatroom::ChatRoomEventData;
use fishpi_sdk::model::article::{
    ArticleDetail, ArticleDraftDetail, ArticleDraftSummary, ArticleListType, VoteStatus,
};
use fishpi_sdk::model::chat::{ChatData, ChatNotice};
use fishpi_sdk::model::chatroom::{
    BarragerMsg, ChatRoomMessageType, ChatRoomMsg, CustomMsg, OnlineInfo,
};
use fishpi_sdk::model::notice::{NoticeDataType, NoticeItem};
use fishpi_sdk::model::redpacket::{GestureType, RedPacketInfo, RedPacketStatusMsg};
use serde_json::{Value, json};
use std::collections::HashMap;

pub(crate) fn chat_message_to_json(msg: ChatRoomMsg, _self_username: &str) -> Value {
    let is_redpacket = msg.r#type == ChatRoomMessageType::RedPacket
        || msg
            .content
            .as_object()
            .and_then(|o| o.get("msgType"))
            .and_then(|v| v.as_str())
            .map(|v| v.eq_ignore_ascii_case("redPacket"))
            .unwrap_or(false);
    let redpacket_preview = if is_redpacket {
        redpacket_preview_from_value(&msg.content, _self_username)
    } else {
        None
    };
    let content_html = match &msg.content {
        Value::String(s) => s.clone(),
        other => serde_json::to_string(&other).unwrap_or_else(|_| "[无法解析消息]".to_string()),
    };
    let content_md = msg.md.trim().to_string();
    let content = if is_redpacket {
        redpacket_preview
            .as_ref()
            .and_then(|preview| preview.get("summary"))
            .and_then(Value::as_str)
            .map(ToString::to_string)
            .unwrap_or_else(|| "【红包消息】".to_string())
    } else if content_md.is_empty() {
        strip_html_tags(&content_html)
    } else {
        content_md.clone()
    };
    let mut payload = json!({
        "oId": msg.oId,
        "userName": msg.userName,
        "userNickname": msg.userNickname,
        "userAvatarURL": msg.userAvatarURL,
        "content": content,
        "md": content_md,
        "contentHtml": if is_redpacket { String::new() } else { content_html },
        "time": msg.time,
        "client": msg.client,
        "type": msg.r#type.as_str(),
        "revoked": msg.r#type == ChatRoomMessageType::Revoke,
        "reactionSummary": msg.reactionSummary,
        "currentUserReaction": msg.currentUserReaction,
    });
    if let Some(preview) = redpacket_preview {
        payload["redPacket"] = preview;
    }
    payload
}

pub(crate) fn private_peer(msg: &ChatData, self_username: &str) -> String {
    let self_name = self_username.trim();
    let sender = msg.senderUserName.trim();
    let receiver = msg.receiverUserName.trim();

    if sender.eq_ignore_ascii_case(self_name) {
        receiver.to_string()
    } else {
        sender.to_string()
    }
}

pub(crate) fn private_session_to_json(msg: ChatData, self_username: &str, unread: u32) -> Value {
    let preview = if msg.preview.trim().is_empty() {
        if msg.markdown.trim().is_empty() {
            strip_html_tags(&msg.content)
        } else {
            msg.markdown.clone()
        }
    } else {
        msg.preview.clone()
    };
    json!({
        "peer": private_peer(&msg, self_username),
        "preview": preview,
        "time": msg.time,
        "avatar": if msg.senderUserName.eq_ignore_ascii_case(self_username) { msg.receiverAvatar } else { msg.senderAvatar },
        "unread": unread,
        "sort": msg.oId.parse::<i64>().unwrap_or(0),
    })
}

pub(crate) fn private_message_to_json(msg: ChatData, _self_username: &str) -> Value {
    let content_html = msg.content.clone();
    let content_md = msg.markdown.trim().to_string();
    let content = if content_md.is_empty() {
        strip_html_tags(&content_html)
    } else {
        content_md.clone()
    };
    let image_urls = extract_image_urls(&content_html, &content);
    let link_urls = extract_link_urls(&content_html, &content_md);

    json!({
        "oId": msg.oId,
        "userName": msg.senderUserName,
        "userAvatarURL": msg.senderAvatar,
        "content": content,
        "md": content_md,
        "contentHtml": content_html,
        "imageUrls": image_urls,
        "linkUrls": link_urls,
        "time": msg.time,
        "type": "msg",
        "revoked": false,
    })
}

pub(crate) fn private_notice_to_json(notice: ChatNotice) -> Value {
    json!({
        "peer": notice.senderUserName,
        "preview": notice.preview,
        "avatar": notice.senderAvatar,
        "userId": notice.userId,
    })
}

pub(crate) fn private_unread_counts(
    list: Vec<ChatData>,
    self_username: &str,
) -> HashMap<String, u32> {
    let mut counts = HashMap::new();
    for item in list {
        let peer = private_peer(&item, self_username);
        if peer.trim().is_empty() {
            continue;
        }
        *counts.entry(peer).or_insert(0) += 1;
    }
    counts
}

pub(crate) fn point_title(data_type: u32) -> &'static str {
    if data_type == NoticeDataType::PointCharge as u32 {
        "积分充值"
    } else if data_type == NoticeDataType::PointTransfer as u32 {
        "积分转账"
    } else if data_type == NoticeDataType::PointArticleReward as u32 {
        "文章打赏"
    } else if data_type == NoticeDataType::PointCommentThank as u32 {
        "评论感谢"
    } else if data_type == NoticeDataType::PointExchange as u32 {
        "积分交易"
    } else if data_type == NoticeDataType::AbusePointDeduct as u32 {
        "积分扣除"
    } else if data_type == NoticeDataType::PointArticleThank as u32 {
        "文章感谢"
    } else if data_type == NoticeDataType::PointPerfectArticle as u32 {
        "文章优选"
    } else if data_type == NoticeDataType::PointCommentAccept as u32 {
        "评论被接受"
    } else if data_type == NoticeDataType::PointReportHandled as u32 {
        "举报处理"
    } else if data_type == NoticeDataType::ArticleVoteUp as u32 {
        "文章点赞"
    } else if data_type == NoticeDataType::ArticleVoteDown as u32 {
        "文章点踩"
    } else if data_type == NoticeDataType::CommentVoteUp as u32 {
        "评论点赞"
    } else if data_type == NoticeDataType::CommentVoteDown as u32 {
        "评论点踩"
    } else {
        "积分通知"
    }
}

pub(crate) fn system_title(data_type: u32) -> &'static str {
    if data_type == NoticeDataType::SysAnnounceArticle as u32 {
        "系统公告"
    } else if data_type == NoticeDataType::SysAnnounceNewUser as u32 {
        "欢迎新用户"
    } else if data_type == NoticeDataType::SysAnnounceRoleChanged as u32 {
        "角色变更"
    } else if data_type == NoticeDataType::Broadcast as u32 {
        "同城广播"
    } else {
        "系统通知"
    }
}

pub(crate) fn notice_item_to_json(item: NoticeItem) -> Option<Value> {
    match item {
        NoticeItem::Point(v) => {
            let article_jump_id =
                resolve_point_article_jump_id(v.dataType, &v.dataId, &v.description);
            let jump_type = if article_jump_id.is_empty() {
                ""
            } else {
                "article"
            };
            Some(json!({
                "id": v.oId,
                "category": "积分",
                "title": point_title(v.dataType),
                "content": v.description,
                "dataType": v.dataType,
                "time": v.createTime,
                "read": v.hasRead,
                "jumpType": jump_type,
                "jumpId": article_jump_id,
                "mentionUser": "",
            }))
        }
        NoticeItem::Comment(v) => Some(json!({
            "id": v.oId,
            "category": "评论",
            "author": v.author,
            "title": v.title,
            "content": v.content,
            "dataType": NoticeDataType::Commented as u32,
            "time": v.createTime,
            "read": v.hasRead,
            "jumpType": parse_article_id_from_url(&v.sharpURL).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_url(&v.sharpURL).unwrap_or_default(),
            "mentionUser": "",
        })),
        NoticeItem::Reply(v) => Some(json!({
            "id": v.oId,
            "category": "回复",
            "author": v.author,
            "title": v.title,
            "content": v.content,
            "dataType": v.dataType,
            "time": v.createTime,
            "read": v.hasRead,
            "jumpType": parse_article_id_from_url(&v.sharpURL).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_url(&v.sharpURL).unwrap_or_default(),
            "mentionUser": "",
        })),
        NoticeItem::At(v) => {
            let body = if !v.content.is_empty() {
                &v.content
            } else {
                &v.description
            };
            let article_jump_id = resolve_article_jump_id_default(v.dataType, &v.dataId, body);
            Some(json!({
                "id": v.oId,
                "category": "@",
                "title": format!("@{}", v.userName),
                "content": body,
                "dataType": v.dataType,
                "time": v.createTime,
                "read": v.hasRead,
                "jumpType": if !article_jump_id.is_empty() { "article" } else { "chatroom" },
                "jumpId": if !article_jump_id.is_empty() { article_jump_id } else { parse_chatroom_message_id_from_text(body).unwrap_or_default() },
                "mentionUser": v.userName,
            }))
        }
        NoticeItem::Follow(v) => Some(json!({
            "id": v.oId,
            "category": "关注",
            "author": v.author,
            "title": v.title,
            "content": v.content,
            "dataType": v.dataType,
            "time": v.createTime,
            "read": v.hasRead,
            "jumpType": parse_article_id_from_url(&v.url).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_url(&v.url).unwrap_or_default(),
            "mentionUser": "",
        })),
        NoticeItem::System(v) => Some(json!({
            "id": v.oId,
            "category": "系统",
            "title": system_title(v.dataType),
            "content": v.description,
            "dataType": v.dataType,
            "time": v.createTime,
            "read": v.hasRead,
            "jumpType": parse_article_id_from_html(&v.description).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_html(&v.description).unwrap_or_default(),
            "mentionUser": "",
        })),
    }
}

pub(crate) fn resolve_point_article_jump_id(
    data_type: u32,
    data_id: &str,
    description: &str,
) -> String {
    if !is_article_related_notice_data_type(data_type) {
        return String::new();
    }
    let prefer_description_link = data_type == NoticeDataType::PointArticleReward as u32
        || data_type == NoticeDataType::PointArticleThank as u32;
    if prefer_description_link {
        parse_article_id_from_html(description)
            .or_else(|| parse_article_id_from_data_id(data_id))
            .unwrap_or_default()
    } else {
        parse_article_id_from_data_id(data_id)
            .or_else(|| parse_article_id_from_html(description))
            .unwrap_or_default()
    }
}

pub(crate) fn resolve_article_jump_id_default(
    data_type: u32,
    data_id: &str,
    text_or_html: &str,
) -> String {
    if !is_article_related_notice_data_type(data_type) {
        return String::new();
    }
    parse_article_id_from_data_id(data_id)
        .or_else(|| parse_article_id_from_html(text_or_html))
        .unwrap_or_default()
}

pub(crate) fn is_article_related_notice_data_type(data_type: u32) -> bool {
    data_type == NoticeDataType::Article as u32
        || data_type == NoticeDataType::Comment as u32
        || data_type == NoticeDataType::Commented as u32
        || data_type == NoticeDataType::Reply as u32
        || data_type == NoticeDataType::FollowingArticleUpdate as u32
        || data_type == NoticeDataType::FollowingArticleComment as u32
        || data_type == NoticeDataType::ArticleNewFollower as u32
        || data_type == NoticeDataType::ArticleNewWatcher as u32
        || data_type == NoticeDataType::SysAnnounceArticle as u32
        || data_type == NoticeDataType::PointArticleReward as u32
        || data_type == NoticeDataType::PointArticleThank as u32
        || data_type == NoticeDataType::PointPerfectArticle as u32
        || data_type == NoticeDataType::ArticleVoteUp as u32
        || data_type == NoticeDataType::ArticleVoteDown as u32
}

pub(crate) fn parse_article_id_from_data_id(data_id: &str) -> Option<String> {
    let head = data_id.split('-').next().unwrap_or("").trim();
    if head.is_empty() {
        return None;
    }
    let id = head
        .chars()
        .take_while(|c| c.is_ascii_digit())
        .collect::<String>();
    (!id.is_empty()).then_some(id)
}

pub(crate) fn notice_time_sort_key(raw: &str) -> (i32, i32, i32, i32, i32, i32) {
    let parts = raw.split_whitespace().collect::<Vec<_>>();
    if parts.len() < 6 {
        return (0, 0, 0, 0, 0, 0);
    }

    let month = match parts[1] {
        "Jan" => 1,
        "Feb" => 2,
        "Mar" => 3,
        "Apr" => 4,
        "May" => 5,
        "Jun" => 6,
        "Jul" => 7,
        "Aug" => 8,
        "Sep" => 9,
        "Oct" => 10,
        "Nov" => 11,
        "Dec" => 12,
        _ => 0,
    };
    let day = parts[2].parse::<i32>().unwrap_or(0);
    let year = parts[5].parse::<i32>().unwrap_or(0);
    let mut hms = parts[3].split(':');
    let hour = hms.next().and_then(|v| v.parse().ok()).unwrap_or(0);
    let minute = hms.next().and_then(|v| v.parse().ok()).unwrap_or(0);
    let second = hms.next().and_then(|v| v.parse().ok()).unwrap_or(0);
    (year, month, day, hour, minute, second)
}

pub(crate) fn parse_article_id_from_html(content: &str) -> Option<String> {
    let mut cursor = content;
    loop {
        let href_pos = cursor.find("href=\"")?;
        let rest = &cursor[href_pos + 6..];
        let end = rest.find('"')?;
        let url = &rest[..end];
        if let Some(id) = parse_article_id_from_url(url) {
            return Some(id);
        }
        cursor = &rest[end + 1..];
    }
}

pub(crate) fn parse_article_id_from_url(url: &str) -> Option<String> {
    let key = "/article/";
    let idx = url.find(key)?;
    let tail = &url[idx + key.len()..];
    let id = tail
        .chars()
        .take_while(|c| c.is_ascii_digit())
        .collect::<String>();
    (!id.is_empty()).then_some(id)
}

pub(crate) fn parse_chatroom_message_id_from_text(text: &str) -> Option<String> {
    for marker in ["oid=", "oid%3D"] {
        if let Some(idx) = text.find(marker) {
            let tail = &text[idx + marker.len()..];
            let id = tail
                .chars()
                .take_while(|c| c.is_ascii_digit())
                .collect::<String>();
            if !id.is_empty() {
                return Some(id);
            }
        }
    }

    let marker = "chatroom";
    let idx = text.find(marker)?;
    let tail = &text[idx + marker.len()..];
    let id = tail
        .chars()
        .take_while(|c| c.is_ascii_digit())
        .collect::<String>();
    (!id.is_empty()).then_some(id)
}

pub(crate) fn article_list_type_from_str(raw: &str) -> ArticleListType {
    match raw {
        "hot" => ArticleListType::Hot,
        "good" => ArticleListType::Good,
        "reply" => ArticleListType::Reply,
        "long" => ArticleListType::Long,
        "perfect" => ArticleListType::Perfect,
        _ => ArticleListType::Recent,
    }
}

pub(crate) fn article_summary_to_json(item: ArticleDetail) -> Value {
    let thumbnail = if !item.img1URL.trim().is_empty() {
        item.img1URL.clone()
    } else {
        item.thumbnailURL.clone()
    };
    json!({
        "id": item.oId,
        "title": if item.title.trim().is_empty() { "[无标题]".to_string() } else { item.title },
        "author": item.authorName,
        "authorUserName": item.authorName,
        "time": if item.timeAgo.trim().is_empty() { item.createTimeStr } else { item.timeAgo },
        "tags": item.tags,
        "preview": strip_html_tags(&item.previewContent),
        "commentCount": item.commentCnt,
        "goodCount": item.goodCnt,
        "viewCount": item.viewCnt,
        "sticky": item.stick > 0 || item.stickRemains > 0,
        "perfect": item.perfect,
        "avatar": item.thumbnailURL48,
        "thumbnail": thumbnail,
    })
}

pub(crate) fn article_detail_to_json(item: ArticleDetail, page: u32) -> Value {
    let vote_state = match item.vote {
        VoteStatus::Up => 1,
        VoteStatus::Down => -1,
        VoteStatus::Normal => 0,
    };
    let comments = item
        .comments
        .into_iter()
        .map(article_comment_to_json)
        .collect::<Vec<_>>();
    let comment_has_more = item
        .pagination
        .as_ref()
        .map(|p| page < p.count)
        .unwrap_or(false);
    let markdown = if item.source.trim().is_empty() {
        item.content.clone()
    } else {
        item.source.clone()
    };
    let image_urls = extract_image_urls(&item.content, &markdown);
    let link_urls = extract_link_urls(&item.content, &markdown);

    json!({
        "id": item.oId,
        "title": if item.title.trim().is_empty() { "[无标题]".to_string() } else { item.title },
        "author": item.authorName,
        "authorUserName": item.authorName,
        "avatar": item.thumbnailURL48,
        "time": if item.timeAgo.trim().is_empty() { item.createTimeStr } else { item.timeAgo },
        "tags": item.tags,
        "markdown": markdown,
        "imageUrls": image_urls,
        "linkUrls": link_urls,
        "goodCount": item.goodCnt,
        "badCount": item.badCnt,
        "thankCount": item.thankCnt,
        "collectCount": item.collectCnt,
        "watchCount": item.watchCnt,
        "commentCount": item.commentCnt,
        "viewCount": item.viewCnt,
        "following": item.isFollowing,
        "watching": item.isWatching,
        "thanked": item.thanked,
        "rewarded": item.rewarded,
        "rewardedCount": item.rewardedCnt,
        "rewardPoint": item.rewardPoint,
        "rewardContent": item.rewardContent,
        "voteState": vote_state,
        "commentNextPage": page + 1,
        "commentHasMore": comment_has_more,
        "comments": comments,
    })
}

pub(crate) fn article_draft_summary_to_json(item: ArticleDraftSummary) -> Value {
    json!({
        "id": item.oId,
        "title": item.articleDraftTitle,
        "summary": item.articleDraftSummary,
        "tags": item.articleDraftTags,
        "type": item.articleDraftType,
        "columnId": item.articleDraftColumnId,
        "columnTitle": item.articleDraftColumnTitle,
        "chapterNo": item.articleDraftChapterNo,
        "updatedTime": item.articleDraftUpdatedTime,
    })
}

pub(crate) fn article_draft_detail_to_json(item: ArticleDraftDetail) -> Value {
    json!({
        "id": item.oId,
        "title": item.articleDraftTitle,
        "content": item.articleDraftContent,
        "thoughtContent": item.articleDraftThoughtContent,
        "tags": item.articleDraftTags,
        "type": item.articleDraftType,
        "columnId": item.articleDraftColumnId,
        "columnTitle": item.articleDraftColumnTitle,
        "chapterNo": item.articleDraftChapterNo,
        "rewardContent": item.articleDraftRewardContent,
        "rewardPoint": item.articleDraftRewardPoint,
        "qnaOfferPoint": item.articleDraftQnAOfferPoint,
        "commentable": item.articleDraftCommentable,
        "anonymous": item.articleDraftAnonymous,
        "notifyFollowers": item.articleDraftNotifyFollowers,
        "showInList": item.articleDraftShowInList,
        "statement": item.articleDraftStatement,
        "updatedTime": item.articleDraftUpdatedTime,
    })
}

pub(crate) fn article_comment_to_json(item: fishpi_sdk::model::article::ArticleComment) -> Value {
    let vote_state = match item.vote {
        VoteStatus::Up => 1,
        VoteStatus::Down => -1,
        VoteStatus::Normal => 0,
    };
    let image_urls = extract_image_urls(&item.content, &item.content);
    let link_urls = extract_link_urls(&item.content, &item.content);
    let display_name = item.commenter.nickname.trim();
    let user_name = item.commenter.userName.trim();
    let author_label = if !display_name.is_empty()
        && !user_name.is_empty()
        && !display_name.eq_ignore_ascii_case(user_name)
    {
        format!("{}({})", display_name, user_name)
    } else if !display_name.is_empty() {
        display_name.to_string()
    } else if !user_name.is_empty() {
        user_name.to_string()
    } else {
        item.author.clone()
    };
    json!({
        "id": item.oId,
        "author": author_label,
        "displayName": if display_name.is_empty() { item.author.clone() } else { display_name.to_string() },
        "userName": if user_name.is_empty() { item.author.clone() } else { user_name.to_string() },
        "time": if item.timeAgo.trim().is_empty() { item.createTimeStr } else { item.timeAgo },
        "content": item.content,
        "imageUrls": image_urls,
        "linkUrls": link_urls,
        "goodCount": item.goodCnt,
        "badCount": item.badCnt,
        "thankCount": item.thankCnt,
        "voteState": vote_state,
        "thanked": item.rewarded,
        "replyId": item.replyId,
        "avatar": if !item.commenter.avatarURL.trim().is_empty() { item.commenter.avatarURL } else { item.thumbnailURL },
    })
}

pub(crate) fn emoji_group_to_json(item: fishpi_sdk::model::emoji::EmojiGroup) -> Value {
    json!({
        "id": item.oId,
        "name": item.name,
        "sort": item.sort,
        "isDefault": item.isDefault,
        "count": item.emojiCnt,
    })
}

pub(crate) fn emoji_item_to_json(item: fishpi_sdk::model::emoji::EmojiItem) -> Value {
    json!({
        "id": item.oId,
        "groupId": item.groupId,
        "name": item.name,
        "url": item.url,
        "sort": item.sort,
    })
}

pub(crate) fn breezemoon_to_json(item: fishpi_sdk::model::breezemoon::BreezemoonContent) -> Value {
    json!({
        "id": item.oId,
        "authorName": item.authorName,
        "updated": item.updated,
        "created": item.created,
        "timeAgo": item.timeAgo,
        "content": item.content,
        "createTime": item.createTime,
        "city": item.city,
        "avatar": item.thumbnailURL48,
    })
}

pub(crate) fn barrager_to_json(msg: BarragerMsg) -> Value {
    json!({
        "oId": format!("barrager:{}:{}", msg.userName, msg.barragerContent),
        "userName": msg.userName,
        "userNickname": msg.userNickname,
        "userAvatarURL": msg.userAvatarURL48,
        "content": msg.barragerContent,
        "md": msg.barragerContent,
        "barragerColor": msg.barragerColor,
        "client": "barrager",
        "type": "barrager",
    })
}

pub(crate) fn online_to_json(
    users: Vec<OnlineInfo>,
    discussing: Option<String>,
    online_count: Option<usize>,
) -> Value {
    let online_users = users
        .iter()
        .map(|user| {
            json!({
                "userName": user.userName.clone(),
                "avatar": user.userAvatarURL.clone(),
            })
        })
        .collect::<Vec<_>>();

    json!({
        "event": "online",
        "users": online_users,
        "discussing": discussing.unwrap_or_default(),
        "onlineCount": online_count.unwrap_or(users.len()),
    })
}

pub(crate) fn redpacket_status_to_json(status: RedPacketStatusMsg) -> Value {
    json!({
        "event": "redPacketStatus",
        "id": status.oId,
        "count": status.count,
        "got": status.got,
        "whoGive": status.whoGive,
    })
}

pub(crate) fn custom_to_json(message: CustomMsg) -> Value {
    json!({
        "event": "custom",
        "message": message.message,
    })
}

pub(crate) fn redpacket_preview_from_value(value: &Value, self_username: &str) -> Option<Value> {
    let obj = value.as_object()?;
    let type_raw = obj.get("type").and_then(|x| x.as_str()).unwrap_or("");
    let type_name = match type_raw {
        "random" => "拼手气红包",
        "average" => "平分红包",
        "specify" => "专属红包",
        "heartbeat" => "心跳红包",
        "rockPaperScissors" => "猜拳红包",
        _ => "红包",
    };
    let money = obj.get("money").and_then(|x| x.as_u64()).unwrap_or(0);
    let count = obj.get("count").and_then(|x| x.as_u64()).unwrap_or(0);
    let got = obj.get("got").and_then(|x| x.as_u64()).unwrap_or(0);
    let bless = obj.get("msg").and_then(|x| x.as_str()).unwrap_or("").trim();
    let gesture = obj
        .get("gesture")
        .or_else(|| obj.get("GestureType"))
        .and_then(value_to_gesture_index);
    let receivers = obj
        .get("recivers")
        .or_else(|| obj.get("receivers"))
        .and_then(|x| x.as_array())
        .map(|arr| {
            arr.iter()
                .filter_map(|v| v.as_str().map(ToString::to_string))
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();
    let is_specify = type_raw == "specify";
    let is_finished = count > 0 && got >= count;
    let is_target_user = receivers
        .iter()
        .any(|u| u.eq_ignore_ascii_case(self_username));
    let specify_openable = !is_specify || receivers.is_empty() || is_target_user;

    let mut line = format!(
        "【{}】{} 积分 / {} 个（已领 {}/{}）",
        type_name, money, count, got, count
    );
    if !bless.is_empty() {
        line.push('\n');
        line.push_str(bless);
    }
    if is_finished {
        line.push_str("\n已抢完");
    } else if is_specify && !specify_openable {
        line.push_str("\n专属红包（你不可领取）");
    } else if type_raw == "rockPaperScissors" {
        line.push_str("\n请选择 石头 / 剪刀 / 布 领取");
    } else {
        line.push_str("\n点击“拆红包”领取");
    }

    Some(json!({
        "type": type_raw,
        "typeName": type_name,
        "money": money,
        "count": count,
        "got": got,
        "message": bless,
        "summary": line,
        "finished": is_finished,
        "openable": specify_openable && !is_finished,
        "needGesture": type_raw == "rockPaperScissors",
        "gesture": gesture,
        "receivers": receivers,
        "who": obj.get("who").cloned().unwrap_or_else(|| json!([])),
    }))
}

pub(crate) fn value_to_gesture_index(value: &Value) -> Option<u8> {
    if let Some(number) = value.as_u64() {
        return (number <= 2).then_some(number as u8);
    }
    match value.as_str()?.trim() {
        "0" | "石头" | "Rock" | "rock" => Some(0),
        "1" | "剪刀" | "Scissors" | "scissors" => Some(1),
        "2" | "布" | "Paper" | "paper" => Some(2),
        _ => None,
    }
}

pub(crate) fn gesture_from_index(index: i32) -> Option<GestureType> {
    match index {
        0 => Some(GestureType::Rock),
        1 => Some(GestureType::Scissors),
        2 => Some(GestureType::Paper),
        _ => None,
    }
}

pub(crate) fn redpacket_info_to_json(info: RedPacketInfo) -> Value {
    let who = info
        .who
        .into_iter()
        .map(|got| {
            json!({
                "userId": got.userId,
                "userName": got.userName,
                "avatar": got.avatar,
                "userMoney": got.userMoney,
                "time": got.time,
            })
        })
        .collect::<Vec<_>>();

    json!({
        "info": {
            "count": info.info.count,
            "gesture": info.info.gesture.map(|g| g as u8),
            "got": info.info.got,
            "message": info.info.msg,
            "userName": info.info.userName,
            "userAvatarURL": info.info.userAvatarURL,
        },
        "receivers": info.recivers,
        "who": who,
    })
}

pub(crate) fn chatroom_event_to_json(event: ChatRoomEventData, self_username: &str) -> Value {
    match event {
        ChatRoomEventData::Online {
            users,
            discussing,
            online_chat_cnt,
        } => online_to_json(users, discussing, online_chat_cnt),
        ChatRoomEventData::DiscussChanged(topic) => json!({
            "event": "discussChanged",
            "topic": topic,
        }),
        ChatRoomEventData::Revoke(id) => json!({
            "event": "revoke",
            "id": id,
        }),
        ChatRoomEventData::Msg(msg)
        | ChatRoomEventData::RedPacket(msg)
        | ChatRoomEventData::Music(msg)
        | ChatRoomEventData::Weather(msg) => json!({
            "event": "message",
            "message": chat_message_to_json(msg, self_username),
        }),
        ChatRoomEventData::Barrager(msg) => json!({
            "event": "message",
            "message": barrager_to_json(msg),
        }),
        ChatRoomEventData::RedPacketStatus(status) => redpacket_status_to_json(status),
        ChatRoomEventData::Custom(message) => custom_to_json(message),
        ChatRoomEventData::ChatReaction(reaction) => json!({
            "event": "chatReaction",
            "id": reaction.oId,
            "reactionSummary": reaction.summary,
            "actorReaction": reaction.actorReaction,
            "actorUserId": reaction.actorUserId,
            "targetType": reaction.targetType,
            "groupType": reaction.groupType,
            "data": reaction.raw,
        }),
    }
}
