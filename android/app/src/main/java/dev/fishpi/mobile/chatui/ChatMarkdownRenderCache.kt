package dev.fishpi.mobile.chatui

import android.text.Spanned
import java.util.LinkedHashMap

internal class ChatMarkdownRenderCache(
    private val maxEntries: Int = 300,
    private val maxChars: Int = 300_000,
    private val ttlMillis: Long = 10 * 60 * 1000L,
) {
    private data class Entry(
        val value: Spanned,
        val sourceLength: Int,
        val createdAtMs: Long,
        var lastAccessMs: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private var totalChars = 0

    @Synchronized
    fun get(key: String): Spanned? {
        val now = System.currentTimeMillis()
        val entry = entries[key] ?: return null
        if (now - entry.createdAtMs > ttlMillis) {
            removeEntry(key, entry)
            return null
        }
        entry.lastAccessMs = now
        return entry.value
    }

    @Synchronized
    fun put(key: String, value: Spanned, sourceLength: Int) {
        entries.remove(key)?.let { totalChars -= it.sourceLength }
        val now = System.currentTimeMillis()
        entries[key] = Entry(
            value = value,
            sourceLength = sourceLength.coerceAtLeast(0),
            createdAtMs = now,
            lastAccessMs = now,
        )
        totalChars += sourceLength.coerceAtLeast(0)
        trimExpired(now)
        trimToLimits()
    }

    @Synchronized
    fun trimToHalf() {
        val targetEntries = (maxEntries / 2).coerceAtLeast(1)
        val targetChars = (maxChars / 2).coerceAtLeast(1)
        trimTo(targetEntries, targetChars)
    }

    @Synchronized
    fun clear() {
        entries.clear()
        totalChars = 0
    }

    private fun trimExpired(now: Long) {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAtMs > ttlMillis) {
                totalChars -= entry.value.sourceLength
                iterator.remove()
            }
        }
    }

    private fun trimToLimits() {
        trimTo(maxEntries, maxChars)
    }

    private fun trimTo(entryLimit: Int, charLimit: Int) {
        val iterator = entries.iterator()
        while ((entries.size > entryLimit || totalChars > charLimit) && iterator.hasNext()) {
            val entry = iterator.next()
            totalChars -= entry.value.sourceLength
            iterator.remove()
        }
    }

    private fun removeEntry(key: String, entry: Entry) {
        entries.remove(key)
        totalChars -= entry.sourceLength
    }
}
