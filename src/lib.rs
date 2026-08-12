//! # 摸鱼派 Rust SDK
//!
//! 与摸鱼派社区 API 交互的异步客户端，覆盖用户、文章、评论、通知、清风明月、
//! 表情、红包、聊天室、私聊、举报、日志、文件上传等功能。
//!
//! ## 主要组件
//!
//! - [`FishPi`] - 客户端，`builder()` 构造，`login()` 换取带鉴权的实例。
//! - [`domain`] - 对外数据类型。
//! - [`wire`] - 服务端响应的原始形状（内部反序列化用）。
//! - [`ws`] - WebSocket 基础设施。
//! - [`utils`] - HTTP、错误处理等辅助。
//!
//! 各功能按域组织，经访问器调用：`client.user()`、`client.article()`、
//! `client.chatroom()` 等。
//!
//! ## 示例
//!
//! ```rust,no_run
//! use fishpi_sdk::{FishPi, model::misc::LoginData};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let login = LoginData::new("your_name_or_email", "your_password", None);
//!     let fishpi = FishPi::builder().login(&login).await?;
//!     let points = fishpi.user().points("target_user").await?;
//!     println!("{}: {}", points.username, points.point);
//!     Ok(())
//! }
//! ```
pub mod client;
pub mod domain;
pub mod ids;
pub mod model;
pub mod utils;
pub mod wire;
pub mod ws;

pub use client::{FishPi, FishPiBuilder};
