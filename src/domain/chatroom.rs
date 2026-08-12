use crate::domain::reaction::ReactionSummaryItem;
use crate::domain::redpacket::RedPacketStatusMsg;
use crate::domain::user::Metal;
use crate::ids::UserId;
use crate::wire::chatroom::{
    WireBarragerMsg, WireChatReactionMsg, WireChatRoomMsg, WireOnlineInfo, payload_type,
};
use serde_json::Value;

/// 聊天室消息内容类别（用于区分历史消息中的红包/音乐/天气/普通消息）。
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ChatRoomContentKind {
    Normal,
    RedPacket,
    Music,
    Weather,
}

/// 在线用户。
#[derive(Clone, Debug)]
pub struct OnlineInfo {
    pub home_page: String,
    pub user_avatar_url: String,
    pub user_name: String,
}

impl From<WireOnlineInfo> for OnlineInfo {
    fn from(w: WireOnlineInfo) -> Self {
        Self {
            home_page: w.home_page,
            user_avatar_url: w.user_avatar_url,
            user_name: w.user_name,
        }
    }
}

/// 弹幕消息。
#[derive(Clone, Debug)]
pub struct BarragerMsg {
    pub user_name: String,
    pub user_nickname: String,
    pub barrager_content: String,
    pub barrager_color: String,
    pub user_avatar_url: String,
    pub user_avatar_url20: String,
    pub user_avatar_url48: String,
    pub user_avatar_url210: String,
}

impl From<WireBarragerMsg> for BarragerMsg {
    fn from(w: WireBarragerMsg) -> Self {
        Self {
            user_name: w.user_name,
            user_nickname: w.user_nickname,
            barrager_content: w.barrager_content,
            barrager_color: w.barrager_color,
            user_avatar_url: w.user_avatar_url,
            user_avatar_url20: w.user_avatar_url20,
            user_avatar_url48: w.user_avatar_url48,
            user_avatar_url210: w.user_avatar_url210,
        }
    }
}

/// 聊天室表态消息。
#[derive(Clone, Debug)]
pub struct ChatReactionMsg {
    pub id: String,
    pub target_type: String,
    pub group_type: String,
    pub summary: Vec<ReactionSummaryItem>,
    pub actor_user_id: String,
    pub actor_reaction: String,
}

impl From<WireChatReactionMsg> for ChatReactionMsg {
    fn from(w: WireChatReactionMsg) -> Self {
        Self {
            id: w.o_id,
            target_type: w.target_type,
            group_type: w.group_type,
            summary: w.summary.into_iter().map(Into::into).collect(),
            actor_user_id: w.actor_user_id,
            actor_reaction: w.actor_reaction,
        }
    }
}

/// 进出场消息。
#[derive(Clone, Debug)]
pub struct CustomMsg {
    pub message: String,
}

/// 禁言成员。
#[derive(Clone, Debug)]
pub struct MuteItem {
    pub time: u64,
    pub user_avatar_url: String,
    pub user_name: String,
    pub user_nickname: String,
}

impl From<crate::wire::chatroom::WireMuteItem> for MuteItem {
    fn from(w: crate::wire::chatroom::WireMuteItem) -> Self {
        Self {
            time: w.time,
            user_avatar_url: w.user_avatar_url,
            user_name: w.user_name,
            user_nickname: w.user_nickname,
        }
    }
}

/// 弹幕花费。
#[derive(Clone, Debug)]
pub struct BarragerCost {
    pub cost: u32,
    pub unit: String,
}

/// 聊天室消息。
#[derive(Clone, Debug)]
pub struct ChatRoomMsg {
    pub id: String,
    pub time: String,
    pub user_id: UserId,
    pub user_name: String,
    pub user_nickname: String,
    pub user_avatar_url: String,
    pub metals: Vec<Metal>,
    pub content: Value,
    pub md: String,
    pub client: String,
    pub reaction_summary: Vec<ReactionSummaryItem>,
    pub current_user_reaction: String,
}

impl ChatRoomMsg {
    pub fn display_name(&self) -> &str {
        if self.user_nickname.is_empty() {
            &self.user_name
        } else {
            &self.user_nickname
        }
    }

    /// 根据 `content` 内嵌类型判断消息类别（历史消息用此区分红包/音乐/天气）。
    pub fn content_kind(&self) -> ChatRoomContentKind {
        match payload_type(&self.content).as_deref() {
            Some(t) if t.eq_ignore_ascii_case("redPacket") => ChatRoomContentKind::RedPacket,
            Some(t) if t.eq_ignore_ascii_case("music") => ChatRoomContentKind::Music,
            Some(t) if t.eq_ignore_ascii_case("weather") => ChatRoomContentKind::Weather,
            _ => ChatRoomContentKind::Normal,
        }
    }
}

impl From<WireChatRoomMsg> for ChatRoomMsg {
    fn from(w: WireChatRoomMsg) -> Self {
        let client = if w.client.trim().is_empty() {
            "Other".to_string()
        } else {
            w.client
        };
        Self {
            id: w.o_id,
            time: w.time,
            user_id: w.user_o_id.into(),
            user_name: w.user_name,
            user_nickname: w.user_nickname,
            user_avatar_url: w.user_avatar_url,
            metals: w.sys_metal.into_iter().map(Metal::from).collect(),
            content: w.content,
            md: w.md,
            client,
            reaction_summary: w.reaction_summary.into_iter().map(Into::into).collect(),
            current_user_reaction: w.current_user_reaction,
        }
    }
}

/// 音乐消息。
#[derive(Clone, Debug)]
pub struct MusicMsg {
    pub base: ChatRoomMsg,
    pub cover_url: String,
    pub source: String,
    pub title: String,
    pub from: String,
}

impl MusicMsg {
    pub(crate) fn from_base(base: ChatRoomMsg) -> Self {
        let field = |k: &str| {
            base.content
                .get(k)
                .and_then(Value::as_str)
                .unwrap_or_default()
                .to_string()
        };
        Self {
            cover_url: field("coverURL"),
            source: field("source"),
            title: field("title"),
            from: field("from"),
            base,
        }
    }
}

/// 聊天室推送事件。
#[derive(Clone, Debug)]
pub enum ChatRoomEvent {
    Online {
        users: Vec<OnlineInfo>,
        discussing: Option<String>,
        online_chat_cnt: Option<usize>,
    },
    DiscussChanged(String),
    Revoke(String),
    Msg(ChatRoomMsg),
    Barrager(BarragerMsg),
    RedPacket(ChatRoomMsg),
    RedPacketStatus(RedPacketStatusMsg),
    Music(MusicMsg),
    Weather(ChatRoomMsg),
    Custom(CustomMsg),
    ChatReaction(ChatReactionMsg),
}

/// 事件类别（监听器键）。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum ChatRoomEventKind {
    Online,
    DiscussChanged,
    Revoke,
    Msg,
    Barrager,
    RedPacket,
    RedPacketStatus,
    Music,
    Weather,
    Custom,
    ChatReaction,
    All,
}

/// 客户端类型标识。
#[derive(Clone, Copy, Debug)]
pub enum ChatContentType {
    Markdown,
    Html,
}

impl ChatContentType {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Markdown => "Markdown",
            Self::Html => "Html",
        }
    }
}

/// 取历史消息的方位模式。
#[derive(Clone, Copy, Debug)]
pub enum ChatRoomMessageMode {
    Context,
    Before,
    After,
}

impl ChatRoomMessageMode {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Context => "0",
            Self::Before => "1",
            Self::After => "2",
        }
    }
}
