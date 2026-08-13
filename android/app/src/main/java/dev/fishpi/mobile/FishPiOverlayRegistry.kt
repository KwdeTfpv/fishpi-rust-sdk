package dev.fishpi.mobile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FishPiOverlayRegistryImpl {
    private val stack = ArrayDeque<Long>()
    private val _activeHostId = MutableStateFlow<Long?>(null)
    val activeHostId: StateFlow<Long?> = _activeHostId.asStateFlow()
    private val idSeq = java.util.concurrent.atomic.AtomicLong(0L)

    fun nextId(): Long = idSeq.incrementAndGet()

    @Synchronized
    fun push(id: Long) {
        stack.addLast(id)
        _activeHostId.value = stack.lastOrNull()
    }

    @Synchronized
    fun pop(id: Long) {
        stack.remove(id)
        _activeHostId.value = stack.lastOrNull()
    }
}

internal val FishPiOverlayRegistry = FishPiOverlayRegistryImpl()
