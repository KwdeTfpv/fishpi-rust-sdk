//! 清风明月方法。

use crate::client::FishPi;
use crate::domain::breezemoon::Breezemoon;
use crate::ids::UserName;
use crate::utils::build_http_path;
use crate::utils::error::Error;
use crate::wire::breezemoon::WireBreezemoonList;

/// 清风明月域访问器。
pub struct BreezemoonApi(pub(crate) FishPi);

impl FishPi {
    /// 清风明月域。
    pub fn breezemoon(&self) -> BreezemoonApi {
        BreezemoonApi(self.clone())
    }
}

impl BreezemoonApi {
    /// 获取清风明月列表。
    ///
    /// - `page` 页码
    /// - `size` 每页个数
    /// - `user` 可选，指定用户名时查询该用户的清风明月
    pub async fn list(
        &self,
        page: u32,
        size: u32,
        user: Option<impl Into<UserName>>,
    ) -> Result<Vec<Breezemoon>, Error> {
        let base = if let Some(u) = user {
            format!("user/{}/", u.into())
        } else {
            String::new()
        };
        let path = build_http_path(
            &format!("api/{}breezemoons", base),
            &[
                ("p", page.to_string()),
                ("size", size.to_string()),
                ("apiKey", self.0.api_key().to_string()),
            ],
        );
        let wire: WireBreezemoonList = self.0.get_data(&path).await?;
        Ok(wire.breezemoons.into_iter().map(Breezemoon::from).collect())
    }

    /// 发送清风明月。
    pub async fn send(&self, content: impl Into<String>) -> Result<(), Error> {
        let body = serde_json::json!({
            "apiKey": self.0.api_key().as_str(),
            "breezemoonContent": content.into(),
        });
        self.0.post_raw("breezemoon", &body).await?;
        Ok(())
    }
}
