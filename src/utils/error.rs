use std::error::Error as StdError;
use std::time::Duration;
use thiserror::Error;

#[derive(Error, Debug)]
#[non_exhaustive]
pub enum Error {
    /// 认证失败（token 失效/未登录）
    #[error("未授权：请重新登录")]
    Unauthorized,

    /// 触发限流，`retry_after` 为服务端建议等待时长
    #[error("请求过于频繁，请稍后再试")]
    RateLimited { retry_after: Option<Duration> },

    /// 资源不存在
    #[error("资源不存在: {resource}")]
    NotFound { resource: String },

    /// 业务层错误：服务端返回非 0 `code`
    #[error("业务错误[{code}]: {msg}")]
    Business { code: i64, msg: String },

    /// 传输层错误（连接/超时/TLS 等）
    #[error("传输错误: {0}")]
    Transport(#[from] reqwest::Error),

    /// 反序列化/解析错误
    #[error("解析错误: {0}")]
    Decode(String),

    /// WebSocket 连接/收发错误
    #[error("WebSocket 错误: {0}")]
    WebSocket(String),

    #[error("Request error: {0}")]
    Request(Box<dyn StdError + Send + Sync>),
    #[error("{0}")]
    Api(String),
    #[error("Parse error: {0}")]
    Parse(String),
}

impl Error {
    /// 该错误是否值得重试
    pub fn is_retryable(&self) -> bool {
        matches!(
            self,
            Error::Transport(_) | Error::RateLimited { .. } | Error::Request(_)
        )
    }

    pub fn business(code: i64, msg: impl Into<String>) -> Self {
        Error::Business {
            code,
            msg: msg.into(),
        }
    }

    pub fn decode(msg: impl Into<String>) -> Self {
        Error::Decode(msg.into())
    }
}
