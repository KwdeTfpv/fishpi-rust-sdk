use crate::handles::HandleRegistry;
use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::client::ChatConnection;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jlong, jstring};
use serde_json::{Value, json};
use std::sync::{Arc, Mutex, OnceLock};
use std::time::Duration;
use tokio::time::timeout;

struct AndroidPrivateChatConnection {
    _runtime: tokio::runtime::Runtime,
    chat: Mutex<ChatConnection>,
}

static PRIVATE_CHAT_HANDLES: OnceLock<HandleRegistry<AndroidPrivateChatConnection>> =
    OnceLock::new();

fn handles() -> &'static HandleRegistry<AndroidPrivateChatConnection> {
    PRIVATE_CHAT_HANDLES.get_or_init(HandleRegistry::new)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getPrivateChatSessions(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    self_username: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let username = string_arg(&mut env, self_username)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let chat = client.chat();
                let unread = timeout(Duration::from_secs(8), chat.unread())
                    .await
                    .ok()
                    .and_then(Result::ok)
                    .map(|items| private_unread_counts(items, &username))
                    .unwrap_or_default();
                let sessions = timeout(Duration::from_secs(16), chat.list())
                    .await
                    .map_err(|_| "加载私聊会话超时".to_string())?
                    .map_err(|err| format!("加载私聊会话失败: {err}"))?
                    .into_iter()
                    .map(|msg| {
                        let peer = private_peer(&msg, &username);
                        let count = unread.get(&peer).copied().unwrap_or(0);
                        private_session_to_json(msg, &username, count)
                    })
                    .filter(|item| {
                        item["peer"]
                            .as_str()
                            .map(|s| !s.trim().is_empty())
                            .unwrap_or(false)
                    })
                    .collect::<Vec<_>>();
                Ok(Value::Array(sessions))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getPrivateChatHistory(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    peer: JString,
    page: i32,
    self_username: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let peer_name = string_arg(&mut env, peer)?;
        let username = string_arg(&mut env, self_username)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let chat = client.chat();
                let history_result = timeout(
                    Duration::from_secs(15),
                    chat.history(peer_name, page.max(1) as u32, 50, false),
                )
                .await
                .map_err(|_| "加载私聊历史超时，请稍后重试".to_string())?;
                let mut history = match history_result {
                    Ok(items) => items,
                    Err(err) => {
                        let message = err.to_string();
                        if message.contains("没有更多消息") {
                            Vec::new()
                        } else {
                            return Err(format!("加载私聊历史失败: {message}"));
                        }
                    }
                };
                history.reverse();
                let messages = history
                    .into_iter()
                    .map(|msg| private_message_to_json(msg, &username))
                    .collect::<Vec<_>>();
                Ok(Value::Array(messages))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendPrivateChatMessage(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    peer: JString,
    content: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let peer_name = string_arg(&mut env, peer)?;
        let text = string_arg(&mut env, content)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let connection = client
                    .chat()
                    .connect(Some(peer_name.trim()))
                    .await
                    .map_err(|err| format!("私聊连接失败: {err}"))?;
                connection
                    .send(&text)
                    .map_err(|err| format!("私聊发送失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendPrivateChatMessageOnConnection(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    content: JString,
) -> jstring {
    let result = (|| {
        if handle == 0 {
            return Err("私聊未连接".to_string());
        }
        let text = string_arg(&mut env, content)?;
        let Some(connection) = handles().get(handle) else {
            return Err("私聊未连接".to_string());
        };
        let chat = connection
            .chat
            .lock()
            .map_err(|_| "私聊连接状态异常".to_string())?;
        chat.send(&text)
            .map_err(|err| format!("私聊发送失败: {err}"))?;
        Ok(runtime_json(|_| Ok(Value::Null)))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_revokePrivateChatMessage(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    message_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, message_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let chat = client.chat();
                timeout(Duration::from_secs(10), chat.revoke(id.as_str()))
                    .await
                    .map_err(|_| "私聊撤回超时，请稍后重试".to_string())?
                    .map_err(|err| format!("私聊撤回失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_markPrivateChatRead(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    peer: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let peer_name = string_arg(&mut env, peer)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let chat = client.chat();
                timeout(Duration::from_secs(8), chat.mark_as_read(peer_name))
                    .await
                    .map_err(|_| "标记私聊已读超时".to_string())?
                    .map_err(|err| format!("标记私聊已读失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_connectPrivateChat(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    self_username: JString,
    peer: JString,
    callback: JObject,
) -> jlong {
    let result: Result<jlong, String> = (|| {
        let token = string_arg(&mut env, api_key)?;
        let username = string_arg(&mut env, self_username)?;
        let peer_name = string_arg(&mut env, peer)?;
        let connect_peer = peer_name.trim().to_string();
        let connect_target = if connect_peer.is_empty() {
            None
        } else {
            Some(connect_peer)
        };
        let java_vm = Arc::new(env.get_java_vm().map_err(|err| err.to_string())?);
        let callback = env
            .new_global_ref(&callback)
            .map_err(|err| err.to_string())?;
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|err| format!("Rust runtime init failed: {err}"))?;

        let chat = runtime.block_on(async {
            let client = current_user(&token).await?;
            let mut chat = client
                .chat()
                .connect(connect_target.as_deref())
                .await
                .map_err(|err| format!("私聊实时连接失败: {err}"))?;
            let java_vm_for_log = Arc::clone(&java_vm);
            let callback_for_log = callback.clone();
            chat.set_log_hook(move |message: &str| {
                emit_callback(
                    &java_vm_for_log,
                    &callback_for_log,
                    json!({ "event": "status", "message": message }),
                );
            });

            let java_vm_for_notice = Arc::clone(&java_vm);
            let callback_for_notice = callback.clone();
            chat.on_notice(move |notice| {
                emit_callback(
                    &java_vm_for_notice,
                    &callback_for_notice,
                    json!({
                        "event": "notice",
                        "notice": private_notice_to_json(notice),
                    }),
                );
            })
            .await;

            let java_vm_for_data = Arc::clone(&java_vm);
            let callback_for_data = callback.clone();
            let username_for_data = username.clone();
            chat.on_data(move |msg| {
                emit_callback(
                    &java_vm_for_data,
                    &callback_for_data,
                    json!({
                        "event": "message",
                        "message": private_message_to_json(msg, &username_for_data),
                    }),
                );
            })
            .await;

            let java_vm_for_revoke = Arc::clone(&java_vm);
            let callback_for_revoke = callback.clone();
            chat.on_revoke(move |revoke| {
                emit_callback(
                    &java_vm_for_revoke,
                    &callback_for_revoke,
                    json!({ "event": "revoke", "id": revoke.id }),
                );
            })
            .await;

            Ok::<ChatConnection, String>(chat)
        })?;

        let handle = AndroidPrivateChatConnection {
            _runtime: runtime,
            chat: Mutex::new(chat),
        };
        emit_callback(
            &java_vm,
            &callback,
            json!({ "event": "status", "message": "私聊已连接" }),
        );

        Ok(handles().insert(handle))
    })();

    match result {
        Ok(handle) => handle,
        Err(err) => {
            let fallback_vm = env.get_java_vm().ok().map(Arc::new);
            if let (Some(java_vm), Ok(callback)) = (fallback_vm, env.new_global_ref(&callback)) {
                emit_callback(
                    &java_vm,
                    &callback,
                    json!({ "event": "error", "message": err }),
                );
            }
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_reconnectPrivateChat(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    if handle == 0 {
        return 0;
    }

    let Some(connection) = handles().get(handle) else {
        return 0;
    };
    let result = (|| {
        let mut chat = connection
            .chat
            .lock()
            .map_err(|_| "私聊连接锁已损坏".to_string())?;
        connection
            ._runtime
            .block_on(async { chat.reconnect().await })
            .map_err(|err| format!("私聊重连失败: {err}"))?;
        Ok::<(), String>(())
    })();

    if result.is_ok() { 1 } else { 0 }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_disconnectPrivateChat(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }

    let Some(connection) = handles().remove(handle) else {
        return;
    };
    if let Ok(mut chat) = connection.chat.lock() {
        chat.disconnect();
    }
}
