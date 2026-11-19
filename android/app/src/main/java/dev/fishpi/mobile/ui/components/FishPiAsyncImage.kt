package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.rememberFishPiImageLoader

@Composable
internal fun FishPiAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholderBackground: Color = FishPiTheme.surfaceContainer,
    placeholderIcon: ImageVector = Icons.Rounded.Image,
    errorIcon: ImageVector = Icons.Rounded.BrokenImage,
    alpha: Float = 1f,
    onSuccess: ((IntSize) -> Unit)? = null,
) {
    val shapedModifier = if (shape != null) Modifier.clip(shape).then(modifier) else modifier
    val hasModel = when (model) {
        null -> false
        is CharSequence -> model.isNotBlank()
        else -> true
    }
    if (!hasModel) {
        FishPiAsyncImagePlaceholder(
            icon = errorIcon,
            background = placeholderBackground,
            alpha = alpha,
            modifier = shapedModifier,
        )
        return
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        imageLoader = rememberFishPiImageLoader(),
        modifier = shapedModifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        loading = {
            FishPiAsyncImagePlaceholder(
                icon = placeholderIcon,
                background = placeholderBackground,
                alpha = alpha,
                modifier = Modifier.fillMaxSize(),
            )
        },
        success = { SubcomposeAsyncImageContent() },
        error = {
            FishPiAsyncImagePlaceholder(
                icon = errorIcon,
                background = placeholderBackground,
                alpha = alpha,
                modifier = Modifier.fillMaxSize(),
            )
        },
        onSuccess = onSuccess?.let { callback ->
            { state: AsyncImagePainter.State.Success ->
                callback(IntSize(state.result.image.width, state.result.image.height))
            }
        },
    )
}

@Composable
private fun FishPiAsyncImagePlaceholder(
    icon: ImageVector,
    background: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(if (alpha < 1f) Modifier.alpha(alpha) else Modifier)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FishPiTheme.weakText,
            modifier = Modifier.size(20.dp),
        )
    }
}
