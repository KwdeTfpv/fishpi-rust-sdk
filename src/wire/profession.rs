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
    #[serde(rename = "rewardGuides", default)]
    pub reward_guides: BTreeMap<String, Vec<WireProfessionRewardGuide>>,
    #[serde(rename = "privacyPreset", default, deserialize_with = "string_lossy")]
    pub privacy_preset: String,
    #[serde(
        rename = "moduleVisibilityJson",
        default,
        deserialize_with = "string_lossy"
    )]
    pub module_visibility_json: String,
    #[serde(
        rename = "primaryProfessionState",
        default,
        deserialize_with = "string_lossy"
    )]
    pub primary_profession_state: String,
    #[serde(rename = "onboardingState", default, deserialize_with = "string_lossy")]
    pub onboarding_state: String,
    #[serde(
        rename = "userProgressVersion",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub user_progress_version: i64,
    #[serde(
        rename = "publicStatsVersion",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub public_stats_version: i64,
    #[serde(
        rename = "userPrivacyVersion",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub user_privacy_version: i64,
    #[serde(rename = "oId", default, deserialize_with = "string_lossy")]
    pub oid: String,
    #[serde(rename = "userId", default, deserialize_with = "string_lossy")]
    pub user_id: String,
    #[serde(rename = "createdAt", default, deserialize_with = "i64_lenient")]
    pub created_at: i64,
    #[serde(rename = "updatedAt", default, deserialize_with = "i64_lenient")]
    pub updated_at: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionProgress {
    #[serde(rename = "professionId", default, deserialize_with = "string_lossy")]
    pub profession_id: String,
    #[serde(rename = "displayName", default, deserialize_with = "string_lossy")]
    pub display_name: String,
    #[serde(rename = "professionCode", default, deserialize_with = "string_lossy")]
    pub profession_code: String,
    #[serde(rename = "shortName", default, deserialize_with = "string_lossy")]
    pub short_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(rename = "levelCode", default, deserialize_with = "string_lossy")]
    pub level_code: String,
    #[serde(rename = "levelName", default, deserialize_with = "string_lossy")]
    pub level_name: String,
    #[serde(
        rename = "levelDescription",
        default,
        deserialize_with = "string_lossy"
    )]
    pub level_description: String,
    #[serde(
        rename = "requiredExperience",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub required_experience: i64,
    #[serde(rename = "totalExperience", default, deserialize_with = "i64_lenient")]
    pub total_experience: i64,
    #[serde(default)]
    pub presentation: WireProfessionPresentation,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRewardGuide {
    #[serde(rename = "rewardType", default, deserialize_with = "string_lossy")]
    pub reward_type: String,
    #[serde(default, deserialize_with = "i64_lenient")]
    pub amount: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionPresentation {
    #[serde(rename = "backgroundColor", default, deserialize_with = "string_lossy")]
    pub background_color: String,
    #[serde(rename = "borderColor", default, deserialize_with = "string_lossy")]
    pub border_color: String,
    #[serde(rename = "borderWidth", default, deserialize_with = "string_lossy")]
    pub border_width: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub glow: String,
    #[serde(rename = "iconUrl", default, deserialize_with = "string_lossy")]
    pub icon_url: String,
    #[serde(rename = "imageUrl", default, deserialize_with = "string_lossy")]
    pub image_url: String,
    #[serde(
        rename = "levelUpAnimation",
        default,
        deserialize_with = "string_lossy"
    )]
    pub level_up_animation: String,
    #[serde(rename = "primaryColor", default, deserialize_with = "string_lossy")]
    pub primary_color: String,
    #[serde(rename = "progressStyle", default, deserialize_with = "string_lossy")]
    pub progress_style: String,
    #[serde(rename = "secondaryColor", default, deserialize_with = "string_lossy")]
    pub secondary_color: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub shadow: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub shape: String,
    #[serde(rename = "textColor", default, deserialize_with = "string_lossy")]
    pub text_color: String,
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
    #[serde(rename = "professionCode", default, deserialize_with = "string_lossy")]
    pub profession_code: String,
    #[serde(rename = "shortName", default, deserialize_with = "string_lossy")]
    pub short_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub code: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(default)]
    pub presentation: WireProfessionPresentation,
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
    #[serde(rename = "professionCode", default, deserialize_with = "string_lossy")]
    pub profession_code: String,
    #[serde(rename = "shortName", default, deserialize_with = "string_lossy")]
    pub short_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(rename = "levelCode", default, deserialize_with = "string_lossy")]
    pub level_code: String,
    #[serde(
        rename = "levelName",
        alias = "levelDisplayName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub level_name: String,
    #[serde(
        rename = "levelDescription",
        default,
        deserialize_with = "string_lossy"
    )]
    pub level_description: String,
    #[serde(
        rename = "requiredExperience",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub required_experience: i64,
    #[serde(rename = "totalExperience", default, deserialize_with = "i64_lenient")]
    pub total_experience: i64,
    #[serde(default)]
    pub presentation: WireProfessionPresentation,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionLevel {
    #[serde(
        rename = "levelCode",
        alias = "code",
        default,
        deserialize_with = "string_lossy"
    )]
    pub code: String,
    #[serde(
        rename = "displayName",
        alias = "levelName",
        default,
        deserialize_with = "string_lossy"
    )]
    pub display_name: String,
    #[serde(rename = "shortName", default, deserialize_with = "string_lossy")]
    pub short_name: String,
    #[serde(default, deserialize_with = "string_lossy")]
    pub description: String,
    #[serde(
        rename = "achievementDescription",
        default,
        deserialize_with = "string_lossy"
    )]
    pub achievement_description: String,
    #[serde(
        rename = "requiredExperience",
        default,
        deserialize_with = "i64_lenient"
    )]
    pub required_experience: i64,
    #[serde(rename = "topLevel", default)]
    pub top_level: bool,
    #[serde(rename = "sortOrder", default, deserialize_with = "i64_lenient")]
    pub sort_order: i64,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionRarity {
    #[serde(default, deserialize_with = "u32_lenient")]
    pub rank: u32,
    #[serde(rename = "surpassedPercent", default, deserialize_with = "f64_lenient")]
    pub surpassed_percent: f64,
    #[serde(rename = "totalUsers", default, deserialize_with = "u32_lenient")]
    pub total_users: u32,
}

#[derive(Clone, Debug, Deserialize, Default)]
pub struct WireProfessionContribution {
    #[serde(rename = "professionId", default, deserialize_with = "string_lossy")]
    pub profession_id: String,
    #[serde(rename = "actionCode", default, deserialize_with = "string_lossy")]
    pub action_code: String,
    #[serde(rename = "sourceLabel", default, deserialize_with = "string_lossy")]
    pub source_label: String,
    #[serde(rename = "eventCount", default, deserialize_with = "u32_lenient")]
    pub event_count: u32,
    #[serde(rename = "experienceSum", default, deserialize_with = "i64_lenient")]
    pub experience_sum: i64,
    #[serde(rename = "lastOccurredAt", default, deserialize_with = "i64_lenient")]
    pub last_occurred_at: i64,
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
    #[serde(rename = "beforeLevelCode", default, deserialize_with = "string_lossy")]
    pub before_level_code: String,
    #[serde(rename = "afterLevelCode", default, deserialize_with = "string_lossy")]
    pub after_level_code: String,
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
    #[serde(rename = "professionId", default, deserialize_with = "string_lossy")]
    pub profession_id: String,
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
    #[serde(rename = "primaryColor", default, deserialize_with = "string_lossy")]
    pub primary_color: String,
    #[serde(rename = "iconUrl", default, deserialize_with = "string_lossy")]
    pub icon_url: String,
    #[serde(
        rename = "totalExperience",
        default,
        deserialize_with = "option_i64_lenient"
    )]
    pub total_experience: Option<i64>,
    #[serde(rename = "surpassedPercent", default, deserialize_with = "f64_lenient")]
    pub surpassed_percent: f64,
}
