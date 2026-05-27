use fishpi_sdk::api::user::User;
use std::sync::OnceLock;
use tokio::sync::Mutex;

static CURRENT_USER: OnceLock<Mutex<Option<User>>> = OnceLock::new();

fn current_user_slot() -> &'static Mutex<Option<User>> {
    CURRENT_USER.get_or_init(|| Mutex::new(None))
}

pub(crate) async fn set_current_user(user: User) -> Result<(), String> {
    let mut guard = current_user_slot().lock().await;
    *guard = Some(user);
    Ok(())
}

pub(crate) async fn current_user(api_key: &str) -> Result<User, String> {
    let token = api_key.trim();
    let mut guard = current_user_slot().lock().await;
    let needs_refresh = guard
        .as_ref()
        .map(|user| user.get_token() != token)
        .unwrap_or(true);
    if needs_refresh {
        *guard = Some(User::new(token.to_string()));
    }
    guard
        .as_ref()
        .cloned()
        .ok_or_else(|| "native user session is not initialized".to_string())
}
