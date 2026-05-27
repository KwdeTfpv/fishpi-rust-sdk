package dev.fishpi.mobile.chatui

internal object ChatMarkdownChunker {
    fun split(input: String, targetSize: Int = 1000): List<String> {
        if (input.length <= targetSize) return listOf(input)
        val rawChunks = mutableListOf<String>()
        var start = 0
        while (start < input.length) {
            val remaining = input.length - start
            if (remaining <= targetSize) {
                rawChunks += input.substring(start)
                break
            }
            val idealEnd = start + targetSize
            val end = findSplitIndex(input, start, idealEnd).coerceAtLeast(start + 1)
            rawChunks += input.substring(start, end)
            start = end
        }
        return balanceFenceChunks(rawChunks.filter(String::isNotEmpty))
    }

    private fun findSplitIndex(input: String, start: Int, idealEnd: Int): Int {
        val min = (idealEnd - 250).coerceAtLeast(start + 1)
        val max = (idealEnd + 250).coerceAtMost(input.length)
        val candidates = listOf("\n\n", "\n", " ")
        candidates.forEach { delimiter ->
            val before = input.lastIndexOf(delimiter, idealEnd).takeIf { it >= min }
            if (before != null) return before + delimiter.length
            val after = input.indexOf(delimiter, idealEnd).takeIf { it in idealEnd until max }
            if (after != null) return after + delimiter.length
        }
        return idealEnd
    }

    private fun balanceFenceChunks(chunks: List<String>): List<String> {
        val balanced = mutableListOf<String>()
        var openFence: String? = null
        chunks.forEach { chunk ->
            val prefix = openFence?.let { "$it\n" }.orEmpty()
            val currentOpenFence = updateOpenFence(chunk, openFence)
            val suffix = currentOpenFence?.let { "\n${it.takeWhile { marker -> marker == '`' }}" }.orEmpty()
            balanced += prefix + chunk + suffix
            openFence = currentOpenFence
        }
        return balanced
    }

    private fun updateOpenFence(chunk: String, initialFence: String?): String? {
        var openFence = initialFence
        chunk.lineSequence().forEach { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                openFence = if (openFence == null) {
                    trimmed
                } else {
                    null
                }
            }
        }
        return openFence
    }
}
