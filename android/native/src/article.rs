use crate::jni_utils::{emit_callback, string_arg, to_jstring};
use crate::mappers::*;
use crate::runtime::runtime_json;
use crate::session::current_user;
use fishpi_sdk::api::article::ArticleListener;
use fishpi_sdk::api::ws::WebSocketClient;
use fishpi_sdk::model::article::{ArticleDraftSave, ArticlePost, ArticleType, CommentPost};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jlong, jstring};
use serde_json::{Value, json};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::time::timeout;

struct AndroidArticleConnection {
    _runtime: tokio::runtime::Runtime,
    ws: Mutex<WebSocketClient>,
}

fn blank_to_none(value: String) -> Option<String> {
    let trimmed = value.trim().to_string();
    if trimmed.is_empty() { None } else { Some(trimmed) }
}

fn reward_point_or_none(value: String) -> Option<String> {
    blank_to_none(value).filter(|v| v.parse::<u32>().unwrap_or(0) > 0)
}

fn qna_offer_or_none(value: i32) -> Option<u32> {
    (value > 0).then_some(value as u32)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getArticles(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    filter: JString,
    tag: JString,
    page: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let filter_name = string_arg(&mut env, filter)?;
        let tag_name = string_arg(&mut env, tag)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article = &user.article;
                let tag_opt = if tag_name.trim().is_empty() {
                    None
                } else {
                    Some(tag_name.trim())
                };
                let list = timeout(
                    Duration::from_secs(15),
                    article.list(
                        article_list_type_from_str(filter_name.trim()),
                        page.max(1) as u32,
                        20,
                        tag_opt,
                    ),
                )
                .await
                .map_err(|_| "加载帖子列表超时".to_string())?
                .map_err(|err| format!("加载帖子列表失败: {err}"))?;
                let has_more = (page.max(1) as u32) < list.pagination.count;
                let items = list
                    .list
                    .into_iter()
                    .map(article_summary_to_json)
                    .collect::<Vec<_>>();
                Ok(json!({
                    "items": items,
                    "nextPage": page.max(1) + 1,
                    "hasMore": has_more,
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getUserArticles(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    username: JString,
    page: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let user = string_arg(&mut env, username)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user_client = current_user(&token).await?;
                let article = &user_client.article;
                let list = timeout(
                    Duration::from_secs(15),
                    article.list_by_user(user.trim(), page.max(1) as u32, 20),
                )
                .await
                .map_err(|_| "加载用户帖子超时".to_string())?
                .map_err(|err| format!("加载用户帖子失败: {err}"))?;
                let has_more = (page.max(1) as u32) < list.pagination.count;
                let items = list
                    .list
                    .into_iter()
                    .map(article_summary_to_json)
                    .collect::<Vec<_>>();
                Ok(json!({
                    "items": items,
                    "nextPage": page.max(1) + 1,
                    "hasMore": has_more,
                }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getArticleDetail(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
    page: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article = &user.article;
                let page = page.max(1) as u32;
                let mut detail = timeout(Duration::from_secs(15), article.detail(&id, page))
                    .await
                    .map_err(|_| "加载帖子详情超时".to_string())?
                    .map_err(|err| format!("加载帖子详情失败: {err}"))?;
                if let Ok(markdown_result) =
                    timeout(Duration::from_secs(10), article.markdown_source(&id)).await
                {
                    if let Ok(source) = markdown_result {
                        if !source.trim().is_empty() {
                            detail.source = source;
                        }
                    }
                }
                Ok(article_detail_to_json(detail, page))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getArticleHeat(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let heat = timeout(Duration::from_secs(10), user.article.heat(&id))
                    .await
                    .map_err(|_| "加载阅读人数超时".to_string())?
                    .map_err(|err| format!("加载阅读人数失败: {err}"))?;
                Ok(json!({ "articleHeat": heat }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_connectArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
    article_type: i32,
    callback: JObject,
) -> jlong {
    let result: Result<jlong, String> = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        let java_vm = Arc::new(env.get_java_vm().map_err(|err| err.to_string())?);
        let callback = env
            .new_global_ref(&callback)
            .map_err(|err| err.to_string())?;
        let runtime = tokio::runtime::Runtime::new()
            .map_err(|err| format!("Rust runtime init failed: {err}"))?;

        let ws = runtime.block_on(async {
            let user = current_user(&token).await?;
            let article_type = ArticleType::from_index(article_type.max(0) as usize);
            let java_vm_for_article = Arc::clone(&java_vm);
            let callback_for_article = callback.clone();
            let listener: ArticleListener = Arc::new(move |msg: Value| {
                let java_vm = Arc::clone(&java_vm_for_article);
                let callback = callback_for_article.clone();
                Box::pin(async move {
                    if msg["type"].as_str() == Some("articleHeat") {
                        emit_callback(
                            &java_vm,
                            &callback,
                            json!({
                                "event": "articleHeat",
                                "articleId": msg["articleId"].as_str().unwrap_or(""),
                                "operation": msg["operation"].as_str().unwrap_or(""),
                            }),
                        );
                    }
                })
            });
            user.article
                .add_listener(&id, article_type, listener)
                .await
                .map_err(|err| format!("帖子监听连接失败: {err}"))
        })?;

        let handle = Box::new(AndroidArticleConnection {
            _runtime: runtime,
            ws: Mutex::new(ws),
        });
        Ok(Box::into_raw(handle) as jlong)
    })();

    match result {
        Ok(handle) => handle,
        Err(_err) => 0,
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_disconnectArticle(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }

    let connection = unsafe { Box::from_raw(handle as *mut AndroidArticleConnection) };
    if let Ok(ws) = connection.ws.lock() {
        ws.disconnect();
    }
    drop(connection);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getArticleDrafts(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let drafts = timeout(Duration::from_secs(15), user.article.list_drafts())
                    .await
                    .map_err(|_| "加载草稿超时".to_string())?
                    .map_err(|err| format!("加载草稿失败: {err}"))?
                    .into_iter()
                    .map(article_draft_summary_to_json)
                    .collect::<Vec<_>>();
                Ok(Value::Array(drafts))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_getArticleDraftDetail(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    draft_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, draft_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let detail = timeout(Duration::from_secs(15), user.article.draft_detail(&id))
                    .await
                    .map_err(|_| "加载草稿详情超时".to_string())?
                    .map_err(|err| format!("加载草稿详情失败: {err}"))?;
                Ok(article_draft_detail_to_json(detail))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_saveArticleDraft(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    draft_id: JString,
    title: JString,
    content: JString,
    thought_content: JString,
    tags: JString,
    article_type: i32,
    column_id: JString,
    column_title: JString,
    chapter_no: JString,
    reward_content: JString,
    reward_point: JString,
    qna_offer_point: i32,
    commentable: jni::sys::jboolean,
    anonymous: jni::sys::jboolean,
    notify_followers: jni::sys::jboolean,
    show_in_list: i32,
    statement: i32,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let payload = ArticleDraftSave {
            draft_id: blank_to_none(string_arg(&mut env, draft_id)?),
            title: string_arg(&mut env, title)?,
            content: string_arg(&mut env, content)?,
            thought_content: string_arg(&mut env, thought_content)?,
            tags: string_arg(&mut env, tags)?,
            article_type: article_type.max(0) as u32,
            column_id: string_arg(&mut env, column_id)?,
            column_title: string_arg(&mut env, column_title)?,
            chapter_no: string_arg(&mut env, chapter_no)?,
            reward_content: string_arg(&mut env, reward_content)?,
            reward_point: string_arg(&mut env, reward_point)?,
            qna_offer_point: qna_offer_or_none(qna_offer_point),
            commentable: commentable != 0,
            anonymous: anonymous != 0,
            notify_followers: notify_followers != 0,
            show_in_list: show_in_list.max(0) as u32,
            statement: statement.max(0) as u32,
        };
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let draft = timeout(Duration::from_secs(15), user.article.save_draft(&payload))
                    .await
                    .map_err(|_| "保存草稿超时".to_string())?
                    .map_err(|err| format!("保存草稿失败: {err}"))?;
                Ok(article_draft_summary_to_json(draft))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_deleteArticleDraft(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    draft_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, draft_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let deleted_id = timeout(Duration::from_secs(15), user.article.delete_draft(&id))
                    .await
                    .map_err(|_| "删除草稿超时".to_string())?
                    .map_err(|err| format!("删除草稿失败: {err}"))?;
                Ok(json!({ "id": deleted_id }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_publishArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    title: JString,
    content: JString,
    tags: JString,
    reward_content: JString,
    reward_point: JString,
    qna_offer_point: i32,
    commentable: jni::sys::jboolean,
    anonymous: jni::sys::jboolean,
    notify_followers: jni::sys::jboolean,
    show_in_list: i32,
    is_good_article: jni::sys::jboolean,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let payload = ArticlePost {
            title: string_arg(&mut env, title)?,
            content: string_arg(&mut env, content)?,
            tags: string_arg(&mut env, tags)?,
            commentable: commentable != 0,
            notify_followers: notify_followers != 0,
            type_: ArticleType::Normal,
            show_in_list: show_in_list.max(0) as u32,
            reward_content: blank_to_none(string_arg(&mut env, reward_content)?),
            reward_point: reward_point_or_none(string_arg(&mut env, reward_point)?),
            anonymous: Some(anonymous != 0),
            offer_point: qna_offer_or_none(qna_offer_point),
            is_good_article: if is_good_article != 0 {
                Some("yes".to_string())
            } else {
                None
            },
        };
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article_id = timeout(Duration::from_secs(20), user.article.post_article(&payload))
                    .await
                    .map_err(|_| "发布帖子超时".to_string())?
                    .map_err(|err| format!("发布帖子失败: {err}"))?;
                Ok(json!({ "articleId": article_id }))
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_sendArticleComment(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
    content: JString,
    reply_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        let text = string_arg(&mut env, content)?;
        let reply = string_arg(&mut env, reply_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let comment = &user.comment;
                let payload = CommentPost {
                    article_id: id,
                    anonymous: false,
                    // commentVisible=true 会触发“仅作者和楼主可见”，默认评论应公开显示。
                    visible: false,
                    content: text,
                    reply_id: reply,
                };
                let result = timeout(Duration::from_secs(15), comment.send(payload))
                    .await
                    .map_err(|_| "发送评论超时".to_string())?
                    .map_err(|err| format!("发送评论失败: {err}"))?;
                if !result.success {
                    return Err(if result.msg.is_empty() {
                        "发送评论失败".to_string()
                    } else {
                        result.msg
                    });
                }
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_voteArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
    like: jni::sys::jboolean,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        let is_like = like != 0;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article = &user.article;
                timeout(Duration::from_secs(15), article.vote(&id, is_like))
                    .await
                    .map_err(|_| "帖子投票超时".to_string())?
                    .map_err(|err| format!("帖子投票失败: {err}"))?;
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_thankArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article = &user.article;
                let result = timeout(Duration::from_secs(15), article.thank(&id))
                    .await
                    .map_err(|_| "感谢帖子超时".to_string())?
                    .map_err(|err| format!("感谢帖子失败: {err}"))?;
                if !result.success {
                    return Err(if result.msg.is_empty() {
                        "感谢帖子失败".to_string()
                    } else {
                        result.msg
                    });
                }
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_rewardArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let result = timeout(Duration::from_secs(15), user.article.reward(&id))
                    .await
                    .map_err(|_| "打赏帖子超时".to_string())?
                    .map_err(|err| format!("打赏帖子失败: {err}"))?;
                if !result.success {
                    return Err(if result.msg.is_empty() {
                        "打赏帖子失败".to_string()
                    } else {
                        result.msg
                    });
                }
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_followArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article = &user.article;
                let result = timeout(Duration::from_secs(15), article.follow(&id))
                    .await
                    .map_err(|_| "收藏帖子超时".to_string())?
                    .map_err(|err| format!("收藏帖子失败: {err}"))?;
                if !result.success {
                    return Err(if result.msg.is_empty() {
                        "收藏帖子失败".to_string()
                    } else {
                        result.msg
                    });
                }
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_unfollowArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let result = timeout(Duration::from_secs(15), user.article.unfollow(&id))
                    .await
                    .map_err(|_| "取消收藏超时".to_string())?
                    .map_err(|err| format!("取消收藏失败: {err}"))?;
                if !result.success {
                    return Err(if result.msg.is_empty() {
                        "取消收藏失败".to_string()
                    } else {
                        result.msg
                    });
                }

                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_fishpi_mobile_data_FishPiNative_watchArticle(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    article_id: JString,
) -> jstring {
    let result = (|| {
        let token = string_arg(&mut env, api_key)?;
        let id = string_arg(&mut env, article_id)?;
        Ok(runtime_json(|rt| {
            rt.block_on(async {
                let user = current_user(&token).await?;
                let article = &user.article;
                let result = timeout(Duration::from_secs(15), article.watch(&id))
                    .await
                    .map_err(|_| "关注帖子超时".to_string())?
                    .map_err(|err| format!("关注帖子失败: {err}"))?;
                if !result.success {
                    return Err(if result.msg.is_empty() {
                        "关注帖子失败".to_string()
                    } else {
                        result.msg
                    });
                }
                Ok(Value::Null)
            })
        }))
    })()
    .unwrap_or_else(|err: String| json!({ "ok": false, "error": err }).to_string());

    to_jstring(&mut env, result)
}
