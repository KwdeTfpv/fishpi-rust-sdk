package dev.fishpi.mobile.feature.article.mapper

import dev.fishpi.mobile.data.ArticleDetailView
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.feature.article.model.ArticleDetailUiModel
import dev.fishpi.mobile.feature.article.model.ArticleSummaryUiModel

internal fun ArticleSummary.toArticleSummaryUiModel(): ArticleSummaryUiModel =
    ArticleSummaryUiModel(
        id = id,
        title = title,
        author = author,
        time = time,
        tags = tags,
        preview = preview,
        commentCount = commentCount,
        goodCount = goodCount,
        viewCount = viewCount,
        sticky = sticky,
        perfect = perfect,
        avatar = avatar,
        thumbnail = thumbnail,
    )

internal fun ArticleSummaryUiModel.toArticleSummary(): ArticleSummary =
    ArticleSummary(
        id = id,
        title = title,
        author = author,
        time = time,
        tags = tags,
        preview = preview,
        commentCount = commentCount,
        goodCount = goodCount,
        viewCount = viewCount,
        sticky = sticky,
        perfect = perfect,
        avatar = avatar,
        thumbnail = thumbnail,
    )

internal fun ArticleDetailView.toArticleDetailUiModel(): ArticleDetailUiModel =
    ArticleDetailUiModel(
        id = id,
        title = title,
        author = author,
        authorUserName = authorUserName,
        avatar = avatar,
        time = time,
        tags = tags,
        markdown = markdown,
        imageUrls = imageUrls,
        linkUrls = linkUrls,
        goodCount = goodCount,
        badCount = badCount,
        thankCount = thankCount,
        collectCount = collectCount,
        watchCount = watchCount,
        commentCount = commentCount,
        viewCount = viewCount,
        following = following,
        watching = watching,
        thanked = thanked,
        rewarded = rewarded,
        rewardedCount = rewardedCount,
        rewardPoint = rewardPoint,
        rewardContent = rewardContent,
        voteState = voteState,
        commentNextPage = commentNextPage,
        commentHasMore = commentHasMore,
        comments = comments,
    )

internal fun ArticleDetailView.toSummary(): ArticleSummary =
    ArticleSummary(
        id = id,
        title = title,
        author = author,
        time = time,
        tags = tags,
        preview = markdown,
        commentCount = commentCount,
        goodCount = goodCount,
        viewCount = viewCount,
        sticky = false,
        perfect = false,
        avatar = avatar,
        thumbnail = imageUrls.firstOrNull().orEmpty(),
    )
