package dev.fishpi.mobile.feature.breezemoon

import dev.fishpi.mobile.ui.components.silentTap

import dev.fishpi.mobile.ui.components.consumeTaps

import dev.fishpi.mobile.ui.components.EmojiPackPanel
import dev.fishpi.mobile.ui.components.ControlSurface
import dev.fishpi.mobile.ui.components.IconActionButton
import dev.fishpi.mobile.ui.components.UiLayerScaffold
import dev.fishpi.mobile.ui.components.statusSuccessColor

import android.graphics.Typeface
import dev.fishpi.mobile.*
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.InsertEmoticon
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.data.BreezemoonView
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.MedalView
import dev.fishpi.mobile.feature.chat.ChatUserProfileOverlay
import dev.fishpi.mobile.ui.components.ChatToolAction
import dev.fishpi.mobile.ui.components.AppToolGridPanel
import dev.fishpi.mobile.ui.animal.AnimalStatusPill
import dev.fishpi.mobile.utils.toFishPiMemberNameOrNull
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun DefaultBreezemoonUi(
    state: BreezemoonState,
    active: Boolean,
    dispatch: (BreezemoonAction) -> Unit,
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var composerHeight by remember { mutableStateOf(64.dp) }
    var listPinnedToBottom by remember { mutableStateOf(true) }
    val emojiPanelReserve = if (state.emojiPanelOpen) 300.dp else 0.dp
    val imeBottom = WindowInsets.ime.getBottom(density)

    LaunchedEffect(active) {
        if (active && state.items.isNotEmpty()) {
            listState.scrollToItem(state.items.lastIndex)
        }
    }

    LaunchedEffect(active, state.items.size, state.shouldScrollToBottom) {
        if (active && state.shouldScrollToBottom && state.items.isNotEmpty()) {
            listState.scrollToItem(state.items.lastIndex)
            dispatch(BreezemoonAction.ConsumeScrollToBottom)
        }
    }

    LaunchedEffect(listState, state.items.size) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val bottomThreshold = (state.items.lastIndex - 1).coerceAtLeast(0)
            state.items.isEmpty() || lastVisible >= bottomThreshold
        }
            .distinctUntilChanged()
            .collect { listPinnedToBottom = it }
    }

    LaunchedEffect(active, imeBottom, state.emojiPanelOpen, state.items.size, listPinnedToBottom) {
        if (active && listPinnedToBottom && state.items.isNotEmpty()) {
            listState.scrollToItem(state.items.lastIndex)
        }
    }

    LaunchedEffect(listState, state.items.size, state.hasMore, state.isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: Int.MAX_VALUE }
            .distinctUntilChanged()
            .collect { first ->
                if (state.items.isNotEmpty() && state.hasMore && !state.isLoading && first <= 3) {
                    dispatch(BreezemoonAction.LoadMore)
                }
            }
    }

    val breezemoonToolActions = listOf(
        ChatToolAction(
            id = "gallery",
            label = "相册",
            icon = Icons.Rounded.PhotoLibrary,
            enabled = !state.isSending && !state.isUploadingAttachment,
            onClick = { dispatch(BreezemoonAction.RequestGalleryAttachment) },
        ),
        ChatToolAction(
            id = "camera",
            label = "拍照",
            icon = Icons.Rounded.AddAPhoto,
            enabled = !state.isSending && !state.isUploadingAttachment,
            onClick = { dispatch(BreezemoonAction.RequestCameraAttachment) },
        ),
    )

    val accent = FishPiTheme.accent
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow

    UiLayerScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            ControlSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = FishPiTheme.spacingPage,
                        vertical = FishPiTheme.spacingItem,
                    ),
                shape = RoundedCornerShape(FishPiTheme.radiusField),
                contentPadding = PaddingValues(
                    horizontal = FishPiTheme.spacingControl,
                    vertical = FishPiTheme.spacingControl,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "清风明月",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AnimalStatusPill(
                                label = "${state.items.size} 条",
                                color = accent,
                            )
                            AnimalStatusPill(
                                label = if (state.isLoading) "同步中" else "已同步",
                                color = statusSuccessColor(),
                                leadingDot = true,
                            )
                        }
                    }
                    IconActionButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "刷新",
                        onClick = { dispatch(BreezemoonAction.Refresh) },
                        enabled = !state.isLoading,
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = FishPiTheme.spacingPage,
                    top = 4.dp,
                    end = FishPiTheme.spacingPage,
                    bottom = emojiPanelReserve + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
            ) {
                items(state.items, key = { it.id.ifBlank { it.createTime + it.authorName + it.content } }) { item ->
                    BreezemoonCard(
                        item = item,
                        accent = accent,
                        bg = cardBg,
                        onOpenUser = { dispatch(BreezemoonAction.OpenUserProfile(it)) },
                    )
                }
                if (state.isLoading) item { StatusText("加载中...") }
                else if (!state.hasMore && state.items.isNotEmpty()) item { StatusText("没有更多了") }
                state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> composerHeight = with(density) { size.height.toDp() } }
                    .padding(
                        horizontal = FishPiTheme.spacingPage,
                        vertical = FishPiTheme.spacingItem * 0.75f,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.75f),
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
                        .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f + FishPiTheme.depth * 0.08f), RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
                        .padding(
                            horizontal = FishPiTheme.spacingItem * 0.75f,
                            vertical = FishPiTheme.spacingItem * 0.64f,
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                dispatch(BreezemoonAction.CloseAttachmentPanel)
                                dispatch(BreezemoonAction.ToggleEmoji)
                            },
                            enabled = !state.isSending && !state.isUploadingAttachment,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.InsertEmoticon,
                                contentDescription = "打开表情包",
                                modifier = Modifier.size(21.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val inputTextStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        BasicTextField(
                            value = state.composeInput,
                            onValueChange = { dispatch(BreezemoonAction.ChangeInput(it)) },
                            enabled = !state.isSending && !state.isUploadingAttachment,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) dispatch(BreezemoonAction.CloseAttachmentPanel)
                                },
                            textStyle = inputTextStyle,
                            maxLines = 4,
                            decorationBox = { inner ->
                                if (state.composeInput.isBlank()) {
                                    Text(
                                        when {
                                            state.isUploadingAttachment -> "正在上传并插入..."
                                            state.isSending -> "发布中..."
                                            else -> "记录此刻"
                                        },
                                        style = inputTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    )
                                }
                                inner()
                            },
                        )
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                dispatch(BreezemoonAction.CloseEmoji)
                                dispatch(BreezemoonAction.ToggleAttachmentPanel)
                            },
                            enabled = !state.isSending && !state.isUploadingAttachment,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "打开附件菜单",
                                modifier = Modifier.size(21.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                val enabled = !state.isSending && !state.isUploadingAttachment && state.composeInput.trim().isNotEmpty()
                IconButton(onClick = { dispatch(BreezemoonAction.Publish) }, enabled = enabled, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.NightsStay,
                        "发布",
                        modifier = Modifier.size(22.dp),
                        tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.attachmentPanelOpen) {
                AppToolGridPanel(actions = breezemoonToolActions)
            }
        }

        if (state.emojiPanelOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .silentTap { dispatch(BreezemoonAction.CloseEmoji) },
                contentAlignment = Alignment.BottomCenter,
            ) {
                EmojiPackPanel(
                    groups = state.emojiGroups,
                    emojiItems = state.emojiItems,
                    selectedGroupId = state.selectedEmojiGroupId,
                    isLoading = state.isLoadingEmojiPack,
                    error = state.emojiPackError,
                    onDismiss = { dispatch(BreezemoonAction.CloseEmoji) },
                    onPickGroup = { dispatch(BreezemoonAction.SelectEmojiGroup(it)) },
                    onPickEmoji = { dispatch(BreezemoonAction.PickEmoji(it.name, it.url)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = FishPiTheme.spacingPage)
                        .padding(bottom = composerHeight + 8.dp)
                        .consumeTaps(),
                )
            }
        }
    }

    state.profileUsername?.let { username ->
        ChatUserProfileOverlay(
            username = username,
            user = state.profileUser,
            medals = state.profileMedals,
            isLoading = state.isLoadingProfile,
            error = state.profileError,
            onDismiss = { dispatch(BreezemoonAction.DismissUserProfile) },
            onRetry = { dispatch(BreezemoonAction.RetryUserProfile) },
        )
    }

    BackHandler(enabled = active && state.profileUsername != null) {
        dispatch(BreezemoonAction.DismissUserProfile)
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun BreezemoonCard(
    item: BreezemoonView,
    accent: Color,
    bg: Color,
    onOpenUser: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f + FishPiTheme.depth * 0.08f), RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
            .padding(FishPiTheme.spacingSection),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = item.avatar,
                imageLoader = rememberFishPiImageLoader(),
                contentDescription = "${item.authorName}头像",
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(FishPiTheme.radiusField)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.authorName.ifBlank { "鱼友" },
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(item.timeAgo, item.city.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
            ) {
                BreezemoonHtmlContent(item.content, accent, onOpenUser)
            }
        }
    }
}

@Composable
private fun BreezemoonHtmlContent(
    html: String,
    accent: Color,
    onOpenUser: (String) -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurface
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                textSize = 13f
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
                minHeight = 0
                minimumHeight = 0
                setLineSpacing(0f, 1.35f)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { view ->
            view.setTextColor(color.toArgb())
            val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            var s = 0
            var e = spanned.length
            while (s < e && spanned[s].isWhitespace()) s++
            while (e > s && spanned[e - 1].isWhitespace()) e--
            view.text = spanned.subSequence(s, e).withBreezemoonMemberLinks(accent.toArgb(), onOpenUser)
        },
    )
}

private fun CharSequence.withBreezemoonMemberLinks(
    accentColor: Int,
    onOpenUser: (String) -> Unit,
): SpannableStringBuilder {
    val spannable = SpannableStringBuilder(this)
    spannable.getSpans(0, spannable.length, URLSpan::class.java).forEach { span ->
        val username = span.url.toFishPiMemberNameOrNull(allowRelative = true) ?: return@forEach
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val flags = spannable.getSpanFlags(span).takeIf { it != 0 } ?: Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        spannable.removeSpan(span)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onOpenUser(username)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = accentColor
                    ds.typeface = Typeface.DEFAULT_BOLD
                    ds.isUnderlineText = false
                }
            },
            start,
            end,
            flags,
        )
        spannable.setSpan(ForegroundColorSpan(accentColor), start, end, flags)
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, flags)
    }
    return spannable
}





