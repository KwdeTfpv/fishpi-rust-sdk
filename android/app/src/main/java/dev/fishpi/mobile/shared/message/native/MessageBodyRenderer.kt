package dev.fishpi.mobile.shared.message.native

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dev.fishpi.mobile.chatui.MarkwonChatRenderer
import dev.fishpi.mobile.data.ChatRoomMessage
import kotlinx.coroutines.Job

internal class MessageBodyRenderer(
    private val context: Context,
    private val markdownRendererProvider: () -> MarkwonChatRenderer,
    private val styleText: (TextView) -> Unit,
    private val attachLongPress: (TextView, ChatRoomMessage, Boolean, View) -> Unit,
) {

    private var renderJob: Job? = null

    var textColor: Int = 0
        private set

    fun renderMarkdown(
        container: LinearLayout,
        messageKey: String,
        markdown: String,
        fallback: String,
        message: ChatRoomMessage,
        isMine: Boolean,
        anchorView: View,
        color: Int,
    ) {
        val textView = TextView(context).apply {
            styleText(this)
            attachLongPress(this, message, isMine, anchorView)
        }
        container.addView(textView)
        textColor = color
        renderJob?.cancel()
        val startedWith = color
        val job = markdownRendererProvider().renderInto(
            textView = textView,
            messageKey = messageKey,
            markdown = markdown,
            fallback = fallback,
            textColor = color,
        )
        renderJob = job
        job?.invokeOnCompletion {
            textView.post {
                if (textView.parent != null && textColor != startedWith) {
                    textView.setTextColor(textColor)
                }
            }
        }
    }

    fun renderPlain(container: LinearLayout, text: CharSequence, size: Float, color: Int) {
        textColor = color
        container.addView(TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            styleText(this)
        })
    }

    fun applyTextColor(root: View, from: Int, to: Int) {
        textColor = to
        recolor(root, from, to)
    }

    fun cancel() {
        renderJob?.cancel()
        renderJob = null
    }

    private fun recolor(view: View, from: Int, to: Int) {
        when (view) {
            is TextView -> if (view.currentTextColor == from) view.setTextColor(to)
            is android.view.ViewGroup -> {
                for (index in 0 until view.childCount) {
                    recolor(view.getChildAt(index), from, to)
                }
            }
        }
    }
}
