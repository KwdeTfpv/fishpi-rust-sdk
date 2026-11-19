package dev.fishpi.mobile.ui.state

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.ui.components.ErrorState
import dev.fishpi.mobile.ui.components.LoadingScreen
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.theme.FishPiType

internal data class ListLoadState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false,
) {
    val showsInitialLoading: Boolean get() = isLoading && isEmpty

    val showsError: Boolean get() = !isLoading && error != null && isEmpty

    val showsEmpty: Boolean get() = !isLoading && error == null && isEmpty

    val showsInlineError: Boolean get() = !isLoading && error != null && !isEmpty

    val canLoadMore: Boolean get() = hasMore && !isLoadingMore && error == null && !isEmpty

    fun startingLoad(): ListLoadState = copy(isLoading = true, isRefreshing = false, error = null)

    fun refreshing(): ListLoadState = copy(isRefreshing = true, isLoading = false, error = null)

    fun loaded(isEmpty: Boolean): ListLoadState = copy(
        isLoading = false,
        isRefreshing = false,
        isLoadingMore = false,
        error = null,
        isEmpty = isEmpty,
    )

    fun failed(message: String?): ListLoadState = copy(
        isLoading = false,
        isRefreshing = false,
        isLoadingMore = false,
        error = message,
    )

    fun loadingMore(): ListLoadState = copy(isLoadingMore = true, error = null)

    fun loadedMore(isEmpty: Boolean, hasMore: Boolean): ListLoadState = copy(
        isLoadingMore = false,
        hasMore = hasMore,
        isEmpty = isEmpty,
    )

    fun exhausted(): ListLoadState = copy(isLoadingMore = false, hasMore = false)
}

@Composable
internal fun FishPiListShell(
    state: ListLoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loadingMessage: String = "加载中...",
    emptyMessage: String = "暂无内容",
    loading: @Composable () -> Unit = { LoadingScreen(loadingMessage) },
    error: @Composable (String) -> Unit = { message -> ErrorState(message, onRetry) },
    empty: @Composable () -> Unit = { FishPiListEmptyText(emptyMessage) },
    content: @Composable ColumnScope.() -> Unit,
) {
    when {
        state.showsInitialLoading -> loading()
        state.showsError -> error(state.error ?: "加载失败")
        else -> Column(modifier = modifier) {
            if (state.showsEmpty) {
                empty()
            } else {
                content()
            }
            if (state.isLoadingMore) {
                FishPiListLoadingMore()
            }
            state.error?.takeIf { !state.isEmpty }?.let { message ->
                FishPiListInlineError(message = message, onRetry = onRetry)
            }
        }
    }
}

@Composable
internal fun FishPiListEmptyText(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = FishPiType.secondary,
            color = FishPiTheme.weakText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun FishPiListLoadingMore() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "加载更多...",
            style = FishPiType.caption,
            color = FishPiTheme.weakText,
        )
    }
}

@Composable
internal fun FishPiListInlineError(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FishPiTheme.spacingSection, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = FishPiType.caption,
            color = FishPiTheme.error,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "重试",
            style = FishPiType.caption,
            color = FishPiTheme.accent,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clickable(onClick = onRetry),
        )
    }
}
