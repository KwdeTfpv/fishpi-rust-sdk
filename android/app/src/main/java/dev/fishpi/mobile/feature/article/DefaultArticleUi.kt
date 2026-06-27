package dev.fishpi.mobile.feature.article

import dev.fishpi.mobile.ui.components.*
import dev.fishpi.mobile.ui.animal.AnimalIconButton
import dev.fishpi.mobile.ui.animal.AnimalStatusPill

import dev.fishpi.mobile.shared.message.previewableContentLinkUrls

import android.content.Context
import dev.fishpi.mobile.*
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.InsertEmoticon
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.chatui.ChatMarkdownRenderCache
import dev.fishpi.mobile.chatui.MarkwonContentRenderer
import dev.fishpi.mobile.chatui.MarkwonContentStyle
import dev.fishpi.mobile.data.ArticleCommentView
import dev.fishpi.mobile.data.ArticleDetailView
import dev.fishpi.mobile.data.ArticleDraftView
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.feature.article.model.ArticleFilterUiModel
import dev.fishpi.mobile.feature.article.publish.ArticlePublishAction
import dev.fishpi.mobile.feature.article.publish.ArticlePublishEditorTarget
import dev.fishpi.mobile.feature.article.publish.ArticlePublishState
import dev.fishpi.mobile.feature.article.publish.ArticleRecommendedTags
import dev.fishpi.mobile.utils.HtmlAnchorHrefRegex
import dev.fishpi.mobile.utils.MarkdownLinkRegex
import dev.fishpi.mobile.utils.MarkdownImageRegex
import dev.fishpi.mobile.utils.PlainUrlRegex
import dev.fishpi.mobile.utils.adaptiveComposeImageRatio
import dev.fishpi.mobile.utils.cleanImageSplitTextSegment
import dev.fishpi.mobile.utils.cleanMarkdownUrl
import dev.fishpi.mobile.utils.extractImageTokens
import dev.fishpi.mobile.utils.isAbsoluteWebUrl
import dev.fishpi.mobile.utils.isDirectVideoUrl
import dev.fishpi.mobile.utils.MarkdownMediaType
import dev.fishpi.mobile.utils.normalizeWebUrl
import dev.fishpi.mobile.utils.trimUrlPunctuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class ArticleFilter(val key: String, val label: String)

private val ArticlePinnedClearance = 38.dp

private val BaseArticleFilters = listOf(
    ArticleFilter("recent", "最新"),
    ArticleFilter("hot", "热门"),
    ArticleFilter("good", "点赞"),
    ArticleFilter("reply", "回复"),
)

private val RecentArticleFilters = BaseArticleFilters + listOf(
    ArticleFilter("long", "长篇"),
)

private val TaggedArticleFilters = BaseArticleFilters + listOf(
    ArticleFilter("perfect", "优选"),
)

private fun articleFiltersForTag(tag: String): List<ArticleFilter> =
    if (tag.isBlank()) RecentArticleFilters else TaggedArticleFilters

@Composable
@OptIn(FlowPreview::class)
internal fun DefaultArticleUi(
    state: ArticleState,
    dispatch: (ArticleAction) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.articles.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .debounce(120)
            .collect { last ->
                if (state.articles.isNotEmpty() && state.hasMore && last >= state.articles.lastIndex - 3) {
                    dispatch(ArticleAction.LoadMoreList)
                }
            }
    }

    if (state.selected == null) {
        ArticleListPage(
            articles = state.rawArticles,
            filter = ArticleFilter(state.filter.key, state.filter.label),
            tagInput = state.tagInput,
            appliedTag = state.appliedTag,
            isLoading = state.isLoading,
            isLoadingMore = state.isLoadingMore,
            error = state.error,
            listState = listState,
            onFilterChange = { dispatch(ArticleAction.ChangeFilter(ArticleFilterUiModel(it.key, it.label))) },
            onTagChange = { dispatch(ArticleAction.ChangeTagInput(it)) },
            onApplyTag = { dispatch(ArticleAction.ApplyTag) },
            onClearTag = { dispatch(ArticleAction.ClearTag) },
            onRefresh = { dispatch(ArticleAction.RefreshList) },
            onOpen = { dispatch(ArticleAction.OpenArticle(it)) },
            onCreateArticle = { dispatch(ArticleAction.OpenPublish) },
        )
    } else {
        ArticleDetailPage(
            summary = state.selected,
            detail = state.detail,
            articleHeat = state.articleHeat,
            commentInput = state.commentInput,
            replyTarget = state.replyTarget,
            replyFocusSignal = state.replyFocusSignal,
            dismissKeyboardSignal = state.dismissKeyboardSignal,
            isLoading = state.isLoadingDetail,
            isArticleActionRunning = state.isArticleActionRunning,
            isSendingComment = state.isSendingComment,
            error = state.error,
            onCommentInputChange = { dispatch(ArticleAction.ChangeCommentInput(it)) },
            onBack = {
                dispatch(ArticleAction.CloseDetail)
            },
            onVoteUp = { dispatch(ArticleAction.VoteUp) },
            onVoteDown = { dispatch(ArticleAction.VoteDown) },
            onThank = { dispatch(ArticleAction.Thank) },
            isRewarding = state.isRewarding,
            rewardConfirmOpen = state.rewardConfirmOpen,
            onRequestReward = { dispatch(ArticleAction.RequestRewardArticle) },
            onConfirmReward = { dispatch(ArticleAction.ConfirmRewardArticle) },
            onDismissRewardConfirm = { dispatch(ArticleAction.DismissRewardConfirm) },
            onFollow = { dispatch(ArticleAction.ToggleFollow) },
            onWatch = { dispatch(ArticleAction.Watch) },
            onLoadMoreComments = { dispatch(ArticleAction.LoadMoreComments) },
            onSendComment = { dispatch(ArticleAction.SendComment) },
            onImageClick = { dispatch(ArticleAction.ShowImagePreview(it)) },
            onLinkClick = { dispatch(ArticleAction.ShowLinkPreview(it)) },
            onVideoClick = { dispatch(ArticleAction.ShowVideoPreview(it)) },
            onOpenUserProfile = { dispatch(ArticleAction.OpenUserProfile(it)) },
            onReplyToComment = { dispatch(ArticleAction.ReplyToComment(it)) },
            onVoteCommentUp = { dispatch(ArticleAction.VoteCommentUp(it)) },
            onVoteCommentDown = { dispatch(ArticleAction.VoteCommentDown(it)) },
            onThankComment = { dispatch(ArticleAction.ThankComment(it)) },
            onCancelReply = { dispatch(ArticleAction.CancelReply) },
            emojiPanelOpen = state.emojiPanelOpen,
            emojiGroups = state.emojiGroups,
            emojiItems = state.emojiItems,
            selectedEmojiGroupId = state.selectedEmojiGroupId,
            isLoadingEmojiPack = state.isLoadingEmojiPack,
            emojiPackError = state.emojiPackError,
            isUploadingCommentImage = state.isUploadingCommentImage,
            onToggleEmojiPanel = { dispatch(ArticleAction.ToggleEmoji) },
            onDismissEmojiPanel = { dispatch(ArticleAction.DismissEmoji) },
            onPickEmojiGroup = { dispatch(ArticleAction.PickEmojiGroup(it)) },
            onPickEmoji = { dispatch(ArticleAction.PickEmoji(it)) },
            onPickCommentImage = { dispatch(ArticleAction.PickCommentImage) },
            onCaptureCommentImage = { dispatch(ArticleAction.CaptureCommentImage) },
            onShare = { dispatch(ArticleAction.ShareArticle) },
        )
    }

    BackHandler(enabled = state.selected != null) {
        dispatch(ArticleAction.CloseDetail)
    }
}

@Composable
private fun ArticleListPage(
    articles: List<ArticleSummary>,
    filter: ArticleFilter,
    tagInput: String,
    appliedTag: String,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onFilterChange: (ArticleFilter) -> Unit,
    onTagChange: (String) -> Unit,
    onApplyTag: () -> Unit,
    onClearTag: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (ArticleSummary) -> Unit,
    onCreateArticle: () -> Unit,
) {
    var showTagFilter by remember { mutableStateOf(false) }
    UiLayerScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            ControlSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FishPiTheme.spacingPage, vertical = FishPiTheme.spacingPage),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2)) {
                        Text(
                            text = "帖子",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem), verticalAlignment = Alignment.CenterVertically) {
                            AnimalStatusPill(
                                label = if (appliedTag.isBlank()) "全部帖子" else "#$appliedTag",
                                color = MaterialTheme.colorScheme.primary,
                            )
                            AnimalStatusPill(
                                label = "${articles.size} 条",
                                color = FishPiTheme.accent,
                            )
                        }
                    }
                    IconActionButton(
                        icon = Icons.Rounded.FilterAlt,
                        contentDescription = "标签筛选",
                        selected = showTagFilter || appliedTag.isNotBlank(),
                        onClick = { showTagFilter = !showTagFilter },
                    )
                    IconActionButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "刷新帖子",
                        onClick = onRefresh,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = FishPiTheme.spacingPage, vertical = FishPiTheme.spacingItem / 2),
                horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
            ) {
                articleFiltersForTag(appliedTag).forEach { item ->
                    val selected = item == filter
                    ActionChipButton(
                        text = item.label,
                        selected = selected,
                        onClick = { onFilterChange(item) },
                    )
                }
            }
            if (showTagFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FishPiTheme.spacingPage, vertical = FishPiTheme.spacingItem),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
                ) {
                    TextField(
                        value = tagInput,
                        onValueChange = onTagChange,
                        label = if (appliedTag.isBlank()) "标签筛选（可选）" else "当前标签：$appliedTag",
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FishPiPillButton(
                        text = "应用",
                        onClick = {
                            onApplyTag()
                            showTagFilter = false
                        },
                        compact = true,
                    )
                    if (appliedTag.isNotBlank()) {
                        FishPiPillButton(
                            text = "清空",
                            onClick = {
                                onClearTag()
                                showTagFilter = false
                            },
                            compact = true,
                        )
                    }
                }
            }
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = FishPiTheme.spacingPage))
            }
            when {
                isLoading && articles.isEmpty() -> LoadingScreen("加载帖子...")
                articles.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "暂无帖子")
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = FishPiTheme.spacingPage, vertical = FishPiTheme.spacingSection),
                    verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
                ) {
                    items(
                        items = articles,
                        key = { it.id },
                        contentType = { "article_row" },
                    ) { item ->
                        ArticleRow(item = item, onClick = { onOpen(item) })
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isLoadingMore) "正在加载更多..." else " ",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                            )
                        }
                    }
                }
            }
        }
        if (!listState.isScrollInProgress) {
            FloatingActionButton(
                onClick = onCreateArticle,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "发布帖子")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DefaultArticlePublishUi(
    state: ArticlePublishState,
    dispatch: (ArticlePublishAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(uiPageBrush())
            .imePadding(),
    ) {
        TopAppBar(
            title = { Text("发布帖子", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                PlainBackButton(onClick = { dispatch(ArticlePublishAction.Close) })
            },
            actions = {
                FishPiPillButton(
                    text = if (state.loadingDrafts) "草稿..." else "草稿(${state.drafts.size})",
                    onClick = { dispatch(ArticlePublishAction.OpenDrafts) },
                    enabled = !state.loadingDrafts && !state.submitting,
                    compact = true,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)),
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextField(
                    value = state.title,
                    onValueChange = { dispatch(ArticlePublishAction.ChangeTitle(it)) },
                    label = "标题",
                    placeholder = "输入帖子标题",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.titleError != null || state.title.length >= 255,
                    errorText = state.titleError,
                )
            }
            item {
                ArticlePublishSection(title = "正文") {
                    ArticleEditorToolbar(
                        uploadingImage = state.uploadingImage,
                        onImage = { dispatch(ArticlePublishAction.PickContentImage) },
                        onBold = { dispatch(ArticlePublishAction.InsertContentBlock("**加粗文字**")) },
                        onItalic = { dispatch(ArticlePublishAction.InsertContentBlock("*斜体文字*")) },
                        onTitle = { dispatch(ArticlePublishAction.InsertContentBlock("## 小标题")) },
                        onList = { dispatch(ArticlePublishAction.InsertContentBlock("- 列表项")) },
                        onLink = { dispatch(ArticlePublishAction.InsertContentBlock("[链接文字](https://)")) },
                        onPreview = { dispatch(ArticlePublishAction.ShowPreview(ArticlePublishEditorTarget.Content)) },
                    )
                    TextField(
                        value = state.content,
                        onValueChange = { dispatch(ArticlePublishAction.ChangeContent(it)) },
                        placeholder = "支持 Markdown，写点什么...",
                        minLines = 12,
                        maxLines = 18,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.contentError != null,
                        errorText = state.contentError,
                    )
                }
            }
            item {
                ArticlePublishSection(title = "标签") {
                    TextField(
                        value = state.tags,
                        onValueChange = { dispatch(ArticlePublishAction.ChangeTags(it)) },
                        label = "英文逗号分隔，最多 4 个",
                        placeholder = "摸鱼,日常",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.tagsError != null,
                        errorText = state.tagsError,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ArticleRecommendedTags.take(4).forEach { tag ->
                            SuggestionChip(
                                onClick = { dispatch(ArticlePublishAction.AppendTag(tag)) },
                                label = { Text(tag) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ArticleRecommendedTags.drop(4).forEach { tag ->
                            SuggestionChip(
                                onClick = { dispatch(ArticlePublishAction.AppendTag(tag)) },
                                label = { Text(tag) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
            item {
                ArticlePublishSection(title = "打赏区") {
                    ArticleEditorToolbar(
                        uploadingImage = state.uploadingImage,
                        onImage = { dispatch(ArticlePublishAction.PickRewardImage) },
                        onBold = { dispatch(ArticlePublishAction.InsertRewardBlock("**加粗文字**")) },
                        onItalic = { dispatch(ArticlePublishAction.InsertRewardBlock("*斜体文字*")) },
                        onTitle = { dispatch(ArticlePublishAction.InsertRewardBlock("## 小标题")) },
                        onList = { dispatch(ArticlePublishAction.InsertRewardBlock("- 列表项")) },
                        onLink = { dispatch(ArticlePublishAction.InsertRewardBlock("[链接文字](https://)")) },
                        onPreview = { dispatch(ArticlePublishAction.ShowPreview(ArticlePublishEditorTarget.Reward)) },
                    )
                    TextField(
                        value = state.rewardContent,
                        onValueChange = { dispatch(ArticlePublishAction.ChangeRewardContent(it)) },
                        label = "打赏可见内容（可选）",
                        placeholder = "支持 Markdown，这里的内容需要打赏后查看",
                        minLines = 8,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = state.rewardPoint,
                            onValueChange = { dispatch(ArticlePublishAction.ChangeRewardPoint(it)) },
                            label = "打赏积分",
                            placeholder = "0",
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextField(
                            value = state.qnaOfferPoint,
                            onValueChange = { dispatch(ArticlePublishAction.ChangeQnaOfferPoint(it)) },
                            label = "问答悬赏",
                            placeholder = "0",
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                ArticlePublishSection(title = "发布设置") {
                    ArticleSettingRow("在列表展示", state.showInList, onChange = { dispatch(ArticlePublishAction.ChangeShowInList(it)) })
                    ArticleSettingRow("允许评论", state.commentable, onChange = { dispatch(ArticlePublishAction.ChangeCommentable(it)) })
                    ArticleSettingRow("原创声明", state.originalStatement, onChange = { dispatch(ArticlePublishAction.ChangeOriginalStatement(it)) })
                    ArticleSettingRow("匿名发布", state.anonymous, onChange = { dispatch(ArticlePublishAction.ChangeAnonymous(it)) })
                    ArticleSettingRow("通知关注者", state.notifyFollowers, onChange = { dispatch(ArticlePublishAction.ChangeNotifyFollowers(it)) })
                }
            }
            state.error?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        ControlSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { dispatch(ArticlePublishAction.SaveDraft) },
                enabled = !state.submitting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("保存草稿")
            }
            Button(
                onClick = { dispatch(ArticlePublishAction.RequestPublish) },
                enabled = !state.submitting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("发布")
            }
        }
        }
    }

    if (state.showDrafts) {
        ArticleDraftDialog(
            drafts = state.drafts,
            onDismiss = { dispatch(ArticlePublishAction.DismissDrafts) },
            onOpen = { dispatch(ArticlePublishAction.OpenDraft(it.id)) },
            onDelete = { draft -> dispatch(ArticlePublishAction.DeleteDraft(draft.id)) },
        )
    }

    state.pendingPublishPayload?.let {
        GoodArticleConfirmDialog(
            onGoodArticle = { dispatch(ArticlePublishAction.ConfirmGoodArticlePublish) },
            onNormalArticle = { dispatch(ArticlePublishAction.ConfirmNormalPublish) },
            onDismiss = { dispatch(ArticlePublishAction.DismissGoodArticleConfirm) },
        )
    }

    if (state.showPreview) {
        val renderCache = remember { ChatMarkdownRenderCache() }
        val renderScope = rememberCoroutineScope()
        val previewSource = if (state.previewTarget == ArticlePublishEditorTarget.Reward) state.rewardContent else state.content
        AlertDialog(
            onDismissRequest = { dispatch(ArticlePublishAction.DismissPreview) },
            title = { Text(if (state.previewTarget == ArticlePublishEditorTarget.Reward) "打赏区预览" else "正文预览") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                    item {
                        ArticleMarkdownContent(
                            source = previewSource.ifBlank { "暂无内容" },
                            contentKey = "article-publish-preview|${state.previewTarget.name}",
                            renderCache = renderCache,
                            renderScope = renderScope,
                            textSizeSp = 15f,
                            lineSpacingMultiplier = 1.25f,
                            imageUrls = emptyList(),
                            linkUrls = emptyList(),
                            onImageClick = {},
                            onLinkClick = {},
                            onVideoClick = {},
                        )
                    }
                }
            },
            confirmButton = {
                FishPiPillButton(text = "关闭", onClick = { dispatch(ArticlePublishAction.DismissPreview) }, compact = true)
            },
        )
    }
}

@Composable
private fun GoodArticleConfirmDialog(
    onGoodArticle: () -> Unit,
    onNormalArticle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("是否申请好帖奖励？") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            TextView(context).apply {
                                setTextColor(textColor)
                                setLinkTextColor(linkColor)
                                textSize = 14f
                                setLineSpacing(0f, 1.12f)
                                movementMethod = LinkMovementMethod.getInstance()
                            }
                        },
                        update = { textView ->
                            textView.setTextColor(textColor)
                            textView.setLinkTextColor(linkColor)
                            textView.setText(Html.fromHtml(GoodArticleConfirmHtml, Html.FROM_HTML_MODE_LEGACY))
                        },
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onGoodArticle,
                    modifier = Modifier.widthIn(max = 280.dp),
                ) {
                    Text("这是好帖，给我奖励！")
                }
                TextButton(
                    onClick = onNormalArticle,
                    modifier = Modifier.widthIn(max = 280.dp),
                ) {
                    Text("我就随便写写，不需要奖励")
                }
            }
        },
    )
}

private const val GoodArticleConfirmHtml =
    "根据 <a href='https://fishpi.cn/article/1684378758315'>摸鱼派发帖规范细则</a>，请对您的帖子进行自觉分类，自行选择是否获得<a href='https://fishpi.cn/article/1683775497629'>好帖积分和活跃度奖励</a> (300积分和45%活跃度，每日仅第一次有效)<br><br>" +
        "<b>有积分奖励的帖子：</b>原创的技术文章 / 原创且用心的美食、旅游、生活内容 / 发自内心的分享 / 有营养的问答 / 原创且用心的长短篇小说、故事、纪实创作<br><br>" +
        "<b>没有积分奖励的帖子：</b>发牢骚、感慨 / 非原创的内容 / 无意义内容帖、单纯的水帖 / 新人报道帖 / 广告帖 / 不用心、无价值的长短篇小说、故事、纪实创作<br><br>" +
        "<b>请注意：</b><b>帖子发布后24小时内无法删除！</b>如果不知道该选哪个，请选“我就随便写写”，选择领取奖励后，我们将对帖子进行检查，如不符合规则，您的积分奖励将会被扣除。"

@Composable
private fun ArticlePublishSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ContentCardSurface(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
    ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
    }
}

@Composable
private fun ArticleEditorToolbar(
    uploadingImage: Boolean,
    onImage: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onTitle: () -> Unit,
    onList: () -> Unit,
    onLink: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArticleToolbarButton(Icons.Rounded.BrokenImage, if (uploadingImage) "上传中" else "插入图片", onImage)
        ArticleToolbarButton(Icons.Rounded.FormatBold, "加粗", onBold)
        ArticleToolbarButton(Icons.Rounded.FormatItalic, "斜体", onItalic)
        ArticleToolbarButton(Icons.Rounded.Title, "标题", onTitle)
        ArticleToolbarButton(Icons.AutoMirrored.Rounded.FormatListBulleted, "列表", onList)
        ArticleToolbarButton(Icons.Rounded.Link, "链接", onLink)
        Spacer(modifier = Modifier.weight(1f))
        ArticleToolbarButton(Icons.Rounded.Preview, "预览", onPreview)
    }
}

@Composable
private fun ArticleToolbarButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    FishPiIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        sizeDp = 34,
        iconSizeDp = 19,
        background = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun ArticleSettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ArticleDraftDialog(
    drafts: List<ArticleDraftView>,
    onDismiss: () -> Unit,
    onOpen: (ArticleDraftView) -> Unit,
    onDelete: (ArticleDraftView) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("草稿") },
        text = {
            if (drafts.isEmpty()) {
                Text("暂无草稿", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(drafts, key = { it.id }) { draft ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpen(draft) }
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = draft.title.ifBlank { "未命名草稿" },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                FishPiPillButton(
                                    text = "删除",
                                    onClick = { onDelete(draft) },
                                    danger = true,
                                    compact = true,
                                )
                            }
                            Text(
                                text = draft.summary.ifBlank { draft.tags.ifBlank { "没有摘要" } },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            FishPiPillButton(text = "关闭", onClick = onDismiss, compact = true)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleRow(item: ArticleSummary, onClick: () -> Unit) {
    ContentCardSurface(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = FishPiTheme.spacingSection,
                    top = FishPiTheme.spacingSection,
                    end = if (item.sticky) ArticlePinnedClearance else FishPiTheme.spacingSection,
                    bottom = FishPiTheme.spacingSection,
                ),
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection)) {
                ArticleAvatar(avatar = item.avatar, author = item.author, perfect = item.perfect)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem)) {
                        Text(text = item.author, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(text = item.time, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "评论 ${item.commentCount} · 赞 ${item.goodCount} · 浏览 ${item.viewCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem)) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.perfect) ArticleStatusIcon(
                        icon = Icons.Rounded.WorkspacePremium,
                        color = MaterialTheme.colorScheme.primary,
                        contentDescription = "优选",
                    )
                }
                if (item.preview.isNotBlank()) {
                    Text(
                        text = item.preview,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.tags.isNotBlank()) {
                    Text(
                        text = item.tags,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (item.sticky) {
            ArticlePinnedCornerBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun ArticleAvatar(
    avatar: String,
    author: String,
    perfect: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (perfect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = author.firstOrNull()?.toString() ?: "帖",
            color = if (perfect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        if (avatar.isNotBlank()) {
            SubcomposeAsyncImage(
                model = avatar,
                imageLoader = rememberFishPiImageLoader(),
                contentDescription = "${author.ifBlank { "作者" }}头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
            )
        }
    }
}

@Composable
private fun ArticleStatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color.copy(alpha = 0.76f),
        modifier = modifier.size(17.dp),
    )
}

@Composable
private fun ArticlePinnedCornerBadge(
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topEnd = FishPiTheme.radiusBox, bottomStart = FishPiTheme.radiusBox)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape)
            .padding(start = FishPiTheme.spacingItem, top = 6.dp, end = FishPiTheme.spacingItem, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = "置顶",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.62f),
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun ArticleDetailPage(
    summary: ArticleSummary,
    detail: ArticleDetailView?,
    articleHeat: Long?,
    commentInput: String,
    replyTarget: String?,
    replyFocusSignal: Int,
    dismissKeyboardSignal: Int,
    isLoading: Boolean,
    isArticleActionRunning: Boolean,
    isSendingComment: Boolean,
    error: String?,
    onCommentInputChange: (String) -> Unit,
    onBack: () -> Unit,
    onVoteUp: () -> Unit,
    onVoteDown: () -> Unit,
    onThank: () -> Unit,
    isRewarding: Boolean,
    rewardConfirmOpen: Boolean,
    onRequestReward: () -> Unit,
    onConfirmReward: () -> Unit,
    onDismissRewardConfirm: () -> Unit,
    onFollow: () -> Unit,
    onWatch: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onSendComment: () -> Unit,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onReplyToComment: (ArticleCommentView) -> Unit,
    onVoteCommentUp: (ArticleCommentView) -> Unit,
    onVoteCommentDown: (ArticleCommentView) -> Unit,
    onThankComment: (ArticleCommentView) -> Unit,
    onCancelReply: () -> Unit,
    emojiPanelOpen: Boolean,
    emojiGroups: List<EmojiGroupView>,
    emojiItems: List<EmojiItemView>,
    selectedEmojiGroupId: String,
    isLoadingEmojiPack: Boolean,
    emojiPackError: String?,
    isUploadingCommentImage: Boolean,
    onToggleEmojiPanel: () -> Unit,
    onDismissEmojiPanel: () -> Unit,
    onPickEmojiGroup: (String) -> Unit,
    onPickEmoji: (EmojiItemView) -> Unit,
    onPickCommentImage: () -> Unit,
    onCaptureCommentImage: () -> Unit,
    onShare: () -> Unit,
) {
    val bodyListState = rememberLazyListState()
    val commentListState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(uiPageBrush())
            .imePadding(),
    ) {
        ControlSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlainBackButton(onClick = onBack)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = summary.title.ifBlank { "帖子详情" },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnimalStatusPill(
                            label = if (pagerState.currentPage == 0) "正文" else "评论",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        )
                        AnimalStatusPill(
                            label = "评论 ${detail?.commentCount ?: 0}",
                            color = FishPiTheme.accent,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        )
                    }
                }
                IconActionButton(
                    icon = Icons.Rounded.IosShare,
                    contentDescription = "分享",
                    onClick = onShare,
                )
            }
        }
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp))
        }
        when {
            isLoading && detail == null -> LoadingScreen("加载帖子详情...")
            detail == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "请选择帖子")
            }
            else -> Column(modifier = Modifier.fillMaxSize()) {
                val markdownCache = remember(detail.id) { ChatMarkdownRenderCache(maxEntries = 160, maxChars = 420_000) }
                val renderScope = remember(detail.id) { kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Default) }
                DisposableEffect(renderScope, markdownCache) {
                    onDispose {
                        renderScope.cancel()
                        markdownCache.clear()
                    }
                }
                val commentAuthorById = remember(detail.comments) {
                    detail.comments.associate { it.id to it.displayLabel() }
                }
                val commentById = remember(detail.comments) {
                    detail.comments.associateBy { it.id }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { page ->
                    if (page == 0) {
                        LazyColumn(
                            state = bodyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                ArticleReaderHeader(
                                    summary = summary,
                                    detail = detail,
                                    articleHeat = articleHeat,
                                    isActionRunning = isArticleActionRunning,
                                    onWatch = onWatch,
                                    onImageClick = onImageClick,
                                    onOpenUserProfile = onOpenUserProfile,
                                )
                            }
                            if (detail.rewardPoint > 0L || detail.rewardContent.isNotBlank() || detail.rewardedCount > 0L) {
                                item {
                                    ArticleRewardCard(
                                        detail = detail,
                                        renderCache = markdownCache,
                                        renderScope = renderScope,
                                        imageUrls = detail.imageUrls,
                                        linkUrls = detail.linkUrls,
                                        onImageClick = onImageClick,
                                        onLinkClick = onLinkClick,
                                        onVideoClick = onVideoClick,
                                        isActionRunning = isArticleActionRunning,
                                        isRewarding = isRewarding,
                                        rewardConfirmOpen = rewardConfirmOpen,
                                        onRequestReward = onRequestReward,
                                        onConfirmReward = onConfirmReward,
                                        onDismissRewardConfirm = onDismissRewardConfirm,
                                    )
                                }
                            }
                            item {
                                ArticleReaderBody(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    markdown = detail.markdown,
                                    imageUrls = detail.imageUrls,
                                    linkUrls = detail.linkUrls,
                                    heroImage = articleHeroImage(summary, detail),
                                    renderCache = markdownCache,
                                    renderScope = renderScope,
                                    onImageClick = onImageClick,
                                    onLinkClick = onLinkClick,
                                    onVideoClick = onVideoClick,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = commentListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "评论",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                    )
                                    Text(
                                        text = detail.commentCount.toString(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            items(detail.comments, key = { it.id }) { comment ->
                                ArticleCommentRow(
                                    comment = comment,
                                    replyTo = comment.replyId
                                        .takeIf { it.isNotBlank() }
                                        ?.let { replyId ->
                                            commentById[replyId]?.toArticleReplyPreview()
                                                ?: ArticleReplyPreview(
                                                    label = commentAuthorById[replyId] ?: "该条评论",
                                                    avatar = "",
                                                    preview = "查看上文回复关系",
                                                )
                                        },
                                    renderCache = markdownCache,
                                    renderScope = renderScope,
                                    onImageClick = onImageClick,
                                    onLinkClick = onLinkClick,
                                    onVideoClick = onVideoClick,
                                    onOpenUserProfile = onOpenUserProfile,
                                    onReply = {
                                        onReplyToComment(comment)
                                        scope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    },
                                    onVoteUp = { onVoteCommentUp(comment) },
                                    onVoteDown = { onVoteCommentDown(comment) },
                                    onThank = { onThankComment(comment) },
                                    isActionRunning = isArticleActionRunning,
                                )
                            }
                            if (detail.comments.isEmpty()) {
                                item {
                                    Text(
                                        text = "还没有评论",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (detail.commentHasMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (isLoading) "加载中..." else "加载更多评论",
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                                                .clickable(enabled = !isLoading, onClick = onLoadMoreComments)
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ArticleReaderBottomBar(
                    detail = detail,
                    value = commentInput,
                    replyTarget = replyTarget,
                    replyFocusSignal = replyFocusSignal,
                    dismissKeyboardSignal = dismissKeyboardSignal,
                    isSending = isSendingComment,
                    isActionRunning = isArticleActionRunning,
                    onValueChange = onCommentInputChange,
                    onSend = onSendComment,
                    onCancelReply = onCancelReply,
                    emojiPanelOpen = emojiPanelOpen,
                    emojiGroups = emojiGroups,
                    emojiItems = emojiItems,
                    selectedEmojiGroupId = selectedEmojiGroupId,
                    isLoadingEmojiPack = isLoadingEmojiPack,
                    emojiPackError = emojiPackError,
                    isUploadingCommentImage = isUploadingCommentImage,
                    onToggleEmojiPanel = onToggleEmojiPanel,
                    onDismissEmojiPanel = onDismissEmojiPanel,
                    onPickEmojiGroup = onPickEmojiGroup,
                    onPickEmoji = onPickEmoji,
                    onPickCommentImage = onPickCommentImage,
                    onCaptureCommentImage = onCaptureCommentImage,
                    onVoteUp = onVoteUp,
                    onThank = onThank,
                    onFollow = onFollow,
                    onCommentsClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                            commentListState.animateScrollToItem(0)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleReaderTopBar(
    onBack: () -> Unit,
    selectedPage: Int,
    onBodyClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onShare: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArticleReaderTab(text = "正文", selected = selectedPage == 0, onClick = onBodyClick)
                ArticleReaderTab(text = "评论", selected = selectedPage == 1, onClick = onCommentsClick)
            }
        },
        navigationIcon = {
            PlainBackButton(onClick = onBack, contentDescription = "返回帖子列表")
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.IosShare,
                    contentDescription = "分享帖子",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun ArticleReaderTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
private fun ArticleReaderHeader(
    summary: ArticleSummary,
    detail: ArticleDetailView,
    articleHeat: Long?,
    isActionRunning: Boolean,
    onWatch: () -> Unit,
    onImageClick: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        articleHeroImage(summary, detail)?.let { image ->
            ArticleHeroImage(url = image, onClick = { onImageClick(image) })
        }
        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = detail.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    modifier = Modifier.weight(1f),
                )
                ArticleHeatBadge(heat = articleHeat)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ArticleAvatar(
                    avatar = detail.avatar,
                    author = detail.author,
                    perfect = summary.perfect,
                    onClick = {
                        val username = detail.authorUserName.ifBlank { detail.author }
                        if (username.isNotBlank()) {
                            onOpenUserProfile(username)
                        }
                    },
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = detail.author.ifBlank { "鱼友" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (summary.perfect) ArticleStatusIcon(
                            icon = Icons.Rounded.WorkspacePremium,
                            color = MaterialTheme.colorScheme.primary,
                            contentDescription = "优选",
                        )
                    }
                    Text(
                        text = detail.time,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (detail.watching) "已关注" else "+ 关注",
                    color = if (detail.watching) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (detail.watching) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary)
                        .clickable(enabled = !isActionRunning, onClick = onWatch)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            ArticleTagRow(tags = detail.tags)
        }
    }
}

@Composable
private fun ArticleHeatBadge(heat: Long?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Visibility,
            contentDescription = "正在阅读人数",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = (heat ?: 0L).toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ArticleHeroImage(
    url: String,
    onClick: () -> Unit,
) {
    SubcomposeAsyncImage(
        model = url,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "帖子封面",
        contentScale = ContentScale.Crop,
        loading = { ArticleHeroImagePlaceholder("加载封面中...") },
        error = { ArticleHeroImagePlaceholder("封面加载失败") },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.86f)
            .heightIn(min = 190.dp, max = 285.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ArticleHeroImagePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ArticleTagRow(tags: String) {
    val tagList = remember(tags) {
        tags.split(",", "，", " ", "#")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
    }
    if (tagList.isEmpty()) {
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalOffer,
            contentDescription = "标签",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = tagList.joinToString(separator = "   "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.34f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            )
        }
    }
}

@Composable
private fun ArticleRewardCard(
    detail: ArticleDetailView,
    renderCache: ChatMarkdownRenderCache,
    renderScope: kotlinx.coroutines.CoroutineScope,
    imageUrls: List<String>,
    linkUrls: List<String>,
    isActionRunning: Boolean,
    isRewarding: Boolean,
    rewardConfirmOpen: Boolean,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onRequestReward: () -> Unit,
    onConfirmReward: () -> Unit,
    onDismissRewardConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = FishPiTheme.spacingPage)
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(FishPiTheme.radiusBox))
            .padding(FishPiTheme.spacingSection),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Rounded.AttachMoney,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "打赏区",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${detail.rewardPoint} 积分 · ${detail.rewardedCount} 人已打赏",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (detail.rewarded && detail.rewardContent.isNotBlank()) {
            val source = remember(detail.rewardContent, imageUrls) {
                detail.rewardContent.trim().withResolvedArticleImageUrls(imageUrls)
            }
            ArticleMarkdownContent(
                source = source,
                contentKey = "article-reward|${detail.id}|${source.hashCode()}",
                renderCache = renderCache,
                renderScope = renderScope,
                textSizeSp = 16f,
                lineSpacingMultiplier = 1.32f,
                imageUrls = imageUrls,
                linkUrls = linkUrls,
                onImageClick = onImageClick,
                onLinkClick = onLinkClick,
                onVideoClick = onVideoClick,
            )
        } else {
            Text(
                text = if (detail.rewarded) "作者没有填写打赏可见内容。" else "打赏后可查看作者设置的隐藏内容。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!detail.rewarded && detail.rewardPoint > 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onRequestReward,
                        enabled = !isActionRunning && !isRewarding,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (isRewarding) "打赏中..." else "打赏 ${detail.rewardPoint} 积分查看")
                    }
                }
            }
        }
    }
    if (rewardConfirmOpen) {
        AlertDialog(
            onDismissRequest = onDismissRewardConfirm,
            title = { Text("确认打赏？") },
            text = {
                Text(
                    text = "确认打赏 ${detail.rewardPoint} 积分查看打赏区内容吗？",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmReward,
                    enabled = !isRewarding && !isActionRunning,
                ) {
                    Text("确认打赏")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRewardConfirm) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ArticleReaderBody(
    modifier: Modifier = Modifier,
    markdown: String,
    imageUrls: List<String>,
    linkUrls: List<String>,
    heroImage: String?,
    renderCache: ChatMarkdownRenderCache,
    renderScope: kotlinx.coroutines.CoroutineScope,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    val source = remember(markdown, imageUrls) { markdown.trim().withResolvedArticleImageUrls(imageUrls) }
    val contentKey = remember(source, heroImage) { "article-body|${heroImage.orEmpty().hashCode()}|${source.hashCode()}" }
    ArticleMarkdownContent(
        source = source,
        contentKey = contentKey,
        renderCache = renderCache,
        renderScope = renderScope,
        textSizeSp = 16f,
        lineSpacingMultiplier = 1.32f,
        modifier = modifier,
        imageUrls = imageUrls,
        linkUrls = linkUrls,
        onImageClick = onImageClick,
        onLinkClick = onLinkClick,
        onVideoClick = onVideoClick,
    )
}

@Composable
private fun ArticleReaderBottomBar(
    detail: ArticleDetailView,
    value: String,
    replyTarget: String?,
    replyFocusSignal: Int,
    dismissKeyboardSignal: Int,
    isSending: Boolean,
    isActionRunning: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit,
    emojiPanelOpen: Boolean,
    emojiGroups: List<EmojiGroupView>,
    emojiItems: List<EmojiItemView>,
    selectedEmojiGroupId: String,
    isLoadingEmojiPack: Boolean,
    emojiPackError: String?,
    isUploadingCommentImage: Boolean,
    onToggleEmojiPanel: () -> Unit,
    onDismissEmojiPanel: () -> Unit,
    onPickEmojiGroup: (String) -> Unit,
    onPickEmoji: (EmojiItemView) -> Unit,
    onPickCommentImage: () -> Unit,
    onCaptureCommentImage: () -> Unit,
    onVoteUp: () -> Unit,
    onThank: () -> Unit,
    onFollow: () -> Unit,
    onCommentsClick: () -> Unit,
) {
    var commentInputFocused by remember { mutableStateOf(false) }
    var commentEditValue by remember { mutableStateOf(TextFieldValue(value, selection = TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != commentEditValue.text) {
            commentEditValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }
    val commentText = commentEditValue.text
    val commentActive = commentInputFocused || commentText.isNotBlank() || !replyTarget.isNullOrBlank()
    val commentImagePreviewUrls = remember(commentText) {
        MarkdownImageRegex.findAll(commentText)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.cleanMarkdownUrl() }
            .filter(String::isNotBlank)
            .distinct()
            .toList()
    }
    ControlSurface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(
                horizontal = FishPiTheme.spacingPage,
                vertical = FishPiTheme.spacingItem * 0.75f,
            ),
        contentPadding = PaddingValues(FishPiTheme.spacingItem * 0.75f),
    ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (emojiPanelOpen) {
            EmojiPackPanel(
                groups = emojiGroups,
                emojiItems = emojiItems,
                selectedGroupId = selectedEmojiGroupId,
                isLoading = isLoadingEmojiPack,
                error = emojiPackError,
                onDismiss = onDismissEmojiPanel,
                onPickGroup = onPickEmojiGroup,
                onPickEmoji = onPickEmoji,
            )
        }
        if (commentImagePreviewUrls.isNotEmpty()) {
            ArticleCommentImagePreviewStrip(commentImagePreviewUrls)
        }
        if (!replyTarget.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FishPiTheme.radiusField))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f))
                    .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(FishPiTheme.radiusField))
                    .padding(
                        horizontal = FishPiTheme.spacingControl,
                        vertical = FishPiTheme.spacingControl * 0.64f,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "回复 $replyTarget",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "取消",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancelReply)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ArticleCompactCommentInput(
                value = commentEditValue,
                replyTarget = replyTarget,
                replyFocusSignal = replyFocusSignal,
                dismissKeyboardSignal = dismissKeyboardSignal,
                isSending = isSending,
                onValueChange = {
                    commentEditValue = it
                    onValueChange(it.text)
                },
                onSend = onSend,
                isUploadingCommentImage = isUploadingCommentImage,
                onToggleEmojiPanel = onToggleEmojiPanel,
                onPickCommentImage = onPickCommentImage,
                onCaptureCommentImage = onCaptureCommentImage,
                onFocusChanged = { commentInputFocused = it },
                showSendButton = commentActive,
                modifier = Modifier.weight(1f),
            )
            if (!commentInputFocused) {
                ArticleBottomAction(
                    icon = Icons.Rounded.ThumbUp,
                    value = detail.goodCount,
                    active = detail.voteState == 1,
                    enabled = !isActionRunning,
                    contentDescription = "点赞",
                    onClick = onVoteUp,
                )
                ArticleBottomAction(
                    icon = Icons.Rounded.Favorite,
                    value = detail.thankCount,
                    active = detail.thanked,
                    enabled = !isActionRunning && !detail.thanked,
                    contentDescription = "感谢",
                    onClick = onThank,
                )
                ArticleBottomAction(
                    icon = Icons.Rounded.Bookmark,
                    value = detail.collectCount,
                    active = detail.following,
                    enabled = !isActionRunning,
                    contentDescription = "收藏",
                    onClick = onFollow,
                )
                ArticleBottomAction(
                    icon = Icons.Rounded.ChatBubble,
                    value = detail.commentCount,
                    active = false,
                    enabled = true,
                    contentDescription = "评论",
                    onClick = onCommentsClick,
                )
            }
        }
    }
    }
}

@Composable
private fun ArticleCommentImagePreviewStrip(urls: List<String>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        contentPadding = PaddingValues(horizontal = FishPiTheme.spacingItem / 2),
    ) {
        itemsIndexed(urls, key = { index, url -> "$index-$url" }) { index, url ->
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                    .background(FishPiTheme.surfaceContainer)
                    .border(
                        FishPiTheme.borderWidth,
                        FishPiTheme.outline.copy(alpha = 0.16f),
                        RoundedCornerShape(FishPiTheme.radiusBox),
                    ),
            ) {
                SubcomposeAsyncImage(
                    model = url,
                    imageLoader = rememberFishPiImageLoader(),
                    contentDescription = "评论图片 ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("...", color = FishPiTheme.weakText, fontSize = 12.sp)
                        }
                    },
                    error = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(FishPiTheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BrokenImage,
                                contentDescription = null,
                                tint = FishPiTheme.weakText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )
                Text(
                    text = "${index + 1}",
                    color = FishPiTheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(FishPiTheme.surface.copy(alpha = 0.82f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun ArticleCompactCommentInput(
    value: TextFieldValue,
    replyTarget: String?,
    replyFocusSignal: Int,
    dismissKeyboardSignal: Int,
    isSending: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    isUploadingCommentImage: Boolean,
    onToggleEmojiPanel: () -> Unit,
    onPickCommentImage: () -> Unit,
    onCaptureCommentImage: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    showSendButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(replyFocusSignal) {
        if (replyFocusSignal > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(dismissKeyboardSignal) {
        if (dismissKeyboardSignal > 0) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(imeBottom) {
        if (imeBottom == 0) {
            focusManager.clearFocus(force = true)
            onFocusChanged(false)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ArticleCommentToolButton(
            icon = Icons.Rounded.InsertEmoticon,
            contentDescription = "打开表情包",
            enabled = !isSending,
            onClick = onToggleEmojiPanel,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val inputTextStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 20.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                enabled = !isSending,
                singleLine = true,
                textStyle = inputTextStyle,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                    if (!isSending && value.text.isNotBlank()) onSend()
                }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 20.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.text.isBlank()) {
                            Text(
                                text = when {
                                    isSending -> "发送中..."
                                    isUploadingCommentImage -> "图片上传中..."
                                    !replyTarget.isNullOrBlank() -> "回复 $replyTarget"
                                    else -> "来说点什么吧"
                                },
                                style = inputTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        ArticleCommentToolButton(
            icon = Icons.Rounded.Add,
            contentDescription = "上传图片",
            enabled = !isSending,
            onClick = onPickCommentImage,
            onLongClick = onCaptureCommentImage,
        )
        if (showSendButton) {
            ArticleCommentToolButton(
                icon = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "发送评论",
                enabled = !isSending && value.text.isNotBlank(),
                prominent = true,
                onClick = onSend,
            )
        }
    }
}

@Composable
private fun ArticleCommentToolButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    prominent: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.44f)
        prominent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val background = when {
        !prominent -> Color.Transparent
        !enabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.54f)
        prominent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f)
    }
    val borderColor = if (prominent && enabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
    } else {
        Color.Transparent
    }
    val shape = RoundedCornerShape(FishPiTheme.radiusField)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(background)
            .border(
                if (prominent) FishPiTheme.borderWidth else 0.dp,
                borderColor,
                shape,
            )
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ArticleBottomAction(
    icon: ImageVector,
    value: Long,
    active: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = value.toString(),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

private fun articleCommentsAnchorIndex(detail: ArticleDetailView): Int = 2

private fun articleHeroImage(summary: ArticleSummary, detail: ArticleDetailView): String? =
    detail.imageUrls.firstOrNull()?.ifBlank { null }

private fun ArticleCommentView.displayLabel(): String {
    val display = displayName.ifBlank { author }.trim()
    val username = userName.trim()
    return if (display.isNotBlank() && username.isNotBlank() && !display.equals(username, ignoreCase = true)) {
        "$display($username)"
    } else {
        display.ifBlank { username.ifBlank { "鱼友" } }
    }
}

private fun ArticleCommentView.toArticleReplyPreview(): ArticleReplyPreview =
    ArticleReplyPreview(
        label = displayLabel(),
        avatar = avatar,
        preview = content
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "图片或富文本评论" },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleCommentRow(
    comment: ArticleCommentView,
    replyTo: ArticleReplyPreview?,
    renderCache: ChatMarkdownRenderCache,
    renderScope: kotlinx.coroutines.CoroutineScope,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onReply: () -> Unit,
    onVoteUp: () -> Unit,
    onVoteDown: () -> Unit,
    onThank: () -> Unit,
    isActionRunning: Boolean,
) {
    val profileName = comment.userName.ifBlank { comment.author }.trim()
    val profileClick = if (profileName.isNotBlank()) {
        Modifier.clickable { onOpenUserProfile(profileName) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
            .padding(FishPiTheme.spacingSection),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .then(profileClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = comment.author.firstOrNull()?.toString() ?: "评",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (comment.avatar.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = comment.avatar,
                        imageLoader = rememberFishPiImageLoader(),
                        contentDescription = "${comment.displayLabel()}头像",
                        contentScale = ContentScale.Crop,
                        error = {},
                        loading = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.displayLabel(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .then(profileClick),
                    )
                    Text(text = comment.time, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                replyTo?.let {
                    ArticleReplyContext(reply = it)
                }
                ArticleMarkdownContent(
                    source = comment.content.withResolvedArticleImageUrls(comment.imageUrls),
                    contentKey = "article-comment|${comment.id}|${comment.content.hashCode()}|${comment.imageUrls.hashCode()}",
                    renderCache = renderCache,
                    renderScope = renderScope,
                    textSizeSp = 14.5f,
                    lineSpacingMultiplier = 1.26f,
                    imageUrls = comment.imageUrls,
                    linkUrls = comment.linkUrls,
                    modifier = Modifier.padding(top = 1.dp),
                    onImageClick = onImageClick,
                    onLinkClick = onLinkClick,
                    onVideoClick = onVideoClick,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ArticleCommentAction(
                        icon = Icons.Rounded.Favorite,
                        value = comment.thankCount,
                        active = comment.thanked,
                        enabled = !isActionRunning && !comment.thanked,
                        contentDescription = "感谢评论",
                        onClick = onThank,
                    )
                    ArticleCommentAction(
                        icon = Icons.Rounded.ThumbUp,
                        value = comment.goodCount,
                        active = comment.voteState == 1,
                        enabled = !isActionRunning,
                        contentDescription = "点赞评论",
                        onClick = onVoteUp,
                    )
                    ArticleCommentAction(
                        icon = Icons.Rounded.ThumbDown,
                        value = comment.badCount,
                        active = comment.voteState == -1,
                        enabled = !isActionRunning,
                        contentDescription = "点踩评论",
                        onClick = onVoteDown,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Reply,
                        contentDescription = "回复",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onReply)
                            .padding(12.dp)
                            .size(20.dp),
                    )
        }
    }
}
}

private data class ArticleReplyPreview(
    val label: String,
    val avatar: String,
    val preview: String,
)

@Composable
private fun ArticleReplyContext(reply: ArticleReplyPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusField))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f), RoundedCornerShape(FishPiTheme.radiusField))
            .padding(
                horizontal = FishPiTheme.spacingControl,
                vertical = FishPiTheme.spacingControl * 0.72f,
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = reply.label.trim().take(1).ifBlank { "回" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            if (reply.avatar.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = reply.avatar,
                    imageLoader = rememberFishPiImageLoader(),
                    contentDescription = "${reply.label}头像",
                    contentScale = ContentScale.Crop,
                    error = {},
                    loading = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(width = 2.dp, height = 30.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "回复 @${reply.label}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reply.preview.ifBlank { "查看上文回复关系" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArticleMarkdownContent(
    source: String,
    contentKey: String,
    renderCache: ChatMarkdownRenderCache,
    renderScope: kotlinx.coroutines.CoroutineScope,
    textSizeSp: Float,
    lineSpacingMultiplier: Float,
    imageUrls: List<String>,
    linkUrls: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    val tokens = remember(source, imageUrls, linkUrls) {
        source.articleContentTokens(linkUrls = linkUrls, imageUrls = imageUrls)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tokens.forEachIndexed { index, token ->
            when (token) {
                is ArticleContentToken.Text -> ArticleMarkdownText(
                    source = token.markdown,
                    contentKey = "$contentKey|text$index",
                    renderCache = renderCache,
                    renderScope = renderScope,
                    textSizeSp = textSizeSp,
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    onLinkClick = onLinkClick,
                )
                is ArticleContentToken.Image -> ArticleInlineImage(
                    url = token.url,
                    onClick = { onImageClick(token.url) },
                )
                is ArticleContentToken.Video -> ArticleVideoCard(
                    url = token.url,
                    label = token.label,
                    onClick = { onVideoClick(token.url) },
                )
                is ArticleContentToken.LinkPreview -> ArticleLinkPreviewCard(
                    url = token.url,
                    onClick = { onLinkClick(token.url) },
                )
            }
        }
    }
}

@Composable
private fun ArticleInlineImage(
    url: String,
    onClick: () -> Unit,
) {
    var imageRatio by remember(url) { mutableStateOf(1.45f) }
    SubcomposeAsyncImage(
        model = url,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "帖子图片",
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            imageRatio = state.result.image.adaptiveComposeImageRatio(fallback = imageRatio)
        },
        loading = { ArticleHeroImagePlaceholder("加载图片中...") },
        error = { ArticleHeroImagePlaceholder("图片加载失败") },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(imageRatio)
            .heightIn(max = 520.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ArticleVideoCard(
    url: String,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label.ifBlank { "视频" },
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ArticleLinkPreviewCard(
    url: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.IosShare,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = url.toArticleLinkTitle(),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = url,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ArticleMarkdownText(
    source: String,
    contentKey: String,
    renderCache: ChatMarkdownRenderCache,
    renderScope: kotlinx.coroutines.CoroutineScope,
    textSizeSp: Float,
    lineSpacingMultiplier: Float,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val style = MarkwonContentStyle(
        textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
        weakTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        accentColor = MaterialTheme.colorScheme.primary.toArgb(),
        codeBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb(),
        textSizeSp = textSizeSp,
        lineSpacingMultiplier = lineSpacingMultiplier,
    )
    val renderer = remember(context, style, renderCache, renderScope, onLinkClick) {
        MarkwonContentRenderer(
            context = context,
            style = style,
            cache = renderCache,
            scope = renderScope,
            onLinkClick = onLinkClick,
            onMentionClick = { username -> onLinkClick("https://fishpi.cn/member/$username") },
        )
    }
    var renderJob by remember(contentKey) { mutableStateOf<Job?>(null) }
    DisposableEffect(contentKey) {
        onDispose {
            renderJob?.cancel()
            renderJob = null
        }
    }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { TextView(it) },
        update = { view ->
            renderJob?.cancel()
            renderJob = renderer.renderInto(
                textView = view,
                contentKey = contentKey,
                markdown = source,
            )
        },
        onRelease = { view ->
            renderJob?.cancel()
            renderJob = null
            renderer.clear(view)
        },
    )
}

@Composable
private fun ArticleCommentAction(
    icon: ImageVector,
    value: Long,
    active: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = value.toString(),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

private sealed interface ArticleContentToken {
    data class Text(val markdown: String) : ArticleContentToken
    data class Image(val url: String) : ArticleContentToken
    data class Video(val url: String, val label: String) : ArticleContentToken
    data class LinkPreview(val url: String) : ArticleContentToken
}

private data class ArticleRangeToken(
    val start: Int,
    val end: Int,
    val token: ArticleContentToken,
    val consumesSource: Boolean,
)

private fun String.articleContentTokens(
    linkUrls: List<String>,
    imageUrls: List<String>,
): List<ArticleContentToken> {
    if (isBlank()) return emptyList()
    val protectedRanges = fencedCodeRanges()
    val imageTokens = articleImageRangeTokens(protectedRanges)
    val videoTokens = articleVideoRangeTokens(protectedRanges)
    val renderedImages = imageTokens.mapNotNull { (it.token as? ArticleContentToken.Image)?.url }
    val previewUrls = previewableContentLinkUrls(
        content = this,
        linkUrls = linkUrls,
        imageUrls = imageUrls,
        renderableImageUrls = renderedImages,
    ).filterNot { it.isDirectVideoUrl() }
    val previewTokens = articlePreviewRangeTokens(previewUrls, protectedRanges)
    val rangeTokens = (imageTokens + videoTokens + previewTokens)
        .sortedWith(compareBy<ArticleRangeToken> { it.start }.thenBy { if (it.consumesSource) 0 else 1 })
    val output = mutableListOf<ArticleContentToken>()
    var cursor = 0
    rangeTokens.forEach { rangeToken ->
        if (rangeToken.start < cursor) return@forEach
        val before = substring(cursor, rangeToken.start).cleanImageSplitTextSegment()
        if (before.isNotBlank()) output += ArticleContentToken.Text(before)
        output += rangeToken.token
        cursor = if (rangeToken.consumesSource) rangeToken.end else rangeToken.start
    }
    val tail = substring(cursor).cleanImageSplitTextSegment()
    if (tail.isNotBlank()) output += ArticleContentToken.Text(tail)
    return output
}

private fun String.articleImageRangeTokens(protectedRanges: List<IntRange>): List<ArticleRangeToken> {
    return extractImageTokens()
        .mapNotNull { token ->
            if (token.start in protectedRanges) return@mapNotNull null
            if (token.type == MarkdownMediaType.Video) {
                return@mapNotNull ArticleRangeToken(
                    start = token.start,
                    end = token.end,
                    token = ArticleContentToken.Video(token.url.normalizeWebUrl(), "视频"),
                    consumesSource = true,
                )
            }
            ArticleRangeToken(
                start = token.start,
                end = token.end,
                token = ArticleContentToken.Image(token.url.normalizeWebUrl()),
                consumesSource = true,
            )
        }
        .sortedBy { it.start }
        .distinctBy { it.start to it.end }
        .toList()
}

private fun String.articleVideoRangeTokens(protectedRanges: List<IntRange>): List<ArticleRangeToken> {
    return MarkdownLinkRegex.findAll(this)
        .mapNotNull { match ->
            if (match.range.first in protectedRanges) return@mapNotNull null
            val label = match.value.substringAfter('[').substringBefore(']').trim()
            val url = match.groupValues.getOrNull(1).orEmpty().substringBefore(' ').normalizeWebUrl()
            if (!url.isDirectVideoUrl()) return@mapNotNull null
            ArticleRangeToken(
                start = match.range.first,
                end = match.range.last + 1,
                token = ArticleContentToken.Video(url, label.ifBlank { "视频" }),
                consumesSource = true,
            )
        }
        .sortedBy { it.start }
        .distinctBy { it.start to it.end }
        .toList()
}

private fun String.articlePreviewRangeTokens(
    previewUrls: List<String>,
    protectedRanges: List<IntRange>,
): List<ArticleRangeToken> {
    if (previewUrls.isEmpty()) return emptyList()
    val remaining = previewUrls.toMutableSet()
    val tokens = mutableListOf<ArticleRangeToken>()
    fun add(url: String, index: Int) {
        if (index < 0 || index in protectedRanges || !remaining.remove(url)) return
        tokens += ArticleRangeToken(index, index, ArticleContentToken.LinkPreview(url), false)
    }
    val htmlAnchorRanges = mutableListOf<IntRange>()
    HtmlAnchorHrefRegex.findAll(this).forEach { match ->
        val url = match.groupValues.getOrNull(2).orEmpty().normalizeWebUrl()
        htmlAnchorRanges += match.range
        if (url in remaining) add(url, match.range.last + 1)
    }
    MarkdownLinkRegex.findAll(this).forEach { match ->
        val url = match.groupValues.getOrNull(1).orEmpty().substringBefore(' ').normalizeWebUrl()
        if (url in remaining) add(url, match.range.last + 1)
    }
    PlainUrlRegex.findAll(this).forEach { match ->
        if (htmlAnchorRanges.any { match.range.first in it }) return@forEach
        val url = match.value.trimUrlPunctuation().normalizeWebUrl()
        if (url in remaining) add(url, match.range.last + 1)
    }
    return tokens.sortedBy { it.start }
}

private fun String.fencedCodeRanges(): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var fenceStart: Int? = null
    var lineStart = 0
    lineSequence().forEach { line ->
        if (line.trimStart().startsWith("```")) {
            val start = fenceStart
            if (start == null) {
                fenceStart = lineStart
            } else {
                ranges += start..(lineStart + line.length)
                fenceStart = null
            }
        }
        lineStart += line.length + 1
    }
    fenceStart?.let { ranges += it until length }
    return ranges
}

private operator fun List<IntRange>.contains(index: Int): Boolean = any { index in it }

private fun String.withResolvedArticleImageUrls(imageUrls: List<String>): String {
    if (isBlank() || imageUrls.isEmpty()) return this
    var index = 0
    return MarkdownImageRegex.replace(this) { match ->
        val raw = match.groupValues.getOrNull(1).orEmpty()
        val clean = raw.substringBefore(' ').cleanMarkdownUrl()
        val resolved = clean.takeIf(String::isAbsoluteWebUrl) ?: imageUrls.getOrNull(index)
        index++
        if (resolved.isNullOrBlank()) match.value else match.value.replace(raw, resolved)
    }
}

private fun String.toArticleLinkTitle(): String {
    return runCatching {
        val uri = java.net.URI(this)
        uri.host.orEmpty().removePrefix("www.").ifBlank { this }
    }.getOrDefault(this)
}

private fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

private fun Context.dpFloat(value: Int): Float =
    value * resources.displayMetrics.density

private fun ArticleDetailView.toSummary(): ArticleSummary =
    ArticleSummary(
        id = id,
        title = title,
        author = author,
        avatar = avatar,
        time = time,
        tags = tags,
        preview = markdown,
        commentCount = commentCount,
        goodCount = goodCount,
        viewCount = viewCount,
        sticky = false,
        perfect = false,
        thumbnail = imageUrls.firstOrNull().orEmpty(),
    )







