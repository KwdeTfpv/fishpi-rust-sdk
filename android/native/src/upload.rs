use crate::jni_utils::{string_arg, to_jstring};
use crate::runtime::runtime_json;
use crate::session::current_user;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::json;

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_uploadChatFile(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    file_path: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let path = string_arg(&mut env, file_path)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let source_is_video = is_video_filename(&path.to_ascii_lowercase());
                let user = current_user(&token).await?;
                let result = user
                    .upload(vec![path])
                    .await
                    .map_err(|err| format!("上传失败: {err}"))?;
                let file = result.success.first().ok_or_else(|| {
                    if result.errs.is_empty() {
                        "上传失败：未返回可用链接".to_string()
                    } else {
                        format!("上传失败：{}", result.errs.join("、"))
                    }
                })?;
                let lower = file.filename.to_ascii_lowercase();
                let markdown = if source_is_video || is_video_filename(&lower) || is_video_filename(file.url.trim()) {
                    format!("[视频]({})", file.url.trim())
                } else {
                    format!("![图片]({})", file.url.trim())
                };
                Ok(json!({
                    "filename": file.filename,
                    "url": file.url,
                    "markdown": markdown,
                    "type": if source_is_video || is_video_filename(&lower) { "video" } else { "image" },
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

fn is_video_filename(filename: &str) -> bool {
    filename.ends_with(".mp4")
        || filename.ends_with(".webm")
        || filename.ends_with(".mov")
        || filename.ends_with(".m4v")
        || filename.ends_with(".m3u8")
}
