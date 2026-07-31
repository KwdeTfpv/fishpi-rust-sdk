use crate::media::{extract_image_urls, extract_link_urls, strip_html_tags};
use fishpi_sdk::api::chatroom::ChatRoomEventData;
use fishpi_sdk::model::article::{
    ArticleDetail, ArticleDraftDetail, ArticleDraftSummary, ArticleListType, VoteStatus,
};
use fishpi_sdk::model::chat::{ChatData, ChatNotice};
use fishpi_sdk::model::chatroom::{
    BarragerMsg, ChatRoomMessageType, ChatRoomMsg, CustomMsg, MusicMsg, OnlineInfo,
};
use fishpi_sdk::model::notice::{NoticeDataType, NoticeItem};
use fishpi_sdk::model::redpacket::{GestureType, RedPacketInfo, RedPacketStatusMsg};
use serde_json::{Value, json};
use std::collections::HashMap;

fn chatroom_payload_type(value: &Value) -> Option<&str> {
    value
        .get("msgType")
        .or_else(|| value.get("type"))
        .and_then(|v| v.as_str())
}

fn payload_type_is(value: &Value, expected: &str) -> bool {
    chatroom_payload_type(value).is_some_and(|actual| actual.eq_ignore_ascii_case(expected))
}

pub(crate) fn chat_message_to_json(msg: ChatRoomMsg, _self_username: &str) -> Value {
    if payload_type_is(&msg.content, "music")
        && let Ok(music) = MusicMsg::from_chatroom_msg(msg.clone())
    {
        return music_message_to_json(music);
    }

    let is_redpacket =
        msg.r#type == ChatRoomMessageType::RedPacket || payload_type_is(&msg.content, "redPacket");
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
        content_html.clone()
    } else {
        content_md.clone()
    };
    let mut payload = json!({
        "oId": msg.oId,
        "userName": msg.user_name,
        "userNickname": msg.user_nickname,
        "userAvatarURL": msg.user_avatar_url,
        "content": content,
        "md": content_md,
        "time": msg.time,
        "client": msg.client,
        "type": msg.r#type.as_str(),
        "revoked": msg.r#type == ChatRoomMessageType::Revoke,
        "reactionSummary": msg.reaction_summary,
        "currentUserReaction": msg.current_user_reaction,
    });
    if let Some(preview) = redpacket_preview {
        payload["redPacket"] = preview;
    }
    payload
}

pub(crate) fn music_message_to_json(msg: MusicMsg) -> Value {
    let raw_music = msg.base.content.clone();
    let raw_music_text = if msg.base.md.trim().is_empty() {
        raw_music.to_string()
    } else {
        msg.base.md.clone()
    };

    json!({
        "oId": msg.base.oId,
        "userName": msg.base.user_name,
        "userNickname": msg.base.user_nickname,
        "userAvatarURL": msg.base.user_avatar_url,
        "content": raw_music_text.clone(),
        "md": raw_music_text,
        "time": msg.base.time,
        "client": msg.base.client,
        "type": msg.base.r#type.as_str(),
        "revoked": false,
        "reactionSummary": msg.base.reaction_summary,
        "currentUserReaction": msg.base.current_user_reaction,
        "music": {
            "coverURL": msg.cover_url,
            "source": msg.source,
            "title": msg.title,
            "from": msg.from,
            "raw": raw_music,
        },
    })
}

pub(crate) fn private_peer(msg: &ChatData, self_username: &str) -> String {
    let self_name = self_username.trim();
    let sender = msg.sender_user_name.trim();
    let receiver = msg.receiver_user_name.trim();

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
        private_session_preview_text(&msg.preview)
    };
    json!({
        "peer": private_peer(&msg, self_username),
        "preview": preview,
        "time": msg.time,
        "avatar": if msg.sender_user_name.eq_ignore_ascii_case(self_username) { msg.receiver_avatar } else { msg.sender_avatar },
        "unread": unread,
        "sort": msg.oId.parse::<i64>().unwrap_or(0),
    })
}

fn private_session_preview_text(preview: &str) -> String {
    let text = preview.trim();
    if text.is_empty() || text.ends_with("...") || text.ends_with('…') {
        return text.to_string();
    }

    if text.chars().count() >= PRIVATE_SESSION_PREVIEW_ELLIPSIS_THRESHOLD {
        format!("{text}...")
    } else {
        text.to_string()
    }
}

const PRIVATE_SESSION_PREVIEW_ELLIPSIS_THRESHOLD: usize = 18;

pub(crate) fn private_message_to_json(msg: ChatData, _self_username: &str) -> Value {
    let content_html = msg.content.clone();
    let content_md = msg.markdown.trim().to_string();
    let content = if content_md.is_empty() {
        content_html.clone()
    } else {
        content_md.clone()
    };
    let image_urls = extract_image_urls(&content_html, &content);
    let link_urls = extract_link_urls(&content_html, &content_md);

    json!({
        "oId": msg.oId,
        "userName": msg.sender_user_name,
        "userAvatarURL": msg.sender_avatar,
        "content": content,
        "md": content_md,
        "imageUrls": image_urls,
        "linkUrls": link_urls,
        "time": msg.time,
        "type": "msg",
        "revoked": false,
    })
}

pub(crate) fn private_notice_to_json(notice: ChatNotice) -> Value {
    json!({
        "peer": notice.sender_user_name,
        "preview": notice.preview,
        "avatar": notice.sender_avatar,
        "userId": notice.user_id,
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
    } else if data_type == NoticeDataType::ExtensionStoreReview as u32 {
        "扩展集市"
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
                resolve_point_article_jump_id(v.data_type, &v.data_id, &v.description);
            let jump_type = if article_jump_id.is_empty() {
                ""
            } else {
                "article"
            };
            Some(json!({
                "id": v.oId,
                "category": "积分",
                "title": point_title(v.data_type),
                "content": v.description,
                "dataType": v.data_type,
                "time": v.create_time,
                "read": v.has_read,
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
            "time": v.create_time,
            "read": v.has_read,
            "jumpType": parse_article_id_from_url(&v.sharp_url).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_url(&v.sharp_url).unwrap_or_default(),
            "mentionUser": "",
        })),
        NoticeItem::Reply(v) => Some(json!({
            "id": v.oId,
            "category": "回复",
            "author": v.author,
            "title": v.title,
            "content": v.content,
            "dataType": v.data_type,
            "time": v.create_time,
            "read": v.has_read,
            "jumpType": parse_article_id_from_url(&v.sharp_url).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_url(&v.sharp_url).unwrap_or_default(),
            "mentionUser": "",
        })),
        NoticeItem::At(v) => {
            let mention_user = if v.user_name.trim().is_empty() {
                v.author_name.trim().to_string()
            } else {
                v.user_name.trim().to_string()
            };
            let body = if !v.content.trim().is_empty() {
                v.content.trim().to_string()
            } else if !v.description.trim().is_empty() {
                v.description.trim().to_string()
            } else {
                v.article_title.trim().to_string()
            };
            let title = if !v.article_title.trim().is_empty() {
                v.article_title.trim().to_string()
            } else {
                format!("@{}", mention_user)
            };
            let article_jump_id = if v.at_in_article {
                first_non_empty([
                    v.article_id.trim().to_string(),
                    parse_article_id_from_url(&v.url).unwrap_or_default(),
                    v.data_id.trim().to_string(),
                ])
            } else {
                resolve_article_jump_id_default(v.data_type, &v.data_id, &body)
            };
            Some(json!({
                "id": v.oId,
                "category": "@",
                "title": title,
                "content": body,
                "dataType": v.data_type,
                "time": v.create_time,
                "read": v.has_read,
                "jumpType": if !article_jump_id.is_empty() { "article" } else { "chatroom" },
                "jumpId": if !article_jump_id.is_empty() { article_jump_id } else { parse_chatroom_message_id_from_text(&body).unwrap_or_default() },
                "mentionUser": mention_user,
            }))
        }
        NoticeItem::Follow(v) => Some(json!({
            "id": v.oId,
            "category": "关注",
            "author": v.author,
            "title": v.title,
            "content": v.content,
            "dataType": v.data_type,
            "time": v.create_time,
            "read": v.has_read,
            "jumpType": parse_article_id_from_url(&v.url).map(|_| "article").unwrap_or(""),
            "jumpId": parse_article_id_from_url(&v.url).unwrap_or_default(),
            "mentionUser": "",
        })),
        NoticeItem::System(v) => {
            let display_content = first_non_empty([
                v.content.trim().to_string(),
                v.description.trim().to_string(),
            ]);
            Some(json!({
                "id": v.oId,
                "category": "系统",
                "title": system_title(v.data_type),
                "content": display_content,
                "dataType": v.data_type,
                "time": v.create_time,
                "read": v.has_read,
                "jumpType": parse_article_id_from_html(&v.description).map(|_| "article").unwrap_or(""),
                "jumpId": parse_article_id_from_html(&v.description).unwrap_or_default(),
                "mentionUser": "",
            }))
        }
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

fn first_non_empty(values: impl IntoIterator<Item = String>) -> String {
    values
        .into_iter()
        .find(|value| !value.trim().is_empty())
        .unwrap_or_default()
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

fn article_display_title(item: &ArticleDetail) -> String {
    let title = item.title_emoji_unicode.trim();
    if !title.is_empty() {
        return title.to_string();
    }

    let title = item.title_emoji.trim();
    if !title.is_empty() {
        return title.to_string();
    }

    let title = item.title.trim();
    if title.is_empty() {
        "[无标题]".to_string()
    } else {
        title.to_string()
    }
}

pub(crate) fn article_summary_to_json(item: ArticleDetail) -> Value {
    let thumbnail = if !item.img1_url.trim().is_empty() {
        item.img1_url.clone()
    } else {
        item.thumbnail_url.clone()
    };
    json!({
        "id": item.oId,
        "title": article_display_title(&item),
        "author": item.author_name,
        "authorUserName": item.author_name,
        "time": if item.time_ago.trim().is_empty() { item.create_time_str } else { item.time_ago },
        "tags": item.tags,
        "preview": strip_html_tags(&item.preview_content),
        "commentCount": item.comment_count,
        "goodCount": item.good_count,
        "viewCount": item.view_count,
        "sticky": item.stick > 0 || item.stick_remains > 0,
        "perfect": item.perfect,
        "avatar": item.thumbnail_url48,
        "thumbnail": thumbnail,
    })
}

pub(crate) fn article_detail_to_json(item: ArticleDetail, page: u32) -> Value {
    let vote_state = match item.vote {
        VoteStatus::Up => 1,
        VoteStatus::Down => -1,
        VoteStatus::Normal => 0,
    };
    let title = article_display_title(&item);
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
        "title": title,
        "author": item.author_name,
        "authorUserName": item.author_name,
        "avatar": item.thumbnail_url48,
        "time": if item.time_ago.trim().is_empty() { item.create_time_str } else { item.time_ago },
        "tags": item.tags,
        "markdown": markdown,
        "imageUrls": image_urls,
        "linkUrls": link_urls,
        "goodCount": item.good_count,
        "badCount": item.bad_count,
        "thankCount": item.thank_count,
        "collectCount": item.collect_count,
        "watchCount": item.watch_count,
        "commentCount": item.comment_count,
        "viewCount": item.view_count,
        "following": item.is_following,
        "watching": item.is_watching,
        "thanked": item.thanked,
        "rewarded": item.rewarded,
        "rewardedCount": item.rewarded_count,
        "rewardPoint": item.reward_point,
        "rewardContent": item.reward_content,
        "voteState": vote_state,
        "commentNextPage": page + 1,
        "commentHasMore": comment_has_more,
        "comments": comments,
    })
}

pub(crate) fn article_draft_summary_to_json(item: ArticleDraftSummary) -> Value {
    json!({
        "id": item.oId,
        "title": item.title,
        "summary": item.summary,
        "tags": item.tags,
        "type": item.type_,
        "columnId": item.column_id,
        "columnTitle": item.column_title,
        "chapterNo": item.chapter_no,
        "updatedTime": item.updated_time,
        "createdTime": item.created_time,
    })
}

pub(crate) fn article_draft_detail_to_json(item: ArticleDraftDetail) -> Value {
    json!({
        "id": item.oId,
        "title": item.title,
        "content": item.content,
        "thoughtContent": item.thought_content,
        "tags": item.tags,
        "type": item.type_,
        "columnId": item.column_id,
        "columnTitle": item.column_title,
        "chapterNo": item.chapter_no,
        "rewardContent": item.reward_content,
        "rewardPoint": item.reward_point,
        "qnaOfferPoint": item.qna_offer_point,
        "commentable": item.commentable,
        "anonymous": item.anonymous,
        "notifyFollowers": item.notify_followers,
        "showInList": item.show_in_list,
        "statement": item.statement,
        "updatedTime": item.updated_time,
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
    let user_name = item.commenter.user_name.trim();
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
        "time": if item.time_ago.trim().is_empty() { item.create_time_str } else { item.time_ago },
        "content": item.content,
        "imageUrls": image_urls,
        "linkUrls": link_urls,
        "goodCount": item.good_count,
        "badCount": item.bad_count,
        "thankCount": item.thank_count,
        "voteState": vote_state,
        "thanked": item.rewarded,
        "replyId": item.reply_id,
        "avatar": if !item.commenter.avatar_url.trim().is_empty() { item.commenter.avatar_url } else { item.thumbnail_url },
    })
}

pub(crate) fn emoji_group_to_json(item: fishpi_sdk::model::emoji::EmojiGroup) -> Value {
    json!({
        "id": item.oId,
        "name": item.name,
        "sort": item.sort,
        "isDefault": item.is_default,
        "count": item.emoji_count,
    })
}

pub(crate) fn emoji_item_to_json(item: fishpi_sdk::model::emoji::EmojiItem) -> Value {
    json!({
        "id": item.oId,
        "groupId": item.group_id,
        "name": item.name,
        "url": item.url,
        "sort": item.sort,
    })
}

pub(crate) fn breezemoon_to_json(item: fishpi_sdk::model::breezemoon::BreezemoonContent) -> Value {
    json!({
        "id": item.oId,
        "authorName": item.author_name,
        "updated": item.updated,
        "created": item.created,
        "timeAgo": item.time_ago,
        "content": item.content,
        "createTime": item.create_time,
        "city": item.city,
        "avatar": item.thumbnail_url48,
    })
}

pub(crate) fn barrager_to_json(msg: BarragerMsg) -> Value {
    json!({
        "oId": format!("barrager:{}:{}", msg.user_name, msg.barrager_content),
        "userName": msg.user_name,
        "userNickname": msg.user_nickname,
        "userAvatarURL": msg.user_avatar_url48,
        "content": msg.barrager_content,
        "md": msg.barrager_content,
        "barragerColor": msg.barrager_color,
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
                "userName": user.user_name.clone(),
                "avatar": user.user_avatar_url.clone(),
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
        "whoGive": status.who_give,
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
                "userId": got.user_id,
                "userName": got.user_name,
                "avatar": got.avatar,
                "userMoney": got.user_money,
                "time": got.time,
            })
        })
        .collect::<Vec<_>>();

    json!({
        "info": {
            "count": info.info.count,
            "gesture": info.info.gesture.map(|g| g as u8),
            "got": info.info.got,
            "message": info.info.message,
            "userName": info.info.user_name,
            "userAvatarURL": info.info.user_avatar_url,
        },
        "receivers": info.receivers,
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
        | ChatRoomEventData::Weather(msg) => json!({
            "event": "message",
            "message": chat_message_to_json(msg, self_username),
        }),
        ChatRoomEventData::Music(msg) => json!({
            "event": "message",
            "message": music_message_to_json(msg),
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
            "actorReaction": reaction.actor_reaction,
            "actorUserId": reaction.actor_user_id,
            "targetType": reaction.target_type,
            "groupType": reaction.group_type,
            "data": reaction.raw,
        }),
    }
}
