use crate::handles::HandleRegistry;
use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::{
    appearance_items_to_json, appearance_purchase_to_json, appearance_shop_to_json,
    chat_appearance_to_json, membership_status_to_json,
};
use crate::runtime::shared_runtime;
use fishpi_sdk::model::misc::LoginData;
use fishpi_sdk::{FishPi, V3ChatEvent, V3ChatEventKind, V3ChatRealtime, V3Cookie, V3Error};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jlong, jstring};
use serde_json::{Value, json};
use std::sync::{Arc, Mutex, OnceLock};

enum Failure {
    V3(V3Error),
    Message(String),
}

impl From<V3Error> for Failure {
    fn from(error: V3Error) -> Self {
        Self::V3(error)
    }
}

impl From<String> for Failure {
    fn from(message: String) -> Self {
        Self::Message(message)
    }
}

impl Failure {
    fn message(&self) -> String {
        match self {
            Self::V3(error) => error.to_string(),
            Self::Message(message) => message.clone(),
        }
    }
}

fn envelope(result: Result<Value, Failure>) -> String {
    match result {
        Ok(data) => json!({ "ok": true, "data": data }).to_string(),
        Err(Failure::V3(error)) => json!({
            "ok": false,
            "error": error.to_string(),
            "v3": {
                "kind": error.kind.as_str(),
                "status": error.status,
                "code": error.code,
                "retryAfter": error.retry_after.map(|duration| duration.as_secs()),
            }
        })
        .to_string(),
        Err(Failure::Message(message)) => json!({
            "ok": false,
            "error": message,
            "v3": { "kind": "transport", "status": null, "code": null, "retryAfter": null },
        })
        .to_string(),
    }
}

fn run_v3<F>(work: F) -> String
where
    F: FnOnce(&tokio::runtime::Runtime) -> Result<Value, Failure>,
{
    match shared_runtime() {
        Ok(runtime) => envelope(work(runtime)),
        Err(error) => envelope(Err(Failure::Message(error))),
    }
}

fn build_client() -> Result<FishPi, Failure> {
    FishPi::builder()
        .build()
        .map_err(|error| Failure::Message(format!("V3 初始化失败: {error}")))
}

fn parse_cookies(json: &str) -> Result<Vec<V3Cookie>, Failure> {
    serde_json::from_str::<Vec<V3Cookie>>(json)
        .map_err(|error| Failure::Message(format!("V3 续期凭据无法解析: {error}")))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_v3Login(
    mut env: JNIEnv,
    _class: JClass,
    name_or_email: JString,
    password: JString,
    mfa_code: JString,
) -> jstring {
    let result = (|| {
        let name = string_arg(&mut env, name_or_email)?;
        let password = string_arg(&mut env, password)?;
        let mfa_code = string_arg(&mut env, mfa_code)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let login = LoginData::new(
                    &name,
                    &password,
                    (!mfa_code.trim().is_empty()).then_some(mfa_code.trim().to_string()),
                );
                let client = build_client()?;
                let session = client.login_v3(&login).await?;
                Ok(json!(session))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_v3CaptchaOptions(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let result = run_v3(|runtime| {
        runtime.block_on(async {
            let client = build_client()?;
            let options = client.v3().await?.captcha_options().await?;
            Ok(json!({
                "provider": options.provider,
                "captchaId": options.captcha_id,
            }))
        })
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_validateCaptcha(
    mut env: JNIEnv,
    _class: JClass,
    captcha_json: JString,
) -> jstring {
    let result = (|| {
        let captcha_json = string_arg(&mut env, captcha_json)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                client.v3().await?.validate_captcha(&captcha_json).await?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_v3Refresh(
    mut env: JNIEnv,
    _class: JClass,
    cookies_json: JString,
) -> jstring {
    let result = (|| {
        let cookies_json = string_arg(&mut env, cookies_json)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let cookies = parse_cookies(&cookies_json)?;
                let client = build_client()?;
                let session = client.refresh_v3(&cookies).await?;
                Ok(json!(session))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_v3Logout(
    mut env: JNIEnv,
    _class: JClass,
    cookies_json: JString,
) -> jstring {
    let result = (|| {
        let cookies_json = string_arg(&mut env, cookies_json)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let cookies = parse_cookies(&cookies_json)?;
                let client = build_client()?;
                client.logout_v3(&cookies).await?;
                Ok(json!({}))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_v3Me(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let me = client.v3_me(&access_token).await?;
                Ok(json!({
                    "userId": me.id,
                    "userName": me.user_name,
                    "userNickname": me.user_nickname,
                }))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getChatAppearance(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let appearance = client.chatroom().get_chat_appearance(&access_token).await?;
                Ok(chat_appearance_to_json(&appearance))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_setChatAppearance(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
    idempotency_key: JString,
    bubble_id: JString,
    frame_id: JString,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        let idempotency_key = string_arg(&mut env, idempotency_key)?;
        let bubble_id = string_arg(&mut env, bubble_id)?;
        let frame_id = string_arg(&mut env, frame_id)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let appearance = client
                    .chatroom()
                    .set_chat_appearance(&access_token, &idempotency_key, &bubble_id, &frame_id)
                    .await?;
                Ok(chat_appearance_to_json(&appearance))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getAppearanceShop(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let token = access_token.trim();
                let token = if token.is_empty() { None } else { Some(token) };
                let shop = client.appearance().shop(token).await?;
                Ok(appearance_shop_to_json(&shop))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getAppearanceItems(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let items = client.appearance().items(&access_token).await?;
                Ok(appearance_items_to_json(&items))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getMembership(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let status = client.appearance().membership(&access_token).await?;
                Ok(membership_status_to_json(&status))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_purchaseAppearanceItem(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
    idempotency_key: JString,
    product_id: JString,
    days: jint,
    revision: jlong,
) -> jstring {
    let result = (|| {
        let access_token = string_arg(&mut env, access_token)?;
        let idempotency_key = string_arg(&mut env, idempotency_key)?;
        let product_id = string_arg(&mut env, product_id)?;
        let days = u32::try_from(days).map_err(|_| "购买期限不合法".to_string())?;
        let revision = u64::try_from(revision).map_err(|_| "商品版本不合法".to_string())?;
        Ok(run_v3(|runtime| {
            runtime.block_on(async {
                let client = build_client()?;
                let purchase = client
                    .appearance()
                    .purchase(&access_token, &idempotency_key, &product_id, days, revision)
                    .await?;
                Ok(appearance_purchase_to_json(&purchase))
            })
        }))
    })()
    .unwrap_or_else(|error: String| {
        json!({ "ok": false, "error": error, "v3": { "kind": "transport" } }).to_string()
    });

    to_jstring(&mut env, result)
}

struct AndroidV3RealtimeConnection {
    _runtime: tokio::runtime::Runtime,
    realtime: Mutex<V3ChatRealtime>,
}

static V3_REALTIME_HANDLES: OnceLock<HandleRegistry<AndroidV3RealtimeConnection>> = OnceLock::new();

fn realtime_handles() -> &'static HandleRegistry<AndroidV3RealtimeConnection> {
    V3_REALTIME_HANDLES.get_or_init(HandleRegistry::new)
}

/// 把 V3 实时事件映射为 App 侧回调 JSON。
fn v3_realtime_event_to_json(event: &V3ChatEvent) -> Value {
    match event.kind {
        V3ChatEventKind::Status => json!({ "event": "status", "message": event.text }),
        V3ChatEventKind::AuthExpired => {
            json!({ "event": "authExpired", "message": event.text })
        }
        V3ChatEventKind::MessageCreated | V3ChatEventKind::MessageUpdated => json!({
            "event": "appearance",
            "messageId": event.message_id,
            "revision": event.revision,
            "appearance": event.appearance.as_ref().map(chat_appearance_to_json),
        }),
        V3ChatEventKind::MessageDeleted => {
            json!({ "event": "appearanceDeleted", "messageId": event.message_id })
        }
        V3ChatEventKind::SyncRequired => json!({ "event": "syncRequired" }),
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_connectV3ChatRealtime(
    mut env: JNIEnv,
    _class: JClass,
    access_token: JString,
    callback: JObject,
) -> jlong {
    let result: Result<jlong, String> = (|| {
        let token = string_arg(&mut env, access_token)?;
        if token.trim().is_empty() {
            return Err("缺少 V3 访问令牌".to_string());
        }
        let java_vm = Arc::new(env.get_java_vm().map_err(|err| err.to_string())?);
        let callback = env
            .new_global_ref(&callback)
            .map_err(|err| err.to_string())?;
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|err| format!("Rust runtime init failed: {err}"))?;

        emit_callback(
            &java_vm,
            &callback,
            json!({ "event": "status", "message": "正在连接 V3 外观订阅..." }),
        );

        let realtime = runtime.block_on(async {
            let client = build_client().map_err(|err| err.message())?;
            let v3 = client.v3().await.map_err(|err| err.to_string())?;
            let realtime = v3
                .connect_chat_realtime(&token)
                .await
                .map_err(|err| err.to_string())?;

            let java_vm_for_event = Arc::clone(&java_vm);
            let callback_for_event = callback.clone();
            realtime
                .on_event(move |event| {
                    emit_callback(
                        &java_vm_for_event,
                        &callback_for_event,
                        v3_realtime_event_to_json(&event),
                    );
                })
                .await;
            Ok::<V3ChatRealtime, String>(realtime)
        })?;

        let handle = AndroidV3RealtimeConnection {
            _runtime: runtime,
            realtime: Mutex::new(realtime),
        };

        Ok(realtime_handles().insert(handle))
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
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_disconnectV3ChatRealtime(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    let Some(handle) = realtime_handles().remove(handle) else {
        return;
    };
    if let Ok(realtime) = handle.realtime.lock() {
        realtime.disconnect();
    }
}
