use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::{runtime_json, shared_runtime};
use crate::session::current_user;
use fishpi_sdk::client::NoticeConnection;
use fishpi_sdk::domain::notice::NoticeType;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jlong, jstring};
use serde_json::{Value, json};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::time::timeout;

struct AndroidNoticeConnection {
    _runtime: tokio::runtime::Runtime,
    connection: Mutex<NoticeConnection>,
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getNoticeUnreadCount(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let notice = user.notice();
                let count = timeout(Duration::from_secs(12), notice.count())
                    .await
                    .map_err(|_| "获取通知未读数超时".to_string())?
                    .map_err(|err| format!("获取通知未读数失败: {err}"))?;
                Ok(json!({
                        "total": count.count,
                        "reply": count.reply,
                        "point": count.point,
                        "at": count.at,
                        "broadcast": count.broadcast,
                "system": count.sys_announce,
                        "following": count.following,
                        "commented": count.commented,
                "newFollower": count.new_follower,
                    }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getNotices(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let rt = shared_runtime()?;
        let data = rt.block_on(async {
            let user = current_user(&token).await?;
            let notice = user.notice();
            let mut items = Vec::new();
            for ty in [
                NoticeType::Reply,
                NoticeType::Commented,
                NoticeType::At,
                NoticeType::Following,
                NoticeType::Point,
                NoticeType::System,
                NoticeType::Broadcast,
            ] {
                let list = timeout(Duration::from_secs(10), notice.list(ty)).await;
                if let Ok(Ok(list)) = list {
                    for item in list {
                        if let Some(view) = notice_item_to_json(item) {
                            items.push(view);
                        }
                    }
                }
            }
            items.sort_by_key(|item| {
                std::cmp::Reverse(notice_time_sort_key(item["time"].as_str().unwrap_or("")))
            });
            Ok::<Value, String>(Value::Array(items))
        })?;
        Ok::<String, String>(json!({ "ok": true, "data": data }).to_string())
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_markAllNoticesRead(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let notice = user.notice();
                timeout(Duration::from_secs(12), notice.read_all())
                    .await
                    .map_err(|_| "通知全部已读超时".to_string())?
                    .map_err(|err| format!("通知全部已读失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_connectNotice(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    callback: JObject,
) -> jlong {
    let result: Result<jlong, String> = (|| {
        let token = string_arg(&mut env, api_key)?;
        let java_vm = Arc::new(env.get_java_vm().map_err(|err| err.to_string())?);
        let callback = env
            .new_global_ref(&callback)
            .map_err(|err| err.to_string())?;
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|err| format!("Rust runtime init failed: {err}"))?;

        let connection = runtime.block_on(async {
            let user = current_user(&token).await?;
            let connection = user
                .notice()
                .connect()
                .await
                .map_err(|err| format!("通知连接失败: {err}"))?;
            let vm = Arc::clone(&java_vm);
            let cb = callback.clone();
            connection
                .on_all(move |msg| {
                    emit_callback(
                        &vm,
                        &cb,
                        json!({
                            "event": "notice",
                            "command": msg.command,
                            "content": msg.content.unwrap_or_default(),
                            "who": msg.who.unwrap_or_default(),
                        }),
                    );
                })
                .await;
            Ok::<NoticeConnection, String>(connection)
        })?;

        let handle = Box::new(AndroidNoticeConnection {
            _runtime: runtime,
            connection: Mutex::new(connection),
        });
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
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_disconnectNotice(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }

    let connection = unsafe { Box::from_raw(handle as *mut AndroidNoticeConnection) };
    if let Ok(mut conn) = connection.connection.lock() {
        conn.disconnect();
    }
    drop(connection);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_reconnectNotice(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    if handle == 0 {
        return 0;
    }
    let connection = unsafe { &*(handle as *mut AndroidNoticeConnection) };
    let result = (|| {
        let mut conn = connection
            .connection
            .lock()
            .map_err(|_| "通知连接锁已损坏".to_string())?;
        connection
            ._runtime
            .block_on(async { conn.reconnect().await })
            .map_err(|err| format!("通知重连失败: {err}"))?;
        Ok::<(), String>(())
    })();
    if result.is_ok() { 1 } else { 0 }
}
