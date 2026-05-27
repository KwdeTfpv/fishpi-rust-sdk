use crate::jni_utils::{string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use fishpi_sdk::api::emoji::Emoji;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::{Value, json};
use std::time::Duration;
use tokio::time::timeout;

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getEmojiGroups(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let emoji = Emoji::new(token);
                let groups = timeout(Duration::from_secs(15), emoji.groups())
                    .await
                    .map_err(|_| "加载表情包分组超时".to_string())?
                    .map_err(|err| format!("加载表情包分组失败: {err}"))?;
                Ok(Value::Array(
                    groups.into_iter().map(emoji_group_to_json).collect(),
                ))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getEmojiGroupItems(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    group_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, group_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let emoji = Emoji::new(token);
                let items = timeout(Duration::from_secs(15), emoji.group_emojis(&id))
                    .await
                    .map_err(|_| "加载表情包超时".to_string())?
                    .map_err(|err| format!("加载表情包失败: {err}"))?;
                Ok(Value::Array(
                    items.into_iter().map(emoji_item_to_json).collect(),
                ))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
