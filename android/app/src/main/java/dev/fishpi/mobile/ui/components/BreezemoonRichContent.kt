package dev.fishpi.mobile.ui.components

import android.graphics.Typeface
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.overlay.LocalMediaOverlay
import dev.fishpi.mobile.utils.toFishPiMemberNameOrNull

private const val CONTENT_IMAGE_MAX_WIDTH_DP = 240f
private const val CONTENT_IMAGE_MAX_HEIGHT_DP = 320f
private const val CONTENT_IMAGE_FALLBACK_RATIO = 1.45f

private val breezemoonImageTagRegex = Regex(
    pattern = """<img\b[^>]*?\ssrc\s*=\s*(["'])(.*?)\1[^>]*>""",
    option = RegexOption.IGNORE_CASE,
)

private sealed interface BreezemoonContentBlock {
    data class HtmlText(val html: String) : BreezemoonContentBlock
    data class Image(val url: String) : BreezemoonContentBlock
}

/**
 * 把服务端 HTML 拆成文本段和 `<img>` 图片段。
 */
private fun parseBreezemoonContentBlocks(html: String): List<BreezemoonContentBlock> {
    val blocks = mutableListOf<BreezemoonContentBlock>()
    var cursor = 0
    for (match in breezemoonImageTagRegex.findAll(html)) {
        val textHtml = html.substring(cursor, match.range.first)
        if (textHtml.isNotBlank()) blocks += BreezemoonContentBlock.HtmlText(textHtml)
        val url = match.groupValues[2].trim()
        if (url.isNotEmpty()) blocks += BreezemoonContentBlock.Image(url)
        cursor = match.range.last + 1
    }
    val tail = html.substring(cursor)
    if (tail.isNotBlank()) blocks += BreezemoonContentBlock.HtmlText(tail)
    return blocks
}

@Composable
internal fun BreezemoonRichContent(
    html: String,
    accent: Color,
    modifier: Modifier = Modifier,
    textSizeSp: Float = 13f,
    lineSpacingMultiplier: Float = 1.35f,
    onOpenUser: ((String) -> Unit)? = null,
) {
    val blocks = remember(html) { parseBreezemoonContentBlocks(html) }
    if (blocks.isEmpty()) return
    val imageUrls = remember(blocks) {
        blocks.mapNotNull { (it as? BreezemoonContentBlock.Image)?.url }
    }
    val imageViewer = LocalMediaOverlay.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var imageIndex = 0
        blocks.forEach { block ->
            when (block) {
                is BreezemoonContentBlock.HtmlText -> BreezemoonHtmlText(
                    html = block.html,
                    accent = accent,
                    textSizeSp = textSizeSp,
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    onOpenUser = onOpenUser,
                )
                is BreezemoonContentBlock.Image -> {
                    val index = imageIndex++
                    BreezemoonContentImage(
                        url = block.url,
                        onClick = {
                            if (imageUrls.isNotEmpty()) imageViewer.open(imageUrls, index)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BreezemoonHtmlText(
    html: String,
    accent: Color,
    textSizeSp: Float,
    lineSpacingMultiplier: Float,
    onOpenUser: ((String) -> Unit)?,
) {
    val color = MaterialTheme.colorScheme.onSurface
    val accentArgb = accent.toArgb()
    val currentOpenUser by rememberUpdatedState(onOpenUser)
    val text = remember(html, accentArgb, onOpenUser != null) {
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        var start = 0
        var end = spanned.length
        while (start < end && spanned[start].isWhitespace()) start++
        while (end > start && spanned[end - 1].isWhitespace()) end--
        if (end <= start) {
            null
        } else {
            spanned.subSequence(start, end).withBreezemoonMemberLinks(accentArgb) { username ->
                currentOpenUser?.invoke(username)
            }
        }
    }
    if (text.isNullOrEmpty()) return
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                textSize = textSizeSp
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
                minHeight = 0
                minimumHeight = 0
                setLineSpacing(0f, lineSpacingMultiplier)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { view ->
            view.setTextColor(color.toArgb())
            view.text = text
        },
    )
}

@Composable
private fun BreezemoonContentImage(
    url: String,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    var naturalSize by remember(url) { mutableStateOf(IntSize.Zero) }
    val hasNaturalSize = naturalSize.width > 0 && naturalSize.height > 0
    val sizeModifier = if (hasNaturalSize) {
        val widthDp = with(density) { naturalSize.width.toDp() }
        val heightDp = with(density) { naturalSize.height.toDp() }
        val scale = minOf(
            1f,
            CONTENT_IMAGE_MAX_WIDTH_DP / widthDp.value,
            CONTENT_IMAGE_MAX_HEIGHT_DP / heightDp.value,
        )
        Modifier.size(widthDp * scale, heightDp * scale)
    } else {
        Modifier.fillMaxWidth(0.72f).aspectRatio(CONTENT_IMAGE_FALLBACK_RATIO)
    }
    SubcomposeAsyncImage(
        model = url,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "清风明月图片",
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            naturalSize = IntSize(state.result.image.width, state.result.image.height)
        },
        modifier = sizeModifier
            .clip(RoundedCornerShape(FishPiTheme.radiusField))
            .background(FishPiTheme.surfaceContainer)
            .clickable(onClick = onClick),
    )
}

private fun CharSequence.withBreezemoonMemberLinks(
    accentColor: Int,
    onOpenUser: ((String) -> Unit)?,
): SpannableStringBuilder {
    val spannable = SpannableStringBuilder(this)
    if (onOpenUser == null) return spannable
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
