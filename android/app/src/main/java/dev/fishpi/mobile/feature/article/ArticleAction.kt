package dev.fishpi.mobile.feature.article

import dev.fishpi.mobile.data.ArticleCommentView
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.feature.article.model.ArticleFilterUiModel

internal sealed interface ArticleAction {
    data object RefreshList : ArticleAction
    data object LoadMoreList : ArticleAction
    data class ChangeFilter(val filter: ArticleFilterUiModel) : ArticleAction
    data class ChangeTagInput(val value: String) : ArticleAction
    data object ApplyTag : ArticleAction
    data object ClearTag : ArticleAction
    data class OpenArticle(val article: ArticleSummary) : ArticleAction
    data class OpenArticleById(val articleId: String) : ArticleAction
    data object CloseDetail : ArticleAction
    data object LoadMoreComments : ArticleAction
    data class ChangeCommentInput(val value: String) : ArticleAction
    data object SendComment : ArticleAction
    data object PickCommentImage : ArticleAction
    data object CaptureCommentImage : ArticleAction
    data class UploadCommentImage(val path: String) : ArticleAction
    data class ShowPickerError(val message: String) : ArticleAction
    data class ReplyToComment(val comment: ArticleCommentView) : ArticleAction
    data object CancelReply : ArticleAction
    data object ToggleEmoji : ArticleAction
    data object DismissEmoji : ArticleAction
    data class PickEmojiGroup(val groupId: String) : ArticleAction
    data class PickEmoji(val item: EmojiItemView) : ArticleAction
    data object VoteUp : ArticleAction
    data object VoteDown : ArticleAction
    data object Thank : ArticleAction
    data class VoteCommentUp(val comment: ArticleCommentView) : ArticleAction
    data class VoteCommentDown(val comment: ArticleCommentView) : ArticleAction
    data class ThankComment(val comment: ArticleCommentView) : ArticleAction
    data object ToggleFollow : ArticleAction
    data object Watch : ArticleAction
    data object RequestRewardArticle : ArticleAction
    data object ConfirmRewardArticle : ArticleAction
    data object DismissRewardConfirm : ArticleAction
    data class ShowImagePreview(val url: String) : ArticleAction
    data class ShowLinkPreview(val url: String) : ArticleAction
    data class ShowVideoPreview(val url: String) : ArticleAction
    data object DismissOverlay : ArticleAction
    data object ShareArticle : ArticleAction
    data class OpenUserProfile(val username: String) : ArticleAction
    data object OpenPublish : ArticleAction
    data object ClosePublish : ArticleAction
    data class PublishCompleted(val articleId: String) : ArticleAction
    data object ClearError : ArticleAction
}
