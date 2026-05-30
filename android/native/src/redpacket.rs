use crate::chatroom::apply_redpacket_client_config;
use crate::jni_utils::{string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::model::redpacket::{RedPacket, RedPacketType};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::{Value, json};

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_openRedPacket(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    message_id: JString,
    gesture: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, message_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let redpacket = &user.redpacket;
                let info = redpacket
                    .open(&id, gesture_from_index(gesture))
                    .await
                    .map_err(|err| format!("拆红包失败: {err}"))?;
                Ok(redpacket_info_to_json(info))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendRedPacket(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    type_name: JString,
    money: i32,
    count: i32,
    message: JString,
    receivers: JString,
    gesture: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let raw_type = string_arg(&mut env, type_name)?;
        let msg = string_arg(&mut env, message)?;
        let receiver_text = string_arg(&mut env, receivers)?;
        if money <= 0 || count <= 0 {
            return Err("红包积分和个数必须大于 0".to_string());
        }
        let packet_type = match raw_type.as_str() {
            "average" => RedPacketType::Average,
            "specify" => RedPacketType::Specify,
            "heartbeat" => RedPacketType::Heartbeat,
            "rockPaperScissors" => RedPacketType::RockPaperScissors,
            _ => RedPacketType::Random,
        };
        let receivers = receiver_text
            .split(|c: char| c == ',' || c == '，' || c.is_whitespace())
            .map(str::trim)
            .filter(|s| !s.is_empty())
            .map(ToString::to_string)
            .collect::<Vec<_>>();
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let mut redpacket = user.redpacket;
                apply_redpacket_client_config(&mut redpacket);
                let packet = RedPacket {
                    r#type: packet_type.clone(),
                    money: money as u32,
                    count: count as u32,
                    message: if msg.trim().is_empty() {
                        match packet_type {
                            RedPacketType::RockPaperScissors => "剪刀石头布!".to_string(),
                            RedPacketType::Specify => "看看是不是给你的".to_string(),
                            _ => "摸鱼者，事竟成".to_string(),
                        }
                    } else {
                        msg.trim().to_string()
                    },
                    receivers,
                    gesture: gesture_from_index(gesture),
                };
                redpacket
                    .send(&packet)
                    .await
                    .map_err(|err| format!("发送红包失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
