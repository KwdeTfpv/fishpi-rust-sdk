use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use fishpi_sdk::FishPi;
use fishpi_sdk::model::user::AtUser;
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
                let users = FishPi::names(&name)
                    .await
                    .map_err(|err| format!("加载@补全失败: {err}"))?;
                let candidates = users
                    .into_iter()
                    .filter(|user| !user.user_name.trim().is_empty())
                    .fold(Vec::<AtUser>::new(), |mut acc, user| {
                        let user_name = user.user_name.trim();
                        if !acc.iter().any(|x| x.user_name.trim().eq_ignore_ascii_case(user_name)) {
                            acc.push(user);
                        }
                        acc
                    })
                    .into_iter()
                    .map(|user| {
                        json!({
                            "userName": user.user_name.trim(),
                            "userAvatarURL": user.user_avatar_url,
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
