package dev.fishpi.mobile.feature.redpacket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.InsertEmoticon
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.data.RedPacketGot
import dev.fishpi.mobile.data.RedPacketPreview
import dev.fishpi.mobile.FishPiErrorRed
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.ControlSurface
import dev.fishpi.mobile.ui.components.IconActionButton
import dev.fishpi.mobile.ui.components.PlainBackButton
import dev.fishpi.mobile.ui.components.uiPageBrush
import dev.fishpi.mobile.ui.components.consumeTaps
import dev.fishpi.mobile.ui.components.silentTap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

private val RedPacketTop = Color(0xFFE94E2F)
private val RedPacketBottom = Color(0xFFC92F23)
private val RedPacketSoftTop = Color(0xFFFF7A65)
private val RedPacketSoftBottom = Color(0xFFE94638)
private val RedPacketGold = Color(0xFFFFD57A)
private val RedPacketGoldDeep = Color(0xFFE8A93C)
private val RedPacketAccentSoft = Color(0xFFFFECE8)

private data class RedPacketPageColors(
    val background: Color,
    val surface: Color,
    val mutedSurface: Color,
    val text: Color,
    val secondaryText: Color,
    val divider: Color,
    val accentSoft: Color,
)

@Composable
private fun redPacketPageColors(): RedPacketPageColors {
    val isDark = FishPiTheme.background.luminance() < 0.5f
    return RedPacketPageColors(
        background = FishPiTheme.background,
        surface = FishPiTheme.surface,
        mutedSurface = FishPiTheme.surfaceContainer,
        text = FishPiTheme.onSurface,
        secondaryText = FishPiTheme.weakText,
        divider = FishPiTheme.outline.copy(alpha = if (isDark) 0.48f else 0.32f),
        accentSoft = if (isDark) RedPacketTop.copy(alpha = 0.18f) else RedPacketAccentSoft,
    )
}

@Composable
internal fun RedPacketCard(
    preview: RedPacketPreview,
    onClick: () -> Unit,
) {
    val status = when {
        preview.finished -> "已抢完"
        !preview.openable -> "不可领取"
        preview.needGesture -> "猜拳拆红包"
        else -> "拆红包"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        RedPacketSoftTop,
                        RedPacketBottom,
                        RedPacketSoftBottom,
                    ),
                ),
            )
            .border(FishPiTheme.borderWidth, RedPacketGold.copy(alpha = 0.32f), RoundedCornerShape(24.dp))
            .clickable(enabled = preview.openable, onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RedPacketSeal(openable = preview.openable)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preview.typeName.ifBlank { "鱼派红包" },
                        color = RedPacketGold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = preview.message.ifBlank { "摸鱼者，事竟成" },
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${preview.got}/${preview.count} 个 · ${preview.money} 积分",
                    color = Color.White.copy(alpha = 0.86f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(RedPacketGold.copy(alpha = if (preview.openable) 1f else 0.55f))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = status,
                        color = RedPacketBottom,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RedPacketSeal(
    openable: Boolean,
    sizeDp: Int = 54,
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (openable) RedPacketGold else RedPacketGoldDeep.copy(alpha = 0.68f),
                        Color.White.copy(alpha = 0.36f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "魚",
            color = RedPacketBottom,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun RedPacketGestureDialog(
    selectedGesture: Int? = null,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    RedPacketDialogFrame(onDismiss = onDismiss) {
        RedPacketCompactHeader(
            title = "猜拳红包",
            subtitle = "出拳",
            onDismiss = onDismiss,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RedPacketGesture.all.forEach { gesture ->
                RedPacketGestureOption(
                    gesture = gesture,
                    selected = selectedGesture == gesture.value,
                    modifier = Modifier.weight(1f),
                    onClick = { onPick(gesture.value) },
                )
            }
        }
    }
}

@Composable
internal fun RedPacketSendDialog(
    type: String,
    money: String,
    count: String,
    message: String,
    receivers: String,
    gesture: Int,
    balance: Long?,
    isSending: Boolean,
    onTypeChange: (String) -> Unit,
    onMoneyChange: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onReceiversChange: (String) -> Unit,
    onGestureChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    var typeSheetOpen by remember { mutableStateOf(false) }
    var gestureSheetOpen by remember { mutableStateOf(false) }
    val colors = redPacketPageColors()
    val selectedType = redPacketTypeOptions.firstOrNull { it.key == type } ?: redPacketTypeOptions.first()
    val selectedGesture = RedPacketGesture.all.firstOrNull { it.value == gesture } ?: RedPacketGesture.all.first()
    val moneyValue = money.toLongOrNull() ?: 0L
    val tax = if (type == "rockPaperScissors") (moneyValue * 5 + 99) / 100 else 0L
    val total = moneyValue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiPageBrush())
            .statusBarsPadding()
            .imePadding()
            .consumeTaps(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ControlSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainBackButton(onClick = onDismiss, contentDescription = "关闭发红包", tint = colors.secondaryText)
                Text(
                    text = "发红包",
                    color = colors.text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.size(40.dp))
            }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    RedPacketFormField(
                        label = "积分",
                        value = money,
                        placeholder = "填写积分",
                        enabled = !isSending,
                        onValueChange = { value -> onMoneyChange(value.filter(Char::isDigit)) },
                        keyboardType = KeyboardType.Number,
                        selectAllOnFocus = true,
                    )
                }
                item {
                    RedPacketFormField(
                        label = "留言",
                        value = message,
                        placeholder = when (type) {
                            "rockPaperScissors" -> "剪刀石头布!"
                            "specify" -> "看看是不是给你的"
                            else -> selectedType.subtitle
                        },
                        enabled = !isSending,
                        onValueChange = onMessageChange,
                        trailing = {
                            Icon(
                                imageVector = Icons.Rounded.InsertEmoticon,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(21.dp),
                            )
                        },
                    )
                }
                item {
                    RedPacketPickerRow(
                        label = "红包类型",
                        value = selectedType.label,
                        enabled = !isSending,
                        onClick = { typeSheetOpen = true },
                    )
                }
                if (type == "specify") {
                    item {
                        RedPacketFormField(
                            label = "接收者",
                            value = receivers,
                            placeholder = "多个用户名用空格或逗号分隔",
                            enabled = !isSending,
                            onValueChange = onReceiversChange,
                        )
                    }
                }
                if (type == "rockPaperScissors") {
                    item {
                        RedPacketPickerRow(
                            label = "我出",
                            value = selectedGesture.label,
                            enabled = !isSending,
                            onClick = { gestureSheetOpen = true },
                        )
                    }
                    item {
                        RedPacketReadonlyRow(label = "红包税", value = "5%（$tax 积分）")
                    }
                } else {
                    item {
                        RedPacketFormField(
                            label = "红包个数",
                            value = count,
                            placeholder = "填写个数",
                            enabled = !isSending,
                            onValueChange = { value -> onCountChange(value.filter(Char::isDigit)) },
                            suffix = "个",
                            keyboardType = KeyboardType.Number,
                            selectAllOnFocus = true,
                        )
                    }
                }
            }
            ControlSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "总积分：", color = colors.text.copy(alpha = 0.72f), fontSize = 15.sp)
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f", total.toDouble()),
                        color = RedPacketTop,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSending) RedPacketTop.copy(alpha = 0.56f) else Color(0xFFFF4650))
                        .clickable(enabled = !isSending, onClick = onSend)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSending) "发送中..." else "发送红包",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                balance?.let {
                    Text(text = "当前余额：$it 积分", color = colors.secondaryText, fontSize = 13.sp)
                }
                if (tax > 0) {
                    Text(
                        text = "总计：${String.format(java.util.Locale.US, "%.2f", total.toDouble())}（猜拳红包会收取5%税费，由系统在后端自动扣除）",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            }
        }
        if (typeSheetOpen) {
            RedPacketTypeSheet(
                selectedType = type,
                enabled = !isSending,
                onDismiss = { typeSheetOpen = false },
                onPick = {
                    typeSheetOpen = false
                    onTypeChange(it)
                },
            )
        }
        if (gestureSheetOpen) {
            RedPacketGestureSheet(
                selectedGesture = gesture,
                enabled = !isSending,
                onDismiss = { gestureSheetOpen = false },
                onPick = {
                    gestureSheetOpen = false
                    onGestureChange(it)
                },
            )
        }
    }
}

private data class RedPacketTypeOption(
    val key: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val redPacketTypeOptions = listOf(
    RedPacketTypeOption("random", "拼手气红包", "摸鱼者，事竟成！", Icons.Rounded.LocalAtm),
    RedPacketTypeOption("average", "平分红包", "平分红包，人人有份！", Icons.Rounded.Share),
    RedPacketTypeOption("specify", "专属红包", "试试看，这是给你的红包吗？", Icons.Rounded.Person),
    RedPacketTypeOption("heartbeat", "心跳红包", "玩的就是心跳！", Icons.Rounded.FavoriteBorder),
    RedPacketTypeOption("rockPaperScissors", "猜拳红包", "石头剪刀布！", Icons.Rounded.ContentCut),
)

@Composable
private fun RedPacketFormField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    selectAllOnFocus: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = redPacketPageColors()
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    var wasFocused by remember { mutableStateOf(false) }
    var replaceOnNextEdit by remember { mutableStateOf(false) }
    var focusBaseline by remember { mutableStateOf("") }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = label, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = fieldValue,
            onValueChange = { next ->
                if (replaceOnNextEdit && next.text != fieldValue.text) {
                    val replacement = firstFocusedEditReplacement(focusBaseline, next.text)
                    replaceOnNextEdit = false
                    fieldValue = TextFieldValue(replacement, selection = TextRange(replacement.length))
                    onValueChange(replacement)
                } else {
                    val textChanged = next.text != fieldValue.text
                    fieldValue = next
                    if (textChanged) {
                        replaceOnNextEdit = false
                    }
                    onValueChange(next.text)
                }
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { state ->
                    if (state.isFocused && !wasFocused && selectAllOnFocus && fieldValue.text.isNotEmpty()) {
                        focusBaseline = fieldValue.text
                        replaceOnNextEdit = true
                        fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                    } else if (!state.isFocused) {
                        replaceOnNextEdit = false
                        focusBaseline = ""
                    }
                    wasFocused = state.isFocused
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (fieldValue.text.isBlank()) {
                        Text(
                            text = placeholder,
                            color = colors.secondaryText,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        suffix?.let { Text(text = it, color = colors.text, fontSize = 16.sp) }
        trailing?.invoke()
    }
}

private fun firstFocusedEditReplacement(previous: String, next: String): String {
    if (previous.isEmpty() || next == previous) return next

    val maxPrefix = minOf(previous.length, next.length)
    var prefix = 0
    while (prefix < maxPrefix && previous[prefix] == next[prefix]) {
        prefix += 1
    }

    val maxSuffix = minOf(previous.length - prefix, next.length - prefix)
    var suffix = 0
    while (
        suffix < maxSuffix &&
        previous[previous.lastIndex - suffix] == next[next.lastIndex - suffix]
    ) {
        suffix += 1
    }

    val inserted = next.substring(prefix, next.length - suffix)
    return inserted.ifEmpty { next }
}

@Composable
private fun RedPacketPickerRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = redPacketPageColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = colors.text.copy(alpha = 0.68f), fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.secondaryText)
    }
}

@Composable
private fun RedPacketReadonlyRow(
    label: String,
    value: String,
) {
    val colors = redPacketPageColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = colors.text.copy(alpha = 0.68f), fontSize = 16.sp)
    }
}

@Composable
private fun RedPacketTypeSheet(
    selectedType: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    RedPacketBottomSheet(title = "红包类型", onDismiss = onDismiss) {
        redPacketTypeOptions.forEach { item ->
            RedPacketSheetOption(
                icon = item.icon,
                title = item.label,
                subtitle = item.subtitle,
                selected = item.key == selectedType,
                enabled = enabled,
                onClick = { onPick(item.key) },
            )
        }
    }
}

@Composable
private fun RedPacketGestureSheet(
    selectedGesture: Int,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val colors = redPacketPageColors()
    RedPacketBottomSheet(title = "猜拳选择", onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            RedPacketGesture.all.forEach { item ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = enabled) { onPick(item.value) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (item.value == selectedGesture) colors.accentSoft else colors.mutedSurface)
                            .border(
                                width = if (item.value == selectedGesture) 2.dp else 0.dp,
                                color = if (item.value == selectedGesture) RedPacketTop else Color.Transparent,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = legacyRedPacketGestureEmoji(item.value), fontSize = 30.sp)
                    }
                    Text(text = item.label, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RedPacketBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = redPacketPageColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .silentTap(onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(colors.surface)
                .navigationBarsPadding()
                .consumeTaps()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                FishPiIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭$title",
                    onClick = onDismiss,
                    tint = colors.secondaryText,
                    background = Color.Transparent,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
            content()
        }
    }
}

@Composable
private fun RedPacketSheetOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = redPacketPageColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = RedPacketTop, modifier = Modifier.size(30.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = colors.secondaryText, fontSize = 15.sp)
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = RedPacketTop, modifier = Modifier.size(26.dp))
        }
    }
}

private fun legacyRedPacketGestureEmoji(value: Int): String = when (value) {
    0 -> "✊"
    1 -> "✌️"
    2 -> "✋"
    else -> "?"
}

@Composable
internal fun RedPacketResultPage(
    message: String,
    count: Long,
    got: Long,
    gesture: Int? = null,
    who: List<RedPacketGot>,
    senderName: String,
    senderAvatar: String,
    packetMessage: String,
    selfUsername: String,
    finished: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = redPacketPageColors()
    val displayMessage = message.ifBlank { packetMessage }.ifBlank { "摸鱼者，事竟成" }
    val displayName = senderName.ifBlank { "鱼派用户" }
    val stateText = when {
        finished || (count > 0L && got >= count) -> "红包已被抢完"
        count > 0L -> "已领取 $got/$count 个"
        else -> "红包详情"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RedPacketResultTopBar(onDismiss = onDismiss)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    RedPacketResultHeroCard(
                        senderName = displayName,
                        senderAvatar = senderAvatar,
                        message = displayMessage,
                        gesture = gesture,
                        who = who,
                        selfUsername = selfUsername,
                        isSelfSender = senderName.equals(selfUsername, ignoreCase = true),
                        stateText = stateText,
                    )
                }
                item {
                    RedPacketReceiverRecordCard(
                        got = got,
                        count = count,
                        who = who,
                    )
                }
            }
        }
    }
}

@Composable
private fun RedPacketResultTopBar(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RedPacketTop)
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        PlainBackButton(
            onClick = onDismiss,
            contentDescription = "返回聊天室",
            tint = Color.White,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = "红包详情",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun RedPacketResultHeroCard(
    senderName: String,
    senderAvatar: String,
    message: String,
    gesture: Int?,
    who: List<RedPacketGot>,
    selfUsername: String,
    isSelfSender: Boolean,
    stateText: String,
) {
    val colors = redPacketPageColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RedPacketLargeAvatar(avatar = senderAvatar, name = senderName)
        Text(
            text = senderName,
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = message,
            color = colors.secondaryText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        RedPacketResultMark(gesture = gesture)
        RedPacketResultMessage(
            gesture = gesture,
            senderName = senderName,
            who = who,
            selfUsername = selfUsername,
            isSelfSender = isSelfSender,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        Text(
            text = stateText,
            color = colors.secondaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RedPacketResultMark(gesture: Int?) {
    if (gesture == null) {
        Icon(
            imageVector = Icons.Rounded.LocalAtm,
            contentDescription = null,
            tint = RedPacketTop,
            modifier = Modifier.size(34.dp),
        )
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = legacyRedPacketGestureEmoji(gesture), fontSize = 32.sp, lineHeight = 34.sp)
        Text(
            text = "出拳：${legacyRedPacketGestureLabel(gesture)}",
            color = FishPiTheme.weakText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RedPacketResultMessage(
    gesture: Int?,
    senderName: String,
    who: List<RedPacketGot>,
    selfUsername: String,
    isSelfSender: Boolean,
) {
    if (gesture == null) {
        val selfGot = who.firstOrNull { item ->
            item.userName.equals(selfUsername, ignoreCase = true)
        }
        val text = selfGot?.let { "你抢到 ${it.userMoney} 积分" } ?: "查看领取详情"
        Text(
            text = text,
            color = RedPacketTop,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp,
        )
        return
    }
    val result = redPacketGestureResultText(
        who = who.map { it.toRedPacketUiModel() },
        senderName = senderName,
        senderGestureLabel = legacyRedPacketGestureLabel(gesture),
        selfUsername = selfUsername,
        isSelfSender = isSelfSender,
    )
    Text(
        text = result,
        color = RedPacketTop,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        lineHeight = 25.sp,
    )
}

@Composable
private fun RedPacketReceiverRecordCard(
    got: Long,
    count: Long,
    who: List<RedPacketGot>,
) {
    val colors = redPacketPageColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "领取记录",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$got/$count",
                color = colors.text.copy(alpha = 0.78f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        if (who.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂时还没有领取记录", color = colors.secondaryText, fontSize = 14.sp)
            }
        } else {
            who.forEach { item ->
                RedPacketReceiverRecordRow(item = item)
            }
        }
    }
}

@Composable
private fun RedPacketReceiverRecordRow(item: RedPacketGot) {
    val colors = redPacketPageColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RedPacketReceiverAvatar(
            avatar = item.avatar,
            name = item.userName,
            sizeDp = 42,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = item.userName.ifBlank { "匿名用户" },
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.time.toRedPacketRecordTime(),
                color = colors.secondaryText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = item.userMoney.toString(),
                color = RedPacketTop,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "积分", color = colors.secondaryText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RedPacketDialogFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .background(Color.Black.copy(alpha = 0.26f))
            .silentTap(onDismiss)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 336.dp)
                .heightIn(max = maxHeight * 0.82f)
                .border(FishPiTheme.borderWidth, FishPiTheme.surfaceContainer.copy(alpha = 0.86f), shape)
                .clip(shape)
                .background(FishPiTheme.surface)
                .consumeTaps(),
        ) {
            content()
        }
    }
}

@Composable
private fun RedPacketCompactHeader(
    title: String,
    titleMeta: String? = null,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FishPiTheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RedPacketMiniMark()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = title, color = FishPiTheme.onSurface, fontWeight = FontWeight.Bold)
                titleMeta?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = RedPacketTop, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, color = FishPiTheme.weakText)
            }
        }
        FishPiIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "关闭$title",
            onClick = onDismiss,
            tint = FishPiTheme.weakText,
            background = FishPiTheme.surfaceContainer.copy(alpha = 0.72f),
            sizeDp = 32,
            iconSizeDp = 17,
        )
    }
}

@Composable
private fun RedPacketMiniMark() {
    Box(
        modifier = Modifier
            .size(width = 5.dp, height = 24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(RedPacketTop),
    )
}

private data class RedPacketGesture(
    val value: Int,
    val label: String,
) {
    companion object {
        val all = listOf(
            RedPacketGesture(0, "石头"),
            RedPacketGesture(1, "剪刀"),
            RedPacketGesture(2, "布"),
        )
    }
}

private fun legacyRedPacketGestureLabel(value: Int): String =
    RedPacketGesture.all.firstOrNull { it.value == value }?.label ?: "未知"

@Composable
private fun RedPacketGestureOption(
    gesture: RedPacketGesture,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> FishPiTheme.surfaceContainer.copy(alpha = 0.54f)
        selected -> RedPacketAccentSoft
        else -> FishPiTheme.surfaceContainer.copy(alpha = 0.78f)
    }
    val foreground = when {
        !enabled -> FishPiTheme.weakText.copy(alpha = 0.5f)
        selected -> RedPacketTop
        else -> FishPiTheme.weakText
    }
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) RedPacketTop.copy(alpha = 0.34f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = gesture.label,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun RedPacketTypeSelector(
    types: List<Pair<String, String>>,
    selectedType: String,
    enabled: Boolean,
    onTypeChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = types.firstOrNull { it.first == selectedType }?.second ?: "拼手气"
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 1.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "${selectedLabel}红包",
                color = if (enabled) RedPacketTop else FishPiTheme.weakText,
                fontWeight = FontWeight.SemiBold,
            )
            FishPiIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "选择红包类型",
                onClick = { if (enabled) expanded = true },
                tint = if (enabled) RedPacketTop else FishPiTheme.weakText,
                background = Color.Transparent,
                sizeDp = 26,
                iconSizeDp = 18,
                enabled = enabled,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FishPiTheme.surface),
        ) {
            types.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${label}红包",
                            color = if (key == selectedType) RedPacketTop else FishPiTheme.onSurface,
                            fontWeight = if (key == selectedType) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onTypeChange(key)
                    },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun RedPacketCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    singleLine: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = FishPiTheme.surfaceContainer.copy(alpha = if (enabled) 1f else 0.56f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FishPiTheme.surfaceContainer.copy(alpha = if (enabled) 0.46f else 0.26f))
            .border(FishPiTheme.borderWidth, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            color = if (enabled) FishPiTheme.onSurface else FishPiTheme.weakText.copy(alpha = 0.54f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = if (enabled) FishPiTheme.onSurface else FishPiTheme.weakText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = FishPiTheme.weakText.copy(alpha = 0.62f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun RedPacketSectionLabel(text: String) {
    Text(
        text = text,
        color = FishPiTheme.weakText,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RedPacketChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> FishPiTheme.surfaceContainer.copy(alpha = 0.68f)
        selected -> RedPacketTop
        else -> FishPiTheme.surfaceContainer.copy(alpha = 0.80f)
    }
    val foreground = when {
        !enabled -> FishPiTheme.weakText.copy(alpha = 0.58f)
        selected -> Color.White
        else -> FishPiTheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RedPacketReceiverRow(item: RedPacketGot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RedPacketReceiverAvatar(
            avatar = item.avatar,
            name = item.userName,
        )
        Text(
            text = item.userName.ifBlank { "匿名用户" },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = "${item.userMoney} 积分", color = RedPacketTop)
    }
}

@Composable
private fun RedPacketReceiverAvatar(
    avatar: String,
    name: String,
    sizeDp: Int = 28,
) {
    val modifier = Modifier
        .size(sizeDp.dp)
        .clip(CircleShape)
        .background(redPacketPageColors().accentSoft)

    if (avatar.isBlank()) {
        RedPacketAvatarPlaceholder(name = name, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = avatar,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "${name.ifBlank { "用户" }}头像",
        contentScale = ContentScale.Crop,
        loading = { RedPacketAvatarPlaceholder(name = name, modifier = modifier) },
        error = { RedPacketAvatarPlaceholder(name = name, modifier = modifier) },
        modifier = modifier,
    )
}

@Composable
private fun RedPacketLargeAvatar(
    avatar: String,
    name: String,
) {
    val modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(redPacketPageColors().accentSoft)
        .border(2.dp, RedPacketTop.copy(alpha = 0.16f), CircleShape)

    if (avatar.isBlank()) {
        RedPacketAvatarPlaceholder(name = name, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = avatar,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "${name.ifBlank { "用户" }}头像",
        contentScale = ContentScale.Crop,
        loading = { RedPacketAvatarPlaceholder(name = name, modifier = modifier) },
        error = { RedPacketAvatarPlaceholder(name = name, modifier = modifier) },
        modifier = modifier,
    )
}

private fun String.toRedPacketRecordTime(): String {
    val value = trim()
    if (value.length >= 16 && value[4] == '-' && value[7] == '-') {
        return value.substring(5, 16)
    }
    return value
}

@Composable
private fun RedPacketAvatarPlaceholder(
    name: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.toString()?.ifBlank { "魚" } ?: "魚",
            color = RedPacketTop,
            fontWeight = FontWeight.Bold,
        )
    }
}
