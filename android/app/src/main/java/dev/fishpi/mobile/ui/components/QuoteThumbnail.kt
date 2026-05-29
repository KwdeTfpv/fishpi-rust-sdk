package dev.fishpi.mobile.ui.components

import dev.fishpi.mobile.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size

@Composable
internal fun QuoteThumbnail(url: String) {
    val context = LocalContext.current
    val imageModel = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(Size(96, 96))
            .build()
    }
    AsyncImage(
        model = imageModel,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "引用图片",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusField))
            .background(FishPiTheme.accent.copy(alpha = 0.12f)),
    )
}


