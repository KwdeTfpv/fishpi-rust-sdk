package dev.fishpi.mobile.data

import android.net.Uri
import dev.fishpi.mobile.utils.attachmentPlaceholderIndexes
import dev.fishpi.mobile.utils.insertAttachmentPlaceholder
import dev.fishpi.mobile.utils.removeAttachmentPlaceholder
import dev.fishpi.mobile.utils.resolveAttachmentPlaceholders

data class ComposerAttachment(
    val index: Int,
    val uploadId: Long,
    val previewUri: Uri,
    val isVideo: Boolean,
    val status: AttachmentUpload.Status = AttachmentUpload.Status.Uploading,
    val uploaded: UploadedChatFile? = null,
    val error: String? = null,
    val referenced: Boolean = true,
)

data class ComposerAttachments(
    val items: List<ComposerAttachment> = emptyList(),
) {
    val isEmpty: Boolean get() = items.isEmpty()

    val hasPendingUpload: Boolean
        get() = items.any { it.referenced && it.status == AttachmentUpload.Status.Uploading }

    val hasSendableAttachment: Boolean
        get() = items.any { it.referenced && it.status != AttachmentUpload.Status.Failed }

    val pendingUploadIds: List<Long>
        get() = items.filter { it.referenced && it.status == AttachmentUpload.Status.Uploading }
            .map { it.uploadId }

    val allUploadIds: List<Long> get() = items.map { it.uploadId }

    fun append(
        text: String,
        cursor: Int,
        uploadId: Long,
        previewUri: Uri,
        isVideo: Boolean,
    ): Triple<ComposerAttachments, String, Int> {
        val index = (items.maxOfOrNull { it.index } ?: 0) + 1
        val (nextText, nextCursor) = insertAttachmentPlaceholder(text, index, cursor)
        val attachment = ComposerAttachment(
            index = index,
            uploadId = uploadId,
            previewUri = previewUri,
            isVideo = isVideo,
        )
        return Triple(copy(items = items + attachment), nextText, nextCursor)
    }

    fun removeAt(text: String, index: Int): Pair<ComposerAttachments, String> {
        if (items.none { it.index == index }) return this to text
        val nextText = removeAttachmentPlaceholder(text, index)
        val nextItems = items
            .filterNot { it.index == index }
            .map { if (it.index > index) it.copy(index = it.index - 1) else it }
        return copy(items = nextItems) to nextText
    }

    fun syncFrom(uploads: Map<Long, AttachmentUpload>): ComposerAttachments {
        if (items.isEmpty()) return this
        val next = items.map { item ->
            val upload = uploads[item.uploadId] ?: return@map item
            item.copy(status = upload.status, uploaded = upload.uploaded, error = upload.error)
        }
        return if (next == items) this else copy(items = next)
    }

    fun syncReferences(text: String): ComposerAttachments {
        if (items.isEmpty()) return this
        val present = attachmentPlaceholderIndexes(text).toSet()
        val next = items.map { it.copy(referenced = it.index in present) }
        return if (next == items) this else copy(items = next)
    }

    fun resolve(text: String): String {
        val markdown = items
            .filter { it.referenced }
            .mapNotNull { item -> item.uploaded?.markdown?.let { item.index to it } }
            .toMap()
        return resolveAttachmentPlaceholders(text, markdown)
    }
}
