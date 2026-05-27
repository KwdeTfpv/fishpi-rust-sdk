package dev.fishpi.mobile.feature.article.model

import dev.fishpi.mobile.data.ArticleCommentView

internal data class ArticleDetailUiModel(
    val id: String,
    val title: String,
    val author: String,
    val authorUserName: String,
    val avatar: String,
    val time: String,
    val tags: String,
    val markdown: String,
    val imageUrls: List<String>,
    val linkUrls: List<String>,
    val goodCount: Long,
    val badCount: Long,
    val thankCount: Long,
    val collectCount: Long,
    val watchCount: Long,
    val commentCount: Long,
    val viewCount: Long,
    val following: Boolean,
    val watching: Boolean,
    val thanked: Boolean,
    val rewarded: Boolean,
    val rewardedCount: Long,
    val rewardPoint: Long,
    val rewardContent: String,
    val voteState: Int,
    val commentNextPage: Int,
    val commentHasMore: Boolean,
    val comments: List<ArticleCommentView>,
)
