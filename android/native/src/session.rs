use fishpi_sdk::FishPi;
use std::sync::OnceLock;
use tokio::sync::Mutex;

static CURRENT_CLIENT: OnceLock<Mutex<Option<FishPi>>> = OnceLock::new();

fn client_slot() -> &'static Mutex<Option<FishPi>> {
    CURRENT_CLIENT.get_or_init(|| Mutex::new(None))
}

pub(crate) async fn set_current_user(client: FishPi) -> Result<(), String> {
    let mut guard = client_slot().lock().await;
    *guard = Some(client);
    Ok(())
}

pub(crate) async fn current_user(api_key: &str) -> Result<FishPi, String> {
    let token = api_key.trim();
    let mut guard = client_slot().lock().await;
    let needs_refresh = guard
        .as_ref()
        .map(|client| client.api_key().as_str() != token)
        .unwrap_or(true);
    if needs_refresh {
        let client = FishPi::builder()
            .api_key(token)
            .build()
            .map_err(|err| format!("native client init failed: {err}"))?;
        *guard = Some(client);
    }
    guard
        .as_ref()
        .cloned()
        .ok_or_else(|| "native user session is not initialized".to_string())
}
