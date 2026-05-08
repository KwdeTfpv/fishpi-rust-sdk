pub mod error;

use crate::utils::error::Error;

use reqwest::header::{HeaderMap, HeaderName, HeaderValue};
use reqwest::{Client, Method, Proxy, StatusCode, multipart};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::RwLock;
use std::time::Duration;
use tokio::time::sleep;
use url::form_urlencoded::Serializer;

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum HttpProxyMode {
    NoProxy,
    System,
    Custom,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct HttpProxyConfig {
    pub mode: HttpProxyMode,
    pub proxy_url: Option<String>,
}

impl HttpProxyConfig {
    pub fn no_proxy() -> Self {
        Self {
            mode: HttpProxyMode::NoProxy,
            proxy_url: None,
        }
    }
}

lazy_static::lazy_static! {
    static ref CLIENT: RwLock<Client> = RwLock::new(build_client(&HttpProxyConfig::no_proxy()).expect("default http client init failed"));
    static ref HTTP_PROXY_CONFIG: RwLock<HttpProxyConfig> = RwLock::new(HttpProxyConfig::no_proxy());
}

const DOMAIN: &str = "fishpi.cn";

fn build_client(config: &HttpProxyConfig) -> Result<Client, Error> {
    let builder = Client::builder();
    let builder = match config.mode {
        HttpProxyMode::NoProxy => builder.no_proxy(),
        HttpProxyMode::System => builder,
        HttpProxyMode::Custom => {
            let proxy_url = config
                .proxy_url
                .as_deref()
                .map(str::trim)
                .filter(|v| !v.is_empty())
                .ok_or_else(|| Error::Api("proxy url is empty".to_string()))?;
            builder
                .no_proxy()
                .proxy(Proxy::all(proxy_url).map_err(|e| Error::Request(Box::new(e)))?)
        }
    };

    builder
        .connect_timeout(Duration::from_secs(8))
        .timeout(Duration::from_secs(15))
        .build()
        .map_err(|e| Error::Request(Box::new(e)))
}

fn http_client() -> Client {
    CLIENT
        .read()
        .map(|client| client.clone())
        .unwrap_or_else(|_| Client::new())
}

pub fn configure_http_proxy(config: HttpProxyConfig) -> Result<(), Error> {
    let client = build_client(&config)?;
    if let Ok(mut guard) = HTTP_PROXY_CONFIG.write() {
        *guard = config;
    }
    if let Ok(mut guard) = CLIENT.write() {
        *guard = client;
    }
    Ok(())
}

pub fn current_http_proxy_config() -> HttpProxyConfig {
    HTTP_PROXY_CONFIG
        .read()
        .map(|guard| guard.clone())
        .unwrap_or_else(|_| HttpProxyConfig::no_proxy())
}

pub async fn get(url: &str) -> Result<Value, Error> {
    request("GET", url, None, None).await
}

pub async fn get_with_body(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("GET", url, None, data).await
}

pub async fn put(url: &str, data: Option<Value>) -> Result<Value, Error> {
    request("PUT", url, None, data).await
}

pub async fn get_text(url: &str) -> Result<String, Error> {
    let full_url = format!("https://{}/{}", DOMAIN, url.trim_start_matches('/'));

    let resp = http_client()
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
    let full_url = format!("https://{}/{}", DOMAIN, url.trim_start_matches('/'));
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

    let response = http_client()
        .post(&full_url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36",
        )
        .header("Referer", &format!("https://{}/", DOMAIN))
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
        let mut req = http_client()
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
