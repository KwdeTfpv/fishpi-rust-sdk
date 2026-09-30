use crate::jni_utils::{string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jstring};
use serde_json::{Value, json};
use std::time::Duration;
use tokio::time::timeout;

const EMOJI_TIMEOUT: Duration = Duration::from_secs(15);

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
                let user = current_user(&token).await?;
                let emoji = user.emoji();
                let groups = timeout(EMOJI_TIMEOUT, emoji.groups())
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
                let user = current_user(&token).await?;
                let emoji = user.emoji();
                let items = timeout(EMOJI_TIMEOUT, emoji.group_items(id.as_str()))
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

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_uploadEmojiUrl(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    url: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let url = string_arg(&mut env, url)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                timeout(EMOJI_TIMEOUT, user.emoji().upload_url(url))
                    .await
                    .map_err(|_| "收藏表情超时".to_string())?
                    .map_err(|err| format!("收藏表情失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_createEmojiGroup(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    name: JString,
    sort: jlong,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let name = string_arg(&mut env, name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                timeout(EMOJI_TIMEOUT, user.emoji().create_group(name, sort))
                    .await
                    .map_err(|_| "创建分组超时".to_string())?
                    .map_err(|err| format!("创建分组失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_updateEmojiGroup(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    group_id: JString,
    name: JString,
    sort: jlong,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let group_id = string_arg(&mut env, group_id)?;
        let name = string_arg(&mut env, name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                timeout(
                    EMOJI_TIMEOUT,
                    user.emoji().update_group(group_id, name, sort),
                )
                .await
                .map_err(|_| "更新分组超时".to_string())?
                .map_err(|err| format!("更新分组失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_deleteEmojiGroup(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    group_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let group_id = string_arg(&mut env, group_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                timeout(EMOJI_TIMEOUT, user.emoji().delete_group(group_id))
                    .await
                    .map_err(|_| "删除分组超时".to_string())?
                    .map_err(|err| format!("删除分组失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_addEmojiToGroup(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    group_id: JString,
    emoji_id: JString,
    sort: jlong,
    name: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let group_id = string_arg(&mut env, group_id)?;
        let emoji_id = string_arg(&mut env, emoji_id)?;
        let name = string_arg(&mut env, name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let label = name.trim();
                timeout(
                    EMOJI_TIMEOUT,
                    user.emoji().add(
                        group_id,
                        emoji_id,
                        sort,
                        if label.is_empty() { None } else { Some(label) },
                    ),
                )
                .await
                .map_err(|_| "添加表情超时".to_string())?
                .map_err(|err| format!("添加表情失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_addEmojiUrlToGroup(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    group_id: JString,
    url: JString,
    sort: jlong,
    name: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let group_id = string_arg(&mut env, group_id)?;
        let url = string_arg(&mut env, url)?;
        let name = string_arg(&mut env, name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let label = name.trim();
                timeout(
                    EMOJI_TIMEOUT,
                    user.emoji().add_url(
                        group_id,
                        url,
                        sort,
                        if label.is_empty() { None } else { Some(label) },
                    ),
                )
                .await
                .map_err(|_| "添加表情超时".to_string())?
                .map_err(|err| format!("添加表情失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_removeEmojiFromGroup(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    group_id: JString,
    emoji_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let group_id = string_arg(&mut env, group_id)?;
        let emoji_id = string_arg(&mut env, emoji_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                timeout(EMOJI_TIMEOUT, user.emoji().remove(group_id, emoji_id))
                    .await
                    .map_err(|_| "移除表情超时".to_string())?
                    .map_err(|err| format!("移除表情失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_updateEmojiItem(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    o_id: JString,
    group_id: JString,
    name: JString,
    sort: jlong,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let o_id = string_arg(&mut env, o_id)?;
        let group_id = string_arg(&mut env, group_id)?;
        let name = string_arg(&mut env, name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                timeout(
                    EMOJI_TIMEOUT,
                    user.emoji().update(o_id, group_id, name, sort),
                )
                .await
                .map_err(|_| "更新表情超时".to_string())?
                .map_err(|err| format!("更新表情失败: {err}"))?;
                Ok(Value::Bool(true))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
