package dev.fishpi.mobile.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun <T> CoroutineScope.launchIoCatching(
    onSuccess: (T) -> Unit,
    onFailure: (Throwable) -> Unit,
    onFinally: () -> Unit = {},
    block: suspend () -> T,
) = launch {
    runCatching {
        withContext(Dispatchers.IO) {
            block()
        }
    }.onSuccess(onSuccess)
        .onFailure(onFailure)
    onFinally()
}
