//! 自定义反序列化器。

use serde::{Deserialize, Deserializer};
use serde_json::Value;

/// 宽松解析为 `f64`。
pub fn f64_lenient<'de, D>(deserializer: D) -> Result<f64, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value_to_f64(&value).unwrap_or(0.0))
}

fn value_to_f64(value: &Value) -> Option<f64> {
    value
        .as_f64()
        .or_else(|| value.as_i64().map(|n| n as f64))
        .or_else(|| value.as_u64().map(|n| n as f64))
        .or_else(|| value.as_str().and_then(|s| s.trim().parse::<f64>().ok()))
}

/// 宽松解析为 `i64`。
pub fn i64_lenient<'de, D>(deserializer: D) -> Result<i64, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value_to_i64(&value).unwrap_or(0))
}

/// 宽松解析为可选 `i64`。
pub fn option_i64_lenient<'de, D>(deserializer: D) -> Result<Option<i64>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value_to_i64(&value))
}

/// 宽松解析为 `u32`，负数会归零。
pub fn u32_lenient<'de, D>(deserializer: D) -> Result<u32, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(value_to_i64(&value).unwrap_or(0).max(0) as u32)
}

fn value_to_i64(value: &Value) -> Option<i64> {
    value
        .as_i64()
        .or_else(|| value.as_u64().map(|n| n as i64))
        .or_else(|| value.as_f64().map(|n| n as i64))
        .or_else(|| value.as_str().and_then(|s| s.trim().parse::<i64>().ok()))
}

/// 0/1 或 bool 解析为 `bool`。
pub fn bool_from_int_or_bool<'de, D>(deserializer: D) -> Result<bool, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(match value {
        Value::Bool(b) => b,
        Value::Number(n) => n.as_i64().map(|v| v != 0).unwrap_or(false),
        Value::String(s) => {
            let t = s.trim();
            t == "1" || t.eq_ignore_ascii_case("true")
        }
        _ => false,
    })
}

/// 任意标量宽松转 `String`。
pub fn string_lossy<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Value::deserialize(deserializer)?;
    Ok(match value {
        Value::String(s) => s,
        Value::Number(n) => n.to_string(),
        Value::Bool(b) => b.to_string(),
        Value::Null => String::new(),
        other => other.to_string(),
    })
}
