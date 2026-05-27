package dev.fishpi.mobile.feature.breezemoon

internal sealed interface BreezemoonAction {
    data object Initialize : BreezemoonAction
    data object Refresh : BreezemoonAction
    data object LoadMore : BreezemoonAction
    data object Publish : BreezemoonAction
    data class ChangeInput(val value: String) : BreezemoonAction
    data object OpenAttachmentPanel : BreezemoonAction
    data object CloseAttachmentPanel : BreezemoonAction
    data object ToggleAttachmentPanel : BreezemoonAction
    data object RequestGalleryAttachment : BreezemoonAction
    data object RequestCameraAttachment : BreezemoonAction
    data class UploadAttachment(val path: String) : BreezemoonAction
    data object ToggleEmoji : BreezemoonAction
    data object CloseEmoji : BreezemoonAction
    data class SelectEmojiGroup(val groupId: String) : BreezemoonAction
    data class PickEmoji(val name: String, val url: String) : BreezemoonAction
    data class OpenUserProfile(val username: String) : BreezemoonAction
    data object DismissUserProfile : BreezemoonAction
    data object RetryUserProfile : BreezemoonAction
    data object ConsumeScrollToBottom : BreezemoonAction
    data class SetError(val message: String?) : BreezemoonAction
}
