use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::FishPi;
use fishpi_sdk::client::ChatRoomConnection;
use fishpi_sdk::domain::chatroom::ChatContentType;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jlong, jstring};
use serde_json::{Value, json};
use std::sync::{Arc, Mutex, OnceLock};

/// App 侧配置的消息来源标识（`客户端/版本`）。
static CLIENT_TAG: OnceLock<Mutex<Option<String>>> = OnceLock::new();

fn client_tag_slot() -> &'static Mutex<Option<String>> {
    CLIENT_TAG.get_or_init(|| Mutex::new(None))
}

fn stored_client_tag() -> Option<String> {
    client_tag_slot().lock().ok().and_then(|g| g.clone())
}

/// 按 App 配置的 client_tag 派生客户端；未配置则用默认。
fn with_stored_tag(client: FishPi) -> FishPi {
    match stored_client_tag() {
        Some(tag) => client.with_client_tag(tag),
        None => client,
    }
}

struct AndroidChatRoomConnection {
    _runtime: tokio::runtime::Runtime,
    connection: Mutex<ChatRoomConnection>,
    java_vm: Arc<jni::JavaVM>,
    callback: jni::objects::GlobalRef,
}

/// 拼接消息来源标识（`客户端/版本`），任一为空返回 None。
fn build_client_tag(client: &str, version: &str) -> Option<String> {
    let client = client.trim();
    let version = version.trim();
    if client.is_empty() || version.is_empty() {
        return None;
    }
    Some(format!("{client}/{version}"))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getChatRoomHistory(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    page: i32,
    self_username: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let username = string_arg(&mut env, self_username)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let mut history = client
                    .chatroom()
                    .history(page.max(1) as u32, ChatContentType::Html)
                    .await
                    .map_err(|err| format!("加载历史失败: {err}"))?;
                history.reverse();
                let messages = history
                    .into_iter()
                    .map(|msg| chat_message_to_json(msg, &username))
                    .collect::<Vec<_>>();
                Ok(Value::Array(messages))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_setChatRoomClientType(
    mut env: JNIEnv,
    _class: JClass,
    client: JString,
    version: JString,
) {
    let Ok(client_name) = string_arg(&mut env, client) else {
        return;
    };
    let Ok(version_name) = string_arg(&mut env, version) else {
        return;
    };
    if let Ok(mut guard) = client_tag_slot().lock() {
        *guard = build_client_tag(&client_name, &version_name);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendChatRoomMessageWithClientType(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    content: JString,
    client: JString,
    version: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let text = string_arg(&mut env, content)?;
        let client_name = string_arg(&mut env, client)?;
        let version_name = string_arg(&mut env, version)?;
        let client_tag = build_client_tag(&client_name, &version_name);
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let base = current_user(&token).await?;
                let client = match client_tag {
                    Some(tag) => base.with_client_tag(tag),
                    None => base,
                };
                client
                    .chatroom()
                    .send(text)
                    .await
                    .map_err(|err| format!("发送失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendChatRoomMessage(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    content: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let text = string_arg(&mut env, content)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = with_stored_tag(current_user(&token).await?);
                client
                    .chatroom()
                    .send(text)
                    .await
                    .map_err(|err| format!("发送失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_setChatRoomDiscuss(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    discuss: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let text = string_arg(&mut env, discuss)?;
        if text.trim().is_empty() {
            return Err("话题不能为空".to_string());
        }
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = with_stored_tag(current_user(&token).await?);
                client
                    .chatroom()
                    .set_discuss(text)
                    .await
                    .map_err(|err| format!("设置话题失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendChatRoomBarrager(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    content: JString,
    color: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let text = string_arg(&mut env, content)?;
        let barrage_color = string_arg(&mut env, color)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = with_stored_tag(current_user(&token).await?);
                let message = client
                    .chatroom()
                    .barrager(text, Some(barrage_color.as_str()))
                    .await
                    .map_err(|err| format!("弹幕发送失败: {err}"))?;
                Ok(json!({ "message": message }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getChatRoomBarragerCost(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let cost = client
                    .chatroom()
                    .barrage_cost()
                    .await
                    .map_err(|err| format!("获取弹幕花费失败: {err}"))?;
                Ok(json!({
                    "cost": cost.cost,
                    "unit": cost.unit,
                    "label": format!("{}{}", cost.cost, cost.unit),
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_revokeChatRoomMessage(
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
                let revoked = client
                    .chatroom()
                    .revoke(id)
                    .await
                    .map_err(|err| format!("撤回失败: {err}"))?;
                Ok(json!({ "msg": revoked }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_reactChatRoomMessage(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    message_id: JString,
    value: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, message_id)?;
        let reaction_value = string_arg(&mut env, value)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = current_user(&token).await?;
                let result = client
                    .reaction()
                    .chat_room(id, reaction_value)
                    .await
                    .map_err(|err| format!("贴表情失败: {err}"))?;
                Ok(json!({
                    "targetId": result.target_id,
                    "targetType": result.target_type,
                    "groupType": result.group_type,
                    "currentUserReaction": result.current_user_reaction,
                    "summary": reaction_summary_to_json(&result.summary),
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_connectChatRoom(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    self_username: JString,
    callback: JObject,
) -> jlong {
    let result: Result<jlong, String> = (|| {
        let token = string_arg(&mut env, api_key)?;
        let username = string_arg(&mut env, self_username)?;
        let java_vm = Arc::new(env.get_java_vm().map_err(|err| err.to_string())?);
        let callback = env
            .new_global_ref(&callback)
            .map_err(|err| err.to_string())?;
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|err| format!("Rust runtime init failed: {err}"))?;

        emit_callback(
            &java_vm,
            &callback,
            json!({ "event": "status", "message": "正在连接聊天室..." }),
        );

        let connection = runtime.block_on(async {
            let client = current_user(&token).await?;
            let mut connection = client
                .chatroom()
                .connect()
                .await
                .map_err(|err| format!("聊天室连接失败: {err}"))?;

            let java_vm_for_log = Arc::clone(&java_vm);
            let callback_for_log = callback.clone();
            connection.set_log_hook(move |message| {
                emit_callback(
                    &java_vm_for_log,
                    &callback_for_log,
                    json!({ "event": "status", "message": message }),
                );
            });

            connection.on_discuss(|_| {}).await;

            let java_vm_for_event = Arc::clone(&java_vm);
            let callback_for_event = callback.clone();
            let username_for_event = username.clone();
            connection
                .on_all(move |event| {
                    emit_callback(
                        &java_vm_for_event,
                        &callback_for_event,
                        chatroom_event_to_json(event, &username_for_event),
                    );
                })
                .await;

            if let Some(node_name) = connection.node_name() {
                emit_callback(
                    &java_vm,
                    &callback,
                    json!({ "event": "node", "name": node_name }),
                );
            }
            Ok::<ChatRoomConnection, String>(connection)
        })?;

        let handle = Box::new(AndroidChatRoomConnection {
            _runtime: runtime,
            connection: Mutex::new(connection),
            java_vm: Arc::clone(&java_vm),
            callback: callback.clone(),
        });
        emit_callback(
            &java_vm,
            &callback,
            json!({ "event": "status", "message": "聊天室已连接" }),
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
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_disconnectChatRoom(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }

    let handle = unsafe { Box::from_raw(handle as *mut AndroidChatRoomConnection) };
    if let Ok(mut connection) = handle.connection.lock() {
        connection.disconnect();
    }
    drop(handle);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_reconnectChatRoom(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    if handle == 0 {
        return 0;
    }

    let handle = unsafe { &*(handle as *mut AndroidChatRoomConnection) };
    let result = (|| {
        let mut connection = handle
            .connection
            .lock()
            .map_err(|_| "聊天室连接锁已损坏".to_string())?;
        handle
            ._runtime
            .block_on(async { connection.reconnect().await })
            .map_err(|err| format!("聊天室重连失败: {err}"))?;
        emit_callback(
            &handle.java_vm,
            &handle.callback,
            json!({ "event": "status", "message": "聊天室已重连" }),
        );
        Ok::<(), String>(())
    })();

    if result.is_ok() { 1 } else { 0 }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_pauseChatRoomEvents(
    _env: JNIEnv,
    _class: JClass,
    _handle: jlong,
) {
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_resumeChatRoomEvents(
    _env: JNIEnv,
    _class: JClass,
    _handle: jlong,
) {
}
