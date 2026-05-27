package dev.fishpi.mobile.feature.home.model

internal data class HomeArticleUiModel(
    val id: String,
    val title: String,
    val author: String,
    val time: String,
    val tags: String,
    val preview: String,
    val commentCount: Long,
    val goodCount: Long,
    val viewCount: Long,
    val avatar: String,
    val thumbnail: String,
)
