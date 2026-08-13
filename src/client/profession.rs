//! 职业成长域方法。

use serde_json::json;

use super::core::FishPi;
use crate::domain::profession::{
    ProfessionDetail, ProfessionModuleVisibility, ProfessionPrivacyPreset,
    ProfessionProfilePosition, ProfessionRanking, ProfessionRecordsPage, ProfessionSettings,
    PublicProfessionProfile,
};
use crate::utils::{build_http_path, error::Error};
use crate::wire::profession::{
    WireProfessionDetail, WireProfessionRanking, WireProfessionRecordsPage, WireProfessionSettings,
    WirePublicProfessionProfile,
};

/// 职业成长域访问器。
pub struct ProfessionApi(pub(crate) FishPi);

impl FishPi {
    /// 职业成长域。
    pub fn profession(&self) -> ProfessionApi {
        ProfessionApi(self.clone())
    }
}

impl ProfessionApi {
    /// 读取当前登录用户的主职业、累计职业、可选职业和隐私设置。
    pub async fn me(&self) -> Result<ProfessionSettings, Error> {
        let wire: WireProfessionSettings = self
            .0
            .get_data(&self.0.with_key("api/profession/me"))
            .await?;
        Ok(wire.into())
    }

    /// 读取指定职业的当前等级、等级路线、稀有度和贡献统计。
    pub async fn detail(
        &self,
        profession_id: impl AsRef<str>,
        dark: bool,
    ) -> Result<ProfessionDetail, Error> {
        let path = build_http_path(
            &format!("api/profession/me/{}/detail", profession_id.as_ref()),
            &[
                ("dark", dark.to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let wire: WireProfessionDetail = self.0.get_data(&path).await?;
        Ok(wire.into())
    }

    /// 读取指定职业最近 90 天的经验变化记录。
    pub async fn records(
        &self,
        profession_id: impl AsRef<str>,
        before_occurred_at: Option<i64>,
        before_effect_id: Option<&str>,
        limit: u32,
    ) -> Result<ProfessionRecordsPage, Error> {
        let limit = limit.clamp(1, 100);
        let mut params = vec![
            ("limit", limit.to_string()),
            ("apiKey", self.0.api_key().to_string()),
        ];
        if let Some(v) = before_occurred_at {
            params.push(("beforeOccurredAt", v.to_string()));
        }
        if let Some(v) = before_effect_id.filter(|v| !v.trim().is_empty()) {
            params.push(("beforeEffectId", v.to_string()));
        }
        let path = build_http_path(
            &format!("api/profession/me/{}/records", profession_id.as_ref()),
            &params,
        );
        let wire: WireProfessionRecordsPage = self.0.get_data(&path).await?;
        Ok(wire.into())
    }

    /// 设置主职业，其他职业的经验和等级不会清除。
    pub async fn set_primary(
        &self,
        profession_id: impl AsRef<str>,
    ) -> Result<ProfessionSettings, Error> {
        let body = json!({ "professionId": profession_id.as_ref() });
        let wire: WireProfessionSettings = self
            .0
            .post_page_data("api/profession/me/primary", &body)
            .await?;
        Ok(wire.into())
    }

    /// 结束首次选择提示。
    pub async fn skip_first_prompt(&self) -> Result<(), Error> {
        let _: serde_json::Value = self
            .0
            .post_data("api/profession/me/skip", &json!({}))
            .await?;
        Ok(())
    }

    /// 更新职业隐私。
    pub async fn update_privacy(
        &self,
        preset: ProfessionPrivacyPreset,
        module_visibility: Option<ProfessionModuleVisibility>,
    ) -> Result<ProfessionSettings, Error> {
        let mut body = json!({ "preset": preset.as_str() });
        if let Some(v) = module_visibility
            && let Some(map) = body.as_object_mut()
        {
            map.insert(
                "moduleVisibility".to_string(),
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
                }),
            );
        }
        let wire: WireProfessionSettings = self
            .0
            .post_page_data("api/profession/me/privacy", &body)
            .await?;
        Ok(wire.into())
    }

    /// 按用户隐私设置读取公开职业资料。
    pub async fn public_profile(
        &self,
        user_name: impl AsRef<str>,
        position: ProfessionProfilePosition,
        dark: bool,
    ) -> Result<PublicProfessionProfile, Error> {
        let path = build_http_path(
            &format!("api/user/{}/profession", user_name.as_ref()),
            &[
                ("position", position.as_str().to_string()),
                ("dark", dark.to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let wire: WirePublicProfessionProfile = self.0.get_data(&path).await?;
        Ok(wire.into())
    }

    /// 读取指定职业的公开榜单。
    pub async fn ranking(
        &self,
        profession_id: Option<&str>,
        dark: bool,
    ) -> Result<ProfessionRanking, Error> {
        let mut params = vec![
            ("dark", dark.to_string()),
            ("apiKey", self.0.api_key().to_string()),
        ];
        if let Some(id) = profession_id.filter(|v| !v.trim().is_empty()) {
            params.push(("professionId", id.to_string()));
        }
        let path = build_http_path("api/profession/ranking", &params);
        let wire: WireProfessionRanking = self.0.get_data(&path).await?;
        Ok(wire.into())
    }
}
