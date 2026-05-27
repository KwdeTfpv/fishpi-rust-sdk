package dev.fishpi.mobile.utils

import com.vdurmont.emoji.EmojiParser

fun String.renderFishPiEmojiShortcodes(): String {
    if (indexOf(':') < 0) return this
    val builder = StringBuilder(length)
    val pending = StringBuilder()
    var index = 0
    var inFence = false
    var inInlineCode = false
    fun flushPending() {
        if (pending.isNotEmpty()) {
            builder.append(EmojiParser.parseToUnicode(pending.toString()))
            pending.clear()
        }
    }
    while (index < length) {
        if (startsWith("```", index)) {
            flushPending()
            inFence = !inFence
            builder.append("```")
            index += 3
            continue
        }
        val char = this[index]
        if (!inFence && char == '`') {
            flushPending()
            inInlineCode = !inInlineCode
            builder.append(char)
            index += 1
            continue
        }
        if (inFence || inInlineCode) {
            builder.append(char)
        } else {
            pending.append(char)
        }
        index += 1
    }
    flushPending()
    return builder.toString()
}
