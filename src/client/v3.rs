use std::fmt;
use std::time::Duration;

use reqwest::header::{
    AUTHORIZATION, COOKIE, HeaderMap, HeaderName, HeaderValue, RETRY_AFTER, SET_COOKIE,
};
use reqwest::{Client, Method, Proxy, RequestBuilder, StatusCode, redirect::Policy};
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};

use super::Config;

pub(crate) const LOGIN_PATH: &str = "api/v3/auth/login";
pub(crate) const REFRESH_PATH: &str = "api/v3/auth/refresh";
pub(crate) const LOGOUT_PATH: &str = "api/v3/auth/logout";
pub(crate) const ME_PATH: &str = "api/v3/me";
pub(crate) const REALTIME_TICKET_PATH: &str = "api/v3/chat/realtime-ticket";
pub(crate) const CAPTCHA_OPTIONS_PATH: &str = "api/v3/auth/captcha-options";
const VALIDATE_CAPTCHA_PATH: &str = "validateCaptcha";
const CAPTCHA_JSON_MAX_BYTES: usize = 4096;

/// 服务端对无浏览器特征请求返回 418/403，V3 请求统一携带浏览器式请求头。
const BROWSER_USER_AGENT: &str = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

/// V3 错误类别，供调用方决定重试、重新授权或直接展示。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum V3ErrorKind {
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
    Invalid,
    RateLimited,
    Captcha,
    MfaRequired,
    Server,
    Transport,
    Protocol,
}

impl V3ErrorKind {
    /// 稳定的小写类别名，供跨语言边界传递。
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Unauthorized => "unauthorized",
            Self::Forbidden => "forbidden",
            Self::NotFound => "notFound",
            Self::Conflict => "conflict",
            Self::Invalid => "invalid",
            Self::RateLimited => "rateLimited",
            Self::Captcha => "captcha",
            Self::MfaRequired => "mfaRequired",
            Self::Server => "server",
            Self::Transport => "transport",
            Self::Protocol => "protocol",
        }
    }
}

/// V3 接口错误，保留 HTTP 状态、业务码与 Retry-After。
#[derive(Clone, Debug)]
pub struct V3Error {
    pub kind: V3ErrorKind,
    pub status: Option<u16>,
    pub code: Option<String>,
    pub message: String,
    pub retry_after: Option<Duration>,
}

impl V3Error {
    pub(crate) fn protocol(message: impl Into<String>) -> Self {
        Self {
            kind: V3ErrorKind::Protocol,
            status: None,
            code: None,
            message: message.into(),
            retry_after: None,
        }
    }

    pub(crate) fn transport(message: impl Into<String>) -> Self {
        Self {
            kind: V3ErrorKind::Transport,
            status: None,
            code: None,
            message: message.into(),
            retry_after: None,
        }
    }

    pub(crate) fn unauthorized(message: impl Into<String>) -> Self {
        Self {
            kind: V3ErrorKind::Unauthorized,
            status: None,
            code: None,
            message: message.into(),
            retry_after: None,
        }
    }

    /// 该错误是否说明访问令牌不可再用。
    pub fn requires_reauth(&self) -> bool {
        self.kind == V3ErrorKind::Unauthorized
    }

    /// 该错误是否为服务端明确拒绝（重试同一请求不会改变结果）。
    pub fn is_rejection(&self) -> bool {
        matches!(
            self.kind,
            V3ErrorKind::Forbidden | V3ErrorKind::Invalid | V3ErrorKind::NotFound
        )
    }
}

impl fmt::Display for V3Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match (&self.code, self.status) {
            (Some(code), Some(status)) => write!(f, "V3 接口错误[{code}/{status}]: {}", self.message),
            (Some(code), None) => write!(f, "V3 接口错误[{code}]: {}", self.message),
            (None, Some(status)) => write!(f, "V3 接口错误[{status}]: {}", self.message),
            (None, None) => f.write_str(&self.message),
        }
    }
}

impl std::error::Error for V3Error {}

/// 登录态刷新凭据（服务端下发的 Cookie 键值对）。
#[derive(Clone, Debug, Default, PartialEq, Eq, Deserialize, Serialize)]
pub struct V3Cookie {
    pub name: String,
    pub value: String,
}

/// 一次 V3 登录或刷新得到的会话材料。
#[derive(Clone, Debug, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct V3Session {
    pub access_token: String,
    #[serde(default)]
    pub expires_in: u64,
    #[serde(default)]
    pub refresh_expires_in: u64,
    #[serde(default)]
    pub scope: String,
    #[serde(default)]
    pub refresh_cookies: Vec<V3Cookie>,
}

/// `GET /api/v3/me` 返回的账号身份。
#[derive(Clone, Debug, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct V3Me {
    pub id: String,
    #[serde(default)]
    pub user_name: String,
    #[serde(default)]
    pub user_nickname: String,
}

/// 聊天室实时订阅的一次性票据，有效期不超过 60 秒。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct V3RealtimeTicket {
    pub ticket: String,
    #[serde(default)]
    pub expires_in: u64,
    #[serde(default)]
    pub path: String,
    #[serde(default)]
    pub channel: String,
}

/// 账号流程使用的极验4组件公开标识。
#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct V3CaptchaOptions {
    #[serde(default)]
    pub provider: String,
    #[serde(default)]
    pub captcha_id: String,
}

/// V3 传输层：独立连接池、显式 Bearer、单次请求、严格信封。
#[derive(Clone)]
pub struct V3Client {
    http: Client,
    base: String,
}

impl V3Client {
    pub(crate) fn new(config: &Config) -> Result<Self, V3Error> {
        let builder = Client::builder()
            .redirect(Policy::none())
            .connect_timeout(config.connect_timeout)
            .timeout(config.timeout);
        let builder = match config.proxy.mode {
            crate::utils::HttpProxyMode::NoProxy => builder.no_proxy(),
            crate::utils::HttpProxyMode::System => builder,
            crate::utils::HttpProxyMode::Custom => {
                let proxy_url = config
                    .proxy
                    .proxy_url
                    .as_deref()
                    .map(str::trim)
                    .filter(|value| !value.is_empty())
                    .ok_or_else(|| V3Error::transport("V3 代理地址为空"))?;
                builder
                    .no_proxy()
                    .proxy(Proxy::all(proxy_url).map_err(|err| V3Error::transport(err.to_string()))?)
            }
        };
        let http = builder
            .build()
            .map_err(|err| V3Error::transport(err.to_string()))?;
        Ok(Self {
            http,
            base: format!("https://{}", config.domain),
        })
    }

    /// 第一方账号登录，返回访问令牌与刷新凭据。
    pub async fn login(
        &self,
        name_or_email: &str,
        password_md5: &str,
        mfa_code: Option<&str>,
    ) -> Result<V3Session, V3Error> {
        #[derive(Deserialize)]
        #[serde(rename_all = "camelCase")]
        struct LoginPayload {
            access_token: String,
            #[serde(default)]
            expires_in: u64,
            #[serde(default)]
            refresh_expires_in: u64,
            #[serde(default)]
            scope: String,
        }

        let mut body = serde_json::Map::new();
        body.insert("nameOrEmail".into(), Value::String(name_or_email.to_string()));
        body.insert("userPassword".into(), Value::String(password_md5.to_string()));
        if let Some(code) = mfa_code.map(str::trim).filter(|code| !code.is_empty()) {
            body.insert("mfaCode".into(), Value::String(code.to_string()));
        }

        let (status, headers, bytes) = self
            .execute(
                Method::POST,
                LOGIN_PATH,
                HeaderMap::new(),
                Some(Payload::Json(Value::Object(body))),
            )
            .await?;
        let payload: LoginPayload = decode_success(status, &headers, &bytes)?;
        let access_token = payload.access_token.trim().to_string();
        if access_token.is_empty() {
            return Err(V3Error::protocol("V3 登录响应缺少 accessToken"));
        }
        Ok(V3Session {
            access_token,
            expires_in: payload.expires_in,
            refresh_expires_in: payload.refresh_expires_in,
            scope: payload.scope,
            refresh_cookies: cookies_from_headers(&headers),
        })
    }

    /// 使用登录 Cookie 换取新的访问令牌。
    pub async fn refresh(&self, cookies: &[V3Cookie]) -> Result<V3Session, V3Error> {
        if cookies.is_empty() {
            return Err(V3Error::unauthorized("缺少 V3 刷新凭据"));
        }
        let mut headers = HeaderMap::new();
        let cookie = cookie_header(cookies)?;
        headers.insert(COOKIE, cookie);
        let (status, response_headers, bytes) =
            self.execute(Method::POST, REFRESH_PATH, headers, None).await?;
        let session: V3Session = decode_success(status, &response_headers, &bytes)?;
        if session.access_token.trim().is_empty() {
            return Err(V3Error::protocol("V3 刷新响应缺少 accessToken"));
        }
        let mut session = session;
        let rotated = cookies_from_headers(&response_headers);
        if !rotated.is_empty() {
            session.refresh_cookies = rotated;
        } else {
            session.refresh_cookies = cookies.to_vec();
        }
        Ok(session)
    }

    /// 注销当前登录 Cookie 对应的会话。
    pub async fn logout(&self, cookies: &[V3Cookie]) -> Result<(), V3Error> {
        if cookies.is_empty() {
            return Err(V3Error::unauthorized("缺少 V3 退出凭据"));
        }
        let mut headers = HeaderMap::new();
        headers.insert(COOKIE, cookie_header(cookies)?);
        let (status, response_headers, bytes) =
            self.execute(Method::POST, LOGOUT_PATH, headers, None).await?;
        let _: Value = decode_success(status, &response_headers, &bytes)?;
        Ok(())
    }

    /// 读取访问令牌对应的账号身份。
    pub async fn me(&self, access_token: &str) -> Result<V3Me, V3Error> {
        self.get_json(ME_PATH, access_token).await
    }

    /// 带 Bearer 的 V3 GET，返回 `data`。
    pub async fn get_json<T: DeserializeOwned>(
        &self,
        path: &str,
        access_token: &str,
    ) -> Result<T, V3Error> {
        let headers = auth_headers(access_token, None)?;
        let (status, response_headers, bytes) =
            self.execute(Method::GET, path, headers, None).await?;
        decode_success(status, &response_headers, &bytes)
    }

    /// 不带 Bearer 的 V3 GET，返回 `data`。
    pub async fn get_json_anon<T: DeserializeOwned>(&self, path: &str) -> Result<T, V3Error> {
        let (status, response_headers, bytes) =
            self.execute(Method::GET, path, HeaderMap::new(), None).await?;
        decode_success(status, &response_headers, &bytes)
    }

    /// 获取聊天室实时订阅票据（单次使用，60 秒内建立连接）。
    pub async fn chat_realtime_ticket(
        &self,
        access_token: &str,
    ) -> Result<V3RealtimeTicket, V3Error> {
        let headers = auth_headers(access_token, None)?;
        let (status, response_headers, bytes) = self
            .execute(
                Method::POST,
                REALTIME_TICKET_PATH,
                headers,
                Some(Payload::Json(json!({}))),
            )
            .await?;
        decode_success(status, &response_headers, &bytes)
    }

    /// 读取账号流程使用的极验4组件公开标识（无需访问令牌）。
    pub async fn captcha_options(&self) -> Result<V3CaptchaOptions, V3Error> {
        let (status, response_headers, bytes) = self
            .execute(Method::GET, CAPTCHA_OPTIONS_PATH, HeaderMap::new(), None)
            .await?;
        decode_success(status, &response_headers, &bytes)
    }

    /// 提交极验4交互结果；成功后可重试此前受限的 V3 请求。
    pub async fn validate_captcha(&self, captcha_json: &str) -> Result<(), V3Error> {
        let captcha_json = captcha_json.trim();
        let parsed: Value = serde_json::from_str(captcha_json)
            .map_err(|_| V3Error::protocol("人机验证结果不是合法 JSON"))?;
        if !parsed.is_object() {
            return Err(V3Error::protocol("人机验证结果必须是 JSON 对象"));
        }
        if captcha_json.len() > CAPTCHA_JSON_MAX_BYTES {
            return Err(V3Error::protocol("人机验证结果超出长度限制"));
        }
        let (status, _, bytes) = self
            .execute(
                Method::POST,
                VALIDATE_CAPTCHA_PATH,
                HeaderMap::new(),
                Some(Payload::Json(json!({ "captcha": captcha_json }))),
            )
            .await?;
        decode_validate_captcha(status, &bytes)
    }

    /// 站点根地址（形如 `https://fishpi.cn`），供实时订阅拼接 WS 地址。
    pub(crate) fn base_url(&self) -> &str {
        &self.base
    }

    /// 带 Bearer 与幂等键的 V3 PUT，返回 `data`。
    pub async fn put_json<T: DeserializeOwned, B: Serialize>(
        &self,
        path: &str,
        access_token: &str,
        idempotency_key: &str,
        body: &B,
    ) -> Result<T, V3Error> {
        self.send_json(Method::PUT, path, access_token, idempotency_key, body)
            .await
    }

    /// 带 Bearer 与幂等键的 V3 POST，返回 `data`。
    pub async fn post_json<T: DeserializeOwned, B: Serialize>(
        &self,
        path: &str,
        access_token: &str,
        idempotency_key: &str,
        body: &B,
    ) -> Result<T, V3Error> {
        self.send_json(Method::POST, path, access_token, idempotency_key, body)
            .await
    }

    /// 带幂等键 JSON 请求体的一次性请求。
    async fn send_json<T: DeserializeOwned, B: Serialize>(
        &self,
        method: Method,
        path: &str,
        access_token: &str,
        idempotency_key: &str,
        body: &B,
    ) -> Result<T, V3Error> {
        let headers = auth_headers(access_token, Some(idempotency_key))?;
        let body = serde_json::to_value(body)
            .map_err(|err| V3Error::protocol(format!("V3 请求体序列化失败: {err}")))?;
        let (status, response_headers, bytes) = self
            .execute(method, path, headers, Some(Payload::Json(body)))
            .await?;
        decode_success(status, &response_headers, &bytes)
    }

    fn request(&self, method: Method, path: &str, headers: HeaderMap) -> RequestBuilder {
        self.http
            .request(method, format!("{}/{}", self.base, path.trim_start_matches('/')))
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Origin", self.base.as_str())
            .header("Referer", format!("{}/", self.base))
            .headers(headers)
    }

    async fn execute(
        &self,
        method: Method,
        path: &str,
        headers: HeaderMap,
        payload: Option<Payload>,
    ) -> Result<(StatusCode, HeaderMap, Vec<u8>), V3Error> {
        let mut request = self.request(method, path, headers);
        if let Some(payload) = payload {
            request = match payload {
                Payload::Json(value) => request.json(&value),
                Payload::Form(fields) => request.form(&fields),
            };
        }
        let response = request
            .send()
            .await
            .map_err(|err| V3Error::transport(err.to_string()))?;
        let status = response.status();
        let headers = response.headers().clone();
        let bytes = response
            .bytes()
            .await
            .map_err(|err| V3Error::transport(err.to_string()))?
            .to_vec();
        Ok((status, headers, bytes))
    }
}

enum Payload {
    Json(Value),
    #[allow(dead_code)]
    Form(Vec<(String, String)>),
}

/// V3 仅接受调用方提供的访问令牌，不读取 apiKey 或 Cookie。
fn auth_headers(access_token: &str, idempotency_key: Option<&str>) -> Result<HeaderMap, V3Error> {
    let token = access_token.trim();
    if token.is_empty() {
        return Err(V3Error::unauthorized("缺少 V3 访问令牌"));
    }
    let mut authorization = HeaderValue::from_str(&format!("Bearer {token}"))
        .map_err(|_| V3Error::protocol("V3 访问令牌包含非法字符"))?;
    authorization.set_sensitive(true);
    let mut headers = HeaderMap::new();
    headers.insert(AUTHORIZATION, authorization);
    if let Some(key) = idempotency_key {
        let key = key.trim();
        if key.is_empty() {
            return Err(V3Error::protocol("V3 幂等键为空"));
        }
        let value = HeaderValue::from_str(key)
            .map_err(|_| V3Error::protocol("V3 幂等键包含非法字符"))?;
        headers.insert(HeaderName::from_static("idempotency-key"), value);
    }
    Ok(headers)
}

fn cookie_header(cookies: &[V3Cookie]) -> Result<HeaderValue, V3Error> {
    let joined = cookies
        .iter()
        .filter(|cookie| !cookie.name.trim().is_empty())
        .map(|cookie| format!("{}={}", cookie.name.trim(), cookie.value))
        .collect::<Vec<_>>()
        .join("; ");
    HeaderValue::from_str(&joined).map_err(|_| V3Error::protocol("V3 刷新凭据包含非法字符"))
}

fn cookies_from_headers(headers: &HeaderMap) -> Vec<V3Cookie> {
    headers
        .get_all(SET_COOKIE)
        .iter()
        .filter_map(|value| value.to_str().ok())
        .filter_map(|cookie| {
            let pair = cookie.split(';').next()?.trim();
            let (name, value) = pair.split_once('=')?;
            let name = name.trim();
            if name.is_empty() {
                None
            } else {
                Some(V3Cookie {
                    name: name.to_string(),
                    value: value.trim().to_string(),
                })
            }
        })
        .collect()
}

fn decode_success<T: DeserializeOwned>(
    status: StatusCode,
    headers: &HeaderMap,
    bytes: &[u8],
) -> Result<T, V3Error> {
    if !status.is_success() {
        return Err(error_from_response(status, headers, bytes));
    }
    let mut value: Value = serde_json::from_slice(bytes)
        .map_err(|err| V3Error::protocol(format!("V3 响应不是合法 JSON: {err}")))?;
    let object = value
        .as_object_mut()
        .ok_or_else(|| V3Error::protocol("V3 响应不是 JSON 对象"))?;
    let code = match object.get("code") {
        Some(Value::Number(number)) => number.as_i64(),
        Some(Value::String(text)) => text.parse::<i64>().ok(),
        _ => None,
    }
    .ok_or_else(|| V3Error::protocol("V3 响应缺少业务码 code"))?;
    if code != 0 {
        let message = object
            .get("msg")
            .and_then(Value::as_str)
            .unwrap_or("V3 接口返回业务错误")
            .to_string();
        return Err(V3Error {
            kind: kind_for_business_code(None),
            status: None,
            code: Some(code.to_string()),
            message,
            retry_after: None,
        });
    }
    let data = object
        .remove("data")
        .ok_or_else(|| V3Error::protocol("V3 响应缺少 data"))?;
    serde_json::from_value(data)
        .map_err(|err| V3Error::protocol(format!("V3 响应 data 解析失败: {err}")))
}

/// 解析 /validateCaptcha 的独立响应格式（仅 code: 0/-1，不使用 V3 信封）。
fn decode_validate_captcha(status: StatusCode, bytes: &[u8]) -> Result<(), V3Error> {
    let body: Value = serde_json::from_slice(bytes)
        .map_err(|_| V3Error::protocol("人机验证响应不是合法 JSON"))?;
    if !body.is_object() {
        return Err(V3Error::protocol("人机验证响应格式异常"));
    }
    match body.get("code").and_then(Value::as_i64) {
        Some(0) => Ok(()),
        Some(_) => Err(V3Error {
            kind: V3ErrorKind::Invalid,
            status: Some(status.as_u16()),
            code: None,
            message: body
                .get("msg")
                .and_then(Value::as_str)
                .filter(|text| !text.trim().is_empty())
                .unwrap_or("人机验证未通过")
                .to_string(),
            retry_after: None,
        }),
        None => Err(V3Error::protocol("人机验证响应缺少 code")),
    }
}

fn error_from_response(status: StatusCode, headers: &HeaderMap, bytes: &[u8]) -> V3Error {
    let parsed = serde_json::from_slice::<Value>(bytes).ok();
    let code = parsed.as_ref().and_then(|value| match value.get("code") {
        Some(Value::String(text)) => Some(text.clone()),
        Some(Value::Number(number)) => Some(number.to_string()),
        _ => None,
    });
    let message = parsed
        .as_ref()
        .and_then(|value| value.get("msg"))
        .and_then(Value::as_str)
        .map(str::to_string)
        .filter(|text| !text.trim().is_empty())
        .unwrap_or_else(|| format!("V3 请求失败: HTTP {status}"));
    let retry_after = headers
        .get(RETRY_AFTER)
        .and_then(|value| value.to_str().ok())
        .and_then(|text| text.trim().parse::<u64>().ok())
        .map(Duration::from_secs);
    V3Error {
        kind: kind_for(status, code.as_deref()),
        status: Some(status.as_u16()),
        code,
        message,
        retry_after,
    }
}

fn kind_for(status: StatusCode, code: Option<&str>) -> V3ErrorKind {
    match code {
        Some("captcha_required") => return V3ErrorKind::Captcha,
        Some("mfa_required") => return V3ErrorKind::MfaRequired,
        _ => {}
    }
    match status {
        StatusCode::UNAUTHORIZED => V3ErrorKind::Unauthorized,
        StatusCode::FORBIDDEN => V3ErrorKind::Forbidden,
        StatusCode::NOT_FOUND => V3ErrorKind::NotFound,
        StatusCode::CONFLICT => V3ErrorKind::Conflict,
        StatusCode::UNPROCESSABLE_ENTITY => V3ErrorKind::Invalid,
        StatusCode::TOO_MANY_REQUESTS => V3ErrorKind::RateLimited,
        status if status.is_server_error() => V3ErrorKind::Server,
        _ => kind_for_business_code(code),
    }
}

fn kind_for_business_code(code: Option<&str>) -> V3ErrorKind {
    match code {
        Some("captcha_required") => V3ErrorKind::Captcha,
        Some("mfa_required") => V3ErrorKind::MfaRequired,
        Some("membership_required") | Some("appearance_required") => V3ErrorKind::Forbidden,
        Some("idempotency_conflict") => V3ErrorKind::Conflict,
        Some("invalid_request") => V3ErrorKind::Invalid,
        _ => V3ErrorKind::Protocol,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn client() -> V3Client {
        V3Client::new(&Config::default()).unwrap()
    }

    #[test]
    fn error_kinds_follow_status_and_business_code() {
        assert_eq!(
            kind_for(StatusCode::UNAUTHORIZED, None),
            V3ErrorKind::Unauthorized
        );
        assert_eq!(
            kind_for(StatusCode::FORBIDDEN, Some("membership_required")),
            V3ErrorKind::Forbidden
        );
        assert_eq!(
            kind_for(StatusCode::FORBIDDEN, Some("captcha_required")),
            V3ErrorKind::Captcha
        );
        assert_eq!(
            kind_for(StatusCode::CONFLICT, Some("idempotency_conflict")),
            V3ErrorKind::Conflict
        );
        assert_eq!(
            kind_for(StatusCode::UNPROCESSABLE_ENTITY, None),
            V3ErrorKind::Invalid
        );
        assert_eq!(
            kind_for(StatusCode::SERVICE_UNAVAILABLE, None),
            V3ErrorKind::Server
        );
    }

    #[test]
    fn error_response_preserves_status_code_message_and_retry_after() {
        let mut headers = HeaderMap::new();
        headers.insert(RETRY_AFTER, HeaderValue::from_static("12"));
        let error = error_from_response(
            StatusCode::FORBIDDEN,
            &headers,
            br#"{"code":"appearance_required","msg":"need vip"}"#,
        );
        assert_eq!(error.kind, V3ErrorKind::Forbidden);
        assert_eq!(error.status, Some(403));
        assert_eq!(error.code.as_deref(), Some("appearance_required"));
        assert_eq!(error.message, "need vip");

        let error = error_from_response(
            StatusCode::TOO_MANY_REQUESTS,
            &headers,
            br#"{"code":429,"msg":"slow down"}"#,
        );
        assert_eq!(error.kind, V3ErrorKind::RateLimited);
        assert_eq!(error.retry_after, Some(Duration::from_secs(12)));
    }

    #[test]
    fn strict_envelope_requires_numeric_zero_code_and_data() {
        let headers = HeaderMap::new();
        assert!(decode_success::<Value>(StatusCode::OK, &headers, br#"{"data":{}}"#).is_err());
        assert!(decode_success::<Value>(StatusCode::OK, &headers, br#"{"code":0}"#).is_err());
        assert!(
            decode_success::<Value>(StatusCode::OK, &headers, br#"{"code":"ok","data":{}}"#)
                .is_err()
        );
        let value: Value =
            decode_success(StatusCode::OK, &headers, br#"{"code":0,"data":{}}"#).unwrap();
        assert_eq!(value, serde_json::json!({}));
    }

    #[test]
    fn business_error_inside_success_status_is_not_accepted() {
        let headers = HeaderMap::new();
        let error = decode_success::<Value>(
            StatusCode::OK,
            &headers,
            br#"{"code":-1,"msg":"auth required"}"#,
        )
        .unwrap_err();
        assert_eq!(error.code.as_deref(), Some("-1"));
        assert!(decode_success::<Value>(StatusCode::OK, &headers, b"<html></html>").is_err());
    }

    #[test]
    fn set_cookie_pairs_are_read_and_replayed() {
        let mut headers = HeaderMap::new();
        headers.append(
            SET_COOKIE,
            HeaderValue::from_static("sym-ce=abc123; Path=/; HttpOnly"),
        );
        headers.append(
            SET_COOKIE,
            HeaderValue::from_static("theme=dark=1; Path=/; Expires=Wed, 21 Oct 2026 07:28:00 GMT"),
        );
        let cookies = cookies_from_headers(&headers);
        assert_eq!(
            cookies,
            vec![
                V3Cookie {
                    name: "sym-ce".into(),
                    value: "abc123".into()
                },
                V3Cookie {
                    name: "theme".into(),
                    value: "dark=1".into()
                },
            ]
        );
        assert_eq!(
            cookie_header(&cookies).unwrap(),
            HeaderValue::from_static("sym-ce=abc123; theme=dark=1")
        );
    }

    #[test]
    fn bearer_requests_never_carry_cookies_or_api_key() {
        let request = client()
            .request(
                Method::GET,
                "api/v3/me/chat-appearance",
                auth_headers("access-token", None).unwrap(),
            )
            .build()
            .unwrap();
        assert_eq!(request.url().path(), "/api/v3/me/chat-appearance");
        assert_eq!(request.url().query(), None);
        assert_eq!(request.headers()[AUTHORIZATION], "Bearer access-token");
        assert!(request.headers()[AUTHORIZATION].is_sensitive());
        assert!(!request.headers().contains_key(COOKIE));
        assert!(!request.headers().contains_key("idempotency-key"));
    }

    #[test]
    fn purchase_request_carries_bearer_and_idempotency_key() {
        let request = client()
            .request(
                Method::POST,
                "api/v3/me/appearance-purchases",
                auth_headers("access-token", Some("purchase-key-1")).unwrap(),
            )
            .json(&json!({ "productId": "bubble-sea-soda", "days": 7, "revision": 3 }))
            .build()
            .unwrap();
        assert_eq!(request.url().path(), "/api/v3/me/appearance-purchases");
        assert_eq!(request.headers()[AUTHORIZATION], "Bearer access-token");
        assert_eq!(request.headers()["idempotency-key"], "purchase-key-1");
        assert!(!request.headers().contains_key(COOKIE));
    }

    #[test]
    fn credentials_are_validated_before_use() {
        assert!(matches!(
            auth_headers("", None),
            Err(V3Error {
                kind: V3ErrorKind::Unauthorized,
                ..
            })
        ));
        assert!(auth_headers("token", Some("   ")).is_err());
        assert!(auth_headers("secret\r\nInjected: 1", None).is_err());
        assert!(auth_headers("token", Some("key\r\nInjected: 1")).is_err());
        assert!(cookie_header(&[V3Cookie {
            name: "sym-ce".into(),
            value: "bad\r\nvalue".into()
        }])
        .is_err());
    }

    #[test]
    fn captcha_options_decode_uses_strict_envelope() {
        let headers = HeaderMap::new();
        let options: V3CaptchaOptions = decode_success(
            StatusCode::OK,
            &headers,
            br#"{"msg":"","code":0,"data":{"provider":"geetest4","captchaId":"6d886bcaec3f86fcfd6f61bff5af2cb4"}}"#,
        )
        .unwrap();
        assert_eq!(options.provider, "geetest4");
        assert_eq!(options.captcha_id, "6d886bcaec3f86fcfd6f61bff5af2cb4");
    }

    #[test]
    fn validate_captcha_uses_independent_format() {
        assert!(decode_validate_captcha(StatusCode::OK, br#"{"code":0}"#).is_ok());
        assert!(decode_validate_captcha(StatusCode::OK, br#"{"msg":"","code":0}"#).is_ok());

        let error = decode_validate_captcha(StatusCode::OK, br#"{"code":-1}"#).unwrap_err();
        assert_eq!(error.kind, V3ErrorKind::Invalid);
        assert!(error.is_rejection());
        assert_eq!(error.message, "人机验证未通过");

        let error = decode_validate_captcha(
            StatusCode::OK,
            "{\"code\":-1,\"msg\":\"校验失败\"}".as_bytes(),
        )
        .unwrap_err();
        assert_eq!(error.message, "校验失败");

        assert!(decode_validate_captcha(StatusCode::OK, br#"{"msg":""}"#).is_err());
        assert!(decode_validate_captcha(StatusCode::OK, b"<html></html>").is_err());
        assert!(decode_validate_captcha(StatusCode::OK, br#"[0]"#).is_err());
    }
}
