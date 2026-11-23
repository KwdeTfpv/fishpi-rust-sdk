package dev.fishpi.mobile.feature.chat.appearance

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.ImageLoader
import dev.fishpi.mobile.feature.chat.ChatAppearancePhase
import dev.fishpi.mobile.feature.chat.ChatAppearanceUiState
import dev.fishpi.mobile.feature.chat.GeetestCaptchaSheet
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.shared.message.native.ChatAppearancePreviewView
import dev.fishpi.mobile.shared.message.native.ChatAppearanceSupport
import dev.fishpi.mobile.ui.components.FishPiFullScreenDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val appearanceDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

private fun formatAppearanceDate(epochMillis: Long): String =
    appearanceDateFormatter.format(Instant.ofEpochMilli(epochMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePanel(
    appearance: ChatAppearanceUiState,
    shop: AppearanceShopUiState,
    avatarUrl: String,
    onDismiss: () -> Unit,
    onSelectBubble: (String) -> Unit,
    onSelectFrame: (String) -> Unit,
    onSave: () -> Unit,
    onRetryEquip: () -> Unit,
    onAuthorize: (String, String?) -> Unit,
    onCompleteCaptcha: (String) -> Unit,
    onRefresh: () -> Unit,
    onConfirmPurchase: (String, Int) -> Unit,
    onRetryPurchase: () -> Unit,
    onDismissPurchase: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(AppearanceProductKind.Bubble) }
    var filter by rememberSaveable { mutableStateOf(0) }
    var purchaseTarget by remember { mutableStateOf<AppearanceProductCard?>(null) }
    var purchaseDays by remember { mutableStateOf(7) }
    var showAuthorize by remember { mutableStateOf(false) }
    var showCaptcha by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var mfaCode by remember { mutableStateOf("") }
    val imageLoader = rememberFishPiImageLoader()

    val membershipActive = shop.membership?.active
    val equippedBubble = appearance.current?.bubbleId
    val equippedFrame = appearance.current?.frameId
    val bubbleCards = remember(shop.items, shop.inventory, membershipActive, equippedBubble) {
        buildAppearanceProductCards(
            AppearanceProductKind.Bubble,
            shop.items,
            shop.inventory,
            membershipActive,
            equippedBubble,
        )
    }
    val frameCards = remember(shop.items, shop.inventory, membershipActive, equippedFrame) {
        buildAppearanceProductCards(
            AppearanceProductKind.Frame,
            shop.items,
            shop.inventory,
            membershipActive,
            equippedFrame,
        )
    }
    val cards = if (tab == AppearanceProductKind.Bubble) bubbleCards else frameCards
    val equippedBubbleName = bubbleCards.firstOrNull { it.equipped }?.name
    val equippedFrameName = frameCards.firstOrNull { it.equipped }?.name
    val pendingEquip = appearance.pendingIdempotencyKey != null
    val editable = appearance.phase == ChatAppearancePhase.Editing && !pendingEquip
    val draftChanged = appearance.current?.let {
        it.bubbleId != appearance.bubbleId || it.frameId != appearance.frameId
    } == true
    val visibleCards = when (filter) {
        1 -> cards.filter { it.action == AppearanceCardAction.Equip || it.equipped }
        2 -> cards.filter { it.entitlementExpiresAt != null }
        else -> cards
    }
    val purchase = shop.purchase
    val purchaseCard = purchase?.let { state ->
        cards.firstOrNull { it.productId == state.productId }
    } ?: purchaseTarget

    FishPiFullScreenDialog(onDismissRequest = onDismiss, edgeToEdge = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                ) {
                    AppearanceHeader(
                        balanceText = when {
                            shop.inventory != null -> "积分 ${shop.inventory.balance}"
                            shop.inventoryError != null -> "余额暂不可用"
                            else -> "余额读取中"
                        },
                        onRefresh = onRefresh,
                        onDismiss = onDismiss,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val usingBubbleName = equippedBubbleName
                                ?: equippedBubble?.let {
                                    if (ChatAppearanceSupport.canonicalBubbleId(it) == "classic") "经典" else it
                                }
                                ?: "经典"
                            val usingText = buildString {
                                append("正在使用：")
                                append(usingBubbleName)
                                append(" · ")
                                append(equippedFrameName ?: equippedFrame ?: "无")
                            }
                            Text(usingText, style = MaterialTheme.typography.titleMedium)
                            if (appearance.isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text("正在读取外观配置", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            AppearanceCombinedPreview(
                                appearance = appearance,
                                shop = shop,
                                imageLoader = imageLoader,
                                avatarUrl = avatarUrl,
                                draftChanged = draftChanged,
                            )
                            if (appearance.needsAuthorization) {
                                AppearanceNoticeCard(
                                    text = "外观功能需要重新授权后才能读写当前账号的配置。",
                                    actionLabel = "重新授权",
                                    onAction = { showAuthorize = true },
                                )
                            }
                            if (appearance.needsCaptcha) {
                                AppearanceNoticeCard(
                                    text = "装备操作需要完成人机验证。",
                                    actionLabel = "完成人机验证",
                                    onAction = { showCaptcha = true },
                                )
                            }
                            if (pendingEquip) {
                                AppearanceNoticeCard(
                                    text = appearance.hint ?: "上次装备操作尚未在服务端核对。",
                                    actionLabel = if (appearance.verifyOnly ||
                                        appearance.phase == ChatAppearancePhase.Verifying
                                    ) {
                                        "继续核对"
                                    } else {
                                        "使用原操作重试"
                                    },
                                    onAction = onRetryEquip,
                                )
                            }
                            appearance.error?.let { message ->
                                Text(
                                    message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (shop.phase == AppearanceShopPhase.Failed) {
                                AppearanceNoticeCard(
                                    text = shop.error ?: "外观商城读取失败。",
                                    actionLabel = "重试",
                                    onAction = onRefresh,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = tab == AppearanceProductKind.Bubble,
                                onClick = { tab = AppearanceProductKind.Bubble },
                                label = { Text("气泡") },
                            )
                            FilterChip(
                                selected = tab == AppearanceProductKind.Frame,
                                onClick = { tab = AppearanceProductKind.Frame },
                                label = { Text("头像框") },
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            FilterChip(
                                selected = filter == 0,
                                onClick = { filter = 0 },
                                label = { Text("全部") },
                            )
                            FilterChip(
                                selected = filter == 1,
                                onClick = { filter = 1 },
                                label = { Text("可使用") },
                            )
                            FilterChip(
                                selected = filter == 2,
                                onClick = { filter = 2 },
                                label = { Text("已购买") },
                            )
                        }
                        if (shop.isLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else if (visibleCards.isEmpty()) {
                            Text(
                                if (filter == 0) "暂无外观商品" else "没有符合条件的外观",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            visibleCards.chunked(2).forEach { rowCards ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowCards.forEach { card ->
                                        AppearanceProductCardView(
                                            card = card,
                                            selected = if (card.kind == AppearanceProductKind.Bubble) {
                                                appearance.bubbleId == card.styleId
                                            } else {
                                                appearance.frameId == card.styleId
                                            },
                                            imageLoader = imageLoader,
                                            avatarUrl = avatarUrl,
                                            onSelect = {
                                                if (card.kind == AppearanceProductKind.Bubble) {
                                                    onSelectBubble(card.styleId)
                                                } else {
                                                    onSelectFrame(card.styleId)
                                                }
                                            },
                                            onPurchase = { purchaseTarget = card },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (rowCards.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (draftChanged) {
                        val draftCard = if (tab == AppearanceProductKind.Bubble) {
                            bubbleCards.firstOrNull { it.styleId == appearance.bubbleId }
                        } else {
                            frameCards.firstOrNull { it.styleId == appearance.frameId }
                        }
                        AppearanceDraftBar(
                            title = "试选：${draftCard?.name ?: appearance.bubbleId}",
                            status = draftCard?.statusText ?: "",
                            saving = appearance.isSaving,
                            enabled = editable,
                            onSave = onSave,
                        )
                    }
                }
            }
        }
    }
    val target = purchaseTarget
    if (purchase == null && target != null) {
        AppearancePurchaseSelector(
            card = target,
            days = purchaseDays,
            onDaysChange = { purchaseDays = it },
            balance = shop.balance,
            balanceAvailable = shop.inventory != null,
            serverNow = shop.inventory?.serverNow ?: 0L,
            onConfirm = { card ->
                val id = card.productId
                if (id != null) onConfirmPurchase(id, purchaseDays)
            },
            onCancel = { purchaseTarget = null },
        )
    }
    if (purchase != null) {
        AppearancePurchaseResult(
            state = purchase,
            card = purchaseCard,
            onEquip = {
                val card = purchaseCard
                if (card != null) {
                    if (card.kind == AppearanceProductKind.Bubble) {
                        onSelectBubble(card.styleId)
                    } else {
                        onSelectFrame(card.styleId)
                    }
                }
                onDismissPurchase()
            },
            onRetry = onRetryPurchase,
            onCaptcha = { showCaptcha = true },
            onDismiss = onDismissPurchase,
        )
    }
    if (showAuthorize) {
        AlertDialog(
            onDismissRequest = { showAuthorize = false },
            title = { Text("授权外观功能") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("使用当前账号的密码换取独立的 V3 授权，不会影响现有登录。")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = mfaCode,
                        onValueChange = { mfaCode = it },
                        label = { Text("两步验证码（可选）") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAuthorize = false
                        onAuthorize(password, mfaCode.ifBlank { null })
                        password = ""
                        mfaCode = ""
                    },
                    enabled = password.isNotBlank(),
                ) { Text("授权") }
            },
            dismissButton = {
                TextButton(onClick = { showAuthorize = false }) { Text("取消") }
            },
        )
    }
    if (showCaptcha) {
        GeetestCaptchaSheet(
            onResult = { captchaJson ->
                showCaptcha = false
                onCompleteCaptcha(captchaJson)
            },
            onDismiss = { showCaptcha = false },
        )
    }
}

@Composable
private fun AppearanceHeader(
    balanceText: String,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "关闭")
            }
            Text(
                "外观管理",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                balanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
    }
}

@Composable
private fun AppearanceCombinedPreview(
    appearance: ChatAppearanceUiState,
    shop: AppearanceShopUiState,
    imageLoader: ImageLoader,
    avatarUrl: String,
    draftChanged: Boolean,
) {
    val previewDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val classicFill = MaterialTheme.colorScheme.secondaryContainer.toArgb()
    val classicText = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()
    val shopStyleJson = shop.items.firstOrNull {
        it.kind == "bubble" && it.styleId == appearance.bubbleId
    }?.bubbleStyleJson
    val equippedStyleJson = appearance.current
        ?.takeIf { it.bubbleId == appearance.bubbleId && it.frameId == appearance.frameId }
        ?.bubbleStyleJson
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AndroidView(
                factory = { context -> ChatAppearancePreviewView(context) },
                update = { view ->
                    view.bind(
                        bubbleId = appearance.bubbleId,
                        frameId = appearance.frameId,
                        dark = previewDark,
                        classicFill = classicFill,
                        classicText = classicText,
                        bubbleStyleJson = shopStyleJson ?: equippedStyleJson,
                        imageLoader = imageLoader,
                        avatarUrl = avatarUrl,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            )
            if (draftChanged) {
                Text(
                    "预览中，尚未装备",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun AppearanceNoticeCard(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun AppearanceProductCardView(
    card: AppearanceProductCard,
    selected: Boolean,
    imageLoader: ImageLoader,
    avatarUrl: String,
    onSelect: () -> Unit,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val classicFill = MaterialTheme.colorScheme.secondaryContainer.toArgb()
    val classicText = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()
    val priceLine = when {
        card.purchasable -> buildString {
            append("7天 ")
            append(card.price7 ?: 0L)
            append(" 积分")
            if ((card.price30 ?: 0L) > 0L) {
                append(" · 30天 ")
                append(card.price30)
                append(" 积分")
            } else {
                append(" · 不提供30天")
            }
        }
        else -> null
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(enabled = card.selectable, onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (card.supported) {
                AndroidView(
                    factory = { context -> ChatAppearancePreviewView(context) },
                    update = { view ->
                        if (card.kind == AppearanceProductKind.Bubble) {
                            view.bind(
                                bubbleId = card.styleId,
                                frameId = "none",
                                dark = previewDark,
                                classicFill = classicFill,
                                classicText = classicText,
                                bubbleStyleJson = card.bubbleStyleJson,
                                imageLoader = imageLoader,
                                avatarUrl = avatarUrl,
                            )
                        } else {
                            view.bind(
                                bubbleId = "classic",
                                frameId = card.styleId,
                                dark = previewDark,
                                classicFill = classicFill,
                                classicText = classicText,
                                bubbleStyleJson = null,
                                imageLoader = imageLoader,
                                avatarUrl = avatarUrl,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂不支持预览", style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (card.equipped) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "已装备",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            if (!card.equipped) {
                Text(card.statusText, style = MaterialTheme.typography.bodySmall)
            }
            if (priceLine != null) {
                Text(
                    priceLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            when (card.action) {
                AppearanceCardAction.Equip -> {
                    Button(
                        onClick = onSelect,
                        enabled = !card.equipped,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(appearanceCardActionLabel(card))
                    }
                }
                AppearanceCardAction.Purchase -> {
                    OutlinedButton(
                        onClick = onPurchase,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(appearanceCardActionLabel(card))
                    }
                }
                AppearanceCardAction.None -> Unit
            }
        }
    }
}

@Composable
private fun AppearanceDraftBar(
    title: String,
    status: String,
    saving: Boolean,
    enabled: Boolean,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (status.isNotBlank()) {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onSave, enabled = enabled && !saving) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text("装备到新消息")
            }
        }
    }
}

@Composable
private fun AppearancePurchaseSelector(
    card: AppearanceProductCard,
    days: Int,
    onDaysChange: (Int) -> Unit,
    balance: Long?,
    balanceAvailable: Boolean,
    serverNow: Long,
    onConfirm: (AppearanceProductCard) -> Unit,
    onCancel: () -> Unit,
) {
    val price = if (days == 30) (card.price30 ?: 0L) else (card.price7 ?: 0L)
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("确认购买") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("商品：${card.name}")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = days == 7,
                        onClick = { onDaysChange(7) },
                        enabled = (card.price7 ?: 0L) > 0L,
                        label = { Text("7天 ${(card.price7 ?: 0L)} 积分") },
                    )
                    FilterChip(
                        selected = days == 30,
                        onClick = { onDaysChange(30) },
                        enabled = (card.price30 ?: 0L) > 0L,
                        label = {
                            Text(
                                if ((card.price30 ?: 0L) > 0L) {
                                    "30天 ${card.price30} 积分"
                                } else {
                                    "不提供30天"
                                },
                            )
                        },
                    )
                }
                Text("实际扣除：$price 积分")
                if (balanceAvailable && balance != null) {
                    Text("当前余额：$balance 积分 · 预计扣除后：${balance - price} 积分")
                } else {
                    Text(
                        "余额暂不可用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                card.entitlementExpiresAt?.let { expiresAt ->
                    val base = maxOf(expiresAt, serverNow.takeIf { it > 0L } ?: expiresAt)
                    val extended = base + days * 24L * 60L * 60L * 1000L
                    Text("当前有效期至：${formatAppearanceDate(expiresAt)} · 预计续期至：${formatAppearanceDate(extended)}")
                }
                Text(
                    "购买不会自动装备，也不自动续费。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(card) },
                enabled = price > 0L,
            ) {
                Text(if (price > 0L) "确认购买（$price 积分）" else "该期限不可购买")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        },
    )
}

@Composable
private fun AppearancePurchaseResult(
    state: AppearancePurchaseUiState,
    card: AppearanceProductCard?,
    onEquip: () -> Unit,
    onRetry: () -> Unit,
    onCaptcha: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = card?.name ?: state.productId
    AlertDialog(
        onDismissRequest = if (state.submitting) {
            {}
        } else {
            onDismiss
        },
        title = { Text(if (state.success != null) "购买成功" else "购买") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("商品：$name · ${state.days}天")
                when {
                    state.submitting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                            Text("正在提交购买…")
                        }
                    }
                    state.success != null -> {
                        Text("实际扣除 ${state.success.paidPoints} 积分。")
                        Text(
                            "有效期至：${formatAppearanceDate(state.success.expiresAt)}",
                        )
                        Text(
                            "购买不会自动装备，也不自动续费。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.needsCaptcha -> {
                        Text("接口需要完成人机验证后才能继续。")
                    }
                    state.unknownResult -> {
                        Text(
                            state.error ?: "购买结果未知，服务端未确认本次扣费。",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "请使用原操作重试（不会重复扣费），或稍后在权益中核对。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    else -> {
                        Text(
                            state.error ?: "购买失败。",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                state.submitting -> Unit
                state.success != null -> {
                    TextButton(onClick = onEquip) { Text("购买成功，去装备") }
                }
                state.needsCaptcha -> {
                    TextButton(onClick = onCaptcha) { Text("完成人机验证") }
                }
                state.unknownResult -> {
                    TextButton(onClick = onRetry) { Text("使用原操作重试") }
                }
                else -> {
                    TextButton(onClick = onDismiss) { Text("知道了") }
                }
            }
        },
        dismissButton = {
            if (!state.submitting) {
                TextButton(onClick = onDismiss) { Text(if (state.success != null) "暂不装备" else "关闭") }
            }
        },
    )
}
