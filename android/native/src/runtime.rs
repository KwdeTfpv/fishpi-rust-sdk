use serde_json::{Value, json};
use std::sync::OnceLock;

pub(crate) static SHARED_RUNTIME: OnceLock<tokio::runtime::Runtime> = OnceLock::new();

pub(crate) fn shared_runtime() -> Result<&'static tokio::runtime::Runtime, String> {
    if let Some(rt) = SHARED_RUNTIME.get() {
        return Ok(rt);
    }
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .thread_name("fishpi-native-rt")
        .build()
        .map_err(|err| format!("Rust runtime init failed: {err}"))?;
    match SHARED_RUNTIME.set(runtime) {
        Ok(()) | Err(_) => SHARED_RUNTIME
            .get()
            .ok_or_else(|| "shared runtime missing after initialization".to_string()),
    }
}

pub(crate) fn runtime_json<F>(f: F) -> String
where
    F: FnOnce(&tokio::runtime::Runtime) -> Result<Value, String>,
{
    match shared_runtime() {
        Ok(rt) => match f(rt) {
            Ok(data) => json!({ "ok": true, "data": data }).to_string(),
            Err(err) => json!({ "ok": false, "error": err }).to_string(),
        },
        Err(err) => json!({ "ok": false, "error": err }).to_string(),
    }
}
