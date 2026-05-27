package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.InsertEmoticon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.UploadedChatFile
import dev.fishpi.mobile.plugin.PluginToolbarEntry
import dev.fishpi.mobile.shared.message.ChatQuote
import dev.fishpi.mobile.utils.appendDraftBlock

@Composable
internal fun ChatInputBar(
    input: String,
    quote: ChatQuote?,
    focusInputAfterQuote: Boolean,
    inputResetKey: Int,
    isUploadingAttachment: Boolean,
    pendingAttachments: List<UploadedChatFile>,
    atCandidates: List<String>,
    sendOnEnter: Boolean,
    emojiPanelOpen: Boolean,
    emojiGroups: List<EmojiGroupView>,
    emojiItems: List<EmojiItemView>,
    selectedEmojiGroupId: String,
    isLoadingEmojiPack: Boolean,
    emojiPackError: String?,
    toolPanelOpen: Boolean,
    toolActions: List<ChatToolAction>,
    pluginToolbarEntries: List<PluginToolbarEntry> = emptyList(),
    pluginToolbarDismissRequest: Int = 0,
    currentTopicLabel: String = "",
    onInputChange: (String) -> Unit,
    onCancelQuote: () -> Unit,
    onRemoveAttachment: (UploadedChatFile) -> Unit,
    onInputFocused: () -> Unit,
    onOpenTools: () -> Unit,
    onDismissToolPanel: () -> Unit,
    onPluginToolbarAction: (PluginToolbarEntry, String) -> Unit = { _, _ -> },
    onPickCurrentTopic: () -> Unit = {},
    onToggleEmojiPanel: () -> Unit,
    onDismissEmojiPanel: () -> Unit,
    onPickEmojiGroup: (String) -> Unit,
    onPickEmoji: (EmojiItemView) -> Unit,
    onPickAtUser: (String) -> Unit,
    onCursorPositionChange: (Int) -> Unit = {},
    inputCursorPositionRequest: Int? = null,
    onInputCursorPositionRequestHandled: () -> Unit = {},
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }
    var topicSelected by remember(currentTopicLabel) { mutableStateOf(false) }
    var textValue by remember(inputResetKey) {
        mutableStateOf(TextFieldValue(input, selection = TextRange(input.length)))
    }
    val inputTextStyle = androidx.compose.ui.text.TextStyle(
        color = FishPiTheme.onSurface,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
    LaunchedEffect(input) {
        if (input != textValue.text) {
            textValue = textValue.copy(text = input, selection = TextRange(input.length))
        }
    }
    LaunchedEffect(inputCursorPositionRequest) {
        val cursor = inputCursorPositionRequest ?: return@LaunchedEffect
        textValue = textValue.copy(selection = TextRange(cursor.coerceIn(0, textValue.text.length)))
        onInputCursorPositionRequestHandled()
    }
    LaunchedEffect(focusInputAfterQuote) {
        if (focusInputAfterQuote) {
            inputFocusRequester.requestFocus()
            keyboardController?.show()
            onInputFocused()
        }
    }
    fun dismissKeyboardForPanel() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    fun composedSendText(): String = textValue.text.withOptionalTopic(currentTopicLabel, topicSelected)
    fun submitComposer() {
        val nextText = composedSendText()
        if (nextText.isBlank()) return
        onSend(nextText)
        topicSelected = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (currentTopicLabel.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (topicSelected) FishPiTheme.accent.copy(alpha = 0.09f)
                            else composerContainerColor().copy(alpha = 0.56f)
                        )
                        .border(
                            1.dp,
                            if (topicSelected) FishPiTheme.accent.copy(alpha = 0.24f)
                            else composerBorderColor().copy(alpha = 0.72f),
                            RoundedCornerShape(15.dp),
                        )
                        .clickable {
                            topicSelected = !topicSelected
                            onPickCurrentTopic()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "#",
                        color = if (topicSelected) FishPiTheme.accent else FishPiTheme.weakText.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = currentTopicLabel.removePrefix("#").removeSuffix("#"),
                        color = if (topicSelected) FishPiTheme.accent else FishPiTheme.weakText,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        quote?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FishPiTheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${it.username}: ${it.preview}",
                    color = FishPiTheme.weakText,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                IconButton(onClick = onCancelQuote, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "取消引用", tint = FishPiTheme.weakText)
                }
            }
        }
        if (pendingAttachments.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingAttachments.forEach { file ->
                    Text(
                            text = file.filename.ifBlank { "附件" },
                        color = FishPiTheme.onSurface,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FishPiTheme.surfaceContainer)
                            .clickable { onRemoveAttachment(file) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
        if (atCandidates.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                atCandidates.forEach { username ->
                    Text(
                        text = "@$username",
                        color = FishPiTheme.accent,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(FishPiTheme.surfaceContainer)
                            .clickable { onPickAtUser(username) }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(composerRadius()),
            color = composerContainerColor(),
            border = androidx.compose.foundation.BorderStroke(1.dp, composerBorderColor()),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComposerIconButton(
                    icon = Icons.Rounded.InsertEmoticon,
                    contentDescription = "表情",
                    onClick = {
                        dismissKeyboardForPanel()
                        onToggleEmojiPanel()
                    },
                    enabled = !isUploadingAttachment,
                    selected = emojiPanelOpen,
                )
                BasicTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        onInputChange(it.text)
                        onCursorPositionChange(it.selection.start)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(inputFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onInputFocused()
                                onDismissEmojiPanel()
                                onDismissToolPanel()
                            }
                        }
                        .heightIn(min = 38.dp)
                        .padding(horizontal = 4.dp, vertical = 0.dp),
                    textStyle = inputTextStyle,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (sendOnEnter) submitComposer()
                    }),
                    maxLines = 4,
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (textValue.text.isBlank()) {
                                Text(
                                    text = if (isUploadingAttachment) "正在上传..." else "说点什么吧",
                                    color = FishPiTheme.weakText,
                                    style = inputTextStyle,
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    },
                )
                ComposerIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "更多",
                    onClick = {
                        dismissKeyboardForPanel()
                        onOpenTools()
                    },
                    enabled = !isUploadingAttachment,
                    selected = toolPanelOpen,
                )
                ComposerIconButton(
                    icon = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "发送",
                    onClick = { submitComposer() },
                    enabled = composedSendText().trim().isNotEmpty() && !isUploadingAttachment,
                    prominent = composedSendText().trim().isNotEmpty(),
                )
            }
        }
        if (emojiPanelOpen) {
            EmojiPackPanel(
                groups = emojiGroups,
                emojiItems = emojiItems,
                selectedGroupId = selectedEmojiGroupId,
                isLoading = isLoadingEmojiPack,
                error = emojiPackError,
                onDismiss = onDismissEmojiPanel,
                onPickGroup = onPickEmojiGroup,
                onPickEmoji = { item ->
                    val label = item.name.ifBlank { "表情" }
                    val nextText = appendDraftBlock(textValue.text, "![$label](${item.url})")
                    textValue = textValue.copy(
                        text = nextText,
                        selection = TextRange(nextText.length),
                    )
                    onPickEmoji(item)
                },
            )
        }
        if (toolPanelOpen) {
            AppToolGridPanel(actions = toolActions)
        }
    }
}

@Composable
private fun composerRadius() =
    22.dp

@Composable
private fun composerContainerColor() =
    FishPiTheme.surface.copy(alpha = 0.82f)

@Composable
private fun composerBorderColor() =
    FishPiTheme.outline.copy(alpha = 0.18f)

private fun String.withOptionalTopic(topic: String, selected: Boolean): String {
    val body = trimEnd()
    val reference = formatTopicReferenceMarkdown(topic)
    if (!selected || reference.isBlank()) return body
    if (body.endsWith(reference)) return body
    return if (body.isBlank()) reference else "$body\n$reference"
}

private fun formatTopicReferenceMarkdown(topic: String): String {
    val text = topic
        .trim()
        .removePrefix("*`")
        .removeSuffix("`*")
        .trim()
        .trim('#')
        .trim()
    return if (text.isBlank()) "" else "*`# $text #`*"
}

@Composable
private fun ComposerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    selected: Boolean = false,
    prominent: Boolean = false,
) {
    val radius = 16.dp
    val size = when {
        prominent -> 42.dp
        else -> 38.dp
    }
    val tint = when {
        !enabled -> FishPiTheme.weakText.copy(alpha = 0.45f)
        prominent -> androidx.compose.ui.graphics.Color.White
        selected -> FishPiTheme.accent
        else -> FishPiTheme.weakText
    }
    val bg = when {
        !enabled && prominent -> FishPiTheme.surfaceContainer.copy(alpha = 0.55f)
        !enabled -> androidx.compose.ui.graphics.Color.Transparent
        prominent -> FishPiTheme.accent
        selected -> FishPiTheme.accent.copy(alpha = 0.10f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val border = when {
        prominent && enabled -> FishPiTheme.accent.copy(alpha = 0.40f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(size)
            .shadow(if (prominent && enabled) 5.dp else 0.dp, RoundedCornerShape(radius), clip = false)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(radius))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}
