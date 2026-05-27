use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::api::chatroom::ChatRoom;
use fishpi_sdk::api::ws::build_ws_url;
use fishpi_sdk::model::chatroom::{ChatContentType, ChatRoomMessageType, ChatRoomMsg, ClientType};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jlong, jstring};
use serde_json::{Value, json};
use std::str::FromStr;
use std::sync::OnceLock;
use std::sync::{Arc, Mutex};

static CHATROOM_CLIENT_CONFIG: OnceLock<Mutex<Option<(ClientType, String)>>> = OnceLock::new();

fn chatroom_client_config() -> &'static Mutex<Option<(ClientType, String)>> {
    CHATROOM_CLIENT_CONFIG.get_or_init(|| Mutex::new(None))
}

fn apply_chatroom_client_config(chatroom: &mut ChatRoom) {
    let Ok(guard) = chatroom_client_config().lock() else {
        return;
    };
    if let Some((client, version)) = guard.as_ref() {
        chatroom.set_client_type(client.clone(), Some(version.clone()));
    }
}

fn parse_chatroom_client_type(client_name: &str) -> Result<ClientType, String> {
    let normalized_client = client_name.trim();
    if normalized_client.is_empty() {
        return Err("client is required".to_string());
    }
    ClientType::from_str(normalized_client)
        .map_err(|_| format!("Unknown client type: {normalized_client}"))
}

fn normalize_chatroom_client_version(version_name: &str) -> Result<String, String> {
    let normalized_version = version_name.trim();
    if normalized_version.is_empty() {
        return Err("version is required".to_string());
    }
    Ok(normalized_version.to_string())
}

struct AndroidChatRoomConnection {
    _runtime: tokio::runtime::Runtime,
    chatroom: Mutex<ChatRoom>,
    java_vm: Arc<jni::JavaVM>,
    callback: jni::objects::GlobalRef,
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
                let user = current_user(&token).await?;
                let mut history = user
                    .chatroom
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
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getChatRoomWsUrl(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let chatroom = &user.chatroom;
                let url = chatroom.get_ws_url().await.or_else(|_| {
                    build_ws_url("fishpi.cn", "chat-room-channel", &[("apiKey", token)])
                });
                url.map(Value::String)
                    .map_err(|err| format!("获取聊天室节点失败: {err}"))
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
    let result = (|| {
        let client_name = string_arg(&mut env, client)?;
        let version_name = string_arg(&mut env, version)?;
        let client_type = parse_chatroom_client_type(&client_name)?;
        let normalized_version = normalize_chatroom_client_version(&version_name)?;
        if let Ok(mut guard) = chatroom_client_config().lock() {
            *guard = Some((client_type, normalized_version));
        }
        Ok::<(), String>(())
    })();
    let _ = result;
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
        let client_type = parse_chatroom_client_type(&client_name)?;
        let normalized_version = normalize_chatroom_client_version(&version_name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let mut chatroom = user.chatroom;
                chatroom.set_client_type(client_type, Some(normalized_version));
                chatroom
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
                let user = current_user(&token).await?;
                let mut chatroom = user.chatroom;
                apply_chatroom_client_config(&mut chatroom);
                chatroom
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
                let user = current_user(&token).await?;
                let chatroom = user.chatroom;
                chatroom.set_discuss(text).await;
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
                let user = current_user(&token).await?;
                let mut chatroom = user.chatroom;
                apply_chatroom_client_config(&mut chatroom);
                let message = chatroom
                    .barrager(text, Some(barrage_color))
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
                let user = current_user(&token).await?;
                let chatroom = user.chatroom;
                let cost = chatroom
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
                let user = current_user(&token).await?;
                let chatroom = &user.chatroom;
                let revoked = chatroom
                    .revoke(&id)
                    .await
                    .map_err(|err| format!("撤回失败: {err}"))?;
                Ok(json!({ "msg": revoked.msg }))
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
                let user = current_user(&token).await?;
                let chatroom = &user.chatroom;
                let result = chatroom
                    .reaction(&id, &reaction_value)
                    .await
                    .map_err(|err| format!("贴表情失败: {err}"))?;
                Ok(json!({
                    "targetId": result.targetId,
                    "targetType": result.targetType,
                    "groupType": result.groupType,
                    "currentUserReaction": result.currentUserReaction,
                    "summary": result.summary,
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

        let chatroom = runtime.block_on(async {
            let user = current_user(&token).await?;
            let mut chatroom = user.chatroom;
            let java_vm_for_log = Arc::clone(&java_vm);
            let callback_for_log = callback.clone();
            chatroom.on_ws_log(move |message| {
                emit_callback(
                    &java_vm_for_log,
                    &callback_for_log,
                    json!({ "event": "status", "message": message }),
                );
            });

            // Keep the SDK's internal discuss cache in sync. Android UI events are still
            // emitted through on_all below, so this listener intentionally does not emit.
            chatroom.on_discuss(|_| {}).await;

            let java_vm_for_event = Arc::clone(&java_vm);
            let callback_for_event = callback.clone();
            let username_for_event = username.clone();
            chatroom
                .on_all(move |event| {
                    emit_callback(
                        &java_vm_for_event,
                        &callback_for_event,
                        chatroom_event_to_json(event, &username_for_event),
                    );
                })
                .await;

            chatroom
                .connect(false)
                .await
                .map_err(|err| format!("聊天室连接失败: {err}"))?;
            if let Some(node_name) = chatroom.current_node_name().await {
                emit_callback(
                    &java_vm,
                    &callback,
                    json!({ "event": "node", "name": node_name }),
                );
            }
            Ok::<ChatRoom, String>(chatroom)
        })?;

        // Keep the Rust runtime alive; the SDK's WS tasks run on it after connect returns.
        let handle = Box::new(AndroidChatRoomConnection {
            _runtime: runtime,
            chatroom: Mutex::new(chatroom),
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

    let connection = unsafe { Box::from_raw(handle as *mut AndroidChatRoomConnection) };
    if let Ok(mut chatroom) = connection.chatroom.lock() {
        chatroom.disconnect();
    }
    drop(connection);
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

    let connection = unsafe { &*(handle as *mut AndroidChatRoomConnection) };
    let result = (|| {
        let mut chatroom = connection
            .chatroom
            .lock()
            .map_err(|_| "聊天室连接锁已损坏".to_string())?;
        connection
            ._runtime
            .block_on(async {
                chatroom.reconnect().await?;
                Ok::<Option<String>, fishpi_sdk::api::ws::WebSocketError>(
                    chatroom.current_node_name().await,
                )
            })
            .map(|node_name| {
                if let Some(node_name) = node_name {
                    emit_callback(
                        &connection.java_vm,
                        &connection.callback,
                        json!({ "event": "node", "name": node_name }),
                    );
                }
            })
            .map_err(|err| format!("聊天室重连失败: {err}"))?;
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

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_parseChatRoomWsMessage(
    mut env: JNIEnv,
    _class: JClass,
    text: JString,
    self_username: JString,
) -> jstring {
    let result = (|| {
        let raw = string_arg(&mut env, text)?;
        let username = string_arg(&mut env, self_username)?;
        let json_value: Value = serde_json::from_str(&raw).map_err(|err| err.to_string())?;
        let type_str = json_value["type"].as_str().unwrap_or("");
        let event = ChatRoomMessageType::from_str(type_str)
            .map_err(|_| format!("Unknown message type: {type_str}"))?;

        let data = match event {
            ChatRoomMessageType::Online => {
                let users = json_value["users"]
                    .as_array()
                    .map(|items| {
                        items
                            .iter()
                            .filter_map(|u| {
                                u["userName"]
                                    .as_str()
                                    .map(|name| Value::String(name.to_string()))
                            })
                            .collect::<Vec<_>>()
                    })
                    .unwrap_or_default();
                json!({
                    "event": "online",
                    "users": users,
                    "discussing": json_value["discussing"].as_str().unwrap_or(""),
                    "onlineCount": json_value["onlineChatCnt"]
                        .as_u64()
                        .or_else(|| json_value["onlineCnt"].as_u64())
                        .unwrap_or(users.len() as u64),
                })
            }
            ChatRoomMessageType::DiscussChanged => json!({
                "event": "discussChanged",
                "topic": json_value["newDiscuss"].as_str().unwrap_or(""),
            }),
            ChatRoomMessageType::Revoke => json!({
                "event": "revoke",
                "id": json_value["oId"].as_str().unwrap_or(""),
            }),
            ChatRoomMessageType::Msg
            | ChatRoomMessageType::RedPacket => {
                let msg = ChatRoomMsg::from_value(&json_value).map_err(|err| err.to_string())?;
                json!({
                    "event": "message",
                    "message": chat_message_to_json(msg, &username),
                })
            }
            ChatRoomMessageType::Barrager => {
                let msg = fishpi_sdk::model::chatroom::BarragerMsg::from_value(&json_value)
                    .map_err(|err| err.to_string())?;
                json!({
                    "event": "message",
                    "message": barrager_to_json(msg),
                })
            }
            ChatRoomMessageType::RedPacketStatus => json!({
                "event": "redPacketStatus",
                "id": json_value["oId"].as_str().unwrap_or(""),
                "count": json_value["count"].as_u64().unwrap_or(0),
                "got": json_value["got"].as_u64().unwrap_or(0),
                "whoGive": json_value["whoGive"].as_str().unwrap_or(""),
            }),
            ChatRoomMessageType::Custom => json!({
                "event": "custom",
                "message": json_value["message"].as_str().unwrap_or(""),
            }),
            ChatRoomMessageType::ChatReaction => json!({
                "event": "chatReaction",
                "id": json_value["oId"].as_str().unwrap_or(""),
                "reactionSummary": json_value["summary"].clone(),
                "actorReaction": json_value["actorReaction"].as_str().unwrap_or(""),
                "actorUserId": json_value["actorUserId"].as_str().unwrap_or(""),
                "targetType": json_value["targetType"].as_str().unwrap_or(""),
                "groupType": json_value["groupType"].as_str().unwrap_or(""),
                "data": json_value,
            }),
        };

        Ok(json!({ "ok": true, "data": data }).to_string())
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
