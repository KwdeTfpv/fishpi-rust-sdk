package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.data.AttachmentUpload
import dev.fishpi.mobile.data.ComposerAttachment
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.overlay.LocalImageViewer

@Composable
internal fun ComposerAttachmentStrip(
    attachments: List<ComposerAttachment>,
    onRemove: (ComposerAttachment) -> Unit,
    onRetry: (ComposerAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    val imageLoader = rememberFishPiImageLoader()
    val imageViewer = LocalImageViewer.current
    val previewable = attachments.filterNot { it.isVideo }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
    ) {
        items(attachments, key = { it.uploadId }) { attachment ->
            Box(
                modifier = Modifier
                    .size(THUMBNAIL_SIZE)
                    .clip(RoundedCornerShape(FishPiTheme.radiusField))
                    .background(FishPiTheme.surfaceContainer)
                    .border(
                        FishPiTheme.borderWidth,
                        if (attachment.referenced) {
                            FishPiTheme.outline.copy(alpha = 0.18f)
                        } else {
                            FishPiTheme.outline.copy(alpha = 0.08f)
                        },
                        RoundedCornerShape(FishPiTheme.radiusField),
                    )
                    .then(
                        if (attachment.isVideo) {
                            Modifier
                        } else {
                            Modifier.clickable {
                                val index = previewable.indexOfFirst { it.uploadId == attachment.uploadId }
                                if (index >= 0) {
                                    imageViewer.open(previewable.map { it.previewUri.toString() }, index)
                                }
                            }
                        },
                    ),
            ) {
                SubcomposeAsyncImage(
                    model = attachment.previewUri,
                    imageLoader = imageLoader,
                    contentDescription = "附件 ${attachment.index}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = FishPiTheme.weakText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )

                if (!attachment.referenced) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FishPiTheme.surface.copy(alpha = 0.66f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "未使用",
                            color = FishPiTheme.weakText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                when (attachment.status) {
                    AttachmentUpload.Status.Uploading -> UploadingScrim()
                    AttachmentUpload.Status.Failed -> FailedScrim(onRetry = { onRetry(attachment) })
                    AttachmentUpload.Status.Done -> Unit
                }

                IndexBadge(
                    index = attachment.index,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onRemove(attachment) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "移除附件 ${attachment.index}",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadingScrim() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun FailedScrim(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable(onClick = onRetry),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "重试上传",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "重试",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun IndexBadge(index: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.52f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = index.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val THUMBNAIL_SIZE = 56.dp
