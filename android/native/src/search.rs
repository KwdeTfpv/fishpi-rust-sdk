use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use fishpi_sdk::FishPi;
use fishpi_sdk::domain::user::AtUser;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::json;

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_searchAtUsers(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
) -> jstring {
    let result = (|| {
        let name = string_arg(&mut env, query)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let client = FishPi::builder()
                    .build()
                    .map_err(|err| format!("native client init failed: {err}"))?;
                let users = client
                    .user()
                    .names(name)
                    .await
                    .map_err(|err| format!("加载@补全失败: {err}"))?;
                let candidates = users
                    .into_iter()
                    .filter(|user| !user.username.as_str().trim().is_empty())
                    .fold(Vec::<AtUser>::new(), |mut acc, user| {
                        let user_name = user.username.as_str().trim();
                        if !acc
                            .iter()
                            .any(|x| x.username.as_str().trim().eq_ignore_ascii_case(user_name))
                        {
                            acc.push(user);
                        }
                        acc
                    })
                    .into_iter()
                    .map(|user| {
                        json!({
                            "userName": user.username.as_str().trim(),
                            "userAvatarURL": user.avatar,
                        })
                    })
                    .collect::<Vec<_>>();
                Ok(json!(candidates))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
