use crate::wire::reaction::{
    WireReactionMutationResult, WireReactionState, WireReactionSummaryItem, WireReactionUserDetail,
};

#[derive(Clone, Debug)]
pub struct ReactionUserDetail {
    pub user_name: String,
    pub display_name: String,
    pub avatar_url: String,
}

impl From<WireReactionUserDetail> for ReactionUserDetail {
    fn from(wire: WireReactionUserDetail) -> Self {
        Self {
            user_name: wire.user_name,
            display_name: wire.display_name,
            avatar_url: wire.avatar_url,
        }
    }
}

#[derive(Clone, Debug)]
pub struct ReactionSummaryItem {
    pub value: String,
    pub emoji: String,
    pub count: u64,
    pub selected: bool,
    pub users: Vec<String>,
    pub user_details: Vec<ReactionUserDetail>,
}

impl From<WireReactionSummaryItem> for ReactionSummaryItem {
    fn from(wire: WireReactionSummaryItem) -> Self {
        Self {
            value: wire.value,
            emoji: wire.emoji,
            count: wire.count,
            selected: wire.selected,
            users: wire.users,
            user_details: wire.user_details.into_iter().map(Into::into).collect(),
        }
    }
}

#[derive(Clone, Debug)]
pub struct ReactionState {
    pub reaction_summary: Vec<ReactionSummaryItem>,
    pub current_user_reaction: String,
}

impl From<WireReactionState> for ReactionState {
    fn from(wire: WireReactionState) -> Self {
        Self {
            reaction_summary: wire.reaction_summary.into_iter().map(Into::into).collect(),
            current_user_reaction: wire.current_user_reaction,
        }
    }
}

#[derive(Clone, Debug)]
pub struct ReactionMutationResult {
    pub target_id: String,
    pub target_type: String,
    pub group_type: String,
    pub current_user_reaction: String,
    pub summary: Vec<ReactionSummaryItem>,
}

impl From<WireReactionMutationResult> for ReactionMutationResult {
    fn from(wire: WireReactionMutationResult) -> Self {
        Self {
            target_id: wire.target_id,
            target_type: wire.target_type,
            group_type: wire.group_type,
            current_user_reaction: wire.current_user_reaction,
            summary: wire.summary.into_iter().map(Into::into).collect(),
        }
    }
}
