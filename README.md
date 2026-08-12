# FishPi Rust SDK

[![Crates.io](https://img.shields.io/crates/v/fishpi-sdk.svg)](https://crates.io/crates/fishpi-sdk)
[![Documentation](https://docs.rs/fishpi-sdk/badge.svg)](https://docs.rs/fishpi-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个用于与摸鱼派社区 API 交互的 Rust SDK，提供用户管理、文章、聊天室、私聊、通知、清风明月、红包、评论、举报、日志、文件上传等功能的异步客户端。

## 安装

在 `Cargo.toml` 中添加：

```toml
[dependencies]
fishpi-sdk = "1.0.0"
tokio = { version = "1", features = ["full"] }
```

或使用 Cargo：

```bash
cargo add fishpi-sdk
```

## 快速开始

登录后按域调用，功能通过访问器组织（`client.user()`、`client.article()`、`client.chatroom()` 等）。

```rust
use fishpi_sdk::{FishPi, model::misc::LoginData};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 构造并登录，得到带鉴权的客户端（可 Clone，跨任务共享）
    let login = LoginData::new("your_name_or_email", "your_password", None);
    let fishpi = FishPi::builder().login(&login).await?;

    // 用户域
    let me = fishpi.user().info().await?;
    println!("你好, {}", me.display_name());

    // 发一条清风明月
    fishpi.breezemoon().send("摸鱼中").await?;

    Ok(())
}
```

### 聊天室（WebSocket 长连接）

```rust
use fishpi_sdk::{FishPi, model::misc::LoginData};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let login = LoginData::new("your_name_or_email", "your_password", None);
    let fishpi = FishPi::builder().login(&login).await?;

    // 建立聊天室连接，返回独立句柄
    let conn = fishpi.chatroom().connect().await?;

    // 监听消息（直接拿到具体类型，无需 match）
    conn.on_msg(|msg| {
        println!("消息: {}", msg.content);
    }).await;

    // 发送消息走 REST
    fishpi.chatroom().send("Hello!").await?;

    Ok(())
}
```

## 域访问器

登录后所有功能按域组织，`fishpi.<域>()` 返回该域的访问器，方法挂在其上。

| 访问器 | 功能 |
|---|---|
| `fishpi.user()` | 账户信息、他人资料、积分、常用表情、活跃度、签到、转账、关注/取关、勋章、改头像/资料、用户名联想、最近注册、VIP 信息 |
| `fishpi.article()` | 发帖/改帖、列表、详情、Markdown 原文、草稿增删查、投票、感谢、收藏、关注、打赏、在线人数；`connect()` 建立文章频道长连接 |
| `fishpi.comment()` | 发布、更新、投票、感谢、删除评论 |
| `fishpi.chatroom()` | 发消息、历史、附近消息、撤回、弹幕、弹幕花费、禁言列表、消息原文、设置话题；`connect()` 建立聊天室长连接 |
| `fishpi.chat()` | 私聊会话列表、历史、标记已读、未读、撤回；`connect(user)` 建立私聊/系统通知长连接 |
| `fishpi.notice()` | 未读数、通知列表、标记已读、全部已读；`connect()` 建立通知长连接 |
| `fishpi.emoji()` | 表情分组与表情项的增删改查 |
| `fishpi.reaction()` | 给帖子/评论/聊天室消息贴表情 |
| `fishpi.redpacket()` | 打开、发送红包 |
| `fishpi.profession()` | 职业成长：查询/设置主职业、等级详情、经验记录、隐私配置、公开资料、排行榜 |
| `fishpi.breezemoon()` | 清风明月列表、发送 |
| `fishpi.finger(gold_key)` | 摸金手指（管理员，需金手指密钥）：勋章增删、背包、积分、活跃度、登录 IP |

顶层方法（无需域，直接挂 `fishpi`）：`login`、`register`、`pre_register`、`verify`、`report`、`upload`、`log`。

WebSocket 长连接（`chatroom`/`chat`/`notice`/`article` 的 `connect()`）返回独立句柄，通过 `on_*` 注册监听器，支持断开与重连。

## API 文档

完整文档请查看 [docs.rs](https://docs.rs/fishpi-sdk)。

## 贡献

欢迎提交 Issue 和 Pull Request。请确保代码通过 `cargo test` 和 `cargo clippy`。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。
