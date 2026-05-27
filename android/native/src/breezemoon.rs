use crate::jni_utils::{string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::{Value, json};
use std::time::Duration;
use tokio::time::timeout;

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getBreezemoons(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    page: i32,
    size: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let p = page.max(1) as u32;
        let s = size.max(1) as u32;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let breezemoon = &user.breezemoon;
                let list = timeout(Duration::from_secs(15), breezemoon.list(p, s, None))
                    .await
                    .map_err(|_| "加载清风明月超时".to_string())?
                    .map_err(|err| format!("加载清风明月失败: {err}"))?;
                Ok(Value::Array(
                    list.into_iter().map(breezemoon_to_json).collect(),
                ))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserBreezemoons(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    user_name: JString,
    page: i32,
    size: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let user = string_arg(&mut env, user_name)?;
        let p = page.max(1) as u32;
        let s = size.max(1) as u32;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user_client = current_user(&token).await?;
                let breezemoon = &user_client.breezemoon;
                let list = timeout(Duration::from_secs(15), breezemoon.list(p, s, Some(&user)))
                    .await
                    .map_err(|_| "加载用户清风明月超时".to_string())?
                    .map_err(|err| format!("加载用户清风明月失败: {err}"))?;
                Ok(Value::Array(
                    list.into_iter().map(breezemoon_to_json).collect(),
                ))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendBreezemoon(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    content: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let text = string_arg(&mut env, content)?;
        if text.trim().is_empty() {
            return Err("清风明月内容不能为空".to_string());
        }
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let breezemoon = &user.breezemoon;
                let rsp = timeout(Duration::from_secs(15), breezemoon.send(text.trim()))
                    .await
                    .map_err(|_| "发送清风明月超时".to_string())?
                    .map_err(|err| format!("发送清风明月失败: {err}"))?;
                if !rsp.success {
                    return Err(if rsp.msg.is_empty() {
                        "发送清风明月失败".to_string()
                    } else {
                        rsp.msg
                    });
                }
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
