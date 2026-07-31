package dev.fishpi.mobile.chatui

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import dev.fishpi.mobile.shared.message.native.NativeMessageTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

internal class MarkwonChatRenderer(
    context: Context,
    private val theme: NativeMessageTheme,
    cache: ChatMarkdownRenderCache,
    scope: CoroutineScope,
    onLinkClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
) {
    private val themeHash = listOf(
        theme.bubbleText,
        theme.weakText,
        theme.linkText,
        theme.quoteText,
        theme.quoteLine,
        theme.quoteBackground,
        theme.incomingBubble,
        theme.outgoingBubble,
    ).hashCode()
    private val core = MarkwonRendererCore(
        context = context,
        cache = cache,
        scope = scope,
        cachePrefix = "v3",
        styleHash = themeHash,
        accentColor = theme.linkText,
        onLinkClick = onLinkClick,
        onMentionClick = onMentionClick,
        textColor = theme.bubbleText,
        weakTextColor = theme.quoteText,
        codeBackgroundColor = theme.quoteBackground,
        blockQuoteTextScale = 0.9f,
        blockQuoteLineColor = theme.quoteLine,
    )

    fun renderInto(
        textView: TextView,
        messageKey: String,
        markdown: String,
        fallback: String,
    ): Job? {
        val displayMarkdown = markdown.withTopicReferenceHardBreaks()
        return core.renderInto(
            textView = textView,
            key = messageKey,
            markdown = displayMarkdown,
            fallback = fallback,
        ) { view ->
            view.setTextColor(theme.bubbleText)
            view.textSize = 15f
            view.includeFontPadding = false
            view.maxLines = Int.MAX_VALUE
            view.ellipsize = null
            view.setHorizontallyScrolling(false)
            view.maxWidth = view.context.dp(250)
            view.setLineSpacing(0f, 1.03f)
            view.highlightColor = 0x00000000
            view.movementMethod = LinkMovementMethod.getInstance()
            view.linksClickable = true
        }
    }

    fun clear(textView: TextView) {
        core.clear(textView)
    }

    private fun String.withTopicReferenceHardBreaks(): String =
        replace(TopicReferenceSoftBreakRegex) { match ->
            "${match.groupValues[1]}  \n${match.groupValues[2]}"
        }

    private companion object {
        val TopicReferenceSoftBreakRegex = Regex("""(?m)(\S)\n(\*`#\s*[^`\n]+?\s*#`\*)""")
    }
}
