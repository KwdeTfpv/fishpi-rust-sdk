use crate::wire::emoji::{WireEmojiGroup, WireEmojiItem};

#[derive(Clone, Debug)]
pub struct EmojiGroup {
    pub id: String,
    pub name: String,
    pub sort: i64,
    /// 是否「全部」默认分组（服务端 `type==1`）。
    pub is_default: bool,
}

impl From<WireEmojiGroup> for EmojiGroup {
    fn from(wire: WireEmojiGroup) -> Self {
        Self {
            id: wire.o_id,
            name: wire.name,
            sort: wire.sort,
            is_default: wire.type_ == 1,
        }
    }
}

#[derive(Clone, Debug)]
pub struct EmojiItem {
    pub id: String,
    /// 表情引用 ID（增删表情用）。
    pub emoji_id: String,
    pub group_id: String,
    pub name: String,
    pub url: String,
    pub sort: i64,
}

impl From<WireEmojiItem> for EmojiItem {
    fn from(wire: WireEmojiItem) -> Self {
        Self {
            id: wire.o_id,
            emoji_id: wire.emoji_id,
            group_id: wire.group_id,
            name: wire.name,
            url: wire.url,
            sort: wire.sort,
        }
    }
}
