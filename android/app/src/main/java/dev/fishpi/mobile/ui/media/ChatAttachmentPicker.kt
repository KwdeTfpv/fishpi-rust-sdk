package dev.fishpi.mobile.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.fishpi.mobile.utils.copyUriToCacheFile
import dev.fishpi.mobile.utils.createCacheFile
import dev.fishpi.mobile.utils.CHAT_UPLOAD_MAX_BYTES
import dev.fishpi.mobile.utils.chatUploadSizeError
import dev.fishpi.mobile.utils.toReadableMediaSize
import dev.fishpi.mobile.utils.uriContentSize
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChatAttachmentPicker(
    private val openGalleryAction: () -> Unit,
    private val openCameraAction: () -> Unit,
    private val openFileAction: () -> Unit,
) {
    fun openGallery() = openGalleryAction()
    fun openCamera() = openCameraAction()
    fun openFile() = openFileAction()
}

@Composable
internal fun rememberChatAttachmentPicker(
    onPickedPath: (String) -> Unit,
    onError: (String) -> Unit,
): ChatAttachmentPicker {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraOpen by remember { mutableStateOf(false) }

    fun processPickedGalleryUri(uri: Uri) {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val isVideo = mimeType.startsWith("video/")
        val defaultExtension = galleryDefaultExtension(isVideo, mimeType)
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val sourceSize = context.uriContentSize(uri)
                    if (sourceSize != null && sourceSize > CHAT_UPLOAD_MAX_BYTES) {
                        error("文件过大，当前 ${sourceSize.toReadableMediaSize()}，最大 ${CHAT_UPLOAD_MAX_BYTES.toReadableMediaSize()}")
                    }
                    context.copyUriToCacheFile(uri, "chat_uploads", "gallery", defaultExtension)
                }
            }.onSuccess { file ->
                val sizeError = file.chatUploadSizeError()
                if (sizeError == null) {
                    onPickedPath(file.absolutePath)
                } else {
                    file.delete()
                    onError(sizeError)
                }
            }.onFailure { err ->
                onError(err.message ?: "读取媒体失败")
            }
        }
    }

    fun processPickedFileUri(uri: Uri) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val sourceSize = context.uriContentSize(uri)
                    if (sourceSize != null && sourceSize > CHAT_UPLOAD_MAX_BYTES) {
                        error("文件过大，当前 ${sourceSize.toReadableMediaSize()}，最大 ${CHAT_UPLOAD_MAX_BYTES.toReadableMediaSize()}")
                    }
                    context.copyUriToCacheFile(uri, "chat_uploads", "file", "bin")
                }
            }.onSuccess { file ->
                val sizeError = file.chatUploadSizeError()
                if (sizeError == null) {
                    onPickedPath(file.absolutePath)
                } else {
                    file.delete()
                    onError(sizeError)
                }
            }.onFailure { err ->
                onError(err.message ?: "读取文件失败")
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            processPickedFileUri(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            processPickedGalleryUri(uri)
        }
    }
    val openGalleryPicker = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = cameraFile
        cameraFile = null
        if (success && file != null && file.exists()) {
            val sizeError = file.chatUploadSizeError()
            if (sizeError == null) {
                onPickedPath(file.absolutePath)
            } else {
                file.delete()
                onError(sizeError)
            }
        } else if (file != null) {
            file.delete()
        }
    }
    val openCameraCapture = {
        runCatching {
            val file = context.createCacheFile("chat_uploads", "camera", "jpg")
            cameraFile = file
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.onSuccess { uri ->
            cameraLauncher.launch(uri)
        }.onFailure { err ->
            onError(err.message ?: "打开相机失败")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            openCameraCapture()
        } else {
            onError("需要相机权限才能拍照")
        }
        pendingCameraOpen = false
    }

    return remember(context) {
        ChatAttachmentPicker(
            openGalleryAction = {
                openGalleryPicker()
            },
            openCameraAction = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCameraCapture()
                } else if (!pendingCameraOpen) {
                    pendingCameraOpen = true
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            openFileAction = {
                fileLauncher.launch(arrayOf("*/*"))
            },
        )
    }
}

private fun galleryDefaultExtension(isVideo: Boolean, mimeType: String): String {
    val normalized = mimeType.trim().lowercase()
    return when {
        normalized == "video/quicktime" -> "mov"
        normalized == "video/x-m4v" -> "m4v"
        normalized.startsWith("video/") -> normalized.substringAfter('/').ifBlank { "mp4" }
        normalized == "image/png" -> "png"
        normalized == "image/gif" -> "gif"
        normalized == "image/webp" -> "webp"
        isVideo -> "mp4"
        else -> "jpg"
    }
}

