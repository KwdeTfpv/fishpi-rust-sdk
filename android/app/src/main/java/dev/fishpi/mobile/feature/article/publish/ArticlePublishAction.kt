package dev.fishpi.mobile.feature.article.publish

import androidx.compose.ui.text.input.TextFieldValue

internal sealed interface ArticlePublishAction {
    data object Initialize : ArticlePublishAction
    data object Close : ArticlePublishAction
    data class ChangeTitle(val value: String) : ArticlePublishAction
    data class ChangeContent(val value: String) : ArticlePublishAction
    data class ChangeTags(val value: TextFieldValue) : ArticlePublishAction
    data class ChangeRewardContent(val value: String) : ArticlePublishAction
    data class ChangeRewardPoint(val value: String) : ArticlePublishAction
    data class ChangeQnaOfferPoint(val value: String) : ArticlePublishAction
    data class ChangeCommentable(val value: Boolean) : ArticlePublishAction
    data class ChangeAnonymous(val value: Boolean) : ArticlePublishAction
    data class ChangeNotifyFollowers(val value: Boolean) : ArticlePublishAction
    data class ChangeShowInList(val value: Boolean) : ArticlePublishAction
    data class ChangeOriginalStatement(val value: Boolean) : ArticlePublishAction
    data object LoadDrafts : ArticlePublishAction
    data object OpenDrafts : ArticlePublishAction
    data object DismissDrafts : ArticlePublishAction
    data class OpenDraft(val draftId: String) : ArticlePublishAction
    data class DeleteDraft(val draftId: String) : ArticlePublishAction
    data object SaveDraft : ArticlePublishAction
    data object RequestPublish : ArticlePublishAction
    data object ConfirmGoodArticlePublish : ArticlePublishAction
    data object ConfirmNormalPublish : ArticlePublishAction
    data object DismissGoodArticleConfirm : ArticlePublishAction
    data object PickContentImage : ArticlePublishAction
    data object PickRewardImage : ArticlePublishAction
    data class UploadContentImage(val path: String) : ArticlePublishAction
    data class UploadRewardImage(val path: String) : ArticlePublishAction
    data class ShowPickerError(val message: String) : ArticlePublishAction
    data class InsertContentBlock(val text: String) : ArticlePublishAction
    data class InsertRewardBlock(val text: String) : ArticlePublishAction
    data class AppendTag(val tag: String) : ArticlePublishAction
    data class ShowPreview(val target: ArticlePublishEditorTarget) : ArticlePublishAction
    data object DismissPreview : ArticlePublishAction
    data object ClearError : ArticlePublishAction
}
