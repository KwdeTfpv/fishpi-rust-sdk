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
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.fishpi.mobile.utils.createCacheFile
import java.io.File

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
    onPicked: (uri: Uri, isVideo: Boolean) -> Unit,
    onError: (String) -> Unit,
): ChatAttachmentPicker {
    val context = androidx.compose.ui.platform.LocalContext.current
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraOpen by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            onPicked(uri, mimeType.startsWith("video/"))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            onPicked(uri, mimeType.startsWith("video/"))
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
            onPicked(
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
                false,
            )
        } else {
            file?.delete()
        }
    }
    val openCameraCapture = {
        runCatching {
            val file = context.createCacheFile("chat_uploads", "camera", "jpg")
            cameraFile = file
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
