# FishPi Rust SDK

[![Crates.io](https://img.shields.io/crates/v/fishpi-sdk.svg)](https://crates.io/crates/fishpi-sdk)
[![Documentation](https://docs.rs/fishpi-sdk/badge.svg)](https://docs.rs/fishpi-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个用于与摸鱼派社区 API 交互的 Rust SDK，提供用户管理、文章、聊天室、私聊、通知、清风明月、红包、评论、举报、日志、文件上传等功能的异步客户端。

## 安装

在 `Cargo.toml` 中添加：

```toml
[dependencies]
fishpi-sdk = "0.1.0"
tokio = { version = "1", features = ["full"] }
```

或使用 Cargo：

```bash
cargo add fishpi-sdk
```

## 快速开始

```rust
use fishpi_sdk::{FishPi, api::user::User};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 登录获取认证用户客户端
    let fishpi = FishPi::login(&login_data).await?;
    
    // 获取用户信息
    let user_info = fishpi.info().await?;
    println!("User: {:?}", user_info);

    // 发送评论
    let result = fishpi.comment.send(&comment_data).await?;
    println!("Comment result: {:?}", result);

    // 使用其他模块
    let articles = fishpi.article.get_recent().await?;
    let chat_result = fishpi.chatroom.send("Hello!").await?;

    Ok(())
}
```

## 功能

- 用户管理：登录、注册、获取用户信息、修改资料
- 内容操作：文章、评论、清风明月
- 聊天：聊天室、私聊
- 其他：通知、红包、举报、日志、文件上传

## API 文档

完整文档请查看 [docs.rs](https://docs.rs/fishpi-sdk)。

## 贡献

欢迎提交 Issue 和 Pull Request。请确保代码通过 `cargo test` 和 `cargo clippy`。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。