use crate::utils::error::Error;
use serde::{Deserialize, Serialize};
use serde_json::Value;

/// Emoji Reaction 用户详情。
#[derive(Clone, Debug, Default, Deserialize, Serialize, PartialEq, Eq)]
#[allow(non_snake_case)]
pub struct ReactionUserDetail {
    /// 用户名。
    pub userName: String,
    /// 显示名，例如：只有午安(Kirito)。
    pub displayName: String,
    /// 头像地址。
    pub avatarURL: String,
}

/// Emoji Reaction 汇总项。
#[derive(Clone, Debug, Default, Deserialize, Serialize, PartialEq, Eq)]
#[allow(non_snake_case)]
pub struct ReactionSummaryItem {
    /// 表情值，例如 thumbsup。
    pub value: String,
    /// 表情字符，例如 👍。
    pub emoji: String,
    /// 当前总数。
    pub count: u64,
    /// 当前登录用户是否已选中该表情。
    pub selected: bool,
    /// 点过该表情的用户显示名列表。
    #[serde(default)]
    pub users: Vec<String>,
    /// 点过该表情的用户详情列表。
    #[serde(default)]
    pub userDetails: Vec<ReactionUserDetail>,
}

/// Emoji Reaction 通用返回数据。
#[derive(Clone, Debug, Default, Deserialize, Serialize, PartialEq, Eq)]
#[allow(non_snake_case)]
pub struct ReactionState {
    /// Reaction 汇总列表。
    #[serde(default)]
    pub reactionSummary: Vec<ReactionSummaryItem>,
    /// 当前登录用户选中的表情值，没有则为空字符串。
    #[serde(default)]
    pub currentUserReaction: String,
}

impl ReactionState {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ReactionState: {}", e)))
    }
}

/// Emoji Reaction 写入接口返回数据。
#[derive(Clone, Debug, Deserialize, Serialize, PartialEq, Eq)]
#[allow(non_snake_case)]
pub struct ReactionMutationResult {
    /// 目标对象 oId。
    pub targetId: String,
    /// 目标类型：article/comment/chat。
    pub targetType: String,
    /// Reaction 分组，目前为 emoji。
    pub groupType: String,
    /// 当前登录用户最终选中的表情值，没有则为空字符串。
    pub currentUserReaction: String,
    /// 目标对象最新的表情汇总。
    #[serde(default)]
    pub summary: Vec<ReactionSummaryItem>,
}

impl ReactionMutationResult {
    pub fn from_value(value: &Value) -> Result<Self, Error> {
        serde_json::from_value(value.clone())
            .map_err(|e| Error::Parse(format!("Failed to parse ReactionMutationResult: {}", e)))
    }
}
