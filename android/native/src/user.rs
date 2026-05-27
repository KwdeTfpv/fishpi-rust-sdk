use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use crate::session::{current_user, set_current_user};
use fishpi_sdk::FishPi;
use fishpi_sdk::model::misc::LoginData;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::{Value, json};
use std::time::Duration;
use tokio::time::timeout;

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_login(
    mut env: JNIEnv,
    _class: JClass,
    name_or_email: JString,
    password: JString,
    mfa_code: JString,
) -> jstring {
    let result = (|| {
        let name = string_arg(&mut env, name_or_email)?;
        let pass = string_arg(&mut env, password)?;
        let mfa = string_arg(&mut env, mfa_code)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let login = LoginData::new(
                    &name,
                    &pass,
                    (!mfa.trim().is_empty()).then_some(mfa.trim().to_string()),
                );
                let user = FishPi::login(&login)
                    .await
                    .map_err(|err| format!("登录失败: {err}"))?;
                let info = user
                    .info()
                    .await
                    .map_err(|err| format!("读取用户信息失败: {err}"))?;
                let api_key = user.get_token().to_string();
                let user_name = info.username().to_string();
                let user_nickname = info.nickname().to_string();
                let user_avatar_url = info.avatar().to_string();
                let role = info.role().to_string();
                let user_no = info.user_no().to_string();
                let card_bg = info.card_bg().to_string();
                set_current_user(user).await?;
                Ok(json!({
                    "apiKey": api_key,
                    "userName": user_name,
                    "userNickname": user_nickname,
                    "userAvatarURL": user_avatar_url,
                    "role": role,
                    "userNo": user_no,
                    "cardBg": card_bg,
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUser(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let info = user
                    .info()
                    .await
                    .map_err(|err| format!("读取用户信息失败: {err}"))?;
                Ok(json!({
                    "userId": info.oid(),
                    "userName": info.username(),
                    "userNickname": info.nickname(),
                    "userAvatarURL": info.avatar(),
                    "cardBg": info.card_bg(),
                    "role": info.role(),
                    "userNo": info.user_no(),
                    "intro": info.intro(),
                    "city": info.city(),
                    "url": info.url(),
                    "points": info.points(),
                    "following": info.following(),
                    "follower": info.follower(),
                    "onlineMinutes": info.online_minutes(),
                    "canFollow": info.can_follow(),
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserProfile(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    username: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let target = string_arg(&mut env, username)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let info = user
                    .get_user(target.trim())
                    .await
                    .map_err(|err| format!("读取用户资料失败: {err}"))?;
                Ok(json!({
                    "userId": info.oid(),
                    "userName": info.username(),
                    "userNickname": info.nickname(),
                    "userAvatarURL": info.avatar(),
                    "cardBg": info.card_bg(),
                    "role": info.role(),
                    "userNo": info.user_no(),
                    "intro": info.intro(),
                    "city": info.city(),
                    "url": info.url(),
                    "points": info.points(),
                    "following": info.following(),
                    "follower": info.follower(),
                    "onlineMinutes": info.online_minutes(),
                    "canFollow": info.can_follow(),
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_followUser(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    user_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, user_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                user.follow(id.trim())
                    .await
                    .map_err(|err| format!("关注用户失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_unfollowUser(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    user_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, user_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                user.unfollow(id.trim())
                    .await
                    .map_err(|err| format!("取消关注用户失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_transferPoint(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    username: JString,
    amount: i32,
    memo: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let target = string_arg(&mut env, username)?;
        let note = string_arg(&mut env, memo)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                user.transfer(target.trim(), amount.max(0) as u32, note.trim())
                    .await
                    .map_err(|err| format!("转账失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserPoints(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    username: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let target = string_arg(&mut env, username)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let points = user
                    .get_points(target.trim())
                    .await
                    .map_err(|err| format!("查询积分余额失败: {err}"))?;
                Ok(json!({
                    "point": points.point,
                    "userName": points.name,
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserActivity(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let liveness = user
                    .liveness()
                    .await
                    .map_err(|err| format!("读取活跃度失败: {err}"))?;
                let checked_in = user
                    .is_checkin()
                    .await
                    .map_err(|err| format!("读取签到状态失败: {err}"))?;
                let liveness_rewarded = user
                    .is_collected_liveness()
                    .await
                    .map_err(|err| format!("读取昨日活跃奖励状态失败: {err}"))?;
                Ok(json!({
                    "liveness": liveness,
                    "checkedIn": checked_in,
                    "livenessRewarded": liveness_rewarded,
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserDailyState(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let checked_in = user
                    .is_checkin()
                    .await
                    .map_err(|err| format!("读取签到状态失败: {err}"))?;
                let liveness_rewarded = user
                    .is_collected_liveness()
                    .await
                    .map_err(|err| format!("读取昨日活跃奖励状态失败: {err}"))?;
                Ok(json!({
                    "checkedIn": checked_in,
                    "livenessRewarded": liveness_rewarded,
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_rewardLiveness(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let sum = user
                    .reward_liveness()
                    .await
                    .map_err(|err| format!("领取昨日活跃奖励失败: {err}"))?;
                Ok(json!({ "sum": sum }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserMedals(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    user_name: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let username = string_arg(&mut env, user_name)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let medals = timeout(Duration::from_secs(15), user.medals(username.trim()))
                    .await
                    .map_err(|_| "获取勋章列表超时".to_string())?
                    .map_err(|err| format!("获取勋章列表失败: {err}"))?;
                Ok(medals)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
