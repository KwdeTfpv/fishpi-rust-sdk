pub mod error;

use crate::utils::error::Error;

use reqwest::header::{HeaderMap, HeaderName, HeaderValue};
use reqwest::{Client, Method, StatusCode, multipart};
use serde_json::Value;
use std::collections::HashMap;
use std::time::Duration;
use tokio::time::sleep;
use url::form_urlencoded::Serializer;

lazy_static::lazy_static! {
    static ref CLIENT: Client = Client::new();
}

const DOMAIN: &str = "fishpi.cn";

pub async fn get(url: &str) -> Result<Value, Error> {
    request("GET", url, None, None).await
}

pub async fn put(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("PUT", url, None, data).await
}

pub async fn get_text(url: &str) -> Result<String, Error> {
    let full_url = format!("https://{}/{}", DOMAIN, url.trim_start_matches('/'));

    let resp = CLIENT
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
    let url_with_key = build_http_path(url, &[("apiKey", api_key.to_string())]);
    request("GET", &url_with_key, None, None).await
}

pub async fn post(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("POST", url, None, data).await
}

pub async fn delete(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("DELETE", url, None, data).await
}

pub async fn upload_files(url: &str, files: Vec<String>, api_key: &str) -> Result<Value, Error> {
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

    let response = CLIENT
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
    let full_url = format!("https://{}/{}", DOMAIN, url.trim_start_matches('/'));

    let method = method
        .parse::<Method>()
        .map_err(|e| Error::Request(Box::new(e)))?;
    let extra_headers = if let Some(headers) = headers {
        Some(
            headers
                .into_iter()
                .map(|(k, v)| {
                    let name = HeaderName::from_bytes(k.as_bytes())
                        .map_err(|e| Error::Request(Box::new(e)))?;
                    let value =
                        HeaderValue::from_str(&v).map_err(|e| Error::Request(Box::new(e)))?;
                    Ok((name, value))
                })
                .collect::<Result<HeaderMap, Error>>()?,
        )
    } else {
        None
    };

    let max_retries = 2;
    let mut attempt = 0;

    loop {
        let mut req = CLIENT
            .request(method.clone(), &full_url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36",
            )
            .header("Referer", &format!("https://{}/", DOMAIN));

        if let Some(map) = extra_headers.clone() {
            req = req.headers(map);
        }

        if let Some(body) = data.clone() {
            req = req.json(&body);
        }

        let resp = match req.send().await {
            Ok(resp) => resp,
            Err(err) => {
                if attempt < max_retries {
                    let wait_ms = 300 * (attempt + 1);
                    sleep(Duration::from_millis(wait_ms as u64)).await;
                    attempt += 1;
                    continue;
                }
                return Err(Error::Request(Box::new(err)));
            }
        };

        if resp.status().is_success() {
            return resp
                .json::<Value>()
                .await
                .map_err(|e| Error::Request(Box::new(e)));
        }

        if resp.status() == StatusCode::SERVICE_UNAVAILABLE && attempt < max_retries {
            let wait_ms = 300 * (attempt + 1);
            sleep(Duration::from_millis(wait_ms as u64)).await;
            attempt += 1;
            continue;
        }

        return Err(Error::Request(
            format!("HTTP error: {}", resp.status()).into(),
        ));
    }
}

/// 构造带查询参数的相对 HTTP 路径，自动进行 query 编码
pub fn build_http_path(path: &str, params: &[(&str, String)]) -> String {
    if params.is_empty() {
        return path.to_string();
    }

    let mut serializer = Serializer::new(String::new());
    for (k, v) in params {
        serializer.append_pair(k, v);
    }
    let query = serializer.finish();

    format!("{}?{}", path, query)
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

#[cfg(test)]
mod tests {
    use super::build_http_path;

    #[test]
    fn build_http_path_encodes_query() {
        let p = build_http_path(
            "chat/get-message",
            &[
                ("apiKey", "token a+b".to_string()),
                ("toUser", "alice/bob".to_string()),
            ],
        );

        assert_eq!(p, "chat/get-message?apiKey=token+a%2Bb&toUser=alice%2Fbob");
    }
}
