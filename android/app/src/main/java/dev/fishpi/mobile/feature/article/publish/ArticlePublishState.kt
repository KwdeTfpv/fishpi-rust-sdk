package dev.fishpi.mobile.feature.article.publish

import androidx.compose.ui.text.input.TextFieldValue
import dev.fishpi.mobile.data.ArticleDraftPayload
import dev.fishpi.mobile.data.ArticleDraftView

internal data class ArticlePublishState(
    val draftId: String = "",
    val title: String = "",
    val content: String = "",
    val tags: TextFieldValue = TextFieldValue(""),
    val rewardContent: String = "",
    val rewardPoint: String = "",
    val qnaOfferPoint: String = "",
    val commentable: Boolean = true,
    val anonymous: Boolean = false,
    val notifyFollowers: Boolean = false,
    val showInList: Boolean = true,
    val originalStatement: Boolean = true,
    val drafts: List<ArticleDraftView> = emptyList(),
    val showDrafts: Boolean = false,
    val showPreview: Boolean = false,
    val previewTarget: ArticlePublishEditorTarget = ArticlePublishEditorTarget.Content,
    val imageInsertTarget: ArticlePublishEditorTarget = ArticlePublishEditorTarget.Content,
    val pendingPublishPayload: ArticleDraftPayload? = null,
    val loadingDrafts: Boolean = false,
    val submitting: Boolean = false,
    val uploadingImage: Boolean = false,
    val error: String? = null,
    val titleError: String? = null,
    val contentError: String? = null,
    val tagsError: String? = null,
)

internal enum class ArticlePublishEditorTarget {
    Content,
    Reward,
}

internal val ArticleRecommendedTags = listOf("摸鱼", "日常", "技术", "分享", "编程", "生活", "学习", "随笔")
