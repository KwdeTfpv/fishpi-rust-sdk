use serde_json::{Value, json};

use crate::client::core::FishPi;
use crate::model::misc::{Log, PreRegisterInfo, RegisterInfo, Report, UploadResult};
use crate::utils::error::Error;
use crate::utils::{build_http_path, request_upload_ticket, upload_files};

impl FishPi {
    /// 预注册（发送手机验证码等）。
    pub async fn pre_register(&self, data: &PreRegisterInfo) -> Result<(), Error> {
        self.post_raw("register", data).await?;
        Ok(())
    }

    /// 验证手机验证码，返回用户 Id。
    pub async fn verify(&self, code: &str) -> Result<String, Error> {
        let url = build_http_path("verify", &[("code", code.to_string())]);
        let rsp = self.get_raw(&url).await?;
        Ok(rsp["userId"].as_str().unwrap_or_default().to_string())
    }

    /// 注册。
    pub async fn register(&self, data: &RegisterInfo) -> Result<(), Error> {
        let path = match &data.r {
            Some(r) => build_http_path("register2", &[("r", r.to_string())]),
            None => "register2".to_string(),
        };
        self.post_raw(&path, data).await?;
        Ok(())
    }

    /// 获取操作日志。
    pub async fn log(&self, page: u32, page_size: u32) -> Result<Vec<Log>, Error> {
        let url = build_http_path(
            "logs/more",
            &[
                ("page", page.to_string()),
                ("pageSize", page_size.to_string()),
            ],
        );
        let rsp = self.get_raw(&url).await?;
        Ok(rsp["data"]
            .as_array()
            .map(|arr| arr.iter().filter_map(|v| Log::from_value(v).ok()).collect())
            .unwrap_or_default())
    }

    /// 举报。
    pub async fn report(&self, data: &Report) -> Result<(), Error> {
        let mut body = serde_json::to_value(data).map_err(|e| Error::decode(e.to_string()))?;
        if let Value::Object(map) = &mut body {
            map.insert("apiKey".to_string(), json!(self.api_key().as_str()));
        }
        self.post_raw("report", &body).await?;
        Ok(())
    }

    /// 上传文件。
    pub async fn upload<I, P>(&self, files: I) -> Result<UploadResult, Error>
    where
        I: IntoIterator<Item = P>,
        P: Into<String>,
    {
        let files: Vec<String> = files.into_iter().map(Into::into).collect();
        for file in &files {
            if !std::path::Path::new(file).exists() {
                return Err(Error::decode(format!("File not exist: {}", file)));
            }
        }

        let api_key = self.api_key().as_str().to_string();
        let mut last_err = None;
        for _ in 0..2 {
            let ticket = request_upload_ticket(&api_key).await?;
            let rsp = upload_files(&ticket.upload_url, files.clone(), &ticket.ticket).await?;
            if rsp.get("code").and_then(Value::as_i64).unwrap_or(-1) != 0 {
                let msg = rsp["msg"].as_str().unwrap_or("API error").to_string();
                if msg.contains("票据已过期") || msg.contains("票据已被使用") {
                    last_err = Some(Error::decode(msg));
                    continue;
                }
                return Err(Error::decode(msg));
            }
            return UploadResult::from_value(&rsp["data"]);
        }
        Err(last_err.unwrap_or_else(|| Error::decode("上传失败".to_string())))
    }
}
