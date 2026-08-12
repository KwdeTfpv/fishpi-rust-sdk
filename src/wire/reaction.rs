use crate::wire::de::string_lossy;
use serde::Deserialize;

#[derive(Clone, Debug, Deserialize)]
pub struct WireReactionUserDetail {
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(rename = "displayName", default, deserialize_with = "string_lossy")]
    pub display_name: String,
    #[serde(rename = "avatarURL", default, deserialize_with = "string_lossy")]
    pub avatar_url: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireReactionSummaryItem {
    #[serde(default, deserialize_with = "string_lossy")]
    pub value: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub emoji: String,
    #[serde(default)]
    pub count: u64,
    #[serde(default)]
    pub selected: bool,
    #[serde(default)]
    pub users: Vec<String>,
    #[serde(rename = "userDetails", default)]
    pub user_details: Vec<WireReactionUserDetail>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireReactionState {
    #[serde(rename = "reactionSummary", default)]
    pub reaction_summary: Vec<WireReactionSummaryItem>,
    #[serde(
        rename = "currentUserReaction",
        default,
        deserialize_with = "string_lossy"
    )]
    pub current_user_reaction: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WireReactionMutationResult {
    #[serde(rename = "targetId", default, deserialize_with = "string_lossy")]
    pub target_id: String,
    #[serde(rename = "targetType", default, deserialize_with = "string_lossy")]
    pub target_type: String,
    #[serde(rename = "groupType", default, deserialize_with = "string_lossy")]
    pub group_type: String,
    #[serde(
        rename = "currentUserReaction",
        default,
        deserialize_with = "string_lossy"
    )]
    pub current_user_reaction: String,
    #[serde(default)]
    pub summary: Vec<WireReactionSummaryItem>,
}
