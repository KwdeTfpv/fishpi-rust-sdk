package dev.fishpi.mobile.feature.article

import dev.fishpi.mobile.data.ArticleDetailView
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.feature.article.model.ArticleFilterUiModel
import dev.fishpi.mobile.feature.article.model.ArticleOverlayState
import dev.fishpi.mobile.feature.article.model.ArticleSummaryUiModel
import dev.fishpi.mobile.feature.article.model.RecentArticleFilters

internal data class ArticleState(
    val articles: List<ArticleSummaryUiModel> = emptyList(),
    val rawArticles: List<ArticleSummary> = emptyList(),
    val filter: ArticleFilterUiModel = RecentArticleFilters.first(),
    val availableFilters: List<ArticleFilterUiModel> = RecentArticleFilters,
    val tagInput: String = "",
    val appliedTag: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextPage: Int = 1,
    val hasMore: Boolean = true,
    val selected: ArticleSummary? = null,
    val detail: ArticleDetailView? = null,
    val isLoadingDetail: Boolean = false,
    val error: String? = null,
    val commentInput: String = "",
    val isSendingComment: Boolean = false,
    val isArticleActionRunning: Boolean = false,
    val replyToCommentId: String = "",
    val replyTarget: String? = null,
    val replyFocusSignal: Int = 0,
    val dismissKeyboardSignal: Int = 0,
    val emojiPanelOpen: Boolean = false,
    val emojiGroups: List<EmojiGroupView> = emptyList(),
    val emojiItems: List<EmojiItemView> = emptyList(),
    val selectedEmojiGroupId: String = "",
    val isLoadingEmojiPack: Boolean = false,
    val emojiPackError: String? = null,
    val isUploadingCommentImage: Boolean = false,
    val overlay: ArticleOverlayState = ArticleOverlayState.None,
    val publishOpen: Boolean = false,
    val articleHeat: Long? = null,
    val rewardConfirmOpen: Boolean = false,
    val isRewarding: Boolean = false,
)
