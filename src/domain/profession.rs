//! 职业成长相关的干净对外类型。

use std::collections::BTreeMap;

use crate::ids::{ProfessionId, UserName};
use crate::wire::profession::{
    WireProfessionActivity, WireProfessionContribution, WireProfessionDetail,
    WireProfessionDisplay, WireProfessionLevel, WireProfessionLevelHistory,
    WireProfessionModuleVisibility, WireProfessionProgress, WireProfessionRanking,
    WireProfessionRankingEntry, WireProfessionRarity, WireProfessionRecord,
    WireProfessionRecordsCursor, WireProfessionRecordsPage, WireProfessionReward,
    WireProfessionSettings, WireProfessionSummary, WirePublicProfessionProfile,
};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ProfessionPrivacyPreset {
    AllPublic,
    LevelOnly,
    PrimaryOnly,
    SelfOnly,
    FullyHidden,
    Custom,
}

impl ProfessionPrivacyPreset {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::AllPublic => "ALL_PUBLIC",
            Self::LevelOnly => "LEVEL_ONLY",
            Self::PrimaryOnly => "PRIMARY_ONLY",
            Self::SelfOnly => "SELF_ONLY",
            Self::FullyHidden => "FULLY_HIDDEN",
            Self::Custom => "CUSTOM",
        }
    }
}

impl From<&str> for ProfessionPrivacyPreset {
    fn from(value: &str) -> Self {
        match value.trim() {
            "ALL_PUBLIC" => Self::AllPublic,
            "PRIMARY_ONLY" => Self::PrimaryOnly,
            "SELF_ONLY" => Self::SelfOnly,
            "FULLY_HIDDEN" => Self::FullyHidden,
            "CUSTOM" => Self::Custom,
            _ => Self::LevelOnly,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ProfessionProfilePosition {
    HomeProfile,
    UserCard,
    ProfessionPage,
    Ranking,
    CompactMobile,
}

impl ProfessionProfilePosition {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::HomeProfile => "homeProfile",
            Self::UserCard => "userCard",
            Self::ProfessionPage => "professionPage",
            Self::Ranking => "ranking",
            Self::CompactMobile => "compactMobile",
        }
    }
}

impl From<&str> for ProfessionProfilePosition {
    fn from(value: &str) -> Self {
        match value.trim() {
            "userCard" => Self::UserCard,
            "professionPage" => Self::ProfessionPage,
            "ranking" => Self::Ranking,
            "compactMobile" => Self::CompactMobile,
            _ => Self::HomeProfile,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionSettings {
    pub primary_profession_id: ProfessionId,
    pub progress: Vec<ProfessionProgress>,
    pub available_professions: Vec<ProfessionSummary>,
    pub growth_guides: BTreeMap<String, Vec<String>>,
    pub privacy_preset: ProfessionPrivacyPreset,
    pub module_visibility: ProfessionModuleVisibility,
}

impl From<WireProfessionSettings> for ProfessionSettings {
    fn from(w: WireProfessionSettings) -> Self {
        Self {
            primary_profession_id: w.primary_profession_id.into(),
            progress: w.progress.into_iter().map(Into::into).collect(),
            available_professions: w
                .available_professions
                .into_iter()
                .map(Into::into)
                .collect(),
            growth_guides: w.growth_guides,
            privacy_preset: ProfessionPrivacyPreset::from(w.privacy_preset.as_str()),
            module_visibility: w.module_visibility.into(),
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionProgress {
    pub profession_id: ProfessionId,
    pub total_experience: i64,
}

impl From<WireProfessionProgress> for ProfessionProgress {
    fn from(w: WireProfessionProgress) -> Self {
        Self {
            profession_id: w.profession_id.into(),
            total_experience: w.total_experience,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionSummary {
    pub profession_id: ProfessionId,
    pub display_name: String,
    pub code: String,
    pub description: String,
}

impl From<WireProfessionSummary> for ProfessionSummary {
    fn from(w: WireProfessionSummary) -> Self {
        Self {
            profession_id: w.profession_id.into(),
            display_name: w.display_name,
            code: w.code,
            description: w.description,
        }
    }
}

#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ProfessionModuleVisibility {
    pub professions: bool,
    pub experience: bool,
    pub contributions: bool,
    pub activity: bool,
    pub ranking: bool,
}

impl ProfessionModuleVisibility {
    pub fn new(
        professions: bool,
        experience: bool,
        contributions: bool,
        activity: bool,
        ranking: bool,
    ) -> Self {
        Self {
            professions,
            experience,
            contributions,
            activity,
            ranking,
        }
    }
}

impl From<WireProfessionModuleVisibility> for ProfessionModuleVisibility {
    fn from(w: WireProfessionModuleVisibility) -> Self {
        Self {
            professions: w.professions,
            experience: w.experience,
            contributions: w.contributions,
            activity: w.activity,
            ranking: w.ranking,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionDetail {
    pub profession: ProfessionDisplay,
    pub levels: Vec<ProfessionLevel>,
    pub rarity: ProfessionRarity,
    pub contributions: Vec<ProfessionContribution>,
}

impl From<WireProfessionDetail> for ProfessionDetail {
    fn from(w: WireProfessionDetail) -> Self {
        Self {
            profession: w.profession.into(),
            levels: w.levels.into_iter().map(Into::into).collect(),
            rarity: w.rarity.into(),
            contributions: w.contributions.into_iter().map(Into::into).collect(),
        }
    }
}

#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ProfessionDisplay {
    pub profession_id: ProfessionId,
    pub display_name: String,
    pub level_code: String,
    pub level_name: String,
    pub total_experience: i64,
}

impl From<WireProfessionDisplay> for ProfessionDisplay {
    fn from(w: WireProfessionDisplay) -> Self {
        Self {
            profession_id: w.profession_id.into(),
            display_name: w.display_name,
            level_code: w.level_code,
            level_name: w.level_name,
            total_experience: w.total_experience,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionLevel {
    pub code: String,
    pub display_name: String,
    pub required_experience: i64,
}

impl From<WireProfessionLevel> for ProfessionLevel {
    fn from(w: WireProfessionLevel) -> Self {
        Self {
            code: w.code,
            display_name: w.display_name,
            required_experience: w.required_experience,
        }
    }
}

#[derive(Clone, Debug, Default)]
#[non_exhaustive]
pub struct ProfessionRarity {
    pub rank: u32,
    pub surpassed_percent: f64,
}

impl From<WireProfessionRarity> for ProfessionRarity {
    fn from(w: WireProfessionRarity) -> Self {
        Self {
            rank: w.rank,
            surpassed_percent: w.surpassed_percent,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionContribution {
    pub action_code: String,
    pub source_label: String,
    pub event_count: u32,
    pub experience_sum: i64,
}

impl From<WireProfessionContribution> for ProfessionContribution {
    fn from(w: WireProfessionContribution) -> Self {
        Self {
            action_code: w.action_code,
            source_label: w.source_label,
            event_count: w.event_count,
            experience_sum: w.experience_sum,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionRecordsPage {
    pub records: Vec<ProfessionRecord>,
    pub next_cursor: Option<ProfessionRecordsCursor>,
}

impl From<WireProfessionRecordsPage> for ProfessionRecordsPage {
    fn from(w: WireProfessionRecordsPage) -> Self {
        Self {
            records: w.records.into_iter().map(Into::into).collect(),
            next_cursor: w.next_cursor.map(Into::into),
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionRecord {
    pub effect_id: String,
    pub action_code: String,
    pub source_label: String,
    pub experience_delta: i64,
    pub before_experience: i64,
    pub after_experience: i64,
    pub occurred_at: i64,
}

impl From<WireProfessionRecord> for ProfessionRecord {
    fn from(w: WireProfessionRecord) -> Self {
        Self {
            effect_id: w.effect_id,
            action_code: w.action_code,
            source_label: w.source_label,
            experience_delta: w.experience_delta,
            before_experience: w.before_experience,
            after_experience: w.after_experience,
            occurred_at: w.occurred_at,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionRecordsCursor {
    pub before_occurred_at: i64,
    pub before_effect_id: String,
}

impl From<WireProfessionRecordsCursor> for ProfessionRecordsCursor {
    fn from(w: WireProfessionRecordsCursor) -> Self {
        Self {
            before_occurred_at: w.before_occurred_at,
            before_effect_id: w.before_effect_id,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct PublicProfessionProfile {
    pub primary_profession: Option<ProfessionDisplay>,
    pub professions: Vec<ProfessionDisplay>,
    pub major_contributions: Vec<ProfessionContribution>,
    pub contribution_stats: Vec<ProfessionContribution>,
    pub activity_feed: Vec<ProfessionActivity>,
    pub level_history: Vec<ProfessionLevelHistory>,
    pub rewards: Vec<ProfessionReward>,
    pub ranking: Vec<ProfessionRankingEntry>,
}

impl From<WirePublicProfessionProfile> for PublicProfessionProfile {
    fn from(w: WirePublicProfessionProfile) -> Self {
        Self {
            primary_profession: w.primary_profession.map(Into::into),
            professions: w.professions.into_iter().map(Into::into).collect(),
            major_contributions: w.major_contributions.into_iter().map(Into::into).collect(),
            contribution_stats: w.contribution_stats.into_iter().map(Into::into).collect(),
            activity_feed: w.activity_feed.into_iter().map(Into::into).collect(),
            level_history: w.level_history.into_iter().map(Into::into).collect(),
            rewards: w.rewards.into_iter().map(Into::into).collect(),
            ranking: w.ranking.into_iter().map(Into::into).collect(),
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionActivity {
    pub action_code: String,
    pub source_label: String,
    pub experience_delta: i64,
    pub occurred_at: i64,
}

impl From<WireProfessionActivity> for ProfessionActivity {
    fn from(w: WireProfessionActivity) -> Self {
        Self {
            action_code: w.action_code,
            source_label: w.source_label,
            experience_delta: w.experience_delta,
            occurred_at: w.occurred_at,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionLevelHistory {
    pub profession_id: ProfessionId,
    pub before_level_code: String,
    pub after_level_code: String,
    pub occurred_at: i64,
}

impl From<WireProfessionLevelHistory> for ProfessionLevelHistory {
    fn from(w: WireProfessionLevelHistory) -> Self {
        Self {
            profession_id: w.profession_id.into(),
            before_level_code: w.before_level_code,
            after_level_code: w.after_level_code,
            occurred_at: w.occurred_at,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionReward {
    pub reward_type: String,
    pub label: String,
    pub granted_at: i64,
}

impl From<WireProfessionReward> for ProfessionReward {
    fn from(w: WireProfessionReward) -> Self {
        Self {
            reward_type: w.reward_type,
            label: w.label,
            granted_at: w.granted_at,
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionRanking {
    pub selected_profession: ProfessionSummary,
    pub professions: Vec<ProfessionSummary>,
    pub entries: Vec<ProfessionRankingEntry>,
}

impl From<WireProfessionRanking> for ProfessionRanking {
    fn from(w: WireProfessionRanking) -> Self {
        Self {
            selected_profession: w.selected_profession.into(),
            professions: w.professions.into_iter().map(Into::into).collect(),
            entries: w.entries.into_iter().map(Into::into).collect(),
        }
    }
}

#[derive(Clone, Debug)]
#[non_exhaustive]
pub struct ProfessionRankingEntry {
    pub rank: u32,
    pub user_name: UserName,
    pub user_nickname: String,
    pub user_avatar_url48: String,
    pub profession_id: ProfessionId,
    pub profession_name: String,
    pub level_name: String,
    pub total_experience: Option<i64>,
    pub surpassed_percent: f64,
}

impl From<WireProfessionRankingEntry> for ProfessionRankingEntry {
    fn from(w: WireProfessionRankingEntry) -> Self {
        Self {
            rank: w.rank,
            user_name: w.user_name.into(),
            user_nickname: w.user_nickname,
            user_avatar_url48: w.user_avatar_url48,
            profession_id: w.profession_id.into(),
            profession_name: w.profession_name,
            level_name: w.level_name,
            total_experience: w.total_experience,
            surpassed_percent: w.surpassed_percent,
        }
    }
}
