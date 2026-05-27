use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::api::chat::Chat;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jlong, jstring};
use serde_json::{Value, json};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::time::timeout;

struct AndroidPrivateChatConnection {
    _runtime: tokio::runtime::Runtime,
    chat: Mutex<Chat>,
    peer: Option<String>,
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
                let user = current_user(&token).await?;
                let chat = &user.chat;
                let unread = chat
                    .unread()
                    .await
                    .map(|items| private_unread_counts(items, &username))
                    .unwrap_or_default();
                let sessions = chat
                    .list()
                    .await
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
                let user = current_user(&token).await?;
                let chat = &user.chat;
                let mut history = timeout(
                    Duration::from_secs(15),
                    chat.history(peer_name.clone(), page.max(1) as u32, 50, false),
                )
                .await
                .map_err(|_| "加载私聊历史超时，请稍后重试".to_string())?
                .map_err(|err| format!("加载私聊历史失败: {err}"))?;
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
                let user = current_user(&token).await?;
                let mut chat = user.chat;
                chat.connect(false, Some(peer_name.clone()))
                    .await
                    .map_err(|err| format!("私聊连接失败: {err}"))?;
                chat.send_ws(&text)
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
        let connection = unsafe { &*(handle as *mut AndroidPrivateChatConnection) };
        let chat = connection
            .chat
            .lock()
            .map_err(|_| "私聊连接状态异常".to_string())?;
        chat.send_ws(&text)
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
                let user = current_user(&token).await?;
                let chat = &user.chat;
                timeout(Duration::from_secs(10), chat.revoke(&id))
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
                let user = current_user(&token).await?;
                let chat = &user.chat;
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
            Some(connect_peer.clone())
        };
        let java_vm = Arc::new(env.get_java_vm().map_err(|err| err.to_string())?);
        let callback = env
            .new_global_ref(&callback)
            .map_err(|err| err.to_string())?;
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|err| format!("Rust runtime init failed: {err}"))?;

        let chat = runtime.block_on(async {
            let user = current_user(&token).await?;
            let mut chat = user.chat;
            let java_vm_for_log = Arc::clone(&java_vm);
            let callback_for_log = callback.clone();
            chat.on_ws_log(move |message| {
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
                    json!({ "event": "revoke", "id": revoke.data }),
                );
            })
            .await;

            chat.connect(false, connect_target.clone())
                .await
                .map_err(|err| format!("私聊实时连接失败: {err}"))?;
            Ok::<Chat, String>(chat)
        })?;

        let handle = Box::new(AndroidPrivateChatConnection {
            _runtime: runtime,
            chat: Mutex::new(chat),
            peer: connect_target,
        });
        emit_callback(
            &java_vm,
            &callback,
            json!({ "event": "status", "message": "私聊已连接" }),
        );

        Ok(Box::into_raw(handle) as jlong)
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

    let connection = unsafe { &*(handle as *mut AndroidPrivateChatConnection) };
    let result = (|| {
        let mut chat = connection
            .chat
            .lock()
            .map_err(|_| "私聊连接锁已损坏".to_string())?;
        connection
            ._runtime
            .block_on(async { chat.reconnect(connection.peer.clone()).await })
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

    let connection = unsafe { Box::from_raw(handle as *mut AndroidPrivateChatConnection) };
    if let Ok(mut chat) = connection.chat.lock() {
        chat.disconnect();
    }
    drop(connection);
}
