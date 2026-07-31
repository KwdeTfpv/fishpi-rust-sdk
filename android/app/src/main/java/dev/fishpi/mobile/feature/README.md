# FishPi 功能 UI 入口地图

这份文档给维护者、UI 作者和后续 AI 使用。目标很直接：想改某个界面时，先从这里找到入口文件，再进入对应区域文件。不要为了改布局、颜色、间距、文案样式去改 Controller 或 Route。

## 整体规则

- `State`：页面状态，是 UI 的唯一数据来源。
- `Action`：UI 发出的用户动作，例如发送、刷新、点击头像。
- `Effect`：一次性事件，例如 toast、分享、导航、打开系统 picker。
- `Controller`：业务逻辑、接口请求、native 调用、realtime、分页、缓存、错误处理。
- `Route`：页面进入/离开、生命周期、导航桥接、平台能力桥接。
- `DefaultUi`：默认 UI 组装入口，用户自定义 UI 时优先替换这里。
- `ui/` 或同 feature 下的区域文件：默认 UI 子组件。
- `ui/components/`：跨功能复用的视觉组件。
- `shared/message/`：聊天室和私聊共用消息渲染器。

UI 组件只能接收 state 切片、展示数据、`dispatch/onAction` 和简单 UI 参数。UI 组件不能直接调用 `FishPiApiClient`、`FishPiNative`、RealtimeClient、Repository、`SessionStore` 网络逻辑或 `PluginManager`。业务行为放 Controller，页面进出放 Route，复用视觉组件放 `ui/components`。

根目录 `dev.fishpi.mobile` 只保留 app-level 文件，例如 `MainActivity.kt`、`FishPiApp.kt`、`MainShell.kt`、`LoginScreen.kt`、`NoticeScreen.kt` 等。不要在根目录新增 feature 私有 UI、旧 Screen、ContentUi 或只转发到 Route/DefaultUi 的薄包装。

## 当前默认 UI 风格

默认 UI 已升级为“重 UI / 重视觉 / 软体验”方向：Compose 绘制的软插画场景、柔和渐层、浮动面板、圆润控件和更强的产品化首屏。旧信息流只作为内容来源，不再作为布局参考。

改 UI 时请优先保持这些约束：

- 不用大面积实心彩色 TopBar。
- 不把所有功能塞进右上角工具按钮。
- 不回到重信息流；首页、资料、插件、红包等页面应优先呈现场景、行动和轻分组。
- 不使用厚重阴影、强渐变、装饰 blob 或单一色相铺满页面。
- 聊天室和私聊以消息流为主，composer 是轻量上下文操作区。
- 帖子、清风明月、资料页以内容流和轻操作区为主。
- 红包可以保留红色业务氛围，但必须跟随主题和整体层级。

## 可切换 UI 风格

`FishPiTheme.kt` 里的 `FishPiThemePreset` 不只提供颜色。新增的 `FishPiUiStyle` 会改变布局密度、容器圆角、底部导航、首页信息组合、聊天室顶栏和 composer 形态。

- `Classic`：新的默认重 UI 风格，走软插画、柔和面板和低压信息密度。
- `Liquid Glass Dashboard`：半透明玻璃层级、浮动 dock、首页仪表盘式工作台、聊天室玻璃上下文栏。
- `Dark AI Workbench`：深色紧凑工作台、命令栏式底部导航、状态线、较小圆角和更高信息密度。

新增风格时不要只改 palette。需要在 `FishPiThemePreset.uiStyle()` 注册风格，并在 `MainShell.kt`、关键 `DefaultUi`、`ui/components` 里按 `FishPiTheme.uiStyle` 提供布局分支。Classic 分支是正式默认重 UI，Liquid 和 AI 可以在此基础上表达更强差异。

## App Shell / Common

- Shell 入口：`MainShell.kt`
- App session：`AppSession.kt`、`AppSessionLocal.kt`
- 主题：`FishPiTheme.kt`、`FishPiColors.kt`
- 图片加载：`FishPiImageLoader.kt`
- 通知/Toast island：`FishPiNotifier.kt`
- 公共按钮、输入框、加载/错误态、主题背景：`ui/components/CommonUi.kt`
- 公共聊天输入栏：`ui/components/ChatComposerBar.kt`
- Emoji 面板：`ui/components/EmojiPackPanel.kt`
- 公共工具面板：`ui/components/ToolPanels.kt`
- 公共底部 Sheet：`ui/components/AppBottomSheet.kt`
- 引用缩略图：`ui/components/QuoteThumbnail.kt`
- 用户资料通用组件：`ui/components/UserProfileComponents.kt`
- 静默点击工具：`ui/components/SilentTap.kt`
- 图片/文件选择：`ui/media/ChatAttachmentPicker.kt`、`ui/media/MediaStoreGalleryPicker.kt`
- 预览弹层：`ui/overlay/PreviewOverlays.kt`、`ui/overlay/VideoPlaybackOverlay.kt`

MainShell 只负责 shell、tab、全局导航、全局弹层和 feature route 挂载。不要把聊天室、帖子、资料、红包、插件的业务 UI 写回 MainShell。

## Home

- 默认 UI 入口：`feature/home/DefaultHomeUi.kt`
- Controller：`feature/home/HomeController.kt`
- Route：`feature/home/HomeRoute.kt`
- State/Action/Effect：`HomeState.kt`、`HomeAction.kt`、`HomeEffect.kt`
- 首页头部、quote、快捷入口：`HomeHeaderUi.kt`
- 活跃度、昨日活跃奖励：`HomeActivityUi.kt`
- 上下班设置：`HomeWorkSettingsUi.kt`
- 推荐帖子：`HomeRecommendedArticlesUi.kt`
- 视觉 token：`HomeVisualTokens.kt`
- 推荐帖子映射：`mapper/HomeArticleMapper.kt`

想改首页 UI，从 `DefaultHomeUi.kt` 开始。想只改活跃度卡片，就改 `HomeActivityUi.kt`。想改上下班时间设置，就改 `HomeWorkSettingsUi.kt`。不要改 `HomeController.kt` 或 `HomeRoute.kt` 做视觉调整。

## Chat Room

- 默认 UI 入口：`feature/chat/DefaultChatUi.kt`
- Controller：`feature/chat/ChatController.kt`
- Route：`feature/chat/ChatRoute.kt`
- State/Action/Effect：`ChatState.kt`、`ChatAction.kt`、`ChatEffect.kt`
- 聊天消息映射：`mapper/ChatMessageMapper.kt`
- composer/connection/overlay 状态模型：`model/`
- 聊天室活跃度浮窗：`ui/ChatLivenessFloatingOrb.kt`
- 聊天室上下文栏：`feature/chat/DefaultChatUi.kt`
- 输入栏：`ui/components/ChatComposerBar.kt`
- 消息列表：`shared/message/DefaultMessageListUi.kt` 或 `shared/message/native/NativeMessageList.kt`
- 消息长按菜单：`shared/message/ui/MessageActionBubbleMenu.kt`、`MessageActionSheet.kt`

聊天室顶部栏、连接状态、输入栏、浮窗、过滤设置等目前主要在 `DefaultChatUi.kt` 组装。消息行视觉到 shared message renderer 修改。聊天室业务、realtime、发送、红包请求、插件通知都不要写进 UI。

## Native Message Renderer

- 契约：`shared/message/MessageAction.kt`、`MessageRenderState.kt`
- 默认入口：`shared/message/DefaultMessageListUi.kt`
- Native list：`shared/message/native/NativeMessageList.kt`
- Native adapter：`shared/message/native/NativeMessageAdapter.kt`
- 滚动控制：`shared/message/native/NativeMessageListController.kt`
- Native 主题：`shared/message/native/NativeMessageTheme.kt`
- 旧 Chat list 过渡模型：`shared/message/ChatListModels.kt`
- 消息工具：`shared/message/ChatMessageUtils.kt`、`ChatQuote.kt`、`ChatReactions.kt`、`ChatRenderHints.kt`
- 消息操作 UI：`shared/message/ui/MessageActionSheet.kt`、`MessageActionBubbleMenu.kt`、`MessageActionSpec.kt`

Renderer 只上报图片点击、链接点击、头像点击、长按、reaction、红包点击等 UI 事件。Renderer 不能发消息、拉历史、改连接状态、调用接口或知道 ChatController / PrivateChatController。

## Private Chat

- 默认 UI 入口：`feature/privatechat/DefaultPrivateChatUi.kt`
- Controller：`feature/privatechat/PrivateChatController.kt`
- Route：`feature/privatechat/PrivateChatRoute.kt`
- State/Action/Effect：`PrivateChatState.kt`、`PrivateChatAction.kt`、`PrivateChatEffect.kt`
- 会话状态模型：`model/PrivateConversationState.kt`
- 最近联系人 UI 模型：`model/PrivateSessionUiModel.kt`
- 映射：`mapper/PrivateChatMapper.kt`
- 消息列表：复用 `shared/message`

想改最近联系人列表、私聊会话页、私聊输入栏，从 `DefaultPrivateChatUi.kt` 开始。发送、上传、realtime、unread、mark read 都归 Controller。

## Article

- 默认 UI 入口：`feature/article/DefaultArticleUi.kt`
- Controller：`feature/article/ArticleController.kt`
- Route：`feature/article/ArticleRoute.kt`
- State/Action/Effect：`ArticleState.kt`、`ArticleAction.kt`、`ArticleEffect.kt`
- 文章 UI 模型：`model/ArticleSummaryUiModel.kt`、`ArticleDetailUiModel.kt`、`ArticleFilterUiModel.kt`
- 文章 overlay：`model/ArticleOverlayState.kt`
- 映射：`mapper/ArticleMapper.kt`
- 发帖默认 UI：`feature/article/DefaultArticleUi.kt` 内的 `DefaultArticlePublishUi`
- 发帖 Controller：`feature/article/publish/ArticlePublishController.kt`
- 发帖 State/Action/Effect：`feature/article/publish/ArticlePublishState.kt`、`ArticlePublishAction.kt`、`ArticlePublishEffect.kt`

想改帖子列表、详情、评论、热度、正文区域，从 `DefaultArticleUi.kt` 进入。想改发帖页，需要看 `feature/article/publish/` 的 state/action/controller；当前发帖默认 UI 仍由文章 UI 入口组装。字段校验、草稿、发布、上传不要写进 UI。

## Profile

- 默认 UI 入口：`feature/profile/DefaultProfileUi.kt`
- Controller：`feature/profile/ProfileController.kt`
- Route：`feature/profile/ProfileRoute.kt`
- State/Action/Effect：`ProfileState.kt`、`ProfileAction.kt`、`ProfileEffect.kt`
- 通用用户资料组件：`ui/components/UserProfileComponents.kt`

想改“我的”、他人资料、头像按钮区、关注/私聊/转账按钮、徽章墙、帖子列表、转账 dialog，从 `DefaultProfileUi.kt` 开始。接口请求、关注、私聊跳转、转账、主题设置、账号切换归 Controller 或 Route。

## Breezemoon

- 默认 UI 入口：`feature/breezemoon/DefaultBreezemoonUi.kt`
- Controller：`feature/breezemoon/BreezemoonController.kt`
- Route：`feature/breezemoon/BreezemoonRoute.kt`
- State/Action/Effect：`BreezemoonState.kt`、`BreezemoonAction.kt`、`BreezemoonEffect.kt`

想改清风明月列表、输入栏、图片/Markdown/shortemoji 渲染入口、操作菜单、空状态，从 `DefaultBreezemoonUi.kt` 开始。load、send、upload、profile lookup 归 Controller。

## Red Packet

- 红包卡片：`feature/redpacket/DefaultRedPacketCard.kt`
- 发红包 UI：`feature/redpacket/DefaultRedPacketSendUi.kt`
- 猜拳选择：`feature/redpacket/DefaultRedPacketGestureUi.kt`
- 领取结果：`feature/redpacket/DefaultRedPacketResultUi.kt`
- 老样式承载：`feature/redpacket/RedPacketLegacyStyle.kt`
- State/Action：`RedPacketState.kt`、`RedPacketAction.kt`
- UI model：`RedPacketUiModel.kt`
- 映射：`RedPacketMapper.kt`
- 表单状态：`RedPacketFormState.kt`
- 表单校验：`RedPacketFormValidator.kt`
- 结果文案：`RedPacketResultLogic.kt`

红包是 shared feature，不是独立页面 Controller。发送、打开、领取结果请求仍由 ChatController 等 owning controller 负责。想恢复或调整红包视觉，优先看 `RedPacketLegacyStyle.kt` 和对应 `DefaultRedPacket*.kt`。

## Plugin UI

- 默认 UI 入口：`feature/pluginui/DefaultPluginUi.kt`
- Controller：`feature/pluginui/PluginUiController.kt`
- Route：`feature/pluginui/PluginUiRoute.kt`
- State/Action/Effect：`PluginUiState.kt`、`PluginUiAction.kt`、`PluginUiEffect.kt`
- 模型/映射/渲染：`PluginUiModel.kt`、`PluginUiMapper.kt`、`PluginUiRenderer.kt`
- 表单状态：`PluginFormState.kt`
- 插件运行桥：`plugin/PluginBridge.kt`
- 插件管理器：`plugin/PluginManager.kt`
- 插件沙箱：`plugin/PluginSandbox.kt`
- 插件列表 Sheet：`plugin/PluginListSheet.kt`
- 插件源码编辑：`plugin/PluginSourceEditorScreen.kt`
- 插件头部：`plugin/PluginHeader.kt`

Plugin UI 不依赖聊天室页面。聊天室刷新按钮旁边的插件入口会打开正式插件列表 Sheet；插件 toolbar action 仍走 PluginManager/Controller 的正式通道。不要用 WebView、自定义 HTML/CSS 或 Compose 内部对象渲染插件 UI。

## 常见自定义方式

### 替换聊天室 UI

```kotlin
@Composable
fun CustomChatUi(
    state: ChatState,
    dispatch: (ChatAction) -> Unit,
) {
    // 只读 state，只 dispatch action
}
```

然后在正式入口处把 `DefaultChatUi(state, dispatch)` 换成你的 UI。不要直接调用 `ChatController` 内部方法。

### 只改聊天室输入栏

改 `ui/components/ChatComposerBar.kt`，或者在 `feature/chat/DefaultChatUi.kt` 中替换输入栏组装。保留 `ChatAction.SendText`、`UploadAttachment`、`ToggleEmoji`、`PickEmoji`、`OpenRedPacketComposer` 等 dispatch。

### 只改帖子详情打赏区

从 `feature/article/DefaultArticleUi.kt` 进入，找到 reward/thank 相关 UI。打赏确认和接口调用继续走 `ArticleAction` 和 `ArticleController`。

### 只改资料页按钮区

从 `feature/profile/DefaultProfileUi.kt` 进入。按钮点击 dispatch `ProfileAction`，不要直接请求接口或修改 SessionStore。

### 新增一个自定义 UI 组件

建议放在当前 feature 的 `ui/` 目录，或者跨功能复用时放到 `ui/components/`。

```text
feature/example/
  ExampleState.kt
  ExampleAction.kt
  ExampleEffect.kt
  ExampleController.kt
  ExampleRoute.kt
  ui/
    ExampleDefaultUi.kt
    ExampleHeader.kt
    ExampleList.kt
    ExampleDialogs.kt
```

命名建议：

- `XxxDefaultUi.kt`
- `XxxTopBar.kt`
- `XxxInputBar.kt`
- `XxxList.kt`
- `XxxCard.kt`
- `XxxDialog.kt`
- `XxxSheet.kt`
- `XxxEmptyState.kt`

## 修改前检查清单

- 我只是改视觉吗？如果是，不要改 Controller。
- 我的组件是否只接收 state、展示数据、dispatch/onAction？
- 有没有直接调用 API、native、realtime、Repository、PluginManager？如果有，移回 Controller 或 Route。
- 有没有把消息列表、文章列表、用户资料、连接状态、红包结果存在 UI 的 `remember` 里？如果有，改回 State。
- 有没有新增根目录 feature UI 文件？如果有，移动到 feature 或 shared/ui 目录。
