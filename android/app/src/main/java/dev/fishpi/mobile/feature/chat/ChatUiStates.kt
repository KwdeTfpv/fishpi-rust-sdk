package dev.fishpi.mobile.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.fishpi.mobile.data.AtUserCandidate
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.feature.chat.model.ChatMentionCandidateUiModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class ChatUiStates(controller: ChatController) {
    val chatState = mutableStateOf(controller.state.value)
    val messages = mutableStateOf(controller.legacyMessages.value)
    val renderModel = mutableStateOf(controller.renderModel.value)
    val interaction = mutableStateOf(controller.legacyInteractionState.value)
    val composer = mutableStateOf(controller.legacyComposerState.value.coldOnly())
    val draft = mutableStateOf(controller.legacyComposerState.value.input)
    val mentionCandidates = mutableStateOf(
        controller.legacyComposerState.value.atCandidates.mapToUiModels(),
    )
}

internal fun ChatLegacyComposerState.coldOnly(): ChatLegacyComposerState {
    val isCold = input.isEmpty() &&
        cursorPosition == 0 &&
        atQuery == null &&
        atAnchor == null &&
        atCandidates.isEmpty()
    if (isCold) return this
    return copy(
        input = "",
        cursorPosition = 0,
        atQuery = null,
        atAnchor = null,
        atCandidates = emptyList(),
    )
}

internal fun List<AtUserCandidate>.mapToUiModels(): List<ChatMentionCandidateUiModel> =
    if (isEmpty()) {
        emptyList()
    } else {
        map { candidate ->
            ChatMentionCandidateUiModel(
                username = candidate.userName,
                displayName = candidate.userName,
                avatarUrl = candidate.avatarUrl,
            )
        }
    }

@Composable
internal fun rememberChatUiStates(controller: ChatController): ChatUiStates {
    val states = remember(controller) { ChatUiStates(controller) }
    LaunchedEffect(controller) {
        launch { controller.state.collect { states.chatState.value = it } }
        launch { controller.legacyMessages.collect { states.messages.value = it } }
        launch { controller.renderModel.collect { states.renderModel.value = it } }
        launch { controller.legacyInteractionState.collect { states.interaction.value = it } }
        launch {
            controller.legacyComposerState
                .map { it.coldOnly() }
                .distinctUntilChanged()
                .collect { states.composer.value = it }
        }
        launch {
            controller.legacyComposerState
                .map { it.input }
                .distinctUntilChanged()
                .collect { states.draft.value = it }
        }
        launch {
            controller.legacyComposerState
                .map { it.atCandidates.mapToUiModels() }
                .distinctUntilChanged()
                .collect { states.mentionCandidates.value = it }
        }
    }
    return states
}

internal class ChatRouteOverlayState {
    val previewLinkUrl = mutableStateOf<String?>(null)
    val actionMessage = mutableStateOf<ChatRoomMessage?>(null)
    val blockedMessagesOpen = mutableStateOf(false)
    val pluginManagerOpen = mutableStateOf(false)
    val emojiManagerOpen = mutableStateOf(false)
    val routeError = mutableStateOf<String?>(null)
}
