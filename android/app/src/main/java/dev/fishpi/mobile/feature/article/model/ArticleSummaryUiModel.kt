package dev.fishpi.mobile.feature.article.model

internal data class ArticleSummaryUiModel(
    val id: String,
    val title: String,
    val author: String,
    val time: String,
    val tags: String,
    val preview: String,
    val commentCount: Long,
    val goodCount: Long,
    val viewCount: Long,
    val sticky: Boolean,
    val perfect: Boolean,
    val avatar: String,
    val thumbnail: String,
)
