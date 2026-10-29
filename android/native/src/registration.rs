use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use base64::Engine;
use fishpi_sdk::FishPi;
use fishpi_sdk::model::misc::{PreRegisterInfo, RegisterInfo};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::json;
use std::sync::{Mutex, OnceLock};

const DEFAULT_INVITER: &str = "Kirito";

static REGISTER_CLIENT: OnceLock<Mutex<Option<FishPi>>> = OnceLock::new();

fn register_client() -> &'static Mutex<Option<FishPi>> {
    REGISTER_CLIENT.get_or_init(|| Mutex::new(None))
}

fn cached_client() -> Result<FishPi, String> {
    let mut guard = register_client()
        .lock()
        .map_err(|_| "注册会话已损坏".to_string())?;
    if guard.is_none() {
        *guard = Some(FishPi::builder().build().map_err(|err| err.to_string())?);
    }
    guard.clone().ok_or_else(|| "注册会话初始化失败".to_string())
}

fn store_client(client: FishPi) -> Result<(), String> {
    let mut guard = register_client()
        .lock()
        .map_err(|_| "注册会话已损坏".to_string())?;
    *guard = Some(client);
    Ok(())
}

fn clear_client() -> Result<(), String> {
    let mut guard = register_client()
        .lock()
        .map_err(|_| "注册会话已损坏".to_string())?;
    *guard = None;
    Ok(())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_fetchRegisterCaptcha(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let result = (|| {
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = FishPi::builder().build().map_err(|err| err.to_string())?;
                let bytes = client
                    .captcha()
                    .await
                    .map_err(|err| format!("获取图形验证码失败: {err}"))?;
                store_client(client)?;
                Ok(json!({
                    "imageBase64": base64::engine::general_purpose::STANDARD.encode(bytes),
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_preRegister(
    mut env: JNIEnv,
    _class: JClass,
    username: JString,
    phone: JString,
    captcha: JString,
) -> jstring {
    let result = (|| {
        let username = string_arg(&mut env, username)?;
        let phone = string_arg(&mut env, phone)?;
        let captcha = string_arg(&mut env, captcha)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = cached_client()?;
                let data = PreRegisterInfo {
                    username: username.trim().to_string(),
                    phone: phone.trim().to_string(),
                    invitecode: Some(String::new()),
                    captcha: captcha.trim().to_string(),
                };
                client
                    .pre_register_with_referral(&data, Some(DEFAULT_INVITER))
                    .await
                    .map_err(|err| format!("发送短信验证码失败: {err}"))?;
                Ok(json!({ "sent": true }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_verifyRegisterCode(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
) -> jstring {
    let result = (|| {
        let code = string_arg(&mut env, code)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = cached_client()?;
                let user_id = client
                    .verify(code.trim())
                    .await
                    .map_err(|err| format!("短信验证码校验失败: {err}"))?;
                if user_id.trim().is_empty() {
                    return Err("短信验证码校验失败: 服务端未返回用户 Id".to_string());
                }
                Ok(json!({ "userId": user_id }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_registerAccount(
    mut env: JNIEnv,
    _class: JClass,
    user_id: JString,
    password: JString,
    role: JString,
) -> jstring {
    let result = (|| {
        let user_id = string_arg(&mut env, user_id)?;
        let password = string_arg(&mut env, password)?;
        let role = string_arg(&mut env, role)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = cached_client()?;
                let data = RegisterInfo {
                    role: role.trim().to_string(),
                    passwd: password,
                    user_id: user_id.trim().to_string(),
                    r: Some(DEFAULT_INVITER.to_string()),
                };
                client
                    .register(&data)
                    .await
                    .map_err(|err| format!("注册失败: {err}"))?;
                clear_client()?;
                Ok(json!({ "registered": true }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_resetRegisterSession(
    _env: JNIEnv,
    _class: JClass,
) {
    let _ = clear_client();
}
