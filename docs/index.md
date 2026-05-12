---
title: FishPi 插件开发指南
description: FishPi Android 插件系统开发文档
---

# FishPi 插件开发指南

## 快速开始

在 `/sdcard/fishpi/plugins/` 下创建一个 `.js` 文件：

```javascript
// ==FishPiPlugin==
// @name         我的插件
// @author       你的名字
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==

on('message', function(msg) {
    log('收到消息: ' + msg.type);
});
```

进入聊天室点击顶栏右侧拼图图标即可管理。

---

## 文件头

| 字段 | 必填 | 说明 |
|------|------|------|
| `@name` | 是 | 显示名称（可重复，不参与唯一标识） |
| `@author` | 否 | 作者 |
| `@version` | 否 | 版本号，默认 `0.0.1` |
| `@scenes` | 否 | 生效场景，逗号分隔：`chatRoom`, `privateChat`, `article`, `notice`, `me`；留空则全局生效 |

> 插件主键为**文件名**（含 `.js`），`@name` 仅展示。重命名文件会被视为新插件。

---

## 全局 API

### 上下文变量

| 变量 | 说明 |
|------|------|
| `userName` | 当前登录用户名 |
| `apiKey` | 当前登录 API Key |

```javascript
// 专属红包：只有 receivers 包含自己才抢
if (rp.type === 'specify' && userName) {
    if (rp.receivers.indexOf(userName) < 0) return;
}
```

### on / off

```javascript
on('message', function(msg) { ... });
off('message', fn);
```

### 消息事件

所有消息通过 `message` 事件分发，`msg` 结构与 `ChatRoomMessage` 数据类一致：

```json
{
    "type": "msg",
    "oId": "1778462350466",
    "userName": "Kirito",
    "userNickname": "",
    "userAvatarURL": "...",
    "content": "今天的摸鱼真香！",
    "md": "",
    "contentHtml": "",
    "time": "Mon May 11 09:19:11 CST 2026",
    "client": "",
    "revoked": false,
    "redPacket": null,
    "quote": null
}
```

#### redPacket 子结构

```json
"redPacket": {
    "type": "random",
    "typeName": "拼手气",
    "money": 32,
    "count": 20,
    "got": 0,
    "message": "摸鱼者，事竟成",
    "summary": "",
    "finished": false,
    "openable": true,
    "needGesture": false
}
```

`redPacket.type` 可选值：`random`（拼手气）、`average`（普通）、`specify`（专属）、`rockPaperScissors`（猜拳）、`gesture`（手势）

### fishpi.hook(name, fn)

注册消息钩子，在 `on('message')` 之前调用，可以拦截和修改消息：

```javascript
fishpi.hook('message', function(msg) {
    // 过滤广告
    if (msg.content.indexOf('广告') >= 0) {
        msg.filtered = true;   // 阻止该消息显示
    }
    // 修改消息
    if (msg.content.indexOf('关键词') >= 0) {
        msg.content = msg.content.replace('关键词', '***');
    }
});
```

- `msg.filtered = true`：拦截消息，后续 `on('message')` 不会触发
- hook 中修改的字段在 `on('message')` 中可见
- 与 `on` 的区别：`hook` 先执行，可以拦截；`on` 后执行，只能监听

### fishpi.call(method, params)

调用任意 SDK API。`method` 对应下方方法列表，参数名必须与表格一致。返回 `Promise<ResponseData>`：
- 成功：返回 API 的 `data` 字段本体（通常不含 `ok`）
- 失败：返回 `{ ok: false, error: string }`

```javascript
fishpi.call('openRedPacket', { messageId: msg.oId, gesture: -1 }).then(function(r) {
    if (r.ok === false) { log('error: ' + r.error); return; }
    // r 为 API 原始响应
});
```

### storage

```javascript
var val = storage.get('key', defaultVal);  // 同步读取，首次自动回写默认值
storage.set('key', val);                    // 同步写入
```

- 值自动 JSON 序列化
- 首次读取时默认值自动持久化，设置页立即可见
- 支持任意 JSON 类型（字符串、数字、数组、对象）

### ui

```javascript
ui.toast('提示文字');   // 聊天中插入一条系统消息
```

### log

```javascript
log('调试信息');        // adb logcat -s FishPiPlugin:D
```

---

## SDK API 参考

### 聊天室

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `sendChatRoomMessage` | `content: String` | `{}` |
| `revokeChatRoomMessage` | `id: String` | `{}` |
| `reactChatRoomMessage` | `id: String, value: String` | `{}` |
| `getChatRoomHistory` | `page: Int, selfUsername: String` | 消息数组 |
| `uploadChatFile` | `filePath: String` | `{}` |
| `searchAtUsers` | `query: String` | 用户列表 |

### 红包

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `openRedPacket` | `messageId: String, gesture: Int` | `{info, who, receivers}` |
| `sendRedPacket` | `type: String, money: Int, count: Int, message: String` | `{}` |

`openRedPacket` 返回值：
```json
{
    "info": { "got": 15, "count": 20, "message": "摸鱼者，事竟成", "userName": "Kirito" },
    "who": [{ "userMoney": 5, "userName": "Kirito", "avatar": "...", "time": "..." }],
    "receivers": []
}
```
> `who` 数组最后一项通常是当前用户。

### 用户

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `getUser` | 无 | `{userName, userAvatarURL, points, breezemoons, ...}` |
| `getUserProfile` | `userName: String` | `{userName, userAvatarURL, points, ...}` |
| `getUserActivity` | 无 | `{liveness, checkedIn, livenessRewarded}` |
| `rewardLiveness` | 无 | `{}` |
| `getUserMedals` | `userName: String` | 勋章列表 |

### 私聊

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `sendPrivateChatMessage` | `peer: String, content: String` | `{}` |
| `getPrivateChatSessions` | `selfUsername: String` | 会话列表 |
| `getPrivateChatHistory` | `peer: String, page: Int, selfUsername: String` | 消息数组 |
| `revokePrivateChatMessage` | `id: String` | `{}` |
| `markPrivateChatRead` | `peer: String` | `{}` |

### 文章

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `getArticles` | `filter: String, tag: String, page: Int` | `{items, nextPage}` |
| `getUserArticles` | `userName: String, page: Int` | `{items, nextPage}` |
| `getArticleDetail` | `articleId: String, page: Int` | 文章详情 |
| `sendArticleComment` | `articleId: String, content: String, replyId: String` | `{}` |
| `voteArticle` | `articleId: String, like: Boolean` | `{}` |
| `thankArticle` | `articleId: String` | `{}` |
| `followArticle` | `articleId: String` | `{}` |
| `unfollowArticle` | `articleId: String` | `{}` |
| `watchArticle` | `articleId: String` | `{}` |

评论相关说明：
- 读取评论：通过 `getArticleDetail(articleId, page)` 返回的详情结构获取评论列表（当前没有独立 `getArticleComments` 方法）。
- 发送评论：`sendArticleComment(articleId, content, replyId)`。
- `replyId` 传空字符串 `""` 表示发顶级评论；传具体评论 ID 表示回复该评论。

```javascript
// 顶级评论
fishpi.call('sendArticleComment', {
  articleId: '123456',
  content: '这篇写得很好',
  replyId: ''
});

// 回复某条评论
fishpi.call('sendArticleComment', {
  articleId: '123456',
  content: '同意你的观点',
  replyId: '987654321'
});
```

### 表情

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `getEmojiGroups` | 无 | 表情分组列表 |
| `getEmojiGroupItems` | `groupId: String` | 表情项列表 |

### 清风明月

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `sendBreezemoon` | `content: String` | `{}` |
| `getBreezemoons` | `page: Int, size: Int` | 列表 |
| `getUserBreezemoons` | `userName: String, page: Int, size: Int` | 列表 |

### 通知

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `getNoticeUnreadCount` | 无 | 各分类未读数 |
| `getNotices` | 无 | 通知列表 |
| `markAllNoticesRead` | 无 | `{}` |

### 特殊 action

| 方法 | 参数 | 说明 |
|------|------|------|
| `systemMessage` | `text: String` | 插入系统消息到聊天 |

---

## 完整示例：红包助手

```javascript
// ==FishPiPlugin==
// @name         红包助手
// @author       Kirito
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==

on('message', function(msg) {
    if (msg.type !== 'redPacket') return;
    var rp = msg.redPacket;
    if (!rp.openable) return;

    // 每次读取最新设置，修改即时生效
    var skip = storage.get('skipTypes', ['rockPaperScissors']);
    var delay = storage.get('delay', 3000);
    if (skip.indexOf(rp.type) >= 0) return;

    // 专属红包：只抢 receivers 包含自己的
    if (rp.type === 'specify' && userName) {
        if ((rp.receivers || []).indexOf(userName) < 0) return;
    }

    setTimeout(function() {
        fishpi.call('openRedPacket', {messageId: msg.oId, gesture: -1}).then(function(r) {
            if (r.ok === false) { log('error: ' + r.error); return; }
            var me = r.who[r.who.length - 1];
            var got = me ? me.userMoney : 0;
            ui.toast('[红包助手] +' + got + ' (' + r.info.got + '/' + r.info.count + ')');
        });
    }, delay + Math.random() * 2000);
});
```

---

## 插件管理

聊天室顶栏右侧**拼图图标** → 底部面板：

| 操作 | 说明 |
|------|------|
| 点击卡片 | 查看详情：状态、错误日志、最近调用记录和耗时 |
| 齿轮图标 | 插件设置：数组用标签编辑、数字用滑动条、字符串用文本框 |
| 开关 | 启用/禁用插件 |
| 垃圾桶 | 卸载插件（需确认） |

---

## 调试

```bash
adb logcat -s FishPiPlugin:D
adb shell ls /sdcard/fishpi/plugins/
adb shell rm /sdcard/fishpi/plugins/xxx.js
```
