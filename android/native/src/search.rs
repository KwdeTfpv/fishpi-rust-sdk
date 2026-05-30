use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use fishpi_sdk::FishPi;
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
                let names = users
                    .into_iter()
                    .map(|user| user.user_name.trim().to_string())
                    .filter(|name| !name.is_empty())
                    .fold(Vec::<String>::new(), |mut acc, name| {
                        if !acc.iter().any(|x| x.eq_ignore_ascii_case(&name)) {
                            acc.push(name);
                        }
                        acc
                    });
                Ok(json!(names))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
