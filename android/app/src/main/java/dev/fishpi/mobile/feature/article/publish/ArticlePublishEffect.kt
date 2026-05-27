package dev.fishpi.mobile.feature.article.publish

import dev.fishpi.mobile.core.ui.UiEffect

internal sealed interface ArticlePublishEffect : UiEffect {
    data object Closed : ArticlePublishEffect
    data object OpenContentImagePicker : ArticlePublishEffect
    data object OpenRewardImagePicker : ArticlePublishEffect
    data class Published(val articleId: String) : ArticlePublishEffect
    data class ShowMessage(val message: String) : ArticlePublishEffect
    data class ShowError(val message: String) : ArticlePublishEffect
}
