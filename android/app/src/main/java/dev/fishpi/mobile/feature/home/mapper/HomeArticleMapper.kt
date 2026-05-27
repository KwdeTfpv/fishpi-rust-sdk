package dev.fishpi.mobile.feature.home.mapper

import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.feature.home.model.HomeArticleUiModel

internal fun ArticleSummary.toHomeArticleUiModel(): HomeArticleUiModel =
    HomeArticleUiModel(
        id = id,
        title = title,
        author = author,
        time = time,
        tags = tags,
        preview = preview,
        commentCount = commentCount,
        goodCount = goodCount,
        viewCount = viewCount,
        avatar = avatar,
        thumbnail = thumbnail,
    )
