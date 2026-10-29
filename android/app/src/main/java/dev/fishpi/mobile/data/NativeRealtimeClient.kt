package dev.fishpi.mobile.data

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicLong

internal abstract class NativeRealtimeClient {
    @Volatile
    private var nativeHandle: Long = 0
    private val connectionToken = AtomicLong(0L)
    private val handleLock = Any()
    protected val mainHandler = Handler(Looper.getMainLooper())

    protected abstract fun disconnectNative(handle: Long)

    protected fun connectInternal(
        connectingStatus: String? = null,
        failedStatus: String? = null,
        onStatus: ((String) -> Unit)? = null,
        connectNative: () -> Long,
    ) {
        disconnect()
        if (connectingStatus != null) {
            onStatus?.invoke(connectingStatus)
        }
        val token = connectionToken.incrementAndGet()
        Thread {
            val handle = connectNative()
            val stale = synchronized(handleLock) {
                if (token != connectionToken.get()) {
                    true
                } else {
                    nativeHandle = handle
                    false
                }
            }
            if (stale) {
                if (handle != 0L) {
                    disconnectNative(handle)
                }
                return@Thread
            }
            mainHandler.post {
                if (handle == 0L && failedStatus != null) {
                    onStatus?.invoke(failedStatus)
                }
            }
        }.start()
    }

    fun disconnect() {
        val handle = synchronized(handleLock) {
            connectionToken.incrementAndGet()
            val current = nativeHandle
            nativeHandle = 0
            current
        }
        if (handle != 0L) {
            Thread {
                disconnectNative(handle)
            }.start()
        }
    }

    protected fun currentHandle(): Long = nativeHandle

    fun close() {
        disconnect()
    }

    @Suppress("deprecation")
    protected fun finalize() {
        // Best-effort safety net if caller forgets disconnect().
        disconnect()
    }
}
