use crate::ids::UserId;
use crate::wire::chat::{WireChatData, WireChatNotice};

/// 私聊消息。
#[derive(Clone, Debug)]
pub struct ChatData {
    pub id: String,
    pub to_id: String,
    pub from_id: String,
    pub preview: String,
    pub user_session: String,
    pub sender_avatar: String,
    pub sender_user_name: String,
    pub receiver_avatar: String,
    pub receiver_user_name: String,
    pub markdown: String,
    pub content: String,
    pub time: String,
}

impl From<WireChatData> for ChatData {
    fn from(w: WireChatData) -> Self {
        Self {
            id: w.o_id,
            to_id: w.to_id,
            from_id: w.from_id,
            preview: w.preview,
            user_session: w.user_session,
            sender_avatar: w.sender_avatar,
            sender_user_name: w.sender_user_name,
            receiver_avatar: w.receiver_avatar,
            receiver_user_name: w.receiver_user_name,
            markdown: w.markdown,
            content: w.content,
            time: w.time,
        }
    }
}

/// 系统通知。
#[derive(Clone, Debug)]
pub struct ChatNotice {
    pub command: String,
    pub user_id: UserId,
    pub preview: String,
    pub sender_avatar: String,
    pub sender_user_name: String,
}

impl From<WireChatNotice> for ChatNotice {
    fn from(w: WireChatNotice) -> Self {
        Self {
            command: w.command,
            user_id: w.user_id.into(),
            preview: w.preview,
            sender_avatar: w.sender_avatar,
            sender_user_name: w.sender_user_name,
        }
    }
}

/// 撤回事件。
#[derive(Clone, Debug)]
pub struct ChatRevoke {
    pub id: String,
}

/// 私聊事件。
#[derive(Clone, Debug)]
pub enum ChatEvent {
    Notice(ChatNotice),
    Data(ChatData),
    Revoke(ChatRevoke),
}

/// 私聊事件类别（监听器键）。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum ChatEventKind {
    Notice,
    Data,
    Revoke,
}
