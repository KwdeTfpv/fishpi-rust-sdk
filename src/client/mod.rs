//! 客户端。

mod article;
mod breezemoon;
mod chat;
mod chatroom;
mod comment;
mod core;
mod emoji;
mod finger;
mod misc;
mod notice;
mod profession;
mod reaction;
mod redpacket;
mod user;

pub use core::{FishPi, FishPiBuilder};

pub use article::{ArticleApi, ArticleConnection, ArticleDraftSave, ArticleEventKind, ArticlePost};
pub use breezemoon::BreezemoonApi;
pub use chat::{ChatApi, ChatConnection};
pub use chatroom::{ChatRoomApi, ChatRoomConnection};
pub use comment::CommentApi;
pub use emoji::EmojiApi;
pub use finger::FingerApi;
pub use notice::{NoticeApi, NoticeConnection, NoticeEventKind};
pub use profession::ProfessionApi;
pub use reaction::ReactionApi;
pub use redpacket::{RedPacketApi, RedPacketSend};
pub use user::UserApi;

use crate::utils::HttpProxyConfig;
use std::time::Duration;

pub(crate) const DEFAULT_DOMAIN: &str = "fishpi.cn";
pub(crate) const DEFAULT_CONNECT_TIMEOUT: Duration = Duration::from_secs(8);
pub(crate) const DEFAULT_TIMEOUT: Duration = Duration::from_secs(15);
pub(crate) const USER_AGENT: &str = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36";

fn default_client_tag() -> String {
    format!("Rust/{}", env!("CARGO_PKG_VERSION"))
}

#[derive(Clone, Debug)]
pub(crate) struct Config {
    pub domain: String,
    pub connect_timeout: Duration,
    pub timeout: Duration,
    pub proxy: HttpProxyConfig,
    pub max_retries: u32,
    pub client_tag: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            domain: DEFAULT_DOMAIN.to_string(),
            connect_timeout: DEFAULT_CONNECT_TIMEOUT,
            timeout: DEFAULT_TIMEOUT,
            proxy: HttpProxyConfig::no_proxy(),
            max_retries: 2,
            client_tag: default_client_tag(),
        }
    }
}
