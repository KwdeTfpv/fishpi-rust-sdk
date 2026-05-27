package dev.fishpi.mobile.feature.article

import dev.fishpi.mobile.core.ui.UiEffect

internal sealed interface ArticleEffect : UiEffect {
    data object DetailClosed : ArticleEffect
    data class OpenUserProfile(val username: String) : ArticleEffect
    data class ShareArticle(val title: String, val articleId: String) : ArticleEffect
    data object OpenCommentGallery : ArticleEffect
    data object OpenCommentCamera : ArticleEffect
    data class ShowMessage(val message: String) : ArticleEffect
    data class ShowError(val message: String) : ArticleEffect
}
