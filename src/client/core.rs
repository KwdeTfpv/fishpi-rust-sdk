use std::sync::Arc;
use std::time::Duration;

use reqwest::{Client, Method, Proxy, Response, StatusCode, redirect::Policy};
use serde::Serialize;
use serde::de::DeserializeOwned;
use serde_json::Value;
use tokio::time::sleep;

use super::{Config, USER_AGENT};
use crate::ids::ApiKey;
use crate::model::misc::LoginData;
use crate::utils::error::Error;
use crate::utils::{HttpProxyConfig, build_http_path};

const VISITOR_VERIFY_MESSAGE: &str = "当前网络触发摸鱼派访客验证，请完成验证后再重试";

struct Inner {
    http: Client,
    config: Config,
    api_key: ApiKey,
    page_auth: tokio::sync::OnceCell<PageAuth>,
}

/// 摸鱼派客户端，可 `Clone`。
#[derive(Clone)]
pub struct FishPi {
    inner: Arc<Inner>,
}

impl FishPi {
    /// 开始构造客户端。
    pub fn builder() -> FishPiBuilder {
        FishPiBuilder::default()
    }

    /// 当前 api_key。
    pub fn api_key(&self) -> &ApiKey {
        &self.inner.api_key
    }

    /// 当前服务域名。
    pub(crate) fn domain(&self) -> &str {
        &self.inner.config.domain
    }

    /// 聊天室消息来源标识（如 `Rust/0.2.0`）。
    pub(crate) fn client_tag(&self) -> &str {
        &self.inner.config.client_tag
    }

    /// 是否已持有 api_key。
    pub fn is_authenticated(&self) -> bool {
        !self.inner.api_key.is_empty()
    }

    /// 换一个 api_key，返回新客户端。
    pub fn with_api_key(&self, api_key: impl Into<ApiKey>) -> Self {
        Self {
            inner: Arc::new(Inner {
                http: self.inner.http.clone(),
                config: self.inner.config.clone(),
                api_key: api_key.into(),
                page_auth: tokio::sync::OnceCell::new(),
            }),
        }
    }

    /// 换一个聊天室消息来源标识，返回新客户端（复用连接池）。
    pub fn with_client_tag(&self, client_tag: impl Into<String>) -> Self {
        let mut config = self.inner.config.clone();
        config.client_tag = client_tag.into();
        Self {
            inner: Arc::new(Inner {
                http: self.inner.http.clone(),
                config,
                api_key: self.inner.api_key.clone(),
                page_auth: tokio::sync::OnceCell::new(),
            }),
        }
    }
}

/// [`FishPi`] 构造器。
#[derive(Default)]
pub struct FishPiBuilder {
    api_key: Option<ApiKey>,
    domain: Option<String>,
    connect_timeout: Option<Duration>,
    timeout: Option<Duration>,
    proxy: Option<HttpProxyConfig>,
    max_retries: Option<u32>,
    client_tag: Option<String>,
}

impl FishPiBuilder {
    /// 设置 api_key。
    pub fn api_key(mut self, api_key: impl Into<ApiKey>) -> Self {
        self.api_key = Some(api_key.into());
        self
    }

    /// 设置服务域名（默认 `fishpi.cn`）。
    pub fn domain(mut self, domain: impl Into<String>) -> Self {
        self.domain = Some(domain.into());
        self
    }

    /// 连接超时。
    pub fn connect_timeout(mut self, d: Duration) -> Self {
        self.connect_timeout = Some(d);
        self
    }

    /// 整体请求超时。
    pub fn timeout(mut self, d: Duration) -> Self {
        self.timeout = Some(d);
        self
    }

    /// 代理配置。
    pub fn proxy(mut self, proxy: HttpProxyConfig) -> Self {
        self.proxy = Some(proxy);
        self
    }

    /// 传输层失败最大重试次数（默认 2）。
    pub fn max_retries(mut self, n: u32) -> Self {
        self.max_retries = Some(n);
        self
    }

    /// 聊天室消息来源标识，默认 `Rust/<版本>`。
    pub fn client_tag(mut self, tag: impl Into<String>) -> Self {
        self.client_tag = Some(tag.into());
        self
    }

    /// 构造客户端。
    pub fn build(self) -> Result<FishPi, Error> {
        let mut config = Config::default();
        if let Some(d) = self.domain {
            config.domain = d;
        }
        if let Some(t) = self.client_tag {
            config.client_tag = t;
        }
        if let Some(t) = self.connect_timeout {
            config.connect_timeout = t;
        }
        if let Some(t) = self.timeout {
            config.timeout = t;
        }
        if let Some(p) = self.proxy {
            config.proxy = p;
        }
        if let Some(n) = self.max_retries {
            config.max_retries = n;
        }

        let http = build_http_client(&config)?;
        Ok(FishPi {
            inner: Arc::new(Inner {
                http,
                config,
                api_key: self.api_key.unwrap_or_else(|| ApiKey::new("")),
                page_auth: tokio::sync::OnceCell::new(),
            }),
        })
    }

    /// 构造并登录，返回带鉴权的客户端。
    pub async fn login(self, data: &LoginData) -> Result<FishPi, Error> {
        self.build()?.login(data).await
    }
}

fn build_http_client(config: &Config) -> Result<Client, Error> {
    let builder = Client::builder();
    build_http_client_from_builder(config, builder)
}

fn build_http_client_without_redirects(config: &Config) -> Result<Client, Error> {
    let builder = Client::builder().redirect(Policy::none());
    build_http_client_from_builder(config, builder)
}

fn build_http_client_from_builder(
    config: &Config,
    builder: reqwest::ClientBuilder,
) -> Result<Client, Error> {
    let builder = match config.proxy.mode {
        crate::utils::HttpProxyMode::NoProxy => builder.no_proxy(),
        crate::utils::HttpProxyMode::System => builder,
        crate::utils::HttpProxyMode::Custom => {
            let proxy_url = config
                .proxy
                .proxy_url
                .as_deref()
                .map(str::trim)
                .filter(|v| !v.is_empty())
                .ok_or_else(|| Error::decode("proxy url is empty"))?;
            builder.no_proxy().proxy(Proxy::all(proxy_url)?)
        }
    };

    builder
        .connect_timeout(config.connect_timeout)
        .timeout(config.timeout)
        .build()
        .map_err(Error::from)
}

impl FishPi {
    fn full_url(&self, path: &str) -> String {
        format!(
            "https://{}/{}",
            self.inner.config.domain,
            path.trim_start_matches('/')
        )
    }

    /// 在路径上附加 `apiKey` 查询参数。
    pub(crate) fn with_key(&self, path: &str) -> String {
        build_http_path(path, &[("apiKey", self.inner.api_key.to_string())])
    }

    /// 发起请求，返回原始 JSON `Value`。
    pub(crate) async fn send_raw(
        &self,
        method: Method,
        path: &str,
        body: Option<&Value>,
    ) -> Result<Value, Error> {
        let url = self.full_url(path);
        let max_retries = self.inner.config.max_retries;
        let mut attempt = 0;

        loop {
            let mut req = self
                .inner
                .http
                .request(method.clone(), &url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", format!("https://{}/", self.inner.config.domain));
            if let Some(b) = body {
                req = req.json(b);
            }

            let resp = match req.send().await {
                Ok(resp) => resp,
                Err(err) => {
                    if attempt < max_retries {
                        sleep(backoff(attempt)).await;
                        attempt += 1;
                        continue;
                    }
                    return Err(Error::from(err));
                }
            };

            let status = resp.status();
            if status.is_success() {
                return parse_json_response(resp).await;
            }

            match status {
                StatusCode::UNAUTHORIZED => return Err(Error::Unauthorized),
                StatusCode::NOT_FOUND => {
                    return Err(Error::NotFound {
                        resource: path.to_string(),
                    });
                }
                StatusCode::TOO_MANY_REQUESTS => {
                    return Err(Error::RateLimited {
                        retry_after: retry_after(&resp),
                    });
                }
                StatusCode::SERVICE_UNAVAILABLE if attempt < max_retries => {
                    sleep(backoff(attempt)).await;
                    attempt += 1;
                    continue;
                }
                _ => {
                    return Err(Error::decode(format!("HTTP error: {}", status)));
                }
            }
        }
    }
}

fn backoff(attempt: u32) -> Duration {
    Duration::from_millis(300 * (attempt as u64 + 1))
}

fn retry_after(resp: &Response) -> Option<Duration> {
    resp.headers()
        .get(reqwest::header::RETRY_AFTER)
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.trim().parse::<u64>().ok())
        .map(Duration::from_secs)
}

/// 校验 `{code,msg}` 信封，返回原始 `Value`。
fn check_envelope(value: Value) -> Result<Value, Error> {
    let code = value.get("code").and_then(Value::as_i64);
    match code {
        None | Some(0) => Ok(value),
        Some(c) => {
            let msg = value
                .get("msg")
                .and_then(Value::as_str)
                .unwrap_or("API error")
                .to_string();
            Err(map_business(c, msg))
        }
    }
}

fn map_business(code: i64, msg: String) -> Error {
    match code {
        -1 if msg.contains("Auth") || msg.contains("登录") || msg.contains("认证") => {
            Error::Unauthorized
        }
        _ => Error::business(code, msg),
    }
}

impl FishPi {
    /// GET 请求，反序列化 `data`。
    pub(crate) async fn get_data<T: DeserializeOwned>(&self, path: &str) -> Result<T, Error> {
        let value = self.send_raw(Method::GET, path, None).await?;
        let value = check_envelope(value)?;
        take_data(value)
    }

    /// 带 body 的 GET 请求，body 注入 apiKey，反序列化 `data`。
    pub(crate) async fn get_data_with_key<T: DeserializeOwned>(
        &self,
        path: &str,
    ) -> Result<T, Error> {
        let mut body = Value::Object(Default::default());
        self.inject_api_key(&mut body);
        let value = self.send_raw(Method::GET, path, Some(&body)).await?;
        let value = check_envelope(value)?;
        take_data(value)
    }

    /// POST 请求，反序列化 `data`。
    pub(crate) async fn post_data<T: DeserializeOwned, B: Serialize>(
        &self,
        path: &str,
        body: &B,
    ) -> Result<T, Error> {
        let mut body = serde_json::to_value(body).map_err(|e| Error::decode(e.to_string()))?;
        self.inject_api_key(&mut body);
        let value = self.send_raw(Method::POST, path, Some(&body)).await?;
        let value = check_envelope(value)?;
        take_data(value)
    }
    /// POST 页面态请求，保持调用方提供的 JSON body，并使用网页登录态的 sym-ce 与 csrfToken。
    pub(crate) async fn post_page_data<T: DeserializeOwned, B: Serialize>(
        &self,
        path: &str,
        body: &B,
    ) -> Result<T, Error> {
        let body = serde_json::to_value(body).map_err(|e| Error::decode(e.to_string()))?;
        let url = self.full_url(path);
        let page_auth = self.page_auth().await?;
        let resp = self
            .inner
            .http
            .post(&url)
            .header("User-Agent", USER_AGENT)
            .header(
                "Referer",
                format!("https://{}/settings/profession", self.inner.config.domain),
            )
            .header("Cookie", page_auth.cookie_header())
            .header("csrfToken", page_auth.csrf_token)
            .json(&body)
            .send()
            .await?;
        let value = parse_json_response(resp).await?;
        let value = check_envelope(value)?;
        take_data(value)
    }
    /// GET 请求，校验信封后返回整个响应 `Value`。
    pub(crate) async fn get_raw(&self, path: &str) -> Result<Value, Error> {
        let value = self.send_raw(Method::GET, path, None).await?;
        check_envelope(value)
    }

    /// POST 请求，校验信封后返回整个响应 `Value`。
    pub(crate) async fn post_raw<B: Serialize>(
        &self,
        path: &str,
        body: &B,
    ) -> Result<Value, Error> {
        let mut body = serde_json::to_value(body).map_err(|e| Error::decode(e.to_string()))?;
        self.inject_api_key(&mut body);
        let value = self.send_raw(Method::POST, path, Some(&body)).await?;
        check_envelope(value)
    }

    /// PUT 请求，校验信封后返回整个响应 `Value`。
    pub(crate) async fn put_raw<B: Serialize>(&self, path: &str, body: &B) -> Result<Value, Error> {
        let mut body = serde_json::to_value(body).map_err(|e| Error::decode(e.to_string()))?;
        self.inject_api_key(&mut body);
        let value = self.send_raw(Method::PUT, path, Some(&body)).await?;
        check_envelope(value)
    }

    /// DELETE 请求，校验信封后返回整个响应 `Value`。
    pub(crate) async fn delete_raw<B: Serialize>(
        &self,
        path: &str,
        body: &B,
    ) -> Result<Value, Error> {
        let mut body = serde_json::to_value(body).map_err(|e| Error::decode(e.to_string()))?;
        self.inject_api_key(&mut body);
        let value = self.send_raw(Method::DELETE, path, Some(&body)).await?;
        check_envelope(value)
    }

    /// GET 请求，返回响应正文纯文本（HTML 等非 JSON 内容）。
    pub(crate) async fn get_text(&self, path: &str) -> Result<String, Error> {
        self.get_text_with_cookie(path, None).await
    }

    async fn get_text_with_cookie(
        &self,
        path: &str,
        cookie: Option<String>,
    ) -> Result<String, Error> {
        let url = self.full_url(path);
        let mut req = self
            .inner
            .http
            .get(&url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", format!("https://{}/", self.inner.config.domain));
        if let Some(cookie) = cookie {
            req = req.header("Cookie", cookie);
        }
        let resp = req.send().await?;
        let status = resp.status();
        if !status.is_success() {
            return Err(Error::decode(format!("HTTP error: {}", status)));
        }
        resp.text().await.map_err(Error::from)
    }

    /// 在 JSON 对象 body 中注入 `apiKey`。
    fn inject_api_key(&self, body: &mut Value) {
        if self.inner.api_key.is_empty() {
            return;
        }
        if let Value::Object(map) = body
            && !map.contains_key("apiKey")
        {
            map.insert(
                "apiKey".to_string(),
                Value::String(self.inner.api_key.to_string()),
            );
        }
    }

    async fn page_auth(&self) -> Result<PageAuth, Error> {
        let page_auth = self
            .inner
            .page_auth
            .get_or_try_init(|| async {
                let sym_ce = self.exchange_page_token().await?;
                if sym_ce.is_empty() {
                    return Err(Error::Unauthorized);
                }
                let html = self
                    .get_text_with_cookie("settings/profession", Some(format!("sym-ce={sym_ce}")))
                    .await?;
                let csrf_token = extract_csrf_token(&html)
                    .filter(|token| !token.trim().is_empty())
                    .ok_or_else(|| Error::decode("cannot parse csrfToken from settings page"))?;
                Ok(PageAuth { sym_ce, csrf_token })
            })
            .await?;
        Ok(page_auth.clone())
    }

    async fn exchange_page_token(&self) -> Result<String, Error> {
        if self.inner.api_key.is_empty() {
            return Err(Error::Unauthorized);
        }
        let path = build_http_path(
            "loginWebInApiKey",
            &[("apiKey", self.inner.api_key.to_string())],
        );
        let url = self.full_url(&path);
        let http = build_http_client_without_redirects(&self.inner.config)?;
        let resp = http
            .get(&url)
            .header("User-Agent", USER_AGENT)
            .header(
                "Referer",
                format!("https://{}/login", self.inner.config.domain),
            )
            .send()
            .await?;
        response_cookie_value(&resp, "sym-ce")
            .ok_or_else(|| Error::decode("cannot exchange apiKey for page sym-ce cookie"))
    }
}

/// 取 `data` 字段并反序列化。
fn take_data<T: DeserializeOwned>(mut value: Value) -> Result<T, Error> {
    let data = match value.get_mut("data") {
        Some(d) => d.take(),
        None => value,
    };
    let data = if let Value::String(s) = &data {
        serde_json::from_str(s).map_err(|e| Error::decode(e.to_string()))?
    } else {
        data
    };
    serde_json::from_value(data).map_err(|e| Error::decode(e.to_string()))
}

async fn parse_json_response(resp: Response) -> Result<Value, Error> {
    let final_path = resp.url().path().to_string();
    let content_type = resp
        .headers()
        .get("content-type")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("")
        .to_ascii_lowercase();
    let text = resp.text().await?;

    if is_visitor_verify_response(&final_path, &content_type, &text) {
        return Err(Error::decode(VISITOR_VERIFY_MESSAGE));
    }

    serde_json::from_str::<Value>(&text).map_err(|err| {
        if is_html_response(&content_type, &text) {
            Error::decode("服务返回了网页内容，无法作为 API 响应解析")
        } else {
            Error::decode(format!("Failed to parse response body: {err}"))
        }
    })
}

fn is_visitor_verify_response(path: &str, content_type: &str, body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    path == "/test"
        || (is_html_response(content_type, body)
            && (body.contains("访客验证")
                || body.contains("在继续浏览摸鱼派社区前请验证")
                || body.contains("/validateCaptcha")
                || lower.contains("initgeetest4")))
}

fn is_html_response(content_type: &str, body: &str) -> bool {
    content_type.contains("text/html")
        || body
            .trim_start()
            .to_ascii_lowercase()
            .starts_with("<!doctype html")
}

#[derive(Clone)]
struct PageAuth {
    sym_ce: String,
    csrf_token: String,
}

impl PageAuth {
    fn cookie_header(&self) -> String {
        format!("sym-ce={}", self.sym_ce)
    }
}

fn extract_csrf_token(html: &str) -> Option<String> {
    let key = html.find("csrfToken")?;
    let rest = &html[key..];
    let colon = rest.find(':')?;
    let mut value = rest[colon + 1..].trim_start();
    let first = value.chars().next()?;
    if first == '\'' || first == '"' {
        value = &value[first.len_utf8()..];
        let end = value.find(first)?;
        Some(value[..end].to_string())
    } else {
        let end = value
            .find(|c: char| c.is_whitespace() || c == ',' || c == ';' || c == '}')
            .unwrap_or(value.len());
        Some(
            value[..end]
                .trim_matches('"')
                .trim_matches('\'')
                .to_string(),
        )
    }
}

fn response_cookie_value(resp: &Response, name: &str) -> Option<String> {
    let prefix = format!("{name}=");
    resp.headers()
        .get_all(reqwest::header::SET_COOKIE)
        .iter()
        .filter_map(|value| value.to_str().ok())
        .find_map(|cookie| {
            cookie
                .split(';')
                .map(str::trim)
                .find_map(|part| part.strip_prefix(&prefix).map(str::to_string))
        })
}

impl FishPi {
    /// 登录，返回持有 api_key 的新客户端。
    pub async fn login(&self, data: &LoginData) -> Result<FishPi, Error> {
        let body = data.to_value()?;
        let rsp = self
            .send_raw(Method::POST, "api/getKey", Some(&body))
            .await?;
        let rsp = check_envelope(rsp)?;
        let api_key = rsp
            .get("Key")
            .and_then(Value::as_str)
            .unwrap_or("")
            .trim()
            .to_string();
        Ok(self.with_api_key(api_key))
    }
}
