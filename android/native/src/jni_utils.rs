use jni::JNIEnv;
use jni::JavaVM;
use jni::objects::{GlobalRef, JObject, JString, JValue};
use jni::sys::jstring;
use serde_json::Value;
use std::sync::Arc;

pub(crate) fn string_arg(env: &mut JNIEnv, value: JString) -> Result<String, String> {
    env.get_string(&value)
        .map(|s| s.to_string_lossy().into_owned())
        .map_err(|err| err.to_string())
}

pub(crate) fn to_jstring(env: &mut JNIEnv, value: String) -> jstring {
    env.new_string(value)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

pub(crate) fn emit_callback(java_vm: &Arc<JavaVM>, callback: &GlobalRef, payload: Value) {
    let Ok(mut env) = java_vm.attach_current_thread() else {
        return;
    };
    let Ok(message) = env.new_string(payload.to_string()) else {
        return;
    };
    let message_obj = JObject::from(message);
    let _ = env.call_method(
        callback.as_obj(),
        "onEvent",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&message_obj)],
    );
}
