//! 职业成长相关响应的 wire 层类型。

use serde::Deserialize;
use std::collections::BTreeMap;

use super::de::{f64_lenient, i64_lenient, option_i64_lenient, string_lossy, u32_lenient};

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionSettings {
    #[serde(
        rename = "primaryProfessionId",
        default,
        deserialize_with = "string_lossy"
    )]
    pub primary_profession_id: String,
    #[serde(default)]
    pub progress: Vec<WireProfessionProgress>,
    #[serde(rename = "availableProfessions", default)]
    pub available_professions: Vec<WireProfessionSummary>,
    #[serde(rename = "growthGuides", default)]
    pub growth_guides: BTreeMap<String, Vec<String>>,
    #[serde(rename = "privacyPreset", default, deserialize_with = "string_lossy")]
    pub privacy_preset: String,
    #[serde(rename = "moduleVisibility", default)]
    pub module_visibility: WireProfessionModuleVisibility,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionProgress {
    #[serde(rename = "professionId", default, deserialize_with = "string_lossy")]
    pub profession_id: String,
    #[serde(rename = "totalExperience", default, deserialize_with = "i64_lenient")]
    pub total_experience: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionSummary {
    #[serde(
        rename = "professionId",
        alias = "id",
        default,
        deserialize_with = "string_lossy"
    )]
    pub profession_id: String,
    #[serde(
        rename = "displayName",
        alias = "professionName",
        alias = "name",
        default,
        deserialize_with = "string_lossy"
    )]
    pub display_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub code: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionModuleVisibility {
    #[serde(default)]
    pub professions: bool,
    #[serde(default)]
    pub experience: bool,
    #[serde(default)]
    pub contributions: bool,
    #[serde(default)]
    pub activity: bool,
    #[serde(default)]
    pub ranking: bool,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionDetail {
    #[serde(default)]
    pub profession: WireProfessionDisplay,
    #[serde(default)]
    pub levels: Vec<WireProfessionLevel>,
    #[serde(default)]
    pub rarity: WireProfessionRarity,
    #[serde(default)]
    pub contributions: Vec<WireProfessionContribution>,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionDisplay {
    #[serde(
        rename = "professionId",
        alias = "id",
        default,
        deserialize_with = "string_lossy"
    )]
    pub profession_id: String,
    #[serde(
        rename = "displayName",
        alias = "professionName",
        alias = "name",
        default,
        deserialize_with = "string_lossy"
    )]
    pub display_name: String,
    #[serde(rename = "levelCode", default, deserialize_with = "string_lossy")]
    pub level_code: String,
    #[serde(
        rename = "levelName",
        alias = "levelDisplayName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub level_name: String,
    #[serde(rename = "totalExperience", default, deserialize_with = "i64_lenient")]
    pub total_experience: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionLevel {
    #[serde(default, deserialize_with = "string_lossy")]
    pub code: String,
    #[serde(
        rename = "displayName",
        alias = "levelName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub display_name: String,
    #[serde(
        rename = "requiredExperience",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub required_experience: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRarity {
    #[serde(default, deserialize_with = "u32_lenient")]
    pub rank: u32,
    #[serde(rename = "surpassedPercent", default, deserialize_with = "f64_lenient")]
    pub surpassed_percent: f64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionContribution {
    #[serde(rename = "actionCode", default, deserialize_with = "string_lossy")]
    pub action_code: String,
    #[serde(rename = "sourceLabel", default, deserialize_with = "string_lossy")]
    pub source_label: String,
    #[serde(rename = "eventCount", default, deserialize_with = "u32_lenient")]
    pub event_count: u32,
    #[serde(rename = "experienceSum", default, deserialize_with = "i64_lenient")]
    pub experience_sum: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRecordsPage {
    #[serde(default)]
    pub records: Vec<WireProfessionRecord>,
    #[serde(rename = "nextCursor", default)]
    pub next_cursor: Option<WireProfessionRecordsCursor>,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRecord {
    #[serde(
        rename = "effectId",
        alias = "oId",
        alias = "id",
        default,
        deserialize_with = "string_lossy"
    )]
    pub effect_id: String,
    #[serde(rename = "actionCode", default, deserialize_with = "string_lossy")]
    pub action_code: String,
    #[serde(rename = "sourceLabel", default, deserialize_with = "string_lossy")]
    pub source_label: String,
    #[serde(rename = "experienceDelta", default, deserialize_with = "i64_lenient")]
    pub experience_delta: i64,
    #[serde(rename = "beforeExperience", default, deserialize_with = "i64_lenient")]
    pub before_experience: i64,
    #[serde(rename = "afterExperience", default, deserialize_with = "i64_lenient")]
    pub after_experience: i64,
    #[serde(rename = "occurredAt", default, deserialize_with = "i64_lenient")]
    pub occurred_at: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRecordsCursor {
    #[serde(rename = "beforeOccurredAt", default, deserialize_with = "i64_lenient")]
    pub before_occurred_at: i64,
    #[serde(rename = "beforeEffectId", default, deserialize_with = "string_lossy")]
    pub before_effect_id: String,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WirePublicProfessionProfile {
    #[serde(rename = "primaryProfession", default)]
    pub primary_profession: Option<WireProfessionDisplay>,
    #[serde(default)]
    pub professions: Vec<WireProfessionDisplay>,
    #[serde(rename = "majorContributions", default)]
    pub major_contributions: Vec<WireProfessionContribution>,
    #[serde(rename = "contributionStats", default)]
    pub contribution_stats: Vec<WireProfessionContribution>,
    #[serde(rename = "activityFeed", default)]
    pub activity_feed: Vec<WireProfessionActivity>,
    #[serde(rename = "levelHistory", default)]
    pub level_history: Vec<WireProfessionLevelHistory>,
    #[serde(default)]
    pub rewards: Vec<WireProfessionReward>,
    #[serde(default)]
    pub ranking: Vec<WireProfessionRankingEntry>,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionActivity {
    #[serde(rename = "actionCode", default, deserialize_with = "string_lossy")]
    pub action_code: String,
    #[serde(rename = "sourceLabel", default, deserialize_with = "string_lossy")]
    pub source_label: String,
    #[serde(rename = "experienceDelta", default, deserialize_with = "i64_lenient")]
    pub experience_delta: i64,
    #[serde(rename = "occurredAt", default, deserialize_with = "i64_lenient")]
    pub occurred_at: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionLevelHistory {
    #[serde(rename = "professionId", default, deserialize_with = "string_lossy")]
    pub profession_id: String,
    #[serde(rename = "beforeLevelCode", default, deserialize_with = "string_lossy")]
    pub before_level_code: String,
    #[serde(rename = "afterLevelCode", default, deserialize_with = "string_lossy")]
    pub after_level_code: String,
    #[serde(rename = "occurredAt", default, deserialize_with = "i64_lenient")]
    pub occurred_at: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionReward {
    #[serde(rename = "rewardType", default, deserialize_with = "string_lossy")]
    pub reward_type: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub label: String,
    #[serde(rename = "grantedAt", default, deserialize_with = "i64_lenient")]
    pub granted_at: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRanking {
    #[serde(rename = "selectedProfession", default)]
    pub selected_profession: WireProfessionSummary,
    #[serde(default)]
    pub professions: Vec<WireProfessionSummary>,
    #[serde(default)]
    pub entries: Vec<WireProfessionRankingEntry>,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRankingEntry {
    #[serde(default, deserialize_with = "u32_lenient")]
    pub rank: u32,
    #[serde(rename = "userName", default, deserialize_with = "string_lossy")]
    pub user_name: String,
    #[serde(rename = "userNickname", default, deserialize_with = "string_lossy")]
    pub user_nickname: String,
    #[serde(rename = "userAvatarURL48", default, deserialize_with = "string_lossy")]
    pub user_avatar_url48: String,
    #[serde(rename = "professionId", default, deserialize_with = "string_lossy")]
    pub profession_id: String,
    #[serde(
        rename = "professionName",
        alias = "displayName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub profession_name: String,
    #[serde(rename = "levelName", default, deserialize_with = "string_lossy")]
    pub level_name: String,
    #[serde(
        rename = "totalExperience",
        default,
        deserialize_with = "option_i64_lenient"
    )]
    pub total_experience: Option<i64>,
    #[serde(rename = "surpassedPercent", default, deserialize_with = "f64_lenient")]
    pub surpassed_percent: f64,
}
