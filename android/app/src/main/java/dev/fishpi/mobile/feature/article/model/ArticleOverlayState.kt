package dev.fishpi.mobile.feature.article.model

internal sealed interface ArticleOverlayState {
    data object None : ArticleOverlayState
    data class Image(val url: String) : ArticleOverlayState
    data class Link(val url: String) : ArticleOverlayState
    data class Video(val url: String) : ArticleOverlayState
}
