package dev.fishpi.mobile.data

import android.os.Handler
import org.json.JSONObject

internal class ArticleRealtimeClient : NativeRealtimeClient() {

    fun connect(
        apiKey: String,
        articleId: String,
        articleType: Int = 0,
        onHeatDelta: (String, Int) -> Unit,
    ) {
        connectInternal {
            val callback = NativeCallback(mainHandler, articleId, onHeatDelta)
            FishPiNative.connectArticle(apiKey, articleId, articleType, callback)
        }
    }

    override fun disconnectNative(handle: Long) {
        FishPiNative.disconnectArticle(handle)
    }

    private class NativeCallback(
        private val mainHandler: Handler,
        private val expectedArticleId: String,
        private val onHeatDelta: (String, Int) -> Unit,
    ) {
        @Suppress("unused")
        fun onEvent(payload: String) {
            val event = runCatching { JSONObject(payload) }.getOrNull() ?: return
            if (event.optString("event") != "articleHeat") return
            val articleId = event.optString("articleId")
            if (articleId.isNotBlank() && articleId != expectedArticleId) return
            val delta = when (event.optString("operation")) {
                "+" -> 1
                "-" -> -1
                else -> 0
            }
            if (delta != 0) {
                mainHandler.post { onHeatDelta(expectedArticleId, delta) }
            }
        }
    }
}
