package dev.fishpi.mobile.chatui

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import dev.fishpi.mobile.utils.FishPiMentionRegex
import dev.fishpi.mobile.utils.PlainUrlRegex
import dev.fishpi.mobile.utils.renderFishPiEmojiShortcodes
import dev.fishpi.mobile.utils.toFishPiMemberNameOrNull
import dev.fishpi.mobile.utils.trimUrlPunctuation
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SpanFactory
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.commonmark.node.BlockQuote

internal class MarkwonRendererCore(
    context: Context,
    private val cache: ChatMarkdownRenderCache,
    private val scope: CoroutineScope,
    private val cachePrefix: String,
    private val styleHash: Int,
    private val accentColor: Int,
    private val onLinkClick: (String) -> Unit,
    private val onMentionClick: (String) -> Unit,
    textColor: Int,
    private val weakTextColor: Int,
    codeBackgroundColor: Int,
    blockQuoteTextScale: Float,
    private val blockQuoteLineColor: Int = weakTextColor,
) {
    private val appContext = context.applicationContext
    private val markwon = Markwon.builder(appContext)
        .usePlugin(HtmlPlugin.create())
        .usePlugin(TablePlugin.create(appContext))
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .linkColor(accentColor)
                    .blockQuoteColor(blockQuoteLineColor)
                    .blockQuoteWidth(appContext.dp(2))
                    .codeTextColor(textColor)
                    .codeBackgroundColor(codeBackgroundColor)
            }

            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link ->
                    val memberName = link.toFishPiMemberNameOrNull()
                    if (memberName != null) {
                        onMentionClick(memberName)
                    } else {
                        onLinkClick(link)
                    }
                }
            }

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                val blockQuoteFactory = builder.requireFactory(BlockQuote::class.java)
                builder.setFactory(
                    BlockQuote::class.java,
                    SpanFactory { configuration, props ->
                        arrayOf(
                            blockQuoteFactory.getSpans(configuration, props),
                            RelativeSizeSpan(blockQuoteTextScale),
                            ForegroundColorSpan(weakTextColor),
                        )
                    },
                )
            }
        })
        .build()
    private val markwonLock = Mutex()

    fun renderInto(
        textView: TextView,
        key: String,
        markdown: String,
        fallback: String,
        configureTextView: (TextView) -> Unit,
    ): Job? {
        val source = markdown.ifBlank { fallback }
        if (source.isBlank()) {
            clear(textView)
            return null
        }
        val renderKey = cacheKey(key, source)
        textView.tag = renderKey
        configureTextView(textView)

        cache.get(renderKey)?.let { cached ->
            textView.text = cached
            return null
        }

        textView.text = fallback.ifBlank { source.take(300) }
        return scope.launch {
            val rendered = render(source)
            cache.put(renderKey, rendered, source.length)
            withContext(Dispatchers.Main.immediate) {
                if (textView.tag == renderKey) {
                    textView.text = rendered
                }
            }
        }
    }

    fun clear(textView: TextView) {
        textView.tag = null
        textView.text = ""
    }

    suspend fun renderToSpanned(source: String): Spanned = render(source)

    private suspend fun render(source: String): Spanned {
        return markwonLock.withLock {
            renderMarkdown(source)
        }
    }

    private suspend fun renderMarkdown(source: String): Spanned {
        val markdown = source.renderFishPiEmojiShortcodes()
        val chunks = ChatMarkdownChunker.split(markdown)
        if (chunks.size == 1) {
            return applyMentionAndWebUrlSpans(markwon.toMarkdown(markdown.trimEnd()))
        }
        val builder = SpannableStringBuilder()
        chunks.forEach { chunk ->
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            builder.append(markwon.toMarkdown(chunk))
        }
        return applyMentionAndWebUrlSpans(trimTrailingWhitespace(builder))
    }

    private fun applyMentionAndWebUrlSpans(source: Spanned): Spanned {
        val spannable = trimTrailingWhitespace(source)
        PlainUrlRegex.findAll(spannable).forEach { match ->
            val start = match.range.first
            val existingLinks = spannable.getSpans(start, match.range.last + 1, ClickableSpan::class.java)
            if (existingLinks.isEmpty()) {
                val url = match.value.trimUrlPunctuation()
                val end = start + url.length
                val span = if (spannable.isInQuoteRange(start, end)) QuoteUrlSpan(url, weakTextColor) else URLSpan(url)
                spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        FishPiMentionRegex.findAll(spannable).forEach { match ->
            val username = match.groupValues.getOrNull(1).orEmpty()
            if (spannable.getSpans(match.range.first, match.range.last + 1, ClickableSpan::class.java).isNotEmpty()) {
                return@forEach
            }
            val isQuoted = spannable.isInQuoteRange(match.range.first, match.range.last + 1)
            val mentionColor = if (isQuoted) weakTextColor else accentColor
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onMentionClick(username)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = mentionColor
                        ds.typeface = if (isQuoted) Typeface.DEFAULT else Typeface.DEFAULT_BOLD
                        ds.isUnderlineText = false
                    }
                },
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            spannable.setSpan(
                ForegroundColorSpan(mentionColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            if (!isQuoted) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        spannable.neutralizeQuoteClickableSpans()
        return spannable
    }

    private fun SpannableStringBuilder.isInQuoteRange(start: Int, end: Int): Boolean =
        getSpans(start, end, ForegroundColorSpan::class.java).any { span ->
            span.foregroundColor == weakTextColor &&
                getSpanStart(span) <= start &&
                getSpanEnd(span) >= end
        }

    private fun SpannableStringBuilder.neutralizeQuoteClickableSpans() {
        getSpans(0, length, ClickableSpan::class.java).forEach { span ->
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            if (start < 0 || end <= start || !isInQuoteRange(start, end) || span is QuoteNeutralSpan) {
                return@forEach
            }
            val flags = getSpanFlags(span)
            removeSpan(span)
            val next = when (span) {
                is URLSpan -> QuoteUrlSpan(span.url, weakTextColor)
                else -> QuoteTintClickableSpan(span, weakTextColor)
            }
            setSpan(next, start, end, flags)
        }
    }

    private fun trimTrailingWhitespace(source: CharSequence): SpannableStringBuilder {
        var end = source.length
        while (end > 0 && source[end - 1].isWhitespace()) {
            end--
        }
        return if (end == source.length) {
            SpannableStringBuilder(source)
        } else {
            SpannableStringBuilder(source.subSequence(0, end))
        }
    }

    private fun cacheKey(key: String, source: String): String =
        "$cachePrefix|$key|${source.hashCode()}|$styleHash"
}

private interface QuoteNeutralSpan

private class QuoteUrlSpan(
    url: String,
    private val color: Int,
) : URLSpan(url), QuoteNeutralSpan {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.color = color
        ds.isUnderlineText = false
    }
}

private class QuoteTintClickableSpan(
    private val delegate: ClickableSpan,
    private val color: Int,
) : ClickableSpan(), QuoteNeutralSpan {
    override fun onClick(widget: View) {
        delegate.onClick(widget)
    }

    override fun updateDrawState(ds: TextPaint) {
        delegate.updateDrawState(ds)
        ds.color = color
        ds.isUnderlineText = false
    }
}

internal fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()
