use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::domain::profession::{
    ProfessionActivity, ProfessionContribution, ProfessionDetail, ProfessionDisplay,
    ProfessionLevel, ProfessionLevelHistory, ProfessionModuleVisibility, ProfessionPrivacyPreset,
    ProfessionProfilePosition, ProfessionRanking, ProfessionRankingEntry, ProfessionRecord,
    ProfessionRecordsCursor, ProfessionRecordsPage, ProfessionReward, ProfessionSettings,
    ProfessionSummary, PublicProfessionProfile,
};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong, jstring};
use serde_json::{Value, json};
use std::time::Duration;
use tokio::time::timeout;

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getProfessionMe(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let settings = timeout(Duration::from_secs(15), client.profession().me())
                    .await
                    .map_err(|_| "读取职业设置超时".to_string())?
                    .map_err(|err| format!("读取职业设置失败: {err}"))?;
                Ok(profession_settings_to_json(settings))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getProfessionDetail(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    profession_id: JString,
    dark: jboolean,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, profession_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let detail = timeout(
                    Duration::from_secs(15),
                    client.profession().detail(id.trim(), dark != 0),
                )
                .await
                .map_err(|_| "读取职业详情超时".to_string())?
                .map_err(|err| format!("读取职业详情失败: {err}"))?;
                Ok(profession_detail_to_json(detail))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getProfessionRecords(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    profession_id: JString,
    before_occurred_at: jlong,
    before_effect_id: JString,
    limit: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, profession_id)?;
        let effect_id = string_arg(&mut env, before_effect_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let page = timeout(
                    Duration::from_secs(15),
                    client.profession().records(
                        id.trim(),
                        (before_occurred_at > 0).then_some(before_occurred_at),
                        (!effect_id.trim().is_empty()).then_some(effect_id.trim()),
                        limit.clamp(1, 100) as u32,
                    ),
                )
                .await
                .map_err(|_| "读取职业记录超时".to_string())?
                .map_err(|err| format!("读取职业记录失败: {err}"))?;
                Ok(profession_records_page_to_json(page))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_setPrimaryProfession(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    profession_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, profession_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let settings = timeout(
                    Duration::from_secs(15),
                    client.profession().set_primary(id.trim()),
                )
                .await
                .map_err(|_| "更新主职业超时".to_string())?
                .map_err(|err| format!("更新主职业失败: {err}"))?;
                Ok(profession_settings_to_json(settings))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_skipProfessionPrompt(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                timeout(
                    Duration::from_secs(15),
                    client.profession().skip_first_prompt(),
                )
                .await
                .map_err(|_| "结束职业提示超时".to_string())?
                .map_err(|err| format!("结束职业提示失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_updateProfessionPrivacy(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    preset: JString,
    module_visibility_json: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let preset_raw = string_arg(&mut env, preset)?;
        let visibility_json = string_arg(&mut env, module_visibility_json)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let preset = ProfessionPrivacyPreset::from(preset_raw.as_str());
                let visibility = (preset == ProfessionPrivacyPreset::Custom
                    && !visibility_json.trim().is_empty())
                .then(|| ProfessionModuleVisibility::from_json(&visibility_json));
                let settings = timeout(
                    Duration::from_secs(15),
                    client.profession().update_privacy(preset, visibility),
                )
                .await
                .map_err(|_| "更新职业隐私超时".to_string())?
                .map_err(|err| format!("更新职业隐私失败: {err}"))?;
                Ok(profession_settings_to_json(settings))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserProfession(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    user_name: JString,
    position: JString,
    dark: jboolean,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let name = string_arg(&mut env, user_name)?;
        let pos = string_arg(&mut env, position)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let profile = timeout(
                    Duration::from_secs(15),
                    client.profession().public_profile(
                        name.trim(),
                        ProfessionProfilePosition::from(pos.as_str()),
                        dark != 0,
                    ),
                )
                .await
                .map_err(|_| "读取公开职业资料超时".to_string())?
                .map_err(|err| format!("读取公开职业资料失败: {err}"))?;
                Ok(public_profession_profile_to_json(profile))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getProfessionRanking(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    profession_id: JString,
    dark: jboolean,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, profession_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let ranking = timeout(
                    Duration::from_secs(15),
                    client
                        .profession()
                        .ranking((!id.trim().is_empty()).then_some(id.trim()), dark != 0),
                )
                .await
                .map_err(|_| "读取职业排行超时".to_string())?
                .map_err(|err| format!("读取职业排行失败: {err}"))?;
                Ok(profession_ranking_to_json(ranking))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());
    to_jstring(&mut env, result)
}

fn profession_settings_to_json(v: ProfessionSettings) -> Value {
    let privacy_preset = v.privacy_preset;
    json!({
        "primaryProfessionId": v.primary_profession_id,
        "progress": v.progress.into_iter().map(|p| json!({
            "professionId": p.profession_id,
            "displayName": p.display_name,
            "professionCode": p.profession_code,
            "shortName": p.short_name,
            "description": p.description,
            "levelCode": p.level_code,
            "levelName": p.level_name,
            "levelDescription": p.level_description,
            "requiredExperience": p.required_experience,
            "totalExperience": p.total_experience,
        })).collect::<Vec<_>>(),
        "availableProfessions": v.available_professions.into_iter().map(profession_summary_to_json).collect::<Vec<_>>(),
        "growthGuides": v.growth_guides,
        "privacyPreset": privacy_preset.as_str(),
        "moduleVisibility": module_visibility_to_json(v.module_visibility),
    })
}

fn profession_summary_to_json(v: ProfessionSummary) -> Value {
    json!({
        "professionId": v.profession_id,
        "displayName": v.display_name,
        "professionCode": v.profession_code,
        "shortName": v.short_name,
        "code": v.code,
        "description": v.description,
    })
}

fn module_visibility_to_json(v: ProfessionModuleVisibility) -> Value {
    json!({
        "primaryProfession": v.primary_profession,
        "allProfessions": v.all_professions,
        "experience": v.experience,
        "rarity": v.rarity,
        "majorContributions": v.major_contributions,
        "contributionStats": v.contribution_stats,
        "activityFeed": v.activity_feed,
        "levelHistory": v.level_history,
        "rewards": v.rewards,
        "ranking": v.ranking,
    })
}

fn profession_detail_to_json(v: ProfessionDetail) -> Value {
    json!({
        "profession": profession_display_to_json(v.profession),
        "levels": v.levels.into_iter().map(profession_level_to_json).collect::<Vec<_>>(),
        "rarity": profession_rarity_to_json(v.rarity),
        "contributions": v.contributions.into_iter().map(profession_contribution_to_json).collect::<Vec<_>>(),
    })
}

fn profession_display_to_json(v: ProfessionDisplay) -> Value {
    json!({
        "professionId": v.profession_id,
        "displayName": v.display_name,
        "levelCode": v.level_code,
        "levelName": v.level_name,
        "totalExperience": v.total_experience,
    })
}

fn profession_level_to_json(v: ProfessionLevel) -> Value {
    json!({
        "code": v.code,
        "displayName": v.display_name,
        "requiredExperience": v.required_experience,
    })
}

fn profession_rarity_to_json(v: fishpi_sdk::domain::profession::ProfessionRarity) -> Value {
    json!({
        "rank": v.rank,
        "surpassedPercent": v.surpassed_percent,
    })
}

fn profession_contribution_to_json(v: ProfessionContribution) -> Value {
    json!({
        "professionId": v.profession_id,
        "actionCode": v.action_code,
        "sourceLabel": v.source_label,
        "eventCount": v.event_count,
        "experienceSum": v.experience_sum,
        "lastOccurredAt": v.last_occurred_at,
    })
}

fn profession_records_page_to_json(v: ProfessionRecordsPage) -> Value {
    json!({
        "records": v.records.into_iter().map(profession_record_to_json).collect::<Vec<_>>(),
        "nextCursor": v.next_cursor.map(profession_cursor_to_json),
    })
}

fn profession_record_to_json(v: ProfessionRecord) -> Value {
    json!({
        "effectId": v.effect_id,
        "actionCode": v.action_code,
        "sourceLabel": v.source_label,
        "experienceDelta": v.experience_delta,
        "beforeExperience": v.before_experience,
        "afterExperience": v.after_experience,
        "occurredAt": v.occurred_at,
    })
}

fn profession_cursor_to_json(v: ProfessionRecordsCursor) -> Value {
    json!({
        "beforeOccurredAt": v.before_occurred_at,
        "beforeEffectId": v.before_effect_id,
    })
}

fn public_profession_profile_to_json(v: PublicProfessionProfile) -> Value {
    json!({
        "primaryProfession": v.primary_profession.map(profession_display_to_json),
        "professions": v.professions.into_iter().map(profession_display_to_json).collect::<Vec<_>>(),
        "majorContributions": v.major_contributions.into_iter().map(profession_contribution_to_json).collect::<Vec<_>>(),
        "contributionStats": v.contribution_stats.into_iter().map(profession_contribution_to_json).collect::<Vec<_>>(),
        "activityFeed": v.activity_feed.into_iter().map(profession_activity_to_json).collect::<Vec<_>>(),
        "levelHistory": v.level_history.into_iter().map(profession_level_history_to_json).collect::<Vec<_>>(),
        "rewards": v.rewards.into_iter().map(profession_reward_to_json).collect::<Vec<_>>(),
        "ranking": v.ranking.into_iter().map(profession_ranking_entry_to_json).collect::<Vec<_>>(),
    })
}

fn profession_activity_to_json(v: ProfessionActivity) -> Value {
    json!({
        "professionId": v.profession_id,
        "actionCode": v.action_code,
        "sourceLabel": v.source_label,
        "experienceDelta": v.experience_delta,
        "occurredAt": v.occurred_at,
    })
}

fn profession_level_history_to_json(v: ProfessionLevelHistory) -> Value {
    json!({
        "professionId": v.profession_id,
        "beforeLevelCode": v.before_level_code,
        "afterLevelCode": v.after_level_code,
        "occurredAt": v.occurred_at,
    })
}

fn profession_reward_to_json(v: ProfessionReward) -> Value {
    json!({
        "rewardType": v.reward_type,
        "label": v.label,
        "grantedAt": v.granted_at,
    })
}

fn profession_ranking_to_json(v: ProfessionRanking) -> Value {
    json!({
        "selectedProfession": profession_summary_to_json(v.selected_profession),
        "professions": v.professions.into_iter().map(profession_summary_to_json).collect::<Vec<_>>(),
        "entries": v.entries.into_iter().map(profession_ranking_entry_to_json).collect::<Vec<_>>(),
    })
}

fn profession_ranking_entry_to_json(v: ProfessionRankingEntry) -> Value {
    json!({
        "rank": v.rank,
        "userName": v.user_name,
        "userNickname": v.user_nickname,
        "userAvatarURL48": v.user_avatar_url48,
        "professionId": v.profession_id,
        "professionName": v.profession_name,
        "levelName": v.level_name,
        "primaryColor": v.primary_color,
        "iconUrl": v.icon_url,
        "totalExperience": v.total_experience,
        "surpassedPercent": v.surpassed_percent,
    })
}
