use std::sync::Arc;

use serde_json::Value;

use crate::client::core::FishPi;
use crate::domain::notice::{
    NoticeCount, NoticeItem, NoticeList, NoticeMsg, NoticeMsgType, NoticeType,
};
use crate::utils::build_http_path;
use crate::utils::error::Error;
use crate::wire::notice::{
    WireNoticeAt, WireNoticeComment, WireNoticeCount, WireNoticeFollow, WireNoticeMsg,
    WireNoticePoint, WireNoticeReply, WireNoticeSystem,
};
use crate::ws::{ParsedMessageHandler, RetryPolicy, WsConnection, WsLogHook, build_ws_url};

type NoticeHandler = ParsedMessageHandler<NoticeEventKind, NoticeMsg>;

/// 通知推送事件类别（监听器键）。
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum NoticeEventKind {
    /// 刷新通知数（需再调 count 拉明细）
    Refresh,
    /// 全局公告
    Broadcast,
    All,
}

fn parse_notice_message(json: &Value) -> Result<(NoticeEventKind, NoticeMsg), Error> {
    let payload = json
        .get("data")
        .filter(|v| v.get("command").is_some())
        .unwrap_or(json);
    let command = payload
        .get("command")
        .and_then(Value::as_str)
        .ok_or_else(|| Error::decode("Missing command in notice message"))?;
    let kind = match NoticeMsgType::from_command(command) {
        Some(NoticeMsgType::Refresh) => NoticeEventKind::Refresh,
        Some(NoticeMsgType::WarnBroadcast) => NoticeEventKind::Broadcast,
        None => return Err(Error::decode(format!("Unknown notice command: {command}"))),
    };
    let wire: WireNoticeMsg =
        serde_json::from_value(payload.clone()).map_err(|e| Error::decode(e.to_string()))?;
    Ok((kind, wire.into()))
}

/// 通知域访问器。
pub struct NoticeApi(pub(crate) FishPi);

impl FishPi {
    /// 通知域。
    pub fn notice(&self) -> NoticeApi {
        NoticeApi(self.clone())
    }
}

impl NoticeApi {
    pub async fn count(&self) -> Result<NoticeCount, Error> {
        let wire: WireNoticeCount = self
            .0
            .get_data(&self.0.with_key("notifications/unread/count"))
            .await?;
        Ok(wire.into())
    }

    pub async fn list(&self, notice_type: NoticeType) -> Result<NoticeList, Error> {
        let path = build_http_path(
            "api/getNotifications",
            &[
                ("apiKey", self.0.api_key().to_string()),
                ("type", notice_type.as_str().to_string()),
            ],
        );
        let list = match notice_type {
            NoticeType::Point => {
                let wire: Vec<WireNoticePoint> = self.0.get_data(&path).await?;
                wire.into_iter()
                    .map(|w| NoticeItem::Point(w.into()))
                    .collect()
            }
            NoticeType::Commented => {
                let wire: Vec<WireNoticeComment> = self.0.get_data(&path).await?;
                wire.into_iter()
                    .map(|w| NoticeItem::Comment(w.into()))
                    .collect()
            }
            NoticeType::Reply => {
                let wire: Vec<WireNoticeReply> = self.0.get_data(&path).await?;
                wire.into_iter()
                    .map(|w| NoticeItem::Reply(w.into()))
                    .collect()
            }
            NoticeType::At => {
                let wire: Vec<WireNoticeAt> = self.0.get_data(&path).await?;
                wire.into_iter().map(|w| NoticeItem::At(w.into())).collect()
            }
            NoticeType::Following => {
                let wire: Vec<WireNoticeFollow> = self.0.get_data(&path).await?;
                wire.into_iter()
                    .map(|w| NoticeItem::Follow(w.into()))
                    .collect()
            }
            NoticeType::System => {
                let wire: Vec<WireNoticeSystem> = self.0.get_data(&path).await?;
                wire.into_iter()
                    .map(|w| NoticeItem::System(w.into()))
                    .collect()
            }
            NoticeType::Broadcast => {
                return Err(Error::decode("broadcast 通知不支持列表查询"));
            }
        };
        Ok(list)
    }

    pub async fn make_read(&self, notice_type: NoticeType) -> Result<(), Error> {
        let path = self
            .0
            .with_key(&format!("notifications/make-read/{}", notice_type.as_str()));
        self.0.get_raw(&path).await?;
        Ok(())
    }

    pub async fn read_all(&self) -> Result<(), Error> {
        self.0
            .get_raw(&self.0.with_key("notifications/all-read"))
            .await?;
        Ok(())
    }

    /// 建立通知长连接（user-channel）。
    pub async fn connect(&self) -> Result<NoticeConnection, Error> {
        let url = build_ws_url(
            self.0.domain(),
            "user-channel",
            &[("apiKey", self.0.api_key().to_string())],
        )
        .map_err(Error::from)?;
        let handler =
            NoticeHandler::new(parse_notice_message, Some(NoticeEventKind::All), "notice");
        let mut connection = WsConnection::new();
        connection
            .connect(false, &url, handler.clone())
            .await
            .map_err(Error::from)?;
        Ok(NoticeConnection {
            connection,
            handler,
            url,
        })
    }
}

/// 通知长连接：监听刷新/公告、断开重连。
pub struct NoticeConnection {
    connection: WsConnection,
    handler: NoticeHandler,
    url: String,
}

impl NoticeConnection {
    /// 监听「刷新通知数」事件。
    pub async fn on_refresh<F>(&self, listener: F)
    where
        F: Fn(NoticeMsg) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(NoticeEventKind::Refresh, listener)
            .await;
    }

    /// 监听全局公告事件。
    pub async fn on_broadcast<F>(&self, listener: F)
    where
        F: Fn(NoticeMsg) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(NoticeEventKind::Broadcast, listener)
            .await;
    }

    /// 监听全部通知推送。
    pub async fn on_all<F>(&self, listener: F)
    where
        F: Fn(NoticeMsg) + Send + Sync + 'static,
    {
        self.handler
            .get_emitter()
            .add_listener(NoticeEventKind::All, listener)
            .await;
    }

    /// 移除某类监听。
    pub async fn off(&self, kind: NoticeEventKind) {
        self.handler.get_emitter().remove_listener(Some(kind)).await;
    }

    /// 重连。
    pub async fn reconnect(&mut self) -> Result<(), Error> {
        self.connection
            .reconnect(&self.url, self.handler.clone())
            .await
            .map_err(Error::from)
    }

    /// 设置重连策略。
    pub fn set_retry_policy(&mut self, policy: RetryPolicy) {
        self.connection.set_retry_policy(policy);
    }

    /// 设置日志钩子。
    pub fn set_log_hook<F>(&mut self, hook: F)
    where
        F: Fn(&str) + Send + Sync + 'static,
    {
        let hook = Arc::new(hook) as WsLogHook;
        self.connection.set_log_hook_arc(hook.clone());
        self.handler.set_log_hook_arc(hook);
    }

    /// 断开连接。
    pub fn disconnect(&mut self) {
        self.connection.disconnect();
    }
}
