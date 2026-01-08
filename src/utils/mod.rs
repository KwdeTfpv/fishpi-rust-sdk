pub mod error;

use crate::utils::error::Error;

use reqwest::header::{HeaderMap, HeaderName, HeaderValue};
use reqwest::{Client, Method, multipart};
use serde_json::Value;
use std::collections::HashMap;

const DOMAIN: &str = "fishpi.cn";

pub async fn get(url: &str) -> Result<Value, Error> {
    request("GET", url, None, None).await
}

pub async fn put(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("PUT", url, None, data).await
}

pub async fn get_text(url: &str) -> Result<String, Error> {
    let client = Client::new();
    let full_url = format!("https://{}/{}", DOMAIN, url.trim_start_matches('/'));

    let resp = client
        .get(&full_url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36",
        )
        .header("Referer", &format!("https://{}/", DOMAIN))
        .send()
        .await
        .map_err(|e| Error::Request(Box::new(e)))?;

    if !resp.status().is_success() {
        return Err(Error::Request(
            format!("HTTP error: {}", resp.status()).into(),
        ));
    }

    resp.text().await.map_err(|e| Error::Request(Box::new(e)))
}

pub async fn get_with_key(url: &str, api_key: &str) -> Result<Value, Error> {
    let url_with_key = format!("{}?apiKey={}", url, api_key);
    request("GET", &url_with_key, None, None).await
}

pub async fn post(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("POST", url, None, data).await
}

pub async fn delete(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("DELETE", url, None, data).await
}

pub async fn upload_files(url: &str, files: Vec<String>, api_key: &str) -> Result<Value, Error> {
    let client = reqwest::Client::new();
    let mut form = multipart::Form::new();

    for file_path in files {
        if !std::path::Path::new(&file_path).exists() {
            return Err(Error::Api(format!("File not exist: {}", file_path)));
        }
        let file_content = tokio::fs::read(&file_path)
            .await
            .map_err(|e| Error::Api(format!("Failed to read file {}: {}", file_path, e)))?;
        let file_name = std::path::Path::new(&file_path)
            .file_name()
            .and_then(|n| n.to_str())
            .unwrap_or("file")
            .to_string();
        form = form.part(
            "file[]",
            multipart::Part::stream(file_content).file_name(file_name),
        );
    }

    form = form.text("apiKey", api_key.to_string());

    let response = client
        .post(url)
        .multipart(form)
        .send()
        .await
        .map_err(|e| Error::Api(format!("Request failed: {}", e)))?;

    let rsp: Value = response
        .json()
        .await
        .map_err(|e| Error::Api(format!("Failed to parse response: {}", e)))?;

    Ok(rsp)
}

async fn request(
    method: &str,
    url: &str,
    headers: Option<HashMap<String, String>>,
    data: Option<Value>,
) -> Result<Value, Error> {
    let client = Client::new();
    let full_url = format!("https://{}/{}", DOMAIN, url.trim_start_matches('/'));

    let method = method
        .parse::<Method>()
        .map_err(|e| Error::Request(Box::new(e)))?;

    let mut req = client
        .request(method, &full_url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36",
        )
        .header("Referer", &format!("https://{}/", DOMAIN));

    if let Some(headers) = headers {
        let header_map: HeaderMap = headers
            .into_iter()
            .map(|(k, v)| {
                let name = HeaderName::from_bytes(k.as_bytes())
                    .map_err(|e| Error::Request(Box::new(e)))?;
                let value = HeaderValue::from_str(&v).map_err(|e| Error::Request(Box::new(e)))?;
                Ok((name, value))
            })
            .collect::<Result<HeaderMap, Error>>()?;

        req = req.headers(header_map);
    }

    if let Some(data) = data {
        req = req.json(&data);
    }

    let resp = req.send().await.map_err(|e| Error::Request(Box::new(e)))?;
    if !resp.status().is_success() {
        return Err(Error::Request(
            format!("HTTP error: {}", resp.status()).into(),
        ));
    }

    resp.json::<Value>()
        .await
        .map_err(|e| Error::Request(Box::new(e)))
}

#[derive(Clone, Debug)]
#[allow(non_snake_case)]
pub struct ResponseResult {
    /// 是否成功
    pub success: bool,
    /// 执行结果或错误信息
    pub msg: String,
}

impl ResponseResult {
    pub fn from_value(data: &Value) -> Result<Self, Error> {
        Ok(ResponseResult {
            success: data.get("code").and_then(|c| c.as_i64()).unwrap_or(0) == 0,
            msg: data["msg"].as_str().unwrap_or("").to_string(),
        })
    }
}
