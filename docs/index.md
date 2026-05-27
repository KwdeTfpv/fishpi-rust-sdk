---
title: FishPi 插件开发指南
description: FishPi Android 插件系统开发文档
---

# FishPi 插件开发指南

FishPi Android 插件是放在手机本地的 JavaScript 文件，运行在 App 内置 WebView 沙箱中。插件可以监听聊天室消息、修改待发送文本、调用已暴露的 SDK API、保存配置、发送系统提示，注册聊天室快捷动作，并生成原生插件页面、对话框和表单 UI。

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
    log('收到消息: ' + msg.type + ' / ' + msg.content);
});
```

进入聊天室点击输入区 `+` 菜单中的“插件”即可管理插件。插件文件名是插件主键，例如 `my-plugin.js`，重命名文件会被视为新插件。

## 文件头

文件头必须放在 JS 文件顶部附近：

```javascript
// ==FishPiPlugin==
// @name         快捷助手
// @author       Kirito
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `@name` | 是 | 管理界面显示名称，可重复 |
| `@author` | 否 | 作者 |
| `@version` | 否 | 版本号，默认 `0.0.1` |
| `@scenes` | 否 | 生效场景，逗号分隔；留空表示全局 |
| `@permissions` | 否 | 当前版本会解析但不做权限校验 |

当前已接入插件事件和快捷工具栏的场景是：

| scene | 说明 |
|------|------|
| `chatRoom` | 聊天室 |

`privateChat`、`article`、`notice`、`me` 可以写在文件头中，但当前版本主要用于后续扩展；插件事件不会自动在这些页面分发。

## 运行环境

插件运行在 App 内部 WebView，支持常见浏览器 JS 能力，例如 `setTimeout`、`JSON`、`Promise`。宿主注入以下全局变量：

| 变量 | 类型 | 说明 |
|------|------|------|
| `userName` | `string` | 当前登录用户名 |
| `apiKey` | `string` | 当前登录 API Key |
| `userAvatarURL` | `string` | 当前预留为空字符串 |

注意：

- 插件可以读取 `apiKey`，因此只安装可信插件。
- 插件禁用、卸载、重载或沙箱销毁时，其快捷工具栏入口和聊天室发送身份配置会被清理。
- `fishpi.call()` 内部有约 10 秒等待限制，超时会返回错误。

## 事件系统

### on / off

```javascript
function handleMessage(msg) {
    log(msg.content);
}

on('message', handleMessage);
off('message', handleMessage);
```

| 事件 | 触发场景 | 参数 |
|------|----------|------|
| `message` | 聊天室收到新消息时 | `ChatRoomMessage` |
| `toolbarAction` | 用户点击插件快捷动作时 | `{ entryId, actionId }` |

### message 事件

`message` 的结构与聊天室消息结构一致：

```json
{
  "oId": "1778462350466",
  "userName": "Kirito",
  "userNickname": "只有午安",
  "userAvatarURL": "https://...",
  "content": "今天的摸鱼真香！",
  "md": "今天的摸鱼真香！",
  "contentHtml": "<p>今天的摸鱼真香！</p>",
  "imageUrls": [],
  "linkUrls": [],
  "time": "Mon May 11 09:19:11 CST 2026",
  "client": "Android",
  "type": "msg",
  "revoked": false,
  "reactionSummary": [
    {
      "value": "thumbsup",
      "emoji": "👍",
      "count": 2,
      "selected": false,
      "users": [],
      "userDetails": []
    }
  ],
  "currentUserReaction": "",
  "redPacket": null,
  "quote": null
}
```

`type` 常见值：

| 值 | 说明 |
|----|------|
| `msg` | 普通消息 |
| `redPacket` | 红包消息 |
| `barrager` | 弹幕消息 |
| `system` | 客户端兜底系统消息 |

### quote 子结构

```json
{
  "text": "被引用的文本",
  "imageUrls": ["https://.../a.png"]
}
```

### redPacket 子结构

```json
{
  "type": "random",
  "typeName": "拼手气红包",
  "money": 32,
  "count": 20,
  "got": 0,
  "message": "摸鱼者，事竟成",
  "summary": "【拼手气红包】32 积分 / 20 个（已领 0/20）",
  "finished": false,
  "openable": true,
  "needGesture": false,
  "gesture": null,
  "receivers": [],
  "who": []
}
```

红包类型：

| type | 说明 |
|------|------|
| `random` | 拼手气 |
| `average` | 平分 |
| `specify` | 专属红包 |
| `heartbeat` | 心跳红包 |
| `rockPaperScissors` | 猜拳红包 |

猜拳手势：`0` 石头，`1` 剪刀，`2` 布；普通红包拆包时传 `-1` 或不传。

## Hook

### fishpi.hook('message', fn)

在 `on('message')` 之前处理收到的消息。

```javascript
fishpi.hook('message', function(msg) {
    if (msg.content.indexOf('广告') >= 0) {
        msg.filtered = true;
        return;
    }
    msg.content = msg.content.replace('关键词', '***');
});
```

- `msg.filtered = true` 会阻止该消息进入后续 `on('message')`。
- hook 中修改的字段会被后续 handler 看到。

### fishpi.hook('sendMessage', fn)

修改聊天室即将发送的文本。

```javascript
fishpi.hook('sendMessage', function(text) {
    if (!text.trim()) return null;
    return text + '\n\n来自 FishPi Android';
});
```

- 返回字符串：用返回值替换原文本。
- 返回 `null` / `undefined`：当前实现会转为空字符串，相当于取消或发送空内容，建议插件自行避免。
- 多个插件同时启用时会按宿主当前加载顺序依次处理。

## fishpi.call(method, params)

调用宿主暴露的 SDK API。

```javascript
fishpi.call('openRedPacket', { messageId: msg.oId, gesture: -1 }).then(function(r) {
    if (r.ok === false) {
        log('失败: ' + r.error);
        return;
    }
    log('已领取: ' + r.info.got + '/' + r.info.count);
});
```

返回约定：

- 成功：返回 native `data` 字段本体，通常不包含 `ok`。
- 失败：返回 `{ "ok": false, "error": "错误信息" }`。
- 发送/撤回等无返回数据的接口成功时通常返回 `null` 或 `{}`，插件不要依赖固定空对象。
- 文档中的结构是当前 Android native 层整理后的结构，服务端未来新增字段时可能额外出现更多字段。

## 聊天室发送身份

插件默认使用 App 的聊天室发送身份。插件也可以为自己声明独立的 client type，只影响当前插件后续调用 `sendChatRoomMessage`，不影响 App 正常发送，也不影响其它插件。

```javascript
fishpi.chat.setClientType('Rust', 'my-plugin-1.0.0');

fishpi.call('sendChatRoomMessage', {
    content: '来自插件的消息'
});
```

清除当前插件的发送身份配置：

```javascript
fishpi.chat.clearClientType();
```

规则：

- `client` 和 `version` 都不能为空。
- 配置只保存在当前插件沙箱生命周期内，不会持久化。
- 插件重载前会先清理旧配置；删除 `setClientType` 代码并重载后，不会继续使用旧值。
- 未调用 `setClientType` 或调用 `clearClientType` 后，`sendChatRoomMessage` 会回到默认兼容行为。

可用 `client` 枚举值：

`Web`、`PC`、`Mobile`、`Windows`、`macOS`、`Linux`、`iOS`、`Android`、`IDEA`、`Chrome`、`Edge`、`VSCode`、`Python`、`Golang`、`Rust`、`Harmony`、`CLI`、`Bird`、`IceNet`、`ElvesOnline`、`Other`

## 快捷动作工具栏

插件可以在聊天室输入框上方注册轻量入口。宿主只展示入口和动作，点击动作后把事件发回插件，具体动作完全由插件决定。

### 注册入口

```javascript
fishpi.toolbar.register({
    id: 'quick-actions',
    title: '快捷助手',
    actions: [
        { id: 'hello', label: '问好', subtitle: '发送固定问候' },
        { id: 'status', label: '状态', subtitle: '读取当前积分', enabled: true }
    ]
});
```

entry 结构：

```ts
type ToolbarEntry = {
  id: string;
  title: string;
  actions: ToolbarAction[];
}

type ToolbarAction = {
  id: string;
  label: string;
  subtitle?: string;
  enabled?: boolean; // 默认 true
}
```

规则：

- `id` 和 `title` 不能为空。
- 一个插件可以注册多个入口。
- 同一个插件重复注册相同 `id` 会覆盖旧入口。
- `enabled: false` 的动作会显示为禁用，不会触发点击。
- 入口只在插件 `@scenes` 命中的当前场景显示。

### 接收点击事件

```javascript
on('toolbarAction', function(action) {
    if (!action || action.entryId !== 'quick-actions') return;

    if (action.actionId === 'hello') {
        fishpi.call('sendChatRoomMessage', { content: '大家好，我来摸鱼了' });
    }

    if (action.actionId === 'status') {
        fishpi.call('getUser', {}).then(function(user) {
            if (user.ok === false) return ui.toast(user.error);
            ui.toast('当前积分：' + user.points);
        });
    }
});
```

### 删除入口

```javascript
fishpi.toolbar.unregister('quick-actions');
fishpi.toolbar.clear();
```

返回：

```json
{ "ok": true }
```

## storage

插件私有存储，按插件文件名隔离。

```javascript
var skipTypes = storage.get('skipTypes', ['rockPaperScissors']);
var delaySec = storage.get('delaySec', 1.0);

storage.set('delaySec', 2.5);
```

规则：

- 同步读取/写入。
- 值会被 JSON 序列化。
- `storage.get(key, defaultValue)` 首次读取不存在的 key 时，会自动写入默认值。
- 插件设置页会根据存储值类型显示编辑控件：数组、数字、字符串等。

## ui 与 log

```javascript
ui.toast('提示文字');
log('调试信息');
```

| API | 说明 |
|-----|------|
| `ui.toast(text)` | 在聊天室插入一条系统消息 |
| `ui.dialog(title)` | 创建一个原生插件对话框 |
| `ui.page(title)` | 创建一个原生插件页面 |
| `log(text)` | 输出到 `adb logcat -s FishPiPlugin:D` |

## 插件生成原生 UI

插件可以用 `ui.dialog()` 或 `ui.page()` 生成 App 内原生 UI。宿主会使用统一的 FishPi 视觉组件渲染，不是 WebView 页面；插件只声明节点结构，点击和表单值会通过 `uiAction` 回到插件。

### 打开一个对话框

```javascript
var panel = ui.dialog('快捷面板')
    .text('今天想做什么？', { style: 'title' })
    .markdown('可以用 **Markdown** 展示说明。')
    .stat('当前积分', 1024, '来自 getUser')
    .button('刷新资料', function(values) {
        fishpi.call('getUser', {}).then(function(user) {
            if (user.ok === false) return ui.toast(user.error);
            panel.clear();
            panel.text(user.userNickname || user.userName, { style: 'title' })
                .stat('积分', user.points, '在线 ' + user.onlineMinutes + ' 分钟')
                .update();
        });
    });

panel.open();
```

### 打开一个页面

```javascript
var page = ui.page('插件工具箱')
    .section('常用操作', [
        { type: 'button', label: '发一条消息', actionId: 'send-hello' },
        { type: 'button', label: '关闭页面', actionId: 'close-page' }
    ]);

on('uiAction', function(e) {
    if (e.actionId === 'send-hello') {
        fishpi.call('sendChatRoomMessage', { content: '来自插件页面' });
    }
    if (e.actionId === 'close-page') {
        page.close();
    }
});

page.open();
```

### 表单与回调

表单节点的值会在按钮、`actionBar` 或带 `actionId` 的节点触发时，一起传给回调。`button(label, fn)` 的第一个参数就是当前表单值。

```javascript
ui.dialog('发送清风明月')
    .textarea('content', '内容', {
        placeholder: '写一点轻轻的东西'
    })
    .switch('alsoChat', '同时发到聊天室', { checked: false })
    .button('发送', function(values) {
        var content = (values.content || '').trim();
        if (!content) return ui.toast('内容不能为空');

        fishpi.call('sendBreezemoon', { content: content }).then(function(r) {
            if (r && r.ok === false) return ui.toast(r.error);
            if (values.alsoChat) {
                fishpi.call('sendChatRoomMessage', { content: content });
            }
            ui.toast('已发送');
            ui.dialog('完成').text('清风明月已发布').open();
        });
    })
    .open();
```

`uiAction` 事件结构：

```json
{
  "actionId": "send",
  "nodeId": "node-id",
  "values": {
    "content": "输入内容",
    "alsoChat": true
  }
}
```

### 动态更新

`open()` 用于首次打开，`update()` 用于替换当前插件 UI 的节点，`clear()` 清空当前节点，`close()` 关闭当前插件 UI。

```javascript
var page = ui.page('加载用户资料').loading('正在读取...');
page.open();

fishpi.call('getUserProfile', { userName: 'Kirito' }).then(function(user) {
    if (user.ok === false) {
        page.clear();
        page.error(user.error).update();
        return;
    }
    page.clear();
    page.userCard({
        username: user.userName,
        displayName: user.userNickname,
        avatar: user.userAvatarURL,
        actionId: 'open-user'
    }).markdown(user.intro || '这个人很神秘。').update();
});
```

### 支持的节点

| 节点 | Builder | 主要字段 |
|------|---------|----------|
| `text` | `.text(text, opts)` | `text`, `style: "body" \| "title"` |
| `markdown` | `.markdown(text)` | `text` |
| `image` | `.image(url, opts)` | `url`, `caption` |
| `divider` | `.divider()` | 无 |
| `space` | `.space(height)` | `height` |
| `json` | `.json(data)` | `data` |
| `card` | `.card(opts)` | `title`, `subtitle`, `children`, `actionId` |
| `section` | `.section(title, children)` | `title`, `children` |
| `row` | `.row(children)` | `children` |
| `columns` | `.columns(children)` | `children` |
| `tabs` | `.tabs(tabs)` | `tabs: [{ id, label, children }]` |
| `input` | `.input(name, label, opts)` | `name`, `label`, `value`, `placeholder` |
| `textarea` | `.textarea(name, label, opts)` | 同 `input`，多行 |
| `number` | `.number(name, label, opts)` | `value`, `min`, `max` |
| `switch` | `.switch(name, label, opts)` | `checked` |
| `select` | `.select(name, label, options, opts)` | `value`, `options` |
| `chips` | `.chips(name, options, opts)` | `values`, `options` |
| `slider` | `.slider(name, label, opts)` | `value`, `min`, `max` |
| `loading` | `.loading(text)` | `text` |
| `error` | `.error(text)` | `text` |
| `empty` | `.empty(text)` | `text` |
| `list` | `.list(items)` | `items: [{ id, title, subtitle, actionId }]` |
| `table` | `.table(headers, rows)` | `headers`, `rows` |
| `stat` | `.stat(label, value, detail)` | `label`, `value`, `detail` |
| `userCard` | `.userCard(opts)` | `username`, `displayName`, `avatar`, `actionId` |
| `articleCard` | `.articleCard(opts)` | `articleId`, `title`, `preview`, `actionId` |
| `actionBar` | `.actionBar(actions)` | `actions: [{ id, label, enabled, onClick }]` |
| `button` | `.button(label, fn, opts)` | `label`, `enabled`, `actionId` |

`options` 可以写字符串数组，也可以写对象数组：

```javascript
[
  'recent',
  { value: 'good', label: '优选' }
]
```

### 原始 ui.open

如果不使用 builder，也可以直接调用底层接口。`container` 支持 `dialog`、`page`、`sheet`。

```javascript
fishpi.call('ui.open', {
    id: 'raw-demo',
    container: 'sheet',
    title: '原始节点示例',
    nodes: [
        { type: 'text', text: '这是一个 sheet', style: 'title' },
        { type: 'button', label: '关闭', actionId: 'close' }
    ]
});

on('uiAction', function(e) {
    if (e.actionId === 'close') fishpi.call('ui.close', {});
});
```

规则：

- 插件 UI 是原生 Compose UI，插件不要写 HTML/CSS 布局。
- `dialog` 适合轻量确认、表单和结果；`page` 适合信息较多的工具页；`sheet` 目前需要通过 `fishpi.call('ui.open', ...)` 使用。
- `update()` 会替换当前 UI 的节点；如果当前插件没有打开 UI，会自动打开。
- 表单值按 `name` 聚合，按钮点击时会带上当前所有表单值。
- `button(label, fn)` 和 `actionBar([{ onClick }])` 会自动绑定回调；原始 `actionId` 可通过 `on('uiAction')` 处理。
- 插件 UI 会跟随 App 主题和 UE 层级，不要把 JSON 当成主要展示方式，`json` 只适合作为调试兜底。

## SDK API 参考

分类快速跳转：

- [聊天室](#聊天室)
- [红包](#红包)
- [用户](#用户)
- [私聊](#私聊)
- [文章](#文章)
- [表情](#表情)
- [清风明月](#清风明月)
- [通知](#通知)

### 聊天室

| 方法 | 参数 | 返回 |
|------|------|------|
| `sendChatRoomMessage` | `content: string` | `null` |
| `revokeChatRoomMessage` | `id: string` | `{ msg }` |
| `reactChatRoomMessage` | `id: string, value: string` | `ChatReactionUpdate` |
| `getChatRoomHistory` | `page: number, selfUsername: string` | `ChatRoomMessage[]` |
| `uploadChatFile` | `filePath: string` | `UploadedChatFile` |
| `searchAtUsers` | `query: string` | `string[]` |

`sendChatRoomMessage` 会自动使用当前插件通过 `fishpi.chat.setClientType` 设置的发送身份；未设置时使用 App 默认发送身份。

`revokeChatRoomMessage`：

```json
{
  "msg": "撤回成功"
}
```

`reactChatRoomMessage`：

```json
{
  "targetId": "1778462350466",
  "targetType": "chat",
  "groupType": "emoji",
  "currentUserReaction": "thumbsup",
  "summary": [
    {
      "value": "thumbsup",
      "emoji": "👍",
      "count": 1,
      "selected": true,
      "users": [],
      "userDetails": []
    }
  ]
}
```

`uploadChatFile`：

```json
{
  "filename": "image.png",
  "url": "https://file.fishpi.cn/...",
  "markdown": "![图片](https://file.fishpi.cn/...)"
}
```

视频文件会返回：

```json
{
  "filename": "video.mp4",
  "url": "https://file.fishpi.cn/...",
  "markdown": "[视频](https://file.fishpi.cn/...)"
}
```

### 红包

| 方法 | 参数 | 返回 |
|------|------|------|
| `openRedPacket` | `messageId: string, gesture: number` | `RedPacketOpenResult` |
| `sendRedPacket` | `type: string, money: number, count: number, message: string, receivers?: string[] \| string, gesture?: number` | `null` |

红包类型：`random`、`average`、`specify`、`heartbeat`、`rockPaperScissors`。

猜拳手势：`0` 石头，`1` 剪刀，`2` 布；普通红包或非猜拳红包可以不传或传 `-1`。

专属红包示例：

```javascript
fishpi.call('sendRedPacket', {
  type: 'specify',
  money: 32,
  count: 1,
  message: '专属摸鱼快乐',
  receivers: ['Kirito']
});
```

猜拳红包示例：

```javascript
fishpi.call('sendRedPacket', {
  type: 'rockPaperScissors',
  money: 256,
  count: 1,
  message: '猜拳见真章',
  gesture: 0
});
```

`openRedPacket`：

```json
{
  "info": {
    "count": 20,
    "gesture": null,
    "got": 3,
    "message": "摸鱼者，事竟成",
    "userName": "Kirito",
    "userAvatarURL": "https://..."
  },
  "receivers": [],
  "who": [
    {
      "userId": "123",
      "userName": "Kirito",
      "avatar": "https://...",
      "userMoney": 5,
      "time": "Mon May 11 09:19:11 CST 2026"
    }
  ]
}
```

### 用户

| 方法 | 参数 | 返回 |
|------|------|------|
| `getUser` | 无 | `FishPiUser` |
| `getUserProfile` | `userName: string` | `FishPiUser` |
| `getUserActivity` | 无 | `UserActivity` |
| `rewardLiveness` | 无 | `{ sum }` |
| `getUserMedals` | `userName: string` | `Medal[]` |

`FishPiUser`：

```json
{
  "userName": "Kirito",
  "userNickname": "只有午安",
  "userAvatarURL": "https://...",
  "role": "黑客",
  "userNo": "10086",
  "intro": "摸鱼中",
  "city": "上海",
  "url": "https://fishpi.cn/member/Kirito",
  "points": 1024,
  "following": 12,
  "follower": 34,
  "onlineMinutes": 5678
}
```

`getUserActivity`：

```json
{
  "liveness": 14,
  "checkedIn": true,
  "livenessRewarded": false
}
```

`rewardLiveness`：

```json
{
  "sum": 20
}
```

`getUserMedals` 返回的原始勋章字段可能随服务端变化；Android UI 会主要使用以下字段：

```json
[
  {
    "medal_id": "1",
    "medal_name": "摸鱼达人",
    "txt": "摸鱼达人",
    "medal_attr": "backcolor=...&fontcolor=...&url=..."
  }
]
```

### 私聊

| 方法 | 参数 | 返回 |
|------|------|------|
| `sendPrivateChatMessage` | `peer: string, content: string` | `null` |
| `getPrivateChatSessions` | `selfUsername: string` | `PrivateChatSession[]` |
| `getPrivateChatHistory` | `peer: string, page: number, selfUsername: string` | `ChatRoomMessage[]` |
| `revokePrivateChatMessage` | `id: string` | `null` |
| `markPrivateChatRead` | `peer: string` | `null` |

`PrivateChatSession`：

```json
{
  "peer": "Kirito",
  "preview": "最近一条消息",
  "time": "Mon May 11 09:19:11 CST 2026",
  "avatar": "https://...",
  "unread": 2
}
```

私聊历史复用 `ChatRoomMessage` 结构，并额外可能包含：

```json
{
  "peer": "Kirito"
}
```

### 文章

| 方法 | 参数 | 返回 |
|------|------|------|
| `getArticles` | `filter: string, tag: string, page: number` | `ArticleListResult` |
| `getUserArticles` | `userName: string, page: number` | `ArticleListResult` |
| `getArticleDetail` | `articleId: string, page: number` | `ArticleDetail` |
| `sendArticleComment` | `articleId: string, content: string, replyId: string` | `null` |
| `voteArticle` | `articleId: string, like: boolean` | `null` |
| `thankArticle` | `articleId: string` | `null` |
| `followArticle` | `articleId: string` | `null` |
| `unfollowArticle` | `articleId: string` | `null` |
| `watchArticle` | `articleId: string` | `null` |

`filter` 常用值：`recent`、`hot`、`good`、`reply`、`long`、`perfect`；空值或未知值会按最近列表处理。

`ArticleListResult`：

```json
{
  "items": [
    {
      "id": "1778462350466",
      "title": "帖子标题",
      "author": "Kirito",
      "time": "刚刚",
      "tags": "Android,插件",
      "preview": "预览文本",
      "commentCount": 12,
      "goodCount": 8,
      "viewCount": 128,
      "sticky": false,
      "perfect": false,
      "avatar": "https://...",
      "thumbnail": "https://..."
    }
  ],
  "nextPage": 2,
  "hasMore": true
}
```

`ArticleDetail`：

```json
{
  "id": "1778462350466",
  "title": "帖子标题",
  "author": "Kirito",
  "avatar": "https://...",
  "time": "刚刚",
  "tags": "Android,插件",
  "content": "Markdown 或 HTML 正文",
  "contentHtml": "<p>正文</p>",
  "imageUrls": ["https://.../a.png"],
  "linkUrls": ["https://fishpi.cn"],
  "goodCount": 8,
  "badCount": 0,
  "thankCount": 1,
  "collectCount": 2,
  "watchCount": 3,
  "commentCount": 12,
  "viewCount": 128,
  "following": false,
  "watching": false,
  "thanked": false,
  "voteState": 0,
  "commentNextPage": 2,
  "commentHasMore": true,
  "comments": []
}
```

`ArticleComment`：

```json
{
  "id": "987654321",
  "author": "只有午安(Kirito)",
  "displayName": "只有午安",
  "userName": "Kirito",
  "time": "刚刚",
  "content": "<p>评论内容</p>",
  "imageUrls": [],
  "linkUrls": [],
  "goodCount": 1,
  "badCount": 0,
  "thankCount": 0,
  "voteState": 0,
  "thanked": false,
  "replyId": "",
  "avatar": "https://..."
}
```

`voteState`：`1` 已赞同，`-1` 已反对，`0` 未投票。

发送顶级评论：

```javascript
fishpi.call('sendArticleComment', {
  articleId: '1778462350466',
  content: '写得很好',
  replyId: ''
});
```

回复评论：

```javascript
fishpi.call('sendArticleComment', {
  articleId: '1778462350466',
  content: '同意你的观点',
  replyId: '987654321'
});
```

### 表情

| 方法 | 参数 | 返回 |
|------|------|------|
| `getEmojiGroups` | 无 | `EmojiGroup[]` |
| `getEmojiGroupItems` | `groupId: string` | `EmojiItem[]` |

`EmojiGroup`：

```json
{
  "id": "group-id",
  "name": "默认",
  "sort": 0,
  "isDefault": true,
  "count": 128
}
```

`EmojiItem`：

```json
{
  "id": "emoji-id",
  "groupId": "group-id",
  "name": "摸鱼",
  "url": "https://file.fishpi.cn/emoji.gif",
  "sort": 0
}
```

### 清风明月

| 方法 | 参数 | 返回 |
|------|------|------|
| `sendBreezemoon` | `content: string` | `null` |
| `getBreezemoons` | `page: number, size: number` | `Breezemoon[]` |
| `getUserBreezemoons` | `userName: string, page: number, size: number` | `Breezemoon[]` |

`Breezemoon`：

```json
{
  "id": "1778462350466",
  "authorName": "Kirito",
  "updated": "2026-05-11 09:19:11",
  "created": "2026-05-11 09:19:11",
  "timeAgo": "刚刚",
  "content": "清风明月正文",
  "createTime": "2026-05-11 09:19:11",
  "city": "上海",
  "avatar": "https://..."
}
```

### 通知

| 方法 | 参数 | 返回 |
|------|------|------|
| `getNoticeUnreadCount` | 无 | `NoticeUnreadCount` |
| `getNotices` | 无 | `NoticeItem[]` |
| `markAllNoticesRead` | 无 | `null` |

`NoticeUnreadCount`：

```json
{
  "total": 3,
  "reply": 1,
  "point": 0,
  "at": 1,
  "broadcast": 0,
  "system": 0,
  "following": 1,
  "commented": 0,
  "newFollower": 0
}
```

`NoticeItem`：

```json
{
  "id": "1778462350466",
  "category": "回复",
  "author": "Kirito",
  "title": "帖子标题",
  "content": "通知内容，可能包含 HTML",
  "dataType": 13,
  "time": "Mon May 11 09:19:11 CST 2026",
  "read": false,
  "jumpType": "article",
  "jumpId": "1778462350466",
  "mentionUser": ""
}
```

`category` 常见值：`积分`、`评论`、`回复`、`@`、`关注`、`系统`。

`jumpType` 当前常见值：

| 值 | 说明 |
|----|------|
| `article` | 可跳转帖子，`jumpId` 为文章 ID |
| `chatroom` | 可跳转聊天室消息，`jumpId` 为消息 ID |
| 空字符串 | 无明确跳转目标 |

## 常用完整示例

### 红包助手 + 快捷动作

```javascript
// ==FishPiPlugin==
// @name         快捷红包助手
// @author       你的名字
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==

storage.get('skipTypes', ['rockPaperScissors']);
storage.get('delaySec', 1.0);

fishpi.toolbar.register({
    id: 'quick-actions',
    title: '快捷助手',
    actions: [
        { id: 'hello', label: '问好', subtitle: '发送固定消息' },
        { id: 'points', label: '积分', subtitle: '查看当前积分' }
    ]
});

on('toolbarAction', function(action) {
    if (!action || action.entryId !== 'quick-actions') return;
    if (action.actionId === 'hello') {
        fishpi.call('sendChatRoomMessage', { content: '大家好，我来摸鱼了' });
    }
    if (action.actionId === 'points') {
        fishpi.call('getUser', {}).then(function(user) {
            if (user.ok === false) return ui.toast(user.error);
            ui.toast('当前积分：' + user.points);
        });
    }
});

on('message', function(msg) {
    if (msg.type !== 'redPacket') return;
    var rp = msg.redPacket;
    if (!rp || !rp.openable) return;

    var skip = storage.get('skipTypes', ['rockPaperScissors']);
    var delaySec = Number(storage.get('delaySec', 1.0));
    if (skip.indexOf(rp.type) >= 0) return;

    if (rp.type === 'specify' && userName) {
        if ((rp.receivers || []).indexOf(userName) < 0) return;
    }

    setTimeout(function() {
        fishpi.call('openRedPacket', { messageId: msg.oId, gesture: -1 }).then(function(r) {
            if (r.ok === false) {
                log('openRedPacket error: ' + r.error);
                return;
            }
            var me = r.who && r.who.length > 0 ? r.who[r.who.length - 1] : null;
            var got = me ? me.userMoney : 0;
            ui.toast('[红包助手] 抢到 ' + got + ' 积分');
        });
    }, Math.round(delaySec * 1000));
});
```

### 自动小尾巴

```javascript
// ==FishPiPlugin==
// @name         小尾巴
// @author       你的名字
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==

fishpi.hook('sendMessage', function(text) {
    return text + '\n\n来自 FishPi Android 插件';
});
```

### 关键词提示

```javascript
on('message', function(msg) {
    if (msg.content.indexOf('开会') >= 0) {
        ui.toast('检测到关键词：开会');
    }
});
```

## 插件管理

聊天室输入区 `+` 菜单中的“插件”进入插件管理；聊天室右上角“更多”里可以显示或隐藏插件快捷浮窗。

| 操作 | 说明 |
|------|------|
| 点击插件卡片 | 查看详情、状态、错误日志、最近调用记录 |
| 齿轮按钮 | 编辑插件 storage 配置 |
| 开关 | 启用或禁用插件 |
| 删除按钮 | 卸载插件文件 |
| 安装按钮 | 从本机选择 `.js` 插件文件复制到插件目录 |

默认示例插件会从 App assets 复制到 `/sdcard/fishpi/plugins/red-packet-assistant.js`。

## 调试

```bash
adb logcat -s FishPiPlugin:D
adb shell ls /sdcard/fishpi/plugins/
adb shell rm /sdcard/fishpi/plugins/xxx.js
```

常见问题：

| 现象 | 排查 |
|------|------|
| 插件没有显示 | 检查文件是否在 `/sdcard/fishpi/plugins/`，后缀是否为 `.js`，文件头是否包含 `@name` |
| 插件不触发 | 检查 `@scenes` 是否包含当前场景，当前聊天室场景为 `chatRoom` |
| API 返回 `{ ok:false }` | 查看 `error` 字段和 logcat |
| 快捷入口不显示 | 确认已调用 `fishpi.toolbar.register`，且 entry 的 `id/title` 不为空 |
| 设置页没有配置项 | 先在插件启动时调用一次 `storage.get(key, defaultValue)` 写入默认值 |
