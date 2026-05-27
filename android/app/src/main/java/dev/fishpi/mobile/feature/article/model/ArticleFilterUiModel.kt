package dev.fishpi.mobile.feature.article.model

internal data class ArticleFilterUiModel(
    val key: String,
    val label: String,
)

internal val BaseArticleFilters = listOf(
    ArticleFilterUiModel("recent", "最新"),
    ArticleFilterUiModel("hot", "热门"),
    ArticleFilterUiModel("good", "点赞"),
    ArticleFilterUiModel("reply", "回复"),
)

internal val RecentArticleFilters = BaseArticleFilters + ArticleFilterUiModel("long", "长篇")

internal val TaggedArticleFilters = BaseArticleFilters + ArticleFilterUiModel("perfect", "优选")

internal fun articleFiltersForTag(tag: String): List<ArticleFilterUiModel> =
    if (tag.isBlank()) RecentArticleFilters else TaggedArticleFilters
