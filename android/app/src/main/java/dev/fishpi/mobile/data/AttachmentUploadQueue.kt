package dev.fishpi.mobile.data

import android.content.Context
import android.net.Uri
import dev.fishpi.mobile.utils.CHAT_UPLOAD_MAX_BYTES
import dev.fishpi.mobile.utils.compressImageForUpload
import dev.fishpi.mobile.utils.copyUriToCacheFile
import dev.fishpi.mobile.utils.isRecompressableImageMime
import dev.fishpi.mobile.utils.toReadableMediaSize
import dev.fishpi.mobile.utils.uriContentSize
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class AttachmentUpload(
    val id: Long,
    val previewUri: Uri,
    val isVideo: Boolean,
    val status: Status = Status.Uploading,
    val uploaded: UploadedChatFile? = null,
    val error: String? = null,
) {
    enum class Status { Uploading, Done, Failed }

    val isTerminal: Boolean get() = status != Status.Uploading
}

internal class AttachmentUploadQueue(
    private val context: Context,
    private val api: FishPiApiClient = FishPiApiClient.shared,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val concurrency = Semaphore(MAX_CONCURRENT_UPLOADS)
    private val nextId = AtomicLong(1L)
    private val jobs = ConcurrentHashMap<Long, Job>()

    private val _uploads = MutableStateFlow<Map<Long, AttachmentUpload>>(emptyMap())
    val uploads: StateFlow<Map<Long, AttachmentUpload>> = _uploads

    fun enqueue(apiKey: String, uri: Uri, isVideo: Boolean): Long {
        val id = nextId.getAndIncrement()
        _uploads.update { it + (id to AttachmentUpload(id = id, previewUri = uri, isVideo = isVideo)) }
        launchUpload(id, apiKey, uri, isVideo)
        return id
    }

    fun retry(apiKey: String, id: Long) {
        val target = _uploads.value[id] ?: return
        if (target.status != AttachmentUpload.Status.Failed) return
        _uploads.update {
            it + (id to target.copy(status = AttachmentUpload.Status.Uploading, error = null))
        }
        launchUpload(id, apiKey, target.previewUri, target.isVideo)
    }

    fun remove(id: Long) {
        jobs.remove(id)?.cancel()
        _uploads.update { it - id }
    }

    fun remove(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        ids.forEach { jobs.remove(it)?.cancel() }
        _uploads.update { current -> current - ids.toSet() }
    }

    suspend fun awaitSettled(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val watched = ids.toSet()
        _uploads.first { snapshot ->
            watched.all { id -> snapshot[id]?.isTerminal ?: true }
        }
    }

    private fun launchUpload(id: Long, apiKey: String, uri: Uri, isVideo: Boolean) {
        val job = scope.launch {
            concurrency.withPermit {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val prepared = prepareForUpload(uri, isVideo)
                        try {
                            api.uploadChatFile(apiKey, prepared.absolutePath)
                        } finally {
                            prepared.delete()
                        }
                    }
                }.onSuccess { uploaded ->
                    _uploads.update { current ->
                        val existing = current[id] ?: return@update current
                        current + (
                            id to existing.copy(
                                status = AttachmentUpload.Status.Done,
                                uploaded = uploaded,
                                error = null,
                            )
                            )
                    }
                }.onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uploads.update { current ->
                        val existing = current[id] ?: return@update current
                        current + (
                            id to existing.copy(
                                status = AttachmentUpload.Status.Failed,
                                error = throwable.message ?: "上传失败",
                            )
                            )
                    }
                }
            }
        }
        jobs[id] = job
        job.invokeOnCompletion { jobs.remove(id, job) }
    }

    private fun prepareForUpload(uri: Uri, isVideo: Boolean): File {
        val mimeType = context.contentResolver.getType(uri)
        if (!isVideo && isRecompressableImageMime(mimeType)) {
            val compressed = context.compressImageForUpload(uri, "chat_uploads", "upload")
            if (compressed != null) {
                if (compressed.length() <= CHAT_UPLOAD_MAX_BYTES) return compressed
                compressed.delete()
            }
        }

        context.uriContentSize(uri)?.let { size ->
            if (size > CHAT_UPLOAD_MAX_BYTES) error(sizeErrorMessage(size))
        }

        val fallback = context.copyUriToCacheFile(
            uri = uri,
            directoryName = "chat_uploads",
            prefix = "upload",
            defaultExtension = if (isVideo) "mp4" else "jpg",
        )
        val size = fallback.length()
        if (size > CHAT_UPLOAD_MAX_BYTES) {
            fallback.delete()
            error(sizeErrorMessage(size))
        }
        return fallback
    }

    private fun sizeErrorMessage(size: Long): String =
        "文件过大，当前 ${size.toReadableMediaSize()}，最大 ${CHAT_UPLOAD_MAX_BYTES.toReadableMediaSize()}"

    companion object {
        private const val MAX_CONCURRENT_UPLOADS = 2

        @Volatile
        private var instance: AttachmentUploadQueue? = null

        fun shared(context: Context): AttachmentUploadQueue {
            return instance ?: synchronized(this) {
                instance ?: AttachmentUploadQueue(context.applicationContext).also { instance = it }
            }
        }
    }
}
