package dev.fishpi.mobile.shared.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.fishpi.mobile.shared.message.native.NativeMessageList
import dev.fishpi.mobile.shared.message.native.NativeMessageListController
import dev.fishpi.mobile.shared.message.native.rememberNativeMessageListController

@Composable
internal fun DefaultMessageListUi(
    state: MessageRenderState,
    dispatch: (MessageAction) -> Unit,
    modifier: Modifier = Modifier,
    controller: NativeMessageListController = rememberNativeMessageListController(),
) {
    NativeMessageList(
        items = state.items,
        selfUsername = state.selfUsername,
        showAvatars = state.showAvatars,
        scrollToBottomRequest = state.scrollToBottomRequest,
        allowScrollToBottom = state.allowScrollToBottom,
        redPacketJumpTargetId = state.redPacketJumpTargetId,
        active = state.active,
        contentTopPaddingDp = state.contentTopPaddingDp,
        modifier = modifier,
        onLoadMore = { dispatch(MessageAction.LoadMoreRequested) },
        onNearBottomChanged = { dispatch(MessageAction.NearBottomChanged(it)) },
        onNearTopChanged = { dispatch(MessageAction.NearTopChanged(it)) },
        onImageClick = { images, index -> dispatch(MessageAction.ImageClicked(images, index)) },
        onLinkClick = { dispatch(MessageAction.LinkClicked(it)) },
        onLongPress = { dispatch(MessageAction.MessageLongPressed(it)) },
        onAvatarClick = { dispatch(MessageAction.AvatarClicked(it)) },
        onAvatarLongPress = { dispatch(MessageAction.AvatarLongPressed(it)) },
        onRedPacketClick = { dispatch(MessageAction.RedPacketClicked(it)) },
        onRedPacketGestureClick = { message, gesture -> dispatch(MessageAction.RedPacketGestureClicked(message, gesture)) },
        onReactionClick = { message, reaction -> dispatch(MessageAction.ReactionClicked(message, reaction)) },
        onRepeatClick = { dispatch(MessageAction.RepeatClicked(it)) },
        onVideoFullscreenClick = { dispatch(MessageAction.VideoFullscreenClicked(it)) },
        onTapBlankArea = { dispatch(MessageAction.BlankAreaTapped) },
        controller = controller,
    )
}
