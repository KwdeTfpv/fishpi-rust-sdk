package dev.fishpi.mobile.ui.components

import dev.fishpi.mobile.*
import dev.fishpi.mobile.ui.motion.fishClickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PlaceholderScreen(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(softPageBrush())
            .padding(FishPiTheme.spacingPage),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f + FishPiTheme.depth * 0.10f), RoundedCornerShape(FishPiTheme.radiusBox))
                .padding(FishPiTheme.spacingSection),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        ) {
            SoftIllustrationGlyph()
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun FishPiListCard(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(FishPiTheme.radiusBox)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f + FishPiTheme.depth * 0.08f), shape)
            .padding(FishPiTheme.spacingSection),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        startAction?.invoke()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (startAction == null) 0.dp else FishPiTheme.spacingItem),
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        endAction?.let {
            Box(modifier = Modifier.padding(start = FishPiTheme.spacingItem)) {
                it()
            }
        }
    }
}

@Composable
internal fun FishPiPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    compact: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val buttonShape = RoundedCornerShape(if (compact) FishPiTheme.radiusField * 0.78f else FishPiTheme.radiusField)
    val targetContainer = when {
        danger -> MaterialTheme.colorScheme.errorContainer
        else -> containerColor ?: MaterialTheme.colorScheme.primaryContainer
    }
    val targetContent = when {
        danger -> MaterialTheme.colorScheme.onErrorContainer
        else -> contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = modifier
            .clip(buttonShape)
            .background(if (enabled) targetContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                FishPiTheme.borderWidth,
                if (enabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                buttonShape,
            )
            .fishClickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) FishPiTheme.spacingControl else FishPiTheme.spacingControl + 4.dp,
                vertical = if (compact) 5.dp else 7.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) targetContent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
internal fun FishPiIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    background: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    sizeDp: Int = 38,
    iconSizeDp: Int = 21,
) {
    val shape = RoundedCornerShape(FishPiTheme.radiusField)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(if (enabled) background else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f + FishPiTheme.depth * 0.08f), shape)
            .fishClickable(enabled = enabled, rippleBounded = false, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
            modifier = Modifier.size(iconSizeDp.dp),
        )
    }
}

@Composable
internal fun PlainBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "返回",
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .semantics { this.contentDescription = contentDescription }
            .fishClickable(rippleBounded = false, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 11.dp, height = 17.dp)) {
            val stroke = 1.28.dp.toPx()
            val left = Offset(size.width * 0.18f, size.height * 0.50f)
            val top = Offset(size.width * 0.82f, size.height * 0.07f)
            val bottom = Offset(size.width * 0.82f, size.height * 0.93f)
            drawLine(color = tint, start = top, end = left, strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color = tint, start = left, end = bottom, strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun Modifier.fishPiChatWallpaper(): Modifier {
    val colors = LocalFishPiPalette.current.wallpaperColors
    return if (colors.size <= 1) {
        background(colors.firstOrNull() ?: MaterialTheme.colorScheme.surface)
    } else {
        background(softPageBrush(colors))
    }
}

@Composable
private fun softPageBrush(colors: List<Color> = LocalFishPiPalette.current.wallpaperColors): Brush {
    val base = colors.ifEmpty { listOf(MaterialTheme.colorScheme.background) }
    return Brush.verticalGradient(
        listOf(
            base.first().copy(alpha = 0.96f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            base.last().copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ),
    )
}

@Composable
internal fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val shape = RoundedCornerShape(FishPiTheme.radiusField)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = FishPiTheme.borderWidth,
                color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = shape,
            )
            .padding(horizontal = FishPiTheme.spacingControl + 2.dp, vertical = FishPiTheme.spacingControl),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2),
    ) {
        label?.let {
            Text(
                text = it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        ) {
            leadingIcon?.invoke()
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                readOnly = readOnly,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isBlank() && placeholder != null) {
                            Text(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
                        }
                        innerTextField()
                    }
                },
            )
            trailingIcon?.invoke()
        }
        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val shape = RoundedCornerShape(FishPiTheme.radiusField)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = FishPiTheme.borderWidth,
                color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = shape,
            )
            .padding(horizontal = FishPiTheme.spacingControl + 2.dp, vertical = FishPiTheme.spacingControl),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2),
    ) {
        label?.let {
            Text(
                text = it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        ) {
            leadingIcon?.invoke()
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                readOnly = readOnly,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.text.isBlank() && placeholder != null) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            trailingIcon?.invoke()
        }
        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun LoadingScreen(message: String, showLogo: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(softPageBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
            modifier = Modifier.padding(horizontal = FishPiTheme.spacingPage + FishPiTheme.spacingSection),
        ) {
            if (showLogo) {
                FishPiBrandLoadingLogo()
            } else {
                SoftIllustrationGlyph()
            }
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LoadingDots()
        }
    }
}

@Composable
internal fun FishPiBrandLoadingLogo() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f + FishPiTheme.depth * 0.08f), RoundedCornerShape(FishPiTheme.radiusBox)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_fishpi_logo),
            contentDescription = "摸鱼派",
            modifier = Modifier.size(50.dp),
        )
    }
}

@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "loading-dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val delay = index * 130
            val progress = transition.animateFloat(
                initialValue = 0.42f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 980
                        0.42f at 0
                        0.42f at delay
                        1f at delay + 180
                        0.42f at delay + 360
                        0.42f at 980
                    },
                ),
                label = "loading-dot-$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        alpha = progress.value
                        scaleX = 0.82f + progress.value * 0.18f
                        scaleY = 0.82f + progress.value * 0.18f
                        translationY = -(progress.value - 0.42f) * 5f
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
internal fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FishPiTheme.spacingPage),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FishPiTheme.radiusBox + 10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.error.copy(alpha = 0.16f + FishPiTheme.depth * 0.08f), RoundedCornerShape(FishPiTheme.radiusBox + 10.dp))
                .padding(FishPiTheme.spacingSection),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        ) {
            SoftIllustrationGlyph(color = MaterialTheme.colorScheme.error.copy(alpha = 0.32f))
            Text(text = message, color = MaterialTheme.colorScheme.error)
            FishPiPillButton(text = "重试", onClick = onRetry)
        }
    }
}

@Composable
private fun SoftIllustrationGlyph(
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
) {
    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusBox + 16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        color,
                        Color.White.copy(alpha = 0.24f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.align(Alignment.Center)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)),
                )
            }
        }
    }
}

