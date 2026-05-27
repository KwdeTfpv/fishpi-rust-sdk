package dev.fishpi.mobile.chatui

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

internal data class MarkwonContentStyle(
    val textColor: Int,
    val weakTextColor: Int,
    val accentColor: Int,
    val codeBackgroundColor: Int,
    val textSizeSp: Float,
    val lineSpacingMultiplier: Float,
    val maxWidthPx: Int? = null,
)

internal class MarkwonContentRenderer(
    context: Context,
    private val style: MarkwonContentStyle,
    cache: ChatMarkdownRenderCache,
    scope: CoroutineScope,
    onLinkClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
) {
    private val styleHash = style.hashCode()
    private val core = MarkwonRendererCore(
        context = context,
        cache = cache,
        scope = scope,
        cachePrefix = "content-v2",
        styleHash = styleHash,
        accentColor = style.accentColor,
        onLinkClick = onLinkClick,
        onMentionClick = onMentionClick,
        textColor = style.textColor,
        weakTextColor = style.weakTextColor,
        codeBackgroundColor = style.codeBackgroundColor,
        blockQuoteTextScale = 0.92f,
    )

    fun renderInto(
        textView: TextView,
        contentKey: String,
        markdown: String,
        fallback: String = "",
    ): Job? {
        return core.renderInto(textView, contentKey, markdown, fallback) { view ->
            view.setTextColor(style.textColor)
            view.textSize = style.textSizeSp
            view.includeFontPadding = false
            view.setLineSpacing(0f, style.lineSpacingMultiplier)
            view.highlightColor = 0x00000000
            view.movementMethod = LinkMovementMethod.getInstance()
            view.linksClickable = true
            style.maxWidthPx?.let { view.maxWidth = it }
        }
    }

    fun clear(textView: TextView) {
        core.clear(textView)
    }
}
