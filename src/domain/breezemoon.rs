//! 清风明月对外类型。

use crate::wire::breezemoon::WireBreezemoon;

/// 清风明月内容。
#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct Breezemoon {
    /// 清风明月 ID
    pub id: String,
    /// 发布者用户名
    pub author_name: String,
    /// 发布者头像 URL
    pub author_avatar: String,
    /// 正文
    pub content: String,
    /// 发布城市
    pub city: String,
    /// 创建时间
    pub created: String,
    /// 创建时间戳
    pub create_time: String,
    /// 最后更新时间
    pub updated: String,
    /// 相对发布时间
    pub time_ago: String,
}

impl From<WireBreezemoon> for Breezemoon {
    fn from(w: WireBreezemoon) -> Self {
        Breezemoon {
            id: w.o_id,
            author_name: w.author_name,
            author_avatar: w.thumbnail_url48,
            content: w.content,
            city: w.city,
            created: w.created,
            create_time: w.create_time,
            updated: w.updated,
            time_ago: w.time_ago,
        }
    }
}
